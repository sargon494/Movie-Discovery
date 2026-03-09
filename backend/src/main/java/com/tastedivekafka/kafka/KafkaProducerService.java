package com.tastedivekafka.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

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
        // Propiedades base (incluye SSL si las variables de entorno están definidas)
        Properties props = KafkaConfig.baseProperties();

        // Propiedades específicas del producer
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.ACKS_CONFIG,                    "1");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,      "30000");
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,     "45000");

        System.out.println("[DEBUG] bootstrap.servers = '" 
            + props.getProperty("bootstrap.servers") + "'");
        System.out.println("[DEBUG] security.protocol = '" 
            + props.getProperty("security.protocol") + "'");


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
                System.err.println("Error al enviar mensaje: " + exception.getMessage());
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
