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

package com.io7m.idstore.tests.arbitraries;

import com.io7m.idstore.model.IdTimeRange;
import com.io7m.idstore.model.IdUserColumnOrdering;
import com.io7m.idstore.model.IdUserSearchByEmailParameters;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;

/**
 * A provider of {@link IdUserSearchByEmailParameters} values.
 */

public final class IdArbUserSearchByEmailParametersProvider extends IdArbAbstractProvider
{
  /**
   * A provider of values.
   */

  public IdArbUserSearchByEmailParametersProvider()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return targetType.isOfType(IdUserSearchByEmailParameters.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    final var t =
      Arbitraries.defaultFor(IdTimeRange.class);
    final var o =
      Arbitraries.defaultFor(IdUserColumnOrdering.class);
    final var s =
      Arbitraries.strings();
    final var i =
      Arbitraries.integers()
        .between(1, 1000);

    final var a =
      Combinators.combine(t, t, s, o, i).as((t0, t1, ss, uo, in) -> {
        return new IdUserSearchByEmailParameters(
          t0,
          t1,
          ss,
          uo,
          in.intValue()
        );
      });

    return Set.of(a);
  }
}
