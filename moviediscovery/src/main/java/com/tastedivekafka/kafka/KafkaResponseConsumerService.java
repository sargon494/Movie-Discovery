package com.tastedivekafka.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class KafkaResponseConsumerService {

    private final KafkaConsumer<String, String> consumer;

    public KafkaResponseConsumerService() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "group-responses");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("movie-responses"));

        System.out.println("KafkaResponseConsumerService -> SUBSCRIBED a movie-responses");
    }

    public void listen(ResponseHandler handler) {

        // Primer poll para asignar particiones
        consumer.poll(Duration.ofMillis(100));

        // Resetear al inicio
        consumer.seekToBeginning(consumer.assignment());

        System.out.println("KafkaResponseConsumerService -> LISTENING...");

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            System.out.println("KafkaResponseConsumerService -> poll() ejecutado. Records: " + records.count());

            for (ConsumerRecord<String, String> record : records) {
                System.out.println("KafkaResponseConsumerService -> recibido: " + record.value());
                handler.onResponse(record.value());
            }
        }
    }

    public interface ResponseHandler {
        void onResponse(String response);
    }
}