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

package com.io7m.idstore.tests;

import com.io7m.idstore.model.IdName;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.user.IdUCommandLogin;
import com.io7m.idstore.protocol.user.IdUCommandUserSelf;
import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Map;

import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerUserErrorsTest extends IdWithServerContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdServerUserErrorsTest.class);

  private IdUCB1Messages message;

  @BeforeEach
  public void setup()
  {
    this.message = new IdUCB1Messages();
  }

  /**
   * Blast the server with nonsense.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNonsenseUnauthenticated()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var client =
      HttpClient.newHttpClient();

    final var arb =
      Arbitraries.defaultFor(IdUMessageType.class);

    for (int index = 0; index < 1000; ++index) {
      final var message = arb.sample();
      LOG.debug("send: {}", message);

      final var req =
        HttpRequest.newBuilder(
            URI.create(this.serverUserAPIURL() + "user/1/0/command")
          ).POST(ofByteArray(this.message.serialize(message)))
          .build();

      final var response =
        client.send(req, HttpResponse.BodyHandlers.ofByteArray());

      LOG.debug("receive: {}", response.statusCode());
      assertTrue(response.statusCode() >= 400);
    }
  }

  /**
   * Blast the server with nonsense.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNonsenseUnauthenticatedLogin()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var client =
      HttpClient.newHttpClient();

    final var arb =
      Arbitraries.defaultFor(IdUMessageType.class);

    for (int index = 0; index < 1000; ++index) {
      final var message = arb.sample();
      LOG.debug("send: {}", message);

      final var req =
        HttpRequest.newBuilder(
            URI.create(this.serverUserAPIURL() + "user/1/0/login")
          ).POST(ofByteArray(this.message.serialize(message)))
          .build();

      final var response =
        client.send(req, HttpResponse.BodyHandlers.ofByteArray());

      LOG.debug("receive: {}", response.statusCode());
      assertTrue(response.statusCode() >= 400);
    }
  }

  /**
   * Blast the server with nonsense.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNonsenseAuthenticatedCommand()
    throws Exception
  {
    this.serverStartIfNecessary();
    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    final var client =
      HttpClient.newBuilder()
        .cookieHandler(new CookieManager())
        .build();

    this.doLogin(client);

    final var arb =
      Arbitraries.defaultFor(IdUMessageType.class)
        .filter(u -> u instanceof IdUResponseType);

    final var succeeded = new ArrayList<IdUMessageType>();
    for (int index = 0; index < 1000; ++index) {
      final var message = arb.sample();
      LOG.debug("send: {}", message);

      final var req =
        HttpRequest.newBuilder(
            URI.create(this.serverUserAPIURL() + "user/1/0/command")
          ).POST(ofByteArray(this.message.serialize(message)))
          .build();

      final var response =
        client.send(req, HttpResponse.BodyHandlers.ofByteArray());

      LOG.debug("receive: {}", response.statusCode());
      if (response.statusCode() < 300) {
        succeeded.add(message);
      }
    }

    succeeded.removeIf(m -> m instanceof IdUCommandUserSelf);
    assertTrue(succeeded.isEmpty());
  }

  private void doLogin(final HttpClient client)
    throws IdProtocolException, IOException, InterruptedException
  {
    final var req =
      HttpRequest.newBuilder(
          URI.create(this.serverUserAPIURL() + "user/1/0/login")
        ).POST(ofByteArray(
          this.message.serialize(
            new IdUCommandLogin(new IdName("someone"), "12345678", Map.of()))))
        .build();

    final var response =
      client.send(req, HttpResponse.BodyHandlers.discarding());
    assertEquals(200, response.statusCode());
  }
}
