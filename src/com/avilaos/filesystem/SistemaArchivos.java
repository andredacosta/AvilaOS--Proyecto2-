/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.avilaos.filesystem;

import com.avilaos.model.IServicioDisco;
import com.avilaos.model.ISistemaArchivos;
import com.avilaos.model.ModoUsuario;
import com.avilaos.model.PermisoArchivo;
import com.avilaos.model.SolicitudDisco;
import com.avilaos.model.TipoOperacionIO;
import com.avilaos.structures.ListaEnlazada;

/**
 *
 * @author Elohym Casais
 */
public class SistemaArchivos implements ISistemaArchivos {
    
    private final Directorio raiz;
    private final IServicioDisco servicioDisco;

    public SistemaArchivos(IServicioDisco servicioDisco) {
        this.servicioDisco = servicioDisco;
        this.raiz = new Directorio("/", "/", "admin", PermisoArchivo.PUBLICO, null);
    }

    public Directorio getRaiz() {
        return raiz;
    }

    public NodoFS buscarNodo(String ruta) {
        String normalizada = NodoFS.normalizarRuta(ruta);
        if ("/".equals(normalizada)) {
            return raiz;
        }
        String[] partes = dividirRuta(normalizada);
        Directorio actual = raiz;
        for (int i = 0; i < partes.length; i++) {
            NodoFS encontrado = actual.buscarHijoPorNombre(partes[i]);
            if (encontrado == null) {
                return null;
            }
            if (i == partes.length - 1) {
                return encontrado;
            }
            if (!encontrado.esDirectorio()) {
                return null;
            }
            actual = (Directorio) encontrado;
        }
        return null;
    }

    @Override
    public String validarPermisoOperacion(
            ModoUsuario modo,
            String usuarioActual,
            TipoOperacionIO operacion,
            String rutaObjetivo,
            boolean crearDirectorio,
            int bloquesRequeridos) {

        if (operacion == null) {
            return "Operacion de E/S invalida.";
        }
        if (operacion == TipoOperacionIO.SUSPENDER || operacion == TipoOperacionIO.REACTIVAR) {
            return null;
        }

        String rutaNormalizada = NodoFS.normalizarRuta(rutaObjetivo);
        if ("/".equals(rutaNormalizada) && operacion == TipoOperacionIO.ELIMINAR) {
            return "No se puede eliminar el directorio raiz '/'.";
        }

        String usuarioEfectivo = (usuarioActual == null || usuarioActual.trim().isEmpty())
                ? "usuario"
                : usuarioActual.trim();
        boolean esAdmin = (modo == ModoUsuario.ADMINISTRADOR);

        String rutaPadre = obtenerRutaPadre(rutaNormalizada);
        NodoFS nodoPadre = buscarNodo(rutaPadre);
        if (nodoPadre == null || !nodoPadre.esDirectorio()) {
            return "El directorio padre '" + rutaPadre + "' no existe.";
        }

        NodoFS existente = buscarNodo(rutaNormalizada);

        if (operacion == TipoOperacionIO.CREAR) {
            if (existente != null) {
                return "Ya existe un elemento en '" + rutaNormalizada + "'.";
            }
            if (!esAdmin && nodoPadre.getPermiso() == PermisoArchivo.PRIVADO
                    && !usuarioEfectivo.equals(nodoPadre.getPropietario())) {
                return "Permiso denegado: el directorio padre es privado.";
            }
            if (!crearDirectorio && bloquesRequeridos > 0 && servicioDisco != null) {
                if (servicioDisco.getBloquesLibresArchivos() < bloquesRequeridos) {
                    return "Espacio insuficiente en disco para asignar " + bloquesRequeridos + " bloques.";
                }
            }
            return null;
        }

        if (operacion == TipoOperacionIO.LEER) {
            if (existente == null) {
                return "No existe el elemento '" + rutaNormalizada + "'.";
            }
            if (!esAdmin && existente.getPermiso() == PermisoArchivo.PRIVADO
                    && !usuarioEfectivo.equals(existente.getPropietario())) {
                return "Permiso denegado: archivo o directorio privado.";
            }
            return null;
        }

        if (operacion == TipoOperacionIO.ACTUALIZAR) {
            if (existente == null || existente.esDirectorio()) {
                return "Solo se pueden actualizar archivos existentes en '" + rutaNormalizada + "'.";
            }
            if (!esAdmin && existente.getPermiso() == PermisoArchivo.PRIVADO
                    && !usuarioEfectivo.equals(existente.getPropietario())) {
                return "Permiso denegado para modificar '" + rutaNormalizada + "'.";
            }
            if (bloquesRequeridos > 0 && servicioDisco != null) {
                Archivo archivo = (Archivo) existente;
                int diferencia = bloquesRequeridos - archivo.getTamanoBloques();
                if (diferencia > 0 && servicioDisco.getBloquesLibresArchivos() < diferencia) {
                    return "Espacio insuficiente en disco para ampliar el archivo.";
                }
            }
            return null;
        }

        if (operacion == TipoOperacionIO.ELIMINAR) {
            if (existente == null) {
                return "No existe el elemento '" + rutaNormalizada + "'.";
            }
            if (!esAdmin && existente.getPermiso() == PermisoArchivo.PRIVADO
                    && !usuarioEfectivo.equals(existente.getPropietario())) {
                return "Permiso denegado para eliminar '" + rutaNormalizada + "'.";
            }
            if (existente.esDirectorio() && !((Directorio) existente).estaVacio() && !esAdmin) {
                return "El directorio '" + rutaNormalizada + "' no esta vacio.";
            }
            return null;
        }

        return null;
    }

    @Override
    public int resolverBloqueObjetivo(TipoOperacionIO operacion, String rutaObjetivo, int bloquesRequeridos) {
        if (operacion == TipoOperacionIO.SUSPENDER || operacion == TipoOperacionIO.REACTIVAR) {
            return 0;
        }
        NodoFS nodo = buscarNodo(rutaObjetivo);
        if (nodo != null && !nodo.esDirectorio()) {
            int primerBloque = ((Archivo) nodo).getPrimerBloque();
            return primerBloque >= 0 ? primerBloque : 0;
        }
        NodoFS padre = buscarNodo(obtenerRutaPadre(rutaObjetivo));
        if (padre != null && !padre.esDirectorio()) {
            int primerBloque = ((Archivo) padre).getPrimerBloque();
            return primerBloque >= 0 ? primerBloque : 0;
        }
        return (servicioDisco != null) ? servicioDisco.getPosicionCabezal() : 0;
    }

    @Override
    public void confirmarOperacionCompletada(SolicitudDisco solicitud, int primerBloqueAsignado) {
        if (solicitud == null) {
            return;
        }
        TipoOperacionIO op = solicitud.getOperacion();
        if (op == TipoOperacionIO.SUSPENDER || op == TipoOperacionIO.REACTIVAR) {
            return;
        }

        String rutaNormalizada = NodoFS.normalizarRuta(solicitud.getRutaObjetivo());
        String nombre = obtenerNombreDesdeRuta(rutaNormalizada);
        String rutaPadre = obtenerRutaPadre(rutaNormalizada);
        NodoFS nodoPadre = buscarNodo(rutaPadre);
        Directorio dirPadre = (nodoPadre != null && nodoPadre.esDirectorio()) ? (Directorio) nodoPadre : raiz;

        String extra = solicitud.getParametroExtra();
        String usuario = (extra != null && !extra.trim().isEmpty())
                ? extra.trim()
                : solicitud.getNombreProceso();
        PermisoArchivo permiso = "PRIVADO".equalsIgnoreCase(extra)
                ? PermisoArchivo.PRIVADO
                : PermisoArchivo.PUBLICO;

        if (op == TipoOperacionIO.CREAR) {
            if (buscarNodo(rutaNormalizada) != null) {
                return;
            }
            boolean crearDirectorio = solicitud.getCantidadBloques() == 0
                    && "DIR".equalsIgnoreCase(extra);
            if (crearDirectorio) {
                Directorio nuevoDir = new Directorio(nombre, rutaNormalizada, usuario, permiso, dirPadre);
                dirPadre.agregarHijo(nuevoDir);
            } else {
                int bloques = solicitud.getCantidadBloques() > 0 ? solicitud.getCantidadBloques() : 1;
                int bloqueInicio = primerBloqueAsignado;
                if (bloqueInicio < 0 && servicioDisco != null) {
                    bloqueInicio = servicioDisco.asignarCadenaBloques(rutaNormalizada, bloques);
                }
                Archivo nuevoArchivo = new Archivo(
                        nombre,
                        rutaNormalizada,
                        usuario,
                        permiso,
                        dirPadre,
                        bloques,
                        bloqueInicio);
                dirPadre.agregarHijo(nuevoArchivo);
            }
        } else if (op == TipoOperacionIO.ACTUALIZAR) {
            NodoFS existente = buscarNodo(rutaNormalizada);
            if (existente != null && !existente.esDirectorio()) {
                Archivo archivo = (Archivo) existente;
                int nuevoTamano = solicitud.getCantidadBloques() > 0
                        ? solicitud.getCantidadBloques()
                        : archivo.getTamanoBloques();
                if (servicioDisco != null && nuevoTamano != archivo.getTamanoBloques()) {
                    if (archivo.getPrimerBloque() >= 0) {
                        servicioDisco.liberarCadenaBloques(archivo.getPrimerBloque());
                    }
                    int nuevoInicio = servicioDisco.asignarCadenaBloques(rutaNormalizada, nuevoTamano);
                    archivo.setPrimerBloque(nuevoInicio);
                }
                archivo.setTamanoBloques(nuevoTamano);
            }
        } else if (op == TipoOperacionIO.ELIMINAR) {
            NodoFS existente = buscarNodo(rutaNormalizada);
            if (existente != null && existente.getPadre() != null) {
                if (!existente.esDirectorio() && servicioDisco != null) {
                    Archivo archivo = (Archivo) existente;
                    if (archivo.getPrimerBloque() >= 0) {
                        servicioDisco.liberarCadenaBloques(archivo.getPrimerBloque());
                    }
                } else if (existente.esDirectorio() && servicioDisco != null) {
                    liberarArchivosRecursivo((Directorio) existente);
                }
                existente.getPadre().eliminarHijo(existente);
            }
        }
    }

    private void liberarArchivosRecursivo(Directorio dir) {
        ListaEnlazada<NodoFS> hijos = dir.getHijos();
        int total = hijos.tamano();
        for (int i = 0; i < total; i++) {
            NodoFS hijo = hijos.obtener(i);
            if (!hijo.esDirectorio()) {
                int inicio = ((Archivo) hijo).getPrimerBloque();
                if (inicio >= 0) {
                    servicioDisco.liberarCadenaBloques(inicio);
                }
            } else {
                liberarArchivosRecursivo((Directorio) hijo);
            }
        }
    }

    private String[] dividirRuta(String rutaNormalizada) {
        String sinRaiz = rutaNormalizada.startsWith("/") ? rutaNormalizada.substring(1) : rutaNormalizada;
        if (sinRaiz.isEmpty()) {
            return new String[0];
        }
        return sinRaiz.split("/");
    }

    private String obtenerRutaPadre(String rutaNormalizada) {
        if ("/".equals(rutaNormalizada)) {
            return "/";
        }
        int ultimoSlash = rutaNormalizada.lastIndexOf('/');
        if (ultimoSlash <= 0) {
            return "/";
        }
        return rutaNormalizada.substring(0, ultimoSlash);
    }

    private String obtenerNombreDesdeRuta(String rutaNormalizada) {
        if ("/".equals(rutaNormalizada)) {
            return "/";
        }
        int ultimoSlash = rutaNormalizada.lastIndexOf('/');
        return rutaNormalizada.substring(ultimoSlash + 1);
    }
    
    
}
