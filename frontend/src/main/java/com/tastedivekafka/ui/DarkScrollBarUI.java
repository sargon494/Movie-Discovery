package com.tastedivekafka.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Scrollbar personalizada — estilo dark minimalista.
 *
 * Uso:
 *   JScrollPane scroll = new JScrollPane(panel);
 *   DarkScrollBarUI.apply(scroll);
 */
public class DarkScrollBarUI extends BasicScrollBarUI {

    private static final Color TRACK  = new Color(25, 25, 32);
    private static final Color THUMB  = new Color(70, 70, 90);
    private static final Color HOVER  = new Color(99, 155, 255);
    private static final int   RADIUS = 6;
    private static final int   SIZE   = 8;

    private boolean hovered = false;

    // ── Aplicar a un JScrollPane ──────────────────────────────────────────

    public static void apply(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(SIZE + 4, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, SIZE + 4));
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getHorizontalScrollBar().setOpaque(false);
        scroll.setCorner(JScrollPane.LOWER_RIGHT_CORNER, cornerPanel());
    }

    // ── Painting ──────────────────────────────────────────────────────────

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(TRACK);
        g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
        if (bounds.width <= 0 || bounds.height <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color color = (isDragging || hovered) ? HOVER : THUMB;

        // Centrar el thumb con padding
        int pad = 2;
        g2.setColor(color);
        g2.fillRoundRect(
            bounds.x + pad,
            bounds.y + pad,
            bounds.width  - pad * 2,
            bounds.height - pad * 2,
            RADIUS, RADIUS);
        g2.dispose();
    }

    // ── Botones de flecha — ocultos ───────────────────────────────────────

    @Override
    protected JButton createDecreaseButton(int orientation) { return invisibleButton(); }

    @Override
    protected JButton createIncreaseButton(int orientation) { return invisibleButton(); }

    private JButton invisibleButton() {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(0, 0));
        btn.setMinimumSize(new Dimension(0, 0));
        btn.setMaximumSize(new Dimension(0, 0));
        return btn;
    }

    // ── Hover ─────────────────────────────────────────────────────────────

    @Override
    protected void installListeners() {
        super.installListeners();
        scrollbar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                hovered = true;  scrollbar.repaint();
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                hovered = false; scrollbar.repaint();
            }
        });
    }

    // ── Esquina inferior derecha ──────────────────────────────────────────

    private static JPanel cornerPanel() {
        JPanel corner = new JPanel();
        corner.setBackground(new Color(25, 25, 32));
        return corner;
    }
}