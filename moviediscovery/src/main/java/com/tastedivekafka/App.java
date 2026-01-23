package com.tastedivekafka;

import com.tastedivekafka.kafka.KafkaConsumerService;
import com.tastedivekafka.kafka.KafkaResponseConsumerService;
import com.tastedivekafka.ui.LoginFrame;

import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {

        // ===== 1) Consumer principal (movie-topic) =====
        new Thread(() -> {
            try {
                KafkaConsumerService consumerService = new KafkaConsumerService();
                consumerService.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // ===== 2) Consumer de respuestas (SINGLETON de facto) =====
        KafkaResponseConsumerService responseConsumer = new KafkaResponseConsumerService();

        // ===== 3) UI =====
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(responseConsumer);
            loginFrame.setVisible(true);
        });
    }
}
