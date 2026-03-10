package com.tastedivekafka.ui;

import com.tastedivekafka.FrontendApp;
import com.tastedivekafka.session.AppSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Diálogo de perfil — diseño profesional dark mode.
 *
 * Sin Unicode problemático — todos los iconos dibujados con Graphics2D.
 * Cuatro pestañas: Stats, Favoritos, Historial, Ajustes.
 */
public class ProfileDialog extends JDialog {

    private static final Color BG       = new Color(18, 18, 22);
    private static final Color BG_PANEL = new Color(24, 24, 30);
    private static final Color BG_CARD  = new Color(32, 32, 42);
    private static final Color BG_INPUT = new Color(28, 28, 36);
    private static final Color ACCENT   = new Color(99, 155, 255);
    private static final Color TEXT     = new Color(225, 225, 230);
    private static final Color TEXT_DIM = new Color(110, 110, 130);
    private static final Color DANGER   = new Color(220, 60, 60);
    private static final Color STAR_ON  = new Color(255, 200, 50);
    private static final Color STAR_OFF = new Color(60, 60, 75);
    private static final Color BORDER   = new Color(45, 45, 60);
    private static final Color SUCCESS  = new Color(60, 180, 100);

    public ProfileDialog(Window owner) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(580, 520);
        setLocationRelativeTo(owner);
        getRootPane().setBorder(BorderFactory.createLineBorder(BORDER, 1));
        setShape(new RoundRectangle2D.Double(0, 0, 580, 520, 12, 12));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        setContentPane(root);

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildBody(),     BorderLayout.CENTER);

        setVisible(true);
    }

    // ─── Barra de título ─────────────────────────────────────────────────────

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(null);
        bar.setBackground(new Color(22, 22, 28));
        bar.setPreferredSize(new Dimension(580, 56));

        // Avatar
        String initial = AppSession.getCurrentUser().substring(0, 1).toUpperCase();
        JPanel avatarCircle = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fill(new Ellipse2D.Double(0, 0, getWidth(), getHeight()));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initial,
                    (getWidth()  - fm.stringWidth(initial)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatarCircle.setOpaque(false);
        avatarCircle.setBounds(16, 10, 36, 36);

        JLabel username = new JLabel(AppSession.getCurrentUser());
        username.setBounds(62, 10, 260, 20);
        username.setFont(new Font("Segoe UI", Font.BOLD, 15));
        username.setForeground(TEXT);

        JLabel subtitle = new JLabel("Gestiona tu perfil y preferencias");
        subtitle.setBounds(62, 30, 280, 16);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(TEXT_DIM);

        // Botón cerrar
        MainFrame.CloseButton btnClose = new MainFrame.CloseButton();
        btnClose.setBounds(536, 0, 44, 56);
        btnClose.addActionListener(e -> dispose());

        bar.add(avatarCircle);
        bar.add(username);
        bar.add(subtitle);
        bar.add(btnClose);

        // Línea separadora
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        return bar;
    }

    // ─── Cuerpo con pestañas ──────────────────────────────────────────────────

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(BG_PANEL);

        // Navegación lateral de pestañas
        String[] tabNames = {"Stats", "Favoritos", "Historial", "Ajustes"};
        JPanel nav = buildNav(tabNames);

        // Panel de contenido
        JPanel content = new JPanel(new CardLayout());
        content.setBackground(BG_PANEL);
        content.add(buildStatsPanel(),    "Stats");
        content.add(buildFavoritesPanel(), "Favoritos");
        content.add(buildHistoryPanel(),   "Historial");
        content.add(buildSettingsPanel(),  "Ajustes");

        // Conectar nav con CardLayout
        CardLayout cl = (CardLayout) content.getLayout();
        for (Component c : nav.getComponents()) {
            if (c instanceof NavButton btn) {
                btn.addActionListener(e -> {
                    for (Component nb : nav.getComponents()) {
                        if (nb instanceof NavButton) ((NavButton) nb).setSelected(false);
                    }
                    btn.setSelected(true);
                    cl.show(content, btn.getText());
                });
            }
        }

        // Seleccionar primera pestaña
        if (nav.getComponent(0) instanceof NavButton first) first.setSelected(true);

        body.add(nav,     BorderLayout.WEST);
        body.add(content, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildNav(String[] names) {
        JPanel nav = new JPanel();
        nav.setBackground(new Color(20, 20, 26));
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setPreferredSize(new Dimension(130, 464));
        nav.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));
        nav.add(Box.createRigidArea(new Dimension(0, 12)));
        for (String name : names) {
            NavButton btn = new NavButton(name);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            nav.add(btn);
            nav.add(Box.createRigidArea(new Dimension(0, 4)));
        }
        return nav;
    }

    // ─── Stats ───────────────────────────────────────────────────────────────

    private JScrollPane buildStatsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = sectionTitle("Resumen de cuenta");
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel loading = dimLabel("Cargando...");
        panel.add(loading);

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return BackendClient.getProfile();
            }
            @Override protected void done() {
                try {
                    String[] p = get().split("\\|\\|");
                    panel.remove(loading);
                    panel.add(statRow("Usuario",           p.length > 0 ? p[0] : "-"));
                    panel.add(Box.createRigidArea(new Dimension(0, 8)));
                    String date = p.length > 1 && p[1] != null ? p[1].substring(0, 10) : "-";
                    panel.add(statRow("Miembro desde",     date));
                    panel.add(Box.createRigidArea(new Dimension(0, 8)));
                    panel.add(statRow("Busquedas totales", p.length > 2 ? p[2] : "0"));
                    panel.add(Box.createRigidArea(new Dimension(0, 8)));
                    panel.add(statRow("Peliculas favoritas", p.length > 3 ? p[3] : "0"));
                    panel.revalidate(); panel.repaint();
                } catch (Exception e) {
                    loading.setText("Error: " + e.getMessage());
                }
            }
        }.execute();

        return scrollWrap(panel);
    }

    private JPanel statRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_CARD);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(12, 16, 12, 16)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_DIM);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 13));
        val.setForeground(TEXT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    // ─── Favoritos ───────────────────────────────────────────────────────────

    private JScrollPane buildFavoritesPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        panel.add(sectionTitle("Peliculas favoritas"));
        panel.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel loading = dimLabel("Cargando favoritos...");
        panel.add(loading);

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return BackendClient.getFavorites();
            }
            @Override protected void done() {
                try {
                    panel.remove(loading);
                    String raw = get();
                    if (raw == null || raw.isBlank()) {
                        panel.add(dimLabel("No tienes peliculas favoritas todavia."));
                    } else {
                        for (String entry : raw.split(";;")) {
                            String[] p = entry.split("\\|\\|");
                            if (p.length >= 4) {
                                panel.add(favRow(p[0], Integer.parseInt(p[3]), panel));
                                panel.add(Box.createRigidArea(new Dimension(0, 8)));
                            }
                        }
                    }
                    panel.revalidate(); panel.repaint();
                } catch (Exception e) {
                    loading.setText("Error: " + e.getMessage());
                }
            }
        }.execute();

        return scrollWrap(panel);
    }

    private JPanel favRow(String movieName, int rating, JPanel parent) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(BG_CARD);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(10, 14, 10, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel title = new JLabel(movieName);
        title.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        title.setForeground(TEXT);

        JPanel stars = buildStarRow(movieName, rating);

        JButton remove = new JButton("Quitar") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(160, 40, 40) : new Color(80, 30, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(255, 120, 120));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("Quitar",
                    (getWidth()  - fm.stringWidth("Quitar")) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        remove.setPreferredSize(new Dimension(56, 26));
        remove.setOpaque(false); remove.setContentAreaFilled(false);
        remove.setBorderPainted(false); remove.setFocusPainted(false);
        remove.setCursor(new Cursor(Cursor.HAND_CURSOR));
        remove.addActionListener(e -> new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                BackendClient.removeFavorite(movieName); return null;
            }
            @Override protected void done() {
                parent.remove(row);
                parent.revalidate(); parent.repaint();
            }
        }.execute());

        row.add(title,  BorderLayout.WEST);
        row.add(stars,  BorderLayout.CENTER);
        row.add(remove, BorderLayout.EAST);
        return row;
    }

    private JPanel buildStarRow(String movieName, int currentRating) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        panel.setOpaque(false);

        // Estrellas dibujadas como botones custom
        StarLabel[] stars = new StarLabel[5];
        for (int i = 0; i < 5; i++) {
            final int val = i + 1;
            stars[i] = new StarLabel(i < currentRating);
            stars[i].addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    new SwingWorker<Void, Void>() {
                        @Override protected Void doInBackground() throws Exception {
                            BackendClient.updateRating(movieName, val); return null;
                        }
                        @Override protected void done() {
                            for (int j = 0; j < 5; j++) stars[j].setOn(j < val);
                        }
                    }.execute();
                }
                @Override public void mouseEntered(MouseEvent e) {
                    for (int j = 0; j < 5; j++) stars[j].setOn(j < val);
                }
            });
            panel.add(stars[i]);
        }
        return panel;
    }

    // ─── Historial ───────────────────────────────────────────────────────────

    private JScrollPane buildHistoryPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        panel.add(sectionTitle("Historial de busquedas"));
        panel.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel loading = dimLabel("Cargando historial...");
        panel.add(loading);

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                return BackendClient.getHistory();
            }
            @Override protected void done() {
                try {
                    panel.remove(loading);
                    String raw = get();
                    if (raw == null || raw.isBlank()) {
                        panel.add(dimLabel("No hay busquedas registradas."));
                    } else {
                        for (String entry : raw.split(";;")) {
                            String[] p   = entry.split("\\|\\|");
                            String query = p.length > 0 ? p[0] : "";
                            String date  = p.length > 1
                                ? p[1].substring(0, Math.min(16, p[1].length())).replace("T", " ")
                                : "";
                            panel.add(historyRow(query, date));
                            panel.add(Box.createRigidArea(new Dimension(0, 6)));
                        }
                    }
                    panel.revalidate(); panel.repaint();
                } catch (Exception e) {
                    loading.setText("Error: " + e.getMessage());
                }
            }
        }.execute();

        return scrollWrap(panel);
    }

    private JPanel historyRow(String query, String date) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_CARD);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(8, 14, 8, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel q = new JLabel(query);
        q.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        q.setForeground(TEXT);

        JLabel d = new JLabel(date);
        d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        d.setForeground(TEXT_DIM);

        row.add(q, BorderLayout.WEST);
        row.add(d, BorderLayout.EAST);
        return row;
    }

    // ─── Ajustes ─────────────────────────────────────────────────────────────

    private JScrollPane buildSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Cambiar username
        panel.add(sectionTitle("Cambiar nombre de usuario"));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        JTextField newUserField = inputField("Nuevo nombre de usuario");
        panel.add(newUserField);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        JButton btnUser = accentButton("Actualizar nombre", ACCENT);
        btnUser.addActionListener(e -> {
            String val = newUserField.getText().trim();
            if (val.isBlank()) return;
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception {
                    return BackendClient.changeUsername(val);
                }
                @Override protected void done() {
                    try {
                        String newName = get();
                        AppSession.setCurrentUser(newName);
                        showToast("Nombre actualizado a: " + newName, SUCCESS);
                        dispose();
                    } catch (Exception ex) {
                        showToast(ex.getMessage(), DANGER);
                    }
                }
            }.execute();
        });
        panel.add(btnUser);

        panel.add(Box.createRigidArea(new Dimension(0, 24)));
        panel.add(separator());
        panel.add(Box.createRigidArea(new Dimension(0, 24)));

        // Cambiar contraseña
        panel.add(sectionTitle("Cambiar contrasena"));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        JPasswordField oldPassField = passField("Contrasena actual");
        JPasswordField newPassField = passField("Nueva contrasena (min. 6 caracteres)");
        panel.add(oldPassField);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(newPassField);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        JButton btnPass = accentButton("Cambiar contrasena", ACCENT);
        btnPass.addActionListener(e -> {
            String oldP = new String(oldPassField.getPassword());
            String newP = new String(newPassField.getPassword());
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    BackendClient.changePassword(oldP, newP); return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        showToast("Contrasena actualizada correctamente", SUCCESS);
                        oldPassField.setText(""); newPassField.setText("");
                    } catch (Exception ex) {
                        showToast(ex.getMessage(), DANGER);
                    }
                }
            }.execute();
        });
        panel.add(btnPass);

        panel.add(Box.createRigidArea(new Dimension(0, 24)));
        panel.add(separator());
        panel.add(Box.createRigidArea(new Dimension(0, 24)));

        // Zona de peligro
        panel.add(sectionTitle("Zona de peligro"));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel warn = new JLabel("Esta accion es irreversible. Se borraran todos tus datos.");
        warn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        warn.setForeground(new Color(180, 80, 80));
        warn.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(warn);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        JButton btnDelete = accentButton("Borrar mi cuenta", DANGER);
        btnDelete.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                "¿Estas seguro? Esta accion no se puede deshacer.",
                "Confirmar borrado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    BackendClient.deleteAccount(); return null;
                }
                @Override protected void done() {
                    AppSession.logout(); dispose(); FrontendApp.showLogin();
                }
            }.execute();
        });
        panel.add(btnDelete);

        return scrollWrap(panel);
    }

    // ─── Helpers de UI ───────────────────────────────────────────────────────

    private JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(TEXT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel dimLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lbl.setForeground(TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField inputField(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_DIM);
        f.setCaretColor(ACCENT);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(7, 10, 7, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(TEXT); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (f.getText().isBlank()) { f.setText(placeholder); f.setForeground(TEXT_DIM); }
            }
        });
        return f;
    }

    private JPasswordField passField(String tooltip) {
        JPasswordField f = new JPasswordField();
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(7, 10, 7, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setToolTipText(tooltip);
        return f;
    }

    private JButton accentButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? color.darker() : color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private JScrollPane scrollWrap(JPanel panel) {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBackground(BG_PANEL);
        scroll.getViewport().setBackground(BG_PANEL);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    private void showToast(String message, Color color) {
        JOptionPane.showMessageDialog(this, message, "",
            color.equals(DANGER) ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
    }

    // ─── Componentes custom ───────────────────────────────────────────────────

    /** Botón de navegación lateral */
    static class NavButton extends JButton {
        private boolean selected = false;
        NavButton(String text) {
            super(text);
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setForeground(new Color(110, 110, 130));
            setMaximumSize(new Dimension(120, 38));
            setPreferredSize(new Dimension(120, 38));
        }
        public void setSelected(boolean s) { selected = s; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (selected) {
                g2.setColor(new Color(99, 155, 255, 30));
                g2.fillRoundRect(6, 2, getWidth() - 12, getHeight() - 4, 8, 8);
                // Barra izquierda
                g2.setColor(new Color(99, 155, 255));
                g2.fillRoundRect(0, 6, 3, getHeight() - 12, 3, 3);
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(6, 2, getWidth() - 12, getHeight() - 4, 8, 8);
            }
            Color textColor = selected ? new Color(99, 155, 255) :
                              getModel().isRollover() ? new Color(180, 180, 200) :
                              new Color(110, 110, 130);
            g2.setColor(textColor);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(),
                (getWidth()  - fm.stringWidth(getText())) / 2,
                (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Estrella custom dibujada con Graphics2D */
    static class StarLabel extends JLabel {
        private boolean on;
        StarLabel(boolean on) {
            this.on = on;
            setPreferredSize(new Dimension(20, 20));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        void setOn(boolean on) { this.on = on; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            int[] xp = new int[10], yp = new int[10];
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI / 5 * i - Math.PI / 2;
                int r = (i % 2 == 0) ? 8 : 4;
                xp[i] = (int)(cx + r * Math.cos(angle));
                yp[i] = (int)(cy + r * Math.sin(angle));
            }
            g2.setColor(on ? STAR_ON : STAR_OFF);
            g2.fillPolygon(xp, yp, 10);
            g2.dispose();
        }
    }
}