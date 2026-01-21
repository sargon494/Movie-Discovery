package com.tastedivekafka;

import javax.swing.JOptionPane;

import com.tastedivekafka.kafka.KafkaConsumerService;
import com.tastedivekafka.kafka.KafkaResponseConsumerService;
import com.tastedivekafka.ui.LoginFrame;
import com.tastedivekafka.ui.MainFrame;

public class App {

    public static void main(String[] args) throws Exception {

        // 1) Lanzar consumer que procesa movie-topic
        new Thread(() -> {
            try {
                KafkaConsumerService consumerService = new KafkaConsumerService();
                consumerService.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // 2) Crear consumer de respuestas UNA sola vez
        KafkaResponseConsumerService responseConsumer = new KafkaResponseConsumerService();

        // 3) Lanzar LoginFrame
        javax.swing.SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(new LoginFrame.LoginListener() {
                @Override
                public void onLoginSuccess() {
                    // Abrir MainFrame solo si login OK
                    MainFrame main = new MainFrame(responseConsumer);
                    main.setVisible(true);
                }

                @Override
                public void onLoginFailure(String reason) {
                    JOptionPane.showMessageDialog(null, reason);
                }
            });

            loginFrame.setVisible(true);
        });
    }
}