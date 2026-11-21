package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.dao.GameDAO;
import com.typex.game.*;
import com.typex.model.GamePersonalBest;
import com.typex.model.GameType;
import com.typex.ui.MainFrame;
import com.typex.ui.components.StatCard;
import com.typex.util.IconManager;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Games Hub: Presents the catalog of 8 interactive typing games
 * and hosts the active gameplay session.
 */
public class GamesPanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {

    private final MainFrame frame;
    private final GameDAO gameDao = new GameDAO();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    // Catalog view
    private final JPanel catalogPanel = new JPanel(new BorderLayout(14, 14));
    private final JPanel gamesGrid = new JPanel(new GridLayout(0, 4, 16, 16));
    private final StatCard totalPlayedCard;
    private final StatCard totalScoreCard;
    private final StatCard favoriteGameCard;

    // Active Game Container
    private final JPanel gameContainer = new JPanel(new BorderLayout());
    private BaseGamePanel currentGamePanel = null;

    public GamesPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.bgApp());

        // Header Stats
        totalPlayedCard = new StatCard("GAMES PLAYED", "0", "Total arcade sessions", IconManager.getIcon("rocket", 18, ThemeManager.accent()));
        totalScoreCard = new StatCard("ARCADE SCORE", "0", "Cumulative high scores", IconManager.getIcon("trophy", 18, ThemeManager.colorWarning()));
        favoriteGameCard = new StatCard("FAVORITE MODE", "Word Rush", "Most practiced mode", IconManager.getIcon("star", 18, ThemeManager.accent()));

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.add(totalPlayedCard);
        statsRow.add(totalScoreCard);
        statsRow.add(favoriteGameCard);

        // Catalog Center Scroll
        gamesGrid.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(gamesGrid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        catalogPanel.setBorder(BorderFactory.createEmptyBorder(18, 24, 20, 24));
        catalogPanel.setOpaque(false);
        catalogPanel.add(statsRow, BorderLayout.NORTH);
        catalogPanel.add(scrollPane, BorderLayout.CENTER);

        // Game Container
        gameContainer.setOpaque(false);

        cardsPanel.setOpaque(false);
        cardsPanel.add(catalogPanel, "catalog");
        cardsPanel.add(gameContainer, "game");

        add(cardsPanel, BorderLayout.CENTER);

        applyTheme();
        refresh();
    }

    private void launchGame(GameType type) {
        if (currentGamePanel != null) {
            gameContainer.remove(currentGamePanel);
        }

        Runnable onExit = () -> {
            cardLayout.show(cardsPanel, "catalog");
            refresh();
        };

        switch (type) {
            case WORD_RUSH -> currentGamePanel = new WordRushGame(frame, onExit);
            case FALLING_WORDS -> currentGamePanel = new FallingWordsGame(frame, onExit);
            case TYPING_SURVIVAL -> currentGamePanel = new TypingSurvivalGame(frame, onExit);
            case LETTER_INVADERS -> currentGamePanel = new LetterInvadersGame(frame, onExit);
            case SENTENCE_SPRINT -> currentGamePanel = new SentenceSprintGame(frame, onExit);
            case KEY_MASTER -> currentGamePanel = new KeyMasterGame(frame, onExit);
            case CODE_RUNNER -> currentGamePanel = new CodeRunnerGame(frame, onExit);
            case NUMBER_NINJA -> currentGamePanel = new NumberNinjaGame(frame, onExit);
            default -> currentGamePanel = new WordRushGame(frame, onExit);
        }

        gameContainer.add(currentGamePanel, BorderLayout.CENTER);
        gameContainer.revalidate();
        gameContainer.repaint();

        cardLayout.show(cardsPanel, "game");
        currentGamePanel.startGame();
    }

    private JPanel createGameCard(GameType type, GamePersonalBest pb) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(ThemeManager.bgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.borderColor(), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        // Header: Icon + Difficulty badge
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel iconLbl = new JLabel(IconManager.getIcon(type.iconKey, 28, ThemeManager.accent()));
        header.add(iconLbl, BorderLayout.WEST);

        JLabel diffLbl = new JLabel(type.difficulty);
        diffLbl.setFont(ThemeManager.getMonoFont(10.5f));
        diffLbl.setForeground(ThemeManager.accent());
        diffLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.accent(), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        header.add(diffLbl, BorderLayout.EAST);

        // Center: Title + Tagline + High Score
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel titleLbl = new JLabel(type.displayName);
        titleLbl.setFont(ThemeManager.getHeadingFont(16f));
        titleLbl.setForeground(ThemeManager.fgPrimary());

        JLabel tagLbl = new JLabel(type.tagline);
        tagLbl.setFont(ThemeManager.getBodyFont(12f));
        tagLbl.setForeground(ThemeManager.fgMuted());

        JLabel descLbl = new JLabel("<html><div style='width:180px;'>" + type.description + "</div></html>");
        descLbl.setFont(ThemeManager.getBodyFont(11.5f));
        descLbl.setForeground(ThemeManager.fgSecondary());

        int hiScore = pb != null ? pb.highScore : 0;
        JLabel pbLbl = new JLabel(String.format("HIGH SCORE: %,d", hiScore));
        pbLbl.setFont(ThemeManager.getMonoFont(12f));
        pbLbl.setForeground(hiScore > 0 ? ThemeManager.colorWarning() : ThemeManager.fgMuted());

        center.add(titleLbl);
        center.add(Box.createVerticalStrut(3));
        center.add(tagLbl);
        center.add(Box.createVerticalStrut(8));
        center.add(descLbl);
        center.add(Box.createVerticalStrut(12));
        center.add(pbLbl);

        // Play Button
        JButton playBtn = new JButton("Play Now");
        playBtn.setFont(ThemeManager.getBodyFont(13f));
        playBtn.setBackground(ThemeManager.accent());
        playBtn.setForeground(ThemeManager.fgOnAccent());
        playBtn.setFocusPainted(false);
        playBtn.setIcon(IconManager.getIcon("play", 14, ThemeManager.fgOnAccent()));
        playBtn.addActionListener(e -> launchGame(type));

        card.add(header, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(playBtn, BorderLayout.SOUTH);

        return card;
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        totalPlayedCard.applyTheme();
        totalScoreCard.applyTheme();
        favoriteGameCard.applyTheme();
        if (currentGamePanel != null) {
            currentGamePanel.applyTheme();
        }
        refresh();
    }

    @Override
    public void refresh() {
        if (frame.getUser() == null) return;
        final int uid = frame.getUser().id;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int totalGames = 0;
            int totalScore = 0;
            Map<String, GamePersonalBest> pbs;

            @Override
            protected Void doInBackground() {
                totalGames = gameDao.getTotalGamesPlayed(uid);
                totalScore = gameDao.getTotalScore(uid);
                pbs = gameDao.getAllPersonalBests(uid);
                return null;
            }

            @Override
            protected void done() {
                totalPlayedCard.setValue(String.format("%,d", totalGames));
                totalScoreCard.setValue(String.format("%,d", totalScore));

                gamesGrid.removeAll();
                for (GameType gt : GameType.values()) {
                    GamePersonalBest pb = pbs.get(gt.id.toLowerCase());
                    gamesGrid.add(createGameCard(gt, pb));
                }
                gamesGrid.revalidate();
                gamesGrid.repaint();
            }
        };
        worker.execute();
    }
}
