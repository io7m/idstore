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


package com.io7m.idstore.tests.integration;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdName;
import com.io7m.idstore.model.IdNonEmptyList;
import com.io7m.idstore.model.IdPasswordAlgorithmRedacted;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdUser;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUResponseEmailRemoveDeny;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseLogin;
import com.io7m.idstore.protocol.user.IdUResponseUserSelf;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import com.io7m.idstore.user_client.IdUClients;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientType;
import com.io7m.verdant.core.VProtocolSupported;
import com.io7m.verdant.core.VProtocols;
import com.io7m.verdant.core.cb.VProtocolMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.AUTHENTICATION_ERROR;
import static com.io7m.idstore.error_codes.IdStandardErrorCodes.PROTOCOL_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("integration")
@Tag("user-client")
public final class IdUClientIT
{
  private ClientAndServer mockServer;
  private IdUCB1Messages messages;
  private IdUser user;
  private IdUClients clients;
  private IdUClientType client;
  private VProtocols versions;
  private VProtocolMessages versionMessages;
  private byte[] versionHeader;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.user =
      new IdUser(
        UUID.randomUUID(),
        new IdName("someone"),
        new IdRealName("Someone"),
        IdNonEmptyList.single(new IdEmail("someone@example.com")),
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        IdPasswordAlgorithmRedacted.create().createHashed("x")
      );

    this.clients =
      new IdUClients();
    this.client =
      this.clients.create(Locale.ROOT);

    this.messages =
      new IdUCB1Messages();

    final var v1 =
      new VProtocolSupported(
        IdUCB1Messages.protocolId(),
        1L,
        0L,
        "/v1/"
      );

    this.versions =
      new VProtocols(List.of(v1));
    this.versionMessages =
      VProtocolMessages.create();
    this.versionHeader =
      this.versionMessages.serialize(this.versions, 1);

    this.mockServer =
      ClientAndServer.startClientAndServer(Integer.valueOf(60001));
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.mockServer.close();
    this.client.close();
  }

  /**
   * Command retries work when the server indicates a session has expired.
   *
   * @throws Exception On errors
   */

  @Test
  public void testCommandRetry()
    throws Exception
  {
    this.mockServer.when(
      HttpRequest.request()
        .withPath("/")
    ).respond(
      HttpResponse.response()
        .withBody(this.versionHeader)
        .withHeader("Content-Type", "application/verdant+cedarbridge")
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/login")
    ).respond(
      HttpResponse.response()
        .withBody(this.messages.serialize(new IdUResponseLogin(
          UUID.randomUUID(),
          this.user)))
        .withHeader("Content-Type", IdUCB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(401)
        .withBody(this.messages.serialize(
          new IdUResponseError(UUID.randomUUID(),
                               AUTHENTICATION_ERROR.id(),
                               "error")))
        .withHeader("Content-Type", IdUCB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withBody(this.messages.serialize(
          new IdUResponseUserSelf(UUID.randomUUID(), this.user)))
        .withHeader("Content-Type", IdUCB1Messages.contentType())
    );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60001/"),
      Map.of()
    );

    final var result = this.client.userSelf();
    assertEquals(this.user.id(), result.id());
  }

  /**
   * The client fails if the server returns a non-response.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServerReturnsNonResponse()
    throws Exception
  {
    this.mockServer.when(
      HttpRequest.request()
        .withPath("/")
    ).respond(
      HttpResponse.response()
        .withBody(this.versionHeader)
        .withHeader("Content-Type", "application/verdant+cedarbridge")
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/login")
    ).respond(
      HttpResponse.response()
        .withBody(this.messages.serialize(new IdUResponseLogin(
          UUID.randomUUID(),
          this.user)))
        .withHeader("Content-Type", IdUCB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withBody(this.messages.serialize(new IdUCommandUserSelf()))
        .withHeader("Content-Type", IdUCB1Messages.contentType())
    );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60001/"),
      Map.of()
    );

    final var ex =
      assertThrows(IdUClientException.class, () -> this.client.userSelf());

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }

  /**
   * The client fails if the server returns the wrong response.
   *
   * @throws Exception On errors
   */

  @Test
  public void testServerReturnsWrongResponse()
    throws Exception
  {
    this.mockServer.when(
      HttpRequest.request()
        .withPath("/")
    ).respond(
      HttpResponse.response()
        .withBody(this.versionHeader)
        .withHeader("Content-Type", "application/verdant+cedarbridge")
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/login")
    ).respond(
      HttpResponse.response()
        .withBody(this.messages.serialize(new IdUResponseLogin(
          UUID.randomUUID(),
          this.user)))
        .withHeader("Content-Type", IdUCB1Messages.contentType())
    );

    this.mockServer.when(
      HttpRequest.request()
        .withPath("/v1/command"),
      Times.exactly(1)
    ).respond(
      HttpResponse.response()
        .withStatusCode(200)
        .withBody(this.messages.serialize(
          new IdUResponseEmailRemoveDeny(UUID.randomUUID())))
        .withHeader("Content-Type", IdUCB1Messages.contentType())
    );

    this.client.login(
      "someone",
      "whatever",
      URI.create("http://localhost:60001/"),
      Map.of()
    );

    final var ex =
      assertThrows(IdUClientException.class, () -> this.client.userSelf());

    assertEquals(PROTOCOL_ERROR, ex.errorCode());
  }
}