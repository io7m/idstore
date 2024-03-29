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

package com.io7m.idstore.user_client.internal;

import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultSuccess;
import com.io7m.hibiscus.api.HBResultType;
import com.io7m.hibiscus.basic.HBClientNewHandler;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdVersion;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientCredentials;
import com.io7m.idstore.user_client.api.IdUClientEventType;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.junreachable.UnreachableCodeException;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_CLIENT;
import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_SERVER;
import static com.io7m.idstore.strings.IdStringConstants.CONNECT_FAILURE;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_UNEXPECTED_CONTENT_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.ERROR_UNEXPECTED_RESPONSE_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.EXPECTED_CONTENT_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.EXPECTED_RESPONSE_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.RECEIVED_CONTENT_TYPE;
import static com.io7m.idstore.strings.IdStringConstants.RECEIVED_RESPONSE_TYPE;
import static com.io7m.idstore.user_client.internal.IdUCompression.decompressResponse;
import static com.io7m.idstore.user_client.internal.IdUUUIDs.nullUUID;
import static java.util.Objects.requireNonNullElse;

/**
 * The version 1 protocol handler.
 */

public final class IdUHandler1 extends IdUHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdUHandler1.class);

  private final IdUCB1Messages messages;
  private final URI loginURI;
  private final URI commandURI;
  private final boolean connected;
  private IdUCommandLogin mostRecentLogin;

  /**
   * The protocol 1 handler.
   *
   * @param inConfiguration The client configuration
   * @param inStrings       String resources
   * @param inHttpClient    The HTTP client
   * @param baseURI         The base URI returned by the server during version
   *                        negotiation
   */

  IdUHandler1(
    final IdUClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final HttpClient inHttpClient,
    final URI baseURI)
  {
    super(inConfiguration, inStrings, inHttpClient);

    this.messages =
      new IdUCB1Messages();
    this.loginURI =
      baseURI.resolve("login")
        .normalize();
    this.commandURI =
      baseURI.resolve("command")
        .normalize();

    this.connected = false;
  }

  private static boolean isAuthenticationError(
    final IdUResponseError error)
  {
    return Objects.equals(
      error.errorCode(),
      IdStandardErrorCodes.AUTHENTICATION_ERROR
    );
  }

  private static String userAgent()
  {
    return "com.io7m.idstore.client/%s (%s)"
      .formatted(IdVersion.MAIN_VERSION, IdVersion.MAIN_BUILD);
  }

  private <R extends IdUResponseType, C extends IdUCommandType<R>>
  HBResultType<R, IdUResponseError>
  send(
    final int attempt,
    final URI uri,
    final boolean isLoggingIn,
    final C message)
    throws InterruptedException
  {
    try {
      final var commandType = message.getClass().getSimpleName();
      LOG.debug("sending {} to {}", commandType, uri);

      final var sendBytes =
        this.messages.serialize(message);

      final HttpRequest.Builder builder =
        HttpRequest.newBuilder(uri)
          .header("User-Agent", userAgent());

      /*
       * Inject any required trace propagation headers.
       */

      this.configuration()
        .openTelemetry()
        .getPropagators()
        .getTextMapPropagator()
        .inject(Context.current(), builder, (b, name, value) -> {
          if (LOG.isTraceEnabled()) {
            LOG.trace("injecting header {} -> {}", name, value);
          }
          builder.header(name, value);
        });

      final var request =
        builder.POST(HttpRequest.BodyPublishers.ofByteArray(sendBytes))
          .build();

      final var response =
        this.httpClient()
          .send(request, HttpResponse.BodyHandlers.ofByteArray());

      LOG.debug("server: status {}", response.statusCode());

      final var responseHeaders =
        response.headers();

      /*
       * Check the content type. Fail if it's not what we expected.
       */

      final var contentType =
        responseHeaders.firstValue("content-type")
          .orElse("application/octet-stream");

      final var expectedContentType = IdUCB1Messages.contentType();
      if (!contentType.equals(expectedContentType)) {
        return this.errorContentType(contentType, expectedContentType);
      }

      /*
       * Parse the response message, decompressing if necessary. If the
       * parsed message isn't a response... fail.
       */

      final var responseMessage =
        this.messages.parse(decompressResponse(response, responseHeaders));

      if (!(responseMessage instanceof final IdUResponseType responseActual)) {
        return this.errorUnexpectedResponseType(message, responseMessage);
      }

      /*
       * If the response is an error, then perhaps retry. We only attempt
       * to retry if the response indicates an authentication error; if this
       * happens, we try to log in again and then re-send the original message.
       *
       * We don't try to blanket re-send any message that "failed" because
       * messages might have side effects on the server.
       */

      if (responseActual instanceof final IdUResponseError error) {
        if (attempt < 3) {
          if (isAuthenticationError(error) && !isLoggingIn) {
            return this.reLoginAndSend(attempt, uri, message);
          }
        }
        return new HBResultFailure<>(error);
      }

      /*
       * We know that the response is an error, but we don't know that the
       * response is of the expected type. Check that here, and fail if it
       * isn't.
       */

      if (!Objects.equals(responseActual.getClass(), message.responseClass())) {
        return this.errorUnexpectedResponseType(message, responseActual);
      }

      return new HBResultSuccess<>(
        message.responseClass().cast(responseMessage)
      );

    } catch (final IdProtocolException e) {
      LOG.debug("protocol exception: ", e);
      return new HBResultFailure<>(
        new IdUResponseError(
          nullUUID(),
          e.message(),
          e.errorCode(),
          e.attributes(),
          Optional.empty(),
          BLAME_SERVER
        )
      );
    } catch (final IOException e) {
      LOG.debug("i/o exception: ", e);
      return new HBResultFailure<>(
        new IdUResponseError(
          nullUUID(),
          requireNonNullElse(
            e.getMessage(),
            this.local(CONNECT_FAILURE)
          ),
          IO_ERROR,
          Map.of(),
          Optional.empty(),
          BLAME_CLIENT
        )
      );
    }
  }

  private <R extends IdUResponseType, C extends IdUCommandType<R>> HBResultType<R, IdUResponseError>
  reLoginAndSend(
    final int attempt,
    final URI uri,
    final C message)
    throws InterruptedException
  {
    LOG.debug("attempting re-login");
    final var loginResponse =
      this.sendLogin(this.mostRecentLogin);

    if (loginResponse instanceof HBResultSuccess<IdUResponseLogin, IdUResponseError>) {
      return this.send(
        attempt + 1,
        uri,
        false,
        message
      );
    }
    if (loginResponse instanceof final HBResultFailure<IdUResponseLogin, IdUResponseError> failure) {
      return failure.cast();
    }

    throw new UnreachableCodeException();
  }

  private HBResultType<IdUResponseLogin, IdUResponseError> sendLogin(
    final IdUCommandLogin login)
    throws InterruptedException
  {
    return this.send(1, this.loginURI, true, login);
  }

  private <R extends IdUResponseType> HBResultFailure<R, IdUResponseError> errorContentType(
    final String contentType,
    final String expectedContentType)
  {
    final var attributes = new HashMap<String, String>();
    attributes.put(
      this.local(EXPECTED_CONTENT_TYPE),
      expectedContentType
    );
    attributes.put(
      this.local(RECEIVED_CONTENT_TYPE),
      contentType
    );

    return new HBResultFailure<>(
      new IdUResponseError(
        nullUUID(),
        this.local(ERROR_UNEXPECTED_CONTENT_TYPE),
        PROTOCOL_ERROR,
        attributes,
        Optional.empty(),
        BLAME_SERVER
      )
    );
  }

  private <R extends IdUResponseType, C extends IdUCommandType<R>>
  HBResultFailure<R, IdUResponseError>
  errorUnexpectedResponseType(
    final C message,
    final IdUMessageType responseActual)
  {
    final var attributes = new HashMap<String, String>();
    attributes.put(
      this.local(EXPECTED_RESPONSE_TYPE),
      message.responseClass().getSimpleName()
    );
    attributes.put(
      this.local(RECEIVED_RESPONSE_TYPE),
      responseActual.getClass().getSimpleName()
    );

    return new HBResultFailure<>(
      new IdUResponseError(
        nullUUID(),
        this.local(ERROR_UNEXPECTED_RESPONSE_TYPE),
        PROTOCOL_ERROR,
        attributes,
        Optional.empty(),
        BLAME_SERVER
      )
    );
  }

  @Override
  public boolean onIsConnected()
  {
    return this.connected;
  }

  @Override
  public List<IdUClientEventType> onPollEvents()
  {
    return List.of();
  }


  @Override
  public HBResultType<
    HBClientNewHandler<
      IdUClientException,
      IdUCommandType<?>,
      IdUResponseType,
      IdUResponseType,
      IdUResponseError,
      IdUClientEventType,
      IdUClientCredentials>,
    IdUResponseError>
  onExecuteLogin(
    final IdUClientCredentials credentials)
    throws InterruptedException
  {
    LOG.debug("login: {}", credentials.baseURI());

    this.mostRecentLogin =
      new IdUCommandLogin(
        new IdName(credentials.userName()),
        credentials.password(),
        credentials.attributes()
      );

    final var response =
      this.sendLogin(this.mostRecentLogin);

    if (response instanceof final HBResultSuccess<IdUResponseLogin, IdUResponseError> success) {
      LOG.debug("login: succeeded");
      return new HBResultSuccess<>(
        new HBClientNewHandler<>(this, success.result())
      );
    }
    if (response instanceof final HBResultFailure<IdUResponseLogin, IdUResponseError> failure) {
      LOG.debug("login: failed ({})", failure.result().message());
      return failure.cast();
    }

    throw new UnreachableCodeException();
  }

  @Override
  public HBResultType<IdUResponseType, IdUResponseError>
  onExecuteCommand(
    final IdUCommandType<?> command)
    throws InterruptedException
  {
    return this.send(1, this.commandURI, false, command)
      .map(x -> x);
  }

  @Override
  public void onDisconnect()
  {

  }

  @Override
  public String toString()
  {
    return String.format(
      "[IdUHandler1 0x%08x]",
      Integer.valueOf(this.hashCode())
    );
  }
}
