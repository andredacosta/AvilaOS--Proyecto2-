
package com.avilaos.core;

import com.avilaos.disk.Disco;
import com.avilaos.disk.GestorSwap;
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
    private final Cola<Proceso> colaSuspendidos;
    private final Cola<Proceso> colaTerminados;

    private PoliticaScheduler planificador;
    private ControlProductorConsumidor controlBuffer;

    private PlanificadorDisco planificadorDisco;
    private GestorSwap gestorSwap;

    private int ciclosQuantum;
    private int latenciaRed;
    private int siguienteIdSolicitud;
    private long cicloDisco;

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
        this.colaSuspendidos = new Cola<>();
        this.colaTerminados = new Cola<>();

        this.planificador = planificador;
        this.latenciaRed = 3;
        this.ciclosQuantum = 0;

        this.cicloDisco = 0;
        this.siguienteIdSolicitud = 0;
    }

    public void configurarPlanificadorDisco(
            PlanificadorDisco nuevo) {

        if (nuevo == null) {
            throw new IllegalArgumentException(
                    "Planificador invalido");
        }

        if (cicloDisco != 0
                || !cpu.estaLibre()
                || !colaBloqueados.estaVacia()) {
            throw new IllegalStateException(
                    "Configure el disco antes de ejecutar");
        }

        planificadorDisco = nuevo;
    }

    public void configurarGestorSwap(
            GestorSwap nuevo) {

        if (nuevo == null || planificadorDisco == null) {
            throw new IllegalArgumentException(
                    "Configure primero el disco");
        }

        if (cicloDisco != 0) {
            throw new IllegalStateException(
                    "Configure swap antes de ejecutar");
        }

        gestorSwap = nuevo;
    }

    public void configurarControlBuffer(
            ControlProductorConsumidor control) {

        if (control == null) {
            throw new IllegalArgumentException(
                    "Control invalido");
        }

        controlBuffer = control;
    }

    public void configurarLatenciaRed(int ciclos) {

        if (ciclos < 0) {
            throw new IllegalArgumentException(
                    "Latencia invalida");
        }

        latenciaRed = ciclos;
    }

    public void agregarProceso(Proceso proceso) {

        if (proceso == null
                || proceso.getPcb().getComputadorId() != id) {
            throw new IllegalArgumentException(
                    "Proceso invalido");
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
                    planificadorDisco != null);

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
                actual, colaListos, ciclosQuantum)) {

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

        boolean memoriaLiberadaPorSwap = false;

        if (gestorSwap != null) {
            memoriaLiberadaPorSwap =
                    gestorSwap.cancelarProceso(
                            proceso.getPcb());
        }

        if (teniaMemoria && !memoriaLiberadaPorSwap) {
            memoria.liberarMemoria(proceso.getPcb());
        }

        colaTerminados.encolar(proceso);
    }

    private void vencerProceso(Proceso proceso) {

        if (proceso != null
                && !proceso.estaTerminado()) {
            proceso.vencerDeadline();
        }
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

    private void actualizarDeadlineSuspendidos() {

        int cantidad = colaSuspendidos.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso =
                    colaSuspendidos.desencolar();

            disminuirDeadline(proceso);

            if (proceso.estaTerminado()) {
                registrarTerminado(proceso, false);
            } else {
                colaSuspendidos.encolar(proceso);
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

    private int siguienteIdDisponible() {

        if (siguienteIdSolicitud == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Se agotaron los IDs de solicitudes");
        }

        return siguienteIdSolicitud + 1;
    }

    public void solicitarDisco(SolicitudDisco solicitud) {

        if (planificadorDisco == null
                || solicitud == null) {
            throw new IllegalArgumentException(
                    "Disco o solicitud invalida");
        }

        Proceso actual = cpu.getProcesoActual();

        if (actual == null
                || actual.getPcb().getEstado()
                != EstadoProceso.EJECUCION) {
            throw new IllegalStateException(
                    "No hay proceso ejecutando");
        }

        if (solicitud.getProcesoId()
                != actual.getPcb().getId()) {
            throw new IllegalArgumentException(
                    "Solicitud de otro proceso");
        }

        if (solicitud.getCicloLlegada() > cicloDisco) {
            throw new IllegalArgumentException(
                    "Ciclo de llegada invalido");
        }

        planificadorDisco.agregarSolicitud(solicitud);

        siguienteIdSolicitud = Math.max(
                siguienteIdSolicitud,
                solicitud.getId());

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

        int total = planificadorDisco.getTotalBloques();

        int destino = (int) (
                ((long) proceso.getPcb().getId()
                + proceso.getPcb().getProgramCounter())
                % total);

        SolicitudDisco solicitud = new SolicitudDisco(
                siguienteIdDisponible(),
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

    public boolean solicitarSuspension(int procesoId) {

        if (gestorSwap == null) {
            throw new IllegalStateException(
                    "Swap no configurado");
        }

        int cantidad = colaBloqueados.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso =
                    colaBloqueados.desencolar();

            colaBloqueados.encolar(proceso);

            if (proceso.getPcb().getId() != procesoId
                    || proceso.estaTerminado()
                    || !proceso.tieneIODiscoPendiente()
                    || gestorSwap.tieneSolicitudPendiente(
                            procesoId)
                    || gestorSwap.estaEnSwap(procesoId)) {
                continue;
            }

            SolicitudDisco solicitud =
                    gestorSwap.solicitarSuspension(
                            proceso.getPcb(),
                            siguienteIdDisponible(),
                            cicloDisco);

            if (solicitud == null) {
                return false;
            }

            siguienteIdSolicitud = solicitud.getId();
            return true;
        }

        return false;
    }

    private void suspenderPorPresionDeRAM() {

        if (gestorSwap == null
                || colaNuevos.estaVacia()) {
            return;
        }

        int cantidad = colaNuevos.getTamanio();
        boolean faltaRAM = false;

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso = colaNuevos.desencolar();
            colaNuevos.encolar(proceso);

            if (!proceso.estaTerminado()
                    && !memoria.hayEspacio(
                            proceso.getPcb()
                                    .getMemoriaAsignada())) {
                faltaRAM = true;
            }
        }

        if (!faltaRAM) {
            return;
        }

        cantidad = colaBloqueados.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso =
                    colaBloqueados.desencolar();
            colaBloqueados.encolar(proceso);

            if (!proceso.estaTerminado()
                    && proceso.tieneIODiscoPendiente()
                    && !gestorSwap.tieneSolicitudPendiente(
                            proceso.getPcb().getId())
                    && !gestorSwap.estaEnSwap(
                            proceso.getPcb().getId())) {

                if (solicitarSuspension(
                        proceso.getPcb().getId())) {
                    return;
                }
            }
        }
    }

    private void intentarReactivaciones() {

        if (gestorSwap == null) {
            return;
        }

        int cantidad = colaSuspendidos.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso =
                    colaSuspendidos.desencolar();
            colaSuspendidos.encolar(proceso);

            if (proceso.getPcb().getEstado()
                    != EstadoProceso.LISTO_SUSPENDIDO
                    || gestorSwap.tieneSolicitudPendiente(
                            proceso.getPcb().getId())) {
                continue;
            }

            SolicitudDisco solicitud =
                    gestorSwap.solicitarReactivacion(
                            proceso.getPcb(),
                            siguienteIdDisponible(),
                            cicloDisco);

            if (solicitud != null) {
                siguienteIdSolicitud = solicitud.getId();
            }
        }
    }

    private Proceso extraerPorId(
            Cola<Proceso> cola, int procesoId) {

        Proceso encontrado = null;
        int cantidad = cola.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            Proceso proceso = cola.desencolar();

            if (encontrado == null
                    && proceso.getPcb().getId()
                    == procesoId) {
                encontrado = proceso;
            } else {
                cola.encolar(proceso);
            }
        }

        return encontrado;
    }

    private void completarSolicitudDisco(
            SolicitudDisco solicitud) {

        if (solicitud.getOperacion() == OperacionDisco.SUSPEND
                || solicitud.getOperacion()
                == OperacionDisco.REACTIVATE) {

            if (gestorSwap == null
                    || !gestorSwap.completarSolicitud(
                            solicitud)) {
                return;
            }

            int procesoId = solicitud.getProcesoId();

            if (solicitud.getOperacion()
                    == OperacionDisco.SUSPEND) {

                Proceso proceso =
                        extraerPorId(colaBloqueados, procesoId);

                if (proceso != null
                        && !proceso.estaTerminado()) {

                    proceso.getPcb().confirmarSuspension();
                    colaSuspendidos.encolar(proceso);
                }

            } else {

                Proceso proceso =
                        extraerPorId(colaSuspendidos, procesoId);

                if (proceso != null
                        && !proceso.estaTerminado()
                        && proceso.getPcb().getEstado()
                        == EstadoProceso.LISTO) {
                    colaListos.encolar(proceso);
                }
            }

            return;
        }

        int procesoId = solicitud.getProcesoId();

        Proceso bloqueado =
                extraerPorId(colaBloqueados, procesoId);

        if (bloqueado != null) {

            if (!bloqueado.estaTerminado()
                    && bloqueado.tieneIODiscoPendiente()) {

                if (gestorSwap != null
                        && gestorSwap.tieneSolicitudPendiente(
                                procesoId)) {

                    bloqueado.getPcb()
                            .completarIODiscoEnSuspension();
                    colaBloqueados.encolar(bloqueado);

                } else {

                    bloqueado.completarIODisco();
                    colaListos.encolar(bloqueado);
                }

            } else {
                colaBloqueados.encolar(bloqueado);
            }

            return;
        }

        Proceso suspendido =
                extraerPorId(colaSuspendidos, procesoId);

        if (suspendido != null) {

            if (!suspendido.estaTerminado()
                    && suspendido.tieneIODiscoPendiente()) {

                suspendido.getPcb()
                        .completarIODiscoEnSuspension();
            }

            colaSuspendidos.encolar(suspendido);
        }
    }

    private void avanzarDisco() {

        if (planificadorDisco == null) {
            return;
        }

        SolicitudDisco finalizada =
                planificadorDisco.avanzarCiclo(cicloDisco);

        cicloDisco++;

        if (finalizada != null) {
            completarSolicitudDisco(finalizada);
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

                boolean swapPendiente =
                        gestorSwap != null
                        && gestorSwap.tieneSolicitudPendiente(
                                proceso.getPcb().getId());

                if (!swapPendiente
                        && !proceso.tieneIODiscoPendiente()) {

                    proceso.avanzarIO();
                    despertar = proceso.ioFinalizada();
                }

            } else if (motivo == MotivoBloqueo.LATENCIA_RED) {

                proceso.getPcb().avanzarLatencia();
                despertar = proceso.getPcb()
                        .latenciaFinalizada();

            } else if (motivo == MotivoBloqueo.BUFFER_LLENO
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

            proceso.getPcb().iniciarLatencia(latenciaRed);
            return;
        }

        FaseOperacionBuffer faseAnterior =
                proceso.getPcb().getFaseOperacionBuffer();

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
        suspenderPorPresionDeRAM();
        intentarReactivaciones();
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
        actualizarDeadlineSuspendidos();

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

        planificador = nuevoPlanificador;
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

    public PlanificadorDisco getPlanificadorDisco() {
        return planificadorDisco;
    }

    public GestorSwap getGestorSwap() {
        return gestorSwap;
    }

    public int getLatenciaRed() {
        return latenciaRed;
    }

    public long getCicloDisco() {
        return cicloDisco;
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

    public int getProcesosSuspendidos() {
        return colaSuspendidos.getTamanio();
    }

    public int getProcesosTerminados() {
        return colaTerminados.getTamanio();
    }
}

