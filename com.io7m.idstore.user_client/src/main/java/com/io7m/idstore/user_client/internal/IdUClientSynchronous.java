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

import com.io7m.hibiscus.basic.HBClientSynchronousAbstract;
import com.io7m.idstore.error_codes.IdStandardErrorCodes;
import com.io7m.idstore.protocol.user.IdUCommandType;
import com.io7m.idstore.protocol.user.IdUResponseError;
import com.io7m.idstore.protocol.user.IdUResponseType;
import com.io7m.idstore.strings.IdStrings;
import com.io7m.idstore.user_client.api.IdUClientConfiguration;
import com.io7m.idstore.user_client.api.IdUClientCredentials;
import com.io7m.idstore.user_client.api.IdUClientEventType;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientSynchronousType;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.idstore.protocol.user.IdUResponseBlame.BLAME_CLIENT;

/**
 * The synchronous client.
 */

public final class IdUClientSynchronous
  extends HBClientSynchronousAbstract<
  IdUClientException,
  IdUCommandType<?>,
  IdUResponseType,
  IdUResponseType,
  IdUResponseError,
  IdUClientEventType,
  IdUClientCredentials>
  implements IdUClientSynchronousType
{
  /**
   * The synchronous client.
   *
   * @param inConfiguration The configuration
   * @param inHttpClient    The HTTP client
   * @param inStrings       The string resources
   */

  public IdUClientSynchronous(
    final IdUClientConfiguration inConfiguration,
    final IdStrings inStrings,
    final HttpClient inHttpClient)
  {
    super(
      new IdUHandlerDisconnected(inConfiguration, inStrings, inHttpClient),
      IdUClientSynchronous::ofException
    );
  }

  private static IdUResponseError ofException(
    final Throwable ex)
  {
    if (ex instanceof final IdUClientException e) {
      return new IdUResponseError(
        e.requestId().orElseGet(IdUUUIDs::nullUUID),
        e.message(),
        e.errorCode(),
        e.attributes(),
        e.remediatingAction(),
        BLAME_CLIENT
      );
    }

    return new IdUResponseError(
      IdUUUIDs.nullUUID(),
      Objects.requireNonNullElse(
        ex.getMessage(),
        ex.getClass().getSimpleName()),
      IdStandardErrorCodes.IO_ERROR,
      Map.of(),
      Optional.empty(),
      BLAME_CLIENT
    );
  }
}
