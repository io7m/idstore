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


package com.io7m.idstore.tests.protocol.user.cb;

import com.io7m.idstore.protocol.user.IdUMessageType;
import com.io7m.idstore.protocol.user.cb.IdUCB1Messages;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class IdUCB1MessagesTest
{
  private static final IdUCB1Messages MESSAGES =
    new IdUCB1Messages();

  @Property(tries = 2000)
  public void testSerialization(
    final @ForAll IdUMessageType message)
    throws Exception
  {
    final var data =
      MESSAGES.serialize(message);
    final var m =
      MESSAGES.parse(data);

    assertEquals(message, m);
  }

  @Test
  public void testProtocolId()
  {
    assertEquals(
      UUID.fromString("ed628c5a-0182-36ab-bee3-5fe6f6a21894"),
      IdUCB1Messages.protocolId()
    );
  }
}
