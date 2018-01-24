package br.com.ifood.chronos.monitor.extension;

import java.time.LocalTime;

import com.typesafe.config.Config;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;

public class ApplicationSettings implements Extension {
	
	public final String NAME;
	
	public final int PORT;
	
	public final LocalTime RESTAURANT_OPEN;
	
	public final LocalTime RESTAURANT_CLOSE;
	
	public final long RESTAURANT_REPORT_INTERVAL;
	
	public final long RESTAURANT_HEART_BEAT_INTERVAL;
	
	public final String SCHEDULER_ADDRESS;
	
	public ApplicationSettings(Config config) throws Exception {
		
		NAME = config.getString("application.name");
		
		PORT = config.getInt("application.port");
		
		RESTAURANT_OPEN = LocalTime.parse(config.getString("application.restaurant.open"));
		
		RESTAURANT_CLOSE = LocalTime.parse(config.getString("application.restaurant.close"));
		
		RESTAURANT_REPORT_INTERVAL = config.getLong("application.restaurant.report-interval");
		
		RESTAURANT_HEART_BEAT_INTERVAL = config.getLong("application.restaurant.heart-beat-interval");
		
		SCHEDULER_ADDRESS = config.getString("application.scheduler.address");
	}
	
	public static class Extension extends AbstractExtensionId<ApplicationSettings> {
		
		public static final Extension PROVIDER = new Extension();
		
		private Extension() { }
		
		@Override
		public ApplicationSettings createExtension(ExtendedActorSystem system) {
			try {
				return new ApplicationSettings(system.settings().config());
			} catch (Exception cause) {
				throw new RuntimeException(cause);
			}
		}
	}
	
}
