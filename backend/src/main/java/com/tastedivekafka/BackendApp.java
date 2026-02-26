package com.tastedivekafka;

import com.tastedivekafka.api.SearchServlet;
import com.tastedivekafka.api.AuthServlet;
import com.tastedivekafka.kafka.KafkaConsumerService;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.logging.Logger;

/**
 * Hace de puente entre el frontend y Kafka. 
 * Expone un servidor HTTP con endpoints REST que el frontend puede llamar, y se encarga de enviar los mensajes a Kafka y devolver las respuestas al frontend.
 *
 * Responsabilidades:
 * - Exponer endpoints HTTP para el frontend (search, login, register)
 * - Enviar las consultas de búsqueda al tópico de Kafka "movie-requests"
 * - Escuchar las respuestas de Kafka en el tópico "movie-responses" y devolverlas
 * - Validar las credenciales de usuario y registrar nuevos usuarios usando UserDAO
 * - Manejar errores de forma robusta, devolviendo códigos HTTP adecuados al frontend
 *
 * Endpoints:
 *  POST /search          → SearchServlet   (envia título a Kafka, espera respuesta, la devuelve al frontend)
 *  POST /auth/login      → AuthServlet     (valida las credenciales contra la base de datos usando UserDAO)
 *  POST /auth/register   → AuthServlet     (registra un nuevo usuario en la base de datos usando UserDAO)
 */
public class BackendApp {

    private static final Logger LOGGER = Logger.getLogger(BackendApp.class.getName());
    private static final int PORT = 8090;

    public static void main(String[] args) throws Exception {

        // ── 1. Kafka consumer thread ──────────────────────────────────────────
        // Escucha en un hilo separado para no bloquear el servidor HTTP. Si el consumidor falla, se registra el error pero el servidor sigue funcionando.
        // Se ejecuta indepedientemente del servidor HTTP, lo que permite que el backend siga respondiendo a las solicitudes incluso si hay problemas con Kafka.
        new Thread(() -> {
            try {
                KafkaConsumerService consumer = new KafkaConsumerService();
                consumer.listen();
            } catch (Exception e) {
                LOGGER.severe(() -> "Kafka consumer failed: " + e.getMessage());
            }
        }, "kafka-consumer-thread").start();

        // ── 2. Jetty HTTP server ──────────────────────────────────────────────
        Server server = new Server(PORT);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        // POST /search  — recibe título de película, lo envía a Kafka y espera la respuesta para devolverla al frontend
        context.addServlet(new ServletHolder(new SearchServlet()), "/search");

        // POST /auth/login    — valida las credenciales contra la base de datos usando UserDAO

        // POST /auth/register — registra un nuevo usuario en la base de datos usando UserDAO
        context.addServlet(new ServletHolder(new AuthServlet()), "/auth/*");

        server.setHandler(context);

        LOGGER.info("Backend starting on port " + PORT);
        server.start();
        server.join(); // Bloquea el hilo principal para mantener el servidor en ejecución
    }
}