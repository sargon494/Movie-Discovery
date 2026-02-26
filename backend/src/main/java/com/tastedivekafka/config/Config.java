package com.tastedivekafka.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lee la configuración de la aplicación.
 *
 * Orden de prioridad:
 *  1. Variable de entorno TASTEDIVE_API_KEY  ← usada en Docker
 *  2. config.properties                      ← usada en local (gitignored)
 *
 * Así el archivo config.properties nunca necesita estar en la imagen Docker
 * ni subirse a GitHub, pero sigue funcionando en desarrollo local.
 */
public class Config {

    private static final Properties props = new Properties();

    static {
        // Intentamos cargar config.properties solo si existe
        // En Docker no existirá — no lanzamos excepción si falta
        try (InputStream input = Config.class.getResourceAsStream("/config/config.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            System.out.println("config.properties no encontrado, usando variables de entorno");
        }
    }

    /**
     * Devuelve la API key de TasteDive.
     *
     * Primero mira la variable de entorno TASTEDIVE_API_KEY,
     * si no existe cae al valor en config.properties.
     */
    public static String getApiKey() {
        // 1. Variable de entorno (Docker)
        String envKey = System.getenv("TASTEDIVE_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        // 2. Archivo local (desarrollo)
        String propKey = props.getProperty("TASTEDIVE_API_KEY");
        if (propKey != null && !propKey.isBlank()) {
            return propKey;
        }
        throw new RuntimeException(
            "TASTEDIVE_API_KEY no encontrada en variables de entorno."
        );
    }
}