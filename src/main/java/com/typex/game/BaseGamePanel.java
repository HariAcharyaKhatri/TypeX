package com.typex.game;

import com.typex.config.ThemeManager;
import com.typex.dao.GameDAO;
import com.typex.model.GamePersonalBest;
import com.typex.model.GameSession;
import com.typex.model.GameType;
import com.typex.service.GamificationService;
import com.typex.ui.MainFrame;
import com.typex.util.IconManager;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Reusable abstract foundation for all TYPEX arcade typing games.
 * Provides a standardized 60fps Swing game loop, HUD header, pause overlay,
 * game over overlay, XP awards, and score persistence.
 */
public abstract class BaseGamePanel extends JPanel implements MainFrame.ThemeAware {

    public enum State { READY, PLAYING, PAUSED, GAME_OVER }

    protected final MainFrame frame;
    protected final GameType gameType;
    protected final Runnable onExitToCatalog;

    protected final GameDAO gameDao = new GameDAO();
    protected final GamificationService gamification = new GamificationService();

    protected State gameState = State.READY;
    protected Timer gameLoopTimer;
    protected long sessionStartNanos;
    protected long pauseStartNanos = 0;
    protected long accumulatedPausedNanos = 0;
    protected int elapsedSeconds = 0;

    // Core Metrics
    protected int score = 0;
    protected int level = 1;
    protected int wordsCompleted = 0;
    protected int totalKeystrokes = 0;
    protected int correctKeystrokes = 0;
    protected int errors = 0;

    // HUD labels
    private final JLabel titleLabel = new JLabel();
    private final JLabel scoreLabel = new JLabel("0");
    private final JLabel metric1Label = new JLabel("-");
    private final JLabel metric2Label = new JLabel("-");
    private final JLabel speedLabel = new JLabel("0 WPM");
    private final JButton pauseBtn = new JButton("Pause");
    private final JButton exitBtn = new JButton("Exit");

    // HUD Container
    private final JPanel hudPanel = new JPanel(new BorderLayout(12, 4));

    // Overlays
    private JPanel pauseOverlay;
    private JPanel gameOverOverlay;
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel gameCanvasContainer = new JPanel(new BorderLayout());

    public BaseGamePanel(MainFrame frame, GameType gameType, Runnable onExitToCatalog) {
        this.frame = frame;
        this.gameType = gameType;
        this.onExitToCatalog = onExitToCatalog;

        setLayout(new BorderLayout());
        setBackground(ThemeManager.bgApp());

        buildHUD();

        layeredPane.setLayout(null);
        gameCanvasContainer.setOpaque(false);
        gameCanvasContainer.add(getGameComponent(), BorderLayout.CENTER);

        layeredPane.add(gameCanvasContainer, JLayeredPane.DEFAULT_LAYER);

        add(hudPanel, BorderLayout.NORTH);
        add(layeredPane, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayoutLayers();
            }
        });

        // 60 FPS Game Loop
        gameLoopTimer = new Timer(16, e -> {
            if (gameState == State.PLAYING) {
                onGameTick();
                updateHUD();
                getGameComponent().repaint();
            }
        });

        // Key Listeners
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (gameState == State.PLAYING) {
                        pauseGame();
                    } else if (gameState == State.PAUSED) {
                        resumeGame();
                    }
                    return;
                }

                if (gameState != State.PLAYING) return;

                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    handleBackspace();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    handleSpace();
                    return;
                }

                char c = e.getKeyChar();
                if (c != KeyEvent.CHAR_UNDEFINED && c >= 32) {
                    handleTypedChar(c);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        applyTheme();
    }

    private void buildHUD() {
        hudPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        hudPanel.setBackground(ThemeManager.bgCard());

        // Left: Game title & diff
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        titleLabel.setText(gameType.displayName);
        titleLabel.setFont(ThemeManager.getHeadingFont(18f));
        titleLabel.setForeground(ThemeManager.fgPrimary());
        left.add(titleLabel);

        JLabel diffBadge = new JLabel(gameType.difficulty);
        diffBadge.setFont(ThemeManager.getMonoFont(11f));
        diffBadge.setForeground(ThemeManager.accent());
        diffBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.accent(), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        left.add(diffBadge);
        hudPanel.add(left, BorderLayout.WEST);

        // Center: Score & Metrics
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        center.setOpaque(false);

        JPanel scoreBox = createStatPill("SCORE", scoreLabel);
        JPanel m1Box = createStatPill(getMetric1Name(), metric1Label);
        JPanel m2Box = createStatPill(getMetric2Name(), metric2Label);
        JPanel speedBox = createStatPill("SPEED", speedLabel);

        center.add(scoreBox);
        center.add(m1Box);
        center.add(m2Box);
        center.add(speedBox);
        hudPanel.add(center, BorderLayout.CENTER);

        // Right: Pause & Exit buttons
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        pauseBtn.setFont(ThemeManager.getBodyFont(12.5f));
        pauseBtn.setFocusPainted(false);
        pauseBtn.setIcon(IconManager.getIcon("pause", 14, ThemeManager.fgPrimary()));
        pauseBtn.addActionListener(e -> {
            if (gameState == State.PLAYING) pauseGame();
            else if (gameState == State.PAUSED) resumeGame();
        });

        exitBtn.setFont(ThemeManager.getBodyFont(12.5f));
        exitBtn.setFocusPainted(false);
        exitBtn.setIcon(IconManager.getIcon("exit", 14, ThemeManager.fgPrimary()));
        exitBtn.addActionListener(e -> confirmExit());

        right.add(pauseBtn);
        right.add(exitBtn);
        hudPanel.add(right, BorderLayout.EAST);
    }

    private JPanel createStatPill(String title, JLabel valueLabel) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 0));
        p.setOpaque(false);
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(ThemeManager.getMonoFont(10f));
        titleLbl.setForeground(ThemeManager.fgMuted());
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setFont(ThemeManager.getMonoFont(16f));
        valueLabel.setForeground(ThemeManager.fgPrimary());
        p.add(titleLbl);
        p.add(valueLabel);
        return p;
    }

    private void relayoutLayers() {
        int w = layeredPane.getWidth();
        int h = layeredPane.getHeight();
        if (w <= 0 || h <= 0) return;

        gameCanvasContainer.setBounds(0, 0, w, h);
        if (pauseOverlay != null) {
            pauseOverlay.setBounds(0, 0, w, h);
        }
        if (gameOverOverlay != null) {
            gameOverOverlay.setBounds(0, 0, w, h);
        }
    }

    public void startGame() {
        gameState = State.PLAYING;
        score = 0;
        level = 1;
        wordsCompleted = 0;
        totalKeystrokes = 0;
        correctKeystrokes = 0;
        errors = 0;
        elapsedSeconds = 0;
        sessionStartNanos = System.nanoTime();
        pauseStartNanos = 0;
        accumulatedPausedNanos = 0;

        hideOverlays();
        onGameStart();
        updateHUD();
        gameLoopTimer.start();
        requestFocusInWindow();
    }

    public void pauseGame() {
        if (gameState != State.PLAYING) return;
        gameState = State.PAUSED;
        pauseStartNanos = System.nanoTime();
        gameLoopTimer.stop();
        showPauseOverlay();
    }

    public void resumeGame() {
        if (gameState != State.PAUSED) return;
        if (pauseStartNanos > 0) {
            accumulatedPausedNanos += (System.nanoTime() - pauseStartNanos);
            pauseStartNanos = 0;
        }
        gameState = State.PLAYING;
        hideOverlays();
        gameLoopTimer.start();
        requestFocusInWindow();
    }

    public void restartGame() {
        hideOverlays();
        startGame();
    }

    public long getActiveElapsedNanos() {
        if (gameState == State.READY) return 0;
        long current = (gameState == State.PAUSED && pauseStartNanos > 0) ? pauseStartNanos : System.nanoTime();
        long totalElapsed = current - sessionStartNanos;
        long active = totalElapsed - accumulatedPausedNanos;
        return Math.max(0, active);
    }

    public void cleanup() {
        if (gameLoopTimer != null && gameLoopTimer.isRunning()) {
            gameLoopTimer.stop();
        }
        if (gameState == State.PLAYING) {
            if (pauseStartNanos == 0) {
                pauseStartNanos = System.nanoTime();
            }
            gameState = State.PAUSED;
        }
    }

    public void endGame() {
        if (gameState == State.GAME_OVER) return;
        if (gameState == State.PAUSED && pauseStartNanos > 0) {
            accumulatedPausedNanos += (System.nanoTime() - pauseStartNanos);
            pauseStartNanos = 0;
        }
        gameState = State.GAME_OVER;
        gameLoopTimer.stop();
        elapsedSeconds = (int) Math.max(1, getActiveElapsedNanos() / 1_000_000_000L);

        double acc = totalKeystrokes > 0 ? (correctKeystrokes * 100.0 / totalKeystrokes) : 100.0;
        double wpm = (correctKeystrokes / 5.0) / (elapsedSeconds / 60.0);
        if (wpm < 0 || Double.isNaN(wpm)) wpm = 0.0;

        int uid = frame.getUser() != null ? frame.getUser().id : 1;
        GameSession session = new GameSession(uid, gameType.id, score, level, wordsCompleted, acc, wpm, elapsedSeconds);

        // Pre-check personal best so XP includes accurate PB bonus
        boolean isNewPb = gameDao.isPersonalBest(uid, gameType.id, score, wpm, acc);

        // Gamification XP (populates session.xpEarned)
        GamificationService.SessionReward reward = gamification.recordGameSession(uid, session, isNewPb);

        // Record in SQLite atomically with accurate session.xpEarned and independent PBs
        boolean persistedPb = gameDao.recordSession(session);

        showGameOverOverlay(session, isNewPb || persistedPb, reward);
    }

    private void confirmExit() {
        if (gameState == State.PLAYING) {
            pauseGame();
        }
        int res = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit to the games library?",
                "Exit Game", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            cleanup();
            if (onExitToCatalog != null) {
                onExitToCatalog.run();
            }
        } else if (gameState == State.PAUSED) {
            resumeGame();
        }
    }

    private void showPauseOverlay() {
        if (pauseOverlay == null) {
            pauseOverlay = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0, 0, 0, 160));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            pauseOverlay.setOpaque(false);

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(ThemeManager.bgCard());
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                    BorderFactory.createEmptyBorder(28, 36, 28, 36)));

            JLabel pTitle = new JLabel("GAME PAUSED");
            pTitle.setFont(ThemeManager.getHeadingFont(24f));
            pTitle.setForeground(ThemeManager.fgPrimary());
            pTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton resumeBtn = new JButton("Resume");
            resumeBtn.setFont(ThemeManager.getBodyFont(14f));
            resumeBtn.setBackground(ThemeManager.accent());
            resumeBtn.setForeground(ThemeManager.fgOnAccent());
            resumeBtn.setFocusPainted(false);
            resumeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            resumeBtn.setMaximumSize(new Dimension(200, 38));
            resumeBtn.addActionListener(e -> resumeGame());

            JButton restartBtn = new JButton("Restart");
            restartBtn.setFont(ThemeManager.getBodyFont(14f));
            restartBtn.setFocusPainted(false);
            restartBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            restartBtn.setMaximumSize(new Dimension(200, 38));
            restartBtn.addActionListener(e -> restartGame());

            JButton exitB = new JButton("Exit to Library");
            exitB.setFont(ThemeManager.getBodyFont(14f));
            exitB.setFocusPainted(false);
            exitB.setAlignmentX(Component.CENTER_ALIGNMENT);
            exitB.setMaximumSize(new Dimension(200, 38));
            exitB.addActionListener(e -> {
                cleanup();
                if (onExitToCatalog != null) onExitToCatalog.run();
            });

            card.add(pTitle);
            card.add(Box.createVerticalStrut(20));
            card.add(resumeBtn);
            card.add(Box.createVerticalStrut(10));
            card.add(restartBtn);
            card.add(Box.createVerticalStrut(10));
            card.add(exitB);

            pauseOverlay.add(card);
            layeredPane.add(pauseOverlay, JLayeredPane.MODAL_LAYER);
        }

        pauseOverlay.setVisible(true);
        relayoutLayers();
    }

    private void showGameOverOverlay(GameSession session, boolean isNewPb, GamificationService.SessionReward reward) {
        if (gameOverOverlay != null) {
            layeredPane.remove(gameOverOverlay);
        }

        gameOverOverlay = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        gameOverOverlay.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ThemeManager.bgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isNewPb ? ThemeManager.colorWarning() : ThemeManager.borderColor(), 2),
                BorderFactory.createEmptyBorder(28, 40, 28, 40)));

        JLabel titleLbl = new JLabel(isNewPb ? "★ NEW PERSONAL BEST! ★" : "GAME OVER");
        titleLbl.setFont(ThemeManager.getHeadingFont(24f));
        titleLbl.setForeground(isNewPb ? ThemeManager.colorWarning() : ThemeManager.fgPrimary());
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreVal = new JLabel(String.format("%,d", session.score));
        scoreVal.setFont(ThemeManager.getMonoFont(38f));
        scoreVal.setForeground(ThemeManager.accent());
        scoreVal.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreSub = new JLabel("FINAL SCORE");
        scoreSub.setFont(ThemeManager.getMonoFont(11f));
        scoreSub.setForeground(ThemeManager.fgMuted());
        scoreSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Stats summary row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
        statsRow.add(createStatPill("LEVEL", new JLabel(String.valueOf(session.levelReached))));
        statsRow.add(createStatPill("WORDS", new JLabel(String.valueOf(session.wordsCompleted))));
        statsRow.add(createStatPill("ACCURACY", new JLabel(String.format("%.0f%%", session.accuracy))));
        statsRow.add(createStatPill("WPM", new JLabel(String.format("%.0f", session.wpm))));

        // XP Banner
        JLabel xpLbl = new JLabel(String.format("+%d XP EARNED", session.xpEarned));
        xpLbl.setFont(ThemeManager.getMonoFont(14f));
        xpLbl.setForeground(ThemeManager.colorSuccess());
        xpLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setOpaque(false);

        JButton againBtn = new JButton("Play Again");
        againBtn.setFont(ThemeManager.getBodyFont(14f));
        againBtn.setBackground(ThemeManager.accent());
        againBtn.setForeground(ThemeManager.fgOnAccent());
        againBtn.setFocusPainted(false);
        againBtn.setIcon(IconManager.getIcon("restart", 16, ThemeManager.fgOnAccent()));
        againBtn.addActionListener(e -> restartGame());

        JButton exitB = new JButton("Exit to Library");
        exitB.setFont(ThemeManager.getBodyFont(14f));
        exitB.setFocusPainted(false);
        exitB.setIcon(IconManager.getIcon("exit", 16, ThemeManager.fgPrimary()));
        exitB.addActionListener(e -> {
            cleanup();
            if (onExitToCatalog != null) onExitToCatalog.run();
        });

        btnRow.add(againBtn);
        btnRow.add(exitB);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(12));
        card.add(scoreVal);
        card.add(scoreSub);
        card.add(Box.createVerticalStrut(10));
        card.add(statsRow);
        card.add(Box.createVerticalStrut(8));
        card.add(xpLbl);
        card.add(Box.createVerticalStrut(20));
        card.add(btnRow);

        gameOverOverlay.add(card);
        layeredPane.add(gameOverOverlay, JLayeredPane.MODAL_LAYER);
        gameOverOverlay.setVisible(true);
        relayoutLayers();
    }

    private void hideOverlays() {
        if (pauseOverlay != null) pauseOverlay.setVisible(false);
        if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
    }

    protected void updateHUD() {
        scoreLabel.setText(String.format("%,d", score));
        metric1Label.setText(getMetric1Value());
        metric2Label.setText(getMetric2Value());

        double activeSeconds = getActiveElapsedNanos() / 1_000_000_000.0;
        double elapsedMins = Math.max(1.0, activeSeconds) / 60.0;
        double wpm = (correctKeystrokes / 5.0) / elapsedMins;
        speedLabel.setText(String.format("%.0f WPM", Math.max(0, wpm)));
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        hudPanel.setBackground(ThemeManager.bgCard());
        hudPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        titleLabel.setForeground(ThemeManager.fgPrimary());
        scoreLabel.setForeground(ThemeManager.accent());
        metric1Label.setForeground(ThemeManager.fgPrimary());
        metric2Label.setForeground(ThemeManager.fgPrimary());
        speedLabel.setForeground(ThemeManager.fgPrimary());

        pauseBtn.setBackground(ThemeManager.bgApp());
        pauseBtn.setForeground(ThemeManager.fgPrimary());
        pauseBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        exitBtn.setBackground(ThemeManager.bgApp());
        exitBtn.setForeground(ThemeManager.fgPrimary());
        exitBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        getGameComponent().repaint();
    }

    // Abstract contract to be implemented by each game
    protected abstract JComponent getGameComponent();
    protected abstract String getMetric1Name();
    protected abstract String getMetric1Value();
    protected abstract String getMetric2Name();
    protected abstract String getMetric2Value();

    protected abstract void onGameStart();
    protected abstract void onGameTick();
    protected abstract void handleTypedChar(char c);
    protected abstract void handleBackspace();
    protected abstract void handleSpace();
}
