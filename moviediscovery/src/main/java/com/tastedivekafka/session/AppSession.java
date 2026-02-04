package com.tastedivekafka.session;

public final class AppSession {

    private static String currentUser;

    private AppSession() {
        // Evita instancias
    }

    public static void login(String username) {
        currentUser = username;
        System.out.println("Sesión iniciada para: " + username);
    }

    public static void logout() {
        System.out.println("Sesión cerrada para: " + currentUser);
        currentUser = null;
    }

    public static boolean isLogged() {
        return currentUser != null;
    }

    public static String getCurrentUser() {
        return currentUser;
    }
}
