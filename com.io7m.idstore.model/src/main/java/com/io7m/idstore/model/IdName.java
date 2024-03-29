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

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A user ID name. This is a unique, short name that can be specified as the
 * identifier used for logging in. It is analogous to a UNIX username.
 *
 * @param value The name value
 */

public record IdName(String value)
  implements Comparable<IdName>
{
  /**
   * The pattern that defines valid ID names.
   */

  public static final Pattern VALID_ID_NAME =
    Pattern.compile("\\p{LC}[\\p{LC}\\p{N}_-]{0,255}");

  /**
   * A user display name.
   *
   * @param value The name value
   */

  public IdName
  {
    Objects.requireNonNull(value, "value");

    if (!VALID_ID_NAME.matcher(value).matches()) {
      throw new IdValidityException(
        "ID name '%s' must match %s".formatted(value, VALID_ID_NAME)
      );
    }
  }

  @Override
  public int hashCode()
  {
    return this.value.toUpperCase(Locale.ROOT).hashCode();
  }

  @Override
  public boolean equals(
    final Object obj)
  {
    if (obj == null) {
      return false;
    }
    if (obj instanceof final IdName other) {
      return this.value.equalsIgnoreCase(other.value);
    }
    return false;
  }

  @Override
  public String toString()
  {
    return this.value;
  }

  @Override
  public int compareTo(
    final IdName other)
  {
    final var thisV =
      this.value.toUpperCase(Locale.ROOT);
    final var thatV =
      other.value.toUpperCase(Locale.ROOT);

    return thisV.compareTo(thatV);
  }
}
