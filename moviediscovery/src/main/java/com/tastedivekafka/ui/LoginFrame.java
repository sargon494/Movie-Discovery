package com.tastedivekafka.ui;

import com.tastedivekafka.controller.LoginController;
import com.tastedivekafka.kafka.KafkaResponseConsumerService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginFrame extends JFrame {

    private final LoginController controller;
    private final KafkaResponseConsumerService responseConsumer;

    private JTextField userField;
    private JPasswordField passwordField;
    private int xMouse, yMouse;

    public LoginFrame(KafkaResponseConsumerService responseConsumer) {
        this.responseConsumer = responseConsumer;
        controller = new LoginController(this);

        initUI();
        getFocusListeners();
    }

    private void initUI() {
        setUndecorated(true);
        setSize(760, 570);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel background = new JPanel(new BorderLayout());
        setContentPane(background);

        // ===== Background =====
        JLabel bgLabel = new JLabel(new ImageIcon(getClass().getResource("/images/photo-1614850523011-8f49ffc73908.jpeg")));
        bgLabel.setLayout(new GridBagLayout());
        background.add(bgLabel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ===== Logo =====
        JLabel logo = new JLabel(new ImageIcon(getClass().getResource("/images/logo.png")));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(50, 0, 30, 0);
        bgLabel.add(logo, gbc);

        // ===== Usuario =====
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 50, 5, 50);

        JLabel userLabel = new JLabel("USUARIO");
        userLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        bgLabel.add(userLabel, gbc);

        userField = new JTextField();
        addPlaceholder(userField, "Ingrese su nombre de usuario");
        gbc.gridx = 0;
        gbc.gridy = 2;
        bgLabel.add(userField, gbc);

        // ===== Contraseña =====
        JLabel passLabel = new JLabel("CONTRASEÑA");
        passLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 3;
        bgLabel.add(passLabel, gbc);

        passwordField = new JPasswordField();
        addPlaceholder(passwordField, "********");
        gbc.gridx = 0;
        gbc.gridy = 4;
        bgLabel.add(passwordField, gbc);

        // ===== Botón Entrar =====
        JButton loginButton = new JButton("ENTRAR");
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.insets = new Insets(20, 200, 20, 200);
        bgLabel.add(loginButton, gbc);

        // Hover effect
        loginButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(new Color(100, 160, 210));
            }

            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(new Color(70, 130, 180));
            }
        });

        loginButton.addActionListener(e ->
                controller.login(userField.getText().trim(), passwordField.getPassword())
        );

        // ===== Barra superior (arrastrable) =====
        JPanel topBar = new JPanel(null);
        topBar.setBackground(new Color(230, 230, 230));
        topBar.setPreferredSize(new Dimension(760, 25));

        JLabel exitBtn = new JLabel("X", SwingConstants.CENTER);
        exitBtn.setOpaque(true);
        exitBtn.setBackground(Color.GRAY);
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setBounds(735, 0, 25, 25);
        exitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                System.exit(0);
            }
        });

        // Arrastrar ventana
        topBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                xMouse = e.getX();
                yMouse = e.getY();
            }
        });

        topBar.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - xMouse, e.getYOnScreen() - yMouse);
            }
        });

        topBar.setLayout(null);
        topBar.add(exitBtn);
        background.add(topBar, BorderLayout.NORTH);
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

    // ===== Métodos del controller =====
    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    public void loginSuccess() {
        dispose();
        new MainFrame(responseConsumer).setVisible(true);
    }
}