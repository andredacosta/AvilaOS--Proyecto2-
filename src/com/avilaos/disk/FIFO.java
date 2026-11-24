
package com.avilaos.disk;

import com.avilaos.structures.Cola;

/**
 * Planificador de disco FIFO
 * (First In, First Out).
 *
 * Atiende las solicitudes en el mismo orden
 * en el que llegaron a la cola de E/S.
 *
 * No utiliza colecciones de Java.
 */
public class FIFO implements PoliticaPlanificacionDisco {

    @Override
    public SolicitudDisco seleccionar(
            Cola<SolicitudDisco> cola,
            int posicionCabezal,
            int totalBloques,
            int direccion) {

        if (cola == null) {
            throw new IllegalArgumentException(
                "La cola de solicitudes no puede ser nula"
            );
        }

        if (totalBloques <= 0) {
            throw new IllegalArgumentException(
                "El disco debe tener al menos un bloque"
            );
        }

        if (posicionCabezal < 0
                || posicionCabezal >= totalBloques) {
            throw new IllegalArgumentException(
                "Posicion del cabezal invalida"
            );
        }

        if (direccion != 1 && direccion != -1) {
            throw new IllegalArgumentException(
                "Direccion del cabezal invalida"
            );
        }

        if (cola.estaVacia()) {
            return null;
        }

        // FIFO: retirar la solicitud mas antigua.
        return cola.desencolar();
    }

    @Override
    public String getNombre() {
        return "FIFO";
    }
}
