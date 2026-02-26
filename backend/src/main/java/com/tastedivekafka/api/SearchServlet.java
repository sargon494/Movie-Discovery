package com.tastedivekafka.api;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.tastedivekafka.kafka.KafkaProducerService;
import com.tastedivekafka.kafka.KafkaResponseConsumerService;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * POST /search — consumer compartido para respuestas rápidas
 */
public class SearchServlet extends HttpServlet {

    private final KafkaProducerService producer = new KafkaProducerService();
    private static KafkaResponseConsumerService sharedConsumer;
    private static final Map<String, BlockingQueue<String>> pendingRequests = new ConcurrentHashMap<>();

    @Override
    public void init() {
        if (sharedConsumer == null) {
            sharedConsumer = new KafkaResponseConsumerService();
            sharedConsumer.listen(response -> {
                for (BlockingQueue<String> queue : pendingRequests.values()) {
                    queue.offer(response);
                }
            });
            System.out.println("✅ SearchServlet consumer compartido iniciado");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String movie = req.getReader().readLine();
        if (movie == null || movie.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Título vacío");
            return;
        }
        movie = movie.trim();

        String requestId = UUID.randomUUID().toString();
        BlockingQueue<String> resultQueue = new ArrayBlockingQueue<>(1);
        pendingRequests.put(requestId, resultQueue);

        try {
            producer.send(movie);            
            String result = resultQueue.poll(20, TimeUnit.SECONDS);
            
            if (result == null) {
                resp.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT, "Timeout esperando respuesta");
                return;
            }

            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Interrumpido");
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    @Override
    public void destroy() {
        if (sharedConsumer != null) {
            sharedConsumer.shutdown();
        }
    }
}
