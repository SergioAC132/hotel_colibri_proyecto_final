package com.hotel_colibri.api_rest.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce cualquier error a ProblemDetail (RFC 9457): type, title, status, detail, instance.
 * Nunca expone trazas, nombres de clases ni detalles de los servicios internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String TIPO_BASE = "https://hotelcolibri.mx/problemas/";

    /** Codigos de fault de los servicios externos y su estado HTTP equivalente. */
    private static final Map<String, HttpStatus> ESTADO_POR_FAULT = Map.of(
            "Client.MonedaNoSoportada", HttpStatus.BAD_REQUEST,
            "Client.RfcInvalido", HttpStatus.BAD_REQUEST,
            "Client.MontoInvalido", HttpStatus.BAD_REQUEST,
            "Client.PagoRechazado", HttpStatus.UNPROCESSABLE_CONTENT,
            "Client.TokenInvalido", HttpStatus.UNPROCESSABLE_CONTENT,
            "Client.PagoDuplicado", HttpStatus.CONFLICT,
            "Client.FacturaDuplicada", HttpStatus.CONFLICT);

    private static final Map<String, String> TITULO_POR_FAULT = Map.of(
            "Client.MonedaNoSoportada", "Moneda no soportada",
            "Client.RfcInvalido", "RFC inválido",
            "Client.MontoInvalido", "Monto inválido",
            "Client.PagoRechazado", "Pago rechazado",
            "Client.TokenInvalido", "Tarjeta inválida",
            "Client.PagoDuplicado", "Pago duplicado",
            "Client.FacturaDuplicada", "Factura duplicada");

    // ------------------------------------------------------------------ negocio

    @ExceptionHandler(ApiException.class)
    public ProblemDetail manejarNegocio(ApiException ex, HttpServletRequest request) {
        return problema(ex.getStatus(), ex.getTipo(), ex.getTitulo(), ex.getMessage(), request);
    }

    @ExceptionHandler(SoapFaultException.class)
    public ProblemDetail manejarFault(SoapFaultException ex, HttpServletRequest request) {
        HttpStatus status = ESTADO_POR_FAULT.get(ex.getCodigo());
        if (status == null) {
            log.warn("Fault no mapeado del servicio externo: {} - {}", ex.getCodigo(), ex.getMessage());
            return problema(HttpStatus.BAD_GATEWAY, "error-servicio-externo", "Error en servicio externo",
                    "El servicio externo respondió con un error inesperado.", request);
        }
        String codigo = ex.getCodigo().substring(ex.getCodigo().indexOf('.') + 1);
        return problema(status, aKebab(codigo), TITULO_POR_FAULT.get(ex.getCodigo()), ex.getMessage(), request);
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ProblemDetail manejarNoDisponible(ServicioNoDisponibleException ex, HttpServletRequest request) {
        log.warn("Servicio externo '{}' no disponible: {}", ex.getServicio(),
                ex.getCause() != null ? ex.getCause().getMessage() : "-");
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "servicio-no-disponible", "Servicio no disponible",
                ex.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail manejarRestricciones(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String ruta = v.getPropertyPath().toString();
            errores.put(ruta.substring(ruta.lastIndexOf('.') + 1), v.getMessage());
        }
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "datos-invalidos", "Datos inválidos",
                resumen(errores), request);
        pd.setProperty("errores", errores);
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail manejarIntegridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violacion de integridad: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "recurso-duplicado", "Conflicto de datos",
                "La operación entra en conflicto con datos existentes.", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarGenerico(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "Error interno",
                "Ocurrió un error inesperado. Intente más tarde.", request);
    }

    // ------------------------------------------------------- errores de Spring MVC

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errores.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "datos-invalidos", "Datos inválidos",
                resumen(errores), request);
        pd.setProperty("errores", errores);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(r -> errores.put(
                r.getMethodParameter().getParameterName(),
                r.getResolvableErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.joining("; "))));
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "datos-invalidos", "Datos inválidos",
                resumen(errores), request);
        pd.setProperty("errores", errores);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detalle = tieneCausa(ex, DateTimeParseException.class)
                ? "Alguna fecha tiene un formato inválido. Use AAAA-MM-DD, por ejemplo 2027-01-15."
                : "El cuerpo de la petición no es un JSON válido o algún campo tiene un tipo incorrecto.";
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "cuerpo-invalido", "Cuerpo de la petición inválido",
                detalle, request);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "parametro-faltante", "Parámetro faltante",
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'.", request);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Cubre MethodArgumentTypeMismatchException (subclase de TypeMismatchException). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String nombre = ex.getPropertyName() != null ? ex.getPropertyName() : "parámetro";
        String detalle = "El valor '" + ex.getValue() + "' no es válido para '" + nombre + "'"
                + (ex.getRequiredType() != null && ex.getRequiredType().getSimpleName().equals("LocalDate")
                    ? ". Use el formato AAAA-MM-DD." : ".");
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "parametro-invalido", "Parámetro inválido",
                detalle, request);
        return handleExceptionInternal(ex, pd, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Punto comun de salida de los errores de Spring MVC: completa type, title, detail e instance en espanol. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }
        if (body instanceof ProblemDetail pd) {
            if (pd.getType() == null || "about:blank".equals(pd.getType().toString())) {
                completarGenerico(pd, ex, statusCode);
            }
            if (pd.getInstance() == null && request instanceof ServletWebRequest swr) {
                pd.setInstance(URI.create(swr.getRequest().getRequestURI()));
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // ------------------------------------------------------------------ utilidades

    private void completarGenerico(ProblemDetail pd, Exception ex, HttpStatusCode status) {
        switch (status.value()) {
            case 404 -> fijar(pd, "recurso-no-encontrado", "Recurso no encontrado", "La ruta solicitada no existe.");
            case 405 -> fijar(pd, "metodo-no-permitido", "Método no permitido",
                    ex instanceof HttpRequestMethodNotSupportedException m
                            ? "El método " + m.getMethod() + " no está permitido en este recurso."
                            : "Método HTTP no permitido en este recurso.");
            case 406 -> fijar(pd, "formato-no-aceptable", "Formato no aceptable",
                    "No se puede responder en el formato solicitado; use application/json.");
            case 415 -> fijar(pd, "tipo-contenido-no-soportado", "Tipo de contenido no soportado",
                    ex instanceof HttpMediaTypeNotSupportedException
                            ? "Envíe el cuerpo con Content-Type: application/json." : "Tipo de contenido no soportado.");
            case 400 -> fijar(pd, "solicitud-invalida", "Solicitud inválida", "La solicitud no es válida.");
            default -> {
                if (status.is5xxServerError()) {
                    fijar(pd, "error-interno", "Error interno", "Ocurrió un error inesperado. Intente más tarde.");
                } else {
                    fijar(pd, "solicitud-invalida", "Solicitud inválida", "La solicitud no pudo procesarse.");
                }
            }
        }
        if (ex instanceof NoResourceFoundException) {
            pd.setDetail("La ruta solicitada no existe.");
        }
    }

    private static void fijar(ProblemDetail pd, String tipo, String titulo, String detalle) {
        pd.setType(URI.create(TIPO_BASE + tipo));
        pd.setTitle(titulo);
        pd.setDetail(detalle);
    }

    private static ProblemDetail problema(HttpStatus status, String tipo, String titulo, String detalle,
                                          HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setType(URI.create(TIPO_BASE + tipo));
        pd.setTitle(titulo);
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }

    private static ProblemDetail problema(HttpStatus status, String tipo, String titulo, String detalle,
                                          WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setType(URI.create(TIPO_BASE + tipo));
        pd.setTitle(titulo);
        if (request instanceof ServletWebRequest swr) {
            pd.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }
        return pd;
    }

    private static String resumen(Map<String, String> errores) {
        if (errores.isEmpty()) {
            return "Los datos enviados no son válidos.";
        }
        List<String> partes = errores.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).toList();
        return "Los datos enviados no son válidos. " + String.join("; ", partes) + ".";
    }

    private static boolean tieneCausa(Throwable ex, Class<? extends Throwable> tipo) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (tipo.isInstance(t)) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /** "MonedaNoSoportada" -> "moneda-no-soportada". */
    private static String aKebab(String texto) {
        return texto.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
}
