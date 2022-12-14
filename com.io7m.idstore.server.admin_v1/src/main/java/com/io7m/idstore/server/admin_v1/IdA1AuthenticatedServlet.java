/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.idstore.server.admin_v1;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.http.IdHTTPErrorStatusException;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.idstore.server.service.sessions.IdSessionAdminService;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;

/**
 * A servlet that checks that an admin is authenticated before delegating
 * execution to a subclass.
 */

public abstract class IdA1AuthenticatedServlet
  extends IdCommonInstrumentedServlet
{
  private IdAdmin admin;
  private final IdACB1Sends sends;
  private final IdDatabaseType database;
  private final IdServerClock clock;
  private final IdServerStrings strings;
  private final IdSessionAdminService adminSessions;
  private IdSessionAdmin adminSession;

  /**
   * A servlet that checks that an admin is authenticated before delegating
   * execution to a subclass.
   *
   * @param services The service directory
   */

  protected IdA1AuthenticatedServlet(
    final IdServiceDirectoryType services)
  {
    super(Objects.requireNonNull(services, "services"));

    this.strings =
      services.requireService(IdServerStrings.class);
    this.clock =
      services.requireService(IdServerClock.class);
    this.sends =
      services.requireService(IdACB1Sends.class);
    this.database =
      services.requireService(IdDatabaseType.class);
    this.adminSessions =
      services.requireService(IdSessionAdminService.class);
  }

  /**
   * @return The authenticated user
   */

  protected final IdAdmin admin()
  {
    return this.admin;
  }

  protected final IdACB1Sends sends()
  {
    return this.sends;
  }

  protected final IdServerClock clock()
  {
    return this.clock;
  }

  protected final IdServerStrings strings()
  {
    return this.strings;
  }

  protected abstract void serviceAuthenticated(
    HttpServletRequest request,
    HttpServletResponse servletResponse,
    IdSessionAdmin session)
    throws Exception;

  @Override
  protected final void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws IOException
  {
    try {
      final var httpSession =
        request.getSession(true);
      final var adminSessionId =
        (IdSessionSecretIdentifier) httpSession.getAttribute("ID");

      if (adminSessionId != null) {
        final var adminSessionOpt =
          this.adminSessions.findSession(adminSessionId);

        if (adminSessionOpt.isPresent()) {
          this.adminSession = adminSessionOpt.get();
          this.admin = this.adminGet(this.adminSession.adminId());
          this.serviceAuthenticated(request, servletResponse, this.adminSession);
          return;
        }
      }

      servletResponse.setStatus(401);
      this.sends.sendError(
        servletResponse,
        IdRequestUniqueIDs.requestIdFor(request),
        HttpStatus.UNAUTHORIZED_401,
        AUTHENTICATION_ERROR,
        this.strings.format("unauthorized")
      );
    } catch (final IdHTTPErrorStatusException e) {
      this.sends.sendError(
        servletResponse,
        IdRequestUniqueIDs.requestIdFor(request),
        e.statusCode(),
        e.errorCode(),
        e.getMessage()
      );
    } catch (final IdPasswordException | IdDatabaseException e) {
      this.sends.sendError(
        servletResponse,
        IdRequestUniqueIDs.requestIdFor(request),
        INTERNAL_SERVER_ERROR_500,
        e.errorCode(),
        e.getMessage()
      );
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  private IdAdmin adminGet(final UUID id)
    throws IdDatabaseException
  {
    try (var c = this.database.openConnection(IDSTORE)) {
      try (var t = c.openTransaction()) {
        final var q =
          t.queries(IdDatabaseAdminsQueriesType.class);
        return q.adminGetRequire(id);
      }
    }
  }
}
