package com.tastedivekafka.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Escucha el topic "movie-responses" y entrega las respuestas al callback.
 *
 * CORRECCIÓN: ahora usa KafkaConfig.baseProperties() en lugar de
 * construir las propiedades manualmente. Antes no tenía SSL configurado,
 * por lo que Aiven rechazaba silenciosamente la conexión y el frontend
 * nunca recibía las respuestas aunque el backend las procesara bien.
 */
public class KafkaResponseConsumerService {

    private static final Logger logger =
        Logger.getLogger(KafkaResponseConsumerService.class.getName());

    private volatile boolean running = true;
    private final KafkaConsumer<String, String> consumer;

    public KafkaResponseConsumerService() {
        // ← KafkaConfig incluye SSL si las variables de entorno están definidas
        Properties props = KafkaConfig.baseProperties();

        // Group ID único por instancia para leer siempre desde "latest"
        // sin compartir offset con otros consumers del mismo grupo
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
            "ui-client-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,     "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");

        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(List.of("movie-responses"));
    }

    /**
     * Inicia el hilo de escucha en segundo plano.
     * Cada respuesta recibida se pasa al callback.
     */
    public void listen(Consumer<String> callback) {
        System.out.println("UI esperando respuestas en 'movie-responses'...");
        new Thread(() -> {
            try (KafkaConsumer<String, String> c = consumer) {
                while (running) {
                    ConsumerRecords<String, String> records =
                        c.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<String, String> record : records) {
                        logger.info(() -> "Respuesta recibida: " + record.value());
                        callback.accept(record.value());
                    }
                }
            } catch (Exception e) {
                logger.severe(() -> "Error en KafkaResponseConsumerService: "
                    + e.getMessage());
            } finally {
                logger.info("KafkaResponseConsumerService detenido.");
            }
        }, "KafkaResponseListener").start();
    }

    public void shutdown() {
        running = false;
        if (consumer != null) consumer.wakeup();
    }
}