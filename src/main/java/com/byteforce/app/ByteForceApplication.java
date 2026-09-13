package com.byteforce.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

public final class ByteForceApplication {

    private static final Logger log = LoggerFactory.getLogger(ByteForceApplication.class);

    private ByteForceApplication() {
    }

    public static void main(String[] args) {
        log.info("Starting ByteForce application...");
        SwingUtilities.invokeLater(() -> {
            ByteForceFrame frame = new ByteForceFrame();
            frame.setVisible(true);
            log.info("ByteForce UI window initialized.");
        });
    }
}
