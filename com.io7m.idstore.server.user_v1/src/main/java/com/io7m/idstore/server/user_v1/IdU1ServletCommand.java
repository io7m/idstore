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

import com.io7m.idstore.database.api.IdDatabaseException;
import com.io7m.idstore.database.api.IdDatabaseTransactionType;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.model.IdUserDomain;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.server.controller.IdServerStrings;
import com.io7m.idstore.server.controller.command_exec.IdCommandExecutionFailure;
import com.io7m.idstore.server.controller.user.IdUCommandContext;
import com.io7m.idstore.server.controller.user.IdUCommandExecutor;
import com.io7m.idstore.server.http.IdHTTPServletFunctional;
import com.io7m.idstore.server.http.IdHTTPServletFunctionalCoreType;
import com.io7m.idstore.server.http.IdHTTPServletRequestInformation;
import com.io7m.idstore.server.http.IdHTTPServletResponseFixedSize;
import com.io7m.idstore.server.http.IdHTTPServletResponseType;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimitExceeded;
import com.io7m.idstore.server.service.reqlimit.IdRequestLimits;
import com.io7m.idstore.server.service.sessions.IdSessionUser;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.API_MISUSE_ERROR;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_CLIENT;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.server.http.IdHTTPServletCoreInstrumented.withInstrumentation;
import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType.setSpanErrorCode;
import static com.io7m.idstore.server.user_v1.IdU1Errors.errorResponseOf;
import static com.io7m.idstore.server.user_v1.IdU1ServletCoreAuthenticated.withAuthentication;
import static com.io7m.idstore.server.user_v1.IdU1ServletCoreTransactional.withTransaction;

/**
 * The v1 command servlet.
 */

public final class IdU1ServletCommand extends IdHTTPServletFunctional
{
  /**
   * The v1 command servlet.
   *
   * @param services The services
   */

  public IdU1ServletCommand(
    final RPServiceDirectoryType services)
  {
    super(createCore(services));
  }

  private static IdHTTPServletFunctionalCoreType createCore(
    final RPServiceDirectoryType services)
  {
    final var limits =
      services.requireService(IdRequestLimits.class);
    final var messages =
      services.requireService(IdUCB1Messages.class);
    final var strings =
      services.requireService(IdServerStrings.class);
    final var telemetry =
      services.requireService(IdServerTelemetryServiceType.class);

    return (request, information) -> {
      return withInstrumentation(
        services,
        IdUserDomain.USER,
        (req0, info0) -> {
          return withAuthentication(
            services,
            (req1, info1, session, user) -> {
              return withTransaction(
                services,
                (req2, info2, transaction) -> {
                  return execute(
                    services,
                    req2,
                    info2,
                    messages,
                    telemetry,
                    limits,
                    strings,
                    session,
                    user,
                    transaction
                  );
                }).execute(req1, info1);
            }).execute(req0, info0);
        }).execute(request, information);
    };
  }

  private static IdHTTPServletResponseType execute(
    final RPServiceDirectoryType services,
    final HttpServletRequest request,
    final IdHTTPServletRequestInformation information,
    final IdUCB1Messages messages,
    final IdServerTelemetryServiceType telemetry,
    final IdRequestLimits limits,
    final IdServerStrings strings,
    final IdSessionUser session,
    final IdUser user,
    final IdDatabaseTransactionType transaction)
  {
    try (var input = limits.boundedMaximumInput(request, 1048576)) {
      final var message =
        parseMessage(telemetry, messages, input);

      if (message instanceof final IdUCommandType<?> command) {
        return executeCommand(
          services,
          information,
          messages,
          telemetry,
          session,
          user,
          command,
          transaction
        );
      }

      return errorResponseOf(
        messages,
        information,
        BLAME_CLIENT,
        new IdProtocolException(
          strings.format("commandNotHere"),
          API_MISUSE_ERROR,
          Map.of(),
          Optional.empty()
        )
      );

    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } catch (final IdRequestLimitExceeded | IdProtocolException e) {
      setSpanErrorCode(e.errorCode());
      return errorResponseOf(messages, information, BLAME_CLIENT, e);
    } catch (final IdDatabaseException e) {
      setSpanErrorCode(e.errorCode());
      return errorResponseOf(messages, information, BLAME_SERVER, e);
    }
  }

  private static IdUMessageType parseMessage(
    final IdServerTelemetryServiceType telemetry,
    final IdUCB1Messages messages,
    final InputStream input)
    throws IOException, IdProtocolException
  {
    final var parseSpan =
      telemetry.tracer()
        .spanBuilder("ParseMessage")
        .startSpan();

    try (var ignored = parseSpan.makeCurrent()) {
      final var data = parseMessageReadData(telemetry, input);
      return parseMessageDeserialize(telemetry, messages, data);
    } finally {
      parseSpan.end();
    }
  }

  private static IdUMessageType parseMessageDeserialize(
    final IdServerTelemetryServiceType telemetry,
    final IdUCB1Messages messages,
    final byte[] data)
    throws IdProtocolException
  {
    final var readSpan =
      telemetry.tracer()
        .spanBuilder("Deserialize")
        .startSpan();

    try (var ignored = readSpan.makeCurrent()) {
      return messages.parse(data);
    } finally {
      readSpan.end();
    }
  }

  private static byte[] parseMessageReadData(
    final IdServerTelemetryServiceType telemetry,
    final InputStream input)
    throws IOException
  {
    final var readSpan =
      telemetry.tracer()
        .spanBuilder("Read")
        .startSpan();

    try (var ignored = readSpan.makeCurrent()) {
      return input.readAllBytes();
    } finally {
      readSpan.end();
    }
  }

  private static IdHTTPServletResponseType executeCommand(
    final RPServiceDirectoryType services,
    final IdHTTPServletRequestInformation information,
    final IdUCB1Messages messages,
    final IdServerTelemetryServiceType telemetry,
    final IdSessionUser session,
    final IdUser user,
    final IdUCommandType<?> command,
    final IdDatabaseTransactionType transaction)
    throws IdDatabaseException
  {
    final var executor =
      new IdUCommandExecutor();

    final var context =
      new IdUCommandContext(
        services,
        information.requestId(),
        transaction,
        session,
        user,
        information.remoteAddress(),
        information.userAgent()
      );

    final IdUResponseType result;
    try {
      result = executor.execute(context, command);
    } catch (final IdCommandExecutionFailure e) {
      setSpanErrorCode(e.errorCode());
      return errorResponseOf(messages, information, e);
    }

    if (result instanceof final IdUResponseError error) {
      setSpanErrorCode(error.errorCode());
      return new IdHTTPServletResponseFixedSize(
        switch (error.blame()) {
          case BLAME_SERVER -> 500;
          case BLAME_CLIENT -> 400;
        },
        IdUCB1Messages.contentType(),
        messages.serialize(error)
      );
    }

    commit(telemetry, transaction);
    return new IdHTTPServletResponseFixedSize(
      200,
      IdUCB1Messages.contentType(),
      messages.serialize(result)
    );
  }

  private static void commit(
    final IdServerTelemetryServiceType telemetry,
    final IdDatabaseTransactionType transaction)
    throws IdDatabaseException
  {
    final var commitSpan =
      telemetry.tracer()
        .spanBuilder("Commit")
        .startSpan();

    try (var ignored = commitSpan.makeCurrent()) {
      transaction.commit();
    } finally {
      commitSpan.end();
    }
  }
}
