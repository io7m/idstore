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

package com.io7m.idstore.server.internal;

import com.io7m.idstore.services.api.IdServiceType;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service to create and manage admin sessions.
 */

public final class IdAdminSessionService
  implements IdServiceType, HttpSessionListener
{
  private static final Logger LOG =
    LoggerFactory.getLogger(IdAdminSessionService.class);

  private final ConcurrentHashMap<String, IdAdminSession> sessions;
  private final ObservableLongGauge sessionsGauge;

  /**
   * A service to create and manage admin sessions.
   *
   * @param inTelemetry The telemetry service
   */

  public IdAdminSessionService(
    final IdServerTelemetryService inTelemetry)
  {
    this.sessions = new ConcurrentHashMap<>();

    final var meter =
      inTelemetry.openTelemetry()
        .meterBuilder(IdAdminSessionService.class.getCanonicalName())
        .build();

    this.sessionsGauge =
      meter.gaugeBuilder("idstore.activeAdminSessions")
        .setDescription("Active admin sessions.")
        .ofLongs()
        .buildWithCallback(m -> {
          m.record(Integer.toUnsignedLong(this.sessions.size()));
        });
  }

  @Override
  public String description()
  {
    return "Admin session service.";
  }

  /**
   * Create or get an existing admin session.
   *
   * @param adminId   The admin ID
   * @param sessionId The session ID
   *
   * @return A admin session
   */

  public IdAdminSession createOrGet(
    final UUID adminId,
    final String sessionId)
  {
    Objects.requireNonNull(adminId, "adminId");
    Objects.requireNonNull(sessionId, "sessionId");

    final var id = "%s:%s".formatted(adminId, sessionId);
    return this.sessions.computeIfAbsent(
      id,
      ignored -> {
        final var sizeNow = this.sessions.size() + 1;
        LOG.debug(
          "[{}] create admin session ({} now active)",
          id,
          Integer.valueOf(sizeNow)
        );
        return new IdAdminSession(adminId, sessionId);
      }
    );
  }

  @Override
  public void sessionCreated(
    final HttpSessionEvent se)
  {

  }

  @Override
  public void sessionDestroyed(
    final HttpSessionEvent se)
  {
    final var session = se.getSession();
    final var adminId = (UUID) session.getAttribute("UserID");
    final var sessionId = session.getId();

    if (adminId != null) {
      this.delete(adminId, sessionId);
    }
  }

  /**
   * Delete a admin session if one exists.
   *
   * @param adminId   The admin ID
   * @param sessionId The session ID
   */

  public void delete(
    final UUID adminId,
    final String sessionId)
  {
    Objects.requireNonNull(adminId, "adminId");
    Objects.requireNonNull(sessionId, "sessionId");

    final var id = "%s:%s".formatted(adminId, sessionId);
    this.sessions.remove(id);

    LOG.debug(
      "[{}] delete admin session ({} now active)",
      id,
      Integer.valueOf(this.sessions.size())
    );
  }

  @Override
  public String toString()
  {
    return "[IdAdminSessionService 0x%s]"
      .formatted(Long.toUnsignedString(this.hashCode(), 16));
  }
}
