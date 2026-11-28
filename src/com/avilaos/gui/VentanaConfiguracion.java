package com.avilaos.gui;

import com.avilaos.config.ConfiguracionSistemaDTO;
import com.avilaos.config.GestorConfiguracion;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 *
 * @author Elohym Casais
 */
public class VentanaConfiguracion extends JFrame {
    private static final long serialVersionUID = 1L;

    // Componentes del formulario
    private JTextField txtDuracionCiclo;
    private JTextField txtLatenciaRed;
    private JTextField txtNumComputadores;
    private JTextField txtCapacidadRAM;

    // Gestor de persistencia y configuración cargada
    private final GestorConfiguracion gestorConfig;
    private ConfiguracionSistemaDTO configuracionCargada;

    public VentanaConfiguracion() {
        this.gestorConfig = new GestorConfiguracion();
        this.configuracionCargada = null;

        setTitle("ÁvilaOS — Configuración Inicial del Sistema");
        setSize(520, 380);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        construirFormulario();
        construirBotones();
    }

    private void construirFormulario() {
        JPanel panelForm = new JPanel(new GridLayout(4, 2, 10, 12));
        panelForm.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        panelForm.add(new JLabel("Duración del Ciclo (ms):"));
        txtDuracionCiclo = new JTextField("1000");
        panelForm.add(txtDuracionCiclo);

        panelForm.add(new JLabel("Latencia de Red (ciclos):"));
        txtLatenciaRed = new JTextField("2");
        panelForm.add(txtLatenciaRed);

        panelForm.add(new JLabel("Número de Computadores:"));
        txtNumComputadores = new JTextField("2");
        panelForm.add(txtNumComputadores);

        panelForm.add(new JLabel("RAM por Computador (KB):"));
        txtCapacidadRAM = new JTextField("1024");
        panelForm.add(txtCapacidadRAM);

        add(panelForm, BorderLayout.CENTER);
    }

    private void construirBotones() {
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));

        JButton btnCargarJSON = new JButton("Cargar JSON");
        JButton btnGuardarJSON = new JButton("Guardar JSON");
        JButton btnIniciar = new JButton("Iniciar Simulación");

        // Eventos de persistencia y arranque
        btnCargarJSON.addActionListener(e -> accionarCargarJSON());
        btnGuardarJSON.addActionListener(e -> accionarGuardarJSON());
        btnIniciar.addActionListener(e -> accionarIniciarSimulacion());

        panelBotones.add(btnCargarJSON);
        panelBotones.add(btnGuardarJSON);
        panelBotones.add(btnIniciar);

        add(panelBotones, BorderLayout.SOUTH);
    }

    private void accionarCargarJSON() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            try {
                ConfiguracionSistemaDTO dto = gestorConfig.cargarConfiguracion(archivo.getAbsolutePath());
                this.configuracionCargada = dto;

                txtDuracionCiclo.setText(String.valueOf(dto.getDuracionCicloMs()));
                txtLatenciaRed.setText(String.valueOf(dto.getLatenciaRedCiclos()));
                if (dto.getComputadores() != null) {
                    txtNumComputadores.setText(String.valueOf(dto.getComputadores().length));
                }
                JOptionPane.showMessageDialog(this, "Configuración cargada con éxito.", "ÁvilaOS", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al cargar archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void accionarGuardarJSON() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            try {
                ConfiguracionSistemaDTO dto = construirConfiguracionDesdeFormulario();
                gestorConfig.guardarConfiguracion(dto, archivo.getAbsolutePath());
                this.configuracionCargada = dto;
                JOptionPane.showMessageDialog(this, "Configuración guardada correctamente.", "ÁvilaOS", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private ConfiguracionSistemaDTO construirConfiguracionDesdeFormulario() {
        ConfiguracionSistemaDTO dto = new ConfiguracionSistemaDTO();
        dto.setDuracionCicloMs(getDuracionCiclo());
        dto.setLatenciaRedCiclos(getLatenciaRed());
        return dto;
    }

    private void accionarIniciarSimulacion() {
        ConfiguracionSistemaDTO dto = (this.configuracionCargada != null)
                ? this.configuracionCargada
                : construirConfiguracionDesdeFormulario();

        VentanaPrincipal vPrincipal = new VentanaPrincipal();
        ControladorSimulacion controlador = new ControladorSimulacion(vPrincipal, dto);
        controlador.iniciar();

        vPrincipal.setVisible(true);
        this.dispose();
    }

    // Getters para consultar los valores desde el controlador o main
    public int getDuracionCiclo() {
        try {
            return Integer.parseInt(txtDuracionCiclo.getText().trim());
        } catch (NumberFormatException e) {
            return 1000;
        }
    }

    public int getLatenciaRed() {
        try {
            return Integer.parseInt(txtLatenciaRed.getText().trim());
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public int getNumComputadores() {
        try {
            return Integer.parseInt(txtNumComputadores.getText().trim());
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public int getCapacidadRAM() {
        try {
            return Integer.parseInt(txtCapacidadRAM.getText().trim());
        } catch (NumberFormatException e) {
            return 1024;
        }
    }
}