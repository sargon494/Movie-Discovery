package com.tastedivekafka.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class LoginFrame extends JFrame {

    private static final int WIDTH  = 780;
    private static final int HEIGHT = 580;

    private static final Color BG       = new Color(12, 12, 20);
    private static final Color GLASS_BG = new Color(22, 22, 34, 220);
    private static final Color ACCENT   = new Color(99, 155, 255);
    private static final Color TEXT     = new Color(225, 225, 230);
    private static final Color TEXT_DIM = new Color(110, 110, 140);
    private static final Color DANGER   = new Color(220, 60, 60);
    private static final Color SUCCESS  = new Color(60, 200, 100);
    private static final Color INPUT_BG = new Color(30, 30, 46);
    private static final Color INPUT_BD = new Color(55, 55, 80);

    private int xMouse, yMouse;

    private JTextField     userField;
    private JTextField     emailField;
    private JPasswordField passField;
    private JLabel         errorLabel;

    public interface LoginListener {
        void onLoginSuccess(String username);
        void onLoginFailure(String reason);
    }

    private final LoginListener loginListener;

    public LoginFrame(LoginListener listener) {
        this.loginListener = listener;
        initUI();
    }

    // ─── UI ──────────────────────────────────────────────────────────────────

    private void initUI() {
        setUndecorated(true);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setShape(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 14, 14));

        Image blurred = loadBlurredBg();
        JPanel root = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                if (blurred != null) g2.drawImage(blurred, 0, 0, getWidth(), getHeight(), null);
                else { g2.setColor(BG); g2.fillRect(0, 0, getWidth(), getHeight()); }
                g2.setColor(new Color(5, 5, 15, 200));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBackground(BG);
        setContentPane(root);

        // ── Barra superior ───────────────────────────────────────────────────
        JPanel bar = new JPanel(null);
        bar.setOpaque(false);
        bar.setBounds(0, 0, WIDTH, 36);
        JLabel title = new JLabel("MOVIE DISCOVERY");
        title.setBounds(16, 0, 200, 36);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(new Color(99, 155, 255, 160));
        bar.add(title);
        MainFrame.CloseButton btnClose = new MainFrame.CloseButton();
        btnClose.setBounds(WIDTH - 46, 0, 46, 36);
        btnClose.addActionListener(e -> System.exit(0));
        bar.add(btnClose);
        bar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { xMouse = e.getX(); yMouse = e.getY(); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - xMouse, e.getYOnScreen() - yMouse);
            }
        });
        root.add(bar);

        // ── Panel glass ──────────────────────────────────────────────────────
        JPanel glass = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(65, 65, 95, 140));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        glass.setOpaque(false);
        glass.setLayout(new BoxLayout(glass, BoxLayout.Y_AXIS));
        glass.setBorder(new EmptyBorder(24, 36, 24, 36));
        int gw = 360, gh = 510;
        glass.setBounds((WIDTH - gw) / 2, (HEIGHT - gh) / 2, gw, gh);
        root.add(glass);

        // ── Logo ─────────────────────────────────────────────────────────────
        JLabel logo;
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/logo.png")));
            Image img = icon.getImage().getScaledInstance(90, 90, Image.SCALE_SMOOTH);
            logo = new JLabel(new ImageIcon(img));
        } catch (Exception e) {
            logo = new JLabel("MOVIE DISCOVERY");
            logo.setFont(new Font("Segoe UI", Font.BOLD, 16));
            logo.setForeground(ACCENT);
        }
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        glass.add(logo);
        glass.add(Box.createRigidArea(new Dimension(0, 8)));

        // ── Subtítulo ────────────────────────────────────────────────────────
        JLabel sub = new JLabel("Inicia sesion para continuar");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TEXT_DIM);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        glass.add(sub);
        glass.add(Box.createRigidArea(new Dimension(0, 20)));

        // ── Campo usuario/email ───────────────────────────────────────────────
        userField = new JTextField();
        styleField(userField, "Usuario o email");
        glass.add(userField);
        glass.add(Box.createRigidArea(new Dimension(0, 12)));

        // ── Campo email (solo registro, oculto inicialmente) ─────────────────
        emailField = new JTextField();
        styleField(emailField, "Correo electronico");
        emailField.setVisible(false);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        glass.add(emailField);

        // Spacer dinámico entre email y password
        JPanel emailSpacer = new JPanel();
        emailSpacer.setOpaque(false);
        emailSpacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        emailSpacer.setVisible(false);
        glass.add(emailSpacer);

        // ── Campo contraseña ─────────────────────────────────────────────────
        passField = new JPasswordField();
        styleField(passField, "Contrasena");
        glass.add(passField);
        glass.add(Box.createRigidArea(new Dimension(0, 10)));

        // ── Error / success ──────────────────────────────────────────────────
        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        errorLabel.setForeground(DANGER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        glass.add(errorLabel);
        glass.add(Box.createRigidArea(new Dimension(0, 8)));

        // ── Botón principal (ENTRAR / CREAR CUENTA) ──────────────────────────
        JButton btnMain = actionButton("ENTRAR", ACCENT, null);
        glass.add(btnMain);
        glass.add(Box.createRigidArea(new Dimension(0, 14)));

        // ── Label separador ──────────────────────────────────────────────────
        JLabel orLbl = new JLabel("No tienes cuenta?");
        orLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        orLbl.setForeground(TEXT_DIM);
        orLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        glass.add(orLbl);
        glass.add(Box.createRigidArea(new Dimension(0, 8)));

        // ── Botón secundario (REGISTRATE / Volver) ───────────────────────────
        JButton btnSec = outlineButton("REGISTRATE", null);
        glass.add(btnSec);

        // ── Lógica de modo login / registro ──────────────────────────────────
        btnMain.addActionListener(e -> {
            if (emailField.isVisible()) signup();
            else login();
        });

        btnSec.addActionListener(e -> {
            if (!emailField.isVisible()) {
                // Cambiar a modo registro
                emailField.setVisible(true);
                emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
                emailSpacer.setVisible(true);
                emailSpacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
                btnMain.setText("CREAR CUENTA");
                orLbl.setText("Ya tienes cuenta?");
                btnSec.setText("VOLVER AL LOGIN");
                errorLabel.setText(" ");
                glass.revalidate();
            } else {
                // Volver a modo login
                emailField.setVisible(false);
                emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
                emailSpacer.setVisible(false);
                emailSpacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
                btnMain.setText("ENTRAR");
                orLbl.setText("No tienes cuenta?");
                btnSec.setText("REGISTRATE");
                errorLabel.setText(" ");
                glass.revalidate();
            }
        });

        // Enter
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (emailField.isVisible()) signup();
                    else login();
                }
            }
        };
        userField.addKeyListener(enter);
        passField.addKeyListener(enter);
    }

    // ─── Lógica ──────────────────────────────────────────────────────────────

    private void login() {
        String identifier = userField.getText().trim();
        String pass       = new String(passField.getPassword()).trim();
        if (identifier.isEmpty() || pass.isEmpty()) {
            showError("Introduce usuario y contrasena"); return;
        }
        try {
            String username = BackendClient.login(identifier, pass);
            if (username != null) {
                if (loginListener != null) loginListener.onLoginSuccess(username);
                dispose();
            } else {
                showError("Usuario o contrasena incorrectos");
                if (loginListener != null) loginListener.onLoginFailure("Credenciales incorrectas");
            }
        } catch (Exception ex) {
            if ("NOT_VERIFIED".equals(ex.getMessage()))
                showError("Verifica tu email antes de iniciar sesion");
            else
                showError("Error al conectar con el servidor");
        }
    }

    private void signup() {
        String user  = userField.getText().trim();
        String email = emailField.getText().trim();
        String pass  = new String(passField.getPassword()).trim();
        if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showError("Rellena todos los campos"); return;
        }
        if (!email.contains("@") || !email.contains(".")) {
            showError("Email no valido"); return;
        }
        if (pass.length() < 8) {
            showError("La contrasena debe tener al menos 8 caracteres"); return;
        }
        try {
            String result = BackendClient.register(user, email, pass);
            if ("VERIFY_EMAIL".equals(result)) {
                errorLabel.setForeground(SUCCESS);
                errorLabel.setText("Revisa tu email para verificar la cuenta");
                passField.setText("");
                emailField.setText("");
            } else if ("USER_EXISTS".equals(result)) {
                showError("El usuario o email ya existe");
            }
        } catch (Exception ex) {
            showError("Error al conectar con el servidor");
        }
    }

    private void showError(String msg) {
        errorLabel.setForeground(DANGER);
        errorLabel.setText(msg);
    }

    // ─── Helpers UI ──────────────────────────────────────────────────────────

    private void styleField(JTextField field, String placeholder) {
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_DIM);
        field.setCaretColor(ACCENT);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(INPUT_BD, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (field instanceof JPasswordField pf) {
            pf.setEchoChar((char) 0);
            pf.setText(placeholder);
            pf.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    if (String.valueOf(pf.getPassword()).equals(placeholder)) {
                        pf.setText(""); pf.setForeground(TEXT); pf.setEchoChar('*');
                    }
                }
                @Override public void focusLost(FocusEvent e) {
                    if (pf.getPassword().length == 0) {
                        pf.setEchoChar((char) 0); pf.setText(placeholder); pf.setForeground(TEXT_DIM);
                    }
                }
            });
        } else {
            field.setText(placeholder);
            field.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    if (field.getText().equals(placeholder)) { field.setText(""); field.setForeground(TEXT); }
                }
                @Override public void focusLost(FocusEvent e) {
                    if (field.getText().isEmpty()) { field.setText(placeholder); field.setForeground(TEXT_DIM); }
                }
            });
        }
    }

    private JButton actionButton(String text, Color bg, ActionListener action) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (action != null) btn.addActionListener(action);
        return btn;
    }

    private JButton outlineButton(String text, ActionListener action) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(99, 155, 255, 20));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setColor(getModel().isRollover() ? new Color(99, 155, 255, 180) : new Color(60, 60, 85, 200));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (action != null) btn.addActionListener(action);
        return btn;
    }

    private Image loadBlurredBg() {
        try {
            ImageIcon raw = new ImageIcon(Objects.requireNonNull(
                getClass().getResource("/photo-1614850523011-8f49ffc73908.jpeg")));
            BufferedImage scaled = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.drawImage(raw.getImage(), 0, 0, WIDTH, HEIGHT, null);
            g2.dispose();
            int k = 9; float sigma = 3f, sum = 0;
            float[] data = new float[k * k];
            for (int y = 0; y < k; y++) for (int x = 0; x < k; x++) {
                float dx = x - k / 2, dy = y - k / 2;
                data[y * k + x] = (float) Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
                sum += data[y * k + x];
            }
            for (int i = 0; i < data.length; i++) data[i] /= sum;
            return new ConvolveOp(new Kernel(k, k, data), ConvolveOp.EDGE_NO_OP, null).filter(scaled, null);
        } catch (Exception e) { return null; }
    }
}