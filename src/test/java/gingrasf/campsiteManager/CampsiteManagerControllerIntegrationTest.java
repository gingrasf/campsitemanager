package gingrasf.campsiteManager;


import gingrasf.campsiteManager.model.CampsiteAvailability;
import gingrasf.campsiteManager.model.CampsiteReservation;
import gingrasf.campsiteManager.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static gingrasf.campsiteManager.CampsiteReservationValidator.MAX_RESERVATION_DURATION;
import static gingrasf.campsiteManager.TestUtil.buildReservationRequest;
import static gingrasf.campsiteManager.TestUtil.buildValidUser;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CampsiteManagerControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MongoTemplate mongoTemplate;



    private TestRestTemplate restTemplate = new TestRestTemplate();

    private HttpHeaders headers = new HttpHeaders();

    @Before
    public void setup(){
        mongoTemplate.dropCollection(CampsiteReservation.class);
    }



    @Test
    public void testRecoverAvailabilityForDefaultRange() {

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<CampsiteAvailability> response = restTemplate.exchange(
                createURLWithPort("/campsite/availability"),
                HttpMethod.GET, entity, CampsiteAvailability.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getSearchPeriodStart()).isEqualTo(LocalDate.now());
        assertThat(response.getBody().getSearchPeriodEnd()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(response.getBody().getAvailableDates()).isNotNull();
        assertThat(response.getBody().getAvailableDates()).isNotEmpty();
    }

    @Test
    public void testRecoverAvailabilityForASpecificRange() {

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/campsite/availability"))
                .queryParam("from", today)
                .queryParam("until", tomorrow);

        ResponseEntity<CampsiteAvailability> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET, entity, CampsiteAvailability.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAvailableDates()).isNotNull();
        assertThat(response.getBody().getAvailableDates()).contains(today);
    }

    @Test
    public void testValidReservationCreation() {
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(buildReservationRequest(LocalDate.now().plusDays(1), 2), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    public void testNothingIsAvailableWhenAllDatesAreReserved() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final LocalDate end = LocalDate.now().plusMonths(1);
        fillWithOneDayReservationBetween(start, end);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(createURLWithPort("/campsite/availability"))
                .queryParam("from", start)
                .queryParam("until", end);

        ResponseEntity<CampsiteAvailability> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET, entity, CampsiteAvailability.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAvailableDates()).isEmpty();
    }

    @Test
    public void testValidReservationUpdate() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final String existingReservationId = createReservationAndGetUniqueId(start, 2);

        final LocalDate newStart = start.plusDays(10);
        final LocalDate newEnd = newStart.plusDays(3);

        final User user = buildValidUser();
        final CampsiteReservation newReservation = CampsiteReservation.builder().user(user).startDate(newStart).endDate(newEnd).build();
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(newReservation, headers);
        ResponseEntity<CampsiteReservation> response = restTemplate.exchange(
                createURLWithPort(format("/campsite/reservation/%s", existingReservationId)),
                HttpMethod.POST, entity, CampsiteReservation.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(existingReservationId);
        assertThat(response.getBody().getUser()).isEqualTo(user);
        assertThat(response.getBody().getStartDate()).isEqualTo(newStart);
        assertThat(response.getBody().getEndDate()).isEqualTo(newEnd);
    }

    @Test
    public void testReservationCancel() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final String existingReservationId = createReservationAndGetUniqueId(start, 2);
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort(format("/campsite/reservation/%s", existingReservationId)),
                HttpMethod.DELETE, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testReservationCreationWithInvalidEmailReturnBadRequest() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final User userWithInvalidEmail = User.builder().email("invalidEmail").fullName("Test Person").build();
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(buildReservationRequest(start, 2, userWithInvalidEmail), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testReservationCreationMissingFullNameReturnBadRequest() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final User userWithNoName = User.builder().email("test@test.com").fullName("").build();
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(buildReservationRequest(start, 2, userWithNoName), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testReservationCreationWithMissingUserReturnBadRequest() {
        final LocalDate start = LocalDate.now().plusDays(1);
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(buildReservationRequest(start, 2, null), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testReservationCreationForMoreThan3DaysReturnBadRequest() {
        final LocalDate start = LocalDate.now().plusDays(1);
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(buildReservationRequest(start, MAX_RESERVATION_DURATION + 1), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testReservationUpdateForMoreThan3DaysReturnBadRequest() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final String existingReservationId = createReservationAndGetUniqueId(start, 2);

        final LocalDate newStart = start.plusDays(10);
        final LocalDate newEnd = newStart.plusDays(MAX_RESERVATION_DURATION + 1);

        final User user = buildValidUser();
        final CampsiteReservation newReservation = CampsiteReservation.builder().user(user).startDate(newStart).endDate(newEnd).build();
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(newReservation, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort(format("/campsite/reservation/%s", existingReservationId)),
                HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    public void testReservationCreationStartingTodayReturnBadRequest() {
        final LocalDate start = LocalDate.now();
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(buildReservationRequest(start, 1), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testReservationUpdateStartingTodayReturnBadRequest() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final String existingReservationId = createReservationAndGetUniqueId(start, 2);

        final LocalDate newStart = LocalDate.now();
        final LocalDate newEnd = newStart.plusDays(2);

        final User user = buildValidUser();
        final CampsiteReservation newReservation = CampsiteReservation.builder().user(user).startDate(newStart).endDate(newEnd).build();
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(newReservation, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort(format("/campsite/reservation/%s", existingReservationId)),
                HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testReservationCreationInMoreThanOneMonthReturnBadRequest() {
        final LocalDate start = LocalDate.now().plusMonths(1).plusDays(1);
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(buildReservationRequest(start, 1), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testReservationUpdateInMoreThanOneMonthReturnBadRequest() {
        final LocalDate start = LocalDate.now().plusDays(1);
        final String existingReservationId = createReservationAndGetUniqueId(start, 2);

        final LocalDate newStart = LocalDate.now().plusMonths(1).plusDays(1);
        final LocalDate newEnd = newStart.plusDays(2);

        final User user = buildValidUser();
        final CampsiteReservation newReservation = CampsiteReservation.builder().user(user).startDate(newStart).endDate(newEnd).build();
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(newReservation, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort(format("/campsite/reservation/%s", existingReservationId)),
                HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testCannotCreateReservationForUnavailableDateReturnConflict() {
        final LocalDate start = LocalDate.now().plusDays(2);
        createReservationAndGetUniqueId(start, 1);

        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(buildReservationRequest(start, 2, User.builder().email("anotherTest@test.com").fullName("Another Test Person").build()), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    public void testCannotUpdateReservationForUnavailableDate() {
        final LocalDate start = LocalDate.now().plusDays(2);
        createReservationAndGetUniqueId(start, 1);

        final LocalDate anotherStart = LocalDate.now().plusDays(1);
        final String existingReservationId = createReservationAndGetUniqueId(anotherStart, 1);

        final LocalDate newStart = LocalDate.now().plusDays(1);
        final LocalDate newEnd = newStart.plusDays(3);

        final User user = buildValidUser();
        final CampsiteReservation newReservation = CampsiteReservation.builder().user(user).startDate(newStart).endDate(newEnd).build();
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(newReservation, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                createURLWithPort(format("/campsite/reservation/%s", existingReservationId)),
                HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }


    @Test
    public void testReservationCreationConcurrent() throws ExecutionException, InterruptedException {
        final int nbOfRequest = 10000;
        final LocalDate start = LocalDate.now().plusDays(1);
        final int duration = 1;

        final List<CompletableFuture<ResponseEntity<String>>> requestsFutures = IntStream.range(0, nbOfRequest)
                .mapToObj(nb -> CompletableFuture.supplyAsync(() -> createReservation(start, duration)))
                .collect(toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(requestsFutures.toArray(new CompletableFuture[requestsFutures.size()]));

        // Make sure all the request are completed
        CompletableFuture<List<ResponseEntity<String>>> allRequestsFuture = allFutures.thenApply(f ->
                requestsFutures.stream()
                        .map(future -> future.join())
                        .collect(Collectors.toList())
        );

        List<ResponseEntity<String>> responses = allRequestsFuture.get();

        assertThat(responses).isNotEmpty();
        assertThat(responses.size()).isEqualTo(nbOfRequest);
        assertThat(responses.stream().map(ResponseEntity::getStatusCode)).containsOnlyOnce(HttpStatus.OK);
        assertThat(responses.stream().map(ResponseEntity::getStatusCode).filter(status -> !HttpStatus.OK.equals(status))).containsOnly(HttpStatus.CONFLICT);

        List<CampsiteReservation> reservations = getAllReservations();
        assertThat(reservations).isNotEmpty();
        assertThat(reservations.size()).isEqualTo(1);
        assertThat(reservations.get(0).getStartDate()).isEqualTo(start);
        assertThat(reservations.get(0).getEndDate()).isEqualTo(start.plusDays(duration));
    }


    private ResponseEntity<String> createReservation(LocalDate start, int duration) {
        HttpEntity<CampsiteReservation> entity = new HttpEntity<>(buildReservationRequest(start, duration), headers);

        return restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.PUT, entity, String.class);
    }


    private String createReservationAndGetUniqueId(LocalDate start, int duration) {
        ResponseEntity<String> response = createReservation(start, duration);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(format("Trying to create a %d days reservation starting on %s failed. Server response: %s", duration, start, response.toString()));
        }
        return response.getBody();
    }

    private void fillWithOneDayReservationBetween(LocalDate start, LocalDate end) {
        LongStream.range(0,DAYS.between(start, end)).forEach(offset -> createReservationAndGetUniqueId(start.plusDays(offset), 1));
    }

    private List<CampsiteReservation> getAllReservations() {
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<List<CampsiteReservation>> response = restTemplate.exchange(
                createURLWithPort("/campsite/reservation"),
                HttpMethod.GET, entity,  new ParameterizedTypeReference<List<CampsiteReservation>>(){});

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(format("Trying to retrieve all reservations failed. Server response: %s", response.toString()));
        }
        return response.getBody();
    }

    private String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }


}
