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


package com.io7m.idstore.server.internal.freemarker;

import freemarker.cache.TemplateLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.NoSuchFileException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A freemarker template loader.
 */

public final class IdFMTemplateLoader implements TemplateLoader
{
  /**
   * A freemarker template loader.
   */

  public IdFMTemplateLoader()
  {

  }

  @Override
  public Object findTemplateSource(
    final String name)
  {
    return name;
  }

  @Override
  public long getLastModified(
    final Object templateSource)
  {
    return 0L;
  }

  @Override
  public Reader getReader(
    final Object templateSource,
    final String encoding)
    throws IOException
  {
    if (templateSource instanceof String name) {
      if (name.startsWith("pageLogin")) {
        return load("pageLogin");
      }
      if (name.startsWith("pageUserSelf")) {
        return load("pageUserSelf");
      }
      if (name.startsWith("pageEmailAdd")) {
        return load("pageEmailAdd");
      }
      if (name.startsWith("pageMessage")) {
        return load("pageMessage");
      }
      if (name.startsWith("emailVerification")) {
        return load("emailVerification");
      }
    }

    throw new IOException(
      "No such template: %s".formatted(templateSource)
    );
  }

  private static Reader load(
    final String name)
    throws IOException
  {
    final var path =
      "/com/io7m/idstore/server/internal/%s.ftlx".formatted(name);

    final var url =
      IdFMTemplateLoader.class.getResource(path);

    if (url == null) {
      throw new NoSuchFileException(path);
    }

    final var stream = url.openStream();
    return new BufferedReader(new InputStreamReader(stream, UTF_8));
  }

  @Override
  public void closeTemplateSource(
    final Object templateSource)
    throws IOException
  {

  }
}
