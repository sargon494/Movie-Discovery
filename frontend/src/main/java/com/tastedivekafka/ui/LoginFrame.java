package com.tastedivekafka.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;

/**
 * LoginFrame es la ventana de autenticación de usuario. Permite a los usuarios
 * iniciar sesión o registrarse. Se comunica con el backend a través de BackendClient
 * para validar credenciales y crear nuevas cuentas.
 * Responsabilidades:
 * - Mostrar campos de usuario y contraseña con placeholders
 * - Validar que los campos no estén vacíos antes de enviar la solicitud
 * - Enviar solicitudes de login/registro al backend usando BackendClient
 * - Notificar al FrontendApp sobre el resultado del login a través de LoginListener
 * - Permitir arrastrar la ventana y cerrarla con un botón personalizado
 * El LoginFrame no se comunica directamente con Kafka. Toda la comunicación con el backend
 * se realiza a través de BackendClient (HTTP en el puerto 8090). El LoginFrame es independiente del MainFrame y se cierra al iniciar sesión correctamente.
 * El MainFrame se encarga de mostrar la interfaz principal de la aplicación y de escuchar las respuestas de Kafka. 
 * El LoginFrame solo se ocupa del proceso de autenticación.
 * El LoginListener es una interfaz que permite al LoginFrame notificar al FrontendApp sobre el resultado del login. 
 * Si el login es exitoso, se pasa el nombre de usuario al FrontendApp para que pueda almacenarlo en la sesión. Si el login falla, se muestra un mensaje de error.
 * El proceso de registro también se maneja dentro del LoginFrame. 
 * Al hacer clic en el botón de registro, se valida que los campos no estén vacíos y que la contraseña tenga al menos 8 caracteres. 
 * Luego se envía una solicitud de registro al backend usando BackendClient. 
 * Si el registro es exitoso, se muestra un mensaje de confirmación. 
 * Si el usuario ya existe o hay un error de conexión, se muestra un mensaje de error.
 */
public class LoginFrame extends JFrame {

    private static final int WIDTH = 760;
    private static final int HEIGHT = 570;
    private static final String USER_PLACEHOLDER = "Ingrese su nombre de usuario";
    private static final String PASS_PLACEHOLDER = "********";

    private int xMouse, yMouse;

    // ── Listener ──
    public interface LoginListener {
        void onLoginSuccess(String username); // ← username passed back to FrontendApp
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
        setUndecorated(true);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        BackgroundPanel background = new BackgroundPanel(
                new ImageIcon(Objects.requireNonNull(
                        getClass().getResource("/photo-1614850523011-8f49ffc73908.jpeg")
                ))
        );
        background.setLayout(new BorderLayout());
        setContentPane(background);

        JPanel topBar = new JPanel(null);
        topBar.setPreferredSize(new Dimension(WIDTH, 25));
        topBar.setBackground(new Color(230, 230, 230));

        JLabel exitBtn = new JLabel("X", SwingConstants.CENTER);
        exitBtn.setBounds(WIDTH - 25, 0, 25, 25);
        exitBtn.setOpaque(true);
        exitBtn.setBackground(Color.GRAY);
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
        });

        topBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { xMouse = e.getX(); yMouse = e.getY(); }
        });
        topBar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - xMouse, e.getYOnScreen() - yMouse);
            }
        });
        topBar.add(exitBtn);
        background.add(topBar, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        background.add(centerPanel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel logo = new JLabel(new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/logo.png"))
        ));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(50, 0, 30, 0);
        centerPanel.add(logo, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 50, 5, 50);

        JLabel userLabel = new JLabel("USUARIO");
        userLabel.setForeground(Color.WHITE);
        gbc.gridy = 1; centerPanel.add(userLabel, gbc);

        userField = new JTextField();
        addPlaceholder(userField, USER_PLACEHOLDER);
        gbc.gridy = 2; centerPanel.add(userField, gbc);

        JLabel passLabel = new JLabel("CONTRASEÑA");
        passLabel.setForeground(Color.WHITE);
        gbc.gridy = 3; centerPanel.add(passLabel, gbc);

        passwordField = new JPasswordField();
        addPlaceholder(passwordField, PASS_PLACEHOLDER);
        gbc.gridy = 4; centerPanel.add(passwordField, gbc);

        JButton loginButton = new JButton("ENTRAR");
        styleButton(loginButton);
        gbc.gridy = 5; gbc.insets = new Insets(25, 200, 25, 200);
        centerPanel.add(loginButton, gbc);
        loginButton.addActionListener(e -> login());

        JButton registerButton = new JButton("REGISTRATE AQUÍ");
        styleButton(registerButton);
        gbc.gridy = 6; gbc.insets = new Insets(5, 200, 25, 200);
        centerPanel.add(registerButton, gbc);
        registerButton.addActionListener(e -> signup());
    }

    /* ===================== LÓGICA DE LOGIN ============= */
    private void login() {
        String user = userField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();

        if (user.isEmpty() || user.equals(USER_PLACEHOLDER)
                || pass.isEmpty() || pass.equals(PASS_PLACEHOLDER)) {
            JOptionPane.showMessageDialog(this,
                    "Introduce usuario y contraseña.", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            boolean ok = BackendClient.login(user, pass);
            if (ok) {
                if (loginListener != null) loginListener.onLoginSuccess(user); // ← passes username
                dispose();
            } else {
                if (loginListener != null)
                    loginListener.onLoginFailure("Usuario o contraseña incorrectos.");
            }
        } catch (Exception ex) {
            if (loginListener != null)
                loginListener.onLoginFailure("Error al conectar con el servidor.");
        }
    }

    /* ===================== LÓGICA DE REGISTRO — usa BackendClient ========== */
    public void signup() {
        String user = userField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();

        if (user.isEmpty() || user.equals(USER_PLACEHOLDER)
                || pass.isEmpty() || pass.equals(PASS_PLACEHOLDER)) {
            JOptionPane.showMessageDialog(this,
                    "Introduce usuario y contraseña deseados", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (pass.length() < 8) {
            JOptionPane.showMessageDialog(this,
                    "La contraseña debe tener al menos 8 carácteres", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            boolean ok = BackendClient.register(user, pass);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Usuario registrado correctamente", "OK", JOptionPane.INFORMATION_MESSAGE);
                passwordField.setText("");
            } else {
                JOptionPane.showMessageDialog(this,
                        "El usuario ya existe", "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al conectar con el servidor.", "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ===================== UTIL ================================= */
    private void addPlaceholder(JTextField field, String placeholder) {
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText(""); field.setForeground(Color.BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder); field.setForeground(Color.GRAY);
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
            public void mouseEntered(MouseEvent e) { button.setBackground(new Color(100, 160, 210)); }
            public void mouseExited(MouseEvent e)  { button.setBackground(new Color(70, 130, 180)); }
        });
    }

    static class BackgroundPanel extends JPanel {
        private final Image bgImage;
        public BackgroundPanel(ImageIcon icon) { this.bgImage = icon.getImage(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
