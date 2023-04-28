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

package com.io7m.idstore.server.http;

import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

/**
 * The logo servlet.
 */

public final class IdCommonLogoServlet extends IdCommonInstrumentedServlet
{
  private final IdServerBrandingServiceType branding;

  /**
   * The logo servlet.
   *
   * @param inServices The service directory
   */

  public IdCommonLogoServlet(
    final RPServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.branding =
      inServices.requireService(IdServerBrandingServiceType.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws IOException
  {
    servletResponse.setStatus(200);
    servletResponse.setContentType("image/svg+xml; charset=utf-8");
    try (var output = servletResponse.getOutputStream()) {
      output.write(this.branding.logoImage());
      output.flush();
    }
  }
}
