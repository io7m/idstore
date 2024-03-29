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

package com.io7m.idstore.server.controller.admin;

import com.io7m.idstore.error_codes.IdException;
import com.io7m.idstore.protocol.admin.IdACommandMaintenanceModeSet;
import com.io7m.idstore.protocol.admin.IdAResponseMaintenanceModeSet;
import com.io7m.idstore.protocol.admin.IdAResponseType;
import com.io7m.idstore.server.security.IdSecAdminActionMaintenanceMode;
import com.io7m.idstore.server.service.maintenance.IdClosedForMaintenanceService;
import com.io7m.idstore.strings.IdStrings;

import static com.io7m.idstore.strings.IdStringConstants.MAINTENANCE_MODE_SET_OFF;
import static com.io7m.idstore.strings.IdStringConstants.MAINTENANCE_MODE_SET_ON;

/**
 * IdACommandMaintenanceMode
 */

public final class IdACmdMaintenanceModeSet
  extends IdACmdAbstract<
  IdACommandContext, IdACommandMaintenanceModeSet, IdAResponseType>
{
  /**
   * IdACommandMaintenanceMode
   */

  public IdACmdMaintenanceModeSet()
  {

  }

  @Override
  protected IdAResponseType executeActual(
    final IdACommandContext context,
    final IdACommandMaintenanceModeSet command)
    throws IdException
  {
    final var services =
      context.services();
    final var maintenance =
      services.requireService(IdClosedForMaintenanceService.class);
    final var strings =
      services.requireService(IdStrings.class);

    context.securityCheck(new IdSecAdminActionMaintenanceMode(context.admin()));

    final String response;
    final var messageOpt = command.message();
    if (messageOpt.isPresent()) {
      final String messageText = messageOpt.get();
      maintenance.closeForMaintenance(messageText);
      response = strings.format(MAINTENANCE_MODE_SET_ON, messageText);
    } else {
      maintenance.openForBusiness();
      response = strings.format(MAINTENANCE_MODE_SET_OFF);
    }

    return new IdAResponseMaintenanceModeSet(context.requestId(), response);
  }
}
