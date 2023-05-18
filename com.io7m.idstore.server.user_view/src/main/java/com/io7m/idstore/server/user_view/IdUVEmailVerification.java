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


package com.io7m.idstore.server.user_view;

import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Common email verification functions.
 */

public final class IdUVEmailVerification
{
  private static final String DESTINATION_ON_SUCCESS = "/";

  private IdUVEmailVerification()
  {

  }

  /**
   * Show that verification succeeded.
   *
   * @param strings     The string resources
   * @param branding    The branding resources
   * @param msgTemplate The message template
   * @param information The request information
   *
   * @return The formatted response
   */

  static IdHTTPServletResponseType showSuccess(
    final IdServerStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> msgTemplate,
    final IdHTTPServletRequestInformation information)
  {
    try (var writer = new StringWriter()) {
      msgTemplate.process(
        new IdFMMessageData(
          branding.htmlTitle(strings.format("emailVerificationSuccessTitle")),
          branding.title(),
          information.requestId(),
          false,
          false,
          strings.format("emailVerificationSuccessTitle"),
          strings.format("emailVerificationSuccess"),
          DESTINATION_ON_SUCCESS
        ),
        writer
      );
      writer.flush();
      return new IdHTTPServletResponseFixedSize(
        200,
        IdUVContentTypes.xhtml(),
        writer.toString().getBytes(UTF_8)
      );
    } catch (final TemplateException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}