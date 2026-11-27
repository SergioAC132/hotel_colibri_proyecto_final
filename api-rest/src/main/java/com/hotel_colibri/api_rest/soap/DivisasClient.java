package com.hotel_colibri.api_rest.soap;

import com.hotel_colibri.api_rest.exception.ServicioNoDisponibleException;
import com.hotel_colibri.api_rest.exception.SoapFaultException;
import com.hotel_colibri.api_rest.soap.divisas.generated.Divisas;
import com.hotel_colibri.api_rest.soap.divisas.generated.DivisasService;
import com.hotel_colibri.api_rest.soap.divisas.generated.StringArray;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Adaptador del servicio SOAP de divisas. Oculta JAX-WS al resto de la aplicacion y traduce
 * los errores a excepciones propias.
 */
@Component
public class DivisasClient {

    private static final Logger log = LoggerFactory.getLogger(DivisasClient.class);

    private static final String WSDL_CLASSPATH = "/wsdl/divisas.wsdl";
    private static final QName SERVICIO = new QName("http://hotelcolibri.mx/divisas", "DivisasService");
    private static final String CONNECT_TIMEOUT = "com.sun.xml.ws.connect.timeout";
    private static final String REQUEST_TIMEOUT = "com.sun.xml.ws.request.timeout";

    /** El Service es costoso de crear y thread-safe: se crea una sola vez desde el WSDL local. */
    private final DivisasService service;
    private final String endpoint;
    private final int timeoutMs;

    public DivisasClient(@Value("${soap.divisas.url}") String endpoint,
                         @Value("${soap.timeout-ms:3000}") int timeoutMs) {
        URL wsdl = DivisasClient.class.getResource(WSDL_CLASSPATH);
        if (wsdl == null) {
            throw new IllegalStateException("No se encontro " + WSDL_CLASSPATH + " en el classpath");
        }
        this.service = new DivisasService(wsdl, SERVICIO);
        this.endpoint = endpoint;
        this.timeoutMs = timeoutMs;
    }

    public List<String> listarMonedas() {
        return invocar("listarMonedas", port -> {
            StringArray resultado = port.listarMonedas();
            return resultado == null ? List.<String>of() : List.copyOf(resultado.getString());
        });
    }

    public BigDecimal convertir(BigDecimal monto, String origen, String destino) {
        return invocar("convertir", port -> port.convertir(monto, origen, destino));
    }

    /** El port NO es thread-safe: se obtiene uno nuevo en cada llamada. */
    private Divisas nuevoPort() {
        Divisas port = service.getDivisas();
        Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
        ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        ctx.put(CONNECT_TIMEOUT, timeoutMs);
        ctx.put(REQUEST_TIMEOUT, timeoutMs);
        return port;
    }

    private <T> T invocar(String operacion, Function<Divisas, T> llamada) {
        try {
            return llamada.apply(nuevoPort());
        } catch (SOAPFaultException e) {
            String codigo = codigoFault(e);
            log.info("Divisas.{} respondio fault {}: {}", operacion, codigo, e.getFault().getFaultString());
            throw new SoapFaultException(codigo, e.getFault().getFaultString());
        } catch (WebServiceException e) {
            throw new ServicioNoDisponibleException("divisas", e);
        }
    }

    /** "soap11env:Client.MonedaNoSoportada" -> "Client.MonedaNoSoportada". */
    static String codigoFault(SOAPFaultException e) {
        QName qname = e.getFault().getFaultCodeAsQName();
        if (qname != null && !qname.getLocalPart().isBlank()) {
            return qname.getLocalPart();
        }
        String codigo = e.getFault().getFaultCode();
        return codigo == null ? "" : codigo.substring(codigo.indexOf(':') + 1);
    }
}
