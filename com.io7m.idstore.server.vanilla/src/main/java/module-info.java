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

import com.io7m.idstore.server.api.IdServerFactoryType;
import com.io7m.idstore.server.service.telemetry.api.IdServerTelemetryServiceFactoryType;
import com.io7m.idstore.server.vanilla.IdServers;

/**
 * Identity server (Server vanilla implementation)
 */

module com.io7m.idstore.server.vanilla
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  uses IdServerTelemetryServiceFactoryType;

  requires transitive com.io7m.idstore.server.api;

  requires com.io7m.jmulticlose.core;

  requires com.io7m.idstore.protocol.admin.cb;
  requires com.io7m.idstore.protocol.user.cb;
  requires com.io7m.idstore.server.admin_v1;
  requires com.io7m.idstore.server.controller;
  requires com.io7m.idstore.server.service.branding;
  requires com.io7m.idstore.server.service.clock;
  requires com.io7m.idstore.server.service.mail;
  requires com.io7m.idstore.server.service.sessions;
  requires com.io7m.idstore.server.service.telemetry.api;
  requires com.io7m.idstore.server.service.templating;
  requires com.io7m.idstore.server.service.verdant;
  requires com.io7m.idstore.server.user_v1;
  requires com.io7m.idstore.server.service.configuration;
  requires com.io7m.idstore.server.service.ratelimit;
  requires com.io7m.idstore.server.service.reqlimit;
  requires com.io7m.idstore.server.user_view;
  requires org.eclipse.jetty.util;
  requires org.eclipse.jetty.server;

  provides IdServerFactoryType
    with IdServers;

  exports com.io7m.idstore.server.vanilla;
}
