package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for LocationGateway — the in-memory location resolver.
 *
 * <p>LocationGateway has no external dependencies, so there are no @Mock fields.
 * We still use @ExtendWith + @InjectMocks for consistency with the rest of the
 * test suite and to benefit from MockitoExtension's strict stubbing detection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocationGateway")
class LocationGatewayTest {

  @InjectMocks private LocationGateway locationGateway;

  @Nested
  @DisplayName("resolveByIdentifier")
  class ResolveByIdentifier {

    @Test
    @DisplayName("should resolve an existing location with correct attributes")
    void shouldResolveExistingLocation() {
      // when
      Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

      // then
      assertNotNull(location);
      assertEquals("ZWOLLE-001", location.identification);
      assertEquals(1, location.maxNumberOfWarehouses);
      assertEquals(40, location.maxCapacity);
    }

    @Test
    @DisplayName("should return null for a non-existing location identifier")
    void shouldReturnNullForNonExistingLocation() {
      // when
      Location location = locationGateway.resolveByIdentifier("NONEXISTENT-001");

      // then
      assertNull(location);
    }
  }
}
