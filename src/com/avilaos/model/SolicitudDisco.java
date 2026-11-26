/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.avilaos.model;

/**
 *
 * @author Elohym Casais
 */
public class SolicitudDisco {
    
    private final int idSolicitud;
    private final int pidProceso;
    private final String nombreProceso;
    private final TipoOperacionIO operacion;
    private final String rutaObjetivo;
    private int bloqueObjetivo;
    private final int cantidadBloques;
    private final long cicloLlegada;
    private final String parametroExtra;
    
    public SolicitudDisco(
            int idSolicitud,
            int pidProceso,
            String nombreProceso,
            TipoOperacionIO operacion,
            String rutaObjetivo,
            int bloqueObjetivo,
            int cantidadBloques,
            long cicloLlegada,
            String parametroExtra){
        
        if (idSolicitud <= 0){
            throw new IllegalArgumentException("El ID de solicitud debe ser positivo");
        }
        if (pidProceso <= 0){
            throw new IllegalArgumentException("El PID del proceso debe ser positivo");
        }
        if (operacion == null){
            throw new IllegalArgumentException("La operacion de E/S no puede ser nula");
        }
        if (rutaObjetivo == null || rutaObjetivo.isBlank()){
            throw new IllegalArgumentException("La ruta objetivo no puede estar vacia");
        }
        if (cantidadBloques < 0){
            throw new IllegalArgumentException("La cantidad de bloques no puede ser negativa");
        }
        
        this.idSolicitud = idSolicitud;
        this.pidProceso = pidProceso;
        this.nombreProceso = (nombreProceso == null || nombreProceso.isBlank())
                ? "P-" + pidProceso: nombreProceso;
        this.operacion = operacion;
        this.rutaObjetivo = rutaObjetivo;
        this.bloqueObjetivo = bloqueObjetivo;
        this.cantidadBloques = cantidadBloques;
        this.cicloLlegada = cicloLlegada;
        this.parametroExtra = parametroExtra;
    }
    
    public int getIdSolicitud(){
        return idSolicitud;
    }
    
    public int getPidProceso(){
        return pidProceso;
    }
    
    public String getNombreProceso(){
        return nombreProceso;
    }
    
    public TipoOperacionIO getOperacion(){
        return operacion;
    }
    
    public String getRutaObjetivo(){
        return rutaObjetivo;
    }
    
    public int getBloqueObjetivo(){
        return bloqueObjetivo;
    }
    
    public void setBloqueObjetivo(int bloqueObjetivo){
        this.bloqueObjetivo = bloqueObjetivo;
    }
    
    public int getCantidadBloques(){
        return cantidadBloques;
    }
    
    public long getCicloLlegada(){
        return cicloLlegada;
    }
    
    public String getParametroExtra(){
        return parametroExtra;
    }
}
