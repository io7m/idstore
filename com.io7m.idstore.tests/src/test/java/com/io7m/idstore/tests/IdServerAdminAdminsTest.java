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

package com.io7m.idstore.tests;

import com.io7m.idstore.admin_client.IdAClients;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientType;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPage;
import com.io7m.idstore.model.IdPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminOrdering;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.model.IdAdminColumn.BY_IDNAME;
import static com.io7m.idstore.model.IdAdminPermission.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerAdminAdminsTest extends IdWithServerContract
{
  private static final IdTimeRange TIME_LARGE_RANGE =
    new IdTimeRange(
      OffsetDateTime.now().minusDays(30L),
      OffsetDateTime.now().plusDays(30L)
    );

  private static final IdAdminOrdering ORDER_BY_IDNAME =
    new IdAdminOrdering(
      List.of(new IdAdminColumnOrdering(BY_IDNAME, true))
    );

  private IdAClients clients;
  private IdAClientType client;

  @BeforeEach
  public void setup()
  {
    this.clients = new IdAClients();
    this.client = this.clients.create(Locale.getDefault());
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.client.close();
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginSelf()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.login("admin", "12345678", this.serverAdminAPIURL());

    final var self = this.client.adminSelf();
    assertEquals("admin", self.realName().value());
  }

  /**
   * Logging in fails with the wrong adminname.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsAdminname()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    final var ex =
      Assertions.assertThrows(IdAClientException.class, () -> {
        this.client.login("admin1", "12345678", this.serverAdminAPIURL());
      });

    assertTrue(
      ex.getMessage().contains("error-authentication"),
      ex.getMessage());
  }

  /**
   * Logging in fails with the wrong password.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsPassword()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    final var ex =
      Assertions.assertThrows(IdAClientException.class, () -> {
        this.client.login("admin", "123456789", this.serverAdminAPIURL());
      });

    assertTrue(
      ex.getMessage().contains("error-authentication"),
      ex.getMessage());
  }

  /**
   * Searching admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSearch()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    for (int index = 0; index < 1033; ++index) {
      this.serverCreateAdmin(admin, "admin-%04d".formatted(index));
    }

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    {
      final var p =
        this.client.adminSearchBegin(
          new IdAdminSearchParameters(
            TIME_LARGE_RANGE,
            TIME_LARGE_RANGE,
            Optional.of("admin-"),
            ORDER_BY_IDNAME,
            100
          )
        );

      assertEquals(0, p.pageIndex());
      assertEquals(0, p.pageFirstOffset());
      assertEquals(10, p.pageCount());
      checkItems(p, 0, 100);
    }

    for (int page = 1; page < 10; ++page) {
      final var p = this.client.adminSearchNext();
      assertEquals(page, p.pageIndex());
      assertEquals(page * 100, p.pageFirstOffset());
      assertEquals(10, p.pageCount());
      checkItems(p, page * 100, 100);
    }

    {
      final var p = this.client.adminSearchNext();
      assertEquals(10, p.pageIndex());
      assertEquals(1000, p.pageFirstOffset());
      assertEquals(10, p.pageCount());
      checkItems(p, 1000, 33);
    }

    for (int page = 9; page >= 0; --page) {
      final var p = this.client.adminSearchPrevious();
      assertEquals(page, p.pageIndex());
      assertEquals(page * 100, p.pageFirstOffset());
      assertEquals(10, p.pageCount());
      checkItems(p, page * 100, 100);
    }
  }

  /**
   * Searching admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminSearchByEmail()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    for (int index = 0; index < 50; ++index) {
      this.serverCreateAdmin(admin, "admin-%04d".formatted(index));
    }

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    {
      final var p =
        this.client.adminSearchByEmailBegin(
          new IdAdminSearchByEmailParameters(
            TIME_LARGE_RANGE,
            TIME_LARGE_RANGE,
            "extra_",
            ORDER_BY_IDNAME,
            10
          )
        );

      assertEquals(0, p.pageIndex());
      assertEquals(0, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 0, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(1, p.pageIndex());
      assertEquals(10, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 10, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(2, p.pageIndex());
      assertEquals(20, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 20, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(3, p.pageIndex());
      assertEquals(30, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 30, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(4, p.pageIndex());
      assertEquals(40, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 40, 10);
    }

    {
      final var p = this.client.adminSearchByEmailNext();
      assertEquals(5, p.pageIndex());
      assertEquals(50, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(0, p.items().size());
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(4, p.pageIndex());
      assertEquals(40, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 40, 10);
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(3, p.pageIndex());
      assertEquals(30, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 30, 10);
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(2, p.pageIndex());
      assertEquals(20, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 20, 10);
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(1, p.pageIndex());
      assertEquals(10, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 10, 10);
    }

    {
      final var p = this.client.adminSearchByEmailPrevious();
      assertEquals(0, p.pageIndex());
      assertEquals(0, p.pageFirstOffset());
      assertEquals(5, p.pageCount());
      assertEquals(10, p.items().size());
      checkItems(p, 0, 10);
    }
  }

  /**
   * Creating, updating, and retrieving admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminUpdate()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    final var id =
      UUID.randomUUID();
    final var password0 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("12345678");
    final var password1 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("abcdefgh");

    final var admin0 =
      this.client.adminCreate(
        Optional.of(id),
        new IdName("someone-0"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-0@example.com"),
        password0,
        EnumSet.allOf(IdAdminPermission.class)
      );

    final var admin0r =
      new IdAdmin(
        id,
        new IdName("someone-1"),
        new IdRealName("Someone R. Else"),
        IdNonEmptyList.single(new IdEmail("someone-1@example.com")),
        OffsetDateTime.now(this.clock()),
        OffsetDateTime.now(this.clock()),
        password1,
        EnumSet.noneOf(IdAdminPermission.class)
      );

    final var admin1 =
      this.client.adminUpdate(admin0r);

    assertEquals(admin0r.id(), admin1.id());
    assertEquals(admin0r.emails(), admin1.emails());
    assertEquals(admin0r.idName(), admin1.idName());
    assertEquals(admin0r.realName(), admin1.realName());
    assertEquals(admin0r.password(), admin1.password());
  }

  /**
   * Trying to grant permissions to an admin without holding those permissions
   * fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminUpdateUnprivileged()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    final var password0 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("12345678");

    final var admin0 =
      this.client.adminCreate(
        Optional.of(UUID.randomUUID()),
        new IdName("someone-0"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-0@example.com"),
        password0,
        EnumSet.of(ADMIN_CREATE, ADMIN_WRITE, ADMIN_READ)
      );

    final var admin1 =
      this.client.adminCreate(
        Optional.of(UUID.randomUUID()),
        new IdName("someone-1"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-1@example.com"),
        password0,
        EnumSet.noneOf(IdAdminPermission.class)
      );

    this.client.login(
      "someone-0",
      "12345678",
      this.serverAdminAPIURL()
    );

    var ex =
      Assertions.assertThrows(IdAClientException.class, () -> {
        this.client.adminUpdate(
          new IdAdmin(
            admin1.id(),
            admin1.idName(),
            admin1.realName(),
            admin1.emails(),
            admin1.timeCreated(),
            admin1.timeUpdated(),
            admin1.password(),
            EnumSet.allOf(IdAdminPermission.class)
          )
        );
      });

    final var msg = ex.getMessage();
    assertTrue(msg.contains("The current admin cannot grant the following permissions"));
    assertTrue(msg.contains("USER_READ"));
    assertTrue(msg.contains("USER_WRITE"));
    assertTrue(msg.contains("ADMIN_DELETE"));
  }

  /**
   * Deleting admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAdminDeleting()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.client.login(
      "admin",
      "12345678",
      this.serverAdminAPIURL()
    );

    final var id =
      UUID.randomUUID();
    final var password0 =
      IdPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("12345678");

    final var admin0 =
      this.client.adminCreate(
        Optional.of(id),
        new IdName("someone-0"),
        new IdRealName("Someone R. Incognito"),
        new IdEmail("someone-0@example.com"),
        password0,
        EnumSet.noneOf(IdAdminPermission.class)
      );

    this.client.adminDelete(id);

    final var ex =
      Assertions.assertThrows(IdAClientException.class, () -> {
        this.client.adminUpdate(admin0);
      });

    assertTrue(
      ex.getMessage().contains("error-admin-nonexistent"),
      ex.getMessage());
  }

  private static void checkItems(
    final IdPage<IdAdminSummary> p,
    final int start,
    final int count)
  {
    final var u = p.items();
    for (int index = 0; index < count; ++index) {
      assertEquals(
        "admin-%04d".formatted(Integer.valueOf(start + index)),
        u.get(index).idName().value()
      );
    }
  }
}
