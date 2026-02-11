package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for Store REST endpoints.
 *
 * Boots the full Quarkus app with Dev Services PostgreSQL.
 * Seed data from import.sql:
 *   - id=1, name=TONSTAD, quantityProductsInStock=10
 *   - id=2, name=KALLAX, quantityProductsInStock=5
 *   - id=3, name=BESTA, quantityProductsInStock=3
 */
@QuarkusTest
@DisplayName("Store REST API")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StoreResourceIT {

  private static final String PATH = "/store";

  // ── GET /store ──────────────────────────────────────────────────────

  @Test
  @Order(1)
  @DisplayName("GET /store → 200 with all seeded stores")
  void shouldListAllStores() {
    given()
        .when().get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("TONSTAD"),
            containsString("KALLAX"),
            containsString("BESTA"));
  }

  // ── GET /store/{id} ─────────────────────────────────────────────────

  @Test
  @Order(2)
  @DisplayName("GET /store/{id} → 200 with matching store")
  void shouldGetStoreById() {
    given()
        .when().get(PATH + "/1")
        .then()
        .statusCode(200)
        .body("name", equalTo("TONSTAD"))
        .body("quantityProductsInStock", equalTo(10));
  }

  @Test
  @Order(3)
  @DisplayName("GET /store/{id} → 404 when not found")
  void shouldReturn404ForUnknownStore() {
    given()
        .when().get(PATH + "/999")
        .then()
        .statusCode(404);
  }

  // ── POST /store ─────────────────────────────────────────────────────

  @Test
  @Order(10)
  @DisplayName("POST /store → 201 creates a new store")
  void shouldCreateStore() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "MALM",
              "quantityProductsInStock": 15
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(201)
        .body("name", equalTo("MALM"))
        .body("quantityProductsInStock", equalTo(15))
        .body("id", notNullValue());
  }

  @Test
  @Order(11)
  @DisplayName("POST /store → 422 when id is set on request")
  void shouldReject422WhenIdSetOnCreate() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "id": 99,
              "name": "HACK",
              "quantityProductsInStock": 0
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  @Order(12)
  @DisplayName("POST /store → 400 when name is blank (Bean Validation)")
  void shouldReject400WhenNameIsBlank() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "",
              "quantityProductsInStock": 5
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(13)
  @DisplayName("POST /store → 400 when stock is negative (Bean Validation)")
  void shouldReject400WhenStockIsNegative() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "NEGATIVE",
              "quantityProductsInStock": -1
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(400);
  }

  // ── PUT /store/{id} ─────────────────────────────────────────────────

  @Test
  @Order(20)
  @DisplayName("PUT /store/{id} → 200 updates the store")
  void shouldUpdateStore() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "TONSTAD-UPDATED",
              "quantityProductsInStock": 25
            }
            """)
        .when().put(PATH + "/1")
        .then()
        .statusCode(200)
        .body("name", equalTo("TONSTAD-UPDATED"))
        .body("quantityProductsInStock", equalTo(25));
  }

  @Test
  @Order(21)
  @DisplayName("PUT /store/{id} → 422 when name is null")
  void shouldReject422WhenNameIsNull() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "quantityProductsInStock": 5
            }
            """)
        .when().put(PATH + "/1")
        .then()
        .statusCode(422);
  }

  // ── PATCH /store/{id} ───────────────────────────────────────────────

  @Test
  @Order(25)
  @DisplayName("PATCH /store/{id} → 200 partial update")
  void shouldPatchStore() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "name": "TONSTAD-PATCHED"
            }
            """)
        .when().patch(PATH + "/1")
        .then()
        .statusCode(200)
        .body("name", equalTo("TONSTAD-PATCHED"));
  }

  // ── DELETE /store/{id} ──────────────────────────────────────────────

  @Test
  @Order(30)
  @DisplayName("DELETE /store/{id} → 204 deletes the store")
  void shouldDeleteStore() {
    given()
        .when().delete(PATH + "/3")
        .then()
        .statusCode(204);
  }

  @Test
  @Order(31)
  @DisplayName("DELETE /store/{id} → 404 when not found")
  void shouldReturn404WhenDeletingUnknown() {
    given()
        .when().delete(PATH + "/999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(32)
  @DisplayName("GET /store → deleted store no longer in list")
  void shouldNotContainDeletedStore() {
    given()
        .when().get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("BESTA")));
  }
}
