/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.avilaos.filesystem;

import com.avilaos.model.PermisoArchivo;
import com.avilaos.structures.ListaEnlazada;

/**
 *
 * @author Elohym Casais
 */

public class Directorio extends NodoFS {
    
    private final ListaEnlazada<NodoFS> hijos;

    public Directorio(String nombre, String ruta, String propietario, PermisoArchivo permiso, Directorio padre) {
        super(nombre, ruta, propietario, permiso, padre);
        this.hijos = new ListaEnlazada<>();
    }

    @Override
    public boolean esDirectorio() {
        return true;
    }

    public ListaEnlazada<NodoFS> getHijos() {
        return hijos;
    }

    public void agregarHijo(NodoFS hijo) {
        if (hijo == null) {
            throw new IllegalArgumentException("El nodo hijo no puede ser nulo");
        }
        if (buscarHijoPorNombre(hijo.getNombre()) != null) {
            throw new IllegalStateException("Ya existe un elemento llamado '" + hijo.getNombre() + "' en " + getRuta());
        }
        hijo.setPadre(this);
        hijos.agregar(hijo);
    }

    public boolean eliminarHijo(NodoFS hijo) {
        if (hijo == null) {
            return false;
        }
        return hijos.eliminar(hijo);
    }

    public NodoFS buscarHijoPorNombre(String nombreBuscado) {
        if (nombreBuscado == null) {
            return null;
        }
        int total = hijos.tamano();
        for (int i = 0; i < total; i++) {
            NodoFS actual = hijos.obtener(i);
            if (actual.getNombre().equals(nombreBuscado)) {
                return actual;
            }
        }
        return null;
    }

    public boolean estaVacio() {
        return hijos.estaVacia();
    }
    
}
