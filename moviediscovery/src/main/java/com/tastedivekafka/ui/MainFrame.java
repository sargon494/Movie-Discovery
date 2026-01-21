package com.tastedivekafka.ui;

import com.tastedivekafka.kafka.KafkaProducerService;
import com.tastedivekafka.kafka.KafkaResponseConsumerService;

import javax.swing.*;

public class MainFrame extends JFrame {

    private final JTextField txtMovie = new JTextField();
    private final JTextArea txtArea = new JTextArea();

    // 1 producer global
    private final KafkaProducerService producer = new KafkaProducerService();

    // Constructor vacío
    public MainFrame() {
        this(new KafkaResponseConsumerService());
    }

    public MainFrame(KafkaResponseConsumerService responseConsumer) {
        setTitle("TasteDive Kafka App");
        setSize(450, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        JLabel lbl = new JLabel("Película:");
        lbl.setBounds(20, 20, 80, 25);
        add(lbl);

        txtMovie.setBounds(100, 20, 180, 25);
        add(txtMovie);

        JButton btnSearch = new JButton("Buscar");
        btnSearch.setBounds(290, 20, 100, 25);
        add(btnSearch);

        txtArea.setBounds(20, 100, 400, 300);
        add(txtArea);

        // 1) Consumidor de respuestas
        new Thread(() -> {
            responseConsumer.listen(response -> {
                SwingUtilities.invokeLater(() -> {
                    txtArea.setText("Respuesta recibida:\n" + response);
                });
            });
        }).start();

        // 2) Enviar evento a Kafka
        btnSearch.addActionListener(e -> {
            String movie = txtMovie.getText().trim();

            if (movie.isEmpty()) {
                txtArea.setText("❗ Introduce una película antes de buscar.");
                return;
            }

            producer.send(movie);
            txtArea.setText("Buscando en TasteDive...\n(Procesado por Kafka)");
        });
    }
}