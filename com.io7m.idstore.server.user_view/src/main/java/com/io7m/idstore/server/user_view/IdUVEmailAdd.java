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

import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMEmailAddData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

import static com.io7m.idstore.server.user_view.IdUVServletCoreAuthenticated.withAuthentication;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The page that displays the email addition form.
 */

public final class IdUVEmailAdd extends IdHTTPServletFunctional
{
  /**
   * The page that displays the email addition form.
   *
   * @param services The services
   */

  public IdUVEmailAdd(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var template =
      services.requireService(IdFMTemplateServiceType.class)
        .pageEmailAddTemplate();

    return withAuthentication(
      services,
      (request, information, session, user) -> {
        return execute(
          branding,
          template,
          information
        );
      });
  }

  private static IdHTTPServletResponseType execute(
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMEmailAddData> template,
    final IdHTTPServletRequestInformation information)
  {
    try (var writer = new StringWriter()) {
      template.process(
        new IdFMEmailAddData(
          branding.htmlTitle("Add an email address."),
          branding.title(),
          information.requestId()
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