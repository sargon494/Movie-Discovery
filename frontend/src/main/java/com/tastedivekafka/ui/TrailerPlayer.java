package com.tastedivekafka.ui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.Scene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class TrailerPlayer extends JDialog {
    
    private static final int WIDTH = 854; // Ancho estándar para trailers de YouTube
    private static final int HEIGHT = 530; // Alto estándar para trailers de YouTube

    public TrailerPlayer(Frame parent, String title, String youtubeUrl) {
        super(parent, "▶ " + title, true);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        Platform.setImplicitExit(false); // Evita que la aplicación JavaFX se cierre al cerrar el diálogo

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(new Color(15, 15, 15));

        JFXPanel fxPanel = new JFXPanel();
        fxPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT - 50)); // Deja espacio para el botón de cerrar

        JButton btnClose = new JButton("x Cerrar");
        btnClose.setBackground(new Color(60, 60, 60));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorderPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> closePlayer(fxPanel));

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.setBackground(new Color(30, 30, 30));
        bottomBar.add(btnClose);

        container.add(fxPanel, BorderLayout.CENTER);
        container.add(bottomBar, BorderLayout.SOUTH);
        setContentPane(container);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { closePlayer(fxPanel); }
        });

         Platform.runLater(() -> initWebView(fxPanel, youtubeUrl));
    }
    
    private void initWebView(JFXPanel fxPanel, String youtubeUrl) {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        engine.setJavaScriptEnabled(true);

        String embedUrl = buildEmbedUrl(youtubeUrl);
        engine.loadContent(buildHtml(embedUrl));

        Scene scene = new Scene(webView);

        fxPanel.setScene(scene);
    }
    
    private String buildEmbedUrl(String url) {
        if (url.contains("watch?v=")) {
         String videoId = url.substring(url.indexOf("watch?v=") + 8);

            if (videoId.contains("&")) {
                videoId = videoId.substring(0, videoId.indexOf("&"));
            } 

            return "https://www.youtube.com/embed/" + videoId + "?autoplay=1&rel=0";

        } return url;

    }

    private String buildHtml(String embedUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <style>
                    * { margin:0; padding:0; background:#000; }
                    iframe { display:block; width:100vw; height:100vh; border:none; }
                  </style>
                </head>
                <body>
                  <iframe src="%s"
                          allow="autoplay; encrypted-media"
                          allowfullscreen>
                  </iframe>
                </body>
                </html>
                """.formatted(embedUrl);
    }

    private void closePlayer(JFXPanel fxPanel) {
        Platform.runLater(() -> {
            if (fxPanel.getScene() != null) {
                WebView wv = (WebView) fxPanel.getScene().getRoot();
                wv.getEngine().load("about:blank");
            }
            SwingUtilities.invokeLater(this::dispose);
        });
    }

}
