# Chronos
This project is a PoC (Proof of Concept) that intends to solve the ifood backend test connection based in java.

The solution is divided in three modules.

##### Chronos Monitor
Reponsible to monitoring the online restaurants with following functionalities:
- Receive the alive signals from restaurants;
- Responde which is online and which is not;
- Consolidate the offline time of restaurants, considering it's alive signal,    schedules of unavailability and the ifood opening hour platform and send it to the Chronos Scheduler module.

Monitor is based in [akka](http://akka.io), which is a set of open-source libraries that embrace the reactive programming and allows designing highly concurrent, distributed, scalable, resilient and fault tolerant applications.

With the CQRS pattern in mind, akka was chosen as the event source once it provides a high throughput event processing using the http and streams libraries, consolidate the events asynchronously, and send it to scheduler module through http using the streams automatic backpressure (respecting the destination throughput).

##### Chronos Scheduler
The core module of the solution, stores the restaurant's consolidate offline time, unavailability schedules and provides the restaurant ranking. Scheduler is based in spring boot using a relational database.

##### Chronos Restaurant
Is the client module, which allows restaurants manage their unavailability schedule and also maintain the alive signal. This module is based in spring boot.

## Considerations
Once this solution is a PoC, the following points was not considered in the implementation:
- Timezone and internationalization;
- Security;

## Next Steps
To evolve the solution and deploy it, the following points should be considered:
- Use of the akka cluster with shard to clusterize the Restaurant Actors. Once akka offers location transparency, it can be done with few lines of code.
- Use of akka persistence to guarantee that no events will be lost
- Evaluate integrations besides the http
- Make performance tests using for example Jmeter, and make the necessaries adjustments
- Paginate the ranking results

## Requirements
- Java 8
- Maven

## Basic Example

Running the three modules, create the restaurant in scheduler module:

`POST http://localhost:8282/restaurants`
```sh
{ "login": "america" }
```

Open the restaurant module in browser at `localhost:8383`, using the created restaurant login as username and left the password empty.

From this moment, the client will send the alive signal every minute to the monitor and will be considered online.

The monitor is configured to consolidate the offline time of restaurants every 5 minutes and send it to the scheduler module.

##### Query the online restaurants in chronos monitor:
`GET http://localhost:8080/restaurants?id=1&id=2`

##### Query the restaurants unavailability in chronos scheduler:
`GET http://localhost:8282/restaurants/1/unavailabilities`

##### Query the restaurants consolidated offline time in chronos scheduler:
`GET http://localhost:8282/restaurants/1/offlinelogs`

##### View the restaurants ranking in chronos scheduler:
`GET http://localhost:8282/restaurants/ranking?start=2018-01-22&end=2018-01-24`