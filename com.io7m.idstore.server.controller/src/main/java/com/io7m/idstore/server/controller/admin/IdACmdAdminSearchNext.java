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
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.security.IdSecAdminActionAdminRead;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;

/**
 * IdACmdAdminSearchNext
 */

public final class IdACmdAdminSearchNext
  extends IdACmdAbstract<
  IdACommandContext, IdACommandAdminSearchNext, IdAResponseType>
{
  /**
   * IdACmdAdminSearchNext
   */

  public IdACmdAdminSearchNext()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandAdminSearchNext command)
    throws IdException, IdCommandExecutionFailure
  {
    final var transaction =
      context.transaction();
    final var admin =
      context.admin();

    context.securityCheck(new IdSecAdminActionAdminRead(admin));

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var session =
      context.session();
    final var searchOpt =
      session.adminSearch();

    if (searchOpt.isEmpty()) {
      throw context.failFormatted(
        400, PROTOCOL_ERROR, "errorSearchStart");
    }

    return new IdAResponseAdminSearchNext(
      context.requestId(),
      searchOpt.get().pageNext(admins)
    );
  }
}
