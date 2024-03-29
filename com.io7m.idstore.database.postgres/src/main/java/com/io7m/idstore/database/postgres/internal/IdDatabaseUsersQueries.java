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
import com.io7m.idstore.database.api.IdDatabaseUserSearchByEmailType;
import com.io7m.idstore.database.api.IdDatabaseUserSearchType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.database.postgres.internal.tables.records.EmailsRecord;
import com.io7m.idstore.database.postgres.internal.tables.records.LoginHistoryRecord;
import com.io7m.idstore.database.postgres.internal.tables.records.UserPasswordResetsRecord;
import com.io7m.idstore.database.postgres.internal.tables.records.UsersRecord;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdLogin;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithms;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserColumnOrdering;
import com.io7m.idstore.model.IdUserPasswordReset;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import com.io7m.idstore.model.IdUserSearchParameters;
import com.io7m.idstore.model.IdUserSummary;
import com.io7m.jqpage.core.JQField;
import com.io7m.jqpage.core.JQKeysetRandomAccessPageDefinition;
import com.io7m.jqpage.core.JQKeysetRandomAccessPagination;
import com.io7m.jqpage.core.JQKeysetRandomAccessPaginationParameters;
import com.io7m.jqpage.core.JQOrder;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.postgres.extensions.types.Hstore;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.database.postgres.internal.IdDatabaseAuditQueries.AU_DATA;
import static com.io7m.idstore.database.postgres.internal.IdDatabaseExceptions.handleDatabaseException;
import static com.io7m.idstore.database.postgres.internal.Tables.AUDIT;
import static com.io7m.idstore.database.postgres.internal.Tables.BANS;
import static com.io7m.idstore.database.postgres.internal.Tables.EMAILS;
import static com.io7m.idstore.database.postgres.internal.Tables.EMAIL_VERIFICATIONS;
import static com.io7m.idstore.database.postgres.internal.Tables.LOGIN_HISTORY;
import static com.io7m.idstore.database.postgres.internal.Tables.USERS;
import static com.io7m.idstore.database.postgres.internal.Tables.USER_IDS;
import static com.io7m.idstore.database.postgres.internal.Tables.USER_PASSWORD_RESETS;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PASSWORD_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.USER_NONEXISTENT;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHostProxied;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.DB_STATEMENT;
import static java.lang.Boolean.TRUE;

final class IdDatabaseUsersQueries
  extends IdBaseQueries
  implements IdDatabaseUsersQueriesType
{
  static IdDatabaseException userDoesNotExist(
    final Map<String, String> attributes)
  {
    return new IdDatabaseException(
      "User does not exist",
      USER_NONEXISTENT,
      attributes,
      Optional.empty()
    );
  }

  IdDatabaseUsersQueries(
    final IdDatabaseTransaction inTransaction)
  {
    super(inTransaction);
  }

  private static IdUser userMap(
    final UsersRecord userRecord,
    final Result<EmailsRecord> emails)
    throws IdPasswordException
  {
    return new IdUser(
      userRecord.getId(),
      new IdName(userRecord.getIdName()),
      new IdRealName(userRecord.getRealName()),
      IdNonEmptyList.ofList(
        emails.stream()
          .map(e -> new IdEmail(e.getEmailAddress()))
          .toList()
      ),
      userRecord.getTimeCreated(),
      userRecord.getTimeUpdated(),
      new IdPassword(
        IdPasswordAlgorithms.parse(userRecord.getPasswordAlgo()),
        userRecord.getPasswordHash().toUpperCase(Locale.ROOT),
        userRecord.getPasswordSalt().toUpperCase(Locale.ROOT),
        Optional.ofNullable(userRecord.getPasswordExpires())
      )
    );
  }

  private static IdDatabaseException handlePasswordException(
    final IdPasswordException exception)
  {
    return new IdDatabaseException(
      exception.getMessage(),
      exception,
      PASSWORD_ERROR,
      exception.attributes(),
      exception.remediatingAction()
    );
  }

  private static IdUserPasswordReset mapPasswordReset(
    final UserPasswordResetsRecord rec)
  {
    return new IdUserPasswordReset(
      rec.getUserId(),
      new IdToken(rec.getToken()),
      rec.getExpires()
    );
  }

  private static IdLogin mapLogin(
    final LoginHistoryRecord r)
  {
    return new IdLogin(
      r.getUserId(),
      r.getTime(),
      r.getHost(),
      r.getAgent()
    );
  }

  @Override
  public IdUser userCreate(
    final UUID id,
    final IdName idName,
    final IdRealName realName,
    final IdEmail email,
    final OffsetDateTime created,
    final IdPassword password)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(realName, "userName");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(created, "created");
    Objects.requireNonNull(password, "password");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var adminId = transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userCreate");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", id.toString()),
        Map.entry("User Name", idName.value()),
        Map.entry("Email", email.value())
      );

    try {
      context.insertInto(USER_IDS)
        .set(USER_IDS.ID, id)
        .execute();

      context.insertInto(USERS)
        .set(USERS.ID, id)
        .set(USERS.ID_NAME, idName.value())
        .set(USERS.REAL_NAME, realName.value())
        .set(USERS.TIME_CREATED, created)
        .set(USERS.TIME_UPDATED, created)
        .set(USERS.PASSWORD_ALGO, password.algorithm().identifier())
        .set(USERS.PASSWORD_HASH, password.hash())
        .set(USERS.PASSWORD_SALT, password.salt())
        .set(USERS.PASSWORD_EXPIRES, password.expires().orElse(null))
        .set(USERS.DELETING, Boolean.FALSE)
        .execute();

      context.insertInto(EMAILS)
        .set(EMAILS.EMAIL_ADDRESS, email.value())
        .set(EMAILS.USER_ID, id)
        .execute();

      final var auditData =
        Map.ofEntries(
          Map.entry("UserID", id.toString())
        );

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "USER_CREATED")
        .set(AUDIT.USER_ID, adminId)
        .set(AU_DATA, Hstore.hstore(auditData))
        .execute();

      return this.userGet(id).orElseThrow();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<IdUser> userGet(
    final UUID id)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userGet");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", id.toString())
      );

    try {
      final var userRecordOpt =
        context.selectFrom(USERS)
          .where(USERS.ID.eq(id))
          .fetchOptional();

      if (userRecordOpt.isEmpty()) {
        return Optional.empty();
      }

      final var userRecord =
        userRecordOpt.get();

      final var emails =
        context.selectFrom(EMAILS)
          .where(EMAILS.USER_ID.eq(userRecord.getId()))
          .fetch();

      return Optional.of(userMap(userRecord, emails));
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } catch (final IdPasswordException e) {
      querySpan.recordException(e);
      throw handlePasswordException(e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public IdUser userGetRequire(
    final UUID id)
    throws IdDatabaseException
  {
    return this.userGet(id)
      .orElseThrow(() -> userDoesNotExist(
        Map.ofEntries(Map.entry("User ID", id.toString()))
      ));
  }

  @Override
  public Optional<IdUser> userGetForName(
    final IdName name)
    throws IdDatabaseException
  {
    Objects.requireNonNull(name, "name");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userGetForName");

    final var attributes =
      Map.ofEntries(
        Map.entry("User Name", name.value())
      );

    try {
      final var userRecordOpt =
        context.selectFrom(USERS)
          .where(USERS.ID_NAME.eq(name.value()))
          .fetchOptional();

      if (userRecordOpt.isEmpty()) {
        return Optional.empty();
      }

      final var userRecord =
        userRecordOpt.get();

      final var emails =
        context.selectFrom(EMAILS)
          .where(EMAILS.USER_ID.eq(userRecord.getId()))
          .fetch();

      return Optional.of(userMap(userRecord, emails));
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } catch (final IdPasswordException e) {
      querySpan.recordException(e);
      throw handlePasswordException(e);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public IdUser userGetForNameRequire(
    final IdName name)
    throws IdDatabaseException
  {
    return this.userGetForName(name)
      .orElseThrow(() -> userDoesNotExist(
        Map.ofEntries(Map.entry("User Name", name.value()))
      ));
  }

  @Override
  public Optional<IdUser> userGetForEmail(
    final IdEmail email)
    throws IdDatabaseException
  {
    Objects.requireNonNull(email, "email");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userGetForEmail");

    final var attributes =
      Map.ofEntries(
        Map.entry("Email", email.value())
      );

    try {
      final var emailOpt =
        context.selectFrom(EMAILS)
          .where(EMAILS.EMAIL_ADDRESS.equalIgnoreCase(email.value()))
          .fetchOptional();

      if (emailOpt.isEmpty()) {
        return Optional.empty();
      }

      final var emailRecord = emailOpt.get();
      if (emailRecord.getUserId() == null) {
        return Optional.empty();
      }

      return this.userGet(emailRecord.getUserId());
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public IdUser userGetForEmailRequire(
    final IdEmail email)
    throws IdDatabaseException
  {
    return this.userGetForEmail(email)
      .orElseThrow(() -> userDoesNotExist(
        Map.ofEntries(Map.entry("Email", email.value()))
      ));
  }

  @Override
  public void userLogin(
    final UUID id,
    final Map<String, String> metadata,
    final int limitHistory)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(metadata, "metadata");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userLogin");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", id.toString())
      );

    try {
      final var limit =
        Math.max(limitHistory, 1);
      final var time =
        this.currentTime();

      context.fetchOptional(USERS, USERS.ID.eq(id))
        .orElseThrow(() -> userDoesNotExist(attributes));

      /*
       * Find the oldest login record.
       */

      final var records =
        context.selectFrom(LOGIN_HISTORY)
          .where(LOGIN_HISTORY.USER_ID.eq(id))
          .orderBy(LOGIN_HISTORY.TIME.desc())
          .limit(Integer.valueOf(limit))
          .fetch();

      /*
       * If the number of records is at the limit, delete any older records.
       */

      if (records.size() == limit) {
        final var last =
          records.get(records.size() - 1);
        final var lastTime =
          last.getTime();
        final var condition =
          LOGIN_HISTORY.USER_ID.eq(id).and(LOGIN_HISTORY.TIME.lt(lastTime));
        context.deleteFrom(LOGIN_HISTORY)
          .where(condition)
          .execute();
      }

      /*
       * Record the login.
       */

      context.insertInto(LOGIN_HISTORY)
        .set(LOGIN_HISTORY.USER_ID, id)
        .set(LOGIN_HISTORY.TIME, this.currentTime())
        .set(LOGIN_HISTORY.AGENT, metadata.getOrDefault(userAgent(), ""))
        .set(LOGIN_HISTORY.HOST, metadata.getOrDefault(remoteHost(), ""))
        .set(
          LOGIN_HISTORY.PROXIED_HOST,
          metadata.getOrDefault(remoteHostProxied(), ""))
        .execute();

      /*
       * The audit event is considered confidential because IP addresses
       * are tentatively considered confidential.
       */

      final var auditData =
        Map.ofEntries(
          Map.entry("Host", formatHosts(metadata))
        );

      final var audit =
        context.insertInto(AUDIT)
          .set(AUDIT.TIME, time)
          .set(AUDIT.TYPE, "USER_LOGGED_IN")
          .set(AUDIT.USER_ID, id)
          .set(AU_DATA, Hstore.hstore(auditData));

      audit.execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  static String formatHosts(
    final Map<String, String> metadata)
  {
    final var host =
      metadata.getOrDefault(remoteHost(), "");
    final var proxied =
      metadata.getOrDefault(remoteHostProxied(), "");

    if (proxied.isEmpty()) {
      return host;
    }
    return "%s (%s)".formatted(host, proxied);
  }

  @Override
  public void userUpdate(
    final UUID id,
    final Optional<IdName> withIdName,
    final Optional<IdRealName> withRealName,
    final Optional<IdPassword> withPassword)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(withIdName, "withIdName");
    Objects.requireNonNull(withRealName, "withRealName");
    Objects.requireNonNull(withPassword, "withPassword");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var owner = transaction.userId();

    this.userUpdateActual(
      id,
      withIdName,
      withRealName,
      withPassword,
      context,
      owner
    );
  }

  @Override
  public void userUpdateAsAdmin(
    final UUID id,
    final Optional<IdName> withIdName,
    final Optional<IdRealName> withRealName,
    final Optional<IdPassword> withPassword)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(withIdName, "withIdName");
    Objects.requireNonNull(withRealName, "withRealName");
    Objects.requireNonNull(withPassword, "withPassword");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var owner = transaction.adminId();

    this.userUpdateActual(
      id,
      withIdName,
      withRealName,
      withPassword,
      context,
      owner
    );
  }

  private void userUpdateActual(
    final UUID id,
    final Optional<IdName> withIdName,
    final Optional<IdRealName> withRealName,
    final Optional<IdPassword> withPassword,
    final DSLContext context,
    final UUID owner)
    throws IdDatabaseException
  {
    final var transaction =
      this.transaction();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userUpdate");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", id.toString())
      );

    try {
      final var record = context.fetchOne(USERS, USERS.ID.eq(id));
      if (record == null) {
        throw userDoesNotExist(attributes);
      }

      if (withIdName.isPresent()) {
        final var name = withIdName.get();
        record.setIdName(name.value());

        final var auditData =
          Map.ofEntries(
            Map.entry("UserID", id.toString()),
            Map.entry("IdName", name.value())
          );

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_CHANGED_ID_NAME")
          .set(AUDIT.USER_ID, owner)
          .set(AU_DATA, Hstore.hstore(auditData))
          .execute();
      }

      if (withRealName.isPresent()) {
        final var name = withRealName.get();
        record.setRealName(name.value());

        final var auditData =
          Map.ofEntries(
            Map.entry("UserID", id.toString()),
            Map.entry("RealName", name.value())
          );

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_CHANGED_REAL_NAME")
          .set(AUDIT.USER_ID, owner)
          .set(AU_DATA, Hstore.hstore(auditData))
          .execute();
      }

      if (withPassword.isPresent()) {
        final var pass = withPassword.get();
        record.setPasswordAlgo(pass.algorithm().identifier());
        record.setPasswordHash(pass.hash());
        record.setPasswordSalt(pass.salt());
        record.setPasswordExpires(pass.expires().orElse(null));

        final var auditData =
          Map.ofEntries(
            Map.entry("UserID", id.toString())
          );

        context.insertInto(AUDIT)
          .set(AUDIT.TIME, this.currentTime())
          .set(AUDIT.TYPE, "USER_CHANGED_PASSWORD")
          .set(AUDIT.USER_ID, owner)
          .set(AU_DATA, Hstore.hstore(auditData))
          .execute();
      }

      record.store();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void userEmailAdd(
    final UUID id,
    final IdEmail email)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(email, "email");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var executor =
      transaction.executorId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userEmailAdd");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", id.toString()),
        Map.entry("Email", email.value())
      );

    try {
      context.insertInto(EMAILS)
        .set(EMAILS.USER_ID, id)
        .set(EMAILS.EMAIL_ADDRESS, email.value())
        .execute();

      final var auditData =
        Map.ofEntries(
          Map.entry("Email", email.value())
        );

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "USER_EMAIL_ADDED")
        .set(AUDIT.USER_ID, executor)
        .set(AU_DATA, Hstore.hstore(auditData))
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void userEmailRemove(
    final UUID id,
    final IdEmail email)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(email, "email");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var executor =
      transaction.executorId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userEmailAdd");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", id.toString()),
        Map.entry("Email", email.value())
      );

    try {
      context.fetchOptional(USERS, USERS.ID.eq(id))
        .orElseThrow(() -> userDoesNotExist(attributes));

      final var existing =
        context.fetchOptional(
          EMAILS,
          EMAILS.USER_ID.eq(id)
            .and(EMAILS.EMAIL_ADDRESS.equalIgnoreCase(email.value()))
        );

      if (existing.isEmpty()) {
        return;
      }

      /*
       * There is a database trigger that prevents the last email address
       * being removed from the account, so we don't perform any check here.
       */

      context.deleteFrom(EMAILS)
        .where(EMAILS.USER_ID.eq(id)
                 .and(EMAILS.EMAIL_ADDRESS.equalIgnoreCase(email.value())))
        .execute();

      final var auditData =
        Map.ofEntries(
          Map.entry("Email", email.value())
        );

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "USER_EMAIL_REMOVED")
        .set(AUDIT.USER_ID, executor)
        .set(AU_DATA, Hstore.hstore(auditData))
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public List<IdLogin> userLoginHistory(
    final UUID id,
    final int limit)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userLoginHistory");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", id.toString())
      );

    try {
      return context.selectFrom(LOGIN_HISTORY)
        .where(LOGIN_HISTORY.USER_ID.eq(id))
        .limit(Integer.valueOf(limit))
        .stream()
        .map(IdDatabaseUsersQueries::mapLogin)
        .toList();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void userDelete(final UUID id)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var executor =
      transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userDelete");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", id.toString())
      );

    try {
      final var user = this.userGetRequire(id);

      context.update(USERS)
        .set(USERS.DELETING, TRUE)
        .where(USERS.ID.eq(id))
        .execute();

      for (final var email : user.emails()) {
        this.userEmailRemove(id, email);
      }

      context.deleteFrom(EMAIL_VERIFICATIONS)
        .where(EMAIL_VERIFICATIONS.USER_ID.eq(id))
        .execute();

      context.deleteFrom(USERS)
        .where(USERS.ID.eq(id))
        .execute();

      final var auditData =
        Map.ofEntries(
          Map.entry("UserID", user.id().toString())
        );

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "USER_DELETED")
        .set(AUDIT.USER_ID, executor)
        .set(AU_DATA, Hstore.hstore(auditData))
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void userBanCreate(
    final IdBan ban)
    throws IdDatabaseException
  {
    Objects.requireNonNull(ban, "ban");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var executor =
      transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userBanCreate");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", ban.user().toString()),
        Map.entry("Reason", ban.reason())
      );

    try {
      final var user =
        this.userGetRequire(ban.user());
      var banRecord =
        context.fetchOne(BANS, BANS.USER_ID.eq(user.id()));

      if (banRecord == null) {
        banRecord = context.newRecord(BANS);
      }

      banRecord.set(BANS.USER_ID, user.id());
      banRecord.set(BANS.EXPIRES, ban.expires().orElse(null));
      banRecord.set(BANS.REASON, ban.reason());
      banRecord.store();

      final var auditData =
        Map.ofEntries(
          Map.entry("UserID", user.id().toString())
        );

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "USER_BANNED")
        .set(AUDIT.USER_ID, executor)
        .set(AU_DATA, Hstore.hstore(auditData))
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<IdBan> userBanGet(
    final UUID id)
    throws IdDatabaseException
  {
    Objects.requireNonNull(id, "id");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userBanGet");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", id.toString())
      );

    try {
      final var user =
        this.userGetRequire(id);
      final var banRecord =
        context.fetchOne(BANS, BANS.USER_ID.eq(user.id()));

      if (banRecord == null) {
        return Optional.empty();
      }

      return Optional.of(
        new IdBan(
          banRecord.getUserId(),
          banRecord.getReason(),
          Optional.ofNullable(banRecord.getExpires())
        )
      );
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void userBanDelete(
    final IdBan ban)
    throws IdDatabaseException
  {
    Objects.requireNonNull(ban, "ban");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var executor =
      transaction.adminId();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userBanDelete");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", ban.user().toString()),
        Map.entry("Reason", ban.reason())
      );

    try {
      final var user =
        this.userGetRequire(ban.user());
      final var banRecord =
        context.fetchOne(BANS, BANS.USER_ID.eq(user.id()));

      if (banRecord == null) {
        return;
      }

      banRecord.delete();

      final var auditData =
        Map.ofEntries(
          Map.entry("UserID", user.id().toString())
        );

      context.insertInto(AUDIT)
        .set(AUDIT.TIME, this.currentTime())
        .set(AUDIT.TYPE, "USER_BAN_REMOVED")
        .set(AUDIT.USER_ID, executor)
        .set(AU_DATA, Hstore.hstore(auditData))
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void userPasswordResetCreate(
    final IdUserPasswordReset reset)
    throws IdDatabaseException
  {
    Objects.requireNonNull(reset, "reset");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseUsersQueries.userPasswordResetCreate");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", reset.user().toString())
      );

    try {
      final var user =
        this.userGetRequire(reset.user());

      context.insertInto(USER_PASSWORD_RESETS)
        .set(USER_PASSWORD_RESETS.USER_ID, user.id())
        .set(USER_PASSWORD_RESETS.EXPIRES, reset.expires())
        .set(USER_PASSWORD_RESETS.TOKEN, reset.token().value())
        .execute();

      final var auditData =
        Map.ofEntries(
          Map.entry("Token", reset.token().value()),
          Map.entry("Expires", reset.expires().toString())
        );

      context.insertInto(AUDIT)
        .set(AUDIT.USER_ID, user.id())
        .set(AU_DATA, Hstore.hstore(auditData))
        .set(AUDIT.TYPE, "USER_PASSWORD_RESET_CREATED")
        .set(AUDIT.TIME, this.currentTime())
        .execute();

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public List<IdUserPasswordReset> userPasswordResetGet(
    final UUID userId)
    throws IdDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan("IdDatabaseUsersQueries.userPasswordResetGet");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", userId.toString())
      );

    try {
      this.userGetRequire(userId);

      return context.selectFrom(USER_PASSWORD_RESETS)
        .where(USER_PASSWORD_RESETS.USER_ID.eq(userId))
        .stream()
        .map(IdDatabaseUsersQueries::mapPasswordReset)
        .toList();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public Optional<IdUserPasswordReset> userPasswordResetGetForToken(
    final IdToken token)
    throws IdDatabaseException
  {
    Objects.requireNonNull(token, "token");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseUsersQueries.userPasswordResetGetForToken");

    final var attributes =
      Map.ofEntries(
        Map.entry("Token", token.value())
      );

    try {
      return context.selectFrom(USER_PASSWORD_RESETS)
        .where(USER_PASSWORD_RESETS.TOKEN.eq(token.value()))
        .stream()
        .map(IdDatabaseUsersQueries::mapPasswordReset)
        .findFirst();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  @Override
  public void userPasswordResetDelete(
    final IdUserPasswordReset reset)
    throws IdDatabaseException
  {
    Objects.requireNonNull(reset, "reset");

    final var transaction =
      this.transaction();
    final var context =
      transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseUsersQueries.userPasswordResetDelete");

    final var attributes =
      Map.ofEntries(
        Map.entry("User ID", reset.user().toString()),
        Map.entry("Token", reset.token().value())
      );

    try {
      context.deleteFrom(USER_PASSWORD_RESETS)
        .where(USER_PASSWORD_RESETS.USER_ID.eq(reset.user())
                 .and(USER_PASSWORD_RESETS.TOKEN.eq(reset.token().value())))
        .execute();
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(transaction, e, attributes);
    } finally {
      querySpan.end();
    }
  }

  private static JQField orderingToJQField(
    final IdUserColumnOrdering ordering)
  {
    final var field =
      switch (ordering.column()) {
        case BY_ID -> USERS.ID;
        case BY_IDNAME -> USERS.ID_NAME;
        case BY_REALNAME -> USERS.REAL_NAME;
        case BY_TIME_CREATED -> USERS.TIME_CREATED;
        case BY_TIME_UPDATED -> USERS.TIME_UPDATED;
      };

    return new JQField(
      field,
      ordering.ascending() ? JQOrder.ASCENDING : JQOrder.DESCENDING
    );
  }

  @Override
  public IdDatabaseUserSearchType userSearch(
    final IdUserSearchParameters parameters)
    throws IdDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseUsersQueries.userSearch.create");

    try {

      /*
       * The users must lie within the given time ranges.
       */

      final var timeCreatedRange = parameters.timeCreatedRange();
      final var timeCreatedCondition =
        DSL.condition(
          USERS.TIME_CREATED.ge(timeCreatedRange.timeLower())
            .and(USERS.TIME_CREATED.le(timeCreatedRange.timeUpper()))
        );

      final var timeUpdatedRange = parameters.timeUpdatedRange();
      final var timeUpdatedCondition =
        DSL.condition(
          USERS.TIME_UPDATED.ge(timeUpdatedRange.timeLower())
            .and(USERS.TIME_UPDATED.le(timeUpdatedRange.timeUpper()))
        );

      /*
       * A search query might be present.
       */

      final Condition searchCondition;
      final var search = parameters.search();
      if (search.isPresent()) {
        final var searchText = "%%%s%%".formatted(search.get());
        searchCondition =
          DSL.condition(USERS.ID_NAME.likeIgnoreCase(searchText))
            .or(DSL.condition(USERS.REAL_NAME.likeIgnoreCase(searchText)))
            .or(DSL.condition(USERS.ID.likeIgnoreCase(searchText)));
      } else {
        searchCondition = DSL.trueCondition();
      }

      final var allConditions =
        timeCreatedCondition
          .and(timeUpdatedCondition)
          .and(searchCondition);

      final var orderField =
        orderingToJQField(parameters.ordering());

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          JQKeysetRandomAccessPaginationParameters.forTable(USERS)
            .addSortField(orderField)
            .setPageSize(Integer.toUnsignedLong(parameters.limit()))
            .addWhereCondition(allConditions)
            .setStatementListener(statement -> {
              querySpan.setAttribute(DB_STATEMENT, statement.toString());
            })
            .build()
        );

      return new IdDatabaseUsersQueries.UsersSearch(pages);
    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, Map.of());
    } finally {
      querySpan.end();
    }
  }

  @Override
  public IdDatabaseUserSearchByEmailType userSearchByEmail(
    final IdUserSearchByEmailParameters parameters)
    throws IdDatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var transaction = this.transaction();
    final var context = transaction.createContext();
    final var querySpan =
      transaction.createQuerySpan(
        "IdDatabaseUsersQueries.userSearchByEmail.create");

    try {
      final var baseTable =
        USERS.join(EMAILS)
          .on(USERS.ID.eq(EMAILS.USER_ID));

      /*
       * The users must lie within the given time ranges.
       */

      final var timeCreatedRange = parameters.timeCreatedRange();
      final var timeCreatedCondition =
        DSL.condition(
          USERS.TIME_CREATED.ge(timeCreatedRange.timeLower())
            .and(USERS.TIME_CREATED.le(timeCreatedRange.timeUpper()))
        );

      final var timeUpdatedRange = parameters.timeUpdatedRange();
      final var timeUpdatedCondition =
        DSL.condition(
          USERS.TIME_UPDATED.ge(timeUpdatedRange.timeLower())
            .and(USERS.TIME_UPDATED.le(timeUpdatedRange.timeUpper()))
        );

      /*
       * Only users with matching email addresses will be returned.
       */

      final var searchLike =
        "%%%s%%".formatted(parameters.search());
      final var searchCondition =
        DSL.condition(EMAILS.EMAIL_ADDRESS.likeIgnoreCase(searchLike));

      final var allConditions =
        timeCreatedCondition
          .and(timeUpdatedCondition)
          .and(searchCondition);

      final var orderField =
        orderingToJQField(parameters.ordering());

      final var pages =
        JQKeysetRandomAccessPagination.createPageDefinitions(
          context,
          JQKeysetRandomAccessPaginationParameters.forTable(baseTable)
            .addSortField(orderField)
            .addWhereCondition(allConditions)
            .addGroupByField(USERS.ID)
            .setPageSize(Integer.toUnsignedLong(parameters.limit()))
            .setStatementListener(statement -> {
              querySpan.setAttribute(DB_STATEMENT, statement.toString());
            })
            .build()
        );

      return new IdDatabaseUsersQueries.UsersByEmailSearch(
        pages
      );

    } catch (final DataAccessException e) {
      querySpan.recordException(e);
      throw handleDatabaseException(this.transaction(), e, Map.of());
    } finally {
      querySpan.end();
    }
  }

  private static final class UsersByEmailSearch
    extends IdAbstractSearch<
    IdDatabaseUsersQueries,
    IdDatabaseUsersQueriesType,
    IdUserSummary>
    implements IdDatabaseUserSearchByEmailType
  {
    UsersByEmailSearch(
      final List<JQKeysetRandomAccessPageDefinition> inPages)
    {
      super(inPages);
    }

    @Override
    protected IdPage<IdUserSummary> page(
      final IdDatabaseUsersQueries queries,
      final JQKeysetRandomAccessPageDefinition page)
      throws IdDatabaseException
    {
      final var transaction =
        queries.transaction();
      final var context =
        transaction.createContext();

      final var querySpan =
        transaction.createQuerySpan(
          "IdDatabaseUsersQueries.userSearchByEmail.page");

      try {
        final var query =
          page.queryFields(context, List.of(
            USERS.ID,
            USERS.ID_NAME,
            USERS.REAL_NAME,
            USERS.TIME_CREATED,
            USERS.TIME_UPDATED
          ));

        querySpan.setAttribute(DB_STATEMENT, query.toString());

        final var items =
          query.fetch().map(record -> {
            return new IdUserSummary(
              record.get(USERS.ID),
              new IdName(record.get(USERS.ID_NAME)),
              new IdRealName(record.get(USERS.REAL_NAME)),
              record.get(USERS.TIME_CREATED),
              record.get(USERS.TIME_UPDATED)
            );
          });

        return new IdPage<>(
          items,
          (int) page.index(),
          this.pageCount(),
          page.firstOffset()
        );
      } catch (final DataAccessException e) {
        querySpan.recordException(e);
        throw handleDatabaseException(transaction, e, Map.of());
      } finally {
        querySpan.end();
      }
    }
  }

  private static final class UsersSearch
    extends IdAbstractSearch<
    IdDatabaseUsersQueries,
    IdDatabaseUsersQueriesType,
    IdUserSummary>
    implements IdDatabaseUserSearchType
  {
    UsersSearch(
      final List<JQKeysetRandomAccessPageDefinition> inPages)
    {
      super(inPages);
    }

    @Override
    protected IdPage<IdUserSummary> page(
      final IdDatabaseUsersQueries queries,
      final JQKeysetRandomAccessPageDefinition page)
      throws IdDatabaseException
    {
      final var transaction =
        queries.transaction();
      final var context =
        transaction.createContext();

      final var querySpan =
        transaction.createQuerySpan(
          "IdDatabaseUsersQueries.userSearch.page");

      try {
        final var query =
          page.queryFields(context, List.of(
            USERS.ID,
            USERS.ID_NAME,
            USERS.REAL_NAME,
            USERS.TIME_CREATED,
            USERS.TIME_UPDATED
          ));

        querySpan.setAttribute(DB_STATEMENT, query.toString());

        final var items =
          query.fetch().map(record -> {
            return new IdUserSummary(
              record.get(USERS.ID),
              new IdName(record.get(USERS.ID_NAME)),
              new IdRealName(record.get(USERS.REAL_NAME)),
              record.get(USERS.TIME_CREATED),
              record.get(USERS.TIME_UPDATED)
            );
          });

        return new IdPage<>(
          items,
          (int) page.index(),
          this.pageCount(),
          page.firstOffset()
        );
      } catch (final DataAccessException e) {
        querySpan.recordException(e);
        throw handleDatabaseException(transaction, e, Map.of());
      } finally {
        querySpan.end();
      }
    }
  }
}
