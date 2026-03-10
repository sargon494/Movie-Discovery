package com.tastedivekafka.ui;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

public class TrailerBrowser extends JDialog {

    private static final int W = 1024, H = 620, BAR_H = 46;

    private static final Color BG       = new Color(10, 10, 14);
    private static final Color BAR_BG   = new Color(18, 18, 24);
    private static final Color BAR_LINE = new Color(38, 38, 52);
    private static final Color ACCENT   = new Color(99, 155, 255);
    private static final Color DANGER   = new Color(200, 50, 50);
    private static final Color TEXT     = new Color(210, 210, 218);
    private static final Color TEXT_DIM = new Color(100, 100, 120);

    private static volatile CefApp    cefApp;
    private static volatile CefClient cefClient;
    private static volatile boolean   cefReady = false;

    private volatile CefBrowser browser;

    public TrailerBrowser(Window owner, String movieTitle, String trailerUrl) {
        super(owner, movieTitle, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setSize(W, H);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setShape(new RoundRectangle2D.Double(0, 0, W, H, 10, 10));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(BorderFactory.createLineBorder(BAR_LINE, 1));
        setContentPane(root);

        root.add(buildToolbar(movieTitle, trailerUrl), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);
        center.add(buildLoadingPanel(), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        getRootPane().registerKeyboardAction(
            e -> closeSafely(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { closeSafely(); }
        });

        new SwingWorker<CefBrowser, Void>() {
            @Override protected CefBrowser doInBackground() throws Exception {
                initCef();
                return cefClient.createBrowser(trailerUrl, false, false);
            }
            @Override protected void done() {
                try {
                    CefBrowser b = get();
                    browser = b;
                    center.removeAll();
                    center.add(b.getUIComponent(), BorderLayout.CENTER);
                    center.revalidate();
                    center.repaint();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    center.removeAll();
                    center.add(buildErrorPanel(ex.getCause().getMessage()), BorderLayout.CENTER);
                    center.revalidate();
                }
            }
        }.execute();
    }

    // ─── CEF ─────────────────────────────────────────────────────────────────

    public static synchronized void initCef() throws Exception {
        if (cefReady) return;
        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(new File(System.getProperty("user.home"), ".jcef-bundle"));
        builder.setProgressHandler(new ConsoleProgressHandler());
        builder.setAppHandler(new MavenCefAppHandlerAdapter() {});
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.getCefSettings().log_severity = org.cef.CefSettings.LogSeverity.LOGSEVERITY_FATAL;
        builder.addJcefArgs("--ignore-gpu-blocklist", "--enable-gpu-rasterization", "--log-level=3", "--silent-debugger-extension-api");
        cefApp    = builder.build();
        cefClient = cefApp.createClient();
        cefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override public void onTitleChange(CefBrowser browser, String title) {
                SwingUtilities.invokeLater(() -> {
                    Window w = SwingUtilities.windowForComponent(browser.getUIComponent());
                    if (w instanceof JDialog d && title != null && !title.isBlank())
                        d.setTitle(title);
                });
            }
        });
        cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override public void onLoadError(CefBrowser b, org.cef.browser.CefFrame frame,
                org.cef.handler.CefLoadHandler.ErrorCode code, String errorText, String failedUrl) {
                System.err.printf("[JCEF] Error [%s]: %s%n", code, failedUrl);
            }
        });
        cefReady = true;
        System.out.println("[JCEF] Chromium listo.");
    }

    private void closeSafely() {
        CefBrowser b = browser;
        if (b != null) { browser = null; b.close(true); }
        dispose();
    }

    public static synchronized void shutdown() {
        if (cefApp != null) {
            cefApp.dispose();
            cefApp = null; cefClient = null; cefReady = false;
        }
    }

    private static final File JCEF_DIR = new File(System.getProperty("user.home"), ".jcef-bundle");

    public static void openTrailer(Window owner, String movieTitle, String trailerUrl) {
        if (!cefReady && !JCEF_DIR.exists()) {
            showFirstRunNotice(owner);
        }
        new TrailerBrowser(owner, movieTitle, trailerUrl).setVisible(true);
    }

    private static void showFirstRunNotice(Window owner) {
        JDialog notice = new JDialog(owner, "Preparando reproductor", Dialog.ModalityType.APPLICATION_MODAL);
        notice.setUndecorated(true);
        notice.setSize(420, 160);
        notice.setLocationRelativeTo(owner);
        notice.setShape(new RoundRectangle2D.Double(0, 0, 420, 160, 12, 12));

        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(22, 22, 32));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 70), 1),
            BorderFactory.createEmptyBorder(24, 28, 24, 28)));
        notice.setContentPane(panel);

        JLabel title = new JLabel("Descargando reproductor de video");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(new Color(225, 225, 230));

        JLabel msg = new JLabel("<html><body style='width:340px'>La primera vez es necesario descargar el reproductor (~100 MB)."
            + " Esto solo ocurre una vez y tarda aproximadamente 1 minuto.</body></html>");
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        msg.setForeground(new Color(120, 120, 140));

        JButton btnOk = new JButton("Entendido, continuar") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(70, 120, 220) : new Color(99, 155, 255));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btnOk.setOpaque(false); btnOk.setContentAreaFilled(false);
        btnOk.setBorderPainted(false); btnOk.setFocusPainted(false);
        btnOk.setPreferredSize(new Dimension(180, 36));
        btnOk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOk.addActionListener(e -> notice.dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(btnOk);

        panel.add(title,  BorderLayout.NORTH);
        panel.add(msg,    BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        notice.setVisible(true); // bloquea hasta que el usuario pulsa OK
    }

    // ─── Toolbar ─────────────────────────────────────────────────────────────

    private JPanel buildToolbar(String movieTitle, String trailerUrl) {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBackground(BAR_BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BAR_LINE));
        bar.setPreferredSize(new Dimension(W, BAR_H));

        // Lado izquierdo: botones navegación + título
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        left.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 0));

        // Botón atrás — triángulo izquierda
        IconButton btnBack = new IconButton(BAR_H - 16) {
            @Override void drawIcon(Graphics2D g2, int cx, int cy) {
                int[] xs = {cx+5, cx-4, cx+5};
                int[] ys = {cy-6, cy,   cy+6};
                g2.fillPolygon(xs, ys, 3);
            }
        };
        btnBack.setToolTipText("Atrás");
        btnBack.addActionListener(e -> { CefBrowser b = browser; if (b != null) b.goBack(); });

        // Botón adelante — triángulo derecha
        IconButton btnFwd = new IconButton(BAR_H - 16) {
            @Override void drawIcon(Graphics2D g2, int cx, int cy) {
                int[] xs = {cx-5, cx+4, cx-5};
                int[] ys = {cy-6, cy,   cy+6};
                g2.fillPolygon(xs, ys, 3);
            }
        };
        btnFwd.setToolTipText("Adelante");
        btnFwd.addActionListener(e -> { CefBrowser b = browser; if (b != null) b.goForward(); });

        // Botón recargar — círculo con flecha
        IconButton btnReload = new IconButton(BAR_H - 16) {
            @Override void drawIcon(Graphics2D g2, int cx, int cy) {
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(cx - 6, cy - 6, 12, 12, 60, 270);
                // Punta de flecha
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx + 4, cy - 7, cx + 7, cy - 5);
                g2.drawLine(cx + 4, cy - 7, cx + 2, cy - 4);
            }
        };
        btnReload.setToolTipText("Recargar");
        btnReload.addActionListener(e -> { CefBrowser b = browser; if (b != null) b.reload(); });

        // Separador vertical
        JPanel sep = new JPanel();
        sep.setBackground(BAR_LINE);
        sep.setPreferredSize(new Dimension(1, 22));

        // Título de la película
        JLabel titleLbl = new JLabel(movieTitle);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(TEXT);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        left.add(btnBack);
        left.add(btnFwd);
        left.add(btnReload);
        left.add(sep);
        left.add(titleLbl);

        // Lado derecho: abrir externo + cerrar
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));

        // Botón abrir en navegador — cuadrado con flecha diagonal
        IconButton btnExt = new IconButton(BAR_H - 16) {
            @Override void drawIcon(Graphics2D g2, int cx, int cy) {
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Caja
                g2.drawRect(cx - 5, cy - 3, 7, 7);
                // Flecha diagonal
                g2.drawLine(cx + 1, cy - 5, cx + 6, cy - 5);
                g2.drawLine(cx + 6, cy - 5, cx + 6, cy);
                g2.drawLine(cx + 1, cy - 4, cx + 5, cy - 8);
            }
        };
        btnExt.setAccent(ACCENT);
        btnExt.setToolTipText("Abrir en navegador");
        btnExt.addActionListener(e -> openExternal(trailerUrl));

        // Botón cerrar — X con color peligro
        IconButton btnClose = new IconButton(BAR_H - 16) {
            @Override void drawIcon(Graphics2D g2, int cx, int cy) {
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 5, cy - 5, cx + 5, cy + 5);
                g2.drawLine(cx + 5, cy - 5, cx - 5, cy + 5);
            }
        };
        btnClose.setAccent(DANGER);
        btnClose.setToolTipText("Cerrar");
        btnClose.addActionListener(e -> closeSafely());

        right.add(btnExt);
        right.add(btnClose);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ─── Paneles de estado ────────────────────────────────────────────────────

    private static JPanel buildLoadingPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG);
        JLabel lbl = new JLabel(!JCEF_DIR.exists() ? "Descargando reproductor... (solo la primera vez)" : "Cargando...");
        lbl.setForeground(TEXT_DIM);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p.add(lbl);
        return p;
    }

    private static JPanel buildErrorPanel(String msg) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG);
        JLabel lbl = new JLabel("Error al cargar: " + msg);
        lbl.setForeground(new Color(200, 80, 80));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        p.add(lbl);
        return p;
    }

    private static void openExternal(String url) {
        try { Desktop.getDesktop().browse(new URI(url)); }
        catch (IOException | URISyntaxException ex) {
            JOptionPane.showMessageDialog(null, "No se pudo abrir el navegador: " + ex.getMessage());
        }
    }

    // ─── IconButton ──────────────────────────────────────────────────────────

    /**
     * Botón cuadrado con icono dibujado a mano con Graphics2D.
     * Sin texto, sin Unicode — subclasear e implementar drawIcon().
     */
    abstract static class IconButton extends JButton {
        private Color accent = new Color(160, 160, 180);

        IconButton(int size) {
            setPreferredSize(new Dimension(size, size));
            setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        void setAccent(Color c) { this.accent = c; }

        abstract void drawIcon(Graphics2D g2, int cx, int cy);

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean hovered = getModel().isRollover();
            if (hovered) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            }

            g2.setColor(hovered ? accent : new Color(150, 150, 170));
            drawIcon(g2, getWidth() / 2, getHeight() / 2);
            g2.dispose();
        }
    }
}