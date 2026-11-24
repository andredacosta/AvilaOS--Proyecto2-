
package com.avilaos.disk;

import com.avilaos.structures.Cola;

/**
 * Politica de planificacion SCAN.
 *
 * Atiende solicitudes siguiendo la direccion
 * actual del cabezal.
 *
 * Si no existen solicitudes en esa direccion,
 * invierte el sentido de busqueda.
 *
 * No utiliza colecciones de Java.
 */
public class SCAN implements PoliticaPlanificacionDisco {

    private int direccionActual;
    private boolean inicializado;

    public SCAN() {
        this.direccionActual = 1;
        this.inicializado = false;
    }

    @Override
    public SolicitudDisco seleccionar(
            Cola<SolicitudDisco> cola,
            int posicionCabezal,
            int totalBloques,
            int direccion) {

        if (cola == null) {
            throw new IllegalArgumentException(
                "La cola no puede ser nula"
            );
        }

        if (totalBloques <= 0
                || posicionCabezal < 0
                || posicionCabezal >= totalBloques) {
            throw new IllegalArgumentException(
                "Posicion o capacidad invalida"
            );
        }

        if (direccion != 1 && direccion != -1) {
            throw new IllegalArgumentException(
                "La direccion debe ser 1 o -1"
            );
        }

        if (cola.estaVacia()) {
            return null;
        }

        // La direccion recibida establece
        // el sentido inicial.
        if (!inicializado) {
            direccionActual = direccion;
            inicializado = true;
        }

        SolicitudDisco elegida =
                buscarEnDireccion(
                    cola,
                    posicionCabezal,
                    totalBloques,
                    direccionActual
                );

        // Si no hay solicitudes hacia ese lado,
        // invertimos la direccion.
        if (elegida == null) {

            direccionActual = -direccionActual;

            elegida = buscarEnDireccion(
                cola,
                posicionCabezal,
                totalBloques,
                direccionActual
            );
        }

        if (elegida == null) {
            return null;
        }

        // Retirar solo la solicitud elegida,
        // conservando el orden de las demas.
        int cantidad = cola.getTamanio();
        boolean retirada = false;

        for (int i = 0; i < cantidad; i++) {

            SolicitudDisco actual =
                    cola.desencolar();

            if (!retirada && actual == elegida) {
                retirada = true;
            } else {
                cola.encolar(actual);
            }
        }

        return elegida;
    }

    private SolicitudDisco buscarEnDireccion(
            Cola<SolicitudDisco> cola,
            int posicionCabezal,
            int totalBloques,
            int direccion) {

        int cantidad = cola.getTamanio();

        SolicitudDisco mejor = null;
        long menorDistancia = Long.MAX_VALUE;

        for (int i = 0; i < cantidad; i++) {

            SolicitudDisco solicitud =
                    cola.desencolar();

            int bloque = solicitud.getBloqueDestino();

            if (bloque < 0 || bloque >= totalBloques) {

                cola.encolar(solicitud);

                throw new IllegalArgumentException(
                    "Solicitud fuera de los limites del disco"
                );
            }

            long diferencia =
                    (long) bloque - posicionCabezal;

            boolean estaEnDireccion =
                    direccion == 1
                    ? diferencia >= 0
                    : diferencia <= 0;

            if (estaEnDireccion) {

                long distancia =
                        Math.abs(diferencia);

                if (distancia < menorDistancia) {

                    menorDistancia = distancia;
                    mejor = solicitud;
                }
            }

            cola.encolar(solicitud);
        }

        return mejor;
    }

    public int getDireccionActual() {
        return direccionActual;
    }

    public void reiniciarDireccion(int direccion) {

        if (direccion != 1 && direccion != -1) {
            throw new IllegalArgumentException(
                "Direccion invalida"
            );
        }

        direccionActual = direccion;
        inicializado = true;
    }

    @Override
    public String getNombre() {
        return "SCAN";
    }
}
