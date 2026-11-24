
package com.avilaos.disk;

import com.avilaos.core.MemoriaRAM;
import com.avilaos.model.EstadoProceso;
import com.avilaos.model.PCB;
import java.util.concurrent.Semaphore;

public class GestorSwap {

    private enum Fase {
        SUSPENDIENDO,
        SUSPENDIDO,
        REACTIVANDO,
        CANCELADO_PENDIENTE
    }

    private static class Registro {

        PCB pcb;
        int primerBloque;
        int solicitudId;
        Fase fase;
        Registro siguiente;

        Registro(PCB pcb, int primerBloque,
                 int solicitudId, Fase fase) {

            this.pcb = pcb;
            this.primerBloque = primerBloque;
            this.solicitudId = solicitudId;
            this.fase = fase;
        }
    }

    private final Disco disco;
    private final MemoriaRAM memoria;
    private final PlanificadorDisco planificador;
    private final Semaphore mutex;

    private Registro registros;

    public GestorSwap(
            Disco disco,
            MemoriaRAM memoria,
            PlanificadorDisco planificador) {

        if (disco == null || memoria == null
                || planificador == null) {
            throw new IllegalArgumentException(
                    "Componentes invalidos");
        }

        this.disco = disco;
        this.memoria = memoria;
        this.planificador = planificador;
        this.mutex = new Semaphore(1, true);
    }

    private Registro buscar(int procesoId) {

        Registro actual = registros;

        while (actual != null) {

            if (actual.pcb.getId() == procesoId) {
                return actual;
            }

            actual = actual.siguiente;
        }

        return null;
    }

    private void eliminar(int procesoId) {

        Registro anterior = null;
        Registro actual = registros;

        while (actual != null) {

            if (actual.pcb.getId() == procesoId) {

                if (anterior == null) {
                    registros = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }

                return;
            }

            anterior = actual;
            actual = actual.siguiente;
        }
    }

    public SolicitudDisco solicitarSuspension(
            PCB pcb, int solicitudId, long ciclo) {

        if (pcb == null || solicitudId <= 0
                || ciclo < 0) {
            throw new IllegalArgumentException(
                    "Solicitud invalida");
        }

        mutex.acquireUninterruptibly();

        try {
            if (pcb.getEstado() != EstadoProceso.BLOQUEADO) {
                throw new IllegalStateException(
                        "El proceso debe estar BLOQUEADO");
            }

            if (buscar(pcb.getId()) != null) {
                throw new IllegalStateException(
                        "El proceso ya tiene swap");
            }

            int cantidad = pcb.getMemoriaAsignada();

            int primerBloque = disco.asignarSwap(
                    String.valueOf(pcb.getId()),
                    cantidad
            );

            if (primerBloque == -1) {
                return null;
            }

            SolicitudDisco solicitud = new SolicitudDisco(
                    solicitudId,
                    pcb.getId(),
                    OperacionDisco.SUSPEND,
                    null,
                    primerBloque,
                    cantidad,
                    ciclo
            );

            try {
                planificador.agregarSolicitud(solicitud);

            } catch (RuntimeException ex) {
                disco.liberarCadena(primerBloque);
                throw ex;
            }

            Registro registro = new Registro(
                    pcb,
                    primerBloque,
                    solicitudId,
                    Fase.SUSPENDIENDO
            );

            registro.siguiente = registros;
            registros = registro;

            return solicitud;

        } finally {
            mutex.release();
        }
    }

    public SolicitudDisco solicitarReactivacion(
            PCB pcb, int solicitudId, long ciclo) {

        if (pcb == null || solicitudId <= 0
                || ciclo < 0) {
            throw new IllegalArgumentException(
                    "Solicitud invalida");
        }

        mutex.acquireUninterruptibly();

        try {
            Registro registro = buscar(pcb.getId());

            if (registro == null
                    || registro.pcb != pcb
                    || registro.fase != Fase.SUSPENDIDO
                    || pcb.getEstado()
                    != EstadoProceso.LISTO_SUSPENDIDO) {
                throw new IllegalStateException(
                        "El proceso no puede reactivarse");
            }

            if (!memoria.asignarMemoria(pcb)) {
                return null;
            }

            SolicitudDisco solicitud = new SolicitudDisco(
                    solicitudId,
                    pcb.getId(),
                    OperacionDisco.REACTIVATE,
                    null,
                    registro.primerBloque,
                    pcb.getMemoriaAsignada(),
                    ciclo
            );

            try {
                planificador.agregarSolicitud(solicitud);

            } catch (RuntimeException ex) {
                memoria.liberarMemoria(pcb);
                throw ex;
            }

            registro.solicitudId = solicitudId;
            registro.fase = Fase.REACTIVANDO;

            return solicitud;

        } finally {
            mutex.release();
        }
    }

    public boolean completarSolicitud(
            SolicitudDisco solicitud) {

        if (solicitud == null) {
            return false;
        }

        mutex.acquireUninterruptibly();

        try {
            Registro registro =
                    buscar(solicitud.getProcesoId());

            if (registro == null
                    || registro.solicitudId
                    != solicitud.getId()) {
                return false;
            }

            if (registro.fase
                    == Fase.CANCELADO_PENDIENTE) {

                disco.liberarCadena(
                        registro.primerBloque
                );

                eliminar(registro.pcb.getId());
                return true;
            }

            PCB pcb = registro.pcb;

            if (solicitud.getOperacion()
                    == OperacionDisco.SUSPEND
                    && registro.fase == Fase.SUSPENDIENDO) {

                if (pcb.getEstado()
                        != EstadoProceso.BLOQUEADO) {
                    throw new IllegalStateException(
                            "Estado invalido para suspension");
                }

                memoria.liberarMemoria(pcb);

                pcb.setEstado(
                        EstadoProceso.BLOQUEADO_SUSPENDIDO
                );

                registro.fase = Fase.SUSPENDIDO;
                return true;
            }

            if (solicitud.getOperacion()
                    == OperacionDisco.REACTIVATE
                    && registro.fase == Fase.REACTIVANDO) {

                if (pcb.getEstado()
                        != EstadoProceso.LISTO_SUSPENDIDO) {
                    throw new IllegalStateException(
                            "Estado invalido para reactivacion");
                }

                disco.liberarCadena(
                        registro.primerBloque
                );

                pcb.setEstado(EstadoProceso.LISTO);
                eliminar(pcb.getId());

                return true;
            }

            return false;

        } finally {
            mutex.release();
        }
    }

    /**
     * Cancela el swap de un proceso terminado.
     *
     * Devuelve true si este metodo libero
     * una reserva de RAM por reactivacion.
     */
    public boolean cancelarProceso(PCB pcb) {

        if (pcb == null) {
            throw new IllegalArgumentException(
                    "PCB nulo");
        }

        mutex.acquireUninterruptibly();

        try {
            Registro registro = buscar(pcb.getId());

            if (registro == null) {
                return false;
            }

            if (registro.pcb != pcb) {
                throw new IllegalStateException(
                        "PCB diferente para el mismo ID");
            }

            if (registro.fase == Fase.SUSPENDIDO) {

                disco.liberarCadena(
                        registro.primerBloque
                );

                eliminar(pcb.getId());
                return false;
            }

            if (registro.fase == Fase.REACTIVANDO) {

                memoria.liberarMemoria(pcb);

                registro.fase =
                        Fase.CANCELADO_PENDIENTE;

                return true;
            }

            if (registro.fase == Fase.SUSPENDIENDO) {

                registro.fase =
                        Fase.CANCELADO_PENDIENTE;

                return false;
            }

            return false;

        } finally {
            mutex.release();
        }
    }

    public boolean tieneSolicitudPendiente(
            int procesoId) {

        mutex.acquireUninterruptibly();

        try {
            Registro registro = buscar(procesoId);

            return registro != null
                    && (registro.fase == Fase.SUSPENDIENDO
                    || registro.fase == Fase.REACTIVANDO
                    || registro.fase
                    == Fase.CANCELADO_PENDIENTE);

        } finally {
            mutex.release();
        }
    }

    public boolean estaEnSwap(int procesoId) {

        mutex.acquireUninterruptibly();

        try {
            Registro registro = buscar(procesoId);

            return registro != null
                    && registro.fase == Fase.SUSPENDIDO;

        } finally {
            mutex.release();
        }
    }

    public int getProcesosEnSwap() {

        mutex.acquireUninterruptibly();

        try {
            int cantidad = 0;
            Registro actual = registros;

            while (actual != null) {

                if (actual.fase == Fase.SUSPENDIDO) {
                    cantidad++;
                }

                actual = actual.siguiente;
            }

            return cantidad;

        } finally {
            mutex.release();
        }
    }

    public int getBloquesSwapLibres() {
        return disco.contarBloquesLibresSwap();
    }
}
