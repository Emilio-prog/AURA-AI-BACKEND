package com.auraia.backend.exceptions;

import com.auraia.backend.models.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request,
                                                      Locale locale) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, code("error.validation", locale), request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex,
                                                      HttpServletRequest request,
                                                      Locale locale) {
        return error(HttpStatus.BAD_REQUEST, code("error.validation", locale), request, Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                    HttpServletRequest request,
                                                    Locale locale) {
        return error(HttpStatus.NOT_FOUND, code("error.resource_not_found", locale), request, Map.of());
    }

    @ExceptionHandler({UnauthorizedException.class, BadCredentialsException.class})
    ResponseEntity<ApiErrorResponse> handleUnauthorized(RuntimeException ex,
                                                        HttpServletRequest request,
                                                        Locale locale) {
        return error(HttpStatus.UNAUTHORIZED, code("error.unauthorized", locale), request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleForbidden(AccessDeniedException ex,
                                                     HttpServletRequest request,
                                                     Locale locale) {
        return error(HttpStatus.FORBIDDEN, code("error.forbidden", locale), request, Map.of());
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex,
                                                    HttpServletRequest request,
                                                    Locale locale) {
        return error(HttpStatus.BAD_REQUEST, code(ex.getMessageCode(), locale), request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnhandled(Exception ex,
                                                    HttpServletRequest request,
                                                    Locale locale) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, code("error.business", locale), request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status,
                                                   String message,
                                                   HttpServletRequest request,
                                                   Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            fieldErrors
        ));
    }

    private String code(String code, Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }
}
