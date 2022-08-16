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

import com.io7m.idstore.protocol.admin_v1.IdA1CommandLogin;
import com.io7m.idstore.protocol.admin_v1.IdA1ResponseType;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutorType;

import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;

/**
 * IdA1CmdAdminSelf
 */

public final class IdA1CmdAdminLogin
  implements IdCommandExecutorType<
  IdA1CommandContext, IdA1CommandLogin, IdA1ResponseType>
{
  /**
   * IdA1CmdAdminSelf
   */

  public IdA1CmdAdminLogin()
  {

  }

  @Override
  public IdA1ResponseType execute(
    final IdA1CommandContext context,
    final IdA1CommandLogin command)
    throws IdCommandExecutionFailure
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    throw context.failFormatted(
      400,
      PROTOCOL_ERROR,
      "commandNotHere"
    );
  }
}
