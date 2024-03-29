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

package com.io7m.idstore.tests.server.service.ratelimit;

import com.io7m.idstore.server.service.ratelimit.IdRateLimitPasswordResetService;
import com.io7m.idstore.server.service.ratelimit.IdRateLimitPasswordResetServiceType;
import com.io7m.idstore.server.service.telemetry.api.IdMetricsService;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryNoOp;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceType;
import com.io7m.idstore.tests.server.service.IdServiceContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdRateLimitPasswordResetServiceTest
  extends IdServiceContract<IdRateLimitPasswordResetServiceType>
{
  private IdServerTelemetryServiceType telemetry;
  private IdMetricsService metrics;

  @BeforeEach
  public void setup()
  {
    this.telemetry = IdServerTelemetryNoOp.noop();
    this.metrics = new IdMetricsService(this.telemetry);
  }

  @Test
  public void testOK()
    throws Exception
  {
    final var service =
      IdRateLimitPasswordResetService.create(
        this.metrics,
        100L,
        MILLISECONDS
      );

    assertTrue(service.isAllowedByRateLimit("127.0.0.1"));
    assertFalse(service.isAllowedByRateLimit("127.0.0.1"));

    Thread.sleep(150L);
    assertTrue(service.isAllowedByRateLimit("127.0.0.1"));
  }

  @Override
  protected IdRateLimitPasswordResetServiceType createInstanceA()
  {
    return IdRateLimitPasswordResetService.create(
      this.metrics,
      100L,
      MILLISECONDS
    );
  }

  @Override
  protected IdRateLimitPasswordResetServiceType createInstanceB()
  {
    return IdRateLimitPasswordResetService.create(
      this.metrics,
      200L,
      MILLISECONDS
    );
  }
}
