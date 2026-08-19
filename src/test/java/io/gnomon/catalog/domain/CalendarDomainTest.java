package io.gnomon.catalog.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gnomon.catalog.domain.exception.CatalogException;
import io.gnomon.catalog.domain.model.Calendar;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalendarDomainTest {

  private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

  @Test
  void create_withInvalidTimezone_shouldRejectWithStableCode() {
    for (String timezone : new String[] {null, "", "Mars/Olympus", "not a zone"}) {
      assertThatThrownBy(
              () ->
                  Calendar.create(UUID.randomUUID(), UUID.randomUUID(), "Calendar", timezone, NOW))
          .isInstanceOf(CatalogException.class)
          .extracting(exception -> ((CatalogException) exception).code())
          .isEqualTo("invalid_timezone");
    }
  }
}
