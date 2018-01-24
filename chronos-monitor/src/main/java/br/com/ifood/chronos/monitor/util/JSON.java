package br.com.ifood.chronos.monitor.util;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JSON {
	
	private static final ObjectMapper mapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(WRITE_DATES_AS_TIMESTAMPS);
	
	private JSON() {
		
	}
	
	public static String serialize(Object object) {
		try {
			return mapper.writer().writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T deserialize(byte[] value, Class<T> type) {
		try {
			return mapper.reader().forType(type).readValue(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> CompletionStage<T> deserializeAsync(String value, Class<T> type) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return mapper.reader().forType(type).readValue(value);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
}