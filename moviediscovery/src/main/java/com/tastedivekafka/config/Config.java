package com.tastedivekafka.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static Properties props = new Properties();

    static {
        try (InputStream input = Config.class.getResourceAsStream("/config.properties")) {
            if (input == null) {
                throw new RuntimeException("No se encontró config.properties");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando config.properties: " + e.getMessage(), e);
        }
    }

    public static String getApiKey() {
        return props.getProperty("TASTEDIVE_API_KEY");
    }
}