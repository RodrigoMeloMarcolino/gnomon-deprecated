package io.gnomon.catalog.api.request;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record ReplaceCalendarOfferingsRequest(@NotNull Set<@NotNull UUID> offeringIds) {
  public ReplaceCalendarOfferingsRequest {
    if (offeringIds != null) {
      offeringIds = Collections.unmodifiableSet(new LinkedHashSet<>(offeringIds));
    }
  }

  @Override
  public Set<@NotNull UUID> offeringIds() {
    return offeringIds == null
        ? null
        : Collections.unmodifiableSet(new LinkedHashSet<>(offeringIds));
  }
}
