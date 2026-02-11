package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.audit.Audited;
import com.fulfilment.application.monolith.logging.Logged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Store REST resource — thin HTTP layer.
 *
 * All business logic (events, persistence, lookups) is delegated to StoreService.
 * This class only handles HTTP concerns: routing, validation, status codes.
 *
 * Cross-cutting concerns are handled declaratively:
 *   - @Audited → CDI interceptor auto-audits POST/PUT/PATCH/DELETE
 *   - @Logged  → CDI interceptor logs method entry/exit with execution time
 *   - @Valid   → Bean Validation on request body
 */
@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Audited
@Logged
public class StoreResource {

  @Inject StoreService storeService;

  // ── CDI event-based approach (before service layer extraction) ─────
  // Previously the resource fired events directly:
  //   @Inject Event<StoreEvent> storeEvent;
  // ───────────────────────────────────────────────────────────────────

  @GET
  public List<Store> get() {
    return storeService.listAll();
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    return storeService.findById(id);
  }

  @POST
  public Response create(@Valid Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }
    Store created = storeService.create(store);
    // storeEvent.fire(new StoreEvent(store, StoreEvent.Type.CREATED));
    return Response.ok(created).status(201).build();
  }

  @PUT
  @Path("{id}")
  public Store update(Long id, @Valid Store updatedStore) {
    // @Valid handles @NotBlank/@Size/@PositiveOrZero (returns 400).
    // This explicit check preserves the original 422 + message for null name.
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }
    // storeEvent.fire(new StoreEvent(entity, StoreEvent.Type.UPDATED));
    return storeService.update(id, updatedStore);
  }

  @PATCH
  @Path("{id}")
  public Store patch(Long id, Store updatedStore) {
    // No @Valid here — PATCH is partial, so Bean Validation would be too strict.
    // But the original code required name even on PATCH, so we check explicitly.
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }
    // storeEvent.fire(new StoreEvent(entity, StoreEvent.Type.UPDATED));
    return storeService.patch(id, updatedStore);
  }

  @DELETE
  @Path("{id}")
  public Response delete(Long id) {
    storeService.delete(id);
    return Response.status(204).build();
  }
}
