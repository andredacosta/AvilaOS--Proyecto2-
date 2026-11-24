
package com.avilaos.model;

public class PCB {

    private final int id;
    private final String nombre;
    private final int computadorId;
    private final TipoProceso tipo;

    private EstadoProceso estado;

    private int prioridad;
    private int memoriaAsignada;
    private int deadline;
    private int tiempoRestante;

    private int programCounter;
    private int mar;

    private boolean vencidoPorDeadline;

    private static final int INTERVALO_IO = 2;
    private static final int DURACION_IO = 3;

    private int ciclosIORestantes;
    private MotivoBloqueo motivoBloqueo;

    private boolean ioDiscoPendiente;
    private boolean ioDiscoCompletadaDuranteSuspension;

    private int ciclosLatenciaRestantes;
    private boolean latenciaAplicadaOperacion;

    private FaseOperacionBuffer faseOperacionBuffer;

    public PCB(int id, String nombre, int computadorId,
               TipoProceso tipo, int prioridad,
               int memoriaRequerida, int deadline,
               int totalInstrucciones) {

        if (id <= 0 || computadorId <= 0) {
            throw new IllegalArgumentException("ID invalido");
        }

        if (nombre == null || nombre.isBlank() || tipo == null) {
            throw new IllegalArgumentException("Proceso invalido");
        }

        if (memoriaRequerida <= 0 || deadline <= 0
                || totalInstrucciones <= 0) {
            throw new IllegalArgumentException(
                    "Los valores deben ser positivos");
        }

        this.id = id;
        this.nombre = nombre;
        this.computadorId = computadorId;
        this.tipo = tipo;

        this.estado = EstadoProceso.NUEVO;
        this.prioridad = prioridad;
        this.memoriaAsignada = memoriaRequerida;
        this.deadline = deadline;
        this.tiempoRestante = totalInstrucciones;

        this.programCounter = 0;
        this.mar = 0;
        this.vencidoPorDeadline = false;

        this.ciclosIORestantes = 0;
        this.motivoBloqueo = MotivoBloqueo.NINGUNO;
        this.ioDiscoPendiente = false;
        this.ioDiscoCompletadaDuranteSuspension = false;

        this.ciclosLatenciaRestantes = 0;
        this.latenciaAplicadaOperacion = false;

        this.faseOperacionBuffer =
                FaseOperacionBuffer.ESPERANDO_PERMISO;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getComputadorId() {
        return computadorId;
    }

    public TipoProceso getTipo() {
        return tipo;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public void setEstado(EstadoProceso estado) {
        if (estado == null) {
            throw new IllegalArgumentException("Estado invalido");
        }
        this.estado = estado;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public int getMemoriaAsignada() {
        return memoriaAsignada;
    }

    public int getDeadline() {
        return deadline;
    }

    public int getTiempoRestante() {
        return tiempoRestante;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public int getMar() {
        return mar;
    }

    public boolean isVencidoPorDeadline() {
        return vencidoPorDeadline;
    }

    public int getCiclosIORestantes() {
        return ciclosIORestantes;
    }

    public MotivoBloqueo getMotivoBloqueo() {
        return motivoBloqueo;
    }

    public boolean isIODiscoPendiente() {
        return ioDiscoPendiente;
    }

    public boolean isIODiscoCompletadaDuranteSuspension() {
        return ioDiscoCompletadaDuranteSuspension;
    }

    public int getCiclosLatenciaRestantes() {
        return ciclosLatenciaRestantes;
    }

    public boolean isLatenciaAplicadaOperacion() {
        return latenciaAplicadaOperacion;
    }

    public FaseOperacionBuffer getFaseOperacionBuffer() {
        return faseOperacionBuffer;
    }

    public void avanzarInstruccion() {
        if (tiempoRestante > 0
                && estado == EstadoProceso.EJECUCION) {
            programCounter++;
            mar++;
            tiempoRestante--;
        }
    }

    public void disminuirDeadline() {
        if (deadline > 0
                && estado != EstadoProceso.TERMINADO) {
            deadline--;
        }
    }

    public void vencerDeadline() {
        if (estado == EstadoProceso.TERMINADO) {
            return;
        }

        deadline = 0;
        vencidoPorDeadline = true;

        ciclosIORestantes = 0;
        ioDiscoPendiente = false;
        ioDiscoCompletadaDuranteSuspension = false;

        ciclosLatenciaRestantes = 0;
        latenciaAplicadaOperacion = false;
        motivoBloqueo = MotivoBloqueo.NINGUNO;

        estado = EstadoProceso.TERMINADO;
    }

    public void bloquear(MotivoBloqueo motivo) {
        if (motivo == null
                || motivo == MotivoBloqueo.NINGUNO) {
            throw new IllegalArgumentException(
                    "Motivo de bloqueo invalido");
        }

        if (estado != EstadoProceso.EJECUCION) {
            throw new IllegalStateException(
                    "Solo un proceso en ejecucion puede bloquearse");
        }

        motivoBloqueo = motivo;
        estado = EstadoProceso.BLOQUEADO;
    }

    public void desbloquear() {
        if (estado != EstadoProceso.BLOQUEADO) {
            throw new IllegalStateException(
                    "El proceso no esta bloqueado");
        }

        if (ioDiscoPendiente) {
            throw new IllegalStateException(
                    "La solicitud de disco aun no ha finalizado");
        }

        motivoBloqueo = MotivoBloqueo.NINGUNO;
        estado = EstadoProceso.LISTO;
    }

    public boolean debeSolicitarIO() {
        return tipo == TipoProceso.IO_BOUND
                && estado == EstadoProceso.EJECUCION
                && tiempoRestante > 0
                && programCounter > 0
                && programCounter % INTERVALO_IO == 0
                && ciclosIORestantes == 0
                && !ioDiscoPendiente;
    }

    public void iniciarIO() {
        if (!debeSolicitarIO()) {
            throw new IllegalStateException(
                    "El proceso no puede iniciar E/S");
        }

        ciclosIORestantes = DURACION_IO;
        bloquear(MotivoBloqueo.ENTRADA_SALIDA);
    }

    public void avanzarIO() {
        if (estado != EstadoProceso.BLOQUEADO
                || motivoBloqueo != MotivoBloqueo.ENTRADA_SALIDA
                || ioDiscoPendiente) {
            return;
        }

        if (ciclosIORestantes > 0) {
            ciclosIORestantes--;
        }
    }

    public boolean ioFinalizada() {
        return estado == EstadoProceso.BLOQUEADO
                && motivoBloqueo == MotivoBloqueo.ENTRADA_SALIDA
                && !ioDiscoPendiente
                && ciclosIORestantes == 0;
    }

    public void iniciarIODisco() {
        if (estado != EstadoProceso.EJECUCION) {
            throw new IllegalStateException(
                    "El proceso debe estar en ejecucion");
        }

        if (ioDiscoPendiente) {
            throw new IllegalStateException(
                    "Ya existe una solicitud de disco pendiente");
        }

        ciclosIORestantes = 0;
        ioDiscoCompletadaDuranteSuspension = false;

        bloquear(MotivoBloqueo.ENTRADA_SALIDA);
        ioDiscoPendiente = true;
    }

    public void completarIODisco() {
        if (!ioDiscoPendiente
                || estado != EstadoProceso.BLOQUEADO
                || motivoBloqueo != MotivoBloqueo.ENTRADA_SALIDA) {
            throw new IllegalStateException(
                    "El proceso no espera una E/S de disco");
        }

        ioDiscoPendiente = false;
        ioDiscoCompletadaDuranteSuspension = false;
        desbloquear();
    }

    public void completarIODiscoEnSuspension() {
        if (!ioDiscoPendiente
                || motivoBloqueo != MotivoBloqueo.ENTRADA_SALIDA
                || (estado != EstadoProceso.BLOQUEADO
                && estado != EstadoProceso.BLOQUEADO_SUSPENDIDO)) {
            throw new IllegalStateException(
                    "No existe E/S pendiente para suspension");
        }

        ioDiscoPendiente = false;

        if (estado == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
            motivoBloqueo = MotivoBloqueo.NINGUNO;
            estado = EstadoProceso.LISTO_SUSPENDIDO;
            ioDiscoCompletadaDuranteSuspension = false;
        } else {
            ioDiscoCompletadaDuranteSuspension = true;
        }
    }

    public void confirmarSuspension() {
        if (estado != EstadoProceso.BLOQUEADO_SUSPENDIDO) {
            throw new IllegalStateException(
                    "El proceso no esta bloqueado y suspendido");
        }

        if (ioDiscoCompletadaDuranteSuspension) {
            ioDiscoCompletadaDuranteSuspension = false;
            motivoBloqueo = MotivoBloqueo.NINGUNO;
            estado = EstadoProceso.LISTO_SUSPENDIDO;
        }
    }

    public void iniciarLatencia(int ciclos) {
        if (ciclos <= 0) {
            throw new IllegalArgumentException(
                    "La latencia debe ser positiva");
        }

        if (tipo != TipoProceso.PRODUCTOR
                && tipo != TipoProceso.CONSUMIDOR) {
            throw new IllegalStateException(
                    "El proceso no utiliza el buffer");
        }

        if (latenciaAplicadaOperacion) {
            throw new IllegalStateException(
                    "La latencia ya fue aplicada");
        }

        if (estado != EstadoProceso.EJECUCION) {
            throw new IllegalStateException(
                    "El proceso debe estar en ejecucion");
        }

        ciclosLatenciaRestantes = ciclos;
        latenciaAplicadaOperacion = true;

        bloquear(MotivoBloqueo.LATENCIA_RED);
    }

    public void avanzarLatencia() {
        if (estado != EstadoProceso.BLOQUEADO
                || motivoBloqueo != MotivoBloqueo.LATENCIA_RED) {
            return;
        }

        if (ciclosLatenciaRestantes > 0) {
            ciclosLatenciaRestantes--;
        }
    }

    public boolean latenciaFinalizada() {
        return estado == EstadoProceso.BLOQUEADO
                && motivoBloqueo == MotivoBloqueo.LATENCIA_RED
                && ciclosLatenciaRestantes == 0;
    }

    public void completarOperacionBuffer() {
        if (tipo != TipoProceso.PRODUCTOR
                && tipo != TipoProceso.CONSUMIDOR) {
            throw new IllegalStateException(
                    "El proceso no utiliza el buffer");
        }

        latenciaAplicadaOperacion = false;
        ciclosLatenciaRestantes = 0;
    }

    public void setFaseOperacionBuffer(
            FaseOperacionBuffer fase) {
        if (fase == null) {
            throw new IllegalArgumentException("Fase invalida");
        }

        this.faseOperacionBuffer = fase;
    }

    public void reiniciarFaseOperacionBuffer() {
        this.faseOperacionBuffer =
                FaseOperacionBuffer.ESPERANDO_PERMISO;
    }
}
