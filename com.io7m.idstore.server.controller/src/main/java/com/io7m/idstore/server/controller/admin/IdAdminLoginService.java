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

package com.io7m.idstore.server.controller.admin;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.service.clock.IdServerClock;
import com.io7m.idstore.server.service.sessions.IdSessionAdmin;
import com.io7m.idstore.server.service.sessions.IdSessionAdminService;
import com.io7m.idstore.services.api.IdServiceType;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.BANNED;

/**
 * A service that handles the logic for admin logins.
 */

public final class IdAdminLoginService implements IdServiceType
{
  private final IdServerClock clock;
  private final IdServerStrings strings;
  private final IdSessionAdminService sessions;

  /**
   * A service that handles the logic for admin logins.
   *
   * @param inClock    The clock
   * @param inStrings  The string resources
   * @param inSessions A session service
   */

  public IdAdminLoginService(
    final IdServerClock inClock,
    final IdServerStrings inStrings,
    final IdSessionAdminService inSessions)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.strings =
      Objects.requireNonNull(inStrings, "strings");
    this.sessions =
      Objects.requireNonNull(inSessions, "inSessions");
  }

  /**
   * A record of an admin logging in.
   *
   * @param session The created session
   * @param admin   The admin
   */

  public record IdAdminLoggedIn(
    IdSessionAdmin session,
    IdAdmin admin)
  {
    /**
     * A record of an admin logging in.
     */

    public IdAdminLoggedIn
    {
      Objects.requireNonNull(session, "session");
      Objects.requireNonNull(admin, "user");
    }
  }

  /**
   * Try logging in. Create a new session if logging in succeeds, or raise an
   * exception if the login cannot proceed for any reason (invalid credentials,
   * banned user, etc).
   *
   * @param transaction A database transaction
   * @param requestId   The ID of the request
   * @param username    The username
   * @param password    The password
   * @param metadata    The request metadata
   *
   * @return A login record
   *
   * @throws IdCommandExecutionFailure On errors
   */

  public IdAdminLoggedIn adminLogin(
    final IdDatabaseTransactionType transaction,
    final UUID requestId,
    final String username,
    final String password,
    final Map<String, String> metadata)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(transaction, "transaction");
    Objects.requireNonNull(requestId, "requestId");
    Objects.requireNonNull(username, "username");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(metadata, "metadata");

    try {
      final var admins =
        transaction.queries(IdDatabaseAdminsQueriesType.class);
      final var user =
        admins.adminGetForNameRequire(new IdName(username));

      this.checkBan(requestId, admins, user);

      final var ok =
        user.password().check(password);

      if (!ok) {
        throw new IdCommandExecutionFailure(
          this.strings.format("errorInvalidUsernamePassword"),
          requestId,
          401,
          AUTHENTICATION_ERROR
        );
      }

      admins.adminLogin(user.id(), metadata);
      final var session = this.sessions.createSession(user.id());
      return new IdAdminLoggedIn(session, user.withRedactedPassword());
    } catch (final IdDatabaseException e) {
      if (Objects.equals(e.errorCode(), ADMIN_NONEXISTENT)) {
        throw this.authenticationFailed(requestId, e);
      }
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        requestId,
        500,
        e.errorCode()
      );
    } catch (final IdPasswordException e) {
      throw new IdCommandExecutionFailure(
        e.getMessage(),
        e,
        requestId,
        500,
        e.errorCode()
      );
    }
  }

  private void checkBan(
    final UUID requestId,
    final IdDatabaseAdminsQueriesType admins,
    final IdAdmin user)
    throws IdDatabaseException, IdCommandExecutionFailure
  {
    final var banOpt =
      admins.adminBanGet(user.id());

    /*
     * If there's no ban, allow the login.
     */

    if (banOpt.isEmpty()) {
      return;
    }

    final var ban = banOpt.get();
    final var expiresOpt = ban.expires();

    /*
     * If there's no expiration on the ban, deny the login.
     */

    if (expiresOpt.isEmpty()) {
      throw new IdCommandExecutionFailure(
        this.strings.format("bannedNoExpire", ban.reason()),
        requestId,
        403,
        BANNED
      );
    }

    /*
     * If the current time is before the expiration date, deny the login.
     */

    final var timeExpires = expiresOpt.get();
    final var timeNow = this.clock.now();

    if (timeNow.compareTo(timeExpires) < 0) {
      throw new IdCommandExecutionFailure(
        this.strings.format("banned", ban.reason(), timeExpires),
        requestId,
        403,
        BANNED
      );
    }
  }

  private IdCommandExecutionFailure authenticationFailed(
    final UUID requestId,
    final Exception cause)
  {
    return new IdCommandExecutionFailure(
      this.strings.format("errorInvalidUsernamePassword"),
      cause,
      requestId,
      401,
      AUTHENTICATION_ERROR
    );
  }

  @Override
  public String description()
  {
    return "Admin login service.";
  }
}
