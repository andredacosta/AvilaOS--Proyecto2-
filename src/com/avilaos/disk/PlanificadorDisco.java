
package com.avilaos.disk;

import com.avilaos.structures.Cola;
import java.util.concurrent.Semaphore;

public class PlanificadorDisco {

    private enum Fase {
        INACTIVO,
        HACIA_EXTREMO,
        RETORNO_CIRCULAR,
        HACIA_DESTINO,
        TRANSFERENCIA
    }

    private final Disco disco;
    private final Cola<SolicitudDisco> cola;
    private final Semaphore mutex;

    private PoliticaPlanificacionDisco politica;
    private PoliticaPlanificacionDisco politicaActiva;

    private SolicitudDisco solicitudActiva;
    private Fase fase;

    private int velocidadCabezal;
    private int bloquesRestantes;
    private int direccionRecorrido;

    private long desplazamientoTotal;
    private long solicitudesAtendidas;
    private long sumaEsperas;
    private long solicitudesIniciadas;

    private long atendidasFIFO;
    private long atendidasSSTF;
    private long atendidasSCAN;
    private long atendidasCSCAN;

    private long ultimoCiclo;

    public PlanificadorDisco(
            Disco disco,
            PoliticaPlanificacionDisco politica,
            int velocidadCabezal) {

        if (disco == null || politica == null) {
            throw new IllegalArgumentException(
                    "Disco y politica son obligatorios"
            );
        }

        if (velocidadCabezal <= 0) {
            throw new IllegalArgumentException(
                    "La velocidad debe ser positiva"
            );
        }

        this.disco = disco;
        this.politica = politica;
        this.velocidadCabezal = velocidadCabezal;

        this.cola = new Cola<>();
        this.mutex = new Semaphore(1, true);

        this.fase = Fase.INACTIVO;
        this.ultimoCiclo = -1;
    }

    public void agregarSolicitud(SolicitudDisco solicitud) {

        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "Solicitud nula"
            );
        }

        int destino = solicitud.getBloqueDestino();

        if (destino < 0
                || destino >= disco.getTotalBloques()) {
            throw new IllegalArgumentException(
                    "Bloque fuera del disco"
            );
        }

        mutex.acquireUninterruptibly();

        try {
            if (solicitudActiva != null
                    && solicitudActiva.getId()
                    == solicitud.getId()) {

                throw new IllegalArgumentException(
                        "Solicitud duplicada"
                );
            }

            int cantidad = cola.getTamanio();

            for (int i = 0; i < cantidad; i++) {

                SolicitudDisco actual = cola.desencolar();
                cola.encolar(actual);

                if (actual.getId() == solicitud.getId()) {
                    throw new IllegalArgumentException(
                            "Solicitud duplicada"
                    );
                }
            }

            cola.encolar(solicitud);

        } finally {
            mutex.release();
        }
    }

    private void seleccionarSolicitud(long cicloActual) {

        if (cola.estaVacia()) {
            return;
        }

        Cola<SolicitudDisco> disponibles = new Cola<>();
        Cola<SolicitudDisco> futuras = new Cola<>();

        int cantidad = cola.getTamanio();

        for (int i = 0; i < cantidad; i++) {

            SolicitudDisco solicitud = cola.desencolar();

            if (solicitud.getCicloLlegada() <= cicloActual) {
                disponibles.encolar(solicitud);
            } else {
                futuras.encolar(solicitud);
            }
        }

        while (!futuras.estaVacia()) {
            cola.encolar(futuras.desencolar());
        }

        if (disponibles.estaVacia()) {
            return;
        }

        int posicionAnterior = disco.getPosicionCabezal();
        int direccionAnterior = disco.getDireccionCabezal();

        PoliticaPlanificacionDisco seleccionada = politica;

        SolicitudDisco siguiente = seleccionada.seleccionar(
                disponibles,
                posicionAnterior,
                disco.getTotalBloques(),
                direccionAnterior
        );

        if (siguiente == null) {
            while (!disponibles.estaVacia()) {
                cola.encolar(disponibles.desencolar());
            }
            return;
        }

        while (!disponibles.estaVacia()) {
            cola.encolar(disponibles.desencolar());
        }

        solicitudActiva = siguiente;
        politicaActiva = seleccionada;

        sumaEsperas +=
                cicloActual - siguiente.getCicloLlegada();

        solicitudesIniciadas++;

        bloquesRestantes = Math.max(
                1, siguiente.getCantidadBloques()
        );

        direccionRecorrido = direccionAnterior;

        int destino = siguiente.getBloqueDestino();

        boolean estaDetras =
                direccionAnterior == 1
                ? destino < posicionAnterior
                : destino > posicionAnterior;

        if (estaDetras
                && (seleccionada instanceof SCAN
                || seleccionada instanceof CSCAN)) {

            fase = Fase.HACIA_EXTREMO;

        } else {
            fase = Fase.HACIA_DESTINO;
        }
    }

    public SolicitudDisco avanzarCiclo(long cicloActual) {

        mutex.acquireUninterruptibly();

        try {
            if (cicloActual < 0
                    || cicloActual <= ultimoCiclo) {
                throw new IllegalArgumentException(
                        "Los ciclos deben avanzar en orden"
                );
            }

            ultimoCiclo = cicloActual;

            if (solicitudActiva == null) {
                seleccionarSolicitud(cicloActual);
            }

            if (solicitudActiva == null) {
                return null;
            }

            if (fase == Fase.HACIA_EXTREMO) {

                int extremo = direccionRecorrido == 1
                        ? disco.getTotalBloques() - 1
                        : 0;

                moverCabezal(extremo);

                if (disco.getPosicionCabezal() == extremo) {

                    if (politicaActiva instanceof CSCAN) {
                        fase = Fase.RETORNO_CIRCULAR;

                    } else {
                        direccionRecorrido = -direccionRecorrido;
                        disco.setDireccionCabezal(
                                direccionRecorrido
                        );
                        fase = Fase.HACIA_DESTINO;
                    }
                }

                return null;
            }

            if (fase == Fase.RETORNO_CIRCULAR) {

                int reinicio = direccionRecorrido == 1
                        ? 0
                        : disco.getTotalBloques() - 1;

                disco.setPosicionCabezal(reinicio);
                fase = Fase.HACIA_DESTINO;

                return null;
            }

            if (fase == Fase.HACIA_DESTINO) {

                int destino =
                        solicitudActiva.getBloqueDestino();

                if (disco.getPosicionCabezal() != destino) {

                    moverCabezal(destino);

                    if (disco.getPosicionCabezal() == destino) {
                        fase = Fase.TRANSFERENCIA;
                    }

                    return null;
                }

                fase = Fase.TRANSFERENCIA;
            }

            if (fase == Fase.TRANSFERENCIA) {

                bloquesRestantes--;

                if (bloquesRestantes == 0) {

                    SolicitudDisco finalizada =
                            solicitudActiva;

                    solicitudesAtendidas++;
                    contarPolitica(politicaActiva);

                    solicitudActiva = null;
                    politicaActiva = null;
                    fase = Fase.INACTIVO;

                    return finalizada;
                }
            }

            return null;

        } finally {
            mutex.release();
        }
    }

    private void moverCabezal(int destino) {

        int actual = disco.getPosicionCabezal();

        if (actual == destino) {
            return;
        }

        int distancia = Math.abs(destino - actual);
        int avance = Math.min(
                velocidadCabezal, distancia
        );

        int sentido = destino > actual ? 1 : -1;

        disco.setPosicionCabezal(
                actual + avance * sentido
        );

        desplazamientoTotal += avance;

        if (!(politicaActiva instanceof SCAN)
                && !(politicaActiva instanceof CSCAN)) {
            disco.setDireccionCabezal(sentido);
        }
    }

    public void cambiarPolitica(
            PoliticaPlanificacionDisco nueva) {

        if (nueva == null) {
            throw new IllegalArgumentException(
                    "Politica invalida"
            );
        }

        mutex.acquireUninterruptibly();

        try {
            politica = nueva;
        } finally {
            mutex.release();
        }
    }

    public String getNombrePolitica() {

        mutex.acquireUninterruptibly();

        try {
            return politica.getNombre();
        } finally {
            mutex.release();
        }
    }

    private void contarPolitica(
            PoliticaPlanificacionDisco utilizada) {

        if (utilizada instanceof FIFO) {
            atendidasFIFO++;
        } else if (utilizada instanceof SSTF) {
            atendidasSSTF++;
        } else if (utilizada instanceof SCAN) {
            atendidasSCAN++;
        } else if (utilizada instanceof CSCAN) {
            atendidasCSCAN++;
        }
    }

    public long getDesplazamientoTotal() {

        mutex.acquireUninterruptibly();

        try {
            return desplazamientoTotal;
        } finally {
            mutex.release();
        }
    }

    public double getTiempoPromedioEspera() {

        mutex.acquireUninterruptibly();

        try {
            if (solicitudesIniciadas == 0) {
                return 0.0;
            }

            return (double) sumaEsperas
                    / solicitudesIniciadas;

        } finally {
            mutex.release();
        }
    }

    public long getSolicitudesAtendidas() {

        mutex.acquireUninterruptibly();

        try {
            return solicitudesAtendidas;
        } finally {
            mutex.release();
        }
    }

    public long getAtendidasFIFO() {
        return consultarAtendidas(0);
    }

    public long getAtendidasSSTF() {
        return consultarAtendidas(1);
    }

    public long getAtendidasSCAN() {
        return consultarAtendidas(2);
    }

    public long getAtendidasCSCAN() {
        return consultarAtendidas(3);
    }

    private long consultarAtendidas(int tipo) {

        mutex.acquireUninterruptibly();

        try {
            return switch (tipo) {
                case 0 -> atendidasFIFO;
                case 1 -> atendidasSSTF;
                case 2 -> atendidasSCAN;
                case 3 -> atendidasCSCAN;
                default -> 0;
            };
        } finally {
            mutex.release();
        }
    }

    public int getCantidadPendientes() {

        mutex.acquireUninterruptibly();

        try {
            return cola.getTamanio();
        } finally {
            mutex.release();
        }
    }

    public SolicitudDisco getSolicitudActiva() {

        mutex.acquireUninterruptibly();

        try {
            return solicitudActiva;
        } finally {
            mutex.release();
        }
    }

    public int getPosicionCabezal() {

        mutex.acquireUninterruptibly();

        try {
            return disco.getPosicionCabezal();
        } finally {
            mutex.release();
        }
    }

    public int getTotalBloques() {

        mutex.acquireUninterruptibly();

        try {
            return disco.getTotalBloques();
        } finally {
            mutex.release();
        }
    }

    public boolean estaInactivo() {

        mutex.acquireUninterruptibly();

        try {
            return solicitudActiva == null
                    && cola.estaVacia();
        } finally {
            mutex.release();
        }
    }
}
