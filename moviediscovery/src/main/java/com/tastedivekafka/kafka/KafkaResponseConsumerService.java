package com.tastedivekafka.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Servicio Kafka que escucha el topic de respuestas "movie-responses".
 *
 * Se utiliza desde la UI para recibir recomendaciones de películas
 * enviadas por el backend.
 */
public class KafkaResponseConsumerService {
    private static final Logger logger = Logger.getLogger(KafkaResponseConsumerService.class.getName());

    // Consumer Kafka para escuchar respuestas
    private final KafkaConsumer<String, String> consumer;

    public KafkaResponseConsumerService() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"); // broker Kafka
        // Usar un ID aleatorio para que Kafka lo trate como cliente nuevo
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ui-client-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // leer solo nuevos mensajes
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        // Creamos el consumer y nos suscribimos al topic de respuestas
        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(List.of("movie-responses"));
    }

    /**
     * Bucle que escucha continuamente el topic de respuestas.
     *
     * @param handler callback que procesa cada mensaje recibido
     */
    public void listen(ResponseHandler handler) {
        System.out.println("👂 UI esperando respuestas en 'movie-responses'...");
        try (consumer) {
            while (true) {
                // Poll con 1 segundo de espera para dar tiempo a la conexión inicial
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("✨ UI RECIBIÓ: " + record.value());
                    // Llamamos al handler que la UI haya definido
                    handler.onResponse(record.value());
                }
            }
        } catch (Exception e) {
            // Log de errores
            logger.severe(() -> "Error listening to Kafka responses: " + e.getMessage());
        }
    }

    /**
     * Interfaz que define cómo manejar las respuestas recibidas.
     */
    public interface ResponseHandler {
        void onResponse(String response);
    }
}