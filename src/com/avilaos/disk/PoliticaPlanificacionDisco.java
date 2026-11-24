
package com.avilaos.disk;

import com.avilaos.structures.Cola;

/**
 * Contrato para las politicas de planificacion del disco.
 *
 * Implementaciones: FIFO, SSTF, SCAN y C-SCAN.
 */
public interface PoliticaPlanificacionDisco {

    /**
     * Selecciona y retira la siguiente solicitud
     * de la cola de E/S.
     *
     * @param cola Solicitudes pendientes.
     * @param posicionCabezal Posicion actual del cabezal.
     * @param totalBloques Capacidad del disco.
     * @param direccion Direccion actual: 1 o -1.
     * @return Solicitud seleccionada, o null si esta vacia.
     */
    SolicitudDisco seleccionar(
            Cola<SolicitudDisco> cola,
            int posicionCabezal,
            int totalBloques,
            int direccion
    );

    String getNombre();
}
