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

package com.io7m.idstore.protocol.admin.cb.internal;

import com.io7m.cedarbridge.runtime.api.CBIntegerUnsigned16;
import com.io7m.cedarbridge.runtime.api.CBList;
import com.io7m.cedarbridge.runtime.api.CBOptionType;
import com.io7m.cedarbridge.runtime.api.CBSome;
import com.io7m.cedarbridge.runtime.api.CBString;
import com.io7m.cedarbridge.runtime.api.CBUUID;
import com.io7m.cedarbridge.runtime.convenience.CBSets;
import com.io7m.idstore.model.IdAdmin;
import com.io7m.idstore.model.IdAdminColumn;
import com.io7m.idstore.model.IdAdminColumnOrdering;
import com.io7m.idstore.model.IdAdminPermission;
import com.io7m.idstore.model.IdAdminPermissionSet;
import com.io7m.idstore.model.IdAdminSearchByEmailParameters;
import com.io7m.idstore.model.IdAdminSearchParameters;
import com.io7m.idstore.model.IdAdminSummary;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPasswordException;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminBanGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminCreate;
import com.io7m.idstore.protocol.admin.IdACommandAdminDelete;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin.IdACommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin.IdACommandAdminGet;
import com.io7m.idstore.protocol.admin.IdACommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin.IdACommandAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdACommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdACommandAdminSelf;
import com.io7m.idstore.protocol.admin.IdACommandAdminUpdateCredentials;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminBanGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminCreate;
import com.io7m.idstore.protocol.admin.IdAResponseAdminDelete;
import com.io7m.idstore.protocol.admin.IdAResponseAdminGet;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchNext;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.IdAResponseAdminSelf;
import com.io7m.idstore.protocol.admin.IdAResponseAdminUpdate;
import com.io7m.idstore.protocol.admin.cb.IdA1Admin;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminColumn;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminColumnOrdering;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminWriteCredentials;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminWriteCredentialsSelf;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminWriteEmail;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminWriteEmailSelf;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminWritePermissions;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminWritePermissionsSelf;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.MailTest;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.MaintenanceMode;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.UserWriteCredentials;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.UserWriteEmail;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminSearchByEmailParameters;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminSearchParameters;
import com.io7m.idstore.protocol.admin.cb.IdA1AdminSummary;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminBanCreate;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminBanDelete;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminBanGet;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminCreate;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminDelete;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminEmailAdd;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminEmailRemove;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminGet;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminGetByEmail;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminPermissionGrant;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminPermissionRevoke;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminSearchBegin;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminSearchNext;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminSelf;
import com.io7m.idstore.protocol.admin.cb.IdA1CommandAdminUpdateCredentials;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminBanCreate;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminBanDelete;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminBanGet;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminCreate;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminDelete;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminGet;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminSearchBegin;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminSearchByEmailBegin;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminSearchByEmailNext;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminSearchByEmailPrevious;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminSearchNext;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminSearchPrevious;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminSelf;
import com.io7m.idstore.protocol.admin.cb.IdA1ResponseAdminUpdate;
import com.io7m.idstore.protocol.api.IdProtocolException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.io7m.cedarbridge.runtime.api.CBBooleanType.fromBoolean;
import static com.io7m.cedarbridge.runtime.api.CBOptionType.fromOptional;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.model.IdAdminColumn.BY_ID;
import static com.io7m.idstore.model.IdAdminColumn.BY_IDNAME;
import static com.io7m.idstore.model.IdAdminColumn.BY_REALNAME;
import static com.io7m.idstore.model.IdAdminColumn.BY_TIME_CREATED;
import static com.io7m.idstore.model.IdAdminColumn.BY_TIME_UPDATED;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminColumn.ByID;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminColumn.ByIDName;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminColumn.ByRealName;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminColumn.ByTimeCreated;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminColumn.ByTimeUpdated;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminBan;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminCreate;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminDelete;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AdminRead;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.AuditRead;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.UserBan;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.UserCreate;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.UserDelete;
import static com.io7m.idstore.protocol.admin.cb.IdA1AdminPermission.UserRead;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWireBan;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWireEmails;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWirePage;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWirePassword;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWirePasswordOptional;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWireTimeRange;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.fromWireTimestamp;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWireBan;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWirePage;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWirePassword;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWireTimeRange;
import static com.io7m.idstore.protocol.admin.cb.internal.IdACB1ValidationGeneral.toWireTimestamp;
import static java.util.Objects.requireNonNullElse;

/**
 * Functions to translate between the core command set and the Admin v1
 * Cedarbridge encoding command set.
 */

public final class IdACB1ValidationAdmin
{
  private IdACB1ValidationAdmin()
  {

  }

  public static IdA1ResponseAdminSearchBegin toWireResponseAdminSearchBegin(
    final IdAResponseAdminSearchBegin r)
  {
    return new IdA1ResponseAdminSearchBegin(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationAdmin::toWireAdminSummary)
    );
  }

  public static IdA1ResponseAdminSearchNext toWireResponseAdminSearchNext(
    final IdAResponseAdminSearchNext r)
  {
    return new IdA1ResponseAdminSearchNext(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationAdmin::toWireAdminSummary)
    );
  }

  public static IdA1ResponseAdminSearchPrevious toWireResponseAdminSearchPrevious(
    final IdAResponseAdminSearchPrevious r)
  {
    return new IdA1ResponseAdminSearchPrevious(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationAdmin::toWireAdminSummary)
    );
  }

  public static IdA1ResponseAdminSearchByEmailBegin toWireResponseAdminSearchByEmailBegin(
    final IdAResponseAdminSearchByEmailBegin r)
  {
    return new IdA1ResponseAdminSearchByEmailBegin(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationAdmin::toWireAdminSummary)
    );
  }

  public static IdA1ResponseAdminSearchByEmailNext toWireResponseAdminSearchByEmailNext(
    final IdAResponseAdminSearchByEmailNext r)
  {
    return new IdA1ResponseAdminSearchByEmailNext(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationAdmin::toWireAdminSummary)
    );
  }

  public static IdA1ResponseAdminSearchByEmailPrevious toWireResponseAdminSearchByEmailPrevious(
    final IdAResponseAdminSearchByEmailPrevious r)
  {
    return new IdA1ResponseAdminSearchByEmailPrevious(
      new CBUUID(r.requestId()),
      toWirePage(r.page(), IdACB1ValidationAdmin::toWireAdminSummary)
    );
  }

  private static IdA1AdminSummary toWireAdminSummary(
    final IdAdminSummary s)
  {
    return new IdA1AdminSummary(
      new CBUUID(s.id()),
      new CBString(s.idName().value()),
      new CBString(s.realName().value()),
      toWireTimestamp(s.timeCreated()),
      toWireTimestamp(s.timeUpdated())
    );
  }

  public static IdA1ResponseAdminSelf toWireResponseAdminSelf(
    final IdAResponseAdminSelf r)
  {
    return new IdA1ResponseAdminSelf(
      new CBUUID(r.requestId()),
      toWireAdmin(r.admin())
    );
  }

  public static IdA1ResponseAdminUpdate toWireResponseAdminUpdate(
    final IdAResponseAdminUpdate r)
  {
    return new IdA1ResponseAdminUpdate(
      new CBUUID(r.requestId()),
      toWireAdmin(r.admin())
    );
  }

  public static IdA1ResponseAdminGet toWireResponseAdminGet(
    final IdAResponseAdminGet r)
  {
    return new IdA1ResponseAdminGet(
      new CBUUID(r.requestId()),
      fromOptional(r.admin().map(IdACB1ValidationAdmin::toWireAdmin))
    );
  }

  public static IdA1ResponseAdminDelete toWireResponseAdminDelete(
    final IdAResponseAdminDelete r)
  {
    return new IdA1ResponseAdminDelete(
      new CBUUID(r.requestId())
    );
  }

  public static IdA1ResponseAdminCreate toWireResponseAdminCreate(
    final IdAResponseAdminCreate r)
  {
    return new IdA1ResponseAdminCreate(
      new CBUUID(r.requestId()),
      toWireAdmin(r.admin())
    );
  }

  public static IdA1Admin toWireAdmin(
    final IdAdmin admin)
  {
    return new IdA1Admin(
      new CBUUID(admin.id()),
      new CBString(admin.idName().value()),
      new CBString(admin.realName().value()),
      new CBList<>(
        admin.emails()
          .toList()
          .stream()
          .map(IdEmail::value)
          .map(CBString::new)
          .toList()
      ),
      toWireTimestamp(admin.timeCreated()),
      toWireTimestamp(admin.timeUpdated()),
      toWirePassword(admin.password()),
      toWirePermissions(admin.permissions().impliedPermissions())
    );
  }

  public static IdA1ResponseAdminBanCreate toWireResponseAdminBanCreate(
    final IdAResponseAdminBanCreate r)
  {
    return new IdA1ResponseAdminBanCreate(
      new CBUUID(r.requestId()),
      toWireBan(r.ban())
    );
  }

  public static IdA1ResponseAdminBanDelete toWireResponseAdminBanDelete(
    final IdAResponseAdminBanDelete r)
  {
    return new IdA1ResponseAdminBanDelete(
      new CBUUID(r.requestId())
    );
  }

  public static IdA1ResponseAdminBanGet toWireResponseAdminBanGet(
    final IdAResponseAdminBanGet r)
  {
    return new IdA1ResponseAdminBanGet(
      new CBUUID(r.requestId()),
      fromOptional(r.ban().map(IdACB1ValidationGeneral::toWireBan))
    );
  }

  public static IdA1CommandAdminUpdateCredentials toWireCommandAdminUpdateCredentials(
    final IdACommandAdminUpdateCredentials c)
  {
    return new IdA1CommandAdminUpdateCredentials(
      new CBUUID(c.admin()),
      fromOptional(c.idName().map(IdName::value).map(CBString::new)),
      fromOptional(c.realName().map(IdRealName::value).map(CBString::new)),
      fromOptional(c.password().map(IdACB1ValidationGeneral::toWirePassword))
    );
  }

  public static IdA1CommandAdminSelf toWireCommandAdminSelf()
  {
    return new IdA1CommandAdminSelf();
  }

  public static IdA1CommandAdminSearchByEmailNext toWireCommandAdminSearchByEmailNext()
  {
    return new IdA1CommandAdminSearchByEmailNext();
  }

  public static IdA1CommandAdminSearchByEmailPrevious toWireCommandAdminSearchByEmailPrevious()
  {
    return new IdA1CommandAdminSearchByEmailPrevious();
  }

  public static IdA1CommandAdminSearchByEmailBegin toWireCommandAdminSearchByEmailBegin(
    final IdACommandAdminSearchByEmailBegin c)
  {
    return new IdA1CommandAdminSearchByEmailBegin(
      toWireAdminSearchByEmailParameters(c.parameters())
    );
  }

  private static IdA1AdminSearchByEmailParameters toWireAdminSearchByEmailParameters(
    final IdAdminSearchByEmailParameters parameters)
  {
    return new IdA1AdminSearchByEmailParameters(
      toWireTimeRange(parameters.timeCreatedRange()),
      toWireTimeRange(parameters.timeUpdatedRange()),
      new CBString(parameters.search()),
      toWireAdminColumnOrdering(parameters.ordering()),
      new CBIntegerUnsigned16(parameters.limit())
    );
  }

  public static IdA1CommandAdminSearchNext toWireCommandAdminSearchNext()
  {
    return new IdA1CommandAdminSearchNext();
  }

  public static IdA1CommandAdminSearchPrevious toWireCommandAdminSearchPrevious()
  {
    return new IdA1CommandAdminSearchPrevious();
  }

  public static IdA1CommandAdminSearchBegin toWireCommandAdminSearchBegin(
    final IdACommandAdminSearchBegin c)
  {
    return new IdA1CommandAdminSearchBegin(
      toWireAdminSearchParameters(c.parameters())
    );
  }

  private static IdA1AdminSearchParameters toWireAdminSearchParameters(
    final IdAdminSearchParameters parameters)
  {
    return new IdA1AdminSearchParameters(
      toWireTimeRange(parameters.timeCreatedRange()),
      toWireTimeRange(parameters.timeUpdatedRange()),
      fromOptional(parameters.search().map(CBString::new)),
      toWireAdminColumnOrdering(parameters.ordering()),
      new CBIntegerUnsigned16(parameters.limit())
    );
  }

  private static IdA1AdminColumnOrdering toWireAdminColumnOrdering(
    final IdAdminColumnOrdering o)
  {
    return new IdA1AdminColumnOrdering(
      toWireAdminColumn(o.column()),
      fromBoolean(o.ascending())
    );
  }

  private static IdA1AdminColumn toWireAdminColumn(
    final IdAdminColumn column)
  {
    return switch (column) {
      case BY_ID -> new ByID();
      case BY_IDNAME -> new ByIDName();
      case BY_REALNAME -> new ByRealName();
      case BY_TIME_CREATED -> new ByTimeCreated();
      case BY_TIME_UPDATED -> new ByTimeUpdated();
    };
  }

  public static IdA1CommandAdminPermissionGrant toWireCommandAdminPermissionGrant(
    final IdACommandAdminPermissionGrant c)
  {
    return new IdA1CommandAdminPermissionGrant(
      new CBUUID(c.admin()),
      toWirePermission(c.permission())
    );
  }

  public static IdA1CommandAdminPermissionRevoke toWireCommandAdminPermissionRevoke(
    final IdACommandAdminPermissionRevoke c)
  {
    return new IdA1CommandAdminPermissionRevoke(
      new CBUUID(c.admin()),
      toWirePermission(c.permission())
    );
  }

  public static IdA1CommandAdminEmailRemove toWireCommandAdminEmailRemove(
    final IdACommandAdminEmailRemove c)
  {
    return new IdA1CommandAdminEmailRemove(
      new CBUUID(c.admin()),
      new CBString(c.email().value())
    );
  }

  public static IdA1CommandAdminEmailAdd toWireCommandAdminEmailAdd(
    final IdACommandAdminEmailAdd c)
  {
    return new IdA1CommandAdminEmailAdd(
      new CBUUID(c.admin()),
      new CBString(c.email().value())
    );
  }

  public static IdA1CommandAdminGet toWireCommandAdminGet(
    final IdACommandAdminGet c)
  {
    return new IdA1CommandAdminGet(
      new CBUUID(c.admin())
    );
  }

  public static IdA1CommandAdminGetByEmail toWireCommandAdminGetByEmail(
    final IdACommandAdminGetByEmail c)
  {
    return new IdA1CommandAdminGetByEmail(
      new CBString(c.email().value())
    );
  }

  public static IdA1CommandAdminBanDelete toWireCommandAdminBanDelete(
    final IdACommandAdminBanDelete c)
  {
    return new IdA1CommandAdminBanDelete(
      new CBUUID(c.admin())
    );
  }

  public static IdA1CommandAdminBanGet toWireCommandAdminBanGet(
    final IdACommandAdminBanGet c)
  {
    return new IdA1CommandAdminBanGet(
      new CBUUID(c.admin())
    );
  }

  public static IdA1CommandAdminBanCreate toWireCommandAdminBanCreate(
    final IdACommandAdminBanCreate c)
  {
    return new IdA1CommandAdminBanCreate(
      toWireBan(c.ban())
    );
  }

  public static IdA1CommandAdminCreate toWireCommandAdminCreate(
    final IdACommandAdminCreate c)
  {
    return new IdA1CommandAdminCreate(
      fromOptional(c.id().map(CBUUID::new)),
      new CBString(c.idName().value()),
      new CBString(c.realName().value()),
      new CBString(c.email().value()),
      toWirePassword(c.password()),
      toWirePermissions(c.permissions())
    );
  }

  public static IdA1CommandAdminDelete toWireCommandAdminDelete(
    final IdACommandAdminDelete c)
  {
    return new IdA1CommandAdminDelete(
      new CBUUID(c.adminId())
    );
  }

  private static CBList<IdA1AdminPermission> toWirePermissions(
    final Set<IdAdminPermission> permissions)
  {
    return new CBList<>(
      permissions.stream()
        .map(IdACB1ValidationAdmin::toWirePermission)
        .toList()
    );
  }

  private static IdA1AdminPermission toWirePermission(
    final IdAdminPermission p)
  {
    return switch (p) {
      case ADMIN_BAN -> new AdminBan();
      case ADMIN_CREATE -> new AdminCreate();
      case ADMIN_DELETE -> new AdminDelete();
      case ADMIN_READ -> new AdminRead();
      case ADMIN_WRITE_CREDENTIALS -> new AdminWriteCredentials();
      case ADMIN_WRITE_CREDENTIALS_SELF -> new AdminWriteCredentialsSelf();
      case ADMIN_WRITE_EMAIL -> new AdminWriteEmail();
      case ADMIN_WRITE_EMAIL_SELF -> new AdminWriteEmailSelf();
      case ADMIN_WRITE_PERMISSIONS -> new AdminWritePermissions();
      case ADMIN_WRITE_PERMISSIONS_SELF -> new AdminWritePermissionsSelf();
      case AUDIT_READ -> new AuditRead();
      case USER_BAN -> new UserBan();
      case USER_CREATE -> new UserCreate();
      case USER_DELETE -> new UserDelete();
      case USER_READ -> new UserRead();
      case USER_WRITE_CREDENTIALS -> new UserWriteCredentials();
      case USER_WRITE_EMAIL -> new UserWriteEmail();
      case MAIL_TEST -> new MailTest();
      case MAINTENANCE_MODE -> new MaintenanceMode();
    };
  }

  public static IdAResponseAdminBanCreate fromWireResponseAdminBanCreate(
    final IdA1ResponseAdminBanCreate c)
  {
    return new IdAResponseAdminBanCreate(
      c.fieldRequestId().value(),
      fromWireBan(c.fieldBan())
    );
  }

  public static IdAResponseAdminBanDelete fromWireResponseAdminBanDelete(
    final IdA1ResponseAdminBanDelete c)
  {
    return new IdAResponseAdminBanDelete(
      c.fieldRequestId().value()
    );
  }

  public static IdAResponseAdminBanGet fromWireResponseAdminBanGet(
    final IdA1ResponseAdminBanGet c)
  {
    return new IdAResponseAdminBanGet(
      c.fieldRequestId().value(),
      c.fieldBan().asOptional().map(IdACB1ValidationGeneral::fromWireBan)
    );
  }

  public static IdAResponseAdminCreate fromWireResponseAdminCreate(
    final IdA1ResponseAdminCreate c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdAResponseAdminCreate(
      c.fieldRequestId().value(),
      fromWireAdmin(c.fieldAdmin())
    );
  }

  public static IdAResponseAdminDelete fromWireResponseAdminDelete(
    final IdA1ResponseAdminDelete c)
  {
    return new IdAResponseAdminDelete(
      c.fieldRequestId().value()
    );
  }

  public static IdAResponseAdminGet fromWireResponseAdminGet(
    final IdA1ResponseAdminGet c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdAResponseAdminGet(
      c.fieldRequestId().value(),
      fromWireAdminOptional(c.fieldAdmin())
    );
  }

  private static Optional<IdAdmin> fromWireAdminOptional(
    final CBOptionType<IdA1Admin> fieldAdmin)
    throws IdProtocolException, IdPasswordException
  {
    if (fieldAdmin instanceof final CBSome<IdA1Admin> some) {
      return Optional.of(fromWireAdmin(some.value()));
    }
    return Optional.empty();
  }

  public static IdAResponseAdminSearchBegin fromWireResponseAdminSearchBegin(
    final IdA1ResponseAdminSearchBegin c)
  {
    return new IdAResponseAdminSearchBegin(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationAdmin::fromWireAdminSummary)
    );
  }

  public static IdAResponseAdminSearchByEmailBegin fromWireResponseAdminSearchByEmailBegin(
    final IdA1ResponseAdminSearchByEmailBegin c)
  {
    return new IdAResponseAdminSearchByEmailBegin(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationAdmin::fromWireAdminSummary)
    );
  }

  public static IdAResponseAdminSearchByEmailNext fromWireResponseAdminSearchByEmailNext(
    final IdA1ResponseAdminSearchByEmailNext c)
  {
    return new IdAResponseAdminSearchByEmailNext(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationAdmin::fromWireAdminSummary)
    );
  }

  public static IdAResponseAdminSearchByEmailPrevious fromWireResponseAdminSearchByEmailPrevious(
    final IdA1ResponseAdminSearchByEmailPrevious c)
  {
    return new IdAResponseAdminSearchByEmailPrevious(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationAdmin::fromWireAdminSummary)
    );
  }

  public static IdAResponseAdminSearchNext fromWireResponseAdminSearchNext(
    final IdA1ResponseAdminSearchNext c)
  {
    return new IdAResponseAdminSearchNext(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationAdmin::fromWireAdminSummary)
    );
  }

  public static IdAResponseAdminSearchPrevious fromWireResponseAdminSearchPrevious(
    final IdA1ResponseAdminSearchPrevious c)
  {
    return new IdAResponseAdminSearchPrevious(
      c.fieldRequestId().value(),
      fromWirePage(c.fieldPage(), IdACB1ValidationAdmin::fromWireAdminSummary)
    );
  }

  public static IdAResponseAdminUpdate fromWireResponseAdminUpdate(
    final IdA1ResponseAdminUpdate c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdAResponseAdminUpdate(
      c.fieldRequestId().value(),
      fromWireAdmin(c.fieldAdmin())
    );
  }

  public static IdAResponseAdminSelf fromWireResponseAdminSelf(
    final IdA1ResponseAdminSelf c)
    throws IdProtocolException, IdPasswordException
  {
    return new IdAResponseAdminSelf(
      c.fieldRequestId().value(),
      fromWireAdmin(c.fieldAdmin())
    );
  }

  public static IdACommandAdminBanCreate fromWireCommandAdminBanCreate(
    final IdA1CommandAdminBanCreate c)
  {
    return new IdACommandAdminBanCreate(
      fromWireBan(c.fieldBan())
    );
  }

  public static IdACommandAdminBanDelete fromWireCommandAdminBanDelete(
    final IdA1CommandAdminBanDelete c)
  {
    return new IdACommandAdminBanDelete(
      c.fieldAdminId().value()
    );
  }

  public static IdACommandAdminBanGet fromWireCommandAdminBanGet(
    final IdA1CommandAdminBanGet c)
  {
    return new IdACommandAdminBanGet(
      c.fieldAdminId().value()
    );
  }

  public static IdACommandAdminCreate fromWireCommandAdminCreate(
    final IdA1CommandAdminCreate c)
    throws IdPasswordException, IdProtocolException
  {
    return new IdACommandAdminCreate(
      c.fieldAdminId().asOptional().map(CBUUID::value),
      new IdName(c.fieldIdName().value()),
      new IdRealName(c.fieldRealName().value()),
      new IdEmail(c.fieldEmail().value()),
      fromWirePassword(c.fieldPassword()),
      fromWireAdminPermissions(c.fieldPermissions())
    );
  }

  public static IdACommandAdminDelete fromWireCommandAdminDelete(
    final IdA1CommandAdminDelete c)
  {
    return new IdACommandAdminDelete(
      c.fieldAdminId().value()
    );
  }

  public static IdACommandAdminEmailAdd fromWireCommandAdminEmailAdd(
    final IdA1CommandAdminEmailAdd c)
  {
    return new IdACommandAdminEmailAdd(
      c.fieldAdminId().value(),
      new IdEmail(c.fieldEmail().value())
    );
  }

  public static IdACommandAdminEmailRemove fromWireCommandAdminEmailRemove(
    final IdA1CommandAdminEmailRemove c)
  {
    return new IdACommandAdminEmailRemove(
      c.fieldAdminId().value(),
      new IdEmail(c.fieldEmail().value())
    );
  }

  public static IdACommandAdminGet fromWireCommandAdminGet(
    final IdA1CommandAdminGet c)
  {
    return new IdACommandAdminGet(
      c.fieldAdminId().value()
    );
  }

  public static IdACommandAdminGetByEmail fromWireCommandAdminGetByEmail(
    final IdA1CommandAdminGetByEmail c)
  {
    return new IdACommandAdminGetByEmail(
      new IdEmail(c.fieldEmail().value())
    );
  }

  public static IdACommandAdminPermissionGrant fromWireCommandAdminPermissionGrant(
    final IdA1CommandAdminPermissionGrant c)
  {
    return new IdACommandAdminPermissionGrant(
      c.fieldAdminId().value(),
      fromWireAdminPermission(c.fieldPermission())
    );
  }

  public static IdACommandAdminPermissionRevoke fromWireCommandAdminPermissionRevoke(
    final IdA1CommandAdminPermissionRevoke c)
  {
    return new IdACommandAdminPermissionRevoke(
      c.fieldAdminId().value(),
      fromWireAdminPermission(c.fieldPermission())
    );
  }

  public static IdACommandAdminSearchBegin fromWireCommandAdminSearchBegin(
    final IdA1CommandAdminSearchBegin c)
  {
    return new IdACommandAdminSearchBegin(
      fromWireAdminSearchParameters(c.fieldParameters())
    );
  }

  public static IdACommandAdminSearchNext fromWireCommandAdminSearchNext()
  {
    return new IdACommandAdminSearchNext();
  }

  public static IdACommandAdminSearchPrevious fromWireCommandAdminSearchPrevious()
  {
    return new IdACommandAdminSearchPrevious();
  }

  public static IdACommandAdminSearchByEmailBegin fromWireCommandAdminSearchByEmailBegin(
    final IdA1CommandAdminSearchByEmailBegin c)
  {
    return new IdACommandAdminSearchByEmailBegin(
      fromWireAdminSearchByEmailParameters(c.fieldParameters())
    );
  }

  public static IdACommandAdminSearchByEmailNext fromWireCommandAdminSearchByEmailNext()
  {
    return new IdACommandAdminSearchByEmailNext();
  }

  public static IdACommandAdminSearchByEmailPrevious fromWireCommandAdminSearchByEmailPrevious()
  {
    return new IdACommandAdminSearchByEmailPrevious();
  }

  public static IdACommandAdminUpdateCredentials fromWireCommandAdminUpdateCredentials(
    final IdA1CommandAdminUpdateCredentials c)
    throws IdPasswordException
  {
    return new IdACommandAdminUpdateCredentials(
      c.fieldAdminId().value(),
      c.fieldIdName().asOptional().map(n -> new IdName(n.value())),
      c.fieldRealName().asOptional().map(n -> new IdRealName(n.value())),
      fromWirePasswordOptional(c.fieldPassword())
    );
  }

  public static IdACommandAdminSelf fromWireCommandAdminSelf()
  {
    return new IdACommandAdminSelf();
  }

  private static IdAdminSearchByEmailParameters fromWireAdminSearchByEmailParameters(
    final IdA1AdminSearchByEmailParameters p)
  {
    return new IdAdminSearchByEmailParameters(
      fromWireTimeRange(p.fieldTimeCreatedRange()),
      fromWireTimeRange(p.fieldTimeUpdatedRange()),
      p.fieldSearch().value(),
      fromWireAdminColumnOrdering(p.fieldOrdering()),
      p.fieldLimit().value()
    );
  }

  private static IdAdminSearchParameters fromWireAdminSearchParameters(
    final IdA1AdminSearchParameters p)
  {
    return new IdAdminSearchParameters(
      fromWireTimeRange(p.fieldTimeCreatedRange()),
      fromWireTimeRange(p.fieldTimeUpdatedRange()),
      p.fieldSearch().asOptional().map(CBString::value),
      fromWireAdminColumnOrdering(p.fieldOrdering()),
      p.fieldLimit().value()
    );
  }

  private static IdAdminColumnOrdering fromWireAdminColumnOrdering(
    final IdA1AdminColumnOrdering o)
  {
    return new IdAdminColumnOrdering(
      fromWireAdminColumn(o.fieldColumn()),
      o.fieldAscending().asBoolean()
    );
  }

  private static IdAdminColumn fromWireAdminColumn(
    final IdA1AdminColumn c)
  {
    if (c instanceof ByID) {
      return BY_ID;
    } else if (c instanceof ByIDName) {
      return BY_IDNAME;
    } else if (c instanceof ByRealName) {
      return BY_REALNAME;
    } else if (c instanceof ByTimeCreated) {
      return BY_TIME_CREATED;
    } else if (c instanceof ByTimeUpdated) {
      return BY_TIME_UPDATED;
    }

    throw new IllegalArgumentException(
      "Unrecognized admin column: %s".formatted(c)
    );
  }

  public static Set<IdAdminPermission> fromWireAdminPermissions(
    final CBList<IdA1AdminPermission> fieldPermissions)
    throws IdProtocolException
  {
    try {
      return CBSets.toSet(
        fieldPermissions,
        IdACB1ValidationAdmin::fromWireAdminPermission
      );
    } catch (final IllegalArgumentException e) {
      throw new IdProtocolException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        PROTOCOL_ERROR,
        Map.of(),
        Optional.empty()
      );
    }
  }

  private static IdAdminPermission fromWireAdminPermission(
    final IdA1AdminPermission p)
  {
    return switch (p) {
      case final AdminDelete adminDelete -> IdAdminPermission.ADMIN_DELETE;
      case final AdminRead adminRead -> IdAdminPermission.ADMIN_READ;
      case final AdminBan adminBan -> IdAdminPermission.ADMIN_BAN;
      case final AdminCreate adminCreate -> IdAdminPermission.ADMIN_CREATE;
      case final AdminWritePermissionsSelf adminWritePermissionsSelf ->
        IdAdminPermission.ADMIN_WRITE_PERMISSIONS_SELF;
      case final AdminWritePermissions adminWritePermissions ->
        IdAdminPermission.ADMIN_WRITE_PERMISSIONS;
      case final AdminWriteEmail adminWriteEmail ->
        IdAdminPermission.ADMIN_WRITE_EMAIL;
      case final AdminWriteEmailSelf adminWriteEmailSelf ->
        IdAdminPermission.ADMIN_WRITE_EMAIL_SELF;
      case final AdminWriteCredentials adminWriteCredentials ->
        IdAdminPermission.ADMIN_WRITE_CREDENTIALS;
      case final AdminWriteCredentialsSelf adminWriteCredentialsSelf ->
        IdAdminPermission.ADMIN_WRITE_CREDENTIALS_SELF;
      case final UserBan userBan -> IdAdminPermission.USER_BAN;
      case final UserCreate userCreate -> IdAdminPermission.USER_CREATE;
      case final UserDelete userDelete -> IdAdminPermission.USER_DELETE;
      case final UserRead userRead -> IdAdminPermission.USER_READ;
      case final UserWriteCredentials userWriteCredentials ->
        IdAdminPermission.USER_WRITE_CREDENTIALS;
      case final UserWriteEmail userWriteEmail ->
        IdAdminPermission.USER_WRITE_EMAIL;
      case final AuditRead auditRead -> IdAdminPermission.AUDIT_READ;
      case final MailTest mailTest -> IdAdminPermission.MAIL_TEST;
      case final MaintenanceMode maintenanceMode ->
        IdAdminPermission.MAINTENANCE_MODE;
      case null, default -> throw new IllegalArgumentException(
        "Unrecognized permission: %s".formatted(p)
      );
    };
  }

  private static IdAdminSummary fromWireAdminSummary(
    final IdA1AdminSummary i)
  {
    return new IdAdminSummary(
      i.fieldId().value(),
      new IdName(i.fieldIdName().value()),
      new IdRealName(i.fieldRealName().value()),
      fromWireTimestamp(i.fieldTimeCreated()),
      fromWireTimestamp(i.fieldTimeUpdated())
    );
  }

  public static IdAdmin fromWireAdmin(
    final IdA1Admin fieldAdmin)
    throws IdPasswordException, IdProtocolException
  {
    return new IdAdmin(
      fieldAdmin.fieldId().value(),
      new IdName(fieldAdmin.fieldIdName().value()),
      new IdRealName(fieldAdmin.fieldRealName().value()),
      fromWireEmails(fieldAdmin.fieldEmails()),
      fromWireTimestamp(fieldAdmin.fieldTimeCreated()),
      fromWireTimestamp(fieldAdmin.fieldTimeUpdated()),
      fromWirePassword(fieldAdmin.fieldPassword()),
      fromWireAdminPermissionSet(fieldAdmin.fieldPermissions())
    );
  }

  private static IdAdminPermissionSet fromWireAdminPermissionSet(
    final CBList<IdA1AdminPermission> fieldPermissions)
    throws IdProtocolException
  {
    return IdAdminPermissionSet.of(fromWireAdminPermissions(fieldPermissions));
  }
}
