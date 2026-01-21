package com.tastedivekafka.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaProducerService {

    private final KafkaProducer<String, String> producer;

    public KafkaProducerService() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(props);
    }

    public void send(String movie) {
        if (movie == null || movie.trim().isEmpty()) {
            System.out.println("No se envía mensaje vacío");
            return;
        }

        producer.send(new ProducerRecord<>("movie-topic", movie), (metadata, exception) -> {
            if (exception != null) {
                exception.printStackTrace();
            } else {
                System.out.println("Mensaje enviado a Kafka: topic=" + metadata.topic() +
                                   " partition=" + metadata.partition() +
                                   " offset=" + metadata.offset());
            }
        });

        producer.flush();
    }
}