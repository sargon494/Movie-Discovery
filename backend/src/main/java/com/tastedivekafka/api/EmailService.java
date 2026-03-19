package com.tastedivekafka.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Servicio de email usando Resend (https://resend.com).
 * Requiere variable de entorno RESEND_API_KEY.
 * Plan gratuito: 3000 emails/mes.
 */
public class EmailService {

    private static final String API_URL  = "https://api.resend.com/emails";
    private static final String API_KEY  = System.getenv("RESEND_API_KEY");
    private static final String FROM     = System.getenv().getOrDefault(
        "EMAIL_FROM", "MovieDiscovery <onboarding@resend.dev>");
    private static final String APP_URL  = System.getenv().getOrDefault(
        "APP_URL", "https://movie-discovery-nf4s.onrender.com");

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Envía el correo de verificación al usuario recién registrado.
     *
     * @param toEmail   email del destinatario
     * @param username  nombre de usuario
     * @param token     token UUID de verificación
     */
    public static void sendVerificationEmail(String toEmail, String username, String token)
            throws Exception {

        String verifyUrl = APP_URL + "/verify?token=" + token;

        String html = """
            <div style="font-family:sans-serif;max-width:480px;margin:auto;padding:32px;
                        background:#12121a;color:#e0e0e8;border-radius:12px;">
              <h2 style="color:#639bff;margin-top:0">Verifica tu cuenta</h2>
              <p>Hola <strong>%s</strong>,</p>
              <p>Gracias por registrarte en MovieDiscovery.<br>
                 Haz clic en el botón para activar tu cuenta:</p>
              <a href="%s"
                 style="display:inline-block;margin:16px 0;padding:12px 28px;
                        background:#639bff;color:#fff;text-decoration:none;
                        border-radius:8px;font-weight:bold;">
                Verificar cuenta
              </a>
              <p style="color:#78788a;font-size:12px">
                El enlace expira en 24 horas.<br>
                Si no creaste esta cuenta, ignora este mensaje.
              </p>
            </div>
            """.formatted(username, verifyUrl);

        String body = """
            {
              "from": "%s",
              "to": ["%s"],
              "subject": "Verifica tu cuenta de MovieDiscovery",
              "html": %s
            }
            """.formatted(FROM, toEmail, toJson(html));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = HTTP.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new Exception("Resend error " + response.statusCode() + ": " + response.body());
        }

        System.out.println("[Email] Verificación enviada a: " + toEmail);
    }

    /** Escapa el HTML como string JSON */
    private static String toJson(String html) {
        return "\"" + html
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "") + "\"";
    }
}
