
package com.avilaos.disk;

/**
 * Representa un bloque del disco simulado de AvilaOS.
 * Permite implementar la asignacion encadenada.
 */
public class BloqueDisco {

    private final int numero;
    private boolean ocupado;
    private String propietario;
    private int siguienteBloque;

    public BloqueDisco(int numero) {
        if (numero < 0) {
            throw new IllegalArgumentException(
                "El numero del bloque no puede ser negativo"
            );
        }

        this.numero = numero;
        this.ocupado = false;
        this.propietario = null;
        this.siguienteBloque = -1;
    }

    public int getNumero() {
        return numero;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public String getPropietario() {
        return propietario;
    }

    public int getSiguienteBloque() {
        return siguienteBloque;
    }

    public void ocupar(String propietario, int siguienteBloque) {
        if (ocupado) {
            throw new IllegalStateException(
                "El bloque " + numero + " ya esta ocupado"
            );
        }

        if (propietario == null || propietario.isBlank()) {
            throw new IllegalArgumentException(
                "El propietario no puede estar vacio"
            );
        }

        if (siguienteBloque < -1 || siguienteBloque == numero) {
            throw new IllegalArgumentException(
                "Referencia al siguiente bloque invalida"
            );
        }

        this.ocupado = true;
        this.propietario = propietario;
        this.siguienteBloque = siguienteBloque;
    }

    public void liberar() {
        this.ocupado = false;
        this.propietario = null;
        this.siguienteBloque = -1;
    }

    public void setSiguienteBloque(int siguienteBloque) {
        if (!ocupado) {
            throw new IllegalStateException(
                "No se puede enlazar un bloque libre"
            );
        }

        if (siguienteBloque < -1 || siguienteBloque == numero) {
            throw new IllegalArgumentException(
                "Referencia al siguiente bloque invalida"
            );
        }

        this.siguienteBloque = siguienteBloque;
    }

    @Override
    public String toString() {
        return "Bloque " + numero
                + " | Ocupado: " + ocupado
                + " | Propietario: " + propietario
                + " | Siguiente: " + siguienteBloque;
    }
}
