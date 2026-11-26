/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.avilaos.filesystem;

import com.avilaos.model.PermisoArchivo;

/**
 *
 * @author Elohym Casais
 */
public abstract class NodoFS {
    
    private String nombre;
    private String ruta;
    private String propietario;
    private PermisoArchivo permiso;
    private Directorio padre;

    protected NodoFS(String nombre, String ruta, String propietario, PermisoArchivo permiso, Directorio padre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del nodo no puede estar vacio");
        }
        if (ruta == null || ruta.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del nodo no puede estar vacia");
        }
        this.nombre = nombre.trim();
        this.ruta = normalizarRuta(ruta);
        this.propietario = (propietario == null || propietario.trim().isEmpty()) ? "admin" : propietario.trim();
        this.permiso = (permiso == null) ? PermisoArchivo.PUBLICO : permiso;
        this.padre = padre;
    }

    public static String normalizarRuta(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) {
            return "/";
        }
        String limpia = ruta.trim().replace('\\', '/');
        while (limpia.contains("//")) {
            limpia = limpia.replace("//", "/");
        }
        if (!limpia.startsWith("/")) {
            limpia = "/" + limpia;
        }
        if (limpia.length() > 1 && limpia.endsWith("/")) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        return limpia;
    }

    public abstract boolean esDirectorio();

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        this.nombre = nombre.trim();
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = normalizarRuta(ruta);
    }

    public String getPropietario() {
        return propietario;
    }

    public void setPropietario(String propietario) {
        if (propietario != null && !propietario.trim().isEmpty()) {
            this.propietario = propietario.trim();
        }
    }

    public PermisoArchivo getPermiso() {
        return permiso;
    }

    public void setPermiso(PermisoArchivo permiso) {
        if (permiso != null) {
            this.permiso = permiso;
        }
    }

    public Directorio getPadre() {
        return padre;
    }

    public void setPadre(Directorio padre) {
        this.padre = padre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodoFS)) {
            return false;
        }
        NodoFS otro = (NodoFS) o;
        return this.ruta.equals(otro.ruta);
    }
}
