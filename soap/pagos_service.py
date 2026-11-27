"""Servicio SOAP de pagos y facturacion del Hotel Colibri.

WSDL: http://localhost:8002/?wsdl
"""
import logging
import re
import threading
from decimal import Decimal as D, ROUND_HALF_UP

from spyne import Application, rpc, ServiceBase, Unicode, Decimal, Boolean
from spyne.error import Fault
from spyne.protocol.soap import Soap11
from spyne.server.wsgi import WsgiApplication

from wsgiref.simple_server import make_server

PUERTO = 8002
TNS = 'http://hotelcolibri.mx/pagos'

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s [pagos] %(message)s')
log = logging.getLogger('pagos')

MONEDAS = ('MXN', 'USD', 'EUR', 'CAD')

TOKEN_APROBADA = 'tok_aprobada'
TOKEN_FONDOS_INSUFICIENTES = 'tok_fondos_insuficientes'
TOKEN_RECHAZADA = 'tok_rechazada'
TOKENS_VALIDOS = (TOKEN_APROBADA, TOKEN_FONDOS_INSUFICIENTES, TOKEN_RECHAZADA)

APROBADO = 'APROBADO'
REEMBOLSADO = 'REEMBOLSADO'

RFC_REGEX = re.compile(r'^[A-ZÑ&]{3,4}\d{6}[A-Z0-9]{3}$')

DOS_DECIMALES = D('0.01')

# ---- Datos en memoria (no se requiere base de datos) ----
PAGOS = {}      # folioPago -> {referencia, monto, moneda, estado}
FACTURAS = {}   # folioFactura -> {folioPago, rfc, razonSocial}
CONTADORES = {'pago': 0, 'factura': 0}
candado = threading.Lock()

# Tipos de parametros: todos obligatorios en el WSDL
Texto = Unicode(min_occurs=1, nillable=False)
Monto = Decimal(min_occurs=1, nillable=False)


# ---- Errores de negocio: se informan como SOAP Fault con un codigo propio ----

def monto_invalido(monto):
    return Fault(faultcode='Client.MontoInvalido',
                 faultstring=f'El monto debe ser mayor que cero (recibido: {monto})')


def moneda_no_soportada(moneda):
    return Fault(faultcode='Client.MonedaNoSoportada',
                 faultstring=f'La moneda "{moneda}" no esta soportada. '
                             f'Monedas validas: {", ".join(MONEDAS)}')


def token_invalido(token):
    return Fault(faultcode='Client.TokenInvalido',
                 faultstring=f'El token de tarjeta "{token}" no es valido')


def pago_duplicado(referencia, folio):
    return Fault(faultcode='Client.PagoDuplicado',
                 faultstring=f'La referencia {referencia} ya tiene un pago aprobado '
                             f'(folio {folio})')


def pago_rechazado(motivo):
    return Fault(faultcode='Client.PagoRechazado',
                 faultstring=f'El pago fue rechazado por el banco emisor: {motivo}')


def pago_inexistente(folio):
    return Fault(faultcode='Client.PagoInexistente',
                 faultstring=f'El pago con folio {folio} no existe')


def pago_ya_reembolsado(folio):
    return Fault(faultcode='Client.PagoYaReembolsado',
                 faultstring=f'El pago {folio} ya fue reembolsado')


def rfc_invalido(rfc):
    return Fault(faultcode='Client.RfcInvalido',
                 faultstring=f'El RFC "{rfc}" no tiene un formato valido')


def pago_no_facturable(folio):
    return Fault(faultcode='Client.PagoNoFacturable',
                 faultstring=f'El pago {folio} fue reembolsado y no se puede facturar')


def factura_duplicada(folio_pago, folio_factura):
    return Fault(faultcode='Client.FacturaDuplicada',
                 faultstring=f'El pago {folio_pago} ya tiene la factura {folio_factura}')


# ---- Utilidades ----

def siguiente_folio(tipo, prefijo):
    CONTADORES[tipo] += 1
    return f'{prefijo}-{CONTADORES[tipo]:06d}'


def obtener_pago(folio):
    pago = PAGOS.get(folio)
    if pago is None:
        raise pago_inexistente(folio)
    return pago


def pago_aprobado_de(referencia):
    for folio, pago in PAGOS.items():
        if pago['referencia'] == referencia and pago['estado'] == APROBADO:
            return folio
    return None


def factura_de(folio_pago):
    for folio, factura in FACTURAS.items():
        if factura['folioPago'] == folio_pago:
            return folio
    return None


class PagosService(ServiceBase):

    @rpc(Texto, Monto, Texto, Texto, _returns=Unicode)
    def autorizarPago(ctx, referencia, monto, moneda, tokenTarjeta):
        referencia = (referencia or '').strip()
        moneda = (moneda or '').strip().upper()
        token = (tokenTarjeta or '').strip()
        log.info('autorizarPago(referencia=%s, monto=%s, moneda=%s, tokenTarjeta=%s)',
                 referencia, monto, moneda, token)

        if monto is None or monto <= 0:
            raise monto_invalido(monto)
        if moneda not in MONEDAS:
            raise moneda_no_soportada(moneda)
        if token not in TOKENS_VALIDOS:
            raise token_invalido(token)

        with candado:
            folio_existente = pago_aprobado_de(referencia)
            if folio_existente is not None:
                raise pago_duplicado(referencia, folio_existente)
            if token == TOKEN_FONDOS_INSUFICIENTES:
                raise pago_rechazado('fondos insuficientes')
            if token == TOKEN_RECHAZADA:
                raise pago_rechazado('tarjeta rechazada')

            folio = siguiente_folio('pago', 'PAG')
            PAGOS[folio] = {
                'referencia': referencia,
                'monto': monto.quantize(DOS_DECIMALES, rounding=ROUND_HALF_UP),
                'moneda': moneda,
                'estado': APROBADO,
            }

        log.info('  -> pago %s APROBADO por %s %s', folio, PAGOS[folio]['monto'], moneda)
        return folio

    @rpc(Texto, _returns=Unicode)
    def consultarPago(ctx, folioPago):
        folio = (folioPago or '').strip().upper()
        log.info('consultarPago(folioPago=%s)', folio)
        estado = obtener_pago(folio)['estado']
        log.info('  -> %s', estado)
        return estado

    @rpc(Texto, _returns=Boolean)
    def reembolsarPago(ctx, folioPago):
        folio = (folioPago or '').strip().upper()
        log.info('reembolsarPago(folioPago=%s)', folio)
        with candado:
            pago = obtener_pago(folio)
            if pago['estado'] == REEMBOLSADO:
                raise pago_ya_reembolsado(folio)
            pago['estado'] = REEMBOLSADO
        log.info('  -> pago %s REEMBOLSADO', folio)
        return True

    @rpc(Texto, Texto, Texto, _returns=Unicode)
    def emitirFactura(ctx, folioPago, rfc, razonSocial):
        folio_pago = (folioPago or '').strip().upper()
        rfc = (rfc or '').strip().upper()
        razon_social = (razonSocial or '').strip()
        log.info('emitirFactura(folioPago=%s, rfc=%s, razonSocial=%s)',
                 folio_pago, rfc, razon_social)

        with candado:
            pago = obtener_pago(folio_pago)
            if not RFC_REGEX.match(rfc):
                raise rfc_invalido(rfc)
            if pago['estado'] == REEMBOLSADO:
                raise pago_no_facturable(folio_pago)
            folio_existente = factura_de(folio_pago)
            if folio_existente is not None:
                raise factura_duplicada(folio_pago, folio_existente)

            folio = siguiente_folio('factura', 'FAC')
            FACTURAS[folio] = {
                'folioPago': folio_pago,
                'rfc': rfc,
                'razonSocial': razon_social,
            }

        log.info('  -> factura %s emitida para el pago %s', folio, folio_pago)
        return folio


application = Application([PagosService], TNS,
                          name='Pagos',
                          in_protocol=Soap11(validator='lxml'),
                          out_protocol=Soap11())

wsgi_application = WsgiApplication(application)

if __name__ == '__main__':
    server = make_server('0.0.0.0', PUERTO, wsgi_application)
    print(f'Servicio Pagos escuchando en http://127.0.0.1:{PUERTO}')
    print(f'El WSDL esta disponible en http://localhost:{PUERTO}/?wsdl')
    server.serve_forever()
