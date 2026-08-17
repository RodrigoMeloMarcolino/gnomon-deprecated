package io.gnomon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AdminPanelIntegrationTest {

  private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");
  private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID STAFF_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
  private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
  private static final UUID OTHER_OWNER_ID =
      UUID.fromString("10000000-0000-0000-0000-000000000004");
  private static final UUID TENANT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_TENANT_ID =
      UUID.fromString("20000000-0000-0000-0000-000000000002");
  private static final UUID OFFERING_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID CALENDAR_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final UUID STAFF_CALENDAR_ID =
      UUID.fromString("40000000-0000-0000-0000-000000000002");
  private static final UUID OTHER_CALENDAR_ID =
      UUID.fromString("40000000-0000-0000-0000-000000000003");
  private static final UUID CUSTOMER_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
  private static final UUID SECOND_CUSTOMER_ID =
      UUID.fromString("50000000-0000-0000-0000-000000000002");
  private static final UUID APPOINTMENT_ID =
      UUID.fromString("60000000-0000-0000-0000-000000000001");
  private static final UUID SECOND_APPOINTMENT_ID =
      UUID.fromString("60000000-0000-0000-0000-000000000002");

  @Container @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Container
  static final GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", redis::getHost);
    properties.add("spring.data.redis.port", redis::getFirstMappedPort);
  }

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private StringRedisTemplate redisTemplate;

  private MockMvc mockMvc;
  private String date;
  private String from;
  private String to;
  private Instant appointmentStartAt;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.execute(
        """
        TRUNCATE TABLE appointment_slots, appointments, customers, availability_rules,
            calendar_offerings, calendars, offerings, collaborators, tenant_memberships,
            tenants, users CASCADE
        """);
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    seed();
  }

  @Test
  void list_whenOwnerUsesFiltersAndPagination_shouldReturnTenantScopedPage() throws Exception {
    mockMvc
        .perform(
            get("/v1/tenants/salon-a/appointments")
                .with(identity("owner-a", "owner-a@example.test", "Owner A"))
                .param("from", from)
                .param("to", to)
                .param("calendar_id", CALENDAR_ID.toString())
                .param("status", "scheduled")
                .param("page", "0")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
        .andExpect(jsonPath("$.content[0].id").value(APPOINTMENT_ID.toString()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.size").value(1));

    mockMvc
        .perform(
            get("/v1/tenants/salon-a/customers")
                .with(identity("admin-a", "admin-a@example.test", "Admin A"))
                .param("page", "0")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
        .andExpect(jsonPath("$.totalElements").value(2));

    mockMvc
        .perform(
            get("/v1/tenants/salon-a/appointments/{id}", APPOINTMENT_ID)
                .with(identity("admin-a", "admin-a@example.test", "Admin A")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(APPOINTMENT_ID.toString()))
        .andExpect(jsonPath("$.customer.id").value(CUSTOMER_ID.toString()));

    mockMvc
        .perform(
            get("/v1/tenants/salon-a/customers/{id}", SECOND_CUSTOMER_ID)
                .with(identity("owner-a", "owner-a@example.test", "Owner A")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(SECOND_CUSTOMER_ID.toString()));
  }

  @Test
  void list_whenStaffRequestsAnotherCalendar_shouldReturnForbidden() throws Exception {
    mockMvc
        .perform(
            get("/v1/tenants/salon-a/appointments")
                .with(identity("staff-a", "staff-a@example.test", "Staff A"))
                .param("from", from)
                .param("to", to)
                .param("calendar_id", CALENDAR_ID.toString()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("staff_calendar_mismatch"));

    mockMvc
        .perform(
            get("/v1/tenants/salon-a/appointments")
                .with(identity("staff-a", "staff-a@example.test", "Staff A"))
                .param("from", from)
                .param("to", to))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
        .andExpect(jsonPath("$.content[0].calendar.id").value(STAFF_CALENDAR_ID.toString()));

    mockMvc
        .perform(
            get("/v1/tenants/salon-a/appointments/{id}", SECOND_APPOINTMENT_ID)
                .with(identity("staff-a", "staff-a@example.test", "Staff A")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/v1/tenants/salon-a/customers")
                .with(identity("staff-a", "staff-a@example.test", "Staff A")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("insufficient_role"));
  }

  @Test
  void crossTenant_whenReadingAppointmentOrCustomer_shouldReturnForbidden() throws Exception {
    mockMvc
        .perform(
            get("/v1/tenants/salon-b/appointments/{id}", APPOINTMENT_ID)
                .with(identity("owner-b", "owner-b@example.test", "Owner B")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("appointment_access_denied"));

    mockMvc
        .perform(
            get("/v1/tenants/salon-b/customers/{id}", CUSTOMER_ID)
                .with(identity("owner-b", "owner-b@example.test", "Owner B")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("customer_access_denied"));
  }

  @Test
  void cancel_whenAppointmentIsScheduled_shouldReleaseSlotsAndRejectSecondTransition()
      throws Exception {
    mockMvc
        .perform(availableSlots())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available_start_times[?(@ == '%s')]", startAt()).doesNotExist());
    assertThat(redisTemplate.keys("gnomon:availability:tenant:*")).isNotEmpty();

    mockMvc
        .perform(
            post("/v1/tenants/salon-a/appointments/{id}/cancel", APPOINTMENT_ID)
                .with(identity("owner-a", "owner-a@example.test", "Owner A")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("cancelled"));

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM appointment_slots WHERE appointment_id=?",
                Integer.class,
                APPOINTMENT_ID))
        .isZero();

    mockMvc
        .perform(availableSlots())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available_start_times[?(@ == '%s')]", startAt()).exists());

    mockMvc
        .perform(
            post("/v1/tenants/salon-a/appointments/{id}/complete", APPOINTMENT_ID)
                .with(identity("owner-a", "owner-a@example.test", "Owner A")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("appointment_status_conflict"));
  }

  @Test
  void transition_whenTwoRequestsRace_shouldAllowOnlyOneStateChange() throws Exception {
    var start = new CountDownLatch(1);
    var submitted = new CountDownLatch(2);
    Callable<MvcResult> cancel = transitionRequest(submitted, start, "/cancel");
    Callable<MvcResult> complete = transitionRequest(submitted, start, "/complete");
    try (var executor = Executors.newFixedThreadPool(2)) {
      List<Future<MvcResult>> futures = List.of(executor.submit(cancel), executor.submit(complete));
      assertThat(submitted.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      var statuses = futures.stream().map(this::statusOf).sorted().toList();
      assertThat(statuses).containsExactly(200, 409);
    }
    String finalStatus =
        jdbc.queryForObject(
            "SELECT status FROM appointments WHERE id=?", String.class, APPOINTMENT_ID);
    int slots =
        jdbc.queryForObject(
            "SELECT count(*) FROM appointment_slots WHERE appointment_id=?",
            Integer.class,
            APPOINTMENT_ID);
    assertThat(finalStatus).isIn("cancelled", "completed");
    assertThat(slots).isEqualTo("cancelled".equals(finalStatus) ? 0 : 2);
  }

  @Test
  void completeAndNoShow_whenSuccessful_shouldPreserveSlots() throws Exception {
    mockMvc
        .perform(
            post("/v1/tenants/salon-a/appointments/{id}/complete", APPOINTMENT_ID)
                .with(identity("owner-a", "owner-a@example.test", "Owner A")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("completed"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM appointment_slots WHERE appointment_id=?",
                Integer.class,
                APPOINTMENT_ID))
        .isEqualTo(2);

    mockMvc
        .perform(
            post("/v1/tenants/salon-a/appointments/{id}/no-show", SECOND_APPOINTMENT_ID)
                .with(identity("owner-a", "owner-a@example.test", "Owner A")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("no_show"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM appointment_slots WHERE appointment_id=?",
                Integer.class,
                SECOND_APPOINTMENT_ID))
        .isEqualTo(2);
  }

  @Test
  void administrativeRoutes_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/v1/tenants/salon-a/appointments")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/v1/tenants/salon-a/customers")).andExpect(status().isUnauthorized());
  }

  private Callable<MvcResult> transitionRequest(
      CountDownLatch submitted, CountDownLatch start, String action) {
    return () -> {
      submitted.countDown();
      start.await();
      return mockMvc
          .perform(
              post("/v1/tenants/salon-a/appointments/{id}" + action, APPOINTMENT_ID)
                  .with(identity("owner-a", "owner-a@example.test", "Owner A")))
          .andReturn();
    };
  }

  private int statusOf(java.util.concurrent.Future<MvcResult> future) {
    try {
      return future.get(10, TimeUnit.SECONDS).getResponse().getStatus();
    } catch (Exception exception) {
      throw new AssertionError("concurrent admin request failed", exception);
    }
  }

  private void seed() {
    LocalDate localDate = LocalDate.now(ZONE).plusDays(7);
    Instant startAt = localDate.atTime(9, 0).atZone(ZONE).toInstant();
    appointmentStartAt = startAt;
    date = localDate.toString();
    from = startAt.minusSeconds(3600).toString();
    to = startAt.plusSeconds(86400).toString();

    jdbc.update(
        "INSERT INTO users (id,keycloak_sub,email,display_name) VALUES (?,?,?,?)",
        OWNER_ID,
        "owner-a",
        "owner-a@example.test",
        "Owner A");
    jdbc.update(
        "INSERT INTO users (id,keycloak_sub,email,display_name) VALUES (?,?,?,?)",
        STAFF_ID,
        "staff-a",
        "staff-a@example.test",
        "Staff A");
    jdbc.update(
        "INSERT INTO users (id,keycloak_sub,email,display_name) VALUES (?,?,?,?)",
        ADMIN_ID,
        "admin-a",
        "admin-a@example.test",
        "Admin A");
    jdbc.update(
        "INSERT INTO users (id,keycloak_sub,email,display_name) VALUES (?,?,?,?)",
        OTHER_OWNER_ID,
        "owner-b",
        "owner-b@example.test",
        "Owner B");
    jdbc.update(
        "INSERT INTO tenants (id,name,slug,timezone,currency_code) VALUES (?,?,?,?,?)",
        TENANT_ID,
        "Salon A",
        "salon-a",
        ZONE.getId(),
        "BRL");
    jdbc.update(
        "INSERT INTO tenants (id,name,slug,timezone,currency_code) VALUES (?,?,?,?,?)",
        OTHER_TENANT_ID,
        "Salon B",
        "salon-b",
        ZONE.getId(),
        "BRL");
    jdbc.update(
        "INSERT INTO tenant_memberships (tenant_id,user_id,role) VALUES (?,?,?)",
        TENANT_ID,
        OWNER_ID,
        "owner");
    jdbc.update(
        "INSERT INTO tenant_memberships (tenant_id,user_id,role) VALUES (?,?,?)",
        TENANT_ID,
        STAFF_ID,
        "staff");
    jdbc.update(
        "INSERT INTO tenant_memberships (tenant_id,user_id,role) VALUES (?,?,?)",
        TENANT_ID,
        ADMIN_ID,
        "admin");
    jdbc.update(
        "INSERT INTO tenant_memberships (tenant_id,user_id,role) VALUES (?,?,?)",
        OTHER_TENANT_ID,
        OTHER_OWNER_ID,
        "owner");
    jdbc.update(
        "INSERT INTO offerings (id,tenant_id,title,duration_minutes,price_cents) VALUES (?,?,?,?,?)",
        OFFERING_ID,
        TENANT_ID,
        "Corte",
        30,
        4500);
    insertCalendar(CALENDAR_ID, UUID.randomUUID(), "Agenda A");
    insertCalendar(STAFF_CALENDAR_ID, UUID.randomUUID(), "Agenda Staff");
    insertCalendar(OTHER_CALENDAR_ID, UUID.randomUUID(), "Agenda B");
    jdbc.update(
        "UPDATE collaborators SET user_id=? WHERE id=(SELECT collaborator_id FROM calendars WHERE id=?)",
        STAFF_ID,
        STAFF_CALENDAR_ID);
    jdbc.update(
        "INSERT INTO calendar_offerings (tenant_id,calendar_id,offering_id) VALUES (?,?,?)",
        TENANT_ID,
        CALENDAR_ID,
        OFFERING_ID);
    jdbc.update(
        "INSERT INTO calendar_offerings (tenant_id,calendar_id,offering_id) VALUES (?,?,?)",
        TENANT_ID,
        STAFF_CALENDAR_ID,
        OFFERING_ID);
    jdbc.update(
        "INSERT INTO availability_rules (tenant_id,calendar_id,weekday,start_time,end_time) VALUES (?,?,?,TIME '08:00',TIME '18:00')",
        TENANT_ID,
        CALENDAR_ID,
        localDate.getDayOfWeek().getValue());
    jdbc.update(
        "INSERT INTO customers (id,name,phone,email) VALUES (?,?,?,?)",
        CUSTOMER_ID,
        "Customer",
        "+5585999999999",
        "customer@example.test");
    jdbc.update(
        "INSERT INTO customers (id,name,phone,email) VALUES (?,?,?,?)",
        SECOND_CUSTOMER_ID,
        "Second Customer",
        "+5585999999998",
        "second.customer@example.test");
    insertAppointment(APPOINTMENT_ID, CALENDAR_ID, startAt, "scheduled", "key-1");
    insertAppointment(
        SECOND_APPOINTMENT_ID,
        STAFF_CALENDAR_ID,
        SECOND_CUSTOMER_ID,
        startAt.plusSeconds(3600),
        "scheduled",
        "key-2");
    jdbc.update(
        "INSERT INTO appointment_slots (tenant_id,appointment_id,calendar_id,slot_start_at) VALUES (?,?,?,?)",
        TENANT_ID,
        APPOINTMENT_ID,
        CALENDAR_ID,
        Timestamp.from(startAt));
    jdbc.update(
        "INSERT INTO appointment_slots (tenant_id,appointment_id,calendar_id,slot_start_at) VALUES (?,?,?,?)",
        TENANT_ID,
        APPOINTMENT_ID,
        CALENDAR_ID,
        Timestamp.from(startAt.plusSeconds(900)));
    jdbc.update(
        "INSERT INTO appointment_slots (tenant_id,appointment_id,calendar_id,slot_start_at) VALUES (?,?,?,?)",
        TENANT_ID,
        SECOND_APPOINTMENT_ID,
        STAFF_CALENDAR_ID,
        Timestamp.from(startAt.plusSeconds(3600)));
    jdbc.update(
        "INSERT INTO appointment_slots (tenant_id,appointment_id,calendar_id,slot_start_at) VALUES (?,?,?,?)",
        TENANT_ID,
        SECOND_APPOINTMENT_ID,
        STAFF_CALENDAR_ID,
        Timestamp.from(startAt.plusSeconds(4500)));
  }

  private void insertCalendar(UUID id, UUID collaboratorId, String name) {
    jdbc.update(
        "INSERT INTO collaborators (id,tenant_id,display_name) VALUES (?,?,?)",
        collaboratorId,
        TENANT_ID,
        name);
    jdbc.update(
        "INSERT INTO calendars (id,tenant_id,collaborator_id,name,timezone) VALUES (?,?,?,?,?)",
        id,
        TENANT_ID,
        collaboratorId,
        name,
        ZONE.getId());
  }

  private void insertAppointment(
      UUID id, UUID calendarId, Instant startAt, String status, String key) {
    insertAppointment(id, calendarId, CUSTOMER_ID, startAt, status, key);
  }

  private void insertAppointment(
      UUID id, UUID calendarId, UUID customerId, Instant startAt, String status, String key) {
    jdbc.update(
        """
        INSERT INTO appointments (id,tenant_id,calendar_id,offering_id,customer_id,start_at,end_at,
            duration_minutes_snapshot,calendar_timezone_snapshot,status,customer_notes,idempotency_key,idempotency_fingerprint)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        """,
        id,
        TENANT_ID,
        calendarId,
        OFFERING_ID,
        customerId,
        Timestamp.from(startAt),
        Timestamp.from(startAt.plusSeconds(1800)),
        30,
        ZONE.getId(),
        status,
        "note",
        key,
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      availableSlots() {
    return get("/v1/public/tenants/salon-a/available-slots")
        .queryParam("calendar_id", CALENDAR_ID.toString())
        .queryParam("offering_id", OFFERING_ID.toString())
        .queryParam("date", date);
  }

  private String startAt() {
    return appointmentStartAt.toString();
  }

  private static JwtRequestPostProcessor identity(String subject, String email, String name) {
    return jwt()
        .jwt(
            token ->
                token
                    .subject(subject)
                    .claim("email", email)
                    .claim("name", name)
                    .claim("email_verified", true));
  }
}
