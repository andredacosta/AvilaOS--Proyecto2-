
package com.avilaos.core;

import com.avilaos.disk.OperacionDisco;
import com.avilaos.disk.PlanificadorDisco;
import com.avilaos.disk.SolicitudDisco;
import com.avilaos.model.EstadoProceso;
import com.avilaos.model.FaseOperacionBuffer;
import com.avilaos.model.MotivoBloqueo;
import com.avilaos.model.Proceso;
import com.avilaos.scheduler.PoliticaScheduler;
import com.avilaos.structures.Cola;
import com.avilaos.sync.BufferSimulado;
import com.avilaos.sync.SemaforoSimulado;

public class Computador implements ComponenteReloj {

    private final int id;
    private final CPU cpu;
    private final MemoriaRAM memoria;

    private final Cola<Proceso> colaNuevos;
    private final Cola<Proceso> colaListos;
    private final Cola<Proceso> colaBloqueados;
    private final Cola<Proceso> colaTerminados;

    private PoliticaScheduler planificador;
    private ControlProductorConsumidor controlBuffer;

    private int ciclosQuantum;
    private int latenciaRed;

    private PlanificadorDisco planificadorDisco;
    private long cicloDisco;
    private int siguienteIdSolicitud;

    public Computador(
            int id,
            int capacidadRAM,
            PoliticaScheduler planificador) {

        if (id <= 0 || planificador == null) {
            throw new IllegalArgumentException(
                    "Configuracion invalida");
        }

        this.id = id;
        this.cpu = new CPU();
        this.memoria = new MemoriaRAM(capacidadRAM);

        this.colaNuevos = new Cola<>();
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaTerminados = new Cola<>();

        this.planificador = planificador;
        this.controlBuffer = null;

        this.ciclosQuantum = 0;
        this.latenciaRed = 3;

        this.planificadorDisco = null;
        this.cicloDisco = 0;
        this.siguienteIdSolicitud = 0;
    }

    public void configurarPlanificadorDisco(
            PlanificadorDisco nuevo) {

        if (nuevo == null) {
            throw new IllegalArgumentException(
                    "Planificador de disco invalido");
        }

        if (cicloDisco != 0
                || !cpu.estaLibre()
                || !colaBloqueados.estaVacia()) {

            throw new IllegalStateException(
                    "Configure el disco antes de ejecutar");
        }

        this.planificadorDisco = nuevo;
    }

    public PlanificadorDisco getPlanificadorDisco() {
        return planificadorDisco;
    }

    public void configurarControlBuffer(
            ControlProductorConsumidor control) {

        if (control == null) {
            throw new IllegalArgumentException(
                    "Control de buffer invalido");
        }

        this.controlBuffer = control;
    }

    public void configurarLatenciaRed(int ciclos) {

        if (ciclos < 0) {
            throw new IllegalArgumentException(
                    "Latencia invalida");
        }

        this.latenciaRed = ciclos;
    }

    public void agregarProceso(Proceso proceso) {

        if (proceso == null) {
            throw new IllegalArgumentException(
                    "Proceso invalido");
        }

        if (proceso.getPcb().getComputadorId() != id) {
            throw new IllegalArgumentException(
                    "Proceso de otro computador");
        }

        if (proceso.getPcb().getEstado()
                != EstadoProceso.NUEVO) {

            throw new IllegalStateException(
                    "El proceso debe estar NUEVO");
        }

        colaNuevos.encolar(proceso);
    }

    private void admitirProcesos() {

        int cantidad = colaNuevos.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso = colaNuevos.desencolar();

            if (proceso.estaTerminado()) {
                registrarTerminado(proceso, false);
                continue;
            }

            if (memoria.asignarMemoria(
                    proceso.getPcb())) {

                proceso.getPcb().setEstado(
                        EstadoProceso.LISTO);

                colaListos.encolar(proceso);

            } else {
                colaNuevos.encolar(proceso);
            }
        }
    }

    private void despachar() {

        if (!cpu.estaLibre()
                || colaListos.estaVacia()) {
            return;
        }

        Proceso siguiente =
                planificador.seleccionar(colaListos);

        if (siguiente != null) {

            siguiente.configurarIOGestionadaPorDisco(
                    planificadorDisco != null
            );

            cpu.asignarProceso(siguiente);
            ciclosQuantum = 0;
        }
    }

    private void evaluarDesalojo() {

        Proceso actual = cpu.getProcesoActual();

        if (actual == null) {
            return;
        }

        if (planificador.debeDesalojar(
                actual,
                colaListos,
                ciclosQuantum)) {

            Proceso desalojado =
                    cpu.desalojarProceso();

            if (desalojado != null) {
                colaListos.encolar(desalojado);
            }

            ciclosQuantum = 0;
        }
    }

    private void registrarTerminado(
            Proceso proceso,
            boolean teniaMemoria) {

        if (proceso == null) {
            return;
        }

        if (controlBuffer != null
                && proceso.esProductorConsumidor()) {

            controlBuffer.cancelarOperacion(proceso);
        }

        if (teniaMemoria) {
            memoria.liberarMemoria(
                    proceso.getPcb());
        }

        colaTerminados.encolar(proceso);
    }

    private void vencerProceso(Proceso proceso) {

        if (proceso == null
                || proceso.estaTerminado()) {
            return;
        }

        proceso.vencerDeadline();
    }

    private void disminuirDeadline(Proceso proceso) {

        if (proceso == null
                || proceso.estaTerminado()) {
            return;
        }

        proceso.getPcb().disminuirDeadline();

        if (proceso.getPcb().getDeadline() <= 0) {
            vencerProceso(proceso);
        }
    }

    private void actualizarDeadlineCola(
            Cola<Proceso> cola,
            boolean tieneMemoria) {

        int cantidad = cola.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso = cola.desencolar();

            disminuirDeadline(proceso);

            if (proceso.estaTerminado()) {
                registrarTerminado(proceso, tieneMemoria);
            } else {
                cola.encolar(proceso);
            }
        }
    }

    private boolean tienePermisoParaDespertar(
            Proceso proceso) {

        if (controlBuffer == null) {
            return false;
        }

        BufferSimulado buffer =
                controlBuffer.getBuffer();

        MotivoBloqueo motivo =
                proceso.getPcb().getMotivoBloqueo();

        SemaforoSimulado semaforo = null;

        if (motivo == MotivoBloqueo.BUFFER_LLENO) {
            semaforo = buffer.getVacios();

        } else if (motivo == MotivoBloqueo.BUFFER_VACIO) {
            semaforo = buffer.getLlenos();

        } else if (motivo == MotivoBloqueo.MUTEX_OCUPADO) {
            semaforo = buffer.getMutex();
        }

        return semaforo != null
                && semaforo.tienePermisoTransferido(
                        proceso);
    }

    public void solicitarDisco(SolicitudDisco solicitud) {

        if (planificadorDisco == null) {
            throw new IllegalStateException(
                    "El disco no esta configurado");
        }

        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "Solicitud nula");
        }

        Proceso actual = cpu.getProcesoActual();

        if (actual == null
                || actual.getPcb().getEstado()
                != EstadoProceso.EJECUCION) {

            throw new IllegalStateException(
                    "Debe existir un proceso en ejecucion");
        }

        if (solicitud.getProcesoId()
                != actual.getPcb().getId()) {

            throw new IllegalArgumentException(
                    "La solicitud pertenece a otro proceso");
        }

        if (solicitud.getCicloLlegada() > cicloDisco) {
            throw new IllegalArgumentException(
                    "La solicitud no puede llegar en el futuro");
        }

        planificadorDisco.agregarSolicitud(solicitud);

        if (solicitud.getId() > siguienteIdSolicitud) {
            siguienteIdSolicitud = solicitud.getId();
        }

        actual.configurarIOGestionadaPorDisco(true);
        actual.iniciarIODisco();

        cpu.liberarCPU();
        colaBloqueados.encolar(actual);

        ciclosQuantum = 0;
    }

    private void solicitarIOAutomatica(
            Proceso proceso) {

        if (planificadorDisco == null
                || !proceso.debeSolicitarDisco()) {
            return;
        }

        int totalBloques =
                planificadorDisco.getTotalBloques();

        int destino = (int) (
                ((long) proceso.getPcb().getId()
                + proceso.getPcb().getProgramCounter())
                % totalBloques
        );

        if (siguienteIdSolicitud
                == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Se agotaron los IDs de solicitudes");
        }

        SolicitudDisco solicitud =
                new SolicitudDisco(
                        siguienteIdSolicitud + 1,
                        proceso.getPcb().getId(),
                        OperacionDisco.READ,
                        "/simulada/proceso-"
                                + proceso.getPcb().getId(),
                        destino,
                        1,
                        cicloDisco
                );

        solicitarDisco(solicitud);
    }

    private void avanzarDisco() {

        if (planificadorDisco == null) {
            return;
        }

        SolicitudDisco finalizada =
                planificadorDisco.avanzarCiclo(
                        cicloDisco
                );

        cicloDisco++;

        if (finalizada != null) {
            completarSolicitudDisco(finalizada);
        }
    }

    private void completarSolicitudDisco(
            SolicitudDisco solicitud) {

        int cantidad = colaBloqueados.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso =
                    colaBloqueados.desencolar();

            boolean corresponde =
                    proceso.getPcb().getId()
                    == solicitud.getProcesoId();

            if (corresponde
                    && !proceso.estaTerminado()
                    && proceso.tieneIODiscoPendiente()) {

                proceso.completarIODisco();
                colaListos.encolar(proceso);

            } else {
                colaBloqueados.encolar(proceso);
            }
        }
    }

    private void avanzarBloqueados() {

        int cantidad = colaBloqueados.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso =
                    colaBloqueados.desencolar();

            if (proceso.estaTerminado()) {
                registrarTerminado(proceso, true);
                continue;
            }

            MotivoBloqueo motivo =
                    proceso.getPcb().getMotivoBloqueo();

            boolean despertar = false;

            if (motivo == MotivoBloqueo.ENTRADA_SALIDA) {

                if (!proceso.tieneIODiscoPendiente()) {
                    proceso.avanzarIO();
                    despertar = proceso.ioFinalizada();
                }

            } else if (motivo
                    == MotivoBloqueo.LATENCIA_RED) {

                proceso.getPcb().avanzarLatencia();
                despertar = proceso.getPcb()
                        .latenciaFinalizada();

            } else if (motivo
                    == MotivoBloqueo.BUFFER_LLENO
                    || motivo == MotivoBloqueo.BUFFER_VACIO
                    || motivo == MotivoBloqueo.MUTEX_OCUPADO) {

                despertar =
                        tienePermisoParaDespertar(proceso);
            }

            if (despertar) {
                proceso.getPcb().desbloquear();
                colaListos.encolar(proceso);
            } else {
                colaBloqueados.encolar(proceso);
            }
        }
    }

    private boolean accesoRemoto(Proceso proceso) {

        if (controlBuffer == null) {
            return false;
        }

        return proceso.getPcb().getComputadorId()
                != controlBuffer.getBuffer()
                        .getComputadorAnfitrionId();
    }

    private void prepararOperacionBuffer(
            Proceso proceso) {

        if (controlBuffer == null) {
            throw new IllegalStateException(
                    "Buffer no configurado");
        }

        if (accesoRemoto(proceso)
                && latenciaRed > 0
                && !proceso.getPcb()
                        .isLatenciaAplicadaOperacion()) {

            proceso.getPcb().iniciarLatencia(
                    latenciaRed);

            return;
        }

        FaseOperacionBuffer faseAnterior =
                proceso.getPcb()
                        .getFaseOperacionBuffer();

        controlBuffer.ejecutarOperacion(proceso);

        if (faseAnterior
                == FaseOperacionBuffer.NOTIFICANDO_BUFFER
                && proceso.getPcb().getEstado()
                == EstadoProceso.EJECUCION) {

            proceso.completarOperacionBuffer();
        }
    }

    @Override
    public void ejecutarCiclo() {

        avanzarDisco();
        avanzarBloqueados();
        admitirProcesos();
        evaluarDesalojo();
        despachar();

        Proceso actual = cpu.getProcesoActual();

        if (actual != null
                && actual.esProductorConsumidor()) {

            cpu.ejecutarCicloSistema();
            prepararOperacionBuffer(actual);

        } else {
            cpu.ejecutarCiclo();
        }

        if (actual != null) {

            if (actual.estaTerminado()) {

                cpu.liberarCPU();
                registrarTerminado(actual, true);
                ciclosQuantum = 0;

            } else if (actual.getPcb().getEstado()
                    == EstadoProceso.BLOQUEADO) {

                cpu.liberarCPU();
                colaBloqueados.encolar(actual);
                ciclosQuantum = 0;

            } else if (actual == cpu.getProcesoActual()
                    && actual.debeSolicitarDisco()) {

                solicitarIOAutomatica(actual);

            } else {
                ciclosQuantum++;
            }
        }

        actualizarDeadlineCola(colaNuevos, false);
        actualizarDeadlineCola(colaListos, true);
        actualizarDeadlineCola(colaBloqueados, true);

        Proceso ejecutando = cpu.getProcesoActual();

        if (ejecutando != null) {

            disminuirDeadline(ejecutando);

            if (ejecutando.estaTerminado()) {

                cpu.liberarCPU();
                registrarTerminado(ejecutando, true);
                ciclosQuantum = 0;
            }
        }
    }

    public void cambiarPlanificador(
            PoliticaScheduler nuevoPlanificador) {

        if (nuevoPlanificador == null) {
            throw new IllegalArgumentException(
                    "Planificador invalido");
        }

        this.planificador = nuevoPlanificador;
        ciclosQuantum = 0;
    }

    public int getId() {
        return id;
    }

    public CPU getCpu() {
        return cpu;
    }

    public MemoriaRAM getMemoria() {
        return memoria;
    }

    public int getLatenciaRed() {
        return latenciaRed;
    }

    public int getProcesosNuevos() {
        return colaNuevos.getTamanio();
    }

    public int getProcesosListos() {
        return colaListos.getTamanio();
    }

    public int getProcesosBloqueados() {
        return colaBloqueados.getTamanio();
    }

    public int getProcesosTerminados() {
        return colaTerminados.getTamanio();
    }

    public long getCicloDisco() {
        return cicloDisco;
    }
}
