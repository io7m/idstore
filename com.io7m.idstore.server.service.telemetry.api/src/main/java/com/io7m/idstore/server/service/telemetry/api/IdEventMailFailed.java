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


package com.io7m.idstore.server.service.telemetry.api;

import com.io7m.idstore.model.IdEmail;

import java.time.Duration;
import java.util.Map;

/**
 * Mail failed to send.
 *
 * @param to       The target address
 * @param time The time it took
 */

public record IdEventMailFailed(
  IdEmail to,
  Duration time)
  implements IdEventType
{
  @Override
  public String name()
  {
    return "mail.failed";
  }

  @Override
  public IdEventSeverity severity()
  {
    return IdEventSeverity.ERROR;
  }

  @Override
  public String message()
  {
    return "%s %s".formatted(this.name(), this.to());
  }

  @Override
  public Map<String, String> asAttributes()
  {
    return Map.ofEntries(
      Map.entry("event.domain", this.domain()),
      Map.entry("event.name", this.name()),
      Map.entry("idstore.email", this.to.value())
    );
  }
}
