package com.urlshortener.exception;

import com.urlshortener.dto.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (left, right) -> left, LinkedHashMap::new));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(UrlGoneException.class)
    public ResponseEntity<?> handleGone(UrlGoneException ex, HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        if (uri.startsWith("/r/") || (accept != null && accept.contains("text/html"))) {
            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Link Expired - URL Shortener</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }
                        .overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.85); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; padding: 20px; z-index: 9999; }
                        .modal-card { background: #ffffff; border-radius: 24px; padding: 36px 32px; max-width: 460px; width: 100%; text-align: center; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); }
                        .icon-container { width: 72px; height: 72px; background: #fef2f2; border: 2px solid #fee2e2; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; color: #dc2626; }
                        h2 { color: #0f172a; font-size: 22px; font-weight: 700; margin-bottom: 12px; }
                        .popup-message { background: #fef2f2; border: 1.5px solid #fca5a5; border-radius: 12px; padding: 16px; color: #991b1b; font-size: 15px; font-weight: 600; margin-bottom: 20px; }
                        p.subtext { color: #64748b; font-size: 14px; line-height: 1.6; }
                    </style>
                </head>
                <body>
                    <div class="overlay">
                        <div class="modal-card">
                            <div class="icon-container">
                                <svg width="36" height="36" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                                </svg>
                            </div>
                            <h2>Link Expired</h2>
                            <div class="popup-message">
                                ⌛ This short link has expired.
                            </div>
                            <p class="subtext">This link is no longer active because its configured expiration time has passed.</p>
                        </div>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.status(HttpStatus.GONE)
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(html);
        }
        return buildResponse(HttpStatus.GONE, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(UrlInactiveException.class)
    public ResponseEntity<?> handleInactive(UrlInactiveException ex, HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        if (uri.startsWith("/r/") || (accept != null && accept.contains("text/html"))) {
            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Session Inactive - URL Shortener</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }
                        .overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.85); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; padding: 20px; z-index: 9999; }
                        .modal-card { background: #ffffff; border-radius: 24px; padding: 36px 32px; max-width: 460px; width: 100%; text-align: center; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); animation: modalPop 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
                        @keyframes modalPop { 0% { transform: scale(0.85) translateY(10px); opacity: 0; } 100% { transform: scale(1) translateY(0); opacity: 1; } }
                        .icon-container { width: 72px; height: 72px; background: #fff7ed; border: 2px solid #ffedd5; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; color: #ea580c; }
                        h2 { color: #0f172a; font-size: 22px; font-weight: 700; margin-bottom: 12px; }
                        .popup-message { background: #fff7ed; border: 1.5px solid #fdba74; border-radius: 12px; padding: 16px; color: #9a3412; font-size: 15px; font-weight: 600; margin-bottom: 20px; line-height: 1.5; }
                        p.subtext { color: #64748b; font-size: 14px; line-height: 1.6; }
                    </style>
                </head>
                <body>
                    <div class="overlay">
                        <div class="modal-card">
                            <div class="icon-container">
                                <svg width="36" height="36" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                                </svg>
                            </div>
                            <h2>Link Currently Inactive</h2>
                            <div class="popup-message">
                                ⚠️ Please activate the session / URL from your dashboard to enable redirection.
                            </div>
                            <p class="subtext">The owner of this link has temporarily deactivated it. Redirection is disabled until the session status is set back to <strong>ACTIVE</strong> in the dashboard.</p>
                        </div>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body(html);
        }
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler({UnauthorizedOperationException.class, AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExceededException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request, Map<String, String> details) {
        ApiErrorResponse body = ApiErrorResponse.builder()
            .timestamp(OffsetDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(request.getRequestURI())
            .details(details)
            .build();
        return ResponseEntity.status(status).body(body);
    }
}
