package com.tastedivekafka;

import com.tastedivekafka.session.AppSession;
import com.tastedivekafka.ui.LoginFrame;
import com.tastedivekafka.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Clase principal de la aplicación frontend. 
 * Se encarga de mostrar la interfaz gráfica
 *
 * Responsabilidades:
 * - Mostrar la ventana de login al iniciar la app
 * - Gestionar el flujo de autenticación (login/logout)
 *
 * El frontend no se comunica directamente con Kafka. 
 * Toda la comunicación con el backend se realiza 
 * a través de BackendClient (HTTP en el puerto 8090).
 */
public class FrontendApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> showLogin());
    }

    /**
     * Shows the login window. Called on startup and again after logout.
     */
    public static void showLogin() {
        LoginFrame loginFrame = new LoginFrame(new LoginFrame.LoginListener() {
            @Override
            public void onLoginSuccess(String username) {
                AppSession.login(username); // Store session in frontend
                MainFrame main = new MainFrame();
                main.setVisible(true);
            }

            @Override
            public void onLoginFailure(String reason) {
                JOptionPane.showMessageDialog(null, reason,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        loginFrame.setVisible(true);
    }
}
