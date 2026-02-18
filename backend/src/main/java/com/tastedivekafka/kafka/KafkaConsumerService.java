package com.tastedivekafka.kafka;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.tastedivekafka.api.TasteDiveClient;

/**
 * Servicio Kafka encargado de:
 * 1. Consumir peticiones de películas desde "movie-topic"
 * 2. Consultar la API de TasteDive
 * 3. Guardar datos en la base de datos
 * 4. Enviar recomendaciones al topic de respuesta "movie-responses"
 */
public class KafkaConsumerService {

    /*
        * ObjectMapper de Jackson para parsear JSON de la API, thread-safe y reutilizable  
    */
    private static final ObjectMapper MAPPER = new ObjectMapper();


    // Consumer para leer mensajes
    private final KafkaConsumer<String, String> consumer;
    // Producer para enviar resultados
    private final KafkaProducer<String, String> producer;
    // Flag para controlar el bucle principal
    private volatile boolean running = true;

    public KafkaConsumerService() {

        /* =========================
           CONFIGURACIÓN DEL CONSUMIDOR
           ========================= */
        Properties cProps = new Properties();
        String kafkaServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:29092");
        cProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers); // broker Kafka
        cProps.put(ConsumerConfig.GROUP_ID_CONFIG, "backend-processor-group"); // grupo de consumidores
        cProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // leer últimos mensajes si es nuevo

        // Commit manual de offsets tras procesar cada mensaje
        cProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // Deserializadores de Kafka
        cProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        cProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");

        // Creamos consumer y suscribimos al topic de peticiones
        consumer = new KafkaConsumer<>(cProps);
        consumer.subscribe(List.of("movie-topic"));

        /* =========================
           CONFIGURACIÓN DEL PRODUCTOR
           ========================= */
        Properties pProps = new Properties();
        pProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers); // broker Kafka
        pProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        pProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(pProps);

        /* =========================
           SHUTDOWN LIMPIO
           ========================= */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Cerrando Kafka...");
            running = false;
            consumer.wakeup(); // Despierta el poll si está bloqueado
        }));
    }

    // Cache Caffeine para evitar consultas repetidas
    private final Cache<String, String> cache = 
        Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    /**
     * Bucle principal de consumo
     */
    public void listen() throws SQLException {

        // Cliente de la API y DAOs de BD
        TasteDiveClient api = new TasteDiveClient();

        System.out.println("🚀 BACKEND LISTO");

        try (consumer) {
            while (running) {

                // Poll con timeout para no bloquear indefinidamente
                ConsumerRecords<String, String> records =
                        consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {

                    String movieQuery = record.value();

                    // Comprobar caché antes de llamar a la API
                    String cachedResponse = cache.getIfPresent(movieQuery);

                    if (cachedResponse != null) {
                        System.out.println("♻️ Respuesta en caché para: " + movieQuery);
                        // Enviar respuesta cacheada
                        producer.send(new ProducerRecord<>(
                                "movie-responses",
                                movieQuery,
                                cachedResponse
                        ));
                        // Commit del mensaje
                        consumer.commitSync();
                        continue;
                    }
                    System.out.println("📩 Petición: " + movieQuery);

                    try {
                        /* =========================
                           1. LLAMADA A LA API
                           ========================= */
                        String rawJson = api.getRawRecommendations(movieQuery);
                        System.out.println("📄 RAW JSON RESPONSE:");
                        System.out.println(rawJson);
                        JsonNode root = MAPPER.readTree(rawJson);

                        JsonNode similar = root.path("similar");
                        if (similar == null) {
                            throw new RuntimeException("Respuesta API inválida");
                        }

                        JsonNode results = similar.path("results");
                        System.out.println("Results size: " + results.size());
                        System.out.println("Results missing: " + results.isMissingNode());
                        System.out.println("Results is array: " + results.isArray());
                        if (results == null || results.size() == 0) {
                            System.out.println("⚠️ Sin resultados para " + movieQuery);
                            consumer.commitSync(); // commit para marcar mensaje como procesado
                            continue;
                        }

                        /* =========================
                           2. GUARDAR EN BASE DE DATOS
                           ========================= */
                        StringBuilder responseBuilder = new StringBuilder();

                        for (int i = 0; i < results.size(); i++) {
                            JsonNode item = results.get(i);

                            String name = item.path("name").asText("Desconocido");
                            String yID = item.path("yID").asText("");

                            String trailerUrl;
                            
                            if (!yID.isEmpty()) {
                                trailerUrl = "https://www.youtube.com/watch?v=" + yID;
                            } else {
                                trailerUrl = "https://www.youtube.com/results?search_query="
                                + URLEncoder.encode(name + " trailer", StandardCharsets.UTF_8);
                            }

                            String imgUrl;

                            if (!yID.isEmpty()) {
                                // YouTube thumbnail si existe
                                imgUrl = "https://img.youtube.com/vi/" + yID + "/0.jpg";
                            } else {
                                // Placeholder seguro
                                String placeholderText = name.trim().replaceAll("[^\\w\\s]", "");
                                placeholderText = URLEncoder.encode(placeholderText, StandardCharsets.UTF_8);
                                imgUrl = "https://dummyimage.com/140x200/cccccc/000000&text=" + placeholderText;
                            }

                            // Construir respuesta para Kafka
                            responseBuilder.append(name)
                                    .append("||")
                                    .append(imgUrl)
                                    .append("||")
                                    .append(trailerUrl);

                            if (i < results.size() - 1) {
                                responseBuilder.append(";;");
                            }
                        }
                        
                        // Guardar en caché la respuesta
                        String finalResponse = responseBuilder.toString();
                        cache.put(movieQuery, finalResponse);

                        // Enviar mensaje al topic de respuestas
                        producer.send(new ProducerRecord<>(
                                "movie-responses",
                                movieQuery,
                                finalResponse
                        ));

                        // Commit SOLO si todo ha ido bien
                        consumer.commitSync();
                        System.out.println("✅ Procesado OK");

                    } catch (Exception e) {
                        System.err.println("❌ Error procesando '" + movieQuery + "': " + e.getMessage());

                        // Enviar al topic de errores
                        producer.send(new ProducerRecord<>(
                                "movie-errors",
                                movieQuery,
                                e.getMessage()
                        ));
                        consumer.commitSync();
                    }
                }
            }
        } catch (WakeupException e) {
            System.out.println("🟡 Wakeup recibido, cerrando...");
        } finally {
            producer.flush();
            producer.close();
            System.out.println("✅ Kafka cerrado correctamente");
        }
    }
}
