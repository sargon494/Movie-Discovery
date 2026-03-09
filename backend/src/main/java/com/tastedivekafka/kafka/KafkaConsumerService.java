package com.tastedivekafka.kafka;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tastedivekafka.api.TasteDiveClient;

/**
 * Servicio Kafka encargado de:
 * 1. Consumir peticiones de películas desde "movie-topic"
 * 2. Consultar la API de TasteDive
 * 3. Enviar recomendaciones al topic "movie-responses"
 *
 * Usa KafkaConfig para obtener las propiedades base (local o Aiven SSL).
 */
public class KafkaConsumerService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> producer;
    private volatile boolean running = true;

    private final Cache<String, String> cache =
        Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    public KafkaConsumerService() {

        // ── Consumer ─────────────────────────────────────────────────
        Properties cProps = KafkaConfig.baseProperties();   // ← base compartida
        cProps.put(ConsumerConfig.GROUP_ID_CONFIG,              "backend-processor-group");
        cProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,     "latest");
        cProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,    "false");
        cProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        cProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(cProps);
        consumer.subscribe(List.of("movie-topic"));

        // ── Producer ─────────────────────────────────────────────────
        Properties pProps = KafkaConfig.baseProperties();   // ← base compartida
        pProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringSerializer");
        pProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(pProps);

        // ── Shutdown limpio ───────────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Cerrando Kafka...");
            running = false;
            consumer.wakeup();
        }));
    }

    public void listen() throws SQLException {
        TasteDiveClient api = new TasteDiveClient();
        System.out.println("BACKEND LISTO");

        try (consumer) {
            while (running) {
                ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {
                    String movieQuery = record.value();

                    // Caché
                    String cached = cache.getIfPresent(movieQuery);
                    if (cached != null) {
                        System.out.println("Caché para: " + movieQuery);
                        producer.send(new ProducerRecord<>("movie-responses", movieQuery, cached));
                        consumer.commitSync();
                        continue;
                    }

                    System.out.println("Petición: " + movieQuery);

                    try {
                        String rawJson = api.getRawRecommendations(movieQuery);
                        System.out.println("RAW JSON: " + rawJson);

                        JsonNode root    = MAPPER.readTree(rawJson);
                        JsonNode similar = root.path("similar");
                        if (similar == null) throw new RuntimeException("Respuesta API inválida");

                        JsonNode results = similar.path("results");
                        if (results == null || results.size() == 0) {
                            System.out.println("⚠️ Sin resultados para " + movieQuery);
                            consumer.commitSync();
                            continue;
                        }

                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < results.size(); i++) {
                            JsonNode item = results.get(i);
                            String name = item.path("name").asText("Desconocido");
                            String yID  = item.path("yID").asText("");

                            String trailerUrl = !yID.isEmpty()
                                ? "https://www.youtube.com/watch?v=" + yID
                                : "https://www.youtube.com/results?search_query="
                                    + URLEncoder.encode(name + " trailer", StandardCharsets.UTF_8);

                            String imgUrl = !yID.isEmpty()
                                ? "https://img.youtube.com/vi/" + yID + "/0.jpg"
                                : "https://dummyimage.com/140x200/cccccc/000000&text="
                                    + URLEncoder.encode(
                                        name.trim().replaceAll("[^\\w\\s]", ""),
                                        StandardCharsets.UTF_8);

                            sb.append(name).append("||").append(imgUrl).append("||").append(trailerUrl);
                            if (i < results.size() - 1) sb.append(";;");
                        }

                        String response = sb.toString();
                        cache.put(movieQuery, response);
                        producer.send(new ProducerRecord<>("movie-responses", movieQuery, response));
                        consumer.commitSync();
                        System.out.println("✅ Procesado OK");

                    } catch (JsonProcessingException | RuntimeException e) {
                        System.err.println("❌ Error: " + e.getMessage());
                        producer.send(new ProducerRecord<>("movie-errors", movieQuery, e.getMessage()));
                        consumer.commitSync();
                    }
                }
            }
        } catch (WakeupException e) {
            System.out.println("Wakeup recibido, cerrando...");
        } finally {
            try (producer) {
                producer.flush();
            }
            System.out.println("Kafka cerrado correctamente");
        }
    }
}