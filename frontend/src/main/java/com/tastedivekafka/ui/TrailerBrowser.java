package com.tastedivekafka.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;

public class TrailerBrowser extends JDialog {

    private static final int DIALOG_WIDTH  = 1024;
    private static final int DIALOG_HEIGHT = 620;
    private static final int TOOLBAR_H     = 42;

    // Singletons: una sola instancia de CefApp/CefClient por JVM
    private static volatile CefApp    cefApp;
    private static volatile CefClient cefClient;
    private static volatile boolean   cefReady = false;

    private volatile CefBrowser browser;

    public TrailerBrowser(Window owner, String movieTitle, String trailerUrl) {
        super(owner, "▶  " + movieTitle, ModalityType.APPLICATION_MODAL);

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(12, 12, 12));

        add(buildToolbar(movieTitle, trailerUrl), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(new Color(12, 12, 12));
        center.add(buildLoadingPanel(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        getRootPane().registerKeyboardAction(
            e -> closeSafely(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { closeSafely(); }
        });

       new SwingWorker<CefBrowser, Void>() {

            @Override
            protected CefBrowser doInBackground() throws Exception {
                initCef();
                return cefClient.createBrowser(trailerUrl, false, false);
            }

                @Override
                protected void done() {
                    try {
                        CefBrowser b = get(); 
                        browser = b;
                        center.removeAll();
                        center.add(b.getUIComponent(), BorderLayout.CENTER);
                        center.revalidate();
                        center.repaint();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();            
                        JOptionPane.showMessageDialog(center, "Operación interrumpida.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    } catch (ExecutionException ex) {
                        JOptionPane.showMessageDialog(center, ex.getCause().getMessage(), "Error al cargar el tráiler", JOptionPane.ERROR_MESSAGE);
                    }
                }

        }.execute();    
    }
    

    public static synchronized void initCef() throws Exception {
        if (cefReady) return;

        CefAppBuilder builder = new CefAppBuilder();

        builder.setInstallDir(new File(System.getProperty("user.home"), ".jcef-bundle"));

        builder.setProgressHandler(new ConsoleProgressHandler());

        builder.setAppHandler(new MavenCefAppHandlerAdapter() {});

        builder.getCefSettings().windowless_rendering_enabled = false;
        
        builder.getCefSettings().log_severity = org.cef.CefSettings.LogSeverity.LOGSEVERITY_FATAL;
        
        builder.addJcefArgs("--ignore-gpu-blocklist", "--enable-gpu-rasterization");


        cefApp    = builder.build();
        cefClient = cefApp.createClient();

        cefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                SwingUtilities.invokeLater(() -> {
                    Window w = SwingUtilities.windowForComponent(browser.getUIComponent());
                    if (w instanceof JDialog d && title != null && !title.isBlank())
                        d.setTitle("▶  " + title);
                });
            }
        });

        cefClient.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadError(CefBrowser b, org.cef.browser.CefFrame frame,
                                    org.cef.handler.CefLoadHandler.ErrorCode code,
                                    String errorText, String failedUrl) {
                System.err.printf("[JCEF] Error carga [%s]: %s%n", code, failedUrl);
            }
        });

        cefReady = true;
        System.out.println("[JCEF] Chromium listo.");
    }

    private void closeSafely() {
        CefBrowser b = browser;
        if (b != null) {
            browser = null;
            b.close(true);
        }
        dispose();
    }

    public static synchronized void shutdown() {
        if (cefApp != null) {
            cefApp.dispose();
            cefApp    = null;
            cefClient = null;
            cefReady  = false;
        }
    }

    private JPanel buildToolbar(String movieTitle, String trailerUrl) {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(new Color(22, 22, 22));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 50, 50)),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        bar.setPreferredSize(new Dimension(DIALOG_WIDTH, TOOLBAR_H));

        JLabel title = new JLabel("▶  " + movieTitle);
        title.setForeground(new Color(215, 215, 215));
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        bar.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controls.setOpaque(false);

        JButton btnGpu = btn("GPU info", new Color(40, 80, 40));
        JButton btnBack  = btn("←",                 new Color(50, 50, 50));
        JButton btnFwd   = btn("→",                 new Color(50, 50, 50));
        JButton btnRel   = btn("↺ Recargar",        new Color(50, 50, 50));
        JButton btnExt   = btn("↗ Navegador",       new Color(35, 70, 105));
        JButton btnClose = btn("✕ Cerrar",          new Color(150, 28, 28));

        
        btnGpu.addActionListener(e -> { CefBrowser b = browser; if (b != null) b.loadURL("chrome://gpu"); });
        btnBack .addActionListener(e -> { CefBrowser b = browser; if (b != null) b.goBack(); });
        btnFwd  .addActionListener(e -> { CefBrowser b = browser; if (b != null) b.goForward(); });
        btnRel  .addActionListener(e -> { CefBrowser b = browser; if (b != null) b.reload(); });
        btnExt  .addActionListener(e -> openExternal(trailerUrl));
        btnClose.addActionListener(e -> closeSafely());

        
        controls.add(btnGpu);
        controls.add(btnBack);
        controls.add(btnFwd);
        controls.add(btnRel);
        controls.add(btnExt);
        controls.add(btnClose);
        bar.add(controls, BorderLayout.EAST);
        return bar;
    }

    private static JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(bg.brighter()); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }

    private static JPanel buildLoadingPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(12, 12, 12));
        JLabel lbl = new JLabel("Iniciando Chromium…");
        lbl.setForeground(new Color(140, 140, 140));
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        p.add(lbl);
        return p;
    }

    private static void openExternal(String url) {
        try { Desktop.getDesktop().browse(new URI(url)); }
        catch (IOException | URISyntaxException ex) {
            JOptionPane.showMessageDialog(null,
                "No se pudo abrir el navegador: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void openTrailer(Window owner, String movieTitle, String trailerUrl) {
        new TrailerBrowser(owner, movieTitle, trailerUrl).setVisible(true);
    }
}