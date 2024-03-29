/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.idstore.server.controller.user;

import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.user.IdUCommandRealnameUpdate;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.IdUResponseUserUpdate;
import com.io7m.idstore.server.security.IdSecUserActionRealnameUpdate;

import java.util.Optional;

/**
 * IdUCmdRealNameUpdate
 */

public final class IdUCmdRealNameUpdate
  extends IdUCmdAbstract<IdUCommandRealnameUpdate>
{
  /**
   * IdUCmdRealNameUpdate
   */

  public IdUCmdRealNameUpdate()
  {

  }

  @Override
  protected IdUResponseType executeActual(
    final IdUCommandContext context,
    final IdUCommandRealnameUpdate command)
    throws IdException
  {
    final var user = context.user();
    context.securityCheck(new IdSecUserActionRealnameUpdate(user));

    final var transaction =
      context.transaction();

    final var users =
      transaction.queries(IdDatabaseUsersQueriesType.class);

    transaction.userIdSet(user.id());
    users.userUpdate(
      user.id(),
      Optional.empty(),
      Optional.of(command.realName()),
      Optional.empty()
    );

    return new IdUResponseUserUpdate(
      context.requestId(),
      users.userGetRequire(user.id())
    );
  }
}
