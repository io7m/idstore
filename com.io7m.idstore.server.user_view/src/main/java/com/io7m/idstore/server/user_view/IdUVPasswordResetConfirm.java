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

import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user_pwreset.IdUserPasswordResetServiceType;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMPasswordResetConfirmData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Optional;

import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The page that checks a password reset token and prompts a user to enter a new
 * password.
 */

public final class IdUVPasswordResetConfirm
  extends IdHTTPServletFunctional
{
  /**
   * The page that checks a password reset token and prompts a user to enter a
   * new password.
   *
   * @param services The services
   */

  public IdUVPasswordResetConfirm(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var userPasswordResets =
      services.requireService(IdUserPasswordResetServiceType.class);
    final var strings =
      services.requireService(IdServerStrings.class);
    final var branding =
      services.requireService(IdServerBrandingServiceType.class);
    final var errorTemplate =
      services.requireService(IdFMTemplateServiceType.class)
        .pageMessage();
    final var formTemplate =
      services.requireService(IdFMTemplateServiceType.class)
        .pagePasswordResetConfirmTemplate();

    return withInstrumentation(services, (request, information) -> {
      return execute(
        userPasswordResets,
        strings,
        branding,
        errorTemplate,
        formTemplate,
        request,
        information
      );
    });
  }

  private static IdHTTPServletResponseType execute(
    final IdUserPasswordResetServiceType userPasswordResets,
    final IdServerStrings strings,
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMMessageData> errorTemplate,
    final IdFMTemplateType<IdFMPasswordResetConfirmData> formTemplate,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information)
  {
    final var tokenParameter =
      getParameterOrEmpty(request, "token");

    try {
      final var token =
        userPasswordResets.resetCheck(
          information.remoteAddress(),
          information.userAgent(),
          information.requestId(),
          tokenParameter
        );

      return showPasswordForm(branding, formTemplate, token);
    } catch (final IdCommandExecutionFailure e) {
      setSpanErrorCode(e.errorCode());
      return IdUVErrorPage.showError(
        strings,
        branding,
        errorTemplate,
        information,
        e.httpStatusCode(),
        e.getMessage(),
        "/"
      );
    }
  }

  private static IdHTTPServletResponseType showPasswordForm(
    final IdServerBrandingServiceType branding,
    final IdFMTemplateType<IdFMPasswordResetConfirmData> formTemplate,
    final IdToken token)
  {
    try (var writer = new StringWriter()) {
      formTemplate.process(
        new IdFMPasswordResetConfirmData(
          branding.htmlTitle("Reset password."),
          branding.title(),
          token
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

  private static Optional<String> getParameterOrEmpty(
    final HttpServletRequest request,
    final String key)
  {
    final var value = request.getParameter(key);
    if (value == null) {
      return Optional.empty();
    }
    if (value.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(value);
  }
}
