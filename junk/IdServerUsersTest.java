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

import com.io7m.idstore.model.IdEmail;
import com.io7m.idstore.model.IdRealName;
import com.io7m.idstore.model.IdToken;
import com.io7m.idstore.user_client.IdUClients;
import com.io7m.idstore.user_client.api.IdUClientException;
import com.io7m.idstore.user_client.api.IdUClientType;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.io7m.idstore.error_codes.IdStandardErrorCodes.RATE_LIMIT_EXCEEDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IdServerUsersTest extends IdWithServerContract
{
  private IdUClients clients;
  private IdUClientType client;

  @BeforeEach
  public void setup()
  {
    this.clients = new IdUClients();
    this.client = this.clients.create(Locale.getDefault());
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.client.close();
  }

  /**
   * Logging in works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginSelf()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var user = this.client.userSelf();
    assertEquals("someone", user.realName().value());
  }

  /**
   * Logging in fails with the wrong username.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsUsername()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    final var ex =
      Assertions.assertThrows(IdUClientException.class, () -> {
        this.client.login("someone1", "12345678", this.serverUserAPIURL(), Map.of());
      });

    assertTrue(
      ex.getMessage().contains("error-authentication"),
      ex.getMessage());
  }

  /**
   * Logging in fails with the wrong password.
   *
   * @throws Exception On errors
   */

  @Test
  public void testLoginFailsPassword()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    final var ex =
      Assertions.assertThrows(IdUClientException.class, () -> {
        this.client.login("someone", "123456789", this.serverUserAPIURL(), Map.of());
      });

    assertTrue(
      ex.getMessage().contains("error-authentication"),
      ex.getMessage());
  }

  /**
   * Adding an email address works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailAddressAddPermit()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.client.userEmailAddBegin(newMail);

    final var email =
      this.emailsReceived()
        .poll();

    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    this.client.userEmailAddPermit(new IdToken(token));

    final var userNow = this.client.userSelf();
    assertTrue(
      userNow.emails()
        .toList()
        .stream().anyMatch(e -> Objects.equals(e, newMail))
    );

    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailAddPermit(new IdToken(token));
    });
  }

  /**
   * Adding an email address fails if the request expires.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailAddressAddPermitExpired()
    throws Exception
  {
    this.serverStartIfNecessary();

    this.clock().setTime(Instant.ofEpochSecond(0L));

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.client.userEmailAddBegin(newMail);

    final var email =
      this.emailsReceived()
        .poll();

    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    this.clock().setTime(Instant.ofEpochSecond(86400L * 30L));

    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailAddPermit(new IdToken(token));
    });
    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailAddDeny(new IdToken(token));
    });

    final var userNow = this.client.userSelf();
    assertFalse(
      userNow.emails()
        .toList()
        .stream().anyMatch(e -> Objects.equals(e, newMail))
    );
  }

  /**
   * Adding an email address fails if the email already exists.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailAddressAddPermitDuplicate()
    throws Exception
  {
    this.serverStartIfNecessary();

    this.clock().setTime(Instant.ofEpochSecond(0L));

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    final var userId =
      this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("someone@example.com");
    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailAddBegin(newMail);
    });
  }

  /**
   * Adding an email address fails if the token is not owned by the user.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailAddressAddPermitNotYours()
    throws Exception
  {
    this.serverStartIfNecessary();

    this.clock().setTime(Instant.ofEpochSecond(0L));

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.serverCreateUser(admin, "someone");
    this.serverCreateUser(admin, "someone2");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.client.userEmailAddBegin(newMail);

    final var email =
      this.emailsReceived()
        .poll();

    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    this.client.login("someone2", "12345678", this.serverUserAPIURL(), Map.of());

    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailAddPermit(new IdToken(token));
    });
    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailAddDeny(new IdToken(token));
    });
  }

  /**
   * Rejecting an email address works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailAddressAddDeny()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.client.userEmailAddBegin(newMail);

    final var email =
      this.emailsReceived()
        .poll();

    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    this.client.userEmailAddDeny(new IdToken(token));

    final var userNow = this.client.userSelf();
    assertFalse(
      userNow.emails()
        .toList()
        .stream().anyMatch(e -> Objects.equals(e, newMail))
    );

    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailAddDeny(new IdToken(token));
    });
  }

  /**
   * Removing an email address works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailAddressRemovePermit()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.requestEmailAddition(newMail);

    this.client.userEmailRemoveBegin(newMail);

    {
      final var email =
        this.emailsReceived()
          .poll();

      final var token =
        email.getHeader("X-IDStore-Verification-Token")[0];

      this.client.userEmailRemovePermit(new IdToken(token));
    }

    final var userNow = this.client.userSelf();
    assertFalse(
      userNow.emails()
        .toList()
        .stream().anyMatch(e -> Objects.equals(e, newMail))
    );
  }

  private void requestEmailAddition(
    final IdEmail newMail)
    throws IdUClientException, InterruptedException, MessagingException
  {
    this.client.userEmailAddBegin(newMail);

    final var email =
      this.emailsReceived()
        .poll();

    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    this.client.userEmailAddPermit(new IdToken(token));
  }

  /**
   * Removing an email address fails if the request expires.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailRemovePermitExpired()
    throws Exception
  {
    this.serverStartIfNecessary();

    this.clock().setTime(Instant.ofEpochSecond(0L));

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.requestEmailAddition(newMail);

    this.client.userEmailRemoveBegin(newMail);

    final var email =
      this.emailsReceived()
        .poll();

    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    this.clock().setTime(Instant.ofEpochSecond(86400L * 30L));

    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailRemovePermit(new IdToken(token));
    });
    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailRemoveDeny(new IdToken(token));
    });

    final var userNow = this.client.userSelf();
    assertTrue(
      userNow.emails()
        .toList()
        .stream().anyMatch(e -> Objects.equals(e, newMail))
    );
  }

  /**
   * Removing an email address fails if the token is not owned by the user.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailRemovePermitNotYours()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.serverCreateUser(admin, "someone");
    this.serverCreateUser(admin, "someone2");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.requestEmailAddition(newMail);

    this.client.userEmailRemoveBegin(newMail);

    final var email =
      this.emailsReceived()
        .poll();

    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    this.client.login("someone2", "12345678", this.serverUserAPIURL(), Map.of());

    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailRemovePermit(new IdToken(token));
    });
    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailRemoveDeny(new IdToken(token));
    });
  }

  /**
   * Removing an email address fails if the email address would remove all
   * addresses from the account.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailRemovePermitTooFew()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());
    this.requestEmailAddition(new IdEmail("gauss@example.com"));

    /*
     * At this point the user has two addresses, so we can request the
     * removal of both (but only complete the removal of one!)
     */

    final var self = this.client.userSelf();
    for (final var e : self.emails()) {
      this.client.userEmailRemoveBegin(e);
    }

    {
      final var email = this.emailsReceived().poll();
      final var token =
        email.getHeader("X-IDStore-Verification-Token")[0];
      this.client.userEmailRemovePermit(new IdToken(token));
    }

    {
      final var email = this.emailsReceived().poll();
      final var token =
        email.getHeader("X-IDStore-Verification-Token")[0];

      assertThrows(IdUClientException.class, () -> {
        this.client.userEmailRemovePermit(new IdToken(token));
      });
    }
  }

  /**
   * Removing an email address fails if the user doesn't have the address.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailRemoveNonexistent()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailRemoveBegin(newMail);
    });
  }

  /**
   * An addition cannot be completed by a removal.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailAddressAddRemoveWrong()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.client.userEmailAddBegin(newMail);

    final var email =
      this.emailsReceived()
        .poll();

    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailRemovePermit(new IdToken(token));
    });
    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailRemoveDeny(new IdToken(token));
    });

    final var userNow = this.client.userSelf();
    assertFalse(
      userNow.emails()
        .toList()
        .stream().anyMatch(e -> Objects.equals(e, newMail))
    );
  }

  /**
   * A removal cannot be completed by an addition.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailAddressRemoveAddWrong()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.requestEmailAddition(newMail);

    this.client.userEmailRemoveBegin(newMail);

    final var email =
      this.emailsReceived()
        .poll();

    final var token =
      email.getHeader("X-IDStore-Verification-Token")[0];

    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailAddPermit(new IdToken(token));
    });
    assertThrows(IdUClientException.class, () -> {
      this.client.userEmailAddDeny(new IdToken(token));
    });

    final var userNow = this.client.userSelf();
    assertTrue(
      userNow.emails()
        .toList()
        .stream().anyMatch(e -> Objects.equals(e, newMail))
    );
  }

  /**
   * Updating a user realname works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRealNameUpdate()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());
    this.client.userRealNameUpdate(new IdRealName("A New Name"));

    final var userNow = this.client.userSelf();
    assertEquals("A New Name", userNow.realName().value());
  }

  /**
   * Updating a user password works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testPasswordUpdate()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");

    this.serverCreateUser(admin, "someone");

    final var user =
      this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    this.client.userPasswordUpdate("abcd", "abcd");

    final var userNow = this.client.userSelf();
    assertNotEquals(user.password(), userNow.password());
  }

  /**
   * Email verifications are rate-limited.
   *
   * @throws Exception On errors
   */

  @Test
  public void testEmailVerificationRateLimited()
    throws Exception
  {
    this.serverStartIfNecessary();

    final var admin =
      this.serverCreateAdminInitial("admin", "12345678");
    this.serverCreateUser(admin, "someone");

    this.client.login("someone", "12345678", this.serverUserAPIURL(), Map.of());

    final var newMail = new IdEmail("gauss@example.com");
    this.client.userEmailAddBegin(newMail);

    final var email0 =
      this.emailsReceived().poll();

    final var ex =
      assertThrows(IdUClientException.class, () -> {
        this.client.userEmailAddBegin(newMail);
      });

    assertEquals(RATE_LIMIT_EXCEEDED, ex.errorCode());
  }

}
