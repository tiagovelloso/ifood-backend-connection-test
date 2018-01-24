package br.com.ifood.chronos.scheduler.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.ifood.chronos.scheduler.service.exception.business.BusinessException;
import br.com.ifood.chronos.scheduler.service.exception.validation.EntityNotFoundException;

@ControllerAdvice
public class ResourceAdvisor extends ResponseEntityExceptionHandler {

	private final MessageSource messageSource;
	
	@Autowired
	public ResourceAdvisor(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
		logger.error(ex.getMessage());
		return super.handleExceptionInternal(ex, new APIErrors(getMessage(ex)), headers, status, request);
	}
	
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		
		List<APIError> errors = new ArrayList<>();
		
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.add(new APIError(error.getField() + ": " + error.getDefaultMessage()));
		}
		
		for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
			errors.add(new APIError(error.getObjectName() + ": " + error.getDefaultMessage()));
		}
		
		return handleExceptionInternal(ex, new APIErrors(errors), headers, HttpStatus.BAD_REQUEST, request);
	}
	
	@ExceptionHandler(BusinessException.class)
	protected ResponseEntity<APIErrors> handleBusinessException(BusinessException ex, WebRequest request) {
		return new ResponseEntity<>(new APIErrors(getMessage(ex)), HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	protected ResponseEntity<APIErrors> handleEntityNotFoundException(EntityNotFoundException ex) {
		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}
	
	@ExceptionHandler(ConstraintViolationException.class)
	protected ResponseEntity<APIErrors> handleConstraintViolationException(ConstraintViolationException ex) {
		return new ResponseEntity<>(new APIErrors(
			ex.getConstraintViolations().stream().map(c -> new APIError(c.getMessage())).collect(Collectors.toList())),
				HttpStatus.BAD_REQUEST);
	}
	
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	protected void handleException(Exception exception) {
		logger.error("Uncaught exception", exception);
	}
	
	private String getMessage(Object target) {
		try {
			return messageSource.getMessage(target.getClass().getName(), new Object[]{}, LocaleContextHolder.getLocale());
		} catch (NoSuchMessageException noSuchMessageException) {
			return null;
		}
	}
	
	
	public final class APIErrors {

		private final List<APIError> errors;

		public APIErrors() {
			this.errors = null;
		}

		public APIErrors(String message) {
			this.errors = Collections.singletonList(new APIError(message));
		}
		
		public APIErrors(List<APIError> errors) {
			this.errors = errors;
		}

		public APIErrors(APIError error) {
			this(Collections.singletonList(error));
		}

		public APIErrors(APIError ... errors) {
			this(Arrays.asList(errors));
		}

		public List<APIError> getErrors() {
			return errors;
		}
	}
	
	public final class APIError {
		
		private final String message;

		public APIError(String message) {
			this.message = message;
		}
		
		public String getMessage() {
			return message;
		}
	}
}