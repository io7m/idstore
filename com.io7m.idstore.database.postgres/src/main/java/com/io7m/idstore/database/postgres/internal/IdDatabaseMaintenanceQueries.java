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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseMaintenanceQueriesType;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.jaffirm.core.Invariants;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;

import static com.io7m.idstore.database.postgres.internal.IdDatabaseAdminsQueries.permissionsSerialize;
import static com.io7m.idstore.database.postgres.internal.IdDatabaseExceptions.handleDatabaseException;
import static com.io7m.idstore.database.postgres.internal.Tables.ADMINS;
import static com.io7m.idstore.database.postgres.internal.Tables.BANS;
import static com.io7m.idstore.database.postgres.internal.Tables.EMAIL_VERIFICATIONS;
import static com.io7m.idstore.database.postgres.internal.Tables.USER_PASSWORD_RESETS;
import static java.lang.Integer.valueOf;

/**
 * The maintenance queries.
 */

public final class IdDatabaseMaintenanceQueries
  extends IdBaseQueries
  implements IdDatabaseMaintenanceQueriesType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdDatabaseMaintenanceQueries.class);

  IdDatabaseMaintenanceQueries(
    final IdDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  @Override
  public void runMaintenance()
    throws IdDatabaseException
  {
    final var exceptions =
      new ExceptionTracker<IdDatabaseException>();

    try {
      this.runExpireEmailVerifications();
    } catch (final IdDatabaseException e) {
      exceptions.addException(e);
    }

    try {
      this.runExpireBans();
    } catch (final IdDatabaseException e) {
      exceptions.addException(e);
    }

    try {
      this.runExpirePasswordResets();
    } catch (final IdDatabaseException e) {
      exceptions.addException(e);
    }

    try {
      this.runUpdateInitialAdminPermissions();
    } catch (final IdDatabaseException e) {
      exceptions.addException(e);
    }

    exceptions.throwIfNecessary();
  }

  private void runUpdateInitialAdminPermissions()
    throws IdDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseMaintenanceQueries.runUpdateInitialAdminPermissions");

    try {
      final var updated =
        context.update(ADMINS)
          .set(
            ADMINS.PERMISSIONS,
            permissionsSerialize(EnumSet.allOf(IdAdminPermission.class)))
          .where(ADMINS.INITIAL.eq(Boolean.TRUE))
          .execute();

      Invariants.checkInvariantI(
        updated,
        updated <= 1,
        x -> "At most one administrator must have been updated."
      );

      LOG.debug("updated permissions for {} initial admins", valueOf(updated));
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, Map.of());
    } finally {
      querySpan.end();
    }
  }

  private void runExpirePasswordResets()
    throws IdDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseMaintenanceQueries.runExpirePasswordResets");

    try {
      final var deleted =
        context.deleteFrom(USER_PASSWORD_RESETS)
          .where(USER_PASSWORD_RESETS.EXPIRES.lt(this.currentTime()))
          .execute();

      LOG.debug("deleted {} expired password resets", valueOf(deleted));
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, Map.of());
    } finally {
      querySpan.end();
    }
  }

  private void runExpireEmailVerifications()
    throws IdDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseMaintenanceQueries.runExpireEmailVerifications");

    try {
      final var deleted =
        context.deleteFrom(EMAIL_VERIFICATIONS)
          .where(EMAIL_VERIFICATIONS.EXPIRES.lt(this.currentTime()))
          .execute();

      LOG.debug("deleted {} expired email verifications", valueOf(deleted));
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, Map.of());
    } finally {
      querySpan.end();
    }
  }

  private void runExpireBans()
    throws IdDatabaseException
  {
    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseMaintenanceQueries.runExpireBans");

    try {
      final var deleted =
        context.deleteFrom(BANS)
          .where(BANS.EXPIRES.lt(this.currentTime()))
          .execute();

      LOG.debug("deleted {} expired bans", valueOf(deleted));
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, Map.of());
    } finally {
      querySpan.end();
    }
  }
}
