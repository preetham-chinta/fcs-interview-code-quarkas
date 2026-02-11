package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.exceptions.ConflictException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CreateWarehouseUseCase.
 *
 * <p>Uses a real WarehouseValidator with mocked dependencies so that
 * tests validate end-to-end behaviour, not just delegation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateWarehouseUseCase")
class CreateWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    WarehouseValidator validator = new WarehouseValidator(warehouseStore, locationResolver);
    useCase = new CreateWarehouseUseCase(warehouseStore, validator);
  }

  @Nested
  @DisplayName("create — happy path")
  class HappyPath {

    @Test
    @DisplayName("should create warehouse and set createdAt when all validations pass")
    void shouldCreateWarehouseWhenAllValidationsPass() {
      // given
      Warehouse warehouse = newWarehouse("MWH.100", "AMSTERDAM-001", 50, 10);
      when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
          .thenReturn(new Location("AMSTERDAM-001", 5, 100));
      when(warehouseStore.getAll()).thenReturn(List.of());

      // when
      useCase.create(warehouse);

      // then
      verify(warehouseStore).create(warehouse);
      assertNotNull(warehouse.createdAt, "createdAt should be set");
    }
  }

  @Nested
  @DisplayName("create — business unit code validation")
  class BusinessUnitCodeValidation {

    @Test
    @DisplayName("should reject duplicate business unit code with 409")
    void shouldRejectDuplicateBusinessUnitCode() {
      // given
      Warehouse warehouse = newWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);
      when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(new Warehouse());

      // when / then
      ConflictException ex =
          assertThrows(ConflictException.class, () -> useCase.create(warehouse));
      assertEquals(409, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("already exists"));
      verify(warehouseStore, never()).create(any());
    }
  }

  @Nested
  @DisplayName("create — location validation")
  class LocationValidation {

    @Test
    @DisplayName("should reject invalid location with 400")
    void shouldRejectInvalidLocation() {
      // given
      Warehouse warehouse = newWarehouse("MWH.100", "INVALID-LOC", 50, 10);
      when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("INVALID-LOC")).thenReturn(null);

      // when / then
      ValidationException ex =
          assertThrows(ValidationException.class, () -> useCase.create(warehouse));
      assertEquals(400, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("not a valid location"));
      verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("should reject when max warehouses at location reached (409)")
    void shouldRejectWhenMaxWarehousesAtLocationReached() {
      // given — ZWOLLE-001 allows max 1 warehouse, and 1 active one already exists
      Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 30, 5);
      when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
          .thenReturn(new Location("ZWOLLE-001", 1, 40));

      Warehouse existing = newWarehouse("MWH.001", "ZWOLLE-001", 30, 10);
      when(warehouseStore.getAll()).thenReturn(List.of(existing));

      // when / then
      ConflictException ex =
          assertThrows(ConflictException.class, () -> useCase.create(warehouse));
      assertEquals(409, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("Maximum number of warehouses"));
      verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("should not count archived warehouses toward location limit")
    void shouldNotCountArchivedWarehousesForLocationLimit() {
      // given — ZWOLLE-001 allows max 1, but the existing one is archived
      Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 30, 5);
      when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
          .thenReturn(new Location("ZWOLLE-001", 1, 40));

      Warehouse archived = newWarehouse("MWH.001", "ZWOLLE-001", 30, 10);
      archived.archivedAt = LocalDateTime.now();
      when(warehouseStore.getAll()).thenReturn(List.of(archived));

      // when
      useCase.create(warehouse);

      // then — should succeed since archived warehouses don't count
      verify(warehouseStore).create(warehouse);
    }
  }

  @Nested
  @DisplayName("create — capacity validation")
  class CapacityValidation {

    @Test
    @DisplayName("should reject when capacity exceeds location maximum (400)")
    void shouldRejectWhenCapacityExceedsLocationMax() {
      // given — location max capacity is 40, warehouse wants 50
      Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 50, 10);
      when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
          .thenReturn(new Location("ZWOLLE-001", 1, 40));
      when(warehouseStore.getAll()).thenReturn(List.of());

      // when / then
      ValidationException ex =
          assertThrows(ValidationException.class, () -> useCase.create(warehouse));
      assertEquals(400, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("exceeds maximum capacity"));
      verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("should reject when stock exceeds capacity (400)")
    void shouldRejectWhenStockExceedsCapacity() {
      // given — capacity 30 but stock 50
      Warehouse warehouse = newWarehouse("MWH.100", "AMSTERDAM-001", 30, 50);
      when(warehouseStore.findByBusinessUnitCode("MWH.100")).thenReturn(null);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
          .thenReturn(new Location("AMSTERDAM-001", 5, 100));
      when(warehouseStore.getAll()).thenReturn(List.of());

      // when / then
      ValidationException ex =
          assertThrows(ValidationException.class, () -> useCase.create(warehouse));
      assertEquals(400, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("cannot handle the stock"));
      verify(warehouseStore, never()).create(any());
    }
  }

  private Warehouse newWarehouse(String buCode, String location, int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }
}
