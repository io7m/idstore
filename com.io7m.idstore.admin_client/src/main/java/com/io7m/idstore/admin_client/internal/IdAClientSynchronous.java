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


package com.io7m.idstore.admin_client.internal;

import com.io7m.hibiscus.basic.HBClientSynchronousAbstract;
import com.io7m.idstore.admin_client.api.IdAClientConfiguration;
import com.io7m.idstore.admin_client.api.IdAClientCredentials;
import com.io7m.idstore.admin_client.api.IdAClientEventType;
import com.io7m.idstore.admin_client.api.IdAClientException;
import com.io7m.idstore.admin_client.api.IdAClientSynchronousType;
import com.io7m.idstore.protocol.admin.IdACommandType;
import com.io7m.idstore.protocol.admin.IdAResponseError;
import com.io7m.idstore.protocol.admin.IdAResponseType;

import java.net.http.HttpClient;

/**
 * The synchronous client.
 */

public final class IdAClientSynchronous
  extends HBClientSynchronousAbstract<
  IdAClientException,
  IdACommandType<?>,
  IdAResponseType,
  IdAResponseType,
  IdAResponseError,
  IdAClientEventType,
  IdAClientCredentials>
  implements IdAClientSynchronousType
{
  /**
   * The synchronous client.
   *
   * @param inConfiguration The configuration
   * @param inHttpClient    The HTTP client
   * @param inStrings       The string resources
   */

  public IdAClientSynchronous(
    final IdAClientConfiguration inConfiguration,
    final IdAStrings inStrings,
    final HttpClient inHttpClient)
  {
    super(new IdAHandlerDisconnected(inConfiguration, inStrings, inHttpClient));
  }

  @Override
  protected void onCommandExecuteSucceeded(
    final IdACommandType<?> command,
    final IdAResponseType result)
  {

  }

  @Override
  protected void onCommandExecuteFailed(
    final IdACommandType<?> command,
    final IdAResponseError result)
  {

  }

  @Override
  protected void onLoginExecuteSucceeded(
    final IdAClientCredentials credentials,
    final IdAResponseType result)
  {

  }

  @Override
  protected void onLoginExecuteFailed(
    final IdAClientCredentials credentials,
    final IdAResponseError result)
  {

  }
}