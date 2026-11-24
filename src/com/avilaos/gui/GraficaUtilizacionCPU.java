
package com.avilaos.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 *
 * @author Elohym Casais
 */
public class GraficaUtilizacionCPU extends JPanel {
    private static final long serialVersionUID = 1L;
    private final DefaultCategoryDataset dataset;

    public GraficaUtilizacionCPU() {
        setLayout(new BorderLayout());
        dataset = new DefaultCategoryDataset();

        JFreeChart chart = ChartFactory.createLineChart(
                "Utilización de CPU por Nodo",
                "Ciclos de Reloj",
                "Uso CPU (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 150));
        add(chartPanel, BorderLayout.CENTER);
    }

    public void agregarPunto(long ciclo, double porcentajeUso, String idComputador) {
        dataset.addValue(porcentajeUso, idComputador, String.valueOf(ciclo));
    }

    public void limpiar() {
        dataset.clear();
    }
}
