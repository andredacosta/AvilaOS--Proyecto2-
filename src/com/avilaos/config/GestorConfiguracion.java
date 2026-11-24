
package com.avilaos.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Elohym Casais
 */

public class GestorConfiguracion {
    private final Gson gson;

    public GestorConfiguracion() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void guardarConfiguracion(ConfiguracionSistemaDTO config, String rutaArchivo) throws IOException {
        try (FileWriter writer = new FileWriter(rutaArchivo)) {
            gson.toJson(config, writer);
        }
    }

    public ConfiguracionSistemaDTO cargarConfiguracion(String rutaArchivo) throws IOException {
        try (FileReader reader = new FileReader(rutaArchivo)) {
            return gson.fromJson(reader, ConfiguracionSistemaDTO.class);
        }
    }
}
