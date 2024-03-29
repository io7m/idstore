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

import com.io7m.idstore.model.IdAuditSearchParameters;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.protocol.admin.IdACommandAuditSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAuditSearchBegin;
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

/**
 * "audit-search-begin"
 */

public final class IdAShellCmdAuditSearchBegin
  extends IdAShellCmdAbstractCR<IdACommandAuditSearchBegin, IdAResponseAuditSearchBegin>
{
  private static final QParameterNamed1<OffsetDateTime> TIME_FROM =
    new QParameterNamed1<>(
      "--time-from",
      List.of(),
      new QConstant("Return audit events later than this date."),
      Optional.of(IdTimeRange.largest().timeLower()),
      OffsetDateTime.class
    );

  private static final QParameterNamed1<OffsetDateTime> TIME_TO =
    new QParameterNamed1<>(
      "--time-to",
      List.of(),
      new QConstant("Return audit events earlier than this date."),
      Optional.of(IdTimeRange.largest().timeUpper()),
      OffsetDateTime.class
    );

  private static final QParameterNamed01<String> OWNER =
    new QParameterNamed01<>(
      "--owner",
      List.of(),
      new QConstant("Filter events by owner."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed01<String> TYPE =
    new QParameterNamed01<>(
      "--type",
      List.of(),
      new QConstant("Filter events by type."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<Integer> LIMIT =
    new QParameterNamed1<>(
      "--limit",
      List.of(),
      new QConstant("The maximum number of results per page."),
      Optional.of(Integer.valueOf(10)),
      Integer.class
    );

  /**
   * Construct a command.
   *
   * @param inServices The service directory
   */

  public IdAShellCmdAuditSearchBegin(
    final RPServiceDirectoryType inServices)
  {
    super(
      inServices,
      new QCommandMetadata(
        "audit-search-begin",
        new QConstant("Begin searching for audits."),
        Optional.empty()
      ),
      IdACommandAuditSearchBegin.class,
      IdAResponseAuditSearchBegin.class
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return List.of(
      LIMIT,
      OWNER,
      TIME_FROM,
      TIME_TO,
      TYPE
    );
  }

  @Override
  protected IdACommandAuditSearchBegin onCreateCommand(
    final QCommandContextType context)
  {
    final var parameters =
      new IdAuditSearchParameters(
        new IdTimeRange(
          context.parameterValue(TIME_FROM),
          context.parameterValue(TIME_TO)
        ),
        context.parameterValue(OWNER),
        context.parameterValue(TYPE),
        context.parameterValue(LIMIT).intValue()
      );

    return new IdACommandAuditSearchBegin(parameters);
  }

  @Override
  protected void onFormatResponse(
    final QCommandContextType context,
    final IdAResponseAuditSearchBegin response)
    throws Exception
  {
    this.formatter().formatAudits(response.page());
  }
}
