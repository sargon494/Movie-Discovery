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
 * Servicio Kafka que escucha el topic de respuestas "movie-responses".
 *
 * Se utiliza desde la UI para recibir recomendaciones de películas
 * enviadas por el backend.
 */
public class KafkaResponseConsumerService {
    private static final Logger logger = Logger.getLogger(KafkaResponseConsumerService.class.getName());
    private volatile boolean running = true;

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
    * Inicia el hilo de escucha de Kafka. 
    * Cada vez que se recibe una respuesta, se llama al callback proporcionado.
    * @param callback Función que procesa la respuesta recibida (ej. actualizar UI)
    */
    public void listen(Consumer<String> callback) {
        System.out.println("UI esperando respuestas en 'movie-responses'...");
        new Thread(() -> {
            try (KafkaConsumer<String, String> records = consumer) {
                while(running) {
                    ConsumerRecords<String, String> consumerRecords = records.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<String, String> record : consumerRecords) {
                        logger.info(() -> "Respuesta recibida: " + record.value());
                        callback.accept(record.value()); // Procesar respuesta con callback
                    }
                }
            } catch (Exception e) {
                logger.severe(() -> "Error en KafkaResponseConsumerService: " + e.getMessage());
            } finally {
                consumer.close();
                logger.info("KafkaResponseConsumerService detenido.");
            }
        }, "KafkaResponseListener").start();
    }

    /*
        * Nota: shutdown() se llama desde MainFrame cuando se cierra la ventana.
        * Esto evita que el hilo de escucha siga corriendo en segundo plano.
    */

    public void shutdown() {
        running = false;
        if (consumer != null) {
            consumer.wakeup(); // Despertar el consumer para cerrar
        }
    }

    /* 
        * Nota: Este servicio se instancia UNA SOLA VEZ en App.java y se comparte con MainFrame.
        * Esto evita problemas de múltiples consumers leyendo el mismo topic y permite centralizar
        * la gestión de respuestas en un solo lugar.
    */

    /**
     * Interfaz que define cómo manejar las respuestas recibidas.
     */
    public interface ResponseHandler {
        void onResponse(String response);
    }
}