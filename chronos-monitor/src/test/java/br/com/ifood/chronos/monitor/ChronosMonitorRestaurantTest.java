package br.com.ifood.chronos.monitor;

import static br.com.ifood.chronos.monitor.ChronosMonitorApplication.settings;
import static br.com.ifood.chronos.monitor.ChronosMonitorApplication.system;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import br.com.ifood.chronos.monitor.actor.RestaurantActor.CheckConnectionStatus;
import br.com.ifood.chronos.monitor.actor.RestaurantActor.ConnectionStatus;
import br.com.ifood.chronos.monitor.actor.RestaurantActor.HeartBeat;
import br.com.ifood.chronos.monitor.actor.RestaurantSupervisorActor;
import br.com.ifood.chronos.monitor.actor.RestaurantSupervisorActor.PostMessage;
import br.com.ifood.chronos.monitor.model.OfflineLog;
import br.com.ifood.chronos.monitor.model.Restaurant;
import br.com.ifood.chronos.monitor.model.Unavailability;
import br.com.ifood.chronos.monitor.util.JSON;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public class ChronosMonitorRestaurantTest {

	private ActorSelection supervisor;
	
	private ClientAndServer mockedChronosSchedulerService;

	@BeforeClass
	public static void setup() {
		System.setProperty("config.resource", "application.test.conf");
		ChronosMonitorApplication.main();
	}

	@AfterClass
	public static void teardown() {
		try {
			FutureConverters.toJava(ChronosMonitorApplication.shutdown()).toCompletableFuture().get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Before
	public void before() {
		mockedChronosSchedulerService = ClientAndServer.startClientAndServer(1080);
		this.supervisor = system.actorSelection(RestaurantSupervisorActor.PATH);
	}
	
	@After
	public void shutdown() {
		mockedChronosSchedulerService.stop();
	}
	
	@Test
	public void offlineWithoutHeartBeatsTest() throws Exception {
		
		Restaurant restaurant = new Restaurant(1L);
		
		// mocking scheduler service
		mockGetRequest(restaurant);
		mockAnyPostRequest();
		
		// sending some message to actor to force his birth 
		supervisor.tell(new PostMessage(1L, ""), ActorRef.noSender());
		
		// waiting the report interval without send any other heart beats
		Thread.sleep(settings.RESTAURANT_REPORT_INTERVAL + 1000);
		
		// The restaurant should be offline
		assertThat(getConnectionStatus(restaurant).isOnline()).isFalse();
		
		// That should be sent an report with offline time
		
		HttpRequest[] requests = getPostsRecorded();
		
		assertThat(requests.length).isEqualTo(1);
		
		OfflineLog log = JSON.deserialize(requests[0].getBody().getRawBytes(), OfflineLog.class);
		assertThat(log.getRestaurant().getId()).isEqualTo(restaurant.getId());
		assertThat(log.getOfflineTime()).isEqualTo(settings.RESTAURANT_REPORT_INTERVAL);
	}
	
	@Test
	public void offlineWithHeartBeatsTest() throws Exception {
		
		Restaurant restaurant = new Restaurant(2L);
		
		// mocking scheduler service
		mockGetRequest(restaurant);
		mockAnyPostRequest();
		
		// sending the heart beat interval by the minimum time to generate a report
		for(int i = 0; i < (settings.RESTAURANT_HEART_BEAT_INTERVAL / settings.RESTAURANT_REPORT_INTERVAL)+1  ;i++) {
			sendHeartBeat(restaurant);
			Thread.sleep(settings.RESTAURANT_HEART_BEAT_INTERVAL/2);
		}
		
		// The restaurant should be online
		assertThat(getConnectionStatus(restaurant).isOnline()).isTrue();
		
		// No reports should be sent to scheduler
		assertThat(getPostsRecorded().length).isZero();
	}
	
	@Test
	public void offlineWithoutHeartBeatsWithUnavailabilityTest() throws Exception {
		
		Restaurant restaurant = new Restaurant(3L);
		
		// mocking scheduler service with unavailability
		mockGetRequest(restaurant, new Unavailability(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusMinutes(30)));
		mockAnyPostRequest();
		
		// sending a heart beat to actor
		sendHeartBeat(restaurant);
		
		// waiting the report interval without send any other heart beats
		Thread.sleep(settings.RESTAURANT_REPORT_INTERVAL + 1000);
		
		// The restaurant should be offline
		assertThat(getConnectionStatus(restaurant).isOnline()).isFalse();
		
		// No reports should be sent to scheduler
		assertThat(getPostsRecorded().length).isZero();
	}
	
	private void sendHeartBeat(Restaurant restaurant) {
		supervisor.tell(new PostMessage(restaurant.getId(), new HeartBeat()), ActorRef.noSender());
	}

	private ConnectionStatus getConnectionStatus(Restaurant restaurant) throws Exception {
		ConnectionStatus status = (ConnectionStatus) Await.result(Patterns.ask(supervisor,
				new PostMessage(restaurant.getId(), new CheckConnectionStatus()), new Timeout(1, SECONDS)),
				Duration.create(2, SECONDS));
		return status;
	}
	
	private void mockGetRequest(Restaurant restaurant, Unavailability ... unavailabilities) {
		mockedChronosSchedulerService.when(
			HttpRequest.request().withMethod("GET").withPath(String.format("/restaurants/%d/unavailabilities", restaurant.getId()))).respond(
				HttpResponse.response().withStatusCode(200).withBody(JSON.serialize(unavailabilities)));
	}
	
	private void mockAnyPostRequest() {
		mockedChronosSchedulerService.when(HttpRequest.request().withMethod("POST")).respond(HttpResponse.response().withStatusCode(202));
	}
	
	private HttpRequest[] getPostsRecorded() {
		return mockedChronosSchedulerService.retrieveRecordedRequests(HttpRequest.request().withMethod("POST"));
	}
}