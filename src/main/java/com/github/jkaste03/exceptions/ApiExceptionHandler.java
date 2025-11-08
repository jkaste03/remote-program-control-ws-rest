package com.github.jkaste03.exceptions;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
	private static final @NonNull URI NOT_FOUND_TYPE = Objects.requireNonNull(URI.create("urn:problem-type:not-found"));
	private static final @NonNull URI CONFLICT_TYPE = Objects.requireNonNull(URI.create("urn:problem-type:conflict"));

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

	private String msg(String key, Locale locale, Object... args) {
		return messageSource.getMessage(key, args, locale);
	}

	private URI requestInstance(HttpServletRequest request) {
		String url = request.getRequestURL().toString();
		String qs = request.getQueryString();
		return URI.create(qs == null ? url : url + "?" + qs);
	}
}
