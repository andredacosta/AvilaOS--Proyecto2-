/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.avilaos.avilaos;

import com.avilaos.gui.VentanaConfiguracion;
import javax.swing.SwingUtilities;

/**
 *
 * @author juand
 */
public class AvilaOS {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaConfiguracion vConfig = new VentanaConfiguracion();
            vConfig.setVisible(true);
        });
    }
}
