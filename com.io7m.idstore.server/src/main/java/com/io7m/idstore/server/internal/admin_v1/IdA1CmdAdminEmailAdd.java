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


package com.io7m.idstore.server.internal.admin_v1;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.protocol.admin_v1.IdA1Admin;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminUpdate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAdminEmailAdd;
import com.io7m.idstore.server.security.IdSecurityException;

import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR_UNIQUE;

/**
 * IdA1CmdAdminEmailAdd
 */

public final class IdA1CmdAdminEmailAdd
  extends IdA1CmdAbstract<
  IdA1CommandContext, IdA1CommandAdminEmailAdd, IdA1ResponseType>
{
  /**
   * IdA1CmdAdminEmailAdd
   */

  public IdA1CmdAdminEmailAdd()
  {

  }

  @Override
  protected IdA1ResponseType executeActual(
    final IdA1CommandContext context,
    final IdA1CommandAdminEmailAdd command)
    throws IdCommandExecutionFailure, IdDatabaseException, IdSecurityException
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();
    final var newAdmin =
      command.admin();

    context.securityCheck(new IdSecAdminActionAdminEmailAdd(admin, newAdmin));

    transaction.adminIdSet(admin.id());

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var strings =
      context.services().requireService(IdServerStrings.class);

    try {
      admins.adminEmailAdd(command.admin(), new IdEmail(command.email()));
    } catch (final IdDatabaseException e) {
      if (Objects.equals(e.errorCode(), SQL_ERROR_UNIQUE)) {
        throw new IdDatabaseException(
          strings.format("emailDuplicate"),
          e,
          SQL_ERROR_UNIQUE
        );
      }
      throw e;
    }

    final var afterAdmin =
      admins.adminGetRequire(newAdmin)
        .redactPassword();

    return new IdA1ResponseAdminUpdate(
      context.requestId(),
      IdA1Admin.ofAdmin(afterAdmin)
    );
  }
}