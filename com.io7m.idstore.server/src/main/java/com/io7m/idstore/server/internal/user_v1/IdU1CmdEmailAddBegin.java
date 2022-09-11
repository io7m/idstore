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


package com.io7m.idstore.server.internal.user_v1;

import com.io7m.idstore.database.api.IdDatabaseEmailsQueriesType;
import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdEmailVerification;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdValidityException;
import com.io7m.idstore.protocol.user_v1.IdU1CommandEmailAddBegin;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseEmailAddBegin;
import com.io7m.idstore.protocol.user_v1.IdU1ResponseType;
import com.io7m.idstore.server.api.IdServerConfiguration;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.internal.IdServerBrandingService;
import com.io7m.idstore.server.internal.IdServerConfigurationService;
import com.io7m.idstore.server.internal.IdServerMailService;
import com.io7m.idstore.server.internal.IdServerStrings;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.internal.command_exec.IdCommandExecutorType;
import com.io7m.idstore.server.internal.freemarker.IdFMEmailVerificationData;
import com.io7m.idstore.server.internal.freemarker.IdFMTemplateService;
import com.io7m.idstore.server.security.IdSecPolicyResultDenied;
import com.io7m.idstore.server.security.IdSecUserActionEmailAddBegin;
import com.io7m.idstore.server.security.IdSecurity;
import com.io7m.idstore.server.security.IdSecurityException;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.EMAIL_DUPLICATE;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.SECURITY_POLICY_DENIED;
import static com.io7m.idstore.model.IdEmailVerificationOperation.EMAIL_ADD;
import static org.eclipse.jetty.http.HttpStatus.FORBIDDEN_403;

/**
 * IdU1CmdEmailAddBegin
 */

public final class IdU1CmdEmailAddBegin
  implements IdCommandExecutorType<
  IdU1CommandContext, IdU1CommandEmailAddBegin, IdU1ResponseType>
{
  /**
   * IdU1CmdEmailAddBegin
   */

  public IdU1CmdEmailAddBegin()
  {

  }

  @Override
  public IdU1ResponseType execute(
    final IdU1CommandContext context,
    final IdU1CommandEmailAddBegin command)
    throws IdCommandExecutionFailure, IOException, InterruptedException
  {
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(command, "command");

    try {
      final var services =
        context.services();
      final var templateService =
        services.requireService(IdFMTemplateService.class);
      final var configurationService =
        services.requireService(IdServerConfigurationService.class);
      final var mailService =
        services.requireService(IdServerMailService.class);
      final var strings =
        services.requireService(IdServerStrings.class);
      final var brandingService =
        services.requireService(IdServerBrandingService.class);

      final var configuration =
        configurationService.configuration();
      final var mailConfiguration =
        configuration.mailConfiguration();

      final var user = context.user();
      if (IdSecurity.check(new IdSecUserActionEmailAddBegin(user))
        instanceof IdSecPolicyResultDenied denied) {
        throw context.fail(
          FORBIDDEN_403,
          SECURITY_POLICY_DENIED,
          denied.message()
        );
      }

      final var email =
        new IdEmail(command.email());
      final var transaction =
        context.transaction();
      final var emails =
        transaction.queries(IdDatabaseEmailsQueriesType.class);

      checkPreconditions(context, emails, strings, email);

      transaction.userIdSet(user.id());

      final var verification =
        createVerification(context, emails, mailConfiguration, user, email);

      sendVerificationMail(
        context,
        templateService,
        configuration,
        mailService,
        brandingService,
        email,
        verification
      );

      return new IdU1ResponseEmailAddBegin(context.requestId());
    } catch (final IdValidityException e) {
      throw context.failValidity(e);
    } catch (final IdSecurityException e) {
      throw context.failSecurity(e);
    } catch (final IdDatabaseException e) {
      throw context.failDatabase(e);
    }
  }

  private static void sendVerificationMail(
    final IdU1CommandContext context,
    final IdFMTemplateService templateService,
    final IdServerConfiguration configuration,
    final IdServerMailService mailService,
    final IdServerBrandingService brandingService,
    final IdEmail email,
    final IdEmailVerification verification)
    throws
    IOException,
    IdCommandExecutionFailure
  {
    final var template =
      templateService.emailVerificationTemplate();

    final var linkPermit =
      configuration.userViewAddress()
        .externalAddress()
        .resolve("/email-verification-permit/?token=%s".formatted(verification.token()))
        .normalize();

    final var linkDeny =
      configuration.userViewAddress()
        .externalAddress()
        .resolve("/email-verification-deny/?token=%s".formatted(verification.token()))
        .normalize();

    final var writer = new StringWriter();
    try {
      template.process(
        new IdFMEmailVerificationData(
          brandingService.title(),
          verification,
          context.remoteHost(),
          context.remoteUserAgent(),
          linkPermit,
          linkDeny
        ),
        writer
      );
    } catch (final TemplateException e) {
      throw new IOException(e);
    }

    final var mailHeaders =
      Map.ofEntries(
        Map.entry(
          "X-IDStore-Verification-Token",
          verification.token().value()),
        Map.entry(
          "X-IDStore-Verification-From-Request",
          context.requestId().toString()),
        Map.entry(
          "X-IDStore-Verification-Permit",
          linkPermit.toString()),
        Map.entry(
          "X-IDStore-Verification-Deny",
          linkDeny.toString())
      );

    try {
      mailService.sendMail(
        context.requestId(),
        email,
        mailHeaders,
        "[idstore] Email verification request",
        writer.toString()
      ).get();
    } catch (final Exception e) {
      throw context.failMail(email, e);
    }
  }

  private static IdEmailVerification createVerification(
    final IdU1CommandContext context,
    final IdDatabaseEmailsQueriesType emails,
    final IdServerMailConfiguration mailConfiguration,
    final IdUser user,
    final IdEmail email)
    throws IdDatabaseException
  {
    final var token =
      IdToken.generate();
    final var expires =
      context.now().plus(mailConfiguration.verificationExpiration());
    final var verification =
      new IdEmailVerification(user.id(), email, token, EMAIL_ADD, expires);

    emails.emailVerificationCreate(verification);
    return verification;
  }

  private static void checkPreconditions(
    final IdU1CommandContext context,
    final IdDatabaseEmailsQueriesType emails,
    final IdServerStrings strings,
    final IdEmail email)
    throws IdDatabaseException, IdCommandExecutionFailure
  {
    final var existingOpt = emails.emailExists(email);
    if (existingOpt.isPresent()) {
      throw new IdCommandExecutionFailure(
        strings.format("emailDuplicate"),
        context.requestId(),
        400,
        EMAIL_DUPLICATE
      );
    }
  }
}