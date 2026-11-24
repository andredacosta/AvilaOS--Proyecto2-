
package com.avilaos.disk;

/**
 * Representa una solicitud de entrada/salida al disco.
 *
 * Soporta operaciones sobre archivos y operaciones
 * de suspension/reactivacion mediante swap.
 *
 * Las solicitudes son atendidas por el planificador
 * de disco segun la politica seleccionada.
 */
public class SolicitudDisco {

    private final int id;
    private final int procesoId;
    private final OperacionDisco operacion;

    private final String rutaArchivo;
    private final int bloqueDestino;
    private final int cantidadBloques;
    private final long cicloLlegada;

    public SolicitudDisco(
            int id,
            int procesoId,
            OperacionDisco operacion,
            String rutaArchivo,
            int bloqueDestino,
            int cantidadBloques,
            long cicloLlegada) {

        if (id <= 0 || procesoId <= 0) {
            throw new IllegalArgumentException(
                "Los identificadores deben ser positivos"
            );
        }

        if (operacion == null) {
            throw new IllegalArgumentException(
                "La operacion no puede ser nula"
            );
        }

        if (bloqueDestino < 0) {
            throw new IllegalArgumentException(
                "El bloque destino no puede ser negativo"
            );
        }

        if (cantidadBloques < 0) {
            throw new IllegalArgumentException(
                "La cantidad de bloques no puede ser negativa"
            );
        }

        if (cicloLlegada < 0) {
            throw new IllegalArgumentException(
                "El ciclo de llegada no puede ser negativo"
            );
        }

        boolean esSwap =
                operacion == OperacionDisco.SUSPEND
                || operacion == OperacionDisco.REACTIVATE;

        if (!esSwap &&
                (rutaArchivo == null
                || rutaArchivo.isBlank())) {

            throw new IllegalArgumentException(
                "Las operaciones sobre archivos requieren una ruta"
            );
        }

        if ((operacion == OperacionDisco.CREATE
                || operacion == OperacionDisco.SUSPEND)
                && cantidadBloques == 0) {

            throw new IllegalArgumentException(
                "La operacion requiere al menos un bloque"
            );
        }

        this.id = id;
        this.procesoId = procesoId;
        this.operacion = operacion;
        this.rutaArchivo = rutaArchivo;
        this.bloqueDestino = bloqueDestino;
        this.cantidadBloques = cantidadBloques;
        this.cicloLlegada = cicloLlegada;
    }

    // =====================================
    // IDENTIFICACION
    // =====================================

    public int getId() {
        return id;
    }

    public int getProcesoId() {
        return procesoId;
    }

    public OperacionDisco getOperacion() {
        return operacion;
    }

    // =====================================
    // INFORMACION DE LA SOLICITUD
    // =====================================

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public int getBloqueDestino() {
        return bloqueDestino;
    }

    public int getCantidadBloques() {
        return cantidadBloques;
    }

    public long getCicloLlegada() {
        return cicloLlegada;
    }

    // =====================================
    // CLASIFICACION DE OPERACIONES
    // =====================================

    public boolean esOperacionSwap() {
        return operacion == OperacionDisco.SUSPEND
                || operacion == OperacionDisco.REACTIVATE;
    }

    public boolean esOperacionArchivo() {
        return !esOperacionSwap();
    }

    public boolean esSuspension() {
        return operacion == OperacionDisco.SUSPEND;
    }

    public boolean esReactivacion() {
        return operacion == OperacionDisco.REACTIVATE;
    }

    // =====================================
    // DISTANCIA DEL CABEZAL
    // =====================================

    /**
     * Calcula la distancia entre el cabezal
     * y el bloque objetivo de esta solicitud.
     *
     * Se utilizara en la politica SSTF.
     */
    public long calcularDistancia(int posicionCabezal) {

        if (posicionCabezal < 0) {
            throw new IllegalArgumentException(
                "Posicion del cabezal invalida"
            );
        }

        return Math.abs(
            (long) bloqueDestino - posicionCabezal
        );
    }

    // =====================================
    // REPRESENTACION
    // =====================================

    @Override
    public String toString() {

        return "Solicitud #" + id
                + " | Proceso: " + procesoId
                + " | Operacion: " + operacion
                + " | Bloque: " + bloqueDestino
                + " | Cantidad: " + cantidadBloques
                + " | Ciclo: " + cicloLlegada;
    }
}
