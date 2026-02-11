package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.exceptions.NotFoundException;
import io.quarkus.panache.common.Sort;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for StoreService — the business logic layer for stores.
 *
 * <p>Uses @Nested inner classes to group tests by operation, producing
 * readable test reports like: StoreService > create > should persist and sync to legacy.
 *
 * <p>Mocks StoreRepository (DB) and LegacyStoreManagerGateway (legacy sync)
 * to test each operation in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService")
class StoreServiceTest {

  @Mock private StoreRepository storeRepository;
  @Mock private LegacyStoreManagerGateway legacyGateway;

  @InjectMocks private StoreService storeService;

  @Nested
  @DisplayName("listAll")
  class ListAll {

    @Test
    @DisplayName("should delegate to repository sorted by name")
    void shouldDelegateToRepositorySortedByName() {
      // given
      Store s1 = newStore(1L, "Alpha", 10);
      Store s2 = newStore(2L, "Beta", 20);
      when(storeRepository.listAll(any(Sort.class))).thenReturn(List.of(s1, s2));

      // when
      List<Store> result = storeService.listAll();

      // then
      assertEquals(2, result.size());
      assertEquals("Alpha", result.get(0).name);
      verify(storeRepository).listAll(any(Sort.class));
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should return store when it exists")
    void shouldReturnStoreWhenExists() {
      // given
      Store store = newStore(1L, "Amsterdam Store", 5);
      when(storeRepository.findById(1L)).thenReturn(store);

      // when
      Store result = storeService.findById(1L);

      // then
      assertNotNull(result);
      assertEquals("Amsterdam Store", result.name);
    }

    @Test
    @DisplayName("should throw NotFoundException when store is missing")
    void shouldThrowNotFoundExceptionWhenMissing() {
      // given
      when(storeRepository.findById(99L)).thenReturn(null);

      // when / then
      NotFoundException ex =
          assertThrows(NotFoundException.class, () -> storeService.findById(99L));
      assertEquals(404, ex.getStatusCode());
      assertTrue(ex.getMessage().contains("Store with id of 99"));
      assertTrue(ex.getMessage().contains("does not exist"));
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should persist store and sync to legacy system")
    void shouldPersistAndSyncToLegacy() {
      // given
      Store store = new Store("New Store");
      store.quantityProductsInStock = 15;

      // when
      Store result = storeService.create(store);

      // then
      verify(storeRepository).persist(store);
      verify(legacyGateway).createStoreOnLegacySystem(store);
      assertEquals("New Store", result.name);
    }

    @Test
    @DisplayName("should call legacy gateway AFTER persist (invocation order)")
    void shouldCallLegacyGatewayAfterPersist() {
      // given
      Store store = new Store("Order Check");
      var order = inOrder(storeRepository, legacyGateway);

      // when
      storeService.create(store);

      // then — persist happens before legacy sync
      order.verify(storeRepository).persist(store);
      order.verify(legacyGateway).createStoreOnLegacySystem(store);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("should apply updateFrom() and sync to legacy")
    void shouldApplyUpdateFromAndSyncToLegacy() {
      // given
      Store existing = newStore(1L, "Old Name", 10);
      when(storeRepository.findById(1L)).thenReturn(existing);

      Store updatedStore = new Store("New Name");
      updatedStore.quantityProductsInStock = 25;

      // when
      Store result = storeService.update(1L, updatedStore);

      // then — updateFrom replaces all fields
      assertEquals("New Name", result.name);
      assertEquals(25, result.quantityProductsInStock);
      verify(legacyGateway).updateStoreOnLegacySystem(existing);
    }

    @Test
    @DisplayName("should throw NotFoundException when store does not exist")
    void shouldThrowNotFoundExceptionWhenStoreDoesNotExist() {
      // given
      when(storeRepository.findById(42L)).thenReturn(null);
      Store updatedStore = new Store("Updated");

      // when / then
      NotFoundException ex =
          assertThrows(NotFoundException.class, () -> storeService.update(42L, updatedStore));
      assertEquals(404, ex.getStatusCode());
      verify(legacyGateway, never()).updateStoreOnLegacySystem(any());
    }
  }

  @Nested
  @DisplayName("patch")
  class Patch {

    @Test
    @DisplayName("should apply patchFrom() (partial) and sync to legacy")
    void shouldApplyPatchFromAndSyncToLegacy() {
      // given
      Store existing = newStore(1L, "Original", 10);
      when(storeRepository.findById(1L)).thenReturn(existing);

      Store patchData = new Store("Patched Name");
      // quantityProductsInStock is 0, so patchFrom should NOT change it

      // when
      Store result = storeService.patch(1L, patchData);

      // then
      assertEquals("Patched Name", result.name);
      assertEquals(10, result.quantityProductsInStock); // unchanged
      verify(legacyGateway).updateStoreOnLegacySystem(existing);
    }

    @Test
    @DisplayName("should throw NotFoundException when store does not exist")
    void shouldThrowNotFoundExceptionWhenStoreDoesNotExist() {
      // given
      when(storeRepository.findById(77L)).thenReturn(null);
      Store patchData = new Store("Patch");

      // when / then
      NotFoundException ex =
          assertThrows(NotFoundException.class, () -> storeService.patch(77L, patchData));
      assertEquals(404, ex.getStatusCode());
      verify(legacyGateway, never()).updateStoreOnLegacySystem(any());
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("should find and remove existing store")
    void shouldRemoveExistingStore() {
      // given
      Store existing = newStore(1L, "To Delete", 5);
      when(storeRepository.findById(1L)).thenReturn(existing);

      // when
      storeService.delete(1L);

      // then
      verify(storeRepository).delete(existing);
    }

    @Test
    @DisplayName("should throw NotFoundException when store does not exist")
    void shouldThrowNotFoundExceptionWhenStoreDoesNotExist() {
      // given
      when(storeRepository.findById(88L)).thenReturn(null);

      // when / then
      NotFoundException ex =
          assertThrows(NotFoundException.class, () -> storeService.delete(88L));
      assertEquals(404, ex.getStatusCode());
      verify(storeRepository, never()).delete(any(Store.class));
    }
  }

  // ── helpers ──────────────────────────────────────────────────────────

  private Store newStore(Long id, String name, int stock) {
    Store s = new Store(name);
    s.id = id;
    s.quantityProductsInStock = stock;
    return s;
  }
}
