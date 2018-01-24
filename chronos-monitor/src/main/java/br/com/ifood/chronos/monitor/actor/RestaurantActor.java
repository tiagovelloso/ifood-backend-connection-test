package br.com.ifood.chronos.monitor.actor;

import static java.time.Duration.between;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import br.com.ifood.chronos.monitor.extension.ApplicationSettings;
import br.com.ifood.chronos.monitor.model.OfflineLog;
import br.com.ifood.chronos.monitor.model.Restaurant;
import br.com.ifood.chronos.monitor.model.Unavailability;
import br.com.ifood.chronos.monitor.model.UnavailabilitySchedule;
import scala.concurrent.duration.Duration;

/**
 * Actor that represents a restaurant
 * 
 * @author Tiago Velloso <tiago.velloso@gmail.com>
 */
public class RestaurantActor extends AbstractActorWithTimers {
	
	private final Restaurant restaurant;
	
	private final long reportInterval;
	
	private final long heartBeatInterval;
	
	private final LocalTime open;
	
	private final LocalTime close;
	
	private final List<LocalDateTime> heartBeats = new ArrayList<>();
	
	private LocalDateTime lastHeartBeat;
	
	private LocalDateTime lastHeartBeatReported;

	private List<Unavailability> unavailabilities = new ArrayList<>();
	
	private static ActorSelection chronosScheduler;
	
	private final LoggingAdapter log = Logging.getLogger(context().system(), this);

	public RestaurantActor(Restaurant restaurant, ApplicationSettings settings) {
		this.restaurant = restaurant;
		
		reportInterval = settings.RESTAURANT_REPORT_INTERVAL;
		heartBeatInterval = settings.RESTAURANT_HEART_BEAT_INTERVAL;
		open = settings.RESTAURANT_OPEN;
		close = settings.RESTAURANT_CLOSE;
		
		chronosScheduler = context().system().actorSelection(ChronosSchedulerActor.PATH);
		
		getTimers().startPeriodicTimer(1, new ReportHeartBeat(), Duration.create(reportInterval, MILLISECONDS));
	}
	
	public RestaurantActor(Restaurant restaurant, List<Unavailability> unavailabilities, ApplicationSettings settings) {
		this(restaurant, settings);
		setUnavailabilitySchedule(new UnavailabilitySchedule(unavailabilities));
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(HeartBeat.class, this::registerHeartBeat)
				.match(CheckConnectionStatus.class, this::checkConnectionStatus)
				.match(ReportHeartBeat.class, this::reportHeartBeat)
				.match(UnavailabilitySchedule.class, this::setUnavailabilitySchedule)
				.match(UnavailabilityScheduleRequest.class, this::getUnavailabilitySchedule)
				.build();
	}
	
	private void registerHeartBeat(HeartBeat heartBeat) {
		lastHeartBeat = LocalDateTime.now();
		heartBeats.add(lastHeartBeat);
	}
	
	private void checkConnectionStatus(CheckConnectionStatus checkStatus) {
		log.info("checking connection status for restaurant {}", restaurant.getId());
		boolean status = isOpen() && isAvailable() && isAlive();
		sender().tell(new ConnectionStatus(restaurant, status), self());
	}
	
	private void reportHeartBeat(ReportHeartBeat reportHeartBeat) {
		
		long offline = 0;
		int heartBeatsSize = heartBeats.size();
		
		LocalDateTime endInterval = LocalDateTime.now().withNano(0);
		LocalDateTime startInterval = lastHeartBeatReported == null ? endInterval.minus(reportInterval, MILLIS) : lastHeartBeatReported;
		
		if (!isBetween(startInterval.toLocalTime(), open, close)) {
			if (!isBetween(endInterval.toLocalTime(), open, close)) {
				log.info("{} and {} is out of working hours range which is between {} and {}", startInterval, endInterval, open, close);
				heartBeats.clear();
				lastHeartBeatReported = endInterval;
				return;
			}
			startInterval = LocalDateTime.of(startInterval.toLocalDate(), open);
			log.info("adjust report start interval to {}", startInterval);
		} else if (!isBetween(endInterval.toLocalTime(), open, close)) {
			endInterval = LocalDateTime.of(endInterval.toLocalDate(), close);
			log.info("adjust report end interval to {}", endInterval);
		}
		
		if (heartBeats.isEmpty()) {
			offline = calculateOfflineTime(startInterval, endInterval);
		} else {
			LocalDateTime lastBeat = startInterval;
			
			for (int i = 0; i < heartBeats.size(); i++) {
				LocalDateTime beat = heartBeats.get(i);
				offline += calculateOfflineTime(lastBeat, beat);
				lastBeat = beat;
			}
			
			offline += calculateOfflineTime(lastBeat, endInterval);
			
			heartBeats.clear();
		}
		
		lastHeartBeatReported = endInterval;
		
		if (offline > 0) {
			log.info("Reporting {} heart beats between {} and {} with {}ms offline", heartBeatsSize, startInterval, endInterval, offline);
			chronosScheduler.tell(new OfflineLog(restaurant, startInterval, endInterval, offline), self());
		} else {
			log.info("No report will be sent. Registered {} heart beats between {} and {}", heartBeatsSize, startInterval, endInterval);
		}
	}
	
	private long calculateOfflineTime(LocalDateTime start, LocalDateTime end) {
		long duration = between(start, end).toMillis();
		if (duration > heartBeatInterval) {
			long unavailable = calculateUnavailability(start, end);
			return duration - unavailable;
		}
		return 0;
	}
	
	private long calculateUnavailability(LocalDateTime start, LocalDateTime end) {
		
		long unavailable = 0;
		
		for (Unavailability u : unavailabilities) {
			if (u.getStart().isAfter(end)) {
				break;
			}
			
			if (u.getEnd().isBefore(start)) {
				continue;
			}
			
			LocalDateTime uStart = (isBetween(u.getStart(), start, end)) ? u.getStart() : start;
			LocalDateTime uEnd = (isBetween(u.getEnd(), start, end)) ? u.getEnd() : end;
			
			unavailable += between(uStart, uEnd).toMillis();
		}
		
		return unavailable;
	}
	
	private void setUnavailabilitySchedule(UnavailabilitySchedule message) {
		unavailabilities = message.getUnavailabilities();
	}
	
	private void getUnavailabilitySchedule(UnavailabilityScheduleRequest req) {
		sender().tell(new UnavailabilitySchedule(unavailabilities), self());
	}
	
	private boolean isAlive() {
		return lastHeartBeat != null && lastHeartBeat.plus(heartBeatInterval, MILLIS).isAfter(LocalDateTime.now());
	}
	
	private boolean isOpen() {
		LocalTime now = LocalTime.now();
		return now.isAfter(open) && now.isBefore(close);
	}
	
	private boolean isAvailable() {
		int i = 0;
		boolean available = true;
		LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
		
		System.out.println(now);
		
		for (; i < unavailabilities.size(); i++) {
			Unavailability unavailability = unavailabilities.get(i);
			
			System.out.println(unavailability);
			
			if (now.isAfter(unavailability.getEnd())) {
				continue;
			} else if (now.isAfter(unavailability.getStart())) {
				available = false;
			}
			break;
		}
		
		if (i > 0) {
			unavailabilities = unavailabilities.subList(i, unavailabilities.size());
		}
		
		return available;
	}
	
	private boolean isBetween(LocalDateTime compare, LocalDateTime min, LocalDateTime max) {
		return (compare.equals(min) || compare.isAfter(min)) && compare.isBefore(max);
	}
	
	private boolean isBetween(LocalTime compare, LocalTime min, LocalTime max) {
		return (compare.equals(min) || compare.isAfter(min)) && compare.isBefore(max);
	}
	
	public static Props props(final Restaurant restaurant, final List<Unavailability> unavailabilities, ApplicationSettings settings) {
		return Props.create(RestaurantActor.class, restaurant, unavailabilities, settings);
	}
	
	
	/** Message used to send a new heart beat */
	public static final class HeartBeat { }
	
	/** Message used to request the connection status based in heart beat */
	public static final class CheckConnectionStatus { }
	
	/** Message used to respond the connection status */
	public static final class ConnectionStatus {
		
		private final Restaurant restaurant;
		
		private final Boolean online;

		public ConnectionStatus(Restaurant restaurant, Boolean online) {
			this.restaurant = restaurant;
			this.online = online;
		}
		
		public Restaurant getRestaurant() {
			return restaurant;
		}
		
		public boolean isOnline() {
			return online;
		}

		@Override
		public String toString() {
			return "ConnectionStatus [restaurant=" + restaurant + ", online=" + online + "]";
		}
	}
	
	public static final class UnavailabilityScheduleRequest { }
	
	private static final class ReportHeartBeat { }
}