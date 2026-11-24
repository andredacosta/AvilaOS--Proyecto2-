
package com.avilaos.gui;

import com.avilaos.model.EventLogger;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Elohym Casais
 */

public class VentanaPrincipal extends JFrame {
    private static final long serialVersionUID = 1L;

    private JLabel lblRelojGlobal;
    private JTextArea txtLogEventos;
    private JPanel panelClustering;
    private JPanel panelBuffersGlobal;
    private GraficaUtilizacionCPU panelGraficaCPU;

    private JLabel lblThroughput;
    private JLabel lblUtilizacionCPU;
    private JLabel lblTasaDeadlines;

    public VentanaPrincipal() {
        setTitle("ÁvilaOS — Simulador de Sistema Operativo Distribuido");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1366, 768);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        construirVistaSuperior();
        construirVistaCentral();
        construirVistaInferior();
    }

    private void construirVistaSuperior() {
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 5));
        panelSuperior.setBorder(BorderFactory.createTitledBorder("Control Global & Sincronización"));

        lblRelojGlobal = new JLabel("Reloj Global: Ciclo 0", SwingConstants.CENTER);
        lblRelojGlobal.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblRelojGlobal.setForeground(new Color(0, 102, 204));
        panelSuperior.add(lblRelojGlobal, BorderLayout.NORTH);

        txtLogEventos = new JTextArea(6, 50);
        txtLogEventos.setEditable(false);
        txtLogEventos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(txtLogEventos);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Registro de Eventos del Sistema"));
        panelSuperior.add(scrollLog, BorderLayout.CENTER);

        panelBuffersGlobal = new JPanel();
        panelBuffersGlobal.setLayout(new BoxLayout(panelBuffersGlobal, BoxLayout.Y_AXIS));
        panelBuffersGlobal.setBorder(BorderFactory.createTitledBorder("Buffers Compartidos & Semáforos"));
        panelSuperior.add(panelBuffersGlobal, BorderLayout.EAST);

        add(panelSuperior, BorderLayout.NORTH);
    }

    private void construirVistaCentral() {
        panelClustering = new JPanel(new GridLayout(1, 2, 10, 10));
        panelClustering.setBorder(BorderFactory.createTitledBorder("Clúster de Computadores Simulados"));

        add(panelClustering, BorderLayout.CENTER);
    }

    private void construirVistaInferior() {
        JPanel panelInferior = new JPanel(new BorderLayout(10, 5));
        panelInferior.setPreferredSize(new Dimension(getWidth(), 180));
        panelInferior.setBorder(BorderFactory.createTitledBorder("Métricas de Rendimiento Global"));

        JPanel panelMetricasTexto = new JPanel(new GridLayout(3, 1));
        lblThroughput = new JLabel("Throughput: 0.00 proc/ciclo");
        lblUtilizacionCPU = new JLabel("Utilización Promedio CPU: 0.00%");
        lblTasaDeadlines = new JLabel("Cumplimiento Deadlines: 100.00%");

        panelMetricasTexto.add(lblThroughput);
        panelMetricasTexto.add(lblUtilizacionCPU);
        panelMetricasTexto.add(lblTasaDeadlines);

        panelInferior.add(panelMetricasTexto, BorderLayout.WEST);

        panelGraficaCPU = new GraficaUtilizacionCPU();
        panelInferior.add(panelGraficaCPU, BorderLayout.CENTER);

        add(panelInferior, BorderLayout.SOUTH);
    }

    public void agregarPanelComputador(PanelComputador panel) {
        panelClustering.add(panel);
        panelClustering.revalidate();
        panelClustering.repaint();
    }

    public void actualizarLog() {
        txtLogEventos.setText(EventLogger.getInstancia().getLogCompleto());
        txtLogEventos.setCaretPosition(txtLogEventos.getDocument().getLength());
    }

    public void actualizarReloj(long ciclo) {
        lblRelojGlobal.setText("Reloj Global: Ciclo " + ciclo);
    }

    public void actualizarGraficaCPU(long ciclo, double porcentaje, String idComputador) {
        panelGraficaCPU.agregarPunto(ciclo, porcentaje, idComputador);
    }
}
