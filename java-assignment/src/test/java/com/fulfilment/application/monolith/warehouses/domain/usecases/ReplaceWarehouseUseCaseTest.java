package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.exceptions.ConflictException;
import com.fulfilment.application.monolith.exceptions.NotFoundException;
import com.fulfilment.application.monolith.exceptions.ValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ReplaceWarehouseUseCase.
 *
 * <p>Uses a real WarehouseValidator with mocked dependencies so that
 * tests validate end-to-end behaviour, not just delegation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReplaceWarehouseUseCase")
class ReplaceWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;
  @Mock private ArchiveWarehouseOperation archiveOperation;

  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    WarehouseValidator validator = new WarehouseValidator(warehouseStore, locationResolver);
    useCase = new ReplaceWarehouseUseCase(warehouseStore, validator, archiveOperation);
  }

  @Nested
  @DisplayName("replace — happy path")
  class HappyPath {

    @Test
    @DisplayName("should archive existing, create replacement, and set createdAt")
    void shouldReplaceWarehouseWhenAllValidationsPass() {
      // given
      Warehouse existing = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
      when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
          .thenReturn(new Location("AMSTERDAM-001", 5, 100));

      Warehouse replacement = newWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);

      // when
      useCase.replace(replacement);

      // then
      verify(archiveOperation).archive(existing);
      verify(warehouseStore).create(replacement);
      assertNotNull(replacement.createdAt);
    }
  }

  @Nested
  @DisplayName("replace — existing warehouse validation")
  class ExistingWarehouseValidation {

    @Test
    @DisplayName("should reject with 404 when existing warehouse not found")
    void shouldRejectWhenExistingWarehouseNotFound() {
      // given
      when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);
      Warehouse replacement = newWarehouse("MWH.999", "AMSTERDAM-001", 50, 10);

      // when / then
      NotFoundException ex =
          assertThrows(NotFoundException.class, () -> useCase.replace(replacement));
      assertEquals(404, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    @DisplayName("should reject with 409 when existing warehouse is already archived")
    void shouldRejectWhenExistingWarehouseIsAlreadyArchived() {
      // given
      Warehouse existing = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
      existing.archivedAt = LocalDateTime.now();
      when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

      Warehouse replacement = newWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);

      // when / then
      ConflictException ex =
          assertThrows(ConflictException.class, () -> useCase.replace(replacement));
      assertEquals(409, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("already archived"));
    }
  }

  @Nested
  @DisplayName("replace — location and capacity validation")
  class LocationAndCapacityValidation {

    @Test
    @DisplayName("should reject with 400 when new location is invalid")
    void shouldRejectWhenNewLocationIsInvalid() {
      // given
      Warehouse existing = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
      when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("INVALID")).thenReturn(null);

      Warehouse replacement = newWarehouse("MWH.001", "INVALID", 50, 10);

      // when / then
      ValidationException ex =
          assertThrows(ValidationException.class, () -> useCase.replace(replacement));
      assertEquals(400, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("not a valid location"));
    }

    @Test
    @DisplayName("should reject with 400 when new capacity exceeds location maximum")
    void shouldRejectWhenNewCapacityExceedsLocationMax() {
      // given
      Warehouse existing = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
      when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
          .thenReturn(new Location("ZWOLLE-001", 1, 40));

      Warehouse replacement = newWarehouse("MWH.001", "ZWOLLE-001", 50, 10);

      // when / then
      ValidationException ex =
          assertThrows(ValidationException.class, () -> useCase.replace(replacement));
      assertEquals(400, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("exceeds maximum capacity"));
    }
  }

  @Nested
  @DisplayName("replace — stock transfer validation")
  class StockTransferValidation {

    @Test
    @DisplayName("should reject with 400 when new capacity cannot accommodate existing stock")
    void shouldRejectWhenNewCapacityCannotAccommodateExistingStock() {
      // given — existing has 30 stock, new warehouse capacity is only 20
      Warehouse existing = newWarehouse("MWH.001", "ZWOLLE-001", 40, 30);
      when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
          .thenReturn(new Location("AMSTERDAM-001", 5, 100));

      Warehouse replacement = newWarehouse("MWH.001", "AMSTERDAM-001", 20, 30);

      // when / then
      ValidationException ex =
          assertThrows(ValidationException.class, () -> useCase.replace(replacement));
      assertEquals(400, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("cannot accommodate"));
    }

    @Test
    @DisplayName("should reject with 400 when new stock does not match existing stock")
    void shouldRejectWhenNewStockDoesNotMatchExistingStock() {
      // given — existing has 10 stock, new warehouse claims 5
      Warehouse existing = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
      when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
      when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
          .thenReturn(new Location("AMSTERDAM-001", 5, 100));

      Warehouse replacement = newWarehouse("MWH.001", "AMSTERDAM-001", 50, 5);

      // when / then
      ValidationException ex =
          assertThrows(ValidationException.class, () -> useCase.replace(replacement));
      assertEquals(400, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("must match"));
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
