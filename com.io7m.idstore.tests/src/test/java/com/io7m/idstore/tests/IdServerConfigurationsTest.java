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


package com.io7m.idstore.tests;

import com.io7m.idstore.protocol.api.IdProtocolException;
import com.io7m.idstore.server.api.IdServerConfigurationFiles;
import com.io7m.idstore.server.api.IdServerConfigurations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Locale;

public final class IdServerConfigurationsTest
{
  private IdServerConfigurationFiles messages;
  private Path directory;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.messages = new IdServerConfigurationFiles();
    this.directory = IdTestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    IdTestDirectories.deleteDirectory(this.directory);
  }

  @Test
  public void testLoad()
    throws IOException, IdProtocolException
  {
    final var file =
      IdTestDirectories.resourceOf(
        IdServerConfigurationsTest.class,
        this.directory,
        "server-config-0.json"
      );

    final var configuration =
      IdServerConfigurations.ofFile(
        Locale.getDefault(),
        Clock.systemUTC(),
        file
      );
  }
}
