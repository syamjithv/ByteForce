package com.byteforce.app;

import javax.swing.SwingUtilities;

public final class ByteForceApplication {

    private ByteForceApplication() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ByteForceFrame().setVisible(true));
    }
}
