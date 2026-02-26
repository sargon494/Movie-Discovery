package com.tastedivekafka.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.tastedivekafka.FrontendApp;
import com.tastedivekafka.session.AppSession;

/**
 * MainFrame es la ventana principal de la aplicación, que muestra el campo de búsqueda
 * y la galería de recomendaciones.
 * Al iniciar, verifica que haya una sesión activa y muestra el nombre del usuario en el título.
 * Permite buscar películas, mostrar recomendaciones con imágenes y trailers, y cerrar sesión.
 * La comunicación con el backend se hace a través de BackendClient, y las imágenes se cachean
 * localmente para mejorar el rendimiento.
 */
public class MainFrame extends JFrame {

    private static final int WINDOW_WIDTH  = 900;
    private static final int WINDOW_HEIGHT = 650;

    private final JTextField searchField  = new JTextField();
    private final JPanel     galleryPanel = new JPanel();
    private final ImageCache imageCache   = new ImageCache();

    private int dragX, dragY;

    public MainFrame() {
        if (!AppSession.isLogged()) {
            throw new IllegalStateException("No hay sesión activa");
        }
        setTitle("MovieDiscovery - Usuario: " + AppSession.getCurrentUser());
        initUI();
    }

    /* ===================== SEARCH ===================== */

    private void onSearch() {
        String movie = searchField.getText().trim();
        if (movie.isEmpty() || movie.equals("Ingrese el nombre de la película")) return;

        galleryPanel.removeAll();
        galleryPanel.add(new JLabel("Buscando recomendaciones...", SwingConstants.CENTER));
        galleryPanel.revalidate();

        new Thread(() -> {
            try {
                String response = BackendClient.search(movie);
                SwingUtilities.invokeLater(() -> updateGallery(response));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    galleryPanel.removeAll();
                    galleryPanel.add(new JLabel(
                            "Error al conectar con el servidor: " + e.getMessage(),
                            SwingConstants.CENTER));
                    galleryPanel.revalidate();
                });
            }
        }, "search-thread").start();
    }

    private void updateGallery(String response) {
        if (!response.contains("||")) {
            System.out.println("Ignorando mensaje de formato antiguo: " + response);
            return;
        }
        galleryPanel.removeAll();
        for (String movieData : response.split(";;")) {
            String[] parts = movieData.split("\\|\\|");
            if (parts.length >= 3) {
                galleryPanel.add(MovieCard.create(parts[0], parts[1], parts[2], imageCache));
            }
        }
        galleryPanel.revalidate();
        galleryPanel.repaint();
    }

    /* ===================== UI ===================== */

    private void initUI() {
        setUndecorated(true);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        BackgroundPanel bgPanel = new BackgroundPanel();
        setContentPane(bgPanel);

        // Barra de título personalizada
        JPanel menuBar = new JPanel(null);
        menuBar.setBounds(0, 0, WINDOW_WIDTH, 30);
        menuBar.setBackground(new Color(45, 45, 45));

        JLabel btnExit = new JLabel("X", SwingConstants.CENTER);
        btnExit.setBounds(WINDOW_WIDTH - 40, 0, 40, 30);
        btnExit.setForeground(Color.WHITE);
        btnExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExit.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { 
                TrailerBrowser.shutdown();
                System.exit(0); }
        });

        menuBar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { dragX = e.getX(); dragY = e.getY(); }
        });
        menuBar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - dragX, e.getYOnScreen() - dragY);
            }
        });
        menuBar.add(btnExit);
        bgPanel.add(menuBar);

        // Contenedor principal
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.setBounds(20, 50, WINDOW_WIDTH - 40, WINDOW_HEIGHT - 70);

        // Barra de búsqueda
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        searchBox.setOpaque(false);
        searchField.setPreferredSize(new Dimension(300, 30));

        JButton btnSearch = new JButton("Buscar");
        btnSearch.setBackground(new Color(70, 130, 180));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> onSearch());

        JButton logoutButton = new JButton("Cerrar sesión");
        logoutButton.setBackground(new Color(220, 50, 50));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setPreferredSize(new Dimension(150, 30));
        logoutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { logoutButton.setBackground(new Color(255, 80, 80)); }
            @Override
            public void mouseExited(MouseEvent e)  { logoutButton.setBackground(new Color(220, 50, 50)); }
        });
        logoutButton.addActionListener(e -> {
            AppSession.logout();
            MainFrame.this.dispose();
            FrontendApp.showLogin();
        });

        searchBox.add(searchField);
        searchBox.add(btnSearch);
        searchBox.add(logoutButton);
        mainContainer.add(searchBox, BorderLayout.NORTH);

        // Galería de películas
        galleryPanel.setLayout(new GridLayout(0, 4, 20, 20));
        galleryPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(galleryPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        mainContainer.add(scroll, BorderLayout.CENTER);
        bgPanel.add(mainContainer);

        warmUpChromium();
    }

    private static void warmUpChromium() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                TrailerBrowser.initCef();   // ← hazlo package-private o public
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    System.out.println("[JCEF] Warm-up completado.");
                } catch (InterruptedException | ExecutionException ex) {
                    System.err.println("[JCEF] Warm-up falló: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /* ===================== MovieCard ===================== */

    private static class MovieCard extends JPanel {

        private Image  cardImage;
        private final String cardTrailerURL;
        private final String cardTitle;

        public static MovieCard create(String title, String imageURL,
                                       String trailerURL, ImageCache cache) {
            MovieCard card = new MovieCard(title, trailerURL);
            card.loadImageAsync(imageURL, cache);
            return card;
        }

        private MovieCard(String title, String trailerURL) {
            this.cardTitle      = title;
            this.cardTrailerURL = trailerURL;

            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(150, 250));
            setOpaque(false);

            JLabel lbl = new JLabel(
                "<html><u>▶ " + title + "</u></html>", SwingConstants.CENTER);
            lbl.setForeground(new Color(100, 180, 255));
            lbl.setToolTipText("Ver trailer de " + title);
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            lbl.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) { openTrailer(); }
            });
            add(lbl, BorderLayout.SOUTH);
        }

        private void loadImageAsync(String imageURL, ImageCache cache) {
            new SwingWorker<Image, Void>() {
                @Override
                protected Image doInBackground() {
                    return cache.loadImage(imageURL);
                }
                @Override
                protected void done() {
                    try {
                        cardImage = get();
                        repaint();
                    } catch (InterruptedException | ExecutionException ignored) {}
                }
            }.execute();
        }

        private void openTrailer() {
            Window parent = SwingUtilities.getWindowAncestor(this);
            TrailerBrowser.openTrailer(parent, cardTitle, cardTrailerURL);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int cardWidth  = getWidth();
            int cardHeight = 200;
            if (cardImage != null) {
                int imgW = cardImage.getWidth(this), imgH = cardImage.getHeight(this);
                double scale = Math.min(140.0 / imgW, (double) cardHeight / imgH);
                int dw = (int)(imgW * scale), dh = (int)(imgH * scale);
                g.drawImage(cardImage, (cardWidth - dw) / 2, (cardHeight - dh) / 2, dw, dh, this);
            } else {
                g.setColor(Color.DARK_GRAY);
                g.fillRect((cardWidth - 140) / 2, 0, 140, cardHeight);
            }
        }
    }

    /* ===================== BackgroundPanel ===================== */

    static class BackgroundPanel extends JPanel {
        public BackgroundPanel() { setLayout(null); setBackground(new Color(25, 25, 25)); }
        @Override protected void paintComponent(Graphics g) { super.paintComponent(g); }
    }
}