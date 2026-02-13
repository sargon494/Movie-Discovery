package com.tastedivekafka.ui;

import com.tastedivekafka.cache.ImageCache;
import com.tastedivekafka.kafka.KafkaProducerService;
import com.tastedivekafka.kafka.KafkaResponseConsumerService;
import com.tastedivekafka.session.AppSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Ventana principal de la aplicación MovieDiscovery.
 *
 * Funciones principales:
 *  - Introducir nombre de película
 *  - Enviar petición a Kafka (Producer)
 *  - Escuchar recomendaciones (Consumer)
 *  - Mostrar resultados en una galería de tarjetas
 */
public class MainFrame extends JFrame {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 650;

    private final JTextField txtMovie = new JTextField();
    private final JPanel moviesPanel = new JPanel();

    private final KafkaProducerService producer = new KafkaProducerService();
    private final ImageCache imageCache = new ImageCache();
    private int xMouse, yMouse; // Para arrastrar ventana
    private final KafkaResponseConsumerService responseConsumer;

    public MainFrame(KafkaResponseConsumerService responseConsumer) {
        this.responseConsumer = responseConsumer;
        if (!AppSession.isLogged()) {
            throw new IllegalStateException("No hay sesión activa");
        }

        setTitle("MovieDiscovery - Usuario: " + AppSession.getCurrentUser());
        initUI();
        this.responseConsumer.listen(response ->
            SwingUtilities.invokeLater(() ->
        updateGallery(response))
        );
    }

    /**
     * Actualiza la galería de películas con la respuesta recibida
     */
    private void updateGallery(String response) {
        if (!response.contains("||")) {
            System.out.println("Ignorando mensaje de formato antiguo: " + response);
            return; 
        }

        moviesPanel.removeAll();

        String[] movies = response.split(";;"); // Separar cada recomendación
        for (String movieData : movies) {
            String[] parts = movieData.split("\\|\\|");
            if (parts.length >= 3) {
                moviesPanel.add(new MovieCard(parts[0],"", parts[1], parts[2], imageCache)); // Crear tarjeta
            }
        }

        moviesPanel.revalidate();
        moviesPanel.repaint();
    }

    /**
     * Enviar búsqueda al producer y mostrar mensaje temporal
     */
    private void onSearch() {
        String movie = txtMovie.getText().trim();
        if (movie.isEmpty() || movie.equals("Ingrese el nombre de la película")) return;

        producer.send(movie); // Enviamos petición a Kafka
        moviesPanel.removeAll();
        moviesPanel.add(new JLabel("Buscando recomendaciones...", SwingConstants.CENTER));
        moviesPanel.revalidate();
    }

    /**
     * Inicializa la UI
     */
    private void initUI() {
        setUndecorated(true);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        BackgroundPanel bgPanel = new BackgroundPanel();
        setContentPane(bgPanel);

        // Barra superior para cerrar/arrastrar
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
            public void mouseDragged(MouseEvent e) { setLocation(e.getXOnScreen() - xMouse, e.getYOnScreen() - yMouse); }
        });
        menuBar.add(btnExit);
        bgPanel.add(menuBar);

        // Contenedor principal
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setOpaque(false);
        mainContainer.setBounds(20, 50, WIDTH - 40, HEIGHT - 70);

        // --- Buscador con botón Logout a la derecha ---
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        searchBox.setOpaque(false);
        txtMovie.setPreferredSize(new Dimension(300, 30));

        // Botón Buscar
        JButton btnSearch = new JButton("Buscar");
        btnSearch.setBackground(new Color(70, 130, 180));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> onSearch());
        searchBox.add(txtMovie);
        searchBox.add(btnSearch);

        // Botón Logout a la derecha del buscador
        JButton logoutButton = new JButton("Cerrar sesión");
        logoutButton.setBackground(new Color(220, 50, 50));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setPreferredSize(new Dimension(150, 30)); // tamaño fijo

        // Hover style
        logoutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(MouseEvent e) { logoutButton.setBackground(new Color(255, 80, 80)); }
            public void mouseExited(MouseEvent e) { logoutButton.setBackground(new Color(220, 50, 50)); }
        });

        /* 
            Nota: El botón de logout cierra la sesión, detiene el consumer de respuestas actual
            y vuelve a mostrar la ventana de login. Esto asegura que no haya listeners activos
            leyendo el topic de Kafka después de cerrar sesión.
         */

        logoutButton.addActionListener(e -> {
            AppSession.logout(); // Limpiamos sesión
            responseConsumer.shutdown(); // Detenemos consumer de respuestas anterior
            MainFrame.this.dispose(); // Cerramos ventana principal

            /*
                Al abrir una nueva ventana de login, se crea un nuevo KafkaResponseConsumerService
                para la nueva sesión. Esto evita que múltiples consumers estén leyendo el mismo topic
                y permite que cada sesión tenga su propio listener de respuestas. 
            */

            LoginFrame login = new LoginFrame(new LoginFrame.LoginListener() {
                @Override
                public void onLoginSuccess() {
                    KafkaResponseConsumerService newConsumer= new KafkaResponseConsumerService(); // Creamos nuevo consumer para la nueva sesión
                    MainFrame main = new MainFrame(newConsumer);
                    main.setVisible(true);
                }

                @Override
                public void onLoginFailure(String reason) {
                    JOptionPane.showMessageDialog(null, reason);
                }
            });
            login.setVisible(true);

            MainFrame.this.dispose(); // Cerramos ventana principal
        });    

        searchBox.add(logoutButton); // Agregamos a la derecha
        mainContainer.add(searchBox, BorderLayout.NORTH);

        // Panel de películas (grid)
        moviesPanel.setLayout(new GridLayout(0, 4, 20, 20));
        moviesPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(moviesPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        mainContainer.add(scroll, BorderLayout.CENTER);

        bgPanel.add(mainContainer);
    }

    /*
        * Tarjeta individual de película. Muestra imagen, título y al hacer click abre trailer.
         * La imagen se carga de forma asíncrona usando ImageCache para mejorar rendimiento.
         * Al hacer click en el título, se intenta abrir el trailer en el navegador predeterminado.
         * Si no se puede abrir el trailer, se muestra un mensaje de error.
    */

    private static class MovieCard extends JPanel{
        private Image img;
        private final String trailerURL;

        public MovieCard(String title, String genre, String imageURL, String trailerURL, ImageCache cache){
            this.trailerURL = trailerURL;

            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(150, 250));
            setOpaque(false);

            JLabel lbl = new JLabel("<html><u>" + title + "</u></html>", SwingConstants.CENTER);
            lbl.setForeground(Color.WHITE);
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            lbl.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openTrailer();
                }
            });

            add(lbl, BorderLayout.SOUTH);

            new Thread(() -> {
                img = cache.loadImage(imageURL);
                repaint();
            }).start();
        }

        private void openTrailer() {
            try {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new java.net.URI(trailerURL));
                } else {
                    JOptionPane.showMessageDialog(this, "Navegador no soportado.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "No se pudo abrir el trailer.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int cardWidth = getWidth();
            int cardHeight = 200;

            if(img != null){
                int imgWidth = img.getWidth(this);
                int imgHeight = img.getHeight(this);
                double scale = Math.min((double)140 / imgWidth, (double)cardHeight / imgHeight);
                int drawWidth = (int)(imgWidth * scale);
                int drawHeight = (int)(imgHeight * scale);
                int x = (cardWidth - drawWidth) / 2;
                int y = (cardHeight - drawHeight) / 2;
                g.drawImage(img, x, y, drawWidth, drawHeight, this);
            } else {
                g.setColor(Color.DARK_GRAY);
                int x = (cardWidth - 140) / 2;
                int y = 0;
                g.fillRect(x, y, 140, cardHeight);
            }
        }
    }

    // --- PANEL DE FONDO ---
    static class BackgroundPanel extends JPanel {
        public BackgroundPanel() { setLayout(null); setBackground(new Color(25, 25, 25)); }
        @Override protected void paintComponent(Graphics g) { super.paintComponent(g); }
    }
}
