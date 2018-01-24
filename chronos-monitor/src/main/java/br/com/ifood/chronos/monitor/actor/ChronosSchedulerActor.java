package br.com.ifood.chronos.monitor.actor;

import static akka.http.javadsl.model.ContentTypes.APPLICATION_JSON;
import static akka.http.javadsl.model.HttpMethods.POST;
import static akka.http.javadsl.model.HttpRequest.create;
import static akka.pattern.Patterns.ask;
import static java.lang.String.format;
import static scala.compat.java8.FutureConverters.toJava;

import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Status.Failure;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.model.HttpRequest;
import akka.util.Timeout;
import br.com.ifood.chronos.monitor.actor.HttpClientStreamActor.Request;
import br.com.ifood.chronos.monitor.actor.HttpClientStreamActor.Response;
import br.com.ifood.chronos.monitor.exception.HttpClientErrorException;
import br.com.ifood.chronos.monitor.extension.ApplicationSettings;
import br.com.ifood.chronos.monitor.model.OfflineLog;
import br.com.ifood.chronos.monitor.model.Restaurant;
import br.com.ifood.chronos.monitor.model.Unavailability;
import br.com.ifood.chronos.monitor.model.UnavailabilitySchedule;
import br.com.ifood.chronos.monitor.util.JSON;

/**
 * Actor responsible for communication with Chronos Scheduler module through
 * HTTP calls
 * 
 * @author Tiago Velloso <tiago.velloso@gmail.com>
 *
 */
public class ChronosSchedulerActor extends AbstractActor {
	
	public static final String ID = "chronosScheduler";

	public static final String PATH = "/user/" + ID;
	
	private final ApplicationSettings settings;
	
	private ActorRef httpClient;
	
	private final LoggingAdapter log = Logging.getLogger(context().system(), this);
	
	public ChronosSchedulerActor(ApplicationSettings settings) {
		this.settings = settings;
		httpClient = context().actorOf(HttpClientStreamActor.props(settings.SCHEDULER_ADDRESS));
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(UnavailabilityRequest.class, this::requestUnavailabilities)
				.match(OfflineLog.class, this::sendOfflineLog)
				.build();
	}
	
	private void requestUnavailabilities(UnavailabilityRequest req) {
		
		final ActorRef sender = sender();
		
		HttpRequest httpRequest = create(format(settings.SCHEDULER_ADDRESS + "/restaurants/%d/unavailabilities", req.getRestaurantId()));
		
		toJava(ask(httpClient, new Request(httpRequest, Unavailability[].class), new Timeout(10, TimeUnit.SECONDS)))
		.thenAccept(r -> {
			sender.tell(new UnavailabilitySchedule(((Response) r).getBody(Unavailability[].class)), self());
		})
		.handle((r,e) -> {
			if (e != null) {
				if (!HttpClientErrorException.is4xx(e.getCause())) {
					log.error(e, "");
				}
				sender.tell(new Failure(e), self());
			}
			return r;
		});
	}
	
	private void sendOfflineLog(OfflineLog offlineLog) {
		log.info("Sending offline report for restaurant {}", offlineLog.getRestaurant().getId());
		
		String uri = buildOfflineReportUrl(offlineLog.getRestaurant());
		String body = JSON.serialize(offlineLog);
		
		HttpRequest httpRequest = HttpRequest.create(uri).withMethod(POST).withEntity(APPLICATION_JSON, body);
		
		toJava(ask(httpClient, new Request(httpRequest), new Timeout(10, TimeUnit.SECONDS)))
		.thenAccept(r -> {
			log.info("Offline report for restaurant {} sent successfully", offlineLog.getRestaurant().getId());
		})
		.handle((r,e) -> {
			if (e != null) {
				log.error(e, "error during sending the offline report");
			}
			return r;
		});
	}
	
	private String buildOfflineReportUrl(final Restaurant restaurant) {
		return String.format(settings.SCHEDULER_ADDRESS + "/restaurants/%d/offlinelogs", restaurant.getId());
	}
	
	public static final class UnavailabilityRequest {
		
		private final Long restaurantId;

		public UnavailabilityRequest(Long restaurantId) {
			this.restaurantId = restaurantId;
		}
		
		public Long getRestaurantId() {
			return restaurantId;
		}
	}
}