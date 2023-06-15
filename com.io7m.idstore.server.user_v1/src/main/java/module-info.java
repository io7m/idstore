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

/**
 * Identity server (Server user v1 API)
 */

module com.io7m.idstore.server.user_v1
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.idstore.database.api;
  requires com.io7m.idstore.error_codes;
  requires com.io7m.idstore.model;
  requires com.io7m.idstore.protocol.user.cb;
  requires com.io7m.idstore.protocol.user;
  requires com.io7m.idstore.server.controller;
  requires com.io7m.idstore.server.http;
  requires com.io7m.idstore.server.service.clock;
  requires com.io7m.idstore.server.service.configuration;
  requires com.io7m.idstore.server.service.health;
  requires com.io7m.idstore.server.service.metrics;
  requires com.io7m.idstore.server.service.reqlimit;
  requires com.io7m.idstore.server.service.sessions;
  requires com.io7m.idstore.server.service.telemetry.api;
  requires com.io7m.idstore.server.service.verdant;

  requires com.io7m.verdant.core;
  requires io.opentelemetry.api;
  requires org.eclipse.jetty.http;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.servlet;
  requires org.slf4j;

  exports com.io7m.idstore.server.user_v1;
}