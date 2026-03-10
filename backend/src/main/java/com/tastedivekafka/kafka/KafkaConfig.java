package com.tastedivekafka.kafka;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;

/**
 * Centraliza la configuración de Kafka para producer y consumer.
 *
 * Soporta dos modos:
 *
 * ── LOCAL (docker-compose, sin SSL) ─────────────────────────────────
 *   KAFKA_BOOTSTRAP_SERVERS=kafka:29092
 *   (sin variables KAFKA_SSL_*)
 *
 * ── AIVEN (Client Certificate, mTLS) ────────────────────────────────
 *   KAFKA_BOOTSTRAP_SERVERS=kafka-mvdv-xxxxx.aivencloud.com:XXXXX
 *   KAFKA_SSL_CA_CERT_PATH   → /certs/ca.pem       (docker local)
 *   KAFKA_SSL_CERT_PATH      → /certs/service.cert  (docker local)
 *   KAFKA_SSL_KEY_PATH       → /certs/service.key   (docker local)
 *
 *   KAFKA_SSL_CA_CERT        → contenido ca.pem      (Render)
 *   KAFKA_SSL_CLIENT_CERT    → contenido service.cert (Render)
 *   KAFKA_SSL_CLIENT_KEY     → contenido service.key  (Render)
 *
 * Requisito: service.key debe estar en formato PKCS8
 *   (primera línea: -----BEGIN PRIVATE KEY-----)
 * Aiven genera las claves en PKCS8 por defecto — Java lo soporta nativamente.
 */
public class KafkaConfig {

    private KafkaConfig() { }

    private static final String KS_PASS = "kafkatemp";

    public static Properties baseProperties() {
        Properties props = new Properties();

        String bootstrapServers = System.getenv()
            .getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:29092");
        props.put("bootstrap.servers", bootstrapServers);

        String caCert     = loadCert("KAFKA_SSL_CA_CERT",     "KAFKA_SSL_CA_CERT_PATH");
        String clientCert = loadCert("KAFKA_SSL_CLIENT_CERT", "KAFKA_SSL_CERT_PATH");
        String clientKey  = loadCert("KAFKA_SSL_CLIENT_KEY",  "KAFKA_SSL_KEY_PATH");

        if (caCert != null && clientCert != null && clientKey != null) {
            System.out.println("[Kafka] Modo SSL activado (Aiven mTLS)");
            try {
                configureSSL(props, caCert, clientCert, clientKey);
                System.out.println("[Kafka] Keystores PKCS12 creados correctamente.");
            } catch (Exception e) {
                System.err.println("[Kafka] Error configurando SSL: " + e.getMessage());
                throw new RuntimeException("No se pudo configurar SSL para Kafka", e);
            }
        } else {
            System.out.println("[Kafka] Modo PLAINTEXT (local sin SSL)");
        }

        return props;
    }

    // ─────────────────────────────────────────────────────────────────── //
    //  SSL — construcción de keystores PKCS12
    // ─────────────────────────────────────────────────────────────────── //

    private static void configureSSL(Properties props,
                                     String caCertPem,
                                     String clientCertPem,
                                     String clientKeyPem) throws Exception {
        File trustStoreFile = buildTrustStore(caCertPem);
        File keyStoreFile   = buildKeyStore(clientCertPem, clientKeyPem);

        props.put("security.protocol",                     "SSL");
        props.put("ssl.truststore.location",                trustStoreFile.getAbsolutePath());
        props.put("ssl.truststore.password",                KS_PASS);
        props.put("ssl.truststore.type",                   "PKCS12");
        props.put("ssl.keystore.location",                  keyStoreFile.getAbsolutePath());
        props.put("ssl.keystore.password",                  KS_PASS);
        props.put("ssl.keystore.type",                     "PKCS12");
        props.put("ssl.key.password",                       KS_PASS);
        props.put("ssl.endpoint.identification.algorithm",  "https");
    }

    private static File buildTrustStore(String caCertPem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate caCert    = cf.generateCertificate(
            new ByteArrayInputStream(normalize(caCertPem).getBytes("UTF-8")));

        KeyStore ts = KeyStore.getInstance("PKCS12");
        ts.load(null, KS_PASS.toCharArray());
        ts.setCertificateEntry("aiven-ca", caCert);

        return saveKeyStore(ts, "kafka-truststore");
    }

    private static File buildKeyStore(String clientCertPem,
                                      String clientKeyPem) throws Exception {
        // Certificado de cliente
        CertificateFactory cf  = CertificateFactory.getInstance("X.509");
        Certificate clientCert = cf.generateCertificate(
            new ByteArrayInputStream(normalize(clientCertPem).getBytes("UTF-8")));

        // Clave privada PKCS8 — -----BEGIN PRIVATE KEY-----
        // Java soporta PKCS8 nativamente sin librerías externas
        String cleanKey = normalize(clientKeyPem)
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----",   "")
            .replaceAll("\\s+", "");

        byte[] keyBytes  = Base64.getDecoder().decode(cleanKey);
        PrivateKey pKey  = KeyFactory.getInstance("RSA")
            .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, KS_PASS.toCharArray());
        ks.setKeyEntry("client-key", pKey, KS_PASS.toCharArray(),
            new Certificate[]{ clientCert });

        return saveKeyStore(ks, "kafka-keystore");
    }

    private static File saveKeyStore(KeyStore ks, String prefix) throws Exception {
        File tmp = File.createTempFile(prefix, ".p12");
        tmp.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            ks.store(fos, KS_PASS.toCharArray());
        }
        return tmp;
    }

    // ─────────────────────────────────────────────────────────────────── //
    //  Helpers
    // ─────────────────────────────────────────────────────────────────── //

    private static String loadCert(String envVarName, String pathVarName) {
        String content = System.getenv(envVarName);
        if (content != null && !content.isBlank()) return content;

        String path = System.getenv(pathVarName);
        if (path != null && !path.isBlank()) {
            try {
                return Files.readString(Path.of(path));
            } catch (IOException e) {
                System.err.println("[Kafka] No se pudo leer: " + path
                    + " — " + e.getMessage());
            }
        }
        return null;
    }

    private static String normalize(String pem) {
        return pem.replace("\r\n", "\n").replace("\r", "\n").trim();
    }
}