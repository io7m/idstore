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

package com.io7m.idstore.server.service.templating;

import com.io7m.idstore.server.api.IdServerColorScheme;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A color scheme.
 *
 * @param colorScheme The color scheme
 */

public record IdFMCSSData(
  IdServerColorScheme colorScheme
) implements IdFMDataModelType
{
  /**
   * A color scheme.
   *
   * @param colorScheme The color scheme
   */

  public IdFMCSSData
  {
    Objects.requireNonNull(colorScheme, "colorScheme");
  }

  @Override
  public Map<String, Object> toTemplateHash()
  {
    final var m = new HashMap<String, Object>(32);
    m.put("colors", this.colorScheme);
    return m;
  }
}
