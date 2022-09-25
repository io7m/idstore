/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.idstore.server.internal.user_view;

import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.server.internal.IdRequests;
import com.io7m.idstore.server.internal.IdServerBrandingService;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.IdUserPasswordResetService;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.common.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.internal.freemarker.IdFMMessageData;
import com.io7m.idstore.server.internal.freemarker.IdFMPasswordResetConfirmData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

import static com.io7m.idstore.server.internal.IdServerRequestDecoration.requestIdFor;

/**
 * The page that checks a password reset token and prompts a user to enter a new
 * password.
 */

public final class IdUViewPasswordResetConfirm
  extends IdCommonInstrumentedServlet
{
  private final IdUserPasswordResetService userPasswordResets;
  private final IdServerStrings strings;
  private final IdServerBrandingService branding;
  private final IdFMTemplateType<IdFMMessageData> errorTemplate;
  private final IdFMTemplateType<IdFMPasswordResetConfirmData> formTemplate;

  /**
   * The page that checks a password reset token and prompts a user to enter a
   * new password.
   *
   * @param inServices The service directory
   */

  public IdUViewPasswordResetConfirm(
    final IdServiceDirectoryType inServices)
  {
    super(inServices);

    this.userPasswordResets =
      inServices.requireService(IdUserPasswordResetService.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.branding =
      inServices.requireService(IdServerBrandingService.class);
    this.errorTemplate =
      inServices.requireService(IdFMTemplateService.class)
        .pageMessage();
    this.formTemplate =
      inServices.requireService(IdFMTemplateService.class)
        .pagePasswordResetConfirmTemplate();
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws ServletException, IOException
  {
    final var tokenParameter =
      getParameterOrEmpty(request, "token");

    try {
      final var token =
        this.userPasswordResets.resetCheck(
          request.getRemoteHost(),
          IdRequests.requestUserAgent(request),
          requestIdFor(request),
          Optional.ofNullable(tokenParameter)
        );

      this.showPasswordForm(response, token);
    } catch (final IdCommandExecutionFailure e) {
      this.showError(
        request,
        response,
        e.httpStatusCode(),
        e.getMessage()
      );
    }
  }

  private void showPasswordForm(
    final HttpServletResponse response,
    final IdToken token)
    throws IOException
  {
    response.setStatus(200);

    try (var writer = response.getWriter()) {
      this.formTemplate.process(
        new IdFMPasswordResetConfirmData(
          this.branding.htmlTitle("Reset password."),
          this.branding.title(),
          token
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private static String getParameterOrEmpty(
    final HttpServletRequest request,
    final String key)
  {
    final var value = request.getParameter(key);
    if (value != null) {
      if (value.isBlank()) {
        return null;
      }
    }
    return value;
  }

  private void showError(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final int statusCode,
    final String message)
    throws IOException
  {
    response.setStatus(statusCode);

    try (var writer = response.getWriter()) {
      this.errorTemplate.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format("error")),
          this.branding.title(),
          requestIdFor(request),
          true,
          statusCode >= 500,
          this.strings.format("error"),
          message,
          "/"
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }
}