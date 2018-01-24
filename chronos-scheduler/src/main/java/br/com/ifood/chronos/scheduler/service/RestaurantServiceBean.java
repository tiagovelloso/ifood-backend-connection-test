package br.com.ifood.chronos.scheduler.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import br.com.ifood.chronos.scheduler.domain.OfflineLog;
import br.com.ifood.chronos.scheduler.domain.Restaurant;
import br.com.ifood.chronos.scheduler.domain.unavailability.Unavailability;
import br.com.ifood.chronos.scheduler.repository.OfflineLogRepository;
import br.com.ifood.chronos.scheduler.repository.RestaurantRepository;
import br.com.ifood.chronos.scheduler.repository.UnavailabilityRepository;
import br.com.ifood.chronos.scheduler.service.exception.business.RestaurantLoginConflictException;
import br.com.ifood.chronos.scheduler.service.exception.business.ScheduleConflictException;
import br.com.ifood.chronos.scheduler.service.exception.business.ScheduleInvalidPeriodException;
import br.com.ifood.chronos.scheduler.service.exception.business.ScheduleRemovePastException;
import br.com.ifood.chronos.scheduler.service.exception.business.ScheduleTimePastException;
import br.com.ifood.chronos.scheduler.service.exception.validation.EntityNotFoundException;

@Service
public class RestaurantServiceBean implements RestaurantService {
	
	private final RestaurantRepository repository;
	
	private final UnavailabilityRepository unavailabilityRepository;
	
	private final OfflineLogRepository offlineLogRepository; 
	
	private final ApplicationEventPublisher eventPublisher;
	
	public RestaurantServiceBean(RestaurantRepository repository, UnavailabilityRepository unavailabilityRepository,
			OfflineLogRepository offlineLogRepository, ApplicationEventPublisher eventPublisher) {
		this.repository = repository;
		this.unavailabilityRepository = unavailabilityRepository;
		this.offlineLogRepository = offlineLogRepository;
		this.eventPublisher = eventPublisher;
	}

	@Override
	@Transactional
	public Restaurant createRestaurant(Restaurant restaurant) {
		
		repository.findByLogin(restaurant.getLogin()).ifPresent(r -> {
			throw new RestaurantLoginConflictException();
		});
		
		Restaurant createdRestaurant = repository.save(new Restaurant(restaurant));
		
		eventPublisher.publishEvent(new RestaurantChangeEvent(createdRestaurant));
		
		return createdRestaurant;
	}
	
	@Override
	public Optional<Restaurant> getRestaurant(Long id) {
		return repository.findById(id);
	}
	
	@Override
	public Optional<Restaurant> getRestaurant(String login) {
		return repository.findByLogin(login);
	}
	
	
	@Override
	@Transactional
	public Unavailability createUnavailability(Unavailability unavailability) {
		unavailability = new Unavailability(unavailability);
		
		Restaurant restaurant = repository.findById(unavailability.getRestaurant().getId()).orElseThrow(() -> new EntityNotFoundException());
		
		if (LocalDateTime.now().isAfter(unavailability.getStart())) {
			throw new ScheduleTimePastException();
		}
		
		if (unavailability.getStart().equals(unavailability.getEnd()) || unavailability.getStart().isAfter(unavailability.getEnd())) {
			throw new ScheduleInvalidPeriodException();
		}
		
		if (unavailabilityRepository.existsByRestaurantAndPeriod(unavailability.getRestaurant(),
				unavailability.getStart(), unavailability.getEnd())) {
			throw new ScheduleConflictException();
		}
		
		unavailability.setCreation(LocalDateTime.now());
		
		unavailability = unavailabilityRepository.save(unavailability);
		
		eventPublisher.publishEvent(new RestaurantChangeEvent(restaurant));
		
		return unavailability;
	}
	
	@Override
	public Optional<Unavailability> getUnavailability(Long id, Restaurant restaurant) {
		return unavailabilityRepository.findByIdAndRestaurant(id, restaurant);
	}
	
	@Override
	@Transactional
	public void deleteUnavailability(Long id, Restaurant restaurant) {
		Unavailability unavailability = unavailabilityRepository.findByIdAndRestaurant(id, restaurant)
				.orElseThrow(() -> new EntityNotFoundException());
		
		if (LocalDateTime.now().isAfter(unavailability.getStart())) {
			throw new ScheduleRemovePastException(); 
		}
		
		unavailabilityRepository.delete(id);
		eventPublisher.publishEvent(new RestaurantChangeEvent(restaurant));
	}
	
	@Override
	public List<Unavailability> getUnavailabilities(Restaurant restaurant) {
		repository.findById(restaurant.getId()).orElseThrow(() -> new EntityNotFoundException());
		return unavailabilityRepository.findByRestaurantOrderByStartDesc(restaurant);
	}
	
	
	@Override
	public OfflineLog createOfflineLog(OfflineLog log) {
		log = new OfflineLog(log);
		log.setCreation(LocalDateTime.now());
		return offlineLogRepository.save(log);
	}
	
	@Override
	public List<OfflineLog> getOfflineLogs(Restaurant restaurant) {
		repository.findById(restaurant.getId()).orElseThrow(() -> new EntityNotFoundException());
		return offlineLogRepository.findByRestaurant(restaurant);
	}
	
	
	@Override
	public List<Restaurant> rank(LocalDate start, LocalDate end) {
		return repository.rank(LocalDateTime.of(start, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
	}
}