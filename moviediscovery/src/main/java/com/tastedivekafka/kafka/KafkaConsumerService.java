package com.tastedivekafka.kafka;

import com.tastedivekafka.api.TasteDiveClient;
import com.tastedivekafka.db.MovieDAO;
import com.tastedivekafka.db.RecommendationDAO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class KafkaConsumerService {

    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> responseProducer;

    public KafkaConsumerService() {
        System.out.println("KafkaConsumerService -> INICIANDO consumer...");

        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", "localhost:9092");
        consumerProps.put("group.id", "group1");
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("movie-topic"));

        System.out.println("KafkaConsumerService -> SUBSCRIBED a movie-topic");

        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:9092");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        responseProducer = new KafkaProducer<>(producerProps);
    }

    public void listen() throws Exception {
        System.out.println("KafkaConsumerService -> LISTENING...");

        TasteDiveClient api = new TasteDiveClient();
        MovieDAO movieDAO = new MovieDAO();
        RecommendationDAO recDAO = new RecommendationDAO();

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            System.out.println("KafkaConsumerService -> poll() ejecutado. Records: " + records.count());

            for (ConsumerRecord<String, String> record : records) {
                System.out.println("Mensaje recibido: " + record.value());

                String movie = record.value();

                int movieId = movieDAO.getOrCreateMovie(movie);

                List<String> recs = api.getRecommendations(movie);

                for (String r : recs) {
                    recDAO.save(movieId, r);
                }

                String response = movie + " -> " + String.join(", ", recs);

                responseProducer.send(
                        new ProducerRecord<>("movie-responses", movie, response),
                        (metadata, exception) -> {
                            if (exception != null) {
                                System.err.println("ERROR al enviar a movie-responses: " + exception.getMessage());
                            } else {
                                System.out.println("Enviado a movie-responses -> topic=" + metadata.topic() +
                                        " partition=" + metadata.partition() +
                                        " offset=" + metadata.offset());
                            }
                        }
                );

                responseProducer.flush();
            }
        }
    }
}