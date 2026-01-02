package com.github.jkaste03.exceptions;

import java.net.URI;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import com.github.jkaste03.service.SseService;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private static final @NonNull URI NOT_FOUND_TYPE = Objects.requireNonNull(URI.create("urn:problem-type:not-found"));
	private static final @NonNull URI CONFLICT_TYPE = Objects.requireNonNull(URI.create("urn:problem-type:conflict"));
	private static final @NonNull URI FORBIDDEN_TYPE = Objects.requireNonNull(URI.create("urn:problem-type:forbidden"));
	private static final @NonNull URI CONNECTION_ERROR_TYPE = Objects
			.requireNonNull(URI.create("urn:problem-type:connection-error"));
	private static final @NonNull URI INTERNAL_ERROR_TYPE = Objects
			.requireNonNull(URI.create("urn:problem-type:internal-error"));

	private final MessageSource messageSource;

	public ApiExceptionHandler(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, Locale locale,
			HttpServletRequest request) {

		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		pd.setType(NOT_FOUND_TYPE);
		pd.setTitle(msg("problem.not-found.title", locale));
		pd.setDetail(msg(ex.messageKey(), locale, ex.messageArgs()));
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", ex.messageKey());
		return pd;
	}

	@ExceptionHandler(ConflictException.class)
	public ProblemDetail handleConflict(ConflictException ex, Locale locale, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
		pd.setType(CONFLICT_TYPE);
		pd.setTitle(msg("problem.conflict.title", locale));
		pd.setDetail(msg(ex.messageKey(), locale, ex.messageArgs()));
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", ex.messageKey());
		return pd;
	}

	@ExceptionHandler(ForbiddenException.class)
	public ProblemDetail handleForbidden(ForbiddenException ex, Locale locale, HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
		pd.setType(FORBIDDEN_TYPE);
		pd.setTitle(msg("problem.forbidden.title", locale));
		pd.setDetail(msg(ex.messageKey(), locale, ex.messageArgs()));
		pd.setInstance(requestInstance(request));
		pd.setProperty("code", ex.messageKey());
		return pd;
	}

	/**
	 * Fanger opp tekniske kommunikasjonsfeil fra SseService og gir en
	 * bruker-vennlig melding.
	 */
	@ExceptionHandler(SseService.SseDeliveryException.class)
	public ProblemDetail handleSseDeliveryException(SseService.SseDeliveryException ex, Locale locale,
			HttpServletRequest request) {
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
		pd.setType(CONNECTION_ERROR_TYPE);
		pd.setTitle(msg("problem.connection.title", locale));
		pd.setDetail(msg("problem.connection.detail", locale));
		pd.setInstance(requestInstance(request));

		// Vi velger å IKKE sende med ex.getMessage() her, da den inneholder teknisk
		// tekst. Hvis du vil logge den tekniske feilen, gjør Spring Boot det
		// automatisk i loggene.

		return pd;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneralException(Exception ex, Locale locale, HttpServletRequest request) {
		log.error("Unexpected error occurred at: " + request.getRequestURI(), ex);
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		pd.setType(INTERNAL_ERROR_TYPE);
		pd.setTitle(msg("problem.internal.title", locale, "System error"));
		pd.setDetail(msg("problem.internal.detail", locale,
				"An internal error occurred in the system. Please try again later or contact support."));
		pd.setInstance(requestInstance(request));
		return pd;
	}

	@ExceptionHandler({ AsyncRequestNotUsableException.class, IOException.class })
	public void handleSseClientDisconnect(Exception ex, HttpServletRequest request) throws Exception {
		if (isSseRequest(request)) {
			log.debug("SSE client disconnected: {}", request.getRequestURI());
			return; // do not attempt to render ProblemDetail on SSE streams
		}
		throw ex;
	}

	@ExceptionHandler(HttpMessageNotWritableException.class)
	public void handleSseWriteFailure(HttpMessageNotWritableException ex, HttpServletRequest request) throws Exception {
		if (isSseRequest(request)) {
			log.debug("SSE write failed (client likely gone): {}", request.getRequestURI());
			return;
		}
		throw ex;
	}

	private boolean isSseRequest(HttpServletRequest request) {
		String accept = request.getHeader("Accept");
		return (accept != null && accept.contains("text/event-stream"))
				|| request.getRequestURI().contains("/events");
	}

	private String msg(String key, Locale locale, Object... args) {
		// Kaller den nye metoden med null som default
		return msg(key, locale, null, args);
	}

	private String msg(String key, Locale locale, String defaultMessage, Object... args) {
		return messageSource.getMessage(key, args, defaultMessage, locale);
	}

	private URI requestInstance(HttpServletRequest request) {
		String url = request.getRequestURL().toString();
		String qs = request.getQueryString();
		return URI.create(qs == null ? url : url + "?" + qs);
	}
}
