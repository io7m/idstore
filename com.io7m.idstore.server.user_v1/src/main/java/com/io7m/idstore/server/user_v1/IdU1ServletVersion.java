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


package com.io7m.idstore.server.user_v1;

import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.nio.charset.StandardCharsets;

import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;

/**
 * A servlet for showing the server version.
 */

public final class IdU1ServletVersion
  extends IdHTTPServletFunctional
{
  /**
   * A servlet for showing the server version.
   *
   * @param services The services
   */

  public IdU1ServletVersion(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    return (request, information) -> {
      return withInstrumentation(
        services,
        (req0, info0) -> {
          return execute();
        }
      ).execute(request, information);
    };
  }

  private static IdHTTPServletResponseType execute()
  {
    final var text =
      String.format(
        "com.io7m.idstore %s %s\r\n\r\n",
        IdU1Version.SERVER_VERSION,
        IdU1Version.SERVER_BUILD
      );

    return new IdHTTPServletResponseFixedSize(
      200,
      "text/plain",
      text.getBytes(StandardCharsets.UTF_8)
    );
  }
}