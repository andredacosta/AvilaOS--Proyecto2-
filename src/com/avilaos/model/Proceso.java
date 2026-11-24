
package com.avilaos.model;

public class Proceso {

    private final PCB pcb;

   
    private boolean ioGestionadaPorDisco;

    public Proceso(PCB pcb) {

        if (pcb == null) {
            throw new IllegalArgumentException(
                    "El PCB no puede ser nulo");
        }

        this.pcb = pcb;

       
        this.ioGestionadaPorDisco = false;
    }

    // ==========================================
    // GETTERS
    // ==========================================

    public PCB getPcb() {
        return pcb;
    }

    public boolean estaTerminado() {
        return pcb.getEstado() == EstadoProceso.TERMINADO;
    }

    public boolean puedeEjecutarse() {

        return pcb.getEstado() == EstadoProceso.EJECUCION
                && pcb.getTiempoRestante() > 0
                && pcb.getDeadline() > 0;
    }

    public boolean esProductorConsumidor() {

        return pcb.getTipo() == TipoProceso.PRODUCTOR
                || pcb.getTipo() == TipoProceso.CONSUMIDOR;
    }

    // ==========================================
    // CONFIGURACION DE E/S - PROYECTO 2
    // ==========================================

    /**
     * Activa o desactiva la E/S gestionada
     * por el disco simulado.
     *
     * false: comportamiento del Proyecto 1.
     * true: el Computador debe generar una
     *       SolicitudDisco y bloquear el PCB.
     */
    public void configurarIOGestionadaPorDisco(
            boolean habilitada) {

        this.ioGestionadaPorDisco = habilitada;
    }

    public boolean isIOGestionadaPorDisco() {
        return ioGestionadaPorDisco;
    }

    /**
     * Indica si el proceso necesita una nueva
     * solicitud de disco.
     *
     * Este metodo no bloquea el PCB ni agrega
     * solicitudes. Eso corresponde a Computador.
     */
    public boolean debeSolicitarDisco() {

        return ioGestionadaPorDisco
                && pcb.debeSolicitarIO();
    }

    // ==========================================
    // EJECUCION DE INSTRUCCIONES
    // ==========================================

    public void ejecutarCiclo() {

        if (!puedeEjecutarse()) {
            return;
        }

        pcb.avanzarInstruccion();

        if (pcb.getTiempoRestante() == 0) {
            finalizar();
            return;
        }

       
        if (pcb.debeSolicitarIO()
                && !ioGestionadaPorDisco) {

            pcb.iniciarIO();
        }
    }

    // ==========================================
    // PRODUCTOR - CONSUMIDOR
    // ==========================================

    public void completarOperacionBuffer() {

        if (!esProductorConsumidor()) {
            throw new IllegalStateException(
                    "El proceso no utiliza el buffer");
        }

        if (!puedeEjecutarse()) {
            throw new IllegalStateException(
                    "El proceso no puede ejecutar");
        }

        pcb.avanzarInstruccion();

        if (pcb.getTiempoRestante() == 0) {
            finalizar();
        }
    }

    // ==========================================
    // ENTRADA Y SALIDA DEL PROYECTO 1
    // ==========================================

    public void avanzarIO() {
        pcb.avanzarIO();
    }

    public boolean ioFinalizada() {
        return pcb.ioFinalizada();
    }

    // ==========================================
    // ENTRADA Y SALIDA DEL PROYECTO 2
    // ==========================================

    /**
     * Se llama despues de registrar correctamente
     * una solicitud en el planificador de disco.
     */
    public void iniciarIODisco() {

        if (!ioGestionadaPorDisco) {
            throw new IllegalStateException(
                    "La E/S de disco no esta habilitada");
        }

        pcb.iniciarIODisco();
    }

    /**
     * Se llama cuando el controlador recibe
     * la confirmacion de E/S finalizada.
     */
    public void completarIODisco() {
        pcb.completarIODisco();
    }

    public boolean tieneIODiscoPendiente() {
        return pcb.isIODiscoPendiente();
    }

    // ==========================================
    // FINALIZACION
    // ==========================================

    public void finalizar() {

        if (!estaTerminado()) {
            pcb.setEstado(EstadoProceso.TERMINADO);
        }
    }

    public void vencerDeadline() {

        if (!estaTerminado()) {
            pcb.vencerDeadline();
        }
    }

    public boolean vencioPorDeadline() {
        return pcb.isVencidoPorDeadline();
    }
}
