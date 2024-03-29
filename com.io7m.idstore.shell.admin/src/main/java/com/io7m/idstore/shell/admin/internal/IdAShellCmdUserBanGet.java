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

import com.io7m.idstore.protocol.admin.IdACommandUserBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseUserBanGet;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType.QConstant;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * "user-ban-get"
 */

public final class IdAShellCmdUserBanGet
  extends IdAShellCmdAbstractCR<IdACommandUserBanGet, IdAResponseUserBanGet>
{
  private static final QParameterNamed1<UUID> USER_ID =
    new QParameterNamed1<>(
      "--user",
      List.of(),
      new QConstant("The user ID."),
      Optional.empty(),
      UUID.class
    );

  /**
   * Construct a command.
   *
   * @param inServices The service directory
   */

  public IdAShellCmdUserBanGet(
    final RPServiceDirectoryType inServices)
  {
    super(
      inServices,
      new QCommandMetadata(
        "user-ban-get",
        new QConstant("Retrieve the ban on a user, if one exists."),
        Optional.empty()
      ),
      IdACommandUserBanGet.class,
      IdAResponseUserBanGet.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(USER_ID);
  }

  @Override
  protected IdACommandUserBanGet onCreateCommand(
    final QCommandContextType context)
  {
    return new IdACommandUserBanGet(context.parameterValue(USER_ID));
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseUserBanGet response)
  {
    final var w = context.output();
    response.ban().ifPresentOrElse(ban -> {
      w.print("User is banned: ");
      w.print(ban.reason());
      w.println();

      ban.expires().ifPresentOrElse(time -> {
        w.printf(
          "The ban expires on %s (%s from now).%n",
          time,
          Duration.between(OffsetDateTime.now(), time)
        );
      }, () -> w.println("The ban does not expire."));
    }, () -> w.println("The user is not banned."));
  }
}
