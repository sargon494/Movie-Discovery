package com.tastedivekafka.ui;

import com.tastedivekafka.db.UserDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;

/**
 * Ventana de login de la aplicación.
 *
 * Permite al usuario:
 *  - Introducir usuario y contraseña
 *  - Validar credenciales con la base de datos
 *  - Notificar éxito o fallo al listener
 */
public class LoginFrame extends JFrame {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 570;
    private static final String USER_PLACEHOLDER = "Ingrese su nombre de usuario";
    private static final String PASS_PLACEHOLDER = "********";

    private int xMouse, yMouse; // Para arrastrar la ventana

    public interface LoginListener {
        void onLoginSuccess();
        void onLoginFailure(String reason);
    }

    private final LoginListener loginListener;

    private JTextField userField;
    private JPasswordField passwordField;

    public LoginFrame(LoginListener listener) {
        this.loginListener = listener;
        initUI();
    }

    /* ===================== UI ===================== */
    private void initUI() {
        setUndecorated(true); // Ventana sin barra de título
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Panel con imagen de fondo
        BackgroundPanel background = new BackgroundPanel(
                new ImageIcon(Objects.requireNonNull(
                        getClass().getResource("/images/photo-1614850523011-8f49ffc73908.jpeg")
                ))
        );
        background.setLayout(new BorderLayout());
        setContentPane(background);

        /* ===== TOP BAR ===== */
        JPanel topBar = new JPanel(null);
        topBar.setPreferredSize(new Dimension(WIDTH, 25));
        topBar.setBackground(new Color(230, 230, 230));

        // Botón para cerrar la ventana
        JLabel exitBtn = new JLabel("X", SwingConstants.CENTER);
        exitBtn.setBounds(WIDTH - 25, 0, 25, 25);
        exitBtn.setOpaque(true);
        exitBtn.setBackground(Color.GRAY);
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                System.exit(0); // Cierra aplicación
            }
        });

        // Permite arrastrar la ventana desde la topBar
        topBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                xMouse = e.getX();
                yMouse = e.getY();
            }
        });
        topBar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - xMouse, e.getYOnScreen() - yMouse);
            }
        });

        topBar.add(exitBtn);
        background.add(topBar, BorderLayout.NORTH);

        /* ===== CENTER PANEL ===== */
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        background.add(centerPanel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        /* ===== LOGO ===== */
        JLabel logo = new JLabel(new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/images/logo.png"))
        ));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(50, 0, 30, 0);
        centerPanel.add(logo, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 50, 5, 50);

        /* ===== USER ===== */
        JLabel userLabel = new JLabel("USUARIO");
        userLabel.setForeground(Color.WHITE);
        gbc.gridy = 1;
        centerPanel.add(userLabel, gbc);

        userField = new JTextField();
        addPlaceholder(userField, USER_PLACEHOLDER); // Placeholder
        gbc.gridy = 2;
        centerPanel.add(userField, gbc);

        /* ===== PASSWORD ===== */
        JLabel passLabel = new JLabel("CONTRASEÑA");
        passLabel.setForeground(Color.WHITE);
        gbc.gridy = 3;
        centerPanel.add(passLabel, gbc);

        passwordField = new JPasswordField();
        addPlaceholder(passwordField, PASS_PLACEHOLDER); // Placeholder
        gbc.gridy = 4;
        centerPanel.add(passwordField, gbc);

        /* ===== LOGIN BUTTON ===== */
        JButton loginButton = new JButton("ENTRAR");
        styleButton(loginButton); // Estilo visual
        gbc.gridy = 5;
        gbc.insets = new Insets(25, 200, 25, 200);
        centerPanel.add(loginButton, gbc);

        loginButton.addActionListener(e -> login());
    }

    /* ===================== LÓGICA DE LOGIN ===================== */
    private void login() {
        String user = userField.getText().trim();
        String pass = String.valueOf(passwordField.getPassword()).trim();

        if (user.isEmpty() || user.equals(USER_PLACEHOLDER)
                || pass.isEmpty() || pass.equals(PASS_PLACEHOLDER)) {
            JOptionPane.showMessageDialog(this,
                    "Introduce usuario y contraseña.",
                    "ERROR",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            UserDAO dao = new UserDAO();
            boolean ok = dao.login(user, pass); // Verificamos en BD

            if (ok) {
                if (loginListener != null) loginListener.onLoginSuccess();
                dispose(); // Cerramos ventana login
            } else {
                if (loginListener != null)
                    loginListener.onLoginFailure("Usuario o contraseña incorrectos.");
            }
        } catch (Exception ex) {
            if (loginListener != null)
                loginListener.onLoginFailure("Error al conectar con la BD.");
        }
    }

    /* ===================== UTIL ===================== */
    private void addPlaceholder(JTextField field, String placeholder) {
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(100, 160, 210));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(70, 130, 180));
            }
        });
    }

    /* ===================== PANEL DE FONDO ===================== */
    static class BackgroundPanel extends JPanel {
        private final Image bgImage;

        public BackgroundPanel(ImageIcon icon) {
            this.bgImage = icon.getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
