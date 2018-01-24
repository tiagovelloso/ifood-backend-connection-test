package br.com.ifood.chronos.monitor.actor;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Status.Failure;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import br.com.ifood.chronos.monitor.actor.RestaurantSupervisorActor.PostMessage;
import br.com.ifood.chronos.monitor.exception.RestaurantNotFoundException;
import scala.concurrent.duration.Duration;

/**
 * Actor that has the capacity to send a message to multiple restaurants, 
 * receive and forward the answers
 * 
 * @author Tiago Velloso <tiago.velloso@gmail.com>
 */
public class RestaurantBroadcastActor extends AbstractActor {
	
	private final List<Long> ids;
	
	private final ActorRef sender;
	
	private final List<Object> responses = new ArrayList<>();
	
	private int responseCount = 0;
	
	private final LoggingAdapter log = Logging.getLogger(context().system(), this);

	public RestaurantBroadcastActor(List<Long> ids, Object message, ActorRef sender) {
		this.ids = Collections.unmodifiableList(ids);
		this.sender = sender;
		
		ids.stream().map(id -> new PostMessage(id, message)).forEach(postMessage -> context().parent().tell(postMessage, self()));
		
		getContext().setReceiveTimeout(Duration.create(10, SECONDS));
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(Failure.class, this::treatFailure)
			.match(ReceiveTimeout.class, this::treatTimeout)
			.matchAny(this::treatMessage)
			.build();
	}
	
	private void treatMessage(Object response) {
		responses.add(response);
		treatMessage();
	}
	
	private void treatFailure(Failure f) {
		if (!(f.cause() instanceof RestaurantNotFoundException)) {
			log.error("RestaurantBroadcastActor error {}", f.cause().getClass());
			sender.tell(f, sender);
			commitSuicide();
		}
		treatMessage();
	}
	
	private void treatMessage() {
		responseCount++;
		if (responseCount == ids.size()) {
			sender.tell(new RestaurantBroadcastResponse(responses), context().parent());
			commitSuicide();
		}
	}
	
	private void treatTimeout(ReceiveTimeout timeout) {
		log.error("RestaurantBroadcastActor timeout!");
		commitSuicide();
	}
	
	private void commitSuicide() {
		context().stop(getSelf());
	}
	
	public static Props props(List<Long> ids, Object message, ActorRef sender) {
		return Props.create(RestaurantBroadcastActor.class, ids, message, sender);
	}
	
	
	public static final class RestaurantBroadcastResponse {
		
		private final List<Object> responses;

		public RestaurantBroadcastResponse(List<Object> responses) {
			this.responses = responses;
		}
		
		public List<Object> getResponses() {
			return responses;
		}
	}
}