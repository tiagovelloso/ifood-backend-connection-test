package br.com.ifood.chronos.scheduler.domain.unavailability;

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

public enum Reason {
	
	LACK_DELIVERY_STAFF("L"), CONNECTION_ISSUES("C"), OVERLOADED("O"), HOLIDAY("H");
	
	private final String id;

	private Reason(final String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	@Converter(autoApply=true)
	public static class ReasonAttributeConverter implements AttributeConverter<Reason, String> {
		
		private static final Map<String,Reason> values = unmodifiableMap(stream(Reason.values()).collect(toMap(Reason::getId, identity())));

		@Override
		public String convertToDatabaseColumn(Reason reason) {
			return reason != null ? reason.id : null;
		}

		@Override
		public Reason convertToEntityAttribute(String id) {
			return values.get(id);
		}
	}
}