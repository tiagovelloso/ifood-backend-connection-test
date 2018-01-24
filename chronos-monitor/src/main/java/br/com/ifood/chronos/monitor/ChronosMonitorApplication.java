package br.com.ifood.chronos.monitor;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import br.com.ifood.chronos.monitor.actor.ChronosSchedulerActor;
import br.com.ifood.chronos.monitor.actor.RestaurantSupervisorActor;
import br.com.ifood.chronos.monitor.extension.ApplicationSettings;
import scala.concurrent.Future;

public class ChronosMonitorApplication {
	
	protected static ActorSystem system;
	
	protected static ApplicationSettings settings;
	
	public static void main(String ... args) {
		Config config = ConfigFactory.load();
		
		system = ActorSystem.create(config.getString("application.name"), config);
		
		settings = ApplicationSettings.Extension.PROVIDER.get(system);
		
		system.actorOf(Props.create(ChronosSchedulerActor.class, settings), ChronosSchedulerActor.ID);
		
		system.actorOf(Props.create(RestaurantSupervisorActor.class, settings), RestaurantSupervisorActor.ID);
		
		ChronosHttpServer.startHttpServer(system, settings);
	}
	
	public static Future<Terminated> shutdown() {
		return system.terminate();
	}
}