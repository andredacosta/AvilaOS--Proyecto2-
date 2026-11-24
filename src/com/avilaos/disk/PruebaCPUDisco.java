
package com.avilaos.disk;

import com.avilaos.core.Computador;
import com.avilaos.model.EstadoProceso;
import com.avilaos.model.PCB;
import com.avilaos.model.Proceso;
import com.avilaos.model.TipoProceso;
import com.avilaos.scheduler.FCFS;

public class PruebaCPUDisco {

    public static void main(String[] args) {

        Disco disco = new Disco(100, 20, 50);

        PlanificadorDisco planificadorDisco =
                new PlanificadorDisco(disco, new FIFO(), 5);

        Computador computador =
                new Computador(1, 100, new FCFS());

        computador.configurarPlanificadorDisco(
                planificadorDisco
        );

        PCB pcb = new PCB(
                1,
                "Proceso IO",
                1,
                TipoProceso.IO_BOUND,
                1,
                10,
                100,
                8
        );

        Proceso proceso = new Proceso(pcb);
        computador.agregarProceso(proceso);

        boolean estuvoBloqueado = false;
        boolean regresoAListo = false;

        for (int ciclo = 0; ciclo < 100; ciclo++) {

            computador.ejecutarCiclo();

            EstadoProceso estado = pcb.getEstado();

            System.out.println(
                    "Ciclo " + ciclo
                    + " | Estado: " + estado
                    + " | E/S pendientes: "
                    + planificadorDisco.getCantidadPendientes()
                    + " | E/S atendidas: "
                    + planificadorDisco.getSolicitudesAtendidas()
            );

            if (estado == EstadoProceso.BLOQUEADO
                    && pcb.isIODiscoPendiente()) {
                estuvoBloqueado = true;
            }

            if (estuvoBloqueado
                    && !pcb.isIODiscoPendiente()
                    && planificadorDisco
                            .getSolicitudesAtendidas() > 0) {

                if (estado == EstadoProceso.LISTO
                        || estado == EstadoProceso.EJECUCION) {

                    regresoAListo = true;
                }
            }

            if (regresoAListo) {
                break;
            }
        }

        if (!estuvoBloqueado) {
            throw new AssertionError(
                    "El proceso nunca se bloqueo por disco"
            );
        }

        if (!regresoAListo) {
            throw new AssertionError(
                    "El proceso no regreso a LISTO"
            );
        }

        System.out.println();
        System.out.println("PRUEBA CPU + PCB + DISCO: PASS");
    }
}
