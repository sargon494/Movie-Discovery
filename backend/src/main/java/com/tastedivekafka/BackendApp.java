package com.tastedivekafka;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.tastedivekafka.api.AuthServlet;
import com.tastedivekafka.api.HistoryServlet;
import com.tastedivekafka.api.ProfileServlet;
import com.tastedivekafka.api.SearchServlet;
import com.tastedivekafka.api.VerificationServlet;
import com.tastedivekafka.api.ViewedServlet;
import com.tastedivekafka.kafka.KafkaConsumerService;

public class BackendApp {

    private static final Logger LOGGER = Logger.getLogger(BackendApp.class.getName());
    private static final int PORT = 8090;

    public static void main(String[] args) throws Exception {

        new Thread(() -> {
            try {
                KafkaConsumerService consumer = new KafkaConsumerService();
                consumer.listen();
            } catch (Exception e) {
                LOGGER.severe(() -> "Kafka consumer failed: " + e.getMessage());
            }
        }, "kafka-consumer-thread").start();

        Server server = new Server(PORT);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        context.addServlet(ProfileServlet.class, "/profile/*");
        context.addServlet(ViewedServlet.class,  "/viewed");
        context.addServlet(HistoryServlet.class,  "/history");
        context.addServlet(new ServletHolder(new SearchServlet()), "/search");
        context.addServlet(new ServletHolder(new AuthServlet()),   "/auth/*");
        context.addServlet(VerificationServlet.class,                "/verify");

        server.setHandler(context);

        LOGGER.info("Backend starting on port " + PORT);
        server.start();
        server.join();
    }
}