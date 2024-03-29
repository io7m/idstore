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

package com.io7m.idstore.shell.admin.internal;

import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanCreate;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QParameterNamed01;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * "admin-ban-create"
 */

public final class IdAShellCmdAdminBanCreate
  extends IdAShellCmdAbstractCR<IdACommandAdminBanCreate, IdAResponseAdminBanCreate>
{
  private static final QParameterNamed1<UUID> USER_ID =
    new QParameterNamed1<>(
      "--admin",
      List.of(),
      new QConstant("The admin ID."),
      Optional.empty(),
      UUID.class
    );

  private static final QParameterNamed1<String> REASON =
    new QParameterNamed1<>(
      "--reason",
      List.of(),
      new QConstant("The ban reason."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed01<OffsetDateTime> EXPIRES =
    new QParameterNamed01<>(
      "--expires-on",
      List.of(),
      new QConstant("The time/date the ban expires."),
      Optional.empty(),
      OffsetDateTime.class
    );

  /**
   * Construct a command.
   *
   * @param inServices The service directory
   */

  public IdAShellCmdAdminBanCreate(
    final RPServiceDirectoryType inServices)
  {
    super(
      inServices,
      new QCommandMetadata(
        "admin-ban-create",
        new QConstant("Ban an admin."),
        Optional.empty()
      ),
      IdACommandAdminBanCreate.class,
      IdAResponseAdminBanCreate.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(USER_ID, REASON, EXPIRES);
  }

  @Override
  protected IdACommandAdminBanCreate onCreateCommand(
    final QCommandContextType context)
  {
    return new IdACommandAdminBanCreate(
      new IdBan(
        context.parameterValue(USER_ID),
        context.parameterValue(REASON),
        context.parameterValue(EXPIRES)
      )
    );
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseAdminBanCreate response)
  {

  }
}
