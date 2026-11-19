package com.typex;

import com.typex.config.ThemeManager;
import com.typex.database.DatabaseInitializer;
import com.typex.database.DatabaseManager;
import com.typex.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        ThemeManager.applyInitial();

        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseManager.getInstance().initialize();
                DatabaseInitializer.initialize();
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> DatabaseManager.getInstance().close()));
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "TYPEX failed to start:\n" + ex.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
