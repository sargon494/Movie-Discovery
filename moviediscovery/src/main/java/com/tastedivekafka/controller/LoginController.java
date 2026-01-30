package com.tastedivekafka.controller;

import com.tastedivekafka.db.UserDAO;
import com.tastedivekafka.model.UserModel;
import com.tastedivekafka.ui.LoginFrame;

public class LoginController {

    private LoginFrame view;

    public LoginController(LoginFrame view) {
        this.view = view;
    }

    public void login(String user, char[] passwordChars) {

        if (user.isEmpty() || passwordChars.length == 0) {
            view.showError("Introduce usuario y contraseña.");
            return;
        }

        String password = new String(passwordChars);

        try {
            UserModel model = new UserModel(user, password);
            UserDAO dao = new UserDAO();

            if (dao.login(model.getUsername(), model.getPassword())) {
                view.loginSuccess();
            } else {
                view.showError("Usuario o contraseña incorrectos.");
            }

        } catch (Exception e) {
            view.showError("Error al conectar con la base de datos.");
        }
    }
}
