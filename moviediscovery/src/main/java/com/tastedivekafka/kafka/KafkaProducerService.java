package com.tastedivekafka.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.util.Properties;

/**
 * Servicio Kafka encargado de enviar peticiones de películas.
 *
 * Envía los nombres de películas al topic "movie-topic" para que
 * el consumer de backend las procese y genere recomendaciones.
 */
public class KafkaProducerService {
    // Productor Kafka
    private final KafkaProducer<String, String> producer;

    public KafkaProducerService() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "127.0.0.1:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        props.put("acks", "1");
        props.put("request.timeout.ms", "30000");
        props.put("delivery.timeout.ms", "45000");

        // Inicializamos producer
        producer = new KafkaProducer<>(props);
    }

    /**
     * Enviar nombre de película al topic "movie-topic".
     *
     * @param movie nombre de la película
     */
    public void send(String movie) {
        if (movie == null || movie.trim().isEmpty()) return;

        // Enviar mensaje al topic
        ProducerRecord<String, String> record = new ProducerRecord<>("movie-topic", movie);

        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println("✅ Mensaje enviado a: " + metadata.topic() + 
                                " | Partición: " + metadata.partition() + 
                                " | Offset: " + metadata.offset());
            } else {
                System.err.println("❌ Error al enviar mensaje: " + exception.getMessage());
                exception.printStackTrace();
            }
        });

        producer.flush(); 
    }

    /**
     * Cierra el producer de Kafka.
     */
    public void close() {
        if (producer != null) producer.close();
    }
}
