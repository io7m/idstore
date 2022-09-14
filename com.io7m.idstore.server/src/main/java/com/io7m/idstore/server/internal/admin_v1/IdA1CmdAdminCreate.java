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
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.admin_v1.IdA1Admin;
import com.io7m.idstore.protocol.admin_v1.IdA1AdminPermission;
import com.io7m.idstore.protocol.admin_v1.IdA1CommandAdminCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseAdminCreate;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAdminCreate;
import com.io7m.idstore.server.security.IdSecurityException;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * IdA1CmdAdminCreate
 */

public final class IdA1CmdAdminCreate
  extends IdA1CmdAbstract<
  IdA1CommandContext, IdA1CommandAdminCreate, IdA1ResponseType>
{
  /**
   * IdA1CmdAdminCreate
   */

  public IdA1CmdAdminCreate()
  {

  }

  @Override
  protected IdA1ResponseType executeActual(
    final IdA1CommandContext context,
    final IdA1CommandAdminCreate command)
    throws
    IdValidityException,
    IdSecurityException,
    IdDatabaseException,
    IdPasswordException,
    IdCommandExecutionFailure
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    final var targetPermissions =
      command.permissions()
        .stream()
        .map(IdA1AdminPermission::toPermission)
        .collect(Collectors.toUnmodifiableSet());

    context.securityCheck(new IdSecAdminActionAdminCreate(admin, targetPermissions));

    transaction.adminIdSet(admin.id());

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var id =
      command.id().orElse(UUID.randomUUID());
    final var idName =
      new IdName(command.idName());
    final var realName =
      new IdRealName(command.realName());
    final var email =
      new IdEmail(command.email());
    final var password =
      command.password().toPassword();

    final var newAdmin =
      admins.adminCreate(
        id,
        idName,
        realName,
        email,
        context.now(),
        password,
        targetPermissions
      ).redactPassword();

    return new IdA1ResponseAdminCreate(
      context.requestId(),
      IdA1Admin.ofAdmin(newAdmin)
    );
  }
}
