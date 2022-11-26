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


package com.io7m.idstore.tests.database;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_EMAIL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_DUPLICATE_ID_NAME;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NOT_INITIAL;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_UNSET;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_ONE_REQUIRED;
import static com.io7m.idstore.model.IdAdminColumn.BY_IDNAME;
import static com.io7m.idstore.model.IdLoginMetadataStandard.remoteHost;
import static com.io7m.idstore.model.IdLoginMetadataStandard.userAgent;
import static java.time.OffsetDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
public final class IdDatabaseAdminsTest extends IdWithDatabaseContract
{
  /**
   * Setting the transaction admin to a nonexistent admin fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSetNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        transaction.adminIdSet(randomUUID());
      });
    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  /**
   * Creating an admin works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdmin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    admin = admins.adminGet(reqId).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    admin = admins.adminGetForEmail(new IdEmail("someone@example.com")).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    admin = admins.adminGetForName(new IdName("someone")).orElseThrow();
    assertEquals("someone", admin.idName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("ADMIN_CREATED", reqId.toString())
    );
  }

  /**
   * Trying to create an initial admin when one already exists, fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminNotInitial()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreateInitial(
          reqId,
          new IdName("someone"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone@example.com"),
          now,
          password
        );
      });

    assertEquals(ADMIN_NOT_INITIAL, ex.errorCode());
  }

  /**
   * Creating an admin requires an admin.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminRequiresAdmin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      createBadPassword();

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          reqId,
          new IdName("someone2"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone2@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_UNSET, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate ID fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateId()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          reqId,
          new IdName("someone2"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone2@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_ID, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate email fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateEmail()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          randomUUID(),
          new IdName("someone2"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_EMAIL, ex.errorCode());
  }

  /**
   * Creating an admin with a duplicate name fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDuplicateName()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var reqId =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminCreate(
          randomUUID(),
          new IdName("someone"),
          new IdRealName("Someone R. Admin"),
          new IdEmail("someone2@example.com"),
          now,
          password,
          Set.of()
        );
      });

    assertEquals(ADMIN_DUPLICATE_ID_NAME, ex.errorCode());
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminLogin()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);
    final var adminId =
      this.databaseCreateAdminInitial("admin", "12345678");

    final var id =
      randomUUID();
    final var now =
      now();

    final var password =
      createBadPassword();

    transaction.adminIdSet(adminId);

    final var admin =
      admins.adminCreate(
        id,
        new IdName("someone"),
        new IdRealName("Someone R. Admin"),
        new IdEmail("someone@example.com"),
        now,
        password,
        Set.of()
      );

    admins.adminLogin(
      admin.id(),
      Map.ofEntries(
        Map.entry(remoteHost(), "127.0.0.1"),
        Map.entry(userAgent(), "Mozilla/5.0 (X11; Linux x86_64)")
      )
    );

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("ADMIN_CREATED", id.toString()),
      new ExpectedEvent("ADMIN_LOGGED_IN", "127.0.0.1")
    );
  }

  /**
   * Logging in fails for nonexistent admins.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminLoginNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var transaction =
      this.transactionOf(IDSTORE);
    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminLogin(
          randomUUID(),
          Map.ofEntries(
            Map.entry(remoteHost(), "127.0.0.1"),
            Map.entry(userAgent(), "Mozilla/5.0 (X11; Linux x86_64)")
          )
        );
      });
    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  private static IdPassword createBadPassword()
    throws IdPasswordException
  {
    return IdPasswordAlgorithmPBKDF2HmacSHA256.create()
      .createHashed("12345678");
  }

  private static void checkPage(
    final int indexLow,
    final int indexHigh,
    final List<IdAdminSummary> items)
  {
    var index = indexLow;
    final var iter = items.iterator();
    while (index < indexHigh) {
      final var item = iter.next();
      final var name =
        new IdName("someone_%03d".formatted(index));
      final var realName =
        new IdRealName("someone %03d".formatted(index));
      final var email =
        new IdEmail("someone_%03d@example.com".formatted(index));

      assertEquals(name, item.idName());
      assertEquals(realName, item.realName());
      ++index;
    }
  }

  /**
   * Updating an admin works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminUpdate()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password,
        EnumSet.allOf(IdAdminPermission.class)
      );

    assertEquals("someone", admin.realName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());
    assertEquals(password, admin.password());

    transaction.adminIdSet(admin.id());

    final var otherPassword = databaseGenerateDifferentBadPassword();
    admins.adminUpdate(
      admin.id(),
      Optional.of(new IdName("newIdName")),
      Optional.of(new IdRealName("newRealName")),
      Optional.of(otherPassword),
      Optional.of(EnumSet.noneOf(IdAdminPermission.class))
    );

    admin = admins.adminGetRequire(admin.id());

    assertEquals("newRealName", admin.realName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals("newIdName", admin.idName().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());
    assertEquals(otherPassword, admin.password());
    assertEquals(IdAdminPermissionSet.empty(), admin.permissions());

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("ADMIN_CREATED", admin.id().toString())
    );
  }

  /**
   * Updating an admin fails for nonexistent admins.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminUpdateNonexistent()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password,
        EnumSet.allOf(IdAdminPermission.class)
      );

    assertEquals("someone", admin.realName().value());
    assertEquals(reqId, admin.id());
    assertEquals("someone@example.com", admin.emails().first().value());
    assertEquals(now.toEpochSecond(), admin.timeCreated().toEpochSecond());
    assertEquals(now.toEpochSecond(), admin.timeUpdated().toEpochSecond());
    assertEquals(password, admin.password());

    transaction.adminIdSet(admin.id());

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        final var otherPassword = databaseGenerateDifferentBadPassword();
        admins.adminUpdate(
          randomUUID(),
          Optional.of(new IdName("newIdName")),
          Optional.of(new IdRealName("newRealName")),
          Optional.of(otherPassword),
          Optional.of(EnumSet.noneOf(IdAdminPermission.class))
        );
      });

    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
  }

  /**
   * Adding and removing email addresses works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminEmailAddresses()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password,
        EnumSet.allOf(IdAdminPermission.class)
      );

    admins.adminEmailAdd(reqId, new IdEmail("someone2@example.com"));
    admin = admins.adminGetRequire(reqId);

    assertTrue(
      admin.emails().contains(new IdEmail("someone@example.com")));
    assertTrue(
      admin.emails().contains(new IdEmail("someone2@example.com")));
    assertEquals(2, admin.emails().toList().size());

    admins.adminEmailRemove(reqId, new IdEmail("someone@example.com"));
    admin = admins.adminGetRequire(reqId);
    assertEquals(1, admin.emails().toList().size());

    final var ex =
      assertThrows(IdDatabaseException.class, () -> {
        admins.adminEmailRemove(reqId, new IdEmail("someone2@example.com"));
      });
    assertEquals(EMAIL_ONE_REQUIRED, ex.errorCode());
  }

  /**
   * Deleting an admin works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDelete()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var reqId =
      randomUUID();
    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    final var admin =
      admins.adminCreate(
        reqId,
        new IdName("someone"),
        new IdRealName("someone"),
        new IdEmail("someone@example.com"),
        now,
        password,
        EnumSet.allOf(IdAdminPermission.class)
      );

    admins.adminDelete(reqId);

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          admins.adminGetRequire(reqId);
        });

      assertEquals(ADMIN_NONEXISTENT, ex.errorCode());
    }

    {
      final var ex =
        assertThrows(IdDatabaseException.class, () -> {
          admins.adminCreate(
            reqId,
            new IdName("someone"),
            new IdRealName("someone"),
            new IdEmail("someone@example.com"),
            now,
            password,
            EnumSet.allOf(IdAdminPermission.class)
          );
        });

      assertEquals(ADMIN_DUPLICATE_ID, ex.errorCode());
    }

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("ADMIN_CREATED", admin.id().toString()),
      new ExpectedEvent("ADMIN_EMAIL_REMOVED", reqId + "|someone@example.com"),
      new ExpectedEvent("ADMIN_DELETED", admin.id().toString())
    );
  }

  /**
   * Bans work.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminBan()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var now =
      timeNow();

    transaction.adminIdSet(adminId);

    final var ban = new IdBan(adminId, "No reason.", Optional.of(now));
    admins.adminBanCreate(ban);
    assertEquals(Optional.of(ban), admins.adminBanGet(adminId));
    admins.adminBanDelete(ban);
    assertEquals(Optional.empty(), admins.adminBanGet(adminId));

    checkAuditLog(
      transaction,
      new ExpectedEvent("ADMIN_CREATED", adminId.toString()),
      new ExpectedEvent("ADMIN_BANNED", adminId.toString()),
      new ExpectedEvent("ADMIN_BAN_REMOVED", adminId.toString())
    );
  }

  /**
   * Admins can be listed and paging works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSearchByEmailPaged()
    throws Exception
  {
    assertTrue(this.containerIsRunning());

    final var adminId =
      this.databaseCreateAdminInitial(
        "admin",
        "12345678"
      );

    final var transaction =
      this.transactionOf(IDSTORE);

    transaction.adminIdSet(adminId);

    final var admins =
      transaction.queries(IdDatabaseAdminsQueriesType.class);

    final var now =
      now();
    final var password =
      databaseGenerateBadPassword();

    final var adminList = new ArrayList<>();
    for (int index = 0; index < 500; ++index) {
      adminList.add(
        admins.adminCreate(
          randomUUID(),
          new IdName("someone_%03d".formatted(index)),
          new IdRealName("someone %03d".formatted(index)),
          new IdEmail("someone_%03d@example.com".formatted(index)),
          now,
          password,
          EnumSet.allOf(IdAdminPermission.class)
        )
      );
    }

    final var parameters =
      new IdAdminSearchByEmailParameters(
        new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
        new IdTimeRange(now.minusDays(1L), now.plusDays(1L)),
        "0@example.com",
        new IdAdminColumnOrdering(BY_IDNAME, true),
        150
      );

    final var paging =
      admins.adminSearchByEmail(parameters);

    {
      final IdPage<IdAdminSummary> page = paging.pageCurrent(admins);
      assertEquals(1, page.pageIndex());
      assertEquals(1, page.pageCount());
      assertTrue(page.items().size() >= 50);
      assertTrue(page.items().size() <= 52);

      for (final var item : page.items()) {
        final var name = item.idName().value();
        if (Objects.equals(name, "admin")) {
          continue;
        }
        assertTrue(
          name.endsWith("0"),
          "Name %s must end with 0".formatted(name)
        );
      }
    }
  }
}