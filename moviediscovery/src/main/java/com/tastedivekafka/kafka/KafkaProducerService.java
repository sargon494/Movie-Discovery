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
        props.put("bootstrap.servers", "localhost:9092"); // broker Kafka
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

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
        producer.send(new ProducerRecord<>("movie-topic", movie));

        // Forzar envío inmediato
        producer.flush();
    }

    /**
     * Cierra el producer de Kafka.
     */
    public void close() {
        if (producer != null) producer.close();
    }
}
