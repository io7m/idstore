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

package com.io7m.idstore.protocol.admin;

import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdPassword;
import com.io7m.idstore.model.IdRealName;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Update the given admin.
 *
 * @param admin    The admin to be updated
 * @param idName   The admin's ID name
 * @param realName The admin's realname
 * @param password The admin's password
 */

public record IdACommandAdminUpdateCredentials(
  UUID admin,
  Optional<IdName> idName,
  Optional<IdRealName> realName,
  Optional<IdPassword> password)
  implements IdACommandType<IdAResponseAdminUpdate>
{
  /**
   * Update the given admin.
   */

  public IdACommandAdminUpdateCredentials
  {
    Objects.requireNonNull(admin, "admin");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(realName, "realName");
    Objects.requireNonNull(password, "password");
  }

  @Override
  public Class<IdAResponseAdminUpdate> responseClass()
  {
    return IdAResponseAdminUpdate.class;
  }
}
