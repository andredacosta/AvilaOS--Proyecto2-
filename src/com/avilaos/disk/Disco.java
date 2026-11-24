
package com.avilaos.disk;

import java.util.concurrent.Semaphore;

/**
 * Disco simulado de AvilaOS.
 *
 * Administra bloques, asignacion encadenada,
 * espacio reservado para swap y posicion del cabezal.
 *
 * No utiliza colecciones de java.util.
 */
public class Disco {

    private final BloqueDisco[] bloques;
    private final int totalBloques;
    private final int bloquesSwap;

    private int posicionCabezal;
    private int direccionCabezal;

    private final Semaphore mutex = new Semaphore(1, true);

    public Disco(int totalBloques, int bloquesSwap,
                 int posicionInicial) {

        if (totalBloques <= 0) {
            throw new IllegalArgumentException(
                "El disco debe tener al menos un bloque"
            );
        }

        if (bloquesSwap < 0 || bloquesSwap >= totalBloques) {
            throw new IllegalArgumentException(
                "El area de swap debe ser menor que el disco"
            );
        }

        if (posicionInicial < 0 ||
            posicionInicial >= totalBloques) {
            throw new IllegalArgumentException(
                "Posicion inicial del cabezal invalida"
            );
        }

        this.totalBloques = totalBloques;
        this.bloquesSwap = bloquesSwap;
        this.posicionCabezal = posicionInicial;
        this.direccionCabezal = 1;

        bloques = new BloqueDisco[totalBloques];

        for (int i = 0; i < totalBloques; i++) {
            bloques[i] = new BloqueDisco(i);
        }
    }

    // -------- INFORMACION DEL DISCO --------

    public int getTotalBloques() {
        return totalBloques;
    }

    public int getBloquesSwap() {
        return bloquesSwap;
    }

    public int getCapacidadArchivos() {
        return totalBloques - bloquesSwap;
    }

    public int getPosicionCabezal() {
        return posicionCabezal;
    }

    public int getDireccionCabezal() {
        return direccionCabezal;
    }

    public void setDireccionCabezal(int direccion) {
        if (direccion != 1 && direccion != -1) {
            throw new IllegalArgumentException(
                "Direccion debe ser 1 o -1"
            );
        }

        this.direccionCabezal = direccion;
    }

    public void setPosicionCabezal(int posicion) {
        if (posicion < 0 || posicion >= totalBloques) {
            throw new IllegalArgumentException(
                "Posicion del cabezal invalida"
            );
        }

        this.posicionCabezal = posicion;
    }

    // -------- CONSULTAS DE BLOQUES --------

    public BloqueDisco getBloque(int numero) {
        validarNumero(numero);
        return bloques[numero];
    }

    public int contarBloquesLibresArchivos() {
        return contarLibres(0, getCapacidadArchivos());
    }

    public int contarBloquesLibresSwap() {
        return contarLibres(getCapacidadArchivos(),
                            totalBloques);
    }

    private int contarLibres(int inicio, int fin) {
        mutex.acquireUninterruptibly();

        try {
            int libres = 0;

            for (int i = inicio; i < fin; i++) {
                if (!bloques[i].isOcupado()) {
                    libres++;
                }
            }

            return libres;
        } finally {
            mutex.release();
        }
    }

    // -------- ASIGNACION ENCADENADA --------

    /**
     * Reserva una cadena de bloques para un archivo.
     *
     * @return primer bloque de la cadena
     *         o -1 si no existe espacio suficiente.
     */
    public int asignarArchivo(String propietario,
                              int cantidad) {

        if (propietario == null ||
            propietario.isBlank()) {
            throw new IllegalArgumentException(
                "Propietario invalido"
            );
        }

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                "Cantidad de bloques invalida"
            );
        }

        return asignarCadena(
            propietario,
            cantidad,
            0,
            getCapacidadArchivos()
        );
    }

    /**
     * Reserva bloques exclusivamente en el area swap.
     */
    public int asignarSwap(String proceso,
                           int cantidad) {

        if (proceso == null || proceso.isBlank()) {
            throw new IllegalArgumentException(
                "Proceso invalido"
            );
        }

        if (cantidad <= 0) {
            throw new IllegalArgumentException(
                "Cantidad de bloques invalida"
            );
        }

        return asignarCadena(
            "SWAP:" + proceso,
            cantidad,
            getCapacidadArchivos(),
            totalBloques
        );
    }

    private int asignarCadena(String propietario,
                              int cantidad,
                              int inicio,
                              int fin) {

        mutex.acquireUninterruptibly();

        try {
            int libres = 0;

            for (int i = inicio; i < fin; i++) {
                if (!bloques[i].isOcupado()) {
                    libres++;
                }
            }

            if (libres < cantidad) {
                return -1;
            }

            int primero = -1;
            int anterior = -1;
            int asignados = 0;

            for (int i = inicio;
                 i < fin && asignados < cantidad;
                 i++) {

                if (bloques[i].isOcupado()) {
                    continue;
                }

                bloques[i].ocupar(propietario, -1);

                if (primero == -1) {
                    primero = i;
                }

                if (anterior != -1) {
                    bloques[anterior].setSiguienteBloque(i);
                }

                anterior = i;
                asignados++;
            }

            return primero;

        } finally {
            mutex.release();
        }
    }

    // -------- LIBERACION DE BLOQUES --------

    /**
     * Libera una cadena desde su primer bloque.
     */
    public void liberarCadena(int primerBloque) {

        mutex.acquireUninterruptibly();

        try {
            validarNumero(primerBloque);

            if (!bloques[primerBloque].isOcupado()) {
                throw new IllegalStateException(
                    "El bloque inicial ya esta libre"
                );
            }

            // Comprobar la cadena antes de modificarla.
            String propietario =
                bloques[primerBloque].getPropietario();

            boolean esSwap = propietario.startsWith("SWAP:");
            int inicio = esSwap ? getCapacidadArchivos() : 0;
            int fin = esSwap ? totalBloques : getCapacidadArchivos();

            int actual = primerBloque;
            int recorridos = 0;

            while (actual != -1) {
                if (actual < inicio || actual >= fin ||
                    !bloques[actual].isOcupado() ||
                    !propietario.equals(
                        bloques[actual].getPropietario())) {
                    throw new IllegalStateException(
                        "Cadena de bloques inconsistente"
                    );
                }

                actual = bloques[actual].getSiguienteBloque();
                recorridos++;

                if (recorridos > fin - inicio) {
                    throw new IllegalStateException(
                        "Ciclo detectado en la cadena"
                    );
                }
            }

            actual = primerBloque;

            while (actual != -1) {
                int siguiente =
                    bloques[actual].getSiguienteBloque();

                bloques[actual].liberar();
                actual = siguiente;
            }

        } finally {
            mutex.release();
        }
    }

    // -------- VALIDACIONES --------

    private void validarNumero(int numero) {
        if (numero < 0 || numero >= totalBloques) {
            throw new IllegalArgumentException(
                "Bloque fuera del disco: " + numero
            );
        }
    }
}
