package com.dynamicduo;

import javax.swing.SwingUtilities;

public class SecurityApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GUI().setVisible(true);
        });
    }
}
