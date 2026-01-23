package com.tastedivekafka.kafka;

import com.tastedivekafka.api.TasteDiveClient;
import com.tastedivekafka.db.MovieDAO;
import com.tastedivekafka.db.RecommendationDAO;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class KafkaConsumerService {

    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> producer;

    public KafkaConsumerService() {

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, 
                "movie-workers");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, 
                "false");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, 
                "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("movie-topic"));

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(producerProps);
    }

    public void listen() {

        TasteDiveClient api = new TasteDiveClient();
        MovieDAO movieDAO = new MovieDAO();
        RecommendationDAO recDAO = new RecommendationDAO();

        while (true) {
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(500));

            try {
                for (ConsumerRecord<String, String> record : records) {

                    String movie = record.value();
                    int movieId = movieDAO.getOrCreateMovie(movie);

                    List<String> recommendations = api.getRecommendations(movie);
                    for (String r : recommendations) {
                        recDAO.save(movieId, r);
                    }

                    String response = movie + " -> " + String.join(", ", recommendations);

                    producer.send(
                            new ProducerRecord<>("movie-responses", movie, response)
                    );
                }

                // SOLO confirmamos si todo ha ido bien
                consumer.commitSync();

            } catch (Exception e) {
                System.err.println("Error procesando mensaje, no se confirma offset");
                e.printStackTrace();
            }
        }
    }
}
