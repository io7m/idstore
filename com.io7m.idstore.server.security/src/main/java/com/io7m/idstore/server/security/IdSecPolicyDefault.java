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

package com.io7m.idstore.server.security;

import java.util.HashSet;
import java.util.Objects;

import static com.io7m.idstore.model.IdAdminPermission.ADMIN_BAN;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_CREATE;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_DELETE;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_READ;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_CREDENTIALS;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_CREDENTIALS_SELF;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_EMAIL;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_EMAIL_SELF;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_PERMISSIONS;
import static com.io7m.idstore.model.IdAdminPermission.ADMIN_WRITE_PERMISSIONS_SELF;
import static com.io7m.idstore.model.IdAdminPermission.AUDIT_READ;
import static com.io7m.idstore.model.IdAdminPermission.MAIL_TEST;
import static com.io7m.idstore.model.IdAdminPermission.MAINTENANCE_MODE;
import static com.io7m.idstore.model.IdAdminPermission.USER_BAN;
import static com.io7m.idstore.model.IdAdminPermission.USER_CREATE;
import static com.io7m.idstore.model.IdAdminPermission.USER_DELETE;
import static com.io7m.idstore.model.IdAdminPermission.USER_READ;
import static com.io7m.idstore.model.IdAdminPermission.USER_WRITE_CREDENTIALS;
import static com.io7m.idstore.model.IdAdminPermission.USER_WRITE_EMAIL;

/**
 * The default security policy.
 */

public final class IdSecPolicyDefault implements IdSecPolicyType
{
  private static final IdSecPolicyDefault INSTANCE =
    new IdSecPolicyDefault();

  private IdSecPolicyDefault()
  {

  }

  /**
   * @return A reference to this policy
   */

  public static IdSecPolicyType get()
  {
    return INSTANCE;
  }

  private static IdSecPolicyResultType checkUserAction(
    final IdSecUserActionType action)
  {
    if (action instanceof final IdSecUserActionEmailAddBegin e) {
      return checkUserActionEmailAddBegin(e);
    }
    if (action instanceof final IdSecUserActionEmailAddPermit e) {
      return checkUserActionEmailAddPermit(e);
    }
    if (action instanceof final IdSecUserActionEmailAddDeny e) {
      return checkUserActionEmailAddDeny(e);
    }
    if (action instanceof final IdSecUserActionEmailRemoveBegin e) {
      return checkUserActionEmailRemoveBegin(e);
    }
    if (action instanceof final IdSecUserActionEmailRemovePermit e) {
      return checkUserActionEmailRemovePermit(e);
    }
    if (action instanceof final IdSecUserActionEmailRemoveDeny e) {
      return checkUserActionEmailRemoveDeny(e);
    }
    if (action instanceof final IdSecUserActionRealnameUpdate e) {
      return checkUserActionRealnameUpdate(e);
    }
    if (action instanceof final IdSecUserActionPasswordUpdate e) {
      return checkUserActionPasswordUpdate(e);
    }

    return new IdSecPolicyResultDenied("Operation not permitted.");
  }

  private static IdSecPolicyResultType checkUserActionRealnameUpdate(
    final IdSecUserActionRealnameUpdate e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionPasswordUpdate(
    final IdSecUserActionPasswordUpdate e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailAddBegin(
    final IdSecUserActionEmailAddBegin e)
  {
    if (Long.compareUnsigned(e.activeVerificationCount(), 10L) > 0) {
      return new IdSecPolicyResultDenied(
        "Too many email verification requests are currently active for this user."
      );
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailAddPermit(
    final IdSecUserActionEmailAddPermit e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailAddDeny(
    final IdSecUserActionEmailAddDeny e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailRemoveBegin(
    final IdSecUserActionEmailRemoveBegin e)
  {
    if (Long.compareUnsigned(e.activeVerificationCount(), 10L) > 0) {
      return new IdSecPolicyResultDenied(
        "Too many email verification requests are currently active for this user."
      );
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailRemovePermit(
    final IdSecUserActionEmailRemovePermit e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkUserActionEmailRemoveDeny(
    final IdSecUserActionEmailRemoveDeny e)
  {
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminAction(
    final IdSecAdminActionType action)
  {
    if (action instanceof final IdSecAdminActionAdminBanGet e) {
      return checkAdminActionAdminBanGet(e);
    }
    if (action instanceof final IdSecAdminActionAdminBanCreate e) {
      return checkAdminActionAdminBanCreate(e);
    }
    if (action instanceof final IdSecAdminActionAdminBanDelete e) {
      return checkAdminActionAdminBanDelete(e);
    }

    if (action instanceof final IdSecAdminActionUserBanGet e) {
      return checkAdminActionUserBanGet(e);
    }
    if (action instanceof final IdSecAdminActionUserBanCreate e) {
      return checkAdminActionUserBanCreate(e);
    }
    if (action instanceof final IdSecAdminActionUserBanDelete e) {
      return checkAdminActionUserBanDelete(e);
    }

    if (action instanceof final IdSecAdminActionUserRead e) {
      return checkAdminActionUserRead(e);
    }
    if (action instanceof final IdSecAdminActionUserCreate e) {
      return checkAdminActionUserCreate(e);
    }
    if (action instanceof final IdSecAdminActionUserUpdateEmail e) {
      return checkAdminActionUserUpdateEmail(e);
    }
    if (action instanceof final IdSecAdminActionUserUpdateCredentials e) {
      return checkAdminActionUserUpdateCredentials(e);
    }
    if (action instanceof final IdSecAdminActionUserDelete e) {
      return checkAdminActionUserDelete(e);
    }

    if (action instanceof final IdSecAdminActionAdminRead e) {
      return checkAdminActionAdminRead(e);
    }
    if (action instanceof final IdSecAdminActionAdminCreate e) {
      return checkAdminActionAdminCreate(e);
    }
    if (action instanceof final IdSecAdminActionAdminUpdate e) {
      return checkAdminActionAdminUpdate(e);
    }
    if (action instanceof final IdSecAdminActionAdminDelete e) {
      return checkAdminActionAdminDelete(e);
    }

    if (action instanceof final IdSecAdminActionAdminEmailAdd e) {
      return checkAdminActionAdminEmailAdd(e);
    }
    if (action instanceof final IdSecAdminActionAdminEmailRemove e) {
      return checkAdminActionAdminEmailRemove(e);
    }
    if (action instanceof final IdSecAdminActionAdminPermissionGrant e) {
      return checkAdminActionAdminPermissionGrant(e);
    }
    if (action instanceof final IdSecAdminActionAdminPermissionRevoke e) {
      return checkAdminActionAdminPermissionRevoke(e);
    }

    if (action instanceof final IdSecAdminActionAuditRead e) {
      return checkAdminActionAuditRead(e);
    }

    if (action instanceof final IdSecAdminActionMailTest e) {
      return checkMailTest(e);
    }

    if (action instanceof final IdSecAdminActionMaintenanceMode e) {
      return checkMaintenanceMode(e);
    }

    return new IdSecPolicyResultDenied("Operation not permitted.");
  }

  private static IdSecPolicyResultType checkMaintenanceMode(
    final IdSecAdminActionMaintenanceMode e)
  {
    if (!e.admin().permissions().implies(MAINTENANCE_MODE)) {
      return new IdSecPolicyResultDenied(
        "Setting maintenance mode requires the %s permission."
          .formatted(MAINTENANCE_MODE)
      );
    }
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkMailTest(
    final IdSecAdminActionMailTest e)
  {
    if (!e.admin().permissions().implies(MAIL_TEST)) {
      return new IdSecPolicyResultDenied(
        "Testing mail requires the %s permission.".formatted(MAIL_TEST)
      );
    }
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminBanDelete(
    final IdSecAdminActionAdminBanDelete e)
  {
    final var admin = e.admin();
    if (!admin.permissions().implies(ADMIN_BAN)) {
      return new IdSecPolicyResultDenied(
        "Unbanning admins requires the %s permission.".formatted(ADMIN_BAN)
      );
    }
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminBanCreate(
    final IdSecAdminActionAdminBanCreate e)
  {
    final var admin = e.admin();
    if (!admin.permissions().implies(ADMIN_BAN)) {
      return new IdSecPolicyResultDenied(
        "Banning admins requires the %s permission.".formatted(ADMIN_BAN)
      );
    }
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminBanGet(
    final IdSecAdminActionAdminBanGet e)
  {
    final var admin = e.admin();
    if (!admin.permissions().implies(ADMIN_READ)) {
      return new IdSecPolicyResultDenied(
        "Unbanning admins requires the %s permission.".formatted(ADMIN_READ)
      );
    }
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionUserBanDelete(
    final IdSecAdminActionUserBanDelete e)
  {
    final var admin = e.admin();
    if (!admin.permissions().implies(USER_BAN)) {
      return new IdSecPolicyResultDenied(
        "Unbanning users requires the %s permission.".formatted(USER_BAN)
      );
    }
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionUserBanCreate(
    final IdSecAdminActionUserBanCreate e)
  {
    final var admin = e.admin();
    if (!admin.permissions().implies(USER_BAN)) {
      return new IdSecPolicyResultDenied(
        "Banning users requires the %s permission.".formatted(USER_BAN)
      );
    }
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionUserBanGet(
    final IdSecAdminActionUserBanGet e)
  {
    final var admin = e.admin();
    if (!admin.permissions().implies(USER_READ)) {
      return new IdSecPolicyResultDenied(
        "Unbanning users requires the %s permission.".formatted(USER_READ)
      );
    }
    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminPermissionRevoke(
    final IdSecAdminActionAdminPermissionRevoke e)
  {
    final var admin = e.admin();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!admin.permissions().implies(ADMIN_WRITE_PERMISSIONS_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_PERMISSIONS_SELF)
        );
      }
    } else {
      if (!admin.permissions().implies(ADMIN_WRITE_PERMISSIONS)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_PERMISSIONS)
        );
      }
    }

    final var permission = e.permission();
    if (!admin.permissions().implies(permission)) {
      return new IdSecPolicyResultDenied(
        "The %s permission cannot be revoked by an admin that does not have it."
          .formatted(permission)
      );
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminPermissionGrant(
    final IdSecAdminActionAdminPermissionGrant e)
  {
    final var admin = e.admin();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!admin.permissions().implies(ADMIN_WRITE_PERMISSIONS_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_PERMISSIONS_SELF)
        );
      }
    } else {
      if (!admin.permissions().implies(ADMIN_WRITE_PERMISSIONS)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_PERMISSIONS)
        );
      }
    }

    final var permission = e.permission();
    if (!admin.permissions().implies(permission)) {
      return new IdSecPolicyResultDenied(
        "The %s permission cannot be granted by an admin that does not have it."
          .formatted(permission)
      );
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminEmailRemove(
    final IdSecAdminActionAdminEmailRemove e)
  {
    final var admin = e.admin();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!admin.permissions().implies(ADMIN_WRITE_EMAIL_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_EMAIL_SELF)
        );
      }
    } else {
      if (!admin.permissions().implies(ADMIN_WRITE_EMAIL)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_EMAIL)
        );
      }
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminEmailAdd(
    final IdSecAdminActionAdminEmailAdd e)
  {
    final var admin = e.admin();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!admin.permissions().implies(ADMIN_WRITE_EMAIL_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_EMAIL_SELF)
        );
      }
    } else {
      if (!admin.permissions().implies(ADMIN_WRITE_EMAIL)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_EMAIL)
        );
      }
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAuditRead(
    final IdSecAdminActionAuditRead e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(AUDIT_READ)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Reading audit records requires the %s permission.".formatted(AUDIT_READ)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserRead(
    final IdSecAdminActionUserRead e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(USER_READ)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Reading users requires the %s permission.".formatted(USER_READ)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserDelete(
    final IdSecAdminActionUserDelete e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(USER_DELETE)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Deleting users requires the %s permission.".formatted(USER_DELETE)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserCreate(
    final IdSecAdminActionUserCreate e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(USER_CREATE)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Creating users requires the %s permission.".formatted(USER_CREATE)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserUpdateEmail(
    final IdSecAdminActionUserUpdateEmail e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(USER_WRITE_EMAIL)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Updating users requires the %s permission."
        .formatted(USER_WRITE_EMAIL)
    );
  }

  private static IdSecPolicyResultType checkAdminActionUserUpdateCredentials(
    final IdSecAdminActionUserUpdateCredentials e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(USER_WRITE_CREDENTIALS)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Updating users requires the %s permission."
        .formatted(USER_WRITE_CREDENTIALS)
    );
  }

  private static IdSecPolicyResultType checkAdminActionAdminRead(
    final IdSecAdminActionAdminRead e)
  {
    final var permissions = e.admin().permissions();
    if (permissions.implies(ADMIN_READ)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Reading admins requires the %s permission.".formatted(ADMIN_READ)
    );
  }

  private static IdSecPolicyResultType checkAdminActionAdminCreate(
    final IdSecAdminActionAdminCreate e)
  {
    final var permissionsHeld =
      e.admin().permissions();
    final var permissionsWanted =
      e.targetPermissions();

    if (!permissionsHeld.implies(ADMIN_CREATE)) {
      return new IdSecPolicyResultDenied(
        "Creating admins requires the %s permission.".formatted(ADMIN_CREATE)
      );
    }

    if (permissionsHeld.impliesAll(permissionsWanted)) {
      return new IdSecPolicyResultPermitted();
    }

    final var missing = new HashSet<>(permissionsWanted);
    missing.removeAll(permissionsHeld.impliedPermissions());

    return new IdSecPolicyResultDenied(
      "The current admin cannot grant the following permissions: %s"
        .formatted(missing)
    );
  }

  private static IdSecPolicyResultType checkAdminActionAdminUpdate(
    final IdSecAdminActionAdminUpdate e)
  {
    final var admin = e.admin();
    final var permissionsHeld = admin.permissions();
    if (Objects.equals(admin.id(), e.targetAdmin())) {
      if (!permissionsHeld.implies(ADMIN_WRITE_CREDENTIALS_SELF)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_CREDENTIALS_SELF)
        );
      }
    } else {
      if (!permissionsHeld.implies(ADMIN_WRITE_CREDENTIALS)) {
        return new IdSecPolicyResultDenied(
          "Modifying admins requires the %s permission."
            .formatted(ADMIN_WRITE_CREDENTIALS)
        );
      }
    }

    return new IdSecPolicyResultPermitted();
  }

  private static IdSecPolicyResultType checkAdminActionAdminDelete(
    final IdSecAdminActionAdminDelete e)
  {
    final var permissionsHeld = e.admin().permissions();
    if (permissionsHeld.implies(ADMIN_DELETE)) {
      return new IdSecPolicyResultPermitted();
    }

    return new IdSecPolicyResultDenied(
      "Deleting admins requires the %s permission.".formatted(ADMIN_DELETE)
    );
  }

  @Override
  public IdSecPolicyResultType check(
    final IdSecActionType action)
  {
    Objects.requireNonNull(action, "action");

    if (action instanceof final IdSecAdminActionType admin) {
      return checkAdminAction(admin);
    }
    if (action instanceof final IdSecUserActionType user) {
      return checkUserAction(user);
    }
    return new IdSecPolicyResultDenied("Operation not permitted.");
  }
}
