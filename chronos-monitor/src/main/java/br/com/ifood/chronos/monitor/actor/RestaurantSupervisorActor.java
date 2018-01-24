package br.com.ifood.chronos.monitor.actor;

import static akka.pattern.Patterns.ask;
import static scala.compat.java8.FutureConverters.toJava;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Identify;
import akka.actor.Status.Failure;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;
import br.com.ifood.chronos.monitor.actor.ChronosSchedulerActor.UnavailabilityRequest;
import br.com.ifood.chronos.monitor.exception.HttpClientErrorException;
import br.com.ifood.chronos.monitor.exception.RestaurantNotFoundException;
import br.com.ifood.chronos.monitor.extension.ApplicationSettings;
import br.com.ifood.chronos.monitor.model.Restaurant;
import br.com.ifood.chronos.monitor.model.Unavailability;
import br.com.ifood.chronos.monitor.model.UnavailabilitySchedule;

/**
 * Actor responsible for create and supervises the restaurants
 * 
 * @author Tiago Velloso <tiago.velloso@gmail.com>
 */
public class RestaurantSupervisorActor extends AbstractActor {
	
	public static final String ID = "restaurantSupervisor";

	public static final String PATH = "/user/" + ID;
	
	private final ApplicationSettings settings;
	
	private static ActorSelection scheduler;
	
	private final LoggingAdapter log = Logging.getLogger(context().system(), this);

	public RestaurantSupervisorActor(ApplicationSettings settings) {
		this.settings = settings;
		scheduler = context().system().actorSelection(ChronosSchedulerActor.PATH);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(PostMessage.class, message -> {
					final ActorRef sender = sender();
					findRestaurant(message.restaurantId, true)
					.thenAccept(restaurant -> restaurant.get().tell(message.message, sender))
					.handle((r,e) -> {
						if (e != null) {
							sender.tell(new Failure(e.getCause()), self());
						}
						return r;
					});
				})
				
				.match(PostBroadcastMessage.class, this::postBroadcastMessage)
		
				.match(UnavailabilitySchedulerChangeNotification.class, message -> {
					findRestaurant(message.restaurantId, true).thenAccept(restaurant -> {
						if (restaurant.isPresent()) {
							getUnavailabilities(message.getRestaurantId()).thenAccept(res -> restaurant.get().tell(new UnavailabilitySchedule(res.getUnavailabilities()), self()));
							restaurant.get().tell("", self());
						}
					});
				})
				
		.build();
	}
	
	private void postBroadcastMessage(PostBroadcastMessage message) {
		context().actorOf(RestaurantBroadcastActor.props(message.restaurantIds, message.message, sender()));
	}
	
	private CompletionStage<Optional<ActorRef>> findRestaurant(Long id, boolean create) {
		final String actorName = "restaurant" + id;
		final ActorSelection path = context().actorSelection(actorName);
		
		return toJava(ask(path, new Identify(id), new Timeout(500, TimeUnit.MILLISECONDS)))
		.thenApply(ActorIdentity.class::cast)
		.thenCompose(identity -> {
			if (identity.getActorRef().isPresent()) {
				return CompletableFuture.completedFuture(identity.getActorRef().get());
			}
			
			if (!create) {
				return null;
			}
			
			log.info("Restaurant with id {} was NOT found and will be resolved", id);
			
			return getUnavailabilities(id).thenApply(uRes -> {
				List<Unavailability> unavailabilities = uRes.getUnavailabilities();
				ActorRef actor = context().actorOf(RestaurantActor.props(new Restaurant(id), unavailabilities, settings), actorName);
				return actor;
			}).handle((r,e) -> {
				if (e != null) {
					if (HttpClientErrorException.is4xx(e.getCause())) {
						throw new RestaurantNotFoundException(id);
					} else {
						log.error(e, "Error during getting the restaurant unavailabilities");
					}
				}
				return r;
			});
		})
		.thenApply(ref -> ref == null ? Optional.<ActorRef>empty() : Optional.of((ActorRef) ref))
		.handle((r,e) -> {
			if (e != null) {
				if (e.getCause() instanceof RuntimeException) {
					throw (RuntimeException) e.getCause();
				}
				log.error(e, "findRestaurant");
			}
			return r;
		});
	}
	
	private CompletionStage<UnavailabilitySchedule> getUnavailabilities(Long id) {
		return toJava(ask(scheduler, new UnavailabilityRequest(id), new Timeout(2000, TimeUnit.MILLISECONDS))).thenApply(UnavailabilitySchedule.class::cast);
	}
	
	public static final class PostMessage {
		
		private final Long restaurantId;
		
		private final Object message;

		public PostMessage(Long restaurantId, Object message) {
			this.restaurantId = restaurantId;
			this.message = message;
		}
	}
	
	public static final class PostBroadcastMessage {
		
		private final List<Long> restaurantIds;
		
		private final Object message;

		public PostBroadcastMessage(List<Long> restaurantIds, Object message) {
			this.restaurantIds = restaurantIds;
			this.message = message;
		}
	}
	
	public static final class UnavailabilitySchedulerChangeNotification {
		
		private final Long restaurantId;

		public UnavailabilitySchedulerChangeNotification(Long restaurantId) {
			this.restaurantId = restaurantId;
		}
		
		public Long getRestaurantId() {
			return restaurantId;
		}
	}
}