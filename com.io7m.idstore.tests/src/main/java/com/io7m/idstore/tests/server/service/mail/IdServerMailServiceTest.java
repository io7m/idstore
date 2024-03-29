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


package com.io7m.idstore.tests.server.service.mail;

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.server.api.IdServerMailConfiguration;
import com.io7m.idstore.server.api.IdServerMailTransportSMTP;
import com.io7m.idstore.server.service.mail.IdServerMailService;
import com.io7m.idstore.server.service.mail.IdServerMailServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdEventService;
import com.io7m.idstore.server.service.telemetry.api.IdEventServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdMetricsService;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import com.io7m.zelador.test_extension.CloseableResourcesType;
import com.io7m.zelador.test_extension.ZeladorExtension;
import io.opentelemetry.api.trace.Span;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.subethamail.smtp.server.SMTPServer;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp.noop;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ZeladorExtension.class)
public final class IdServerMailServiceTest
  extends IdServiceContract<IdServerMailServiceType>
{
  private ConcurrentLinkedQueue<MimeMessage> emailsReceived;
  private SMTPServer smtp;
  private IdServerMailConfiguration configuration;
  private IdServerMailServiceType mailService;
  private IdServerTelemetryNoOp telemetry;
  private IdMetricsService metrics;
  private IdEventServiceType events;

  @BeforeEach
  public void setup(
    final CloseableResourcesType closeables)
  {
    this.telemetry =
      noop();
    this.metrics =
      new IdMetricsService(this.telemetry);
    this.events =
      IdEventService.create(this.telemetry, this.metrics);

    this.emailsReceived = new ConcurrentLinkedQueue<>();
    this.smtp =
      SMTPServer.port(32025)
        .messageHandler((messageContext, source, destination, data) -> {
          try {
            final var message =
              new MimeMessage(
                Session.getDefaultInstance(new Properties()),
                new ByteArrayInputStream(data)
              );

            this.emailsReceived.add(message);
          } catch (final MessagingException e) {
            throw new IllegalStateException(e);
          }
        })
        .build();

    closeables.addPerTestResource(this.smtp::stop);
    this.smtp.start();

    this.configuration =
      new IdServerMailConfiguration(
        new IdServerMailTransportSMTP("127.0.0.1", 32025),
        Optional.empty(),
        "examples@localhost.com",
        Duration.ofHours(6L)
      );

    this.mailService =
      closeables.addPerTestResource(
        IdServerMailService.create(noop(), this.events, this.configuration)
      );
  }

  /**
   * Sending mail works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testSendMail()
    throws Exception
  {
    this.mailService.sendMail(
      Span.current(),
      UUID.randomUUID(),
      new IdEmail("someone@example.com"),
      Map.ofEntries(
        entry("X-A", "0"),
        entry("X-B", "1"),
        entry("X-C", "2")
      ),
      "Example subject.",
      "Example text."
    ).get();

    final var mail = this.emailsReceived.poll();
    assertEquals("Example subject.", mail.getSubject());
    assertEquals("Example text.\r\n", mail.getContent());
    assertEquals("0", mail.getHeader("X-A")[0]);
    assertEquals("1", mail.getHeader("X-B")[0]);
    assertEquals("2", mail.getHeader("X-C")[0]);
  }

  @Override
  protected IdServerMailServiceType createInstanceA()
  {
    return IdServerMailService.create(noop(), this.events, this.configuration);
  }

  @Override
  protected IdServerMailServiceType createInstanceB()
  {
    return IdServerMailService.create(noop(), this.events, this.configuration);
  }
}
