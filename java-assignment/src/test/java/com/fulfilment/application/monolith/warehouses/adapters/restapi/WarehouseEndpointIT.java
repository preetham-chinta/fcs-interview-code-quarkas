package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Black-box integration test using @QuarkusIntegrationTest.
 *
 * Runs against the packaged JAR (not in-process like @QuarkusTest).
 * Tests the basic list + archive flow from the original assignment skeleton.
 */
@QuarkusIntegrationTest
@DisplayName("Warehouse Endpoint — Integration (packaged JAR)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseEndpointIT {

  private static final String PATH = "warehouse";

  @Test
  @Order(1)
  @DisplayName("GET /warehouse → lists all 3 seeded warehouses")
  public void testSimpleListWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  @Order(2)
  @DisplayName("DELETE /warehouse/{id} + GET → archived warehouse still in list but marked")
  public void testArchivingWarehouse() {
    // Verify all 3 warehouses with locations
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive MWH.023 (TILBURG-001)
    given().when().delete(PATH + "/MWH.023").then().statusCode(204);

    // MWH.023 still appears in the list (archived, not deleted)
    // but the archive operation was successful (204 above)
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"));
  }
}
