package com.tastedivekafka.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.ExecutionException;

import javax.swing.*;

import com.tastedivekafka.FrontendApp;
import com.tastedivekafka.session.AppSession;

/**
 * MainFrame — ventana principal rediseñada.
 * Correcciones: antialiasing en todos los componentes custom,
 * iconos dibujados con Graphics2D en lugar de caracteres Unicode,
 * avatar circular real con initial del usuario.
 */
public class MainFrame extends JFrame {

    private static final int WINDOW_WIDTH  = 920;
    private static final int WINDOW_HEIGHT = 660;

    // Paleta
    static final Color BG          = new Color(18, 18, 22);
    static final Color BG_BAR      = new Color(26, 26, 32);
    static final Color BG_CARD     = new Color(30, 30, 38);
    static final Color ACCENT      = new Color(99, 155, 255);
    static final Color ACCENT_DARK = new Color(60, 100, 200);
    static final Color TEXT        = new Color(230, 230, 235);
    static final Color TEXT_DIM    = new Color(120, 120, 135);
    static final Color DANGER      = new Color(220, 60, 60);

    private final JTextField searchField  = new JTextField();
    private final JPanel     galleryPanel = new JPanel();
    private final ImageCache imageCache   = new ImageCache();
    private int dragX, dragY;

    public MainFrame() {
        if (!AppSession.isLogged()) throw new IllegalStateException("No hay sesión activa");
        setTitle("MovieDiscovery");
        initUI();
    }

    // ─── Search ──────────────────────────────────────────────────────────────

    private void onSearch() {
        String movie = searchField.getText().trim();
        if (movie.isEmpty()) return;

        galleryPanel.removeAll();
        galleryPanel.setLayout(new BorderLayout());
        JLabel loading = new JLabel("Buscando recomendaciones...", SwingConstants.CENTER);
        loading.setForeground(TEXT_DIM);
        loading.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        galleryPanel.add(loading, BorderLayout.CENTER);
        galleryPanel.revalidate();

        new Thread(() -> {
            try {
                String response = BackendClient.search(movie);
                BackendClient.recordSearch(movie);
                SwingUtilities.invokeLater(() -> updateGallery(response));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    galleryPanel.removeAll();
                    JLabel err = new JLabel("Error: " + e.getMessage(), SwingConstants.CENTER);
                    err.setForeground(DANGER);
                    galleryPanel.setLayout(new BorderLayout());
                    galleryPanel.add(err, BorderLayout.CENTER);
                    galleryPanel.revalidate();
                });
            }
        }, "search-thread").start();
    }

    private void updateGallery(String response) {
        if (!response.contains("||")) return;
        galleryPanel.removeAll();
        galleryPanel.setLayout(new GridLayout(0, 4, 16, 16));
        for (String movieData : response.split(";;")) {
            String[] parts = movieData.split("\\|\\|");
            if (parts.length >= 3)
                galleryPanel.add(MovieCard.create(parts[0], parts[1], parts[2], imageCache));
        }
        galleryPanel.revalidate();
        galleryPanel.repaint();
    }

    // ─── UI ──────────────────────────────────────────────────────────────────

    private void initUI() {
        setUndecorated(true);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setShape(new RoundRectangle2D.Double(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, 14, 14));

        JPanel root = new JPanel(null);
        root.setBackground(BG);
        setContentPane(root);

        // ── Barra superior ──────────────────────────────────────────────────
        JPanel bar = new JPanel(null);
        bar.setBackground(BG_BAR);
        bar.setBounds(0, 0, WINDOW_WIDTH, 42);

        JLabel appTitle = new JLabel("MOVIE DISCOVERY");
        appTitle.setBounds(16, 0, 200, 42);
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        appTitle.setForeground(ACCENT);
        bar.add(appTitle);

        // Avatar circular con initial del usuario
        String initial = AppSession.getCurrentUser().substring(0, 1).toUpperCase();
        AvatarButton avatar = new AvatarButton(initial);
        avatar.setBounds(WINDOW_WIDTH - 84, 7, 28, 28);
        avatar.setToolTipText("Perfil de " + AppSession.getCurrentUser());
        avatar.addActionListener(e -> new ProfileDialog(MainFrame.this));
        bar.add(avatar);

        // Botón cerrar dibujado con lineas — sin Unicode
        CloseButton btnClose = new CloseButton();
        btnClose.setBounds(WINDOW_WIDTH - 46, 0, 46, 42);
        btnClose.addActionListener(e -> { TrailerBrowser.shutdown(); System.exit(0); });
        bar.add(btnClose);

        // Drag
        bar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragX = e.getX(); dragY = e.getY(); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - dragX, e.getYOnScreen() - dragY);
            }
        });
        root.add(bar);

        // ── Barra de búsqueda ────────────────────────────────────────────────
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        searchBar.setBackground(BG);
        searchBar.setBounds(0, 42, WINDOW_WIDTH, 52);

        searchField.setPreferredSize(new Dimension(340, 34));
        searchField.setBackground(BG_CARD);
        searchField.setForeground(TEXT);
        searchField.setCaretColor(ACCENT);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(55, 55, 70), 1),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        searchField.addActionListener(e -> onSearch());

        PillButton btnSearch = new PillButton("Buscar", ACCENT, ACCENT_DARK);
        btnSearch.setPreferredSize(new Dimension(90, 34));
        btnSearch.addActionListener(e -> onSearch());

        PillButton btnLogout = new PillButton("Salir", DANGER, new Color(160, 40, 40));
        btnLogout.setPreferredSize(new Dimension(80, 34));
        btnLogout.addActionListener(e -> {
            AppSession.logout();
            MainFrame.this.dispose();
            FrontendApp.showLogin();
        });

        searchBar.add(searchField);
        searchBar.add(btnSearch);
        searchBar.add(btnLogout);
        root.add(searchBar);

        // ── Galería ──────────────────────────────────────────────────────────
        galleryPanel.setBackground(BG);
        JScrollPane scroll = new JScrollPane(galleryPanel);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBounds(16, 100, WINDOW_WIDTH - 32, WINDOW_HEIGHT - 116);
        DarkScrollBarUI.apply(scroll);
        root.add(scroll);

        warmUpChromium();
    }

    private void warmUpChromium() {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                TrailerBrowser.initCef(); return null;
            }
            @Override protected void done() {
                try { get(); } catch (InterruptedException | ExecutionException ex) {
                    System.err.println("[JCEF] Warm-up falló: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ─── MovieCard ───────────────────────────────────────────────────────────

    static class MovieCard extends JPanel {

        private Image cardImage;
        private final String cardTitle, cardImageURL, cardTrailerURL;

        static MovieCard create(String title, String imageURL, String trailerURL, ImageCache cache) {
            MovieCard c = new MovieCard(title, imageURL, trailerURL);
            c.loadImageAsync(imageURL, cache);
            return c;
        }

        private MovieCard(String title, String imageURL, String trailerURL) {
            this.cardTitle      = title;
            this.cardImageURL   = imageURL;
            this.cardTrailerURL = trailerURL;
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(160, 280));
            setOpaque(false);

            JLabel lbl = new JLabel("<html><center>" + title + "</center></html>", SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lbl.setForeground(new Color(150, 190, 255));
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            lbl.setToolTipText("Ver trailer de " + title);
            lbl.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    TrailerBrowser.openTrailer(SwingUtilities.getWindowAncestor(MovieCard.this), cardTitle, cardTrailerURL);
                }
                @Override public void mouseEntered(MouseEvent e) { lbl.setForeground(Color.WHITE); }
                @Override public void mouseExited(MouseEvent e)  { lbl.setForeground(new Color(150, 190, 255)); }
            });

            FavButton favBtn = new FavButton();
            favBtn.addActionListener(e -> showRatingDialog(favBtn));

            JPanel south = new JPanel(new BorderLayout(0, 2));
            south.setOpaque(false);
            south.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            south.add(lbl, BorderLayout.CENTER);
            south.add(favBtn, BorderLayout.SOUTH);
            add(south, BorderLayout.SOUTH);
        }

        private void showRatingDialog(FavButton favBtn) {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(BG_CARD);
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JLabel msg = new JLabel("Valoracion para: " + cardTitle);
            msg.setForeground(TEXT);
            msg.setFont(new Font("Segoe UI", Font.BOLD, 13));
            JSlider slider = new JSlider(1, 5, 3);
            slider.setMajorTickSpacing(1);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.setSnapToTicks(true);
            slider.setBackground(BG_CARD);
            slider.setForeground(TEXT);
            panel.add(msg, BorderLayout.NORTH);
            panel.add(slider, BorderLayout.CENTER);

            int res = JOptionPane.showConfirmDialog(this, panel,
                "Añadir a visto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                int rating = slider.getValue();
                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() throws Exception {
                        BackendClient.addFavorite(cardTitle, cardImageURL, cardTrailerURL, rating);
                        return null;
                    }
                    @Override protected void done() {
                        try { get(); favBtn.setActive(true); }
                        catch (Exception ex) {
                            JOptionPane.showMessageDialog(MovieCard.this, ex.getMessage());
                        }
                    }
                }.execute();
            }
        }

        private void loadImageAsync(String url, ImageCache cache) {
            new SwingWorker<Image, Void>() {
                @Override protected Image doInBackground() { return cache.loadImage(url); }
                @Override protected void done() {
                    try { cardImage = get(); repaint(); } catch (Exception ignored) {}
                }
            }.execute();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Fondo redondeado
            g2.setColor(BG_CARD);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            int imgH = 210;
            if (cardImage != null) {
                int imgW = cardImage.getWidth(this), imgH2 = cardImage.getHeight(this);
                double scale = Math.min((double)(getWidth() - 8) / imgW, (double) imgH / imgH2);
                int dw = (int)(imgW * scale), dh = (int)(imgH2 * scale);
                int x = (getWidth() - dw) / 2, y = (imgH - dh) / 2 + 4;
                g2.setClip(new RoundRectangle2D.Double(x, y, dw, dh, 6, 6));
                g2.drawImage(cardImage, x, y, dw, dh, this);
                g2.setClip(null);
            } else {
                g2.setColor(new Color(45, 45, 58));
                g2.fillRoundRect(4, 4, getWidth() - 8, imgH - 4, 8, 8);
            }
            g2.dispose();
        }
    }

    // ─── Componentes custom ───────────────────────────────────────────────────

    /** Botón cierre — X dibujada con lineas, sin Unicode */
    public static class CloseButton extends JButton {
        private boolean hovered = false;
        public CloseButton() {
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hovered) {
                g2.setColor(new Color(200, 50, 50));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.setColor(hovered ? Color.WHITE : new Color(180, 180, 180));
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = getWidth() / 2, cy = getHeight() / 2, r = 6;
            g2.drawLine(cx - r, cy - r, cx + r, cy + r);
            g2.drawLine(cx + r, cy - r, cx - r, cy + r);
            g2.dispose();
        }
    }

    /** Avatar circular con initial, dibujado con antialiasing */
    public static class AvatarButton extends JButton {
        private final String initial;
        public AvatarButton(String initial) {
            this.initial = initial;
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? ACCENT.brighter() : ACCENT);
            g2.fill(new Ellipse2D.Double(0, 0, getWidth(), getHeight()));
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial,
                (getWidth()  - fm.stringWidth(initial)) / 2,
                (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Botón pill redondeado */
    public static class PillButton extends JButton {
        private final Color normal, hover;
        public PillButton(String text, Color normal, Color hover) {
            super(text);
            this.normal = normal; this.hover = hover;
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? hover : normal);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    public static class FavButton extends JButton {
        private boolean active = false;
        public FavButton() {
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(100, 24));
        }
        public void setActive(boolean a) { active = a; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color color = active ? new Color(255, 200, 50) : new Color(140, 140, 160);
            // Dibujar estrella manualmente
            drawStar(g2, 12, 12, 7, 3, active ? new Color(255, 200, 50) : new Color(100, 100, 120));
            g2.setColor(color);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            String label = active ? "Guardado" : "Visto";
            g2.drawString(label, 24, 16);
            g2.dispose();
        }
        private void drawStar(Graphics2D g2, int cx, int cy, int outerR, int innerR, Color color) {
            int points = 5;
            int[] xp = new int[points * 2], yp = new int[points * 2];
            for (int i = 0; i < points * 2; i++) {
                double angle = Math.PI / points * i - Math.PI / 2;
                int r = (i % 2 == 0) ? outerR : innerR;
                xp[i] = (int)(cx + r * Math.cos(angle));
                yp[i] = (int)(cy + r * Math.sin(angle));
            }
            g2.setColor(color);
            g2.fillPolygon(xp, yp, points * 2);
        }
    }
}