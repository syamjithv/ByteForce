package com.byteforce.app;

import com.byteforce.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * ByteForce desktop application entry point.
 * Bootstraps AppConfig and AppContext, instantiates ByteForceFrame on the EDT,
 * and ensures graceful release of database and connection pool resources.
 */
public final class ByteForceApplication {

    private static final Logger log = LoggerFactory.getLogger(ByteForceApplication.class);

    private ByteForceApplication() {
    }

    public static void main(String[] args) {
        log.info("Starting ByteForce application...");
        AppContext appContext = null;
        try {
            // 1. Load existing AppConfig
            AppConfig config = AppConfig.load();
            log.info("Loaded configuration for application '{}' (Version: {})", config.getAppName(), config.getAppVersion());

            // 2. Initialize AppContext composition root
            appContext = AppContext.initialize(config);

            final AppContext ctx = appContext;

            // 5. Register shutdown hook for JVM termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("JVM shutdown hook triggered. Closing AppContext...");
                ctx.close();
            }, "byteforce-shutdown"));

            // 3 & 4. Create ByteForceFrame on EDT and attach close listener
            SwingUtilities.invokeLater(() -> {
                try {
                    ByteForceFrame frame = new ByteForceFrame(ctx);
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            log.info("Window closing event received. Closing AppContext resources...");
                            ctx.close();
                        }
                    });
                    frame.setVisible(true);
                    log.info("ByteForce UI window initialized.");
                } catch (Exception e) {
                    log.error("Failed to construct or display ByteForceFrame", e);
                    ctx.close();
                }
            });
        } catch (Exception e) {
            log.error("Failed to bootstrap ByteForce application", e);
            if (appContext != null) {
                appContext.close();
            }
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        null,
                        "Failed to initialize ByteForce database connection.\n\n"
                                + "Details: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                                + "\n\nPlease ensure your database server is running and configuration is correct.",
                        "ByteForce Startup Error",
                        JOptionPane.ERROR_MESSAGE
                );
            });
        }
    }
}
