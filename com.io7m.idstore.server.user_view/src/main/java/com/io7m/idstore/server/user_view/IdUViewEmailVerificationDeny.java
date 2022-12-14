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


package com.io7m.idstore.server.user_view;

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseType;
import com.io7m.idstore.database.api.IdDatabaseUsersQueriesType;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user.IdUCommandEmailAddDeny;
import com.io7m.idstore.protocol.user.IdUCommandEmailRemoveDeny;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCmdEmailAddDeny;
import com.io7m.idstore.server.controller.user.IdUCmdEmailRemoveDeny;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.http.IdCommonInstrumentedServlet;
import com.io7m.idstore.server.http.IdRequestUniqueIDs;
import com.io7m.idstore.server.http.IdRequestUserAgents;
import com.io7m.idstore.server.service.branding.IdServerBrandingServiceType;
import com.io7m.idstore.server.service.sessions.IdSessionSecretIdentifier;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.templating.IdFMMessageData;
import com.io7m.idstore.server.service.templating.IdFMTemplateServiceType;
import com.io7m.idstore.server.service.templating.IdFMTemplateType;
import com.io7m.idstore.services.api.IdServiceDirectoryType;
import com.io7m.jvindicator.core.Vindication;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

import static com.io7m.idstore.database.api.IdDatabaseRole.IDSTORE;

/**
 * The endpoint that allows for completing email verification challenges.
 */

public final class IdUViewEmailVerificationDeny
  extends IdCommonInstrumentedServlet
{
  private final IdDatabaseType database;
  private final IdServerStrings strings;
  private final IdFMTemplateType<IdFMMessageData> template;
  private final IdServiceDirectoryType services;
  private final IdServerBrandingServiceType branding;

  /**
   * The endpoint that allows for completing email verification challenges.
   *
   * @param inServices The services
   */

  public IdUViewEmailVerificationDeny(
    final IdServiceDirectoryType inServices)
  {
    super(Objects.requireNonNull(inServices, "services"));

    this.services =
      Objects.requireNonNull(inServices, "inServices");
    this.database =
      inServices.requireService(IdDatabaseType.class);
    this.strings =
      inServices.requireService(IdServerStrings.class);
    this.branding =
      inServices.requireService(IdServerBrandingServiceType.class);
    this.template =
      inServices.requireService(IdFMTemplateServiceType.class)
        .pageMessage();
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws ServletException, IOException
  {
    try {
      final var vindicator =
        Vindication.startWithExceptions(IdValidityException::new);
      final var tokenParameter =
        vindicator.addRequiredParameter("token", IdToken::new);

      vindicator.check(request.getParameterMap());

      this.runForToken(request, servletResponse, tokenParameter.get());
    } catch (final IdCommandExecutionFailure e) {
      this.showError(
        request,
        servletResponse,
        e.getMessage(),
        e.httpStatusCode() >= 500
      );
    } catch (final IdValidityException e) {
      this.showError(request, servletResponse, e.getMessage(), false);
    } catch (final Exception e) {
      this.showError(request, servletResponse, e.getMessage(), true);
    }
  }

  private void runForToken(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final IdToken token)
    throws
    IdDatabaseException,
    IOException,
    IdCommandExecutionFailure,
    InterruptedException
  {
    try (var connection =
           this.database.openConnection(IDSTORE)) {
      try (var transaction =
             connection.openTransaction()) {
        final var emails =
          transaction.queries(IdDatabaseEmailsQueriesType.class);
        final var users =
          transaction.queries(IdDatabaseUsersQueriesType.class);
        final var verificationOpt =
          emails.emailVerificationGet(token);

        if (verificationOpt.isEmpty()) {
          this.showError(
            request,
            servletResponse,
            this.strings.format("notFound"),
            false
          );
          return;
        }

        final var verification =
          verificationOpt.get();
        final var user =
          users.userGetRequire(verification.user());

        /*
         * Create a fake user session in order to run the command. The session
         * is not entered into the session service and so can't be used for
         * any other commands.
         */

        final var fakeSession =
          new IdSessionUser(
            user.id(),
            IdSessionSecretIdentifier.generate()
          );

        final var commandContext =
          new IdUCommandContext(
            this.services,
            IdRequestUniqueIDs.requestIdFor(request),
            transaction,
            fakeSession,
            user,
            request.getRemoteAddr(),
            IdRequestUserAgents.requestUserAgent(request)
          );

        switch (verification.operation()) {
          case EMAIL_ADD -> {
            final var command =
              new IdUCommandEmailAddDeny(new IdToken(token.value()));
            new IdUCmdEmailAddDeny()
              .execute(commandContext, command);
          }
          case EMAIL_REMOVE -> {
            final var command =
              new IdUCommandEmailRemoveDeny(new IdToken(token.value()));
            new IdUCmdEmailRemoveDeny()
              .execute(commandContext, command);
          }
        }

        transaction.commit();
        this.showSuccess(request, servletResponse);
      }
    }
  }

  private void showSuccess(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse)
    throws IOException
  {
    try (var writer = servletResponse.getWriter()) {
      this.template.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format(
            "emailVerificationSuccessTitle")),
          this.branding.title(),
          IdRequestUniqueIDs.requestIdFor(request),
          false,
          false,
          this.strings.format("emailVerificationSuccessTitle"),
          this.strings.format("emailVerificationSuccess"),
          "/"
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }
  }

  private void showError(
    final HttpServletRequest request,
    final HttpServletResponse servletResponse,
    final String message,
    final boolean isServerError)
    throws IOException
  {
    try (var writer = servletResponse.getWriter()) {
      if (isServerError) {
        servletResponse.setStatus(500);
      } else {
        servletResponse.setStatus(400);
      }

      this.template.process(
        new IdFMMessageData(
          this.branding.htmlTitle(this.strings.format("error")),
          this.branding.title(),
          IdRequestUniqueIDs.requestIdFor(request),
          true,
          isServerError,
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

