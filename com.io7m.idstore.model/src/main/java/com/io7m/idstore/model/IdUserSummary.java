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

package com.io7m.idstore.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A summary of a user.
 *
 * @param id          The user ID
 * @param idName      The user's ID name
 * @param realName    The user's real name
 * @param timeCreated The time the user was created
 * @param timeUpdated The time the user was updated
 */

public record IdUserSummary(
  UUID id,
  IdName idName,
  IdRealName realName,
  OffsetDateTime timeCreated,
  OffsetDateTime timeUpdated)
{
  /**
   * A summary of a user.
   *
   * @param id          The user ID
   * @param idName      The user's ID name
   * @param realName    The user name
   * @param timeCreated The time the user was created
   * @param timeUpdated The time the user was updated
   */

  public IdUserSummary
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(idName, "idName");
    Objects.requireNonNull(realName, "realName");
    Objects.requireNonNull(timeCreated, "created");
    Objects.requireNonNull(timeUpdated, "timeUpdated");
  }
}
