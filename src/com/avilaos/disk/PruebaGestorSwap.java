
package com.avilaos.disk;

import com.avilaos.core.MemoriaRAM;
import com.avilaos.model.EstadoProceso;
import com.avilaos.model.PCB;
import com.avilaos.model.TipoProceso;

public class PruebaGestorSwap {

    public static void main(String[] args) {

        Disco disco = new Disco(100, 30, 20);
        MemoriaRAM memoria = new MemoriaRAM(50);

        PlanificadorDisco planificador =
                new PlanificadorDisco(
                        disco, new FIFO(), 10);

        GestorSwap gestor =
                new GestorSwap(
                        disco, memoria, planificador);

        PCB pcb = new PCB(
                1, "Proceso prueba", 1,
                TipoProceso.IO_BOUND,
                1, 20, 100, 10);

        verificar(
                memoria.asignarMemoria(pcb),
                "Reserva inicial de RAM");

        verificar(
                memoria.getMemoriaUsada() == 20,
                "RAM ocupada inicialmente");

        pcb.setEstado(EstadoProceso.BLOQUEADO);

        SolicitudDisco suspension =
                gestor.solicitarSuspension(
                        pcb, 1, 0);

        verificar(
                suspension != null,
                "Solicitud SUSPEND creada");

        verificar(
                memoria.getMemoriaUsada() == 20,
                "RAM retenida durante SUSPEND");

        SolicitudDisco finalizada =
                esperarSolicitud(planificador, 0);

        verificar(
                finalizada != null
                        && finalizada.getId() == 1,
                "SUSPEND atendida por el disco");

        verificar(
                gestor.completarSolicitud(finalizada),
                "Suspension completada");

        verificar(
                pcb.getEstado()
                        == EstadoProceso.BLOQUEADO_SUSPENDIDO,
                "Estado BLOQUEADO_SUSPENDIDO");

        verificar(
                memoria.getMemoriaUsada() == 0,
                "RAM liberada");

        verificar(
                gestor.estaEnSwap(pcb.getId()),
                "Proceso registrado en swap");

        pcb.setEstado(
                EstadoProceso.LISTO_SUSPENDIDO);

        SolicitudDisco reactivacion =
                gestor.solicitarReactivacion(
                        pcb, 2, 100);

        verificar(
                reactivacion != null,
                "Solicitud REACTIVATE creada");

        verificar(
                memoria.getMemoriaUsada() == 20,
                "RAM reservada para reactivacion");

        finalizada =
                esperarSolicitud(planificador, 100);

        verificar(
                finalizada != null
                        && finalizada.getId() == 2,
                "REACTIVATE atendida por el disco");

        verificar(
                gestor.completarSolicitud(finalizada),
                "Reactivacion completada");

        verificar(
                pcb.getEstado() == EstadoProceso.LISTO,
                "Proceso vuelve a LISTO");

        verificar(
                !gestor.estaEnSwap(pcb.getId()),
                "Proceso retirado de swap");

        verificar(
                disco.contarBloquesLibresSwap() == 30,
                "Bloques de swap liberados");

        System.out.println();
        System.out.println("PRUEBA GESTOR SWAP: PASS");
    }

    private static SolicitudDisco esperarSolicitud(
            PlanificadorDisco planificador,
            long cicloInicial) {

        for (long ciclo = cicloInicial;
                ciclo < cicloInicial + 100;
                ciclo++) {

            SolicitudDisco finalizada =
                    planificador.avanzarCiclo(ciclo);

            if (finalizada != null) {
                return finalizada;
            }
        }

        return null;
    }

    private static void verificar(
            boolean condicion,
            String descripcion) {

        if (!condicion) {
            throw new AssertionError(
                    "FAIL: " + descripcion);
        }

        System.out.println(
                "PASS: " + descripcion);
    }
}

