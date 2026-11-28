package com.avilaos.gui;

import com.avilaos.config.ConfiguracionComputadorDTO;
import com.avilaos.config.ConfiguracionSistemaDTO;
import com.avilaos.model.EventLogger;
import javax.swing.Timer;

/**
 * 
 * @author Elohym Casais
 */

public class ControladorSimulacion {

    private final VentanaPrincipal ventanaPrincipal;
    private final ConfiguracionSistemaDTO config;
    private long cicloActual;
    private Timer timerCiclos;

    private PanelComputador[] paneles;
    private int[] ramTotalPc;
    private String[][] colasNuevos;
    private String[][] colasListos;
    private String[][] colasBloqueados;
    private String[][] colasTerminados;
    private int[] contadorProcesos;

    public ControladorSimulacion(VentanaPrincipal ventanaPrincipal, ConfiguracionSistemaDTO config) {
        this.ventanaPrincipal = ventanaPrincipal;
        this.config = config;
        this.cicloActual = 0;
    }

    public void iniciar() {
        inicializarCluster();
        registrarEventoInicio();
        refrescarVista();
        iniciarBucleCiclos();
    }

    private void inicializarCluster() {
        int numComputadores = 2;
        int ramPorComputador = 1024;
        ConfiguracionComputadorDTO[] pcs = null;

        if (config != null && config.getComputadores() != null && config.getComputadores().length > 0) {
            pcs = config.getComputadores();
            numComputadores = pcs.length;
        }

        paneles = new PanelComputador[numComputadores];
        ramTotalPc = new int[numComputadores];
        colasNuevos = new String[numComputadores][];
        colasListos = new String[numComputadores][];
        colasBloqueados = new String[numComputadores][];
        colasTerminados = new String[numComputadores][];
        contadorProcesos = new int[numComputadores];

        for (int i = 0; i < numComputadores; i++) {
            String idPc = "PC-" + (i + 1);
            int ramPc = ramPorComputador;

            if (pcs != null && pcs[i] != null) {
                if (pcs[i].getId() != null && !pcs[i].getId().trim().isEmpty()) {
                    idPc = pcs[i].getId();
                }
                if (pcs[i].getCapacidadRAM() > 0) {
                    ramPc = pcs[i].getCapacidadRAM();
                }
            }

            ramTotalPc[i] = ramPc;
            colasNuevos[i] = new String[]{"P" + (i + 1) + "-1", "P" + (i + 1) + "-2"};
            colasListos[i] = new String[]{"P" + (i + 1) + "-0"};
            colasBloqueados[i] = new String[0];
            colasTerminados[i] = new String[0];
            contadorProcesos[i] = 3;

            PanelComputador panelPc = new PanelComputador(idPc, ramPc);
            paneles[i] = panelPc;
            ventanaPrincipal.agregarPanelComputador(panelPc);
        }
    }

    private void registrarEventoInicio() {
        EventLogger.getInstancia().registrarEvento(cicloActual, "Sistema inicializado.");
    }

    private void avanzarEstadoComputadores() {
        if (paneles == null) {
            return;
        }

        for (int i = 0; i < paneles.length; i++) {
            // Rotación simple de procesos entre colas en cada ciclo
            String actualEnListo = (colasListos[i].length > 0) ? colasListos[i][0] : null;
            String siguienteNuevo = (colasNuevos[i].length > 0) ? colasNuevos[i][0] : null;

            if (actualEnListo != null) {
                if (cicloActual % 4 == 0) {
                    colasBloqueados[i] = agregarElemento(colasBloqueados[i], actualEnListo);
                    colasListos[i] = quitarPrimero(colasListos[i]);
                    EventLogger.getInstancia().registrarEvento(
                            cicloActual, paneles[i].getIdComputador() + ": " + actualEnListo + " -> BLOQUEADO");
                } else if (cicloActual % 6 == 0) {
                    colasTerminados[i] = agregarElemento(colasTerminados[i], actualEnListo);
                    colasListos[i] = quitarPrimero(colasListos[i]);
                    EventLogger.getInstancia().registrarEvento(
                            cicloActual, paneles[i].getIdComputador() + ": " + actualEnListo + " -> TERMINADO");
                }
            }

            if (siguienteNuevo != null && cicloActual % 2 == 0) {
                colasListos[i] = agregarElemento(colasListos[i], siguienteNuevo);
                colasNuevos[i] = quitarPrimero(colasNuevos[i]);
            }

            // Reponer un nuevo proceso cada 5 ciclos para mantener actividad
            if (cicloActual % 5 == 0) {
                contadorProcesos[i]++;
                String nuevoProc = "P" + (i + 1) + "-" + contadorProcesos[i];
                colasNuevos[i] = agregarElemento(colasNuevos[i], nuevoProc);
            }

            String procesoCpu = (colasListos[i].length > 0) ? colasListos[i][0] : "[IDLE]";
            int activos = colasListos[i].length + colasBloqueados[i].length;
            double usoCpu = (procesoCpu.equals("[IDLE]"))
                    ? 0.05
                    : Math.min(0.95, 0.25 + (activos * 0.15) + ((cicloActual + i) % 3) * 0.08);

            int ramUsada = Math.min(ramTotalPc[i], activos * (ramTotalPc[i] / 6));
            String modo = procesoCpu.equals("[IDLE]") ? "USUARIO" : ((cicloActual % 3 == 0) ? "KERNEL" : "USUARIO");

            paneles[i].actualizarColas(colasNuevos[i], colasListos[i], colasBloqueados[i], colasTerminados[i]);
            paneles[i].actualizarCPU(procesoCpu, (int) cicloActual, modo);
            paneles[i].actualizarRAM(ramUsada, ramTotalPc[i]);

            ventanaPrincipal.actualizarGraficaCPU(cicloActual, usoCpu, paneles[i].getIdComputador());
        }
    }

    private void refrescarVista() {
        avanzarEstadoComputadores();
        ventanaPrincipal.actualizarReloj(cicloActual);
        ventanaPrincipal.actualizarLog();
    }

    private void iniciarBucleCiclos() {
        int delayMs = (config != null && config.getDuracionCicloMs() > 0)
                ? config.getDuracionCicloMs()
                : 1000;

        timerCiclos = new Timer(delayMs, e -> {
            cicloActual++;
            refrescarVista();
        });
        timerCiclos.start();
    }

    public void detener() {
        if (timerCiclos != null) {
            timerCiclos.stop();
        }
    }

    private String[] agregarElemento(String[] origen, String elemento) {
        int len = (origen != null) ? origen.length : 0;
        String[] destino = new String[len + 1];
        for (int i = 0; i < len; i++) {
            destino[i] = origen[i];
        }
        destino[len] = elemento;
        return destino;
    }

    private String[] quitarPrimero(String[] origen) {
        if (origen == null || origen.length <= 1) {
            return new String[0];
        }
        String[] destino = new String[origen.length - 1];
        for (int i = 1; i < origen.length; i++) {
            destino[i - 1] = origen[i];
        }
        return destino;
    }
}