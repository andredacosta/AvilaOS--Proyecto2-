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
public class Archivo extends NodoFS {
    
    private int tamanoBloques;
    private int primerBloque;
    
    public Archivo(
    String nombre,
    String ruta,
    String propietario,
    PermisoArchivo permiso,
    Directorio padre,
    int tamanoBloques,
    int primerBloque){
        super(nombre, ruta, propietario, permiso, padre);
        if (tamanoBloques < 0){
            throw new IllegalArgumentException("El tamano en bloques no puede ser negativo");
        }
        this.tamanoBloques = tamanoBloques;
        this.primerBloque = primerBloque;
    }

    @Override
    public boolean esDirectorio() {
        return false;
    }

    public int getTamanoBloques() {
        return tamanoBloques;
    }

    public void setTamanoBloques(int tamanoBloques) {
        if (tamanoBloques < 0) {
            throw new IllegalArgumentException("El tamano en bloques no puede ser negativo");
        }
        this.tamanoBloques = tamanoBloques;
    }

    public int getPrimerBloque() {
        return primerBloque;
    }

    public void setPrimerBloque(int primerBloque) {
        this.primerBloque = primerBloque;
    }
    
}
