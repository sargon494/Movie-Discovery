package com.tastedivekafka.ui;

import com.tastedivekafka.db.UserDAO;

import java.awt.Color;
import javax.swing.*;

public class LoginFrame extends JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(LoginFrame.class.getName());

    int xMouse, yMouse;

    public interface LoginListener {
        void onLoginSuccess();
        void onLoginFailure(String reason);
    }

    private LoginListener loginListener;

    public LoginFrame(LoginListener listener) {
        this.loginListener = listener;
        initComponents();
    }
    private void initComponents() {

        background = new JPanel();
        menuBar = new JPanel();
        bar = new JPanel();
        exitBtn = new JLabel();
        entrar = new JPanel();
        entrarlbl = new JLabel();
        passWordLabel = new JLabel();
        jPasswordField1 = new JPasswordField();
        userField = new JTextField();
        UserLabel = new JLabel();
        logo = new JLabel();
        jSeparator1 = new JSeparator();
        bgphoto = new JLabel();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
        setUndecorated(true);
        setResizable(false);

        // ─────────────────────────────────────────────
        // IMPORTANTE: NO NetBeans AbsoluteLayout
        // ─────────────────────────────────────────────
        background.setLayout(null);
        background.setBackground(new Color(255, 255, 255));

        menuBar.setForeground(new Color(102, 102, 102));
        menuBar.setBounds(0, 0, 760, 20);
        menuBar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                menuBarMouseDragged(evt);
            }
        });
        menuBar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                menuBarMousePressed(evt);
            }
        });

        bar.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        bar.setBounds(733, 0, 27, 20);

        exitBtn.setHorizontalAlignment(SwingConstants.CENTER);
        exitBtn.setText("x");
        exitBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        exitBtn.setBounds(0, 0, 27, 20);
        exitBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                exitBtnMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exitBtnMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exitBtnMouseExited(evt);
            }
        });

        bar.setLayout(null);
        bar.add(exitBtn);
        menuBar.add(bar);

        background.add(menuBar);

        entrarlbl.setFont(new java.awt.Font("Roboto Medium", 0, 12));
        entrarlbl.setHorizontalAlignment(SwingConstants.CENTER);
        entrarlbl.setText("ENTRAR");
        entrarlbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        entrarlbl.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                entrarlblMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                entrarlblMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                entrarlblMouseExited(evt);
            }
        });

        entrar.setBounds(150, 400, 130, 40);
        entrar.setLayout(null);
        entrarlbl.setBounds(0, 0, 130, 40);
        entrar.add(entrarlbl);

        background.add(entrar);

        passWordLabel.setFont(new java.awt.Font("Roboto Medium", 0, 12));
        passWordLabel.setForeground(new Color(255, 255, 255));
        passWordLabel.setText("CONTRASEÑA");
        passWordLabel.setBounds(150, 330, 120, 20);
        background.add(passWordLabel);

        jPasswordField1.setFont(new java.awt.Font("Roboto Light", 0, 12));
        jPasswordField1.setText("********");
        jPasswordField1.setBounds(150, 350, 530, 25);
        jPasswordField1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jPasswordField1MousePressed(evt);
            }
        });
        background.add(jPasswordField1);

        userField.setFont(new java.awt.Font("Roboto Light", 0, 12));
        userField.setText("Ingrese su nombre de usuario");
        userField.setBounds(150, 270, 530, 25);
        userField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                userFieldMousePressed(evt);
            }
        });
        background.add(userField);

        UserLabel.setFont(new java.awt.Font("Roboto Medium", 0, 12));
        UserLabel.setForeground(new Color(255, 255, 255));
        UserLabel.setText("USUARIO");
        UserLabel.setBounds(150, 250, 80, 20);
        background.add(UserLabel);

        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setIcon(new ImageIcon(getClass().getResource("/images/logo.png")));
        logo.setBounds(0, 50, 760, 200);
        background.add(logo);

        jSeparator1.setBounds(150, 310, 530, 20);
        background.add(jSeparator1);

        bgphoto.setIcon(new ImageIcon(getClass().getResource("/images/photo-1614850523011-8f49ffc73908.jpeg")));
        bgphoto.setBounds(0, 20, 760, 550);
        background.add(bgphoto);

        add(background);
        setSize(760, 570);
        setLocationRelativeTo(null);
    }

    private void menuBarMousePressed(java.awt.event.MouseEvent evt) {
        xMouse = evt.getX();
        yMouse = evt.getY();
    }

    private void menuBarMouseDragged(java.awt.event.MouseEvent evt) {
        int x = evt.getXOnScreen();
        int y = evt.getYOnScreen();
        this.setLocation(x - xMouse, y - yMouse);
    }

    private void exitBtnMouseClicked(java.awt.event.MouseEvent evt) {
        System.exit(0);
    }

    private void exitBtnMouseEntered(java.awt.event.MouseEvent evt) {
        exitBtn.setBackground(Color.red);
    }

    private void exitBtnMouseExited(java.awt.event.MouseEvent evt) {
        exitBtn.setBackground(Color.GRAY);
    }

    private void entrarlblMouseEntered(java.awt.event.MouseEvent evt) {
        exitBtn.setBackground(Color.blue);
    }

    private void entrarlblMouseExited(java.awt.event.MouseEvent evt) {
        exitBtn.setBackground(Color.GRAY);
    }

    private void userFieldMousePressed(java.awt.event.MouseEvent evt) {
        if (userField.getText().equals("Ingrese su nombre de usuario")) {
            userField.setText("");
            userField.setForeground(Color.black);
        }
        if (String.valueOf(jPasswordField1.getPassword()).isEmpty()) {
            jPasswordField1.setText("********");
            jPasswordField1.setForeground(Color.gray);
        }
    }

    private void jPasswordField1MousePressed(java.awt.event.MouseEvent evt) {
        if (userField.getText().isEmpty()) {
            userField.setText("Ingrese su nombre de usuario");
            userField.setForeground(Color.gray);
        }
        if (String.valueOf(jPasswordField1.getPassword()).equals("********")) {
            jPasswordField1.setText("");
            jPasswordField1.setForeground(Color.black);
        }
    }

    private void entrarlblMouseClicked(java.awt.event.MouseEvent evt) {
        String user = userField.getText().trim();
        String pass = String.valueOf(jPasswordField1.getPassword()).trim();

        if (user.isEmpty() || user.equals("Ingrese su nombre de usuario") || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Introduce usuario y contraseña.", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            UserDAO dao = new UserDAO();
            boolean ok = dao.login(user, pass);

            if (ok) {
                if (loginListener != null) loginListener.onLoginSuccess();
                dispose();
            } else {
                if (loginListener != null) loginListener.onLoginFailure("Usuario o contraseña incorrectos.");
            }
        } catch (Exception ex) {
            if (loginListener != null) loginListener.onLoginFailure("Error al conectar con la BD.");
        }
    }
    public static void main(String args[]) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            new LoginFrame(new LoginListener() {
                @Override
                public void onLoginSuccess() {
                    MainFrame main = new MainFrame();
                    main.setVisible(true);
                }
                @Override
                public void onLoginFailure(String reason) {
                    JOptionPane.showMessageDialog(null, reason, "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }).setVisible(true);
        });
    }

    // Variables declaration
    private javax.swing.JLabel UserLabel;
    private javax.swing.JPanel background;
    private javax.swing.JPanel bar;
    private javax.swing.JLabel bgphoto;
    private javax.swing.JPanel entrar;
    private javax.swing.JLabel entrarlbl;
    private javax.swing.JLabel exitBtn;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel logo;
    private javax.swing.JPanel menuBar;
    private javax.swing.JLabel passWordLabel;
    private javax.swing.JTextField userField;
    // End of variables declaration
}