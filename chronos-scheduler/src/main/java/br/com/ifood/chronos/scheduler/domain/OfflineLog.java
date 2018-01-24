package br.com.ifood.chronos.scheduler.domain;

import static javax.persistence.GenerationType.SEQUENCE;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import br.com.ifood.chronos.scheduler.service.RestaurantService.CreateOfflineLogValidation;

@Entity
@SequenceGenerator(name="SQ_OFFLINE_LOG", sequenceName="SQ_OFFLINE_LOG", allocationSize=1)
public class OfflineLog {
	
	@Null(groups=CreateOfflineLogValidation.class)
	@Id
	@GeneratedValue(strategy=SEQUENCE, generator="SQ_OFFLINE_LOG")
	private Long id;
	
	@NotNull(groups=CreateOfflineLogValidation.class)
	@Valid
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="restaurant", nullable=false)
	private Restaurant restaurant;
	
	@NotNull(groups=CreateOfflineLogValidation.class)
	@Column(nullable=false)
	private LocalDateTime start;
	
	@NotNull(groups=CreateOfflineLogValidation.class)
	@Column(nullable=false)
	private LocalDateTime end;
	
	@NotNull(groups=CreateOfflineLogValidation.class)
	@Column(nullable=false)
	private Long offlineTime;
	
	@Null(groups=CreateOfflineLogValidation.class)
	@Column(nullable=false)
	private LocalDateTime creation;
	
	
	public OfflineLog() {
		
	}
	
	public OfflineLog(Long id) {
		this.id = id;
	}
	
	public OfflineLog(Restaurant restaurant, Long offlineTime) {
		this.restaurant = restaurant;
		this.offlineTime = offlineTime;
	}
	
	public OfflineLog(OfflineLog log) {
		id = log.id;
		restaurant = log.restaurant;
		start = log.start;
		end = log.end;
		offlineTime = log.offlineTime;
		creation = log.creation;
	}

	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public Restaurant getRestaurant() {
		return restaurant;
	}

	public void setRestaurant(Restaurant restaurant) {
		this.restaurant = restaurant;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public void setStart(LocalDateTime start) {
		this.start = start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	public void setEnd(LocalDateTime end) {
		this.end = end;
	}

	public Long getOfflineTime() {
		return offlineTime;
	}

	public void setOfflineTime(Long offlineTime) {
		this.offlineTime = offlineTime;
	}

	public LocalDateTime getCreation() {
		return creation;
	}

	public void setCreation(LocalDateTime creation) {
		this.creation = creation;
	}
}