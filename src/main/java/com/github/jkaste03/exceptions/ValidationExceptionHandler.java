package com.github.jkaste03.exceptions;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ValidationExceptionHandler {
    private static final @NonNull URI VALIDATION_PROBLEM_TYPE = Objects
            .requireNonNull(URI.create("urn:problem-type:validation-error"));

    private final MessageSource messageSource;

    public ValidationExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ProblemDetail handleBeanValidation(Exception ex, Locale locale, HttpServletRequest request) {

        var bindingResult = (ex instanceof MethodArgumentNotValidException manve)
                ? manve.getBindingResult()
                : ((BindException) ex).getBindingResult();

        List<ValidationError> errors = bindingResult.getAllErrors().stream()
                .map(err -> toValidationError(err, locale))
                .toList();

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(VALIDATION_PROBLEM_TYPE);
        pd.setTitle(msg("problem.validation.title", locale, "Validation failed"));
        pd.setDetail(msg("problem.validation.detail", locale, "One or more fields have invalid values."));
        pd.setInstance(requestInstance(request));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolations(ConstraintViolationException ex, Locale locale,
            HttpServletRequest request) {

        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(this::toValidationError)
                .toList();

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(VALIDATION_PROBLEM_TYPE);
        pd.setTitle(msg("problem.validation.title", locale, "Validation failed"));
        pd.setDetail(msg("problem.validation.detail", locale, "One or more parameters have invalid values."));
        pd.setInstance(requestInstance(request));
        pd.setProperty("errors", errors);
        return pd;
    }

    /* ---------- helpers ---------- */

    private ValidationError toValidationError(ObjectError err, Locale locale) {
        // Feltnavn (eller objekt-navn for globale feil)
        String field = (err instanceof FieldError fe) ? fe.getField() : err.getObjectName();

        // "Kode" – bruk første code/validator-navn om mulig, ellers generisk
        String code = err.getCode() != null ? err.getCode() : "validation_error";

        // Lokaliser meldingen fra MessageSource
        String message = messageSource.getMessage(err, locale);

        // JSON Pointer-lignende sti for klienter som vil peke rett i payload
        String pointer = (err instanceof FieldError fe)
                ? pointerFromObjectAndField(fe.getObjectName(), fe.getField())
                : "/"; // global objektfeil

        return new ValidationError(field, code, message, pointer);
    }

    private ValidationError toValidationError(ConstraintViolation<?> v) {
        // Eksempel path: "create.arg0.name" eller "user.addresses[0].street"
        String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";

        String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        if (field.isBlank())
            field = "object";

        String code = (v.getConstraintDescriptor() != null && v.getConstraintDescriptor().getAnnotation() != null)
                ? v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
                : "constraint_violation";

        // Bean Validation har allerede lokalisert denne meldingen
        String message = v.getMessage();
        String pointer = pointerFromPath(path);
        return new ValidationError(field, code, message, pointer);
    }

    private String msg(String key, Locale locale, String fallback) {
        try {
            return messageSource.getMessage(key, null, locale);
        } catch (Exception _) {
            return fallback;
        }
    }

    private URI requestInstance(HttpServletRequest request) {
        // Absolutt URI inkl. query-string (RFC 7807 anbefaler full referanse)
        String url = request.getRequestURL().toString();
        String qs = request.getQueryString();
        return URI.create(qs == null ? url : url + "?" + qs);
        // Hvis du vil anonymisere query params, dropp "?"+qs
    }

    private String pointerFromObjectAndField(String objectName, String field) {
        // "user.addresses[0].street" -> "/user/addresses/0/street"
        String joined = (objectName == null || objectName.isBlank())
                ? field
                : objectName + "." + field;
        return pointerFromPath(joined);
    }

    private String pointerFromPath(String path) {
        if (path == null || path.isBlank())
            return "/";
        // Bytt punktum til '/', fjern ']' og bytt '[' til '/'
        String p = path.replace(".", "/")
                .replace("]", "")
                .replace("[", "/");
        if (!p.startsWith("/"))
            p = "/" + p;
        return p;
    }

    public record ValidationError(String field, String code, String message, String pointer) {
    }
}
