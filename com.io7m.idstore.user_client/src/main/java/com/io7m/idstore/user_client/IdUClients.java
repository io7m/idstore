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

package com.io7m.idstore.user_client;

import com.io7m.idstore.user_client.api.IdUClientFactoryType;
import com.io7m.idstore.user_client.api.IdUClientType;
import com.io7m.idstore.user_client.internal.IdUClient;
import com.io7m.idstore.user_client.internal.IdUClientProtocolHandlerDisconnected;
import com.io7m.idstore.user_client.internal.IdUStrings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.Locale;

/**
 * The default client factory.
 */

public final class IdUClients implements IdUClientFactoryType
{
  /**
   * The default client factory.
   */

  public IdUClients()
  {

  }

  @Override
  public IdUClientType create(final Locale locale)
  {
    final var cookieJar =
      new CookieManager();

    final IdUStrings strings;
    try {
      strings = new IdUStrings(locale);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    final var httpClient =
      HttpClient.newBuilder()
        .cookieHandler(cookieJar)
        .build();

    return new IdUClient(
      strings,
      httpClient,
      new IdUClientProtocolHandlerDisconnected(strings, httpClient)
    );
  }
}
