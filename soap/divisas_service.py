"""Servicio SOAP de divisas del Hotel Colibri.

WSDL: http://localhost:8001/?wsdl
"""
import logging
from decimal import Decimal as D, ROUND_HALF_UP

from spyne import Application, rpc, ServiceBase, Unicode, Decimal, Array
from spyne.error import Fault
from spyne.protocol.soap import Soap11
from spyne.server.wsgi import WsgiApplication

from wsgiref.simple_server import make_server

PUERTO = 8001
TNS = 'http://hotelcolibri.mx/divisas'

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s [divisas] %(message)s')
log = logging.getLogger('divisas')

# Tasas fijas: pesos mexicanos por unidad de cada moneda
TASAS = {
    'MXN': D('1.00'),
    'USD': D('18.00'),
    'EUR': D('21.00'),
    'CAD': D('13.00'),
}

DOS_DECIMALES = D('0.01')
SEIS_DECIMALES = D('0.000001')

# Tipos de parametros: todos obligatorios en el WSDL
Texto = Unicode(min_occurs=1, nillable=False)
Monto = Decimal(min_occurs=1, nillable=False)


# ---- Errores de negocio: se informan como SOAP Fault con un codigo propio ----

def moneda_no_soportada(codigo):
    return Fault(faultcode='Client.MonedaNoSoportada',
                 faultstring=f'La moneda "{codigo}" no esta soportada. '
                             f'Monedas validas: {", ".join(TASAS)}')


def monto_invalido(monto):
    return Fault(faultcode='Client.MontoInvalido',
                 faultstring=f'El monto debe ser mayor que cero (recibido: {monto})')


def normalizar_moneda(codigo):
    return (codigo or '').strip().upper()


def obtener_tasa(codigo):
    tasa = TASAS.get(codigo)
    if tasa is None:
        raise moneda_no_soportada(codigo)
    return tasa


class DivisasService(ServiceBase):

    @rpc(_returns=Array(Unicode))
    def listarMonedas(ctx):
        log.info('listarMonedas()')
        return list(TASAS)

    @rpc(Texto, Texto, _returns=Decimal)
    def obtenerTipoCambio(ctx, origen, destino):
        origen = normalizar_moneda(origen)
        destino = normalizar_moneda(destino)
        log.info('obtenerTipoCambio(origen=%s, destino=%s)', origen, destino)

        tasa_origen = obtener_tasa(origen)
        tasa_destino = obtener_tasa(destino)
        tipo = (tasa_origen / tasa_destino).quantize(SEIS_DECIMALES, rounding=ROUND_HALF_UP)

        log.info('  -> 1 %s = %s %s', origen, tipo, destino)
        return tipo

    @rpc(Monto, Texto, Texto, _returns=Decimal)
    def convertir(ctx, monto, origen, destino):
        origen = normalizar_moneda(origen)
        destino = normalizar_moneda(destino)
        log.info('convertir(monto=%s, origen=%s, destino=%s)', monto, origen, destino)

        tasa_origen = obtener_tasa(origen)
        tasa_destino = obtener_tasa(destino)
        if monto is None or monto <= 0:
            raise monto_invalido(monto)

        resultado = (monto * tasa_origen / tasa_destino).quantize(DOS_DECIMALES,
                                                                  rounding=ROUND_HALF_UP)
        log.info('  -> %s %s = %s %s', monto, origen, resultado, destino)
        return resultado


application = Application([DivisasService], TNS,
                          name='Divisas',
                          in_protocol=Soap11(validator='lxml'),
                          out_protocol=Soap11())

wsgi_application = WsgiApplication(application)

if __name__ == '__main__':
    server = make_server('0.0.0.0', PUERTO, wsgi_application)
    print(f'Servicio Divisas escuchando en http://127.0.0.1:{PUERTO}')
    print(f'El WSDL esta disponible en http://localhost:{PUERTO}/?wsdl')
    server.serve_forever()
