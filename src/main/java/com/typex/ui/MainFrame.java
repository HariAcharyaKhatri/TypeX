package com.typex.ui;

import com.typex.config.ThemeManager;
import com.typex.dao.UserDAO;
import com.typex.model.User;
import com.typex.ui.components.Sidebar;
import com.typex.ui.components.TopBar;
import com.typex.ui.pages.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Sidebar sidebar;
    private final TopBar topBar;
    private User currentUser;

    public MainFrame() {
        super("TYPEX — Master Every Keystroke");
        try {
            setIconImages(com.typex.util.IconManager.getAppIconImages());
        } catch (Exception ignored) {}
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);

        UserDAO ud = new UserDAO();
        List<User> users = ud.findAll();
        if (users.isEmpty()) {
            currentUser = ud.create("Guest");
        } else {
            currentUser = users.get(0);
        }

        sidebar = new Sidebar(this::navigate);
        topBar = new TopBar(this, this::onThemeToggle, this::openProfile);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeManager.bgApp());
        root.add(sidebar, BorderLayout.WEST);
        root.add(topBar, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        buildPages();
        navigate("dashboard");
    }

    private void buildPages() {
        content.add(new DashboardPanel(this), "dashboard");
        content.add(new LearnTypingPanel(this), "learn");
        content.add(new TypingTestPanel(this), "test");
        content.add(new GamesPanel(this), "games");
        content.add(new CustomPracticePanel(this), "custom");
        content.add(new ChallengePanel(this), "challenge");
        content.add(new StatisticsPanel(this), "stats");
        content.add(new AchievementsPanel(this), "achievements");
        content.add(new KeyboardTrainerPanel(this), "keyboard");
        content.add(new SettingsPanel(this), "settings");
        content.add(new AboutPanel(this), "about");
    }

    public void navigate(String key) {
        setDistractionFree(false);
        for (Component c : content.getComponents()) {
            if (c instanceof com.typex.ui.pages.GamesPanel gp && !"games".equals(key)) {
                gp.cleanup();
            }
        }
        cards.show(content, key);
        sidebar.setActive(key);
        topBar.refresh();
        refreshAll();
    }

    public void setDistractionFree(boolean active) {
        sidebar.setVisible(!active);
        topBar.setVisible(!active);
        revalidate();
        repaint();
    }

    public boolean isDistractionFree() {
        return !sidebar.isVisible();
    }

    public void onThemeToggle() {
        ThemeManager.apply(!ThemeManager.isDark(), ThemeManager.accent());
        applyTheme();
    }

    public void applyTheme() {
        getContentPane().setBackground(ThemeManager.bgApp());
        sidebar.applyTheme();
        topBar.applyTheme();
        for (Component c : content.getComponents()) {
            if (c instanceof ThemeAware ta) {
                ta.applyTheme();
            }
        }
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    public void openProfile() {
        new ProfilePanel(this, currentUser).setVisible(true);
        topBar.refresh();
        refreshAll();
    }

    public User getUser() {
        return currentUser;
    }

    public void setUser(User u) {
        if (u != null) {
            this.currentUser = u;
            topBar.refresh();
            refreshAll();
        }
    }

    public void refreshAll() {
        for (Component c : content.getComponents()) {
            if (c instanceof Refreshable r) {
                r.refresh();
            }
        }
    }

    public interface Refreshable {
        void refresh();
    }

    public interface ThemeAware {
        void applyTheme();
    }
}
