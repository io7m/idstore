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

package com.io7m.idstore.tests.server.controller.admin;

import com.io7m.idstore.database.api.IdDatabaseAdminsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdBan;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanCreate;
import com.io7m.idstore.server.controller.admin.IdACmdAdminBanCreate;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.ADMIN_NONEXISTENT;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_BAN;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public final class IdACmdAdminBanCreateTest
  extends IdACmdAbstractContract
{
  /**
   * Banning admins requires the ADMIN_BAN permission.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAllowed()
    throws Exception
  {
    /* Arrange. */

    final var admin =
      this.createAdmin("admin", IdAdminPermissionSet.empty());
    final var context =
      this.createContextAndSession(admin);

    /* Act. */

    final var handler = new IdACmdAdminBanCreate();

    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandAdminBanCreate(
          new IdBan(UUID.randomUUID(), "No reason", empty())
        ));
      });

    /* Assert. */

    assertEquals(SECURITY_POLICY_DENIED, ex.errorCode());
  }

  /**
   * Banning admins requires the admin to exist.
   *
   * @throws Exception On errors
   */

  @Test
  public void testMustExist()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(ADMIN_BAN));
    final var admin1 =
      this.createAdmin("admin1", IdAdminPermissionSet.empty());
    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);

    doThrow(new IdDatabaseException("", ADMIN_NONEXISTENT, Map.of(), empty()))
      .when(admins)
      .adminBanCreate(Mockito.any());

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler = new IdACmdAdminBanCreate();
    final var ex =
      assertThrows(IdCommandExecutionFailure.class, () -> {
        handler.execute(context, new IdACommandAdminBanCreate(
          new IdBan(admin1.id(), "No reason", empty())
        ));
      });

    /* Assert. */

    assertEquals(ADMIN_NONEXISTENT, ex.errorCode());

    verify(transaction).queries(IdDatabaseAdminsQueriesType.class);
    verify(transaction).adminIdSet(admin0.id());
    verify(admins, this.once()).adminBanCreate(Mockito.any());
    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
  }

  /**
   * Banning admins works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCreatesOK()
    throws Exception
  {
    /* Arrange. */

    final var admin0 =
      this.createAdmin("admin0", IdAdminPermissionSet.of(ADMIN_BAN));
    final var admin1 =
      this.createAdmin("admin1", IdAdminPermissionSet.empty());
    final var context =
      this.createContextAndSession(admin0);

    final var admins =
      mock(IdDatabaseAdminsQueriesType.class);

    final var transaction = this.transaction();
    when(transaction.queries(IdDatabaseAdminsQueriesType.class))
      .thenReturn(admins);

    /* Act. */

    final var handler =
      new IdACmdAdminBanCreate();
    final var ban =
      new IdBan(admin1.id(), "No reason", empty());
    final var response =
      handler.execute(context, new IdACommandAdminBanCreate(ban));

    /* Assert. */

    assertEquals(
      new IdAResponseAdminBanCreate(context.requestId(), ban),
      response
    );

    verify(transaction).queries(IdDatabaseAdminsQueriesType.class);
    verify(transaction).adminIdSet(admin0.id());
    verify(admins, this.once()).adminBanCreate(ban);
    verifyNoMoreInteractions(admins);
    verifyNoMoreInteractions(transaction);
  }
}
