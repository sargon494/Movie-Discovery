package com.tastedivekafka.ui;

import com.tastedivekafka.cache.ImageCache;
import com.tastedivekafka.kafka.KafkaProducerService;
import com.tastedivekafka.kafka.KafkaResponseConsumerService;

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

    public MainFrame(KafkaResponseConsumerService responseConsumer) {
        initUI();
        bindConsumer(responseConsumer); // Conectar con consumer de respuestas
    }

    /**
     * Conecta el consumer de Kafka para recibir recomendaciones
     */
    private void bindConsumer(KafkaResponseConsumerService responseConsumer) {
        new Thread(() -> {
            responseConsumer.listen(response -> {
                // Actualizamos la UI en el hilo de Swing
                SwingUtilities.invokeLater(() -> updateGallery(response));
            });
        }).start();
    }

    /**
     * Actualiza la galería de películas con la respuesta recibida
     */
    private void updateGallery(String response) {
        // Si el mensaje no tiene el formato esperado, ignorarlo
        if (!response.contains("||")) {
            System.out.println("Ignorando mensaje de formato antiguo: " + response);
            return; 
        }

        moviesPanel.removeAll();

        String[] movies = response.split(";;"); // Separar cada recomendación
        for (String movieData : movies) {
            String[] parts = movieData.split("\\|\\|");
            if (parts.length >= 3) {
                moviesPanel.add(new MovieCard(parts[0], parts[1], parts[2], imageCache)); // Crear tarjeta
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

        // Buscador
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchBox.setOpaque(false);
        txtMovie.setPreferredSize(new Dimension(300, 30));
        JButton btnSearch = new JButton("Buscar");
        btnSearch.addActionListener(e -> onSearch());
        searchBox.add(txtMovie);
        searchBox.add(btnSearch);
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

    private static class MovieCard extends JPanel{
        private Image img;

        public MovieCard(String title, String genre, String url, ImageCache cache){
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(150, 250));
            setOpaque(false);

            JLabel lbl = new JLabel(title, SwingConstants.CENTER);
            lbl.setForeground(Color.WHITE);
            add(lbl, BorderLayout.SOUTH);

            new Thread(() -> {
                img = cache.loadImage(url);
                repaint();
            }).start();
        }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int cardWidth = getWidth();
        int cardHeight = 200;

        // Imagen
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
            // Place holder por si falla
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