
package com.avilaos.disk;

public class PruebaPlanificadorDisco {

    public static void main(String[] args) {

        Disco disco = new Disco(200, 50, 50);

        PlanificadorDisco planificador =
                new PlanificadorDisco(
                        disco,
                        new FIFO(),
                        20
                );

        int[] destinos = {95, 180, 34};

        for (int i = 0; i < destinos.length; i++) {

            SolicitudDisco solicitud = new SolicitudDisco(
                    i + 1,
                    i + 1,
                    OperacionDisco.READ,
                    "/archivo" + (i + 1),
                    destinos[i],
                    1,
                    0
            );

            planificador.agregarSolicitud(solicitud);
        }

        int[] esperado = {95, 180, 34};
        int terminadas = 0;

        for (int ciclo = 0;
                ciclo < 1000 && !planificador.estaInactivo();
                ciclo++) {

            SolicitudDisco finalizada =
                    planificador.avanzarCiclo(ciclo);

            if (finalizada != null) {

                int bloque = finalizada.getBloqueDestino();

                if (bloque != esperado[terminadas]) {
                    throw new AssertionError(
                            "Orden de atencion incorrecto"
                    );
                }

                System.out.println(
                        "Ciclo " + ciclo
                        + ": solicitud del bloque "
                        + bloque + " finalizada"
                );

                terminadas++;
            }
        }

        if (terminadas != esperado.length) {
            throw new AssertionError(
                    "No se atendieron todas las solicitudes"
            );
        }

        if (planificador.getSolicitudesAtendidas() != 3) {
            throw new AssertionError(
                    "Conteo de solicitudes incorrecto"
            );
        }

        if (planificador.getDesplazamientoTotal() != 316) {
            throw new AssertionError(
                    "Desplazamiento incorrecto"
            );
        }

        System.out.println();
        System.out.println("Solicitudes atendidas: "
                + planificador.getSolicitudesAtendidas());

        System.out.println("Desplazamiento total: "
                + planificador.getDesplazamientoTotal());

        System.out.println("Espera promedio: "
                + planificador.getTiempoPromedioEspera());

        System.out.println("PRUEBA PLANIFICADOR: PASS");
    }
}
