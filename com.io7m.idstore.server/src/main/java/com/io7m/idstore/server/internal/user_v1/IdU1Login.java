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


package com.io7m.idstore.server.internal.user_v1;

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user_v1.IdU1CommandLogin;
import com.io7m.idstore.protocol.user_v1.IdU1Messages;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseLogin;
import com.io7m.idstore.server.internal.IdHTTPErrorStatusException;
import com.io7m.idstore.server.internal.IdRequestLimits;
import com.io7m.idstore.server.internal.IdRequests;
import com.io7m.idstore.server.internal.IdServerClock;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.HTTP_METHOD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;
import static com.io7m.idstore.server.logging.IdServerMDCRequestProcessor.mdcForRequest;
import static org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400;
import static org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500;
import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.UNAUTHORIZED_401;

/**
 * A servlet that handles user logins.
 */

public final class IdU1Login extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdU1Login.class);

  private final IdDatabaseType database;
  private final IdU1Messages messages;
  private final IdServerStrings strings;
  private final IdServerClock clock;
  private final IdU1Sends errors;
  private final IdRequestLimits limits;

  /**
   * A servlet that handles user logins.
   *
   * @param inServices The service directory
   */

  public IdU1Login(
    final IdServiceDirectoryType inServices)
  {
    Objects.requireNonNull(inServices, "inServices");

    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.messages =
      inServices.requireService(IdU1Messages.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.clock =
      inServices.requireService(IdServerClock.class);
    this.errors =
      inServices.requireService(IdU1Sends.class);
    this.limits =
      inServices.requireService(IdRequestLimits.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    try (var ignored0 = mdcForRequest(request)) {
      try {
        if (!Objects.equals(request.getMethod(), "POST")) {
          throw new IdHTTPErrorStatusException(
            METHOD_NOT_ALLOWED_405,
            HTTP_METHOD_ERROR,
            this.strings.format("methodNotAllowed")
          );
        }

        final var login =
          this.readLoginCommand(request);

        try (var connection = this.database.openConnection(IDSTORE)) {
          try (var transaction = connection.openTransaction()) {
            final var users =
              transaction.queries(IdDatabaseUsersQueriesType.class);
            this.tryLogin(request, response, users, login);
            transaction.commit();
          }
        }

      } catch (final IdHTTPErrorStatusException e) {
        this.errors.sendError(
          response,
          requestIdFor(request),
          e.statusCode(),
          e.errorCode(),
          e.getMessage()
        );
      } catch (final IdPasswordException e) {
        LOG.debug("password: ", e);
        this.errors.sendError(
          response,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          PASSWORD_ERROR,
          e.getMessage()
        );
      } catch (final IdDatabaseException e) {
        LOG.debug("database: ", e);
        this.errors.sendError(
          response,
          requestIdFor(request),
          INTERNAL_SERVER_ERROR_500,
          SQL_ERROR,
          e.getMessage()
        );
      }
    }
  }

  private void tryLogin(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final IdDatabaseUsersQueriesType users,
    final IdU1CommandLogin login)
    throws
    IdHTTPErrorStatusException,
    IdDatabaseException,
    IdPasswordException, IOException
  {
    final var userOpt =
      users.userGetForName(new IdName(login.userName()));

    if (userOpt.isEmpty()) {
      throw new IdHTTPErrorStatusException(
        UNAUTHORIZED_401,
        AUTHENTICATION_ERROR,
        this.strings.format("loginFailed")
      );
    }

    final var user =
      userOpt.get();
    final var ok =
      user.password().check(login.password());

    if (!ok) {
      throw new IdHTTPErrorStatusException(
        UNAUTHORIZED_401,
        AUTHENTICATION_ERROR,
        this.strings.format("loginFailed")
      );
    }

    LOG.info("user '{}' logged in", login.userName());
    final var session = request.getSession();
    session.setAttribute("UserID", user.id());
    response.setStatus(200);

    users.userLogin(
      user.id(),
      IdRequests.requestUserAgent(request),
      request.getRemoteAddr()
    );

    this.sendLoginResponse(request, response, user);
  }

  private void sendLoginResponse(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final IdUser user)
    throws IOException
  {
    response.setStatus(200);
    response.setContentType(IdU1Messages.contentType());

    try {
      final var data =
        this.messages.serialize(
          new IdU1ResponseLogin(
            requestIdFor(request),
            this.clock.now()
          )
        );
      response.setContentLength(data.length + 2);
      try (var output = response.getOutputStream()) {
        output.write(data);
        output.write('\r');
        output.write('\n');
      }
    } catch (final IdProtocolException e) {
      throw new IOException(e);
    }
  }

  private IdU1CommandLogin readLoginCommand(
    final HttpServletRequest request)
    throws IdHTTPErrorStatusException, IOException
  {
    try (var input = this.limits.boundedMaximumInput(request, 1024)) {
      final var data = input.readAllBytes();
      final var message = this.messages.parse(data);
      if (message instanceof IdU1CommandLogin login) {
        return login;
      }
    } catch (final IdProtocolException e) {
      throw new IdHTTPErrorStatusException(
        BAD_REQUEST_400,
        PROTOCOL_ERROR,
        e.getMessage(),
        e
      );
    }

    throw new IdHTTPErrorStatusException(
      BAD_REQUEST_400,
      PROTOCOL_ERROR,
      this.strings.format("expectedCommand", "CommandLogin")
    );
  }
}