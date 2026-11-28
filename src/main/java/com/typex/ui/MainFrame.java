package com.typex.ui;

import com.typex.config.ThemeManager;
import com.typex.dao.UserDAO;
import com.typex.model.User;
import com.typex.ui.components.Sidebar;
import com.typex.ui.components.TopBar;
import com.typex.ui.pages.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Component> pageMap = new LinkedHashMap<>();
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
        setupGlobalShortcuts();
        navigate("dashboard");
    }

    private void registerPage(String key, Component comp) {
        pageMap.put(key, comp);
        content.add(comp, key);
    }

    private void buildPages() {
        registerPage("dashboard", new DashboardPanel(this));
        registerPage("learn", new LearnTypingPanel(this));
        registerPage("test", new TypingTestPanel(this));
        registerPage("games", new GamesPanel(this));
        registerPage("custom", new CustomPracticePanel(this));
        registerPage("challenge", new ChallengePanel(this));
        registerPage("stats", new StatisticsPanel(this));
        registerPage("achievements", new AchievementsPanel(this));
        registerPage("keyboard", new KeyboardTrainerPanel(this));
        registerPage("settings", new SettingsPanel(this));
        registerPage("about", new AboutPanel(this));
    }

    private void setupGlobalShortcuts() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();
        int menuKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Ctrl+T: Theme Toggle
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, menuKey), "toggleTheme");
        am.put("toggleTheme", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { onThemeToggle(); }
        });

        // Ctrl+P: Profile
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, menuKey), "openProfile");
        am.put("openProfile", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { openProfile(); }
        });

        // Ctrl+M: Mute / Sound Toggle
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, menuKey), "toggleSound");
        am.put("toggleSound", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                com.typex.util.SoundManager.toggleEnabled();
                topBar.refresh();
            }
        });

        // F11: Distraction Free Mode Toggle
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleDistractionFree");
        am.put("toggleDistractionFree", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                setDistractionFree(!isDistractionFree());
            }
        });
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
        Component targetPage = pageMap.get(key);
        if (targetPage instanceof Refreshable r) {
            r.refresh();
        }
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
