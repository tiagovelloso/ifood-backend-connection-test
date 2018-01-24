package br.com.ifood.chronos.monitor;

import static akka.http.javadsl.model.ContentTypes.APPLICATION_JSON;
import static akka.http.javadsl.model.StatusCodes.ACCEPTED;
import static akka.http.javadsl.model.StatusCodes.INTERNAL_SERVER_ERROR;
import static akka.http.javadsl.model.StatusCodes.NOT_FOUND;
import static akka.http.javadsl.model.StatusCodes.OK;
import static akka.http.javadsl.server.PathMatchers.longSegment;
import static akka.pattern.Patterns.ask;
import static java.util.stream.Collectors.toList;
import static scala.compat.java8.FutureConverters.toJava;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import br.com.ifood.chronos.monitor.actor.RestaurantActor.CheckConnectionStatus;
import br.com.ifood.chronos.monitor.actor.RestaurantActor.HeartBeat;
import br.com.ifood.chronos.monitor.actor.RestaurantActor.UnavailabilityScheduleRequest;
import br.com.ifood.chronos.monitor.actor.RestaurantSupervisorActor;
import br.com.ifood.chronos.monitor.actor.RestaurantSupervisorActor.PostBroadcastMessage;
import br.com.ifood.chronos.monitor.actor.RestaurantSupervisorActor.PostMessage;
import br.com.ifood.chronos.monitor.actor.RestaurantSupervisorActor.UnavailabilitySchedulerChangeNotification;
import br.com.ifood.chronos.monitor.exception.RestaurantNotFoundException;
import br.com.ifood.chronos.monitor.extension.ApplicationSettings;
import br.com.ifood.chronos.monitor.util.JSON;

public class ChronosHttpServer extends AllDirectives {
	
	private static ServerBinding server;
	
	private ActorSelection restaurantSupervisor;
	
	private final LoggingAdapter log;
	
	public ChronosHttpServer(ActorSystem system) {
		restaurantSupervisor = system.actorSelection(RestaurantSupervisorActor.PATH);
		log = Logging.getLogger(system, this);
	}

	public static void startHttpServer(ActorSystem system, ApplicationSettings settings) {
		final Http http = Http.get(system);
		final ActorMaterializer materializer = ActorMaterializer.create(system);
		final ChronosHttpServer chronosHttp = new ChronosHttpServer(system);
		
		final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = chronosHttp.createRoute(system).flow(system, materializer);
		
		http.bindAndHandle(routeFlow, ConnectHttp.toHost("0.0.0.0", settings.PORT), materializer).thenAccept(binding -> {
			server = binding;
		});
	}
	
	public static void stopHttpServer() {
		server.unbind().thenAccept(x -> {
			System.out.println("Server shut down!");
		});
	}
	
	private Route createRoute(ActorSystem system) {
		return route(
			pathPrefix("restaurants", () -> route (
				pathPrefix(longSegment(), (Long id) -> route(
					pathEndOrSingleSlash(() -> route(
						get(() -> completeWithFuture(postMessageRestaurant(id, new CheckConnectionStatus()))),
						post(() -> {
							restaurantSupervisor.tell(new PostMessage(id, new HeartBeat()), ActorRef.noSender());
							return complete(HttpResponse.create().withStatus(ACCEPTED).addHeader(RawHeader.create("Access-Control-Allow-Origin", "*")));
						})
					)),
					pathPrefix("unavailabilities", () -> pathEndOrSingleSlash(() -> route(
						get(() -> completeWithFuture(postMessageRestaurant(id, new UnavailabilityScheduleRequest()))),
						put(() -> {
							restaurantSupervisor.tell(new UnavailabilitySchedulerChangeNotification(id), ActorRef.noSender());
							return complete(HttpResponse.create().withStatus(ACCEPTED));
						})
					)))
				)),
				pathEndOrSingleSlash(() -> route(
					get(() -> parameterList("id", ids -> completeWithFuture(postBroadcastMessageRestaurant(ids))))
				))
			))
		);
	}
	
	private CompletionStage<HttpResponse> postMessageRestaurant(Long id, Object message) {
		return toJava(ask(restaurantSupervisor, new PostMessage(id, message), new Timeout(2, TimeUnit.SECONDS))).handle((r,e) -> {
			if (e != null) {
				if (e.getCause() instanceof RestaurantNotFoundException) {
					log.warning("restaurant with id {} was not found", id);
					return HttpResponse.create().withStatus(NOT_FOUND);
				}
				log.error(e, "postMessageRestaurant");
				return HttpResponse.create().withStatus(INTERNAL_SERVER_ERROR);
			}
			return HttpResponse.create().withStatus(OK).withEntity(HttpEntities.create(APPLICATION_JSON, JSON.serialize(r)));
		});
	}
	
	private CompletionStage<HttpResponse> postBroadcastMessageRestaurant(List<String> ids) {
		return toJava(ask(restaurantSupervisor, new PostBroadcastMessage(ids.stream().map(Long::parseLong).collect(toList()),
			new CheckConnectionStatus()), new Timeout(2, TimeUnit.SECONDS))).handle((r, e) -> {
				if (e != null) {
					log.error(e, "postMessageRestaurant");
					return HttpResponse.create().withStatus(INTERNAL_SERVER_ERROR);
				}
				return HttpResponse.create().withStatus(OK).withEntity(HttpEntities.create(APPLICATION_JSON, JSON.serialize(r)));
			});
	}
}