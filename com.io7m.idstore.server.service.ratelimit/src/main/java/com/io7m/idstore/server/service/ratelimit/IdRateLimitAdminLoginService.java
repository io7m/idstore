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

package com.io7m.idstore.server.service.ratelimit;

import com.io7m.idstore.server.service.metrics.IdMetricsServiceType;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A rate limiting service for logins.
 */

public final class IdRateLimitAdminLoginService
  implements IdRateLimitAdminLoginServiceType
{
  private final IdRateLimiter limiter;

  private IdRateLimitAdminLoginService(
    final IdRateLimiter inLimiter)
  {
    this.limiter = Objects.requireNonNull(inLimiter, "limiter");
  }

  /**
   * Create a rate limit service.
   *
   * @param metrics    The metrics service
   * @param expiration The expiration for tokens
   * @param timeUnit   The time unit for expirations
   *
   * @return A rate limiter
   */

  public static IdRateLimitAdminLoginServiceType create(
    final IdMetricsServiceType metrics,
    final long expiration,
    final TimeUnit timeUnit)
  {
    return new IdRateLimitAdminLoginService(
      IdRateLimiter.create(
        metrics,
        "admin_login",
        expiration,
        timeUnit
      )
    );
  }

  @Override
  public boolean isAllowedByRateLimit(
    final String host)
  {
    return this.limiter.isAllowedByRateLimit(host, "", "LOGIN");
  }

  @Override
  public String description()
  {
    return "Admin login rate limiting service.";
  }

  @Override
  public String toString()
  {
    return "[IdRateLimitAdminLoginService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }

  @Override
  public Duration waitTime()
  {
    return this.limiter.waitTime();
  }
}