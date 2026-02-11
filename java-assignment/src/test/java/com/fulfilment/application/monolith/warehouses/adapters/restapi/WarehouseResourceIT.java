package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

/**
 * Integration tests for Warehouse REST endpoints.
 *
 * Boots the full Quarkus app with Dev Services PostgreSQL.
 * Seed data from import.sql:
 *   - MWH.001 → ZWOLLE-001, capacity=100, stock=10
 *   - MWH.012 → AMSTERDAM-001, capacity=50, stock=5
 *   - MWH.023 → TILBURG-001, capacity=30, stock=27
 *
 * Tests are ordered because some depend on state changes (archive, create).
 */
@QuarkusTest
@DisplayName("Warehouse REST API")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WarehouseResourceIT {

  private static final String PATH = "/warehouse";

  // ── GET /warehouse ──────────────────────────────────────────────────

  @Test
  @Order(1)
  @DisplayName("GET /warehouse → 200 with all seeded warehouses")
  void shouldListAllWarehouses() {
    given()
        .when().get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"));
  }

  // ── GET /warehouse/{id} ─────────────────────────────────────────────

  @Test
  @Order(2)
  @DisplayName("GET /warehouse/{id} → 200 with matching warehouse")
  void shouldGetWarehouseById() {
    given()
        .when().get(PATH + "/MWH.001")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.001"))
        .body("location", equalTo("ZWOLLE-001"))
        .body("capacity", equalTo(100))
        .body("stock", equalTo(10));
  }

  @Test
  @Order(3)
  @DisplayName("GET /warehouse/{id} → 404 when not found")
  void shouldReturn404ForUnknownWarehouse() {
    given()
        .when().get(PATH + "/DOES-NOT-EXIST")
        .then()
        .statusCode(404);
  }

  // ── POST /warehouse (Create) ────────────────────────────────────────

  @Test
  @Order(10)
  @DisplayName("POST /warehouse → 200 creates a new warehouse")
  void shouldCreateWarehouse() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "MWH.100",
              "location": "AMSTERDAM-001",
              "capacity": 80,
              "stock": 0
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.100"))
        .body("location", equalTo("AMSTERDAM-001"))
        .body("capacity", equalTo(80));
  }

  @Test
  @Order(11)
  @DisplayName("POST /warehouse → 409 when business unit code already exists")
  void shouldReject409ForDuplicateBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "MWH.001",
              "location": "AMSTERDAM-001",
              "capacity": 50,
              "stock": 0
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(409);
  }

  @Test
  @Order(12)
  @DisplayName("POST /warehouse → 400 when location is invalid")
  void shouldReject400ForInvalidLocation() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "MWH.INVALID-LOC",
              "location": "NARNIA-001",
              "capacity": 50,
              "stock": 0
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(13)
  @DisplayName("POST /warehouse → 409 when max warehouses at location reached")
  void shouldReject409WhenMaxWarehousesReached() {
    // ZWOLLE-001 allows max 1 warehouse, MWH.001 is already there
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "MWH.ZWOLLE-FULL",
              "location": "ZWOLLE-001",
              "capacity": 30,
              "stock": 0
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(409);
  }

  @Test
  @Order(14)
  @DisplayName("POST /warehouse → 400 when capacity exceeds location maximum")
  void shouldReject400WhenCapacityExceedsLocationMax() {
    // AMSTERDAM-001 maxCapacity is 100
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "MWH.OVER-CAP",
              "location": "AMSTERDAM-001",
              "capacity": 999,
              "stock": 0
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(15)
  @DisplayName("POST /warehouse → 400 when stock exceeds capacity")
  void shouldReject400WhenStockExceedsCapacity() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "MWH.OVER-STOCK",
              "location": "AMSTERDAM-001",
              "capacity": 50,
              "stock": 100
            }
            """)
        .when().post(PATH)
        .then()
        .statusCode(400);
  }

  // ── DELETE /warehouse/{id} (Archive) ────────────────────────────────

  @Test
  @Order(20)
  @DisplayName("DELETE /warehouse/{id} → 204 archives the warehouse")
  void shouldArchiveWarehouse() {
    given()
        .when().delete(PATH + "/MWH.023")
        .then()
        .statusCode(204);
  }

  @Test
  @Order(21)
  @DisplayName("DELETE /warehouse/{id} → 409 when already archived")
  void shouldReject409WhenAlreadyArchived() {
    // MWH.023 was archived in the previous test
    given()
        .when().delete(PATH + "/MWH.023")
        .then()
        .statusCode(409);
  }

  @Test
  @Order(22)
  @DisplayName("DELETE /warehouse/{id} → 404 when not found")
  void shouldReturn404WhenArchivingUnknown() {
    given()
        .when().delete(PATH + "/DOES-NOT-EXIST")
        .then()
        .statusCode(404);
  }

  // ── POST /warehouse/{buCode}/replacement ────────────────────────────

  @Test
  @Order(30)
  @DisplayName("POST /warehouse/{buCode}/replacement → 200 replaces the warehouse")
  void shouldReplaceWarehouse() {
    // MWH.012 → AMSTERDAM-001, stock=5
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "MWH.012",
              "location": "AMSTERDAM-002",
              "capacity": 60,
              "stock": 5
            }
            """)
        .when().post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.012"))
        .body("location", equalTo("AMSTERDAM-002"))
        .body("capacity", equalTo(60))
        .body("stock", equalTo(5));
  }

  @Test
  @Order(31)
  @DisplayName("POST /warehouse/{buCode}/replacement → 404 when not found")
  void shouldReturn404WhenReplacingUnknown() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "NOPE",
              "location": "AMSTERDAM-001",
              "capacity": 50,
              "stock": 0
            }
            """)
        .when().post(PATH + "/NOPE/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(32)
  @DisplayName("POST /warehouse/{buCode}/replacement → 400 when capacity can't hold old stock")
  void shouldReject400WhenReplacementCapacityTooSmall() {
    // MWH.001 has stock=10, new capacity=5 can't hold it
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "MWH.001",
              "location": "AMSTERDAM-001",
              "capacity": 5,
              "stock": 10
            }
            """)
        .when().post(PATH + "/MWH.001/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(33)
  @DisplayName("POST /warehouse/{buCode}/replacement → 400 when stock doesn't match")
  void shouldReject400WhenStockDoesNotMatch() {
    // MWH.001 has stock=10, requesting stock=999
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
              "businessUnitCode": "MWH.001",
              "location": "AMSTERDAM-001",
              "capacity": 80,
              "stock": 999
            }
            """)
        .when().post(PATH + "/MWH.001/replacement")
        .then()
        .statusCode(400);
  }
}
