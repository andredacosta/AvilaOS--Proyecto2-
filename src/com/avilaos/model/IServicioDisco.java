/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.avilaos.model;

/**
 *
 * @author Elohym Casais
 */
public interface IServicioDisco {
    
    void encolarSolicitud(SolicitudDisco solicitud);
    
    void cambiarPolitica(PoliticaPlanificacionDisco nuevaPolitica);
    
    PoliticaPlanificacionDisco getPoliticaActual();
    
    int getTotalBloques();
    int getSwapSizeBlocks();
    int getBloquesLibresArchivos();
    int getPosicionCabezal();
    
    boolean isDireccionAscendente();
    
    void asignarCadenaPredefinida(String rutaArchivo, int bloqueInicio, int cantidadBloques);
    
    int asignarCadenaBloques(String rutaArchivo, int cantidadBloques);
    
    void liberarCadenaBloques(int primerBloque);
    
}
