package br.com.ifood.chronos.monitor.actor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status.Failure;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.HostConnectionPool;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCode;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueue;
import akka.util.ByteString;
import br.com.ifood.chronos.monitor.exception.HttpClientErrorException;
import br.com.ifood.chronos.monitor.util.JSON;
import scala.util.Try;

/**
 * Actor responsible to send HTTP requests using a stream
 * 
 * @author Tiago Velloso <tiago.velloso@gmail.com>
 * 
 */
public class HttpClientStreamActor extends AbstractActor {
	
	private final ActorMaterializer materializer;
	
	private final Integer bufferSize;
	
	private final OverflowStrategy overflowStrategy;
	
	private SourceQueue<Pair<HttpRequest, Request>> backpressuredQueue;

	private Flow<Pair<HttpRequest, Request>, Pair<Try<HttpResponse>, Request>, HostConnectionPool> connectionPool;
	
	private final LoggingAdapter log = Logging.getLogger(context().system(), this);
	
	private static final String START_QUEUE_SIGNAL = "START_QUEUE_SIGNAL";
	
	public HttpClientStreamActor(String host, Integer bufferSize, OverflowStrategy overflowStrategy) {
		materializer = ActorMaterializer.create(context());
		connectionPool = Http.get(context().system()).<Request>cachedHostConnectionPool(host, materializer);
		this.bufferSize = bufferSize;
		this.overflowStrategy = overflowStrategy;
		
		buildBackpressuredQueueStream();
	}
	
	public HttpClientStreamActor(String host) {
		this(host, 0, OverflowStrategy.dropBuffer());
	}
	
	private void buildBackpressuredQueueStream() {
		if (this.backpressuredQueue != null) {
			return;
		}
		
		this.backpressuredQueue = Source.<Pair<HttpRequest, Request>>queue(bufferSize, overflowStrategy)
		.via(connectionPool)
		.toMat(Sink.foreach(pairReqRes -> {
			try {
				final Request request = pairReqRes.second();
				final HttpResponse response = pairReqRes.first().get();
				
				log.info("HTTP {} {} sent by stream", request.getHttpRequest().method().name(), request.getHttpRequest().getUri());
				
				response.entity().getDataBytes().runFold(ByteString.empty(), ByteString::concat, materializer)
				.thenCompose(r -> {
					CompletionStage<?> bodyUnmarshall = null;
					if (!response.status().isSuccess() || String.class.equals(request.getReturnType())) {
						bodyUnmarshall = CompletableFuture.completedFuture(r.utf8String());
					} else {
						bodyUnmarshall = JSON.deserializeAsync(r.utf8String(), request.getReturnType());
					}
					return bodyUnmarshall;
				})
				.thenAccept(body -> {
					if (!response.status().isSuccess()) {
						request.getSender().tell(new Failure(new HttpClientErrorException(response.status(), body.toString())), self());
					} else {
						request.getSender().tell(new Response(request, response.status(), body),self());
					}
				})
				.handle((r,e) -> {
					if (e != null) {
						request.getSender().tell(new Failure(e), self());
					}
					return r;
				});
			} catch (Exception e) {
				pairReqRes.second().getSender().tell(new Failure(e), self());
			}
		}),Keep.left()).run(materializer);
		
		backpressuredQueue.watchCompletion().handle((r,e) -> {
			backpressuredQueue = null;
			
			if (e != null) {
				log.error(e, "The stream was closed with error");
			}
			
			self().tell(START_QUEUE_SIGNAL, ActorRef.noSender());
			return r;
		});
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(Request.class, request -> {
				final ActorRef sender = sender();
				backpressuredQueue.offer(Pair.create(request.getHttpRequest(), new Request(request,sender))).handle((r,e) -> {
					if (e != null) {
						sender.tell(new Failure(e), self());
					}
					return r;
				});
			})
			.match(String.class, s -> s.equals(START_QUEUE_SIGNAL),  s -> buildBackpressuredQueueStream())
		.build();
	}
	
	public static Props props(String host) {
		return Props.create(HttpClientStreamActor.class, host);
	}
	
	public static Props props(String host, Integer bufferSize, OverflowStrategy strategy) {
		return Props.create(HttpClientStreamActor.class, host, bufferSize, strategy);
	}
	
	
	public static final class Request {

		private final HttpRequest httpRequest;
		
		private final Class<?> returnType;
		
		private final ActorRef sender;

		private Request(HttpRequest httpRequest, Class<?> returnType, ActorRef sender) {
			this.httpRequest = httpRequest;
			this.returnType = returnType == null ? String.class : returnType;
			this.sender = sender;
		}
		
		private Request(Request request, ActorRef sender) {
			this(request.httpRequest, request.returnType, sender);
		}
		
		public Request(HttpRequest httpRequest, Class<?> returnType) {
			this(httpRequest, returnType, null);
		}
		
		public Request(HttpRequest httpRequest) {
			this(httpRequest, null, null);
		}
		
		public HttpRequest getHttpRequest() {
			return httpRequest;
		}
		
		public Class<?> getReturnType() {
			return returnType;
		}
		
		public ActorRef getSender() {
			return sender;
		}
	}
	
	public static final class Response {
		
		private final Request request;
		
		private final StatusCode statusCode;
		
		private final Object body;

		public Response(Request request, StatusCode statusCode, Object body) {
			this.request = request;
			this.statusCode = statusCode;
			this.body = body;
		}
		
		public Request getRequest() {
			return request;
		}
		
		public StatusCode getStatusCode() {
			return statusCode;
		}
		
		public Object getBody() {
			return body;
		}
		
		public <T> T getBody(Class<T> clazz) {
			return clazz.cast(body);
		}
	}
}