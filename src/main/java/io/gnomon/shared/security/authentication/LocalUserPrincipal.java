package io.gnomon.shared.security.authentication;

import io.gnomon.tenancy.application.port.in.result.LocalUserResult;
import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

public record LocalUserPrincipal(
    UUID userId, String keycloakSubject, String email, String displayName)
    implements Principal, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  public LocalUserPrincipal {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(keycloakSubject, "keycloakSubject");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(displayName, "displayName");
  }

  public static LocalUserPrincipal from(LocalUserResult user) {
    return new LocalUserPrincipal(
        user.userId(), user.keycloakSubject(), user.email(), user.displayName());
  }

  @Override
  public String getName() {
    return keycloakSubject;
  }
}
