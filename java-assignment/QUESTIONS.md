# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
Yes — the original codebase mixed two Panache patterns inconsistently:

- Store used PanacheEntity (active record): Store.findById(), store.persist(), entity.delete().
  The entity itself contained persistence methods, mixing data and behavior in one class.

- Product used PanacheRepository: a separate ProductRepository with PanacheRepository<Product>.
  The entity was a plain JPA class; persistence was delegated to a repository bean.

- Warehouse used a hexagonal approach: a domain model (Warehouse POJO), a JPA entity
  (DbWarehouse), a port interface (WarehouseStore), and a repository adapter that implements both
  WarehouseStore and PanacheRepository<DbWarehouse>.

I refactored towards consistency:

1. Converted Store from PanacheEntity → plain JPA entity + StoreRepository (PanacheRepository<Store>).
   Now Store and Product follow the same pattern. This is better because:
   - Entities stay as pure data carriers (Single Responsibility)
   - Repositories are injectable CDI beans, making them mockable in unit tests
   - Active record mixes persistence concerns into the domain model, which becomes
     problematic as the codebase grows

2. Added StoreService as a business logic layer (Resource → Service → Repository).
   Store has real side effects (legacy gateway sync, CDI events) that don't belong in a
   resource or repository. Product has no business logic beyond CRUD, so a service there
   would be a passthrough — not worth the extra layer.

3. Warehouse's hexagonal architecture (ports + adapters) is good for its complexity —
   it has multiple use cases with validation rules, a domain model distinct from its JPA
   entity, and a generated OpenAPI interface. I kept that architecture and implemented
   the missing pieces. For simpler domains like Store/Product, hexagonal would be
   over-engineering.

The guiding principle: use the lightest architecture that handles your domain's complexity.
Simple CRUD → Resource + Repository. Business logic → add a Service. Complex domain with
multiple use cases and external contracts → hexagonal with ports and adapters.

----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
OpenAPI-first (code generation) — used by Warehouse:

  Pros:
  - Contract-driven: the API spec IS the source of truth. Frontend teams, testers,
    and documentation all derive from the same YAML. No drift between docs and code.
  - Auto-generated DTOs and interfaces: less boilerplate to write and maintain.
    The generated WarehouseResource interface and Warehouse bean are guaranteed to
    match the spec.
  - Better for cross-team collaboration: teams can agree on the API contract before
    any implementation starts. Backend and frontend can develop in parallel.
  - Tooling ecosystem: generated clients, mock servers, validation middleware all
    come "for free" from the spec.

  Cons:
  - Build complexity: requires a code generation plugin in the build (openapi-generator).
    Generated code lives in target/, which can confuse IDEs and developers.
  - Less flexibility during rapid iteration: changing a field means editing YAML, then
    regenerating, then adapting your implementation. More friction for quick changes.
  - Generated code style may not match team conventions (e.g., the generated Warehouse
    bean uses getters/setters while the rest of the codebase uses public fields).
  - Debugging: a layer of indirection between what you write and what runs.

Code-first — used by Store and Product:

  Pros:
  - Faster iteration: change the Java class and you're done. No generation step.
  - Full control: annotations, custom validation, response types are all in your hands.
  - Simpler build: no extra plugins or generated source directories.
  - In Quarkus, you can still generate an OpenAPI spec FROM the code using the
    quarkus-smallrye-openapi extension — so you get documentation without code-gen.

  Cons:
  - Spec drift: the API behavior is defined implicitly by the code. If someone changes
    a field name, there's no spec file that breaks. Documentation can go stale.
  - No contract to share upfront with consumers.

My choice: Code-first with generated documentation.

For a monolith like this, I'd use code-first for all endpoints (including Warehouse) and
add quarkus-smallrye-openapi to auto-generate the spec from the JAX-RS annotations. This
gives you the best of both worlds: fast iteration, full control, AND an always-accurate
OpenAPI spec that consumers can use.

I'd switch to API-first (code generation) only when:
- Multiple teams consume the API and need a contract before implementation starts
- The API is a public-facing product with versioning guarantees
- You're building microservices where the contract IS the coupling boundary

----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
I'd prioritize tests in this order, highest ROI first:

1. USE CASE UNIT TESTS (highest priority)
   The warehouse use cases (CreateWarehouseUseCase, ReplaceWarehouseUseCase,
   ArchiveWarehouseUseCase) contain the core business rules — validation logic that
   protects data integrity. These are pure domain logic with injected ports, so they're
   easy to unit test with mocked WarehouseStore and LocationResolver.

   Example test cases for CreateWarehouseUseCase:
   - Duplicate business unit code → 409
   - Invalid location → 400
   - Max warehouses at location exceeded → 409
   - Capacity exceeds location max → 400
   - Stock exceeds capacity → 400
   - Happy path → warehouse created with timestamp

   These tests are fast (no container, no DB), catch the most critical bugs, and
   document the business rules in executable form.

2. INTEGRATION TESTS (REST endpoints)
   Using @QuarkusTest + RestAssured (like the existing WarehouseEndpointIT) to test
   the full HTTP path: request → resource → service/use case → repository → DB → response.
   These catch wiring issues (CDI injection, transaction boundaries, exception mapping).

   Key scenarios:
   - Store CRUD with validation errors (name null, id set on create)
   - Store create → legacy gateway called within transaction
   - Warehouse create/replace/archive with all validation paths
   - GlobalExceptionHandler produces correct ErrorResponse format for each exception type

3. REPOSITORY TESTS
   Verify Panache queries work correctly — especially WarehouseRepository.findByBusinessUnitCode()
   and the update-by-businessUnitCode logic. Use @QuarkusTest with PostgreSQL Dev Services
   (Testcontainers) so they run against a real PostgreSQL instance — same dialect as production.

4. SERVICE LAYER TESTS (StoreService)
   Verify that StoreService correctly delegates to StoreRepository AND calls the
   LegacyStoreManagerGateway within the same transaction. Mock the gateway to verify
   it's called with MANDATORY propagation — if called outside a tx, it should throw
   TransactionRequiredException.

What I'd skip or defer:
- UI/E2E tests: no frontend in this project
- LocationGateway unit tests: it's a static list with a stream filter — trivially correct
- Audit interceptor tests: cross-cutting, tricky to unit test, better covered by
  integration tests that verify audit logs appear after operations

Keeping coverage effective over time:
- CI pipeline with test execution on every PR (GitHub Actions)
- Fail the build if any test fails — no "known failures" allowed
- Focus on testing BEHAVIOR (what the system does) not IMPLEMENTATION (how it does it).
  Tests that assert on internal method calls break on every refactor.
  Tests that assert on HTTP responses and DB state survive refactors.
- Use PostgreSQL Dev Services (Testcontainers) for all profiles — dev, test, and CI all
  run against real PostgreSQL containers. This eliminates H2-vs-PostgreSQL dialect mismatches
  and ensures tests validate the same SQL behavior as production. The only requirement is
  Docker running locally (or available in CI), which is a reasonable trade-off for test fidelity.

----

## Additional Implementation Notes

### Global Exception Handling
I introduced a centralized exception handling pattern — similar to Spring Boot's
@ControllerAdvice but using Quarkus's @Provider + ExceptionMapper<Exception>.

A single GlobalExceptionHandler catches every unhandled exception and maps it
to a consistent ErrorResponse JSON shape (with errorId, code, message, timestamp).
This way, API consumers always get the same error format regardless of error type.

I created a custom exception hierarchy to keep the domain layer clean:

  BusinessException (abstract)
    ├── NotFoundException     → 404
    ├── ConflictException     → 409
    └── ValidationException   → 400

The handler resolves status codes in priority order:
  ConstraintViolationException (Bean Validation) → BusinessException → WebApplicationException → 500

The warehouse use cases throw ConflictException, ValidationException, and
NotFoundException instead of WebApplicationException directly. This keeps
domain code free of HTTP/JAX-RS dependencies — the use cases don't know they're
running behind a REST API. The global handler takes care of the HTTP translation.

5xx errors are logged with full stack traces; 
4xx errors are logged as warnings only.

----

### Cross-Cutting Concerns: Logging & Audit Interceptors

I implemented two CDI interceptors for cross-cutting concerns — this is the
Quarkus equivalent of Spring AOP (@Aspect + @Around). The mechanism is the same:
the container generates a proxy subclass that wraps method calls, running
interceptor logic before/after without touching business code.

@Logged (LoggingInterceptor):
  Annotate a class or method → automatically logs entry (with params), exit
  (with execution time in ms), and exceptions. Unwraps CDI proxy class names
  so logs show "StoreService" not "StoreService_Subclass". Useful for
  debugging and performance monitoring without cluttering business methods.

@Audited (AuditInterceptor):
  Annotate a resource class → automatically audits mutating operations.
  The interceptor reads JAX-RS annotations (@POST, @PUT, @PATCH, @DELETE)
  to determine the action type and skips GETs. It persists an AuditLog
  entity to the database via AuditService, which runs in a REQUIRES_NEW
  transaction so audit records survive even if the business transaction rolls back.

Interceptor ordering is controlled via @Priority — LoggingInterceptor runs
first (outermost), so the logged execution time includes audit overhead.

Both interceptors follow the same principle: business methods stay clean,
and new methods added to annotated classes get logging/auditing automatically.

----

### Testing Strategy

The project has two layers of automated tests, both integrated into the CI pipeline:

Unit Tests (Surefire — *Test.java):
  Pure JUnit 5 + Mockito tests for business logic. Each warehouse use case
  (Create, Replace, Archive) has its own test class organized with @Nested
  by validation rule. Dependencies (WarehouseStore, LocationResolver) are
  mocked — tests run fast with no container or DB. StoreService, StoreResource,
  ProductResource, and GlobalExceptionHandler also have dedicated unit tests.

Integration Tests (Failsafe — *IT.java):
  @QuarkusTest + RestAssured tests that hit the real HTTP endpoints with a
  live PostgreSQL instance (via Dev Services / Testcontainers). These test
  the full stack: HTTP request → resource → use case → repository → DB → response.
  WarehouseResourceIT covers all 5 endpoints with every validation path.
  StoreResourceIT covers Store CRUD including Bean Validation error scenarios.

Both run in CI via `./mvnw verify` — Surefire in the test phase,
Failsafe in the integration-test phase. JaCoCo generates coverage reports.

----