package com.tastedivekafka.api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Cliente para consumir la API de TasteDive.
 *
 * Esta clase se encarga de:
 *  - Construir la URL de petición
 *  - Codificar correctamente el nombre de la película
 *  - Realizar la petición HTTP
 *  - Devolver la respuesta en formato JSON (String)
 */
public class TasteDiveClient {

    /**
     * Clave de acceso a la API de TasteDive.
     * ⚠️ En un entorno real, esto debería ir en variables de entorno
     * o en un archivo de configuración, no en código.
     */
    private static final String API_KEY = "1066479-MovieDis-CD78C85B"; 

    /**
     * Realiza una petición a la API de TasteDive y obtiene recomendaciones
     * relacionadas con una película.
     *
     * @param movie nombre de la película a buscar
     * @return respuesta cruda en formato JSON (String)
     *         o un JSON de error si falla la petición
     */
    public String getRawRecommendations(String movie) {
        try {
            // Limpiar espacios y codificar el nombre de la película para URL
            String encodedMovie = URLEncoder.encode(movie.trim(), StandardCharsets.UTF_8);

            // Construcción de la URL de la API
            String url = "https://tastedive.com/api/similar?q="
                + encodedMovie
                + "&type=movie&info=1&k="
                + API_KEY;

            // Crear cliente HTTP
            HttpClient client = HttpClient.newHttpClient();

            // Construir la petición HTTP GET
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // Enviar petición y recibir respuesta
            HttpResponse<String> response = client.send(
                    request, 
                    HttpResponse.BodyHandlers.ofString()
            );

            // Devolver el JSON recibido
            return response.body();

        } catch (Exception e) {
            // En caso de error, devolver JSON controlado con mensaje de error
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}