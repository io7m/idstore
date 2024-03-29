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


package com.io7m.idstore.protocol.admin.cb;

import com.io7m.cedarbridge.runtime.api.CBProtocolMessageVersionedSerializerType;
import com.io7m.cedarbridge.runtime.bssio.CBSerializationContextBSSIO;
import com.io7m.idstore.protocol.admin.IdAMessageType;
import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.protocol.api.IdProtocolMessagesType;
import com.io7m.jbssio.api.BSSReaderProviderType;
import com.io7m.jbssio.api.BSSWriterProviderType;
import com.io7m.jbssio.vanilla.BSSReaders;
import com.io7m.jbssio.vanilla.BSSWriters;
import com.io7m.repetoir.core.RPServiceType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.IO_ERROR;
import static java.util.Objects.requireNonNullElse;

/**
 * The protocol messages for Admin v1 Cedarbridge.
 */

public final class IdACB1Messages
  implements IdProtocolMessagesType<IdAMessageType>, RPServiceType
{
  private static final ProtocolIdA PROTOCOL = new ProtocolIdA();

  /**
   * The content type for the protocol.
   */

  public static final String CONTENT_TYPE =
    "application/idstore_admin+cedarbridge";

  private final BSSReaderProviderType readers;
  private final BSSWriterProviderType writers;
  private final IdACB1Validation validator;
  private final CBProtocolMessageVersionedSerializerType<ProtocolIdAType> serializer;

  /**
   * The protocol messages for Admin v1 Cedarbridge.
   *
   * @param inReaders The readers
   * @param inWriters The writers
   */

  public IdACB1Messages(
    final BSSReaderProviderType inReaders,
    final BSSWriterProviderType inWriters)
  {
    this.readers =
      Objects.requireNonNull(inReaders, "readers");
    this.writers =
      Objects.requireNonNull(inWriters, "writers");

    this.validator = new IdACB1Validation();
    this.serializer =
      PROTOCOL.serializerForProtocolVersion(1L)
        .orElseThrow(() -> {
          return new IllegalStateException("No support for version 1");
        });
  }

  /**
   * The protocol messages for Admin v1 Cedarbridge.
   */

  public IdACB1Messages()
  {
    this(new BSSReaders(), new BSSWriters());
  }

  /**
   * @return The content type
   */

  public static String contentType()
  {
    return CONTENT_TYPE;
  }

  /**
   * @return The protocol identifier
   */

  public static UUID protocolId()
  {
    return PROTOCOL.protocolId();
  }

  @Override
  public IdAMessageType parse(
    final byte[] data)
    throws IdProtocolException
  {
    final var context =
      CBSerializationContextBSSIO.createFromByteArray(this.readers, data);

    try {
      return this.validator.convertFromWire(
        (ProtocolIdAv1Type) this.serializer.deserialize(context)
      );
    } catch (final IOException e) {
      throw new IdProtocolException(
        requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
        e,
        IO_ERROR,
        Map.of(),
        Optional.empty()
      );
    }
  }

  @Override
  public byte[] serialize(
    final IdAMessageType message)
  {
    try (var output = new ByteArrayOutputStream()) {
      final var context =
        CBSerializationContextBSSIO.createFromOutputStream(
          this.writers,
          output);
      this.serializer.serialize(context, this.validator.convertToWire(message));
      return output.toByteArray();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    } catch (final IdProtocolException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String description()
  {
    return "Admin v1 Cedarbridge message service.";
  }

  @Override
  public String toString()
  {
    return "[IdACB1Messages 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}
