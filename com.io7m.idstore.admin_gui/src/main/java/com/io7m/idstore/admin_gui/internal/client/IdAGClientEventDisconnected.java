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


package com.io7m.idstore.admin_gui.internal.client;

import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusCompleted;
import com.io7m.idstore.admin_gui.internal.events.IdAGEventStatusType;

import java.util.Objects;

import static com.io7m.idstore.admin_gui.internal.client.IdAGClientStatus.DISCONNECTED;

/**
 * The client disconnected from the server.
 *
 * @param message The message
 */

public record IdAGClientEventDisconnected(
  String message)
  implements IdAGClientEventType
{
  /**
   * The client disconnected from the server.
   */

  public IdAGClientEventDisconnected
  {
    Objects.requireNonNull(message, "message");
  }

  @Override
  public IdAGEventStatusType status()
  {
    return new IdAGEventStatusCompleted();
  }

  @Override
  public IdAGClientStatus clientStatus()
  {
    return DISCONNECTED;
  }
}
