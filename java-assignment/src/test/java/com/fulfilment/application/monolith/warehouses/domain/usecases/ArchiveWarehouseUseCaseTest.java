package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.exceptions.ConflictException;
import com.fulfilment.application.monolith.exceptions.NotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
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
 * Unit tests for ArchiveWarehouseUseCase.
 *
 * <p>Uses a real WarehouseValidator with mocked dependencies so that
 * tests validate end-to-end behaviour, not just delegation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveWarehouseUseCase")
class ArchiveWarehouseUseCaseTest {

  @Mock private WarehouseStore warehouseStore;
  @Mock private LocationResolver locationResolver;

  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    WarehouseValidator validator = new WarehouseValidator(warehouseStore, locationResolver);
    useCase = new ArchiveWarehouseUseCase(warehouseStore, validator);
  }

  @Nested
  @DisplayName("archive")
  class Archive {

    @Test
    @DisplayName("should set archivedAt and persist when warehouse is active")
    void shouldArchiveActiveWarehouse() {
      // given
      Warehouse existing = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
      when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

      Warehouse toArchive = new Warehouse();
      toArchive.businessUnitCode = "MWH.001";

      // when
      useCase.archive(toArchive);

      // then
      verify(warehouseStore).update(argThat(w -> w.archivedAt != null));
    }

    @Test
    @DisplayName("should reject with 404 when warehouse does not exist")
    void shouldRejectWhenWarehouseDoesNotExist() {
      // given
      when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);
      Warehouse toArchive = new Warehouse();
      toArchive.businessUnitCode = "MWH.999";

      // when / then
      NotFoundException ex =
          assertThrows(NotFoundException.class, () -> useCase.archive(toArchive));
      assertEquals(404, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("does not exist"));
      verify(warehouseStore, never()).update(any());
    }

    @Test
    @DisplayName("should reject with 409 when warehouse is already archived")
    void shouldRejectWhenWarehouseIsAlreadyArchived() {
      // given
      Warehouse existing = newWarehouse("MWH.001", "ZWOLLE-001", 40, 10);
      existing.archivedAt = LocalDateTime.now();
      when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

      Warehouse toArchive = new Warehouse();
      toArchive.businessUnitCode = "MWH.001";

      // when / then
      ConflictException ex =
          assertThrows(ConflictException.class, () -> useCase.archive(toArchive));
      assertEquals(409, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("already archived"));
      verify(warehouseStore, never()).update(any());
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
