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

package com.io7m.idstore.database.postgres.internal;

import com.io7m.idstore.database.api.IdDatabaseConnectionType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseRole;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SQL_ERROR;

/**
 * The default postgres server database implementation.
 */

public final class IdDatabase implements IdDatabaseType
{
  private final Clock clock;
  private final HikariDataSource dataSource;
  private final Settings settings;
  private final IdDatabaseMetrics metrics;

  /**
   * The default postgres server database implementation.
   *
   * @param inClock                 The clock
   * @param inDataSource            A pooled data source
   * @param inMetrics               A metrics bean
   */

  public IdDatabase(
    final Clock inClock,
    final HikariDataSource inDataSource,
    final IdDatabaseMetrics inMetrics)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.dataSource =
      Objects.requireNonNull(inDataSource, "dataSource");
    this.metrics =
      Objects.requireNonNull(inMetrics, "metrics");
    this.settings =
      new Settings().withRenderNameCase(RenderNameCase.LOWER);
  }

  @Override
  public void close()
  {
    this.dataSource.close();
  }

  @Override
  public IdDatabaseConnectionType openConnection(
    final IdDatabaseRole role)
    throws IdDatabaseException
  {
    try {
      final var conn = this.dataSource.getConnection();
      conn.setAutoCommit(false);
      return new IdDatabaseConnection(this, conn, role);
    } catch (final SQLException e) {
      throw new IdDatabaseException(e.getMessage(), e, SQL_ERROR);
    }
  }

  /**
   * @return The database metrics
   */

  public IdDatabaseMetrics metrics()
  {
    return this.metrics;
  }

  /**
   * @return The jooq SQL settings
   */

  public Settings settings()
  {
    return this.settings;
  }

  /**
   * @return The clock used for time-related queries
   */

  public Clock clock()
  {
    return this.clock;
  }

  @Override
  public String description()
  {
    return "Server database service.";
  }

  @Override
  public String toString()
  {
    return "[IdDatabase 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode()));
  }
}