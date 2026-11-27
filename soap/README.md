# Servicios SOAP – Hotel Colibrí

Dos servicios SOAP 1.1 en Python (spyne + lxml), con datos en memoria.

| Servicio | Archivo              | Puerto | WSDL                          | Namespace                        |
|----------|----------------------|--------|-------------------------------|----------------------------------|
| Divisas  | `divisas_service.py` | 8001   | http://localhost:8001/?wsdl   | `http://hotelcolibri.mx/divisas` |
| Pagos    | `pagos_service.py`   | 8002   | http://localhost:8002/?wsdl   | `http://hotelcolibri.mx/pagos`   |

## Arranque en Windows

> **Usar Python 3.11.** spyne 2.14 no arranca en Python 3.12 o superior
> (`ModuleNotFoundError: No module named 'spyne.util.six.moves'`).
> Si no tienes 3.11: `winget install Python.Python.3.11`.

```powershell
cd soap
py -3.11 -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

Cada servicio va en su propia terminal (con el venv activado):

```powershell
python divisas_service.py   # puerto 8001
python pagos_service.py     # puerto 8002
```

Cada operación que se invoca queda registrada en la consola.

## Operaciones

**Divisas**: `listarMonedas()`, `obtenerTipoCambio(origen, destino)`, `convertir(monto, origen, destino)`
Faults: `Client.MonedaNoSoportada`, `Client.MontoInvalido`.

**Pagos**: `autorizarPago(referencia, monto, moneda, tokenTarjeta)`, `consultarPago(folioPago)`,
`reembolsarPago(folioPago)`, `emitirFactura(folioPago, rfc, razonSocial)`
Faults: `Client.MontoInvalido`, `Client.MonedaNoSoportada`, `Client.TokenInvalido`,
`Client.PagoDuplicado`, `Client.PagoRechazado`, `Client.PagoInexistente`,
`Client.PagoYaReembolsado`, `Client.RfcInvalido`, `Client.PagoNoFacturable`,
`Client.FacturaDuplicada`.

Tokens de prueba: `tok_aprobada`, `tok_fondos_insuficientes`, `tok_rechazada`.

## Ejemplos (Postman / curl)

En `ejemplos/divisas/` y `ejemplos/pagos/` hay un sobre SOAP por caso, incluido un Fault de
cada operación. Envíalos por `POST` a `http://localhost:8001/` o `http://localhost:8002/` con
el header `Content-Type: text/xml; charset=utf-8`.

Los sobres de pagos están numerados y dependen del estado: ejecútalos **en orden** con el
servicio recién arrancado (por ejemplo, el 05 espera que el 01 ya haya creado `PAG-000001`).

```powershell
curl.exe -s -H "Content-Type: text/xml; charset=utf-8" --data-binary "@ejemplos/divisas/04_convertir_MXN_USD.xml" http://localhost:8001/
```

Un Fault responde con HTTP 500 y, en el cuerpo, `<faultcode>soap11env:Client.XXX</faultcode>`.
