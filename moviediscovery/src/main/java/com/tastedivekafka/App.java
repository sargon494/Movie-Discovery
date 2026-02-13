package com.tastedivekafka;

import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.tastedivekafka.kafka.KafkaConsumerService;
import com.tastedivekafka.kafka.KafkaResponseConsumerService;
import com.tastedivekafka.ui.LoginFrame;
import com.tastedivekafka.ui.MainFrame;

/**
 * Clase principal de la aplicación MovieDiscovery.
 *
 * Funciones:
 *  - Arranca los consumers de Kafka (peticiones y respuestas)
 *  - Inicializa la interfaz gráfica (login + main frame)
 *  - Gestiona el flujo de autenticación de usuario
 */
public class App {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    /**
     * Método principal de la aplicación.
     *
     * @param args argumentos de línea de comandos (no utilizados)
     * @throws Exception en caso de error crítico al iniciar la app
     */
    public static void main(String[] args) throws Exception {

        // 1) Lanzar el consumer que escucha el topic de peticiones de películas
        new Thread(() -> {
            try {
                KafkaConsumerService consumerService = new KafkaConsumerService();
                consumerService.listen(); // Comienza a escuchar mensajes de Kafka
            } catch (Exception e) {
                LOGGER.severe(() -> "Error starting Kafka consumer: " + e.getMessage());
            }
        }).start();

        // 2) Crear el consumer de respuestas UNA sola vez
        // Este consumer será compartido con el MainFrame
        KafkaResponseConsumerService responseConsumer = new KafkaResponseConsumerService();

        // 3) Lanzar la interfaz gráfica de login en el hilo de Swing
        SwingUtilities.invokeLater(() -> {

            // Método para abrir login
            Runnable showLogin = new Runnable() {
                @Override
                public void run() {
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
                }
            };

            // Mostrar login por primera vez
            showLogin.run();
        });
    }
}