package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for StoreResource — the thin HTTP layer.
 *
 * <p>Organized by endpoint using @Nested. Tests HTTP-level concerns only:
 * status codes, validation guards (id-on-create, null-name-on-update/patch),
 * and correct delegation to StoreService.
 *
 * <p>Test report reads as: StoreResource > POST > should return 201 when valid.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreResource")
class StoreResourceTest {

  @Mock private StoreService storeService;

  @InjectMocks private StoreResource storeResource;

  @Nested
  @DisplayName("GET /store")
  class GetAll {

    @Test
    @DisplayName("should delegate to service and return all stores")
    void shouldDelegateToService() {
      // given
      Store s1 = newStore(1L, "Store A", 5);
      when(storeService.listAll()).thenReturn(List.of(s1));

      // when
      List<Store> result = storeResource.get();

      // then
      assertEquals(1, result.size());
      assertEquals("Store A", result.get(0).name);
    }
  }

  @Nested
  @DisplayName("GET /store/{id}")
  class GetSingle {

    @Test
    @DisplayName("should delegate to service and return single store")
    void shouldDelegateToService() {
      // given
      Store store = newStore(1L, "Single Store", 10);
      when(storeService.findById(1L)).thenReturn(store);

      // when
      Store result = storeResource.getSingle(1L);

      // then
      assertEquals("Single Store", result.name);
    }
  }

  @Nested
  @DisplayName("POST /store")
  class Post {

    @Test
    @DisplayName("should return 201 when store is valid")
    void shouldReturn201WhenValid() {
      // given
      Store store = new Store("New Store");
      store.quantityProductsInStock = 10;
      when(storeService.create(store)).thenReturn(store);

      // when
      Response response = storeResource.create(store);

      // then
      assertEquals(201, response.getStatus());
      verify(storeService).create(store);
    }

    @Test
    @DisplayName("should reject with 422 when id is set on request")
    void shouldReject422WhenIdIsSet() {
      // given
      Store store = new Store("Bad Store");
      store.id = 99L; // ID should not be set on create

      // when / then
      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> storeResource.create(store));
      assertEquals(422, ex.getResponse().getStatus());
      assertTrue(ex.getMessage().contains("Id was invalidly set on request"));
      verify(storeService, never()).create(any());
    }
  }

  @Nested
  @DisplayName("PUT /store/{id}")
  class Put {

    @Test
    @DisplayName("should delegate to service when name is present")
    void shouldDelegateToServiceWhenNameIsPresent() {
      // given
      Store updated = new Store("Updated Name");
      updated.quantityProductsInStock = 20;
      when(storeService.update(1L, updated)).thenReturn(updated);

      // when
      Store result = storeResource.update(1L, updated);

      // then
      assertEquals("Updated Name", result.name);
      verify(storeService).update(1L, updated);
    }

    @Test
    @DisplayName("should reject with 422 when name is null")
    void shouldReject422WhenNameIsNull() {
      // given
      Store updated = new Store();
      // name is null

      // when / then
      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> storeResource.update(1L, updated));
      assertEquals(422, ex.getResponse().getStatus());
      assertTrue(ex.getMessage().contains("Store Name was not set on request"));
      verify(storeService, never()).update(anyLong(), any());
    }
  }

  @Nested
  @DisplayName("PATCH /store/{id}")
  class PatchEndpoint {

    @Test
    @DisplayName("should delegate to service when name is present")
    void shouldDelegateToServiceWhenNameIsPresent() {
      // given
      Store patchData = new Store("Patched");
      when(storeService.patch(1L, patchData)).thenReturn(patchData);

      // when
      Store result = storeResource.patch(1L, patchData);

      // then
      assertEquals("Patched", result.name);
      verify(storeService).patch(1L, patchData);
    }

    @Test
    @DisplayName("should reject with 422 when name is null")
    void shouldReject422WhenNameIsNull() {
      // given
      Store patchData = new Store();
      // name is null

      // when / then
      WebApplicationException ex =
          assertThrows(WebApplicationException.class, () -> storeResource.patch(1L, patchData));
      assertEquals(422, ex.getResponse().getStatus());
      assertTrue(ex.getMessage().contains("Store Name was not set on request"));
      verify(storeService, never()).patch(anyLong(), any());
    }
  }

  @Nested
  @DisplayName("DELETE /store/{id}")
  class DeleteEndpoint {

    @Test
    @DisplayName("should delegate to service and return 204")
    void shouldReturn204() {
      // when
      Response response = storeResource.delete(1L);

      // then
      assertEquals(204, response.getStatus());
      verify(storeService).delete(1L);
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
