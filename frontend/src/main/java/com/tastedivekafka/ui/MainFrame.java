package com.tastedivekafka.ui;

import com.tastedivekafka.FrontendApp;
import com.tastedivekafka.session.AppSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * MainFrame es la ventana principal de la aplicación, que muestra el campo de búsqueda y la galería de recomendaciones.
 * Al iniciar, verifica que haya una sesión activa y muestra el nombre del usuario en el título
 * Permite buscar películas, mostrar recomendaciones con imágenes y trailers, y cerrar sesión.
 * La comunicación con el backend se hace a través de BackendClient, y las imágenes se cachean localmente para mejorar el rendimiento.
 * El diseño es moderno y minimalista, con un fondo oscuro y elementos claros para resaltar el contenido.
 * Cada película recomendada se muestra como una tarjeta con su imagen y título, y al hacer click se abre el trailer en el navegador.
 * El menú superior es personalizado, sin bordes, y permite arrastrar la ventana. El botón de cerrar sesión cierra la sesión actual y vuelve a la pantalla de login.
 * La UI se actualiza dinámicamente al recibir las recomendaciones del backend, mostrando un mensaje de "Buscando recomendaciones..." mientras se espera la respuesta.
 * El código está organizado en métodos claros para manejar la búsqueda, actualizar la galería y construir la interfaz, con clases internas para los componentes específicos como MovieCard y BackgroundPanel.
 * En resumen, MainFrame es el corazón de la experiencia del usuario, integrando la lógica de búsqueda, presentación de resultados y gestión de sesión en una interfaz atractiva y funcional.
 */
public class MainFrame extends JFrame {

    private static final int WIDTH  = 900;
    private static final int HEIGHT = 650;

    private final JTextField txtMovie    = new JTextField();
    private final JPanel     moviesPanel = new JPanel();

    // Frontend keeps its own image cache — no backend dep required
    private final ImageCache imageCache = new ImageCache();
    private int xMouse, yMouse;

    public MainFrame() {
        if (!AppSession.isLogged()) {
            throw new IllegalStateException("No hay sesión activa");
        }
        setTitle("MovieDiscovery - Usuario: " + AppSession.getCurrentUser());
        initUI();
    }

    /* ===================== SEARCH ===================== */

    /**
     * Al hacer click en "Buscar", se toma el texto del campo, se muestra un mensaje de "Buscando recomendaciones..." y se lanza un hilo para llamar a BackendClient.search(movie).
     * Ejecuta la búsqueda en un hilo separado para no bloquear la UI, y cuando recibe la respuesta, llama a updateGallery() para mostrar las recomendaciones. 
     * Si hay error de conexión, muestra un mensaje de error en el panel.
     */
    private void onSearch() {
        String movie = txtMovie.getText().trim();
        if (movie.isEmpty() || movie.equals("Ingrese el nombre de la película")) return;

        // Show "searching" immediately
        moviesPanel.removeAll();
        moviesPanel.add(new JLabel("Buscando recomendaciones...", SwingConstants.CENTER));
        moviesPanel.revalidate();

        new Thread(() -> {
            try {
                String response = BackendClient.search(movie); // POST /search
                SwingUtilities.invokeLater(() -> updateGallery(response));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    moviesPanel.removeAll();
                    moviesPanel.add(new JLabel(
                            "Error al conectar con el servidor: " + e.getMessage(),
                            SwingConstants.CENTER));
                    moviesPanel.revalidate();
                });
            }
        }, "search-thread").start();
    }

    /**
     * Se prendea la galería de películas a partir de la respuesta del backend, que tiene el formato:
     * "Title||imgURL||trailerURL;;Title2||imgURL2||trailerURL2;;...".
     * Cada película se muestra como un MovieCard con su imagen y título, y al hacer click se abre el trailer. 
     */
    private void updateGallery(String response) {
        if (!response.contains("||")) {
            System.out.println("Ignorando mensaje de formato antiguo: " + response);
            return;
        }
        moviesPanel.removeAll();
        for (String movieData : response.split(";;")) {
            String[] parts = movieData.split("\\|\\|");
            if (parts.length >= 3) {
                moviesPanel.add(new MovieCard(parts[0], "", parts[1], parts[2], imageCache));
            }
        }
        moviesPanel.revalidate();
        moviesPanel.repaint();
    }

    /* ===================== UI ===================== */
    private void initUI() {
        setUndecorated(true);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        BackgroundPanel bgPanel = new BackgroundPanel();
        setContentPane(bgPanel);

        JPanel menuBar = new JPanel(null);
        menuBar.setBounds(0, 0, WIDTH, 30);
        menuBar.setBackground(new Color(45, 45, 45));

        JLabel btnExit = new JLabel("X", SwingConstants.CENTER);
        btnExit.setBounds(WIDTH - 40, 0, 40, 30);
        btnExit.setForeground(Color.WHITE);
        btnExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnExit.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent e) { System.exit(0); }
        });

        menuBar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(MouseEvent e) { xMouse = e.getX(); yMouse = e.getY(); }
        });
        menuBar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - xMouse, e.getYOnScreen() - yMouse);
            }
        });
        menuBar.add(btnExit);
        bgPanel.add(menuBar);

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.setBounds(20, 50, WIDTH - 40, HEIGHT - 70);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        searchBox.setOpaque(false);
        txtMovie.setPreferredSize(new Dimension(300, 30));

        JButton btnSearch = new JButton("Buscar");
        btnSearch.setBackground(new Color(70, 130, 180));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> onSearch());
        searchBox.add(txtMovie);
        searchBox.add(btnSearch);

        JButton logoutButton = new JButton("Cerrar sesión");
        logoutButton.setBackground(new Color(220, 50, 50));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setPreferredSize(new Dimension(150, 30));
        logoutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(MouseEvent e) { logoutButton.setBackground(new Color(255, 80, 80)); }
            public void mouseExited(MouseEvent e)  { logoutButton.setBackground(new Color(220, 50, 50)); }
        });

        logoutButton.addActionListener(e -> {
            AppSession.logout();
            MainFrame.this.dispose();
            // Show login again — FrontendApp.showLogin() handles creating the new session
            FrontendApp.showLogin();
        });

        searchBox.add(logoutButton);
        mainContainer.add(searchBox, BorderLayout.NORTH);

        moviesPanel.setLayout(new GridLayout(0, 4, 20, 20));
        moviesPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(moviesPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        mainContainer.add(scroll, BorderLayout.CENTER);
        bgPanel.add(mainContainer);
    }

    /* ===================== MovieCard ===================== */
    private static class MovieCard extends JPanel {
        private Image img;
        private final String trailerURL;
        private String title;

        public MovieCard(String title, String genre, String imageURL,
                         String trailerURL, ImageCache cache) {
            this.trailerURL = trailerURL;
            this.title = title;
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(150, 250));
            setOpaque(false);

            JLabel lbl = new JLabel("<html><u>" + title + "</u></html>", SwingConstants.CENTER);
            lbl.setForeground(Color.WHITE);
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            lbl.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { openTrailer(); }
            });
            add(lbl, BorderLayout.SOUTH);

            new Thread(() -> {
                img = cache.loadImage(imageURL);
                repaint();
            }).start();
        }

        private void openTrailer() {
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            TrailerPlayer player = new TrailerPlayer(parentFrame, title, trailerURL);
            player.setVisible(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int cardWidth  = getWidth();
            int cardHeight = 200;
            if (img != null) {
                int imgW = img.getWidth(this), imgH = img.getHeight(this);
                double scale = Math.min(140.0 / imgW, (double) cardHeight / imgH);
                int dw = (int)(imgW * scale), dh = (int)(imgH * scale);
                g.drawImage(img, (cardWidth - dw) / 2, (cardHeight - dh) / 2, dw, dh, this);
            } else {
                g.setColor(Color.DARK_GRAY);
                g.fillRect((cardWidth - 140) / 2, 0, 140, cardHeight);
            }
        }
    }

    static class BackgroundPanel extends JPanel {
        public BackgroundPanel() { setLayout(null); setBackground(new Color(25, 25, 25)); }
        @Override protected void paintComponent(Graphics g) { super.paintComponent(g); }
    }
}