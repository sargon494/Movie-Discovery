package com.tastedivekafka.ui;

import com.tastedivekafka.kafka.KafkaProducerService;
import com.tastedivekafka.kafka.KafkaResponseConsumerService;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final JTextField txtMovie = new JTextField();
    private final JTextArea txtArea = new JTextArea();
    private final KafkaProducerService producer = new KafkaProducerService();

    public MainFrame() {
        this(new KafkaResponseConsumerService());
    }

    public MainFrame(KafkaResponseConsumerService responseConsumer) {
        setTitle("TasteDive Kafka App");
        setSize(500, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(245, 245, 245));
        setContentPane(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ===== Etiqueta y campo de película =====
        JLabel lblMovie = new JLabel("Película:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        mainPanel.add(lblMovie, gbc);

        txtMovie.setPreferredSize(new Dimension(200, 30));
        addPlaceholder(txtMovie, "Ingrese el nombre de la película");
        gbc.gridx = 1;
        gbc.weightx = 1;
        mainPanel.add(txtMovie, gbc);

        // ===== Botón Buscar =====
        JButton btnSearch = new JButton("Buscar");
        btnSearch.setBackground(new Color(70, 130, 180));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        btnSearch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btnSearch.setBackground(new Color(100, 160, 210));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                btnSearch.setBackground(new Color(70, 130, 180));
            }
        });

        gbc.gridx = 2;
        gbc.weightx = 0;
        mainPanel.add(btnSearch, gbc);

        // ===== Área de texto con scroll =====
        txtArea.setEditable(false);
        txtArea.setLineWrap(true);
        txtArea.setWrapStyleWord(true);
        txtArea.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        JScrollPane scrollPane = new JScrollPane(txtArea);
        scrollPane.setPreferredSize(new Dimension(450, 350));

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        mainPanel.add(scrollPane, gbc);

        // ===== Consumidor de Kafka =====
        new Thread(() -> {
            responseConsumer.listen(response -> {
                SwingUtilities.invokeLater(() -> {
                    txtArea.setText("✅ Respuesta recibida:\n" + response);
                });
            });
        }).start();

        // ===== Acción del botón =====
        btnSearch.addActionListener(e -> {
            String movie = txtMovie.getText().trim();
            if (movie.isEmpty() || movie.equals("Ingrese el nombre de la película")) {
                txtArea.setText("❗ Introduce una película antes de buscar.");
                return;
            }

            producer.send(movie);
            txtArea.setText("🔄 Buscando en TasteDive...\n(Procesado por Kafka)");
        });
    }

    private void addPlaceholder(JTextField field, String placeholder) {
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
    }
}