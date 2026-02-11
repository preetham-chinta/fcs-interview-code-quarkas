-- V1.0.0 - Initial Schema Creation (production only)
--
-- Mirrors Panache entity definitions.
-- Dev/test: Panache auto-generates schema via drop-and-create.
-- Prod: This Flyway migration creates the schema.

-- Sequences (Panache default allocationSize = 50)
CREATE SEQUENCE IF NOT EXISTS store_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS product_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS warehouse_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS AuditLog_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE store (
    id                        BIGINT       NOT NULL DEFAULT nextval('store_seq'),
    name                      VARCHAR(40)  NOT NULL UNIQUE,
    quantityProductsInStock   INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE product (
    id          BIGINT         NOT NULL DEFAULT nextval('product_seq'),
    name        VARCHAR(40)    NOT NULL UNIQUE,
    description VARCHAR(255),
    price       DECIMAL(10,2),
    stock       INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE warehouse (
    id                BIGINT       NOT NULL DEFAULT nextval('warehouse_seq'),
    businessUnitCode  VARCHAR(255),
    location          VARCHAR(255),
    capacity          INT,
    stock             INT,
    createdAt         TIMESTAMP,
    archivedAt        TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE AuditLog (
    id            BIGINT       NOT NULL DEFAULT nextval('AuditLog_seq'),
    resourceName  VARCHAR(255),
    action        VARCHAR(50),
    performedBy   VARCHAR(255),
    outcome       VARCHAR(50),
    timestamp     TIMESTAMP,
    PRIMARY KEY (id)
);
