
package com.avilaos.gui;


import com.avilaos.model.PoliticaPlanificacion;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Elohym Casais
 */
public class PanelComputador extends JPanel {
    private static final long serialVersionUID = 1L;
    private final String idComputador;
    
    private JLabel lblProcesoEnCPU;
    private JLabel lblProgramCounter;
    private JLabel lblModoCPU;
    private JComboBox<PoliticaPlanificacion> comboPolitica;
    private JTextField txtQuantum;
    private JProgressBar barraRAM;

    private DefaultListModel<String> modelNuevos;
    private DefaultListModel<String> modelListos;
    private DefaultListModel<String> modelBloqueados;
    private DefaultListModel<String> modelTerminados;

    public PanelComputador(String idComputador, int capacidadRAMTotal) {
        this.idComputador = idComputador;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Computador: " + idComputador));
        inicializarComponentes(capacidadRAMTotal);
    }

    private void inicializarComponentes(int capacidadRAMTotal) {
        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTop.add(new JLabel("Política:"));
        comboPolitica = new JComboBox<>(PoliticaPlanificacion.values());
        panelTop.add(comboPolitica);

        panelTop.add(new JLabel("Quantum:"));
        txtQuantum = new JTextField("2", 3);
        panelTop.add(txtQuantum);
        add(panelTop, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel panelCPU = new JPanel(new GridLayout(3, 1));
        panelCPU.setBorder(BorderFactory.createTitledBorder("Estado de la CPU"));
        
        lblProcesoEnCPU = new JLabel("Proceso en CPU: [IDLE]");
        lblProgramCounter = new JLabel("Program Counter (PC): 0");
        lblModoCPU = new JLabel("Modo: USUARIO");

        panelCPU.add(lblProcesoEnCPU);
        panelCPU.add(lblProgramCounter);
        panelCPU.add(lblModoCPU);

        JPanel panelRAM = new JPanel(new BorderLayout());
        panelRAM.setBorder(BorderFactory.createTitledBorder("Ocupación de RAM"));
        barraRAM = new JProgressBar(0, capacidadRAMTotal);
        barraRAM.setStringPainted(true);
        barraRAM.setValue(0);
        barraRAM.setString("0 / " + capacidadRAMTotal + " KB");
        panelRAM.add(barraRAM, BorderLayout.CENTER);

        panelCentro.add(panelCPU);
        panelCentro.add(panelRAM);
        add(panelCentro, BorderLayout.CENTER);

        JPanel panelColas = new JPanel(new GridLayout(1, 4, 3, 3));
        panelColas.setBorder(BorderFactory.createTitledBorder("Colas del Sistema"));

        modelNuevos = new DefaultListModel<>();
        modelListos = new DefaultListModel<>();
        modelBloqueados = new DefaultListModel<>();
        modelTerminados = new DefaultListModel<>();

        panelColas.add(crearPanelCola("Nuevos", new JList<>(modelNuevos)));
        panelColas.add(crearPanelCola("Listos", new JList<>(modelListos)));
        panelColas.add(crearPanelCola("Bloqueados", new JList<>(modelBloqueados)));
        panelColas.add(crearPanelCola("Terminados", new JList<>(modelTerminados)));

        add(panelColas, BorderLayout.SOUTH);
    }

    private JPanel crearPanelCola(String titulo, JList<String> lista) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        panel.add(new JScrollPane(lista), BorderLayout.CENTER);
        return panel;
    }

    public void actualizarCPU(String idProceso, int pc, String modo) {
        lblProcesoEnCPU.setText("Proceso en CPU: " + idProceso);
        lblProgramCounter.setText("Program Counter (PC): " + pc);
        lblModoCPU.setText("Modo: " + modo);
    }

    public void actualizarRAM(int usada, int total) {
        barraRAM.setValue(usada);
        barraRAM.setString(usada + " / " + total + " KB (" + (total > 0 ? (usada * 100 / total) : 0) + "%)");
    }

    public PoliticaPlanificacion getPoliticaSeleccionada() {
        return (PoliticaPlanificacion) comboPolitica.getSelectedItem();
    }

    public int getQuantum() {
        try {
            return Integer.parseInt(txtQuantum.getText().trim());
        } catch (NumberFormatException e) {
            return 2;
        }
    }
}
