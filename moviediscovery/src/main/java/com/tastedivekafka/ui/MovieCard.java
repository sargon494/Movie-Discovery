package com.tastedivekafka.ui;

import javax.swing.*;

import com.tastedivekafka.cache.ImageCache;

import java.awt.*;

/**
 * Tarjeta visual de las peliculas
 * Carga la imagen de la pelicula en segundo plano y no bloquea la UI
 */

public class MovieCard extends JPanel{
    private Image image;

    public MovieCard(String title, String genre, String url, ImageCache cache){
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(160, 260));
        setOpaque(false);

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setForeground(Color.WHITE);
        add(lbl, BorderLayout.SOUTH);

        new Thread(() -> {
            image = cache.loadImage(url);
            repaint();
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Imagen
        if(image != null){
            g.drawImage(image, 10, 10, 140, 200, this);
        } else {
            // Place holder por si falla
            g.setColor(Color.DARK_GRAY);
            g.fillRect(10, 10, 140, 200);
        }
        // Titulo
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.BOLD, 12));
    }
}