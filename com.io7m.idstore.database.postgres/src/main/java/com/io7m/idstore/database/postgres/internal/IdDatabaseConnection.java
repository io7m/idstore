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

package com.io7m.idstore.database.postgres.internal;

import com.io7m.idstore.database.api.IdDatabaseConnectionType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseRole;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;
import static java.util.Objects.requireNonNullElse;

record IdDatabaseConnection(
  IdDatabase database,
  Connection connection,
  OffsetDateTime timeStart,
  IdDatabaseRole role,
  Span connectionSpan)
  implements IdDatabaseConnectionType
{
  @Override
  public IdDatabaseTransactionType openTransaction()
    throws IdDatabaseException
  {
    final var transactionSpan =
      this.database.tracer()
        .spanBuilder("IdDatabaseTransaction")
        .setParent(Context.current().with(this.connectionSpan))
        .startSpan();

    try {
      final var t =
        new IdDatabaseTransaction(
          this,
          transactionSpan
        );

      this.database.counterTransactions().add(1L);
      t.setRole(this.role);
      t.commit();
      return t;
    } catch (final SQLException e) {
      transactionSpan.recordException(e);
      transactionSpan.end();
      throw new IdDatabaseException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        SQL_ERROR,
        Map.of(),
        Optional.empty()
      );
    }
  }

  @Override
  public void close()
    throws IdDatabaseException
  {
    try {
      final var timeNow = OffsetDateTime.now();
      this.database.setConnectionTimeNow(
        Duration.between(this.timeStart, timeNow).toNanos()
      );

      if (!this.connection.isClosed()) {
        this.connection.close();
      }
    } catch (final SQLException e) {
      this.connectionSpan.recordException(e);
      throw new IdDatabaseException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        SQL_ERROR,
        Map.of(),
        Optional.empty()
      );
    } finally {
      this.connectionSpan.end();
    }
  }
}
