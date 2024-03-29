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

import java.util.Objects;
import java.util.Optional;

/**
 * A "redacted" hashing algorithm used to represent passwords that should not be
 * returned to users/admins.
 */

public final class IdPasswordAlgorithmRedacted
  implements IdPasswordAlgorithmType
{
  private static final IdPasswordAlgorithmRedacted INSTANCE =
    new IdPasswordAlgorithmRedacted();

  private IdPasswordAlgorithmRedacted()
  {

  }

  /**
   * Create an algorithm with the given iteration count and key length.
   *
   * @return An algorithm
   */

  public static IdPasswordAlgorithmRedacted create()
  {
    return INSTANCE;
  }

  @Override
  public boolean check(
    final String expectedHash,
    final String receivedPassword,
    final byte[] salt)
  {
    Objects.requireNonNull(expectedHash, "expectedHash");
    Objects.requireNonNull(receivedPassword, "receivedPassword");
    Objects.requireNonNull(salt, "salt");

    return false;
  }

  @Override
  public IdPassword createHashed(
    final String passwordText,
    final byte[] salt)
  {
    Objects.requireNonNull(passwordText, "passwordText");
    Objects.requireNonNull(salt, "salt");

    return new IdPassword(
      this,
      "0",
      "DEADBEEF",
      Optional.empty()
    );
  }

  @Override
  public String identifier()
  {
    return "REDACTED";
  }
}
