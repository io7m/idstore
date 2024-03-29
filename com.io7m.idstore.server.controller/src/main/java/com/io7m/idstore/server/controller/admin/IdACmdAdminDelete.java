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

package com.io7m.idstore.server.controller.admin;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.admin.IdACommandAdminDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminDelete;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.security.IdSecAdminActionAdminDelete;

import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.OPERATION_NOT_PERMITTED;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_DELETE_SELF;

/**
 * IdACmdAdminDelete
 */

public final class IdACmdAdminDelete
  extends IdACmdAbstract<
  IdACommandContext, IdACommandAdminDelete, IdAResponseType>
{
  /**
   * IdACmdAdminDelete
   */

  public IdACmdAdminDelete()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandAdminDelete command)
    throws IdException
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionAdminDelete(admin));

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    if (Objects.equals(admin.id(), command.adminId())) {
      throw context.failFormatted(
        400,
        OPERATION_NOT_PERMITTED,
        ERROR_DELETE_SELF
      );
    }

    transaction.adminIdSet(admin.id());
    admins.adminDelete(command.adminId());
    return new IdAResponseAdminDelete(context.requestId());
  }
}
