package cz.vse.avatask;

import cz.vse.avatask.model.Pouzivatel;

public final class CurrentUserSession {
    private static Pouzivatel currentUser;

    private CurrentUserSession() {
    }

    public static void setCurrentUser(Pouzivatel pouzivatel) {
        currentUser = pouzivatel;
    }

    public static Pouzivatel getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
    private int mena; // Změň název podle potřeby

}
