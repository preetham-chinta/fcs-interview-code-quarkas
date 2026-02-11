package com.fulfilment.application.monolith.products;

import com.fulfilment.application.monolith.exceptions.NotFoundException;
import com.fulfilment.application.monolith.logging.Logged;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("product")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Logged
public class ProductResource {

  @Inject ProductRepository productRepository;

  @GET
  public List<Product> get() {
    return productRepository.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Product getSingle(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new NotFoundException("Product", id);
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(@Valid Product product) {
    if (product.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    productRepository.persist(product);
    return Response.ok(product).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Product update(Long id, @Valid Product product) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new NotFoundException("Product", id);
    }

    entity.updateFrom(product);
    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Product entity = productRepository.findById(id);
    if (entity == null) {
      throw new NotFoundException("Product", id);
    }
    productRepository.delete(entity);
    return Response.status(204).build();
  }
}

//moved the ErrorMapper to the ExceptionHandler