
package com.avilaos.disk;

import com.avilaos.core.Computador;
import com.avilaos.model.EstadoProceso;
import com.avilaos.model.PCB;
import com.avilaos.model.Proceso;
import com.avilaos.model.TipoProceso;
import com.avilaos.scheduler.FCFS;

public class PruebaIntegracionSwap {

    public static void main(String[] args) {

        probarEscenario("FIFO", new FIFO(), false);
        probarEscenario("SSTF", new SSTF(), true);

        System.out.println();
        System.out.println("PRUEBA INTEGRACION SWAP: PASS");
    }

    private static void probarEscenario(
            String nombre,
            PoliticaPlanificacionDisco politica,
            boolean suspensionPrimero) {

        Disco disco = new Disco(100, 30, 70);

        PlanificadorDisco planificador =
                new PlanificadorDisco(disco, politica, 100);

        Computador computador =
                new Computador(1, 50, new FCFS());

        GestorSwap gestor =
                new GestorSwap(
                        disco,
                        computador.getMemoria(),
                        planificador
                );

        computador.configurarPlanificadorDisco(
                planificador
        );

        computador.configurarGestorSwap(gestor);

        PCB pcb = new PCB(
                1,
                "Proceso swap",
                1,
                TipoProceso.IO_BOUND,
                1,
                10,
                500,
                8
        );

        Proceso proceso = new Proceso(pcb);
        computador.agregarProceso(proceso);

        computador.ejecutarCiclo();
        computador.ejecutarCiclo();

        verificar(
                pcb.getEstado() == EstadoProceso.BLOQUEADO
                        && pcb.isIODiscoPendiente(),
                nombre + ": proceso bloqueado por READ"
        );

        verificar(
                computador.getMemoria().getMemoriaUsada() == 10,
                nombre + ": RAM asignada inicialmente"
        );

        verificar(
                computador.solicitarSuspension(pcb.getId()),
                nombre + ": solicitud SUSPEND registrada"
        );

        boolean readAntesDeSuspend = false;
        boolean suspendAntesDeRead = false;
        boolean observoSuspension = false;
        boolean observoReactivacion = false;
        boolean finalizado = false;

        for (int ciclo = 0; ciclo < 300; ciclo++) {

            computador.ejecutarCiclo();

            EstadoProceso estado = pcb.getEstado();

            if (pcb.isIODiscoCompletadaDuranteSuspension()) {
                readAntesDeSuspend = true;
            }

            if (estado == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
                suspendAntesDeRead = true;
            }

            if (estado == EstadoProceso.BLOQUEADO_SUSPENDIDO
                    || estado == EstadoProceso.LISTO_SUSPENDIDO) {
                observoSuspension = true;

                verificar(
                        computador.getMemoria()
                                .getMemoriaUsada() == 0,
                        nombre + ": RAM liberada en swap"
                );
            }

            if (observoSuspension
                    && computador.getMemoria()
                            .getMemoriaUsada() == 10
                    && estado == EstadoProceso.LISTO_SUSPENDIDO) {
                observoReactivacion = true;
            }

            if (planificador.getSolicitudesAtendidas() >= 3
                    && !pcb.isIODiscoPendiente()
                    && !gestor.estaEnSwap(pcb.getId())
                    && (estado == EstadoProceso.LISTO
                    || estado == EstadoProceso.EJECUCION)) {

                finalizado = true;
                break;
            }
        }

        verificar(
                observoSuspension,
                nombre + ": proceso suspendido"
        );

        verificar(
                observoReactivacion,
                nombre + ": reactivacion iniciada"
        );

        verificar(
                finalizado,
                nombre + ": proceso reactivado"
        );

        verificar(
                computador.getMemoria().getMemoriaUsada() == 10,
                nombre + ": RAM recuperada"
        );

        verificar(
                disco.contarBloquesLibresSwap() == 30,
                nombre + ": espacio swap liberado"
        );

        verificar(
                planificador.getSolicitudesAtendidas() == 3,
                nombre + ": READ, SUSPEND y REACTIVATE atendidas"
        );

        if (suspensionPrimero) {
            verificar(
                    suspendAntesDeRead,
                    nombre + ": SUSPEND termino antes de READ"
            );
        } else {
            verificar(
                    readAntesDeSuspend,
                    nombre + ": READ termino antes de SUSPEND"
            );
        }
    }

    private static void verificar(
            boolean condicion,
            String descripcion) {

        if (!condicion) {
            throw new AssertionError(
                    "FAIL: " + descripcion
            );
        }

        System.out.println("PASS: " + descripcion);
    }
}
