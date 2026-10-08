package com.college.exception;

import com.college.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ONE place that turns every exception into the same JSON error ({@link ErrorResponse}).
 * "layer" says which layer found the problem - a favourite viva question.
 *
 *   Java validation          our own checks (400 / 404 / 409)
 *   Spring exception handling auth, bad JSON, bad parameter
 *   Oracle constraint        PK / UNIQUE / FK / CHECK / NOT NULL  (ORA-00001, 02291, 02292, 02290, 01400)
 *   Trigger                  ORA-20001 ... raised by a trigger
 *   PL/SQL exception         ORA-20010 ... raised by a function or procedure
 *   Database connection      Oracle not reachable (503)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern ORA = Pattern.compile("ORA-(\\d{5})[: ]*([^\\n]*)");
    private static final Pattern CONSTRAINT = Pattern.compile("\\(([A-Z0-9_]+\\.[A-Z0-9_]+)\\)");

    @ExceptionHandler(CollegeException.class)
    public ResponseEntity<ErrorResponse> college(CollegeException e) {
        HttpStatus status;
        String layer = "Java validation";
        if (e instanceof ResourceNotFoundException || e instanceof StudentNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (e instanceof DuplicateResourceException) {
            status = HttpStatus.CONFLICT;
        } else if (e instanceof UnauthorizedException) {
            status = HttpStatus.UNAUTHORIZED;
            layer = "Spring exception handling";
        } else if (e instanceof ForbiddenException) {
            status = HttpStatus.FORBIDDEN;
            layer = "Spring exception handling";
        } else if (e instanceof DatabaseException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            layer = "Database";
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return build(status, e.getClass().getSimpleName(), e.getMessage(), layer);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> duplicate(DuplicateKeyException e) {
        String text = oracleText(e);
        return build(HttpStatus.CONFLICT, "DuplicateKey",
                "Duplicate value rejected by Oracle" + constraintSuffix(text) + " - " + text, "Oracle constraint");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e) {
        String text = oracleText(e);
        int code = oraCode(text);
        HttpStatus status = code == 2292 || code == 1 ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        String hint = switch (code) {
            case 2291 -> "The parent record does not exist (foreign key). ";
            case 2292 -> "Child records still exist, so this cannot be deleted. ";
            case 2290 -> "A CHECK rule is violated. ";
            case 1400 -> "A mandatory value is missing (NOT NULL). ";
            default -> "";
        };
        return build(status, "DataIntegrityViolation", hint + text, "Oracle constraint");
    }

    /** ORA-20xxx raised by RAISE_APPLICATION_ERROR in a trigger or a PL/SQL unit. */
    @ExceptionHandler(UncategorizedSQLException.class)
    public ResponseEntity<ErrorResponse> plsql(UncategorizedSQLException e) {
        String text = oracleText(e);
        int code = oraCode(text);
        if (code >= 20000 && code <= 20999) {
            Matcher m = ORA.matcher(text);
            String message = m.find() ? m.group(2).trim() : text;
            String layer = code == 20001 || code == 20021 ? "Trigger" : "PL/SQL exception";
            return build(HttpStatus.BAD_REQUEST, "ORA-" + code, message, layer);
        }
        LOG.error("Unexpected SQL error", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "SqlError", text, "Database");
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public ResponseEntity<ErrorResponse> noConnection(CannotGetJdbcConnectionException e) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "DatabaseUnavailable",
                "Oracle is not reachable. Start it with ./run.sh (or ./setup-db.sh).", "Database connection");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception e) {
        return build(HttpStatus.BAD_REQUEST, "BadRequest", "The request body or parameter has the wrong format",
                "Spring exception handling");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> other(Exception e) {
        LOG.error("Unhandled error", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "InternalError",
                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), "Spring exception handling");
    }

    // ------------------------------------------------------------------ helpers
    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, String layer) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), error, message, layer));
    }

    private static String oracleText(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SQLException && c.getMessage() != null) {
                String m = c.getMessage();
                int nl = m.indexOf("\nORA-06512");
                return (nl > 0 ? m.substring(0, nl) : m).trim();
            }
        }
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }

    private static int oraCode(String text) {
        Matcher m = ORA.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static String constraintSuffix(String text) {
        Matcher m = CONSTRAINT.matcher(text);
        return m.find() ? " (constraint " + m.group(1).substring(m.group(1).indexOf('.') + 1) + ")" : "";
    }
}
