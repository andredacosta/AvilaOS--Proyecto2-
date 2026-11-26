/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.avilaos.model;

/**
 *
 * @author Elohym Casais
 */
public interface ISistemaArchivos {
    
    /**
     * Valida si el usuario y el modo actual pueden ejecutar la operacion.Retorna null si es valido, o un mensaje explicando el motivo del rechazo.
     *
     * @param modo
     * @param usuarioActual
     * @param operacion
     * @param rutaObjetivo
     * @param crearDirectorio
     * @param bloquesRequeridos
     * @return 
     */
    String validarPermisoOperacion(
            ModoUsuario modo,
            String usuarioActual,
            TipoOperacionIO operacion,
            String rutaObjetivo,
            boolean crearDirectorio,
            int bloquesRequeridos);
    
    /**
     * Devuelve el bloque inicial sobre el que actuara el cabezal del disco.
     * @param operacion
     * @param rutaObjetivo
     * @param bloquesRequeridos
     * @return 
     */
    int resolverBloqueObjetivo(TipoOperacionIO operacion, String rutaObjetivo, int bloquesRequeridos);
    
    /**
     * Aplica la mutacion logica en el arbol de archivos/directorios cuando el disco
     * finaliza la atencion de la solicitud.
     * @param solicitud
     * @param primerBloqueAsignado
     */
    void confirmarOperacionCompletada(SolicitudDisco solicitud, int primerBloqueAsignado);
    
    
    
}
