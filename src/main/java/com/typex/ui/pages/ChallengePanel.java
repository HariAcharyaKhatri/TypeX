package com.typex.ui.pages;

import com.typex.config.ThemeManager;
import com.typex.model.Challenge;
import com.typex.model.UserChallenge;
import com.typex.service.GamificationService;
import com.typex.ui.MainFrame;
import com.typex.util.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChallengePanel extends JPanel implements MainFrame.Refreshable, MainFrame.ThemeAware {
    private final MainFrame frame;
    private final GamificationService gamification = new GamificationService();
    private final JPanel contentPanel = new JPanel();
    private final JScrollPane scrollPane;
    private List<UserChallenge> activeChallenges = new ArrayList<>();

    public ChallengePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JLabel footerNote = new JLabel("<html><i>Challenges rotate deterministically on your local calendar date. Practice offline to claim XP!</i></html>");
        footerNote.setForeground(ThemeManager.fgMuted());
        add(footerNote, BorderLayout.SOUTH);

        applyTheme();
        refresh();
    }

    @Override
    public void applyTheme() {
        setBackground(ThemeManager.bgApp());
        renderChallenges();
    }

    @Override
    public void refresh() {
        if (frame.getUser() == null) return;
        final int uid = frame.getUser().id;

        SwingWorker<List<UserChallenge>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<UserChallenge> doInBackground() {
                return gamification.getActiveChallenges(uid);
            }

            @Override
            protected void done() {
                try {
                    activeChallenges = get();
                    renderChallenges();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void renderChallenges() {
        contentPanel.removeAll();

        // Daily Section
        JLabel dailyHeader = new JLabel("🎯 Today's Daily Missions");
        dailyHeader.setFont(dailyHeader.getFont().deriveFont(Font.BOLD, 18f));
        dailyHeader.setForeground(ThemeManager.accent());
        dailyHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(dailyHeader);
        contentPanel.add(Box.createVerticalStrut(10));

        JPanel dailyGrid = new JPanel(new GridLayout(0, 3, 14, 14));
        dailyGrid.setOpaque(false);
        dailyGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Weekly Section
        JPanel weeklyGrid = new JPanel(new GridLayout(0, 2, 14, 14));
        weeklyGrid.setOpaque(false);
        weeklyGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (UserChallenge uc : activeChallenges) {
            Challenge ch = uc.challenge;
            if (ch == null) continue;
            JPanel card = createChallengeCard(uc);
            if ("DAILY".equalsIgnoreCase(ch.type)) {
                dailyGrid.add(card);
            } else {
                weeklyGrid.add(card);
            }
        }

        contentPanel.add(dailyGrid);
        contentPanel.add(Box.createVerticalStrut(24));

        JLabel weeklyHeader = new JLabel("⚡ Weekly Challenges");
        weeklyHeader.setFont(weeklyHeader.getFont().deriveFont(Font.BOLD, 18f));
        weeklyHeader.setForeground(ThemeManager.accent());
        weeklyHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(weeklyHeader);
        contentPanel.add(Box.createVerticalStrut(10));

        contentPanel.add(weeklyGrid);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel createChallengeCard(UserChallenge uc) {
        Challenge ch = uc.challenge;
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(ThemeManager.bgCard());
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(uc.completed ? (ThemeManager.isDark() ? new Color(0x10B981) : new Color(0x059669))
                        : (ThemeManager.isDark() ? new Color(0x333333) : new Color(0xE5E7EB)), uc.completed ? 2 : 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        // Header: Type badge & XP
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JLabel typeBadge = new JLabel(ch.type);
        typeBadge.setFont(typeBadge.getFont().deriveFont(Font.BOLD, 10f));
        typeBadge.setForeground("DAILY".equalsIgnoreCase(ch.type) ? new Color(0x38BDF8) : new Color(0xA855F7));
        cardHeader.add(typeBadge, BorderLayout.WEST);

        JLabel xpBadge = new JLabel("+" + ch.xpReward + " XP");
        xpBadge.setFont(xpBadge.getFont().deriveFont(Font.BOLD, 12f));
        xpBadge.setForeground(ThemeManager.accent());
        cardHeader.add(xpBadge, BorderLayout.EAST);

        // Center: Title & Description
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(ch.title);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 15f));
        titleLbl.setForeground(ThemeManager.fgPrimary());
        centerPanel.add(titleLbl);
        centerPanel.add(Box.createVerticalStrut(4));

        JLabel descLbl = new JLabel("<html>" + ch.description + "</html>");
        descLbl.setFont(descLbl.getFont().deriveFont(Font.PLAIN, 12f));
        descLbl.setForeground(ThemeManager.fgMuted());
        centerPanel.add(descLbl);
        centerPanel.add(Box.createVerticalStrut(8));

        // Progress bar
        int progressInt = (int) Math.round(uc.getProgressPercentage());
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setValue(progressInt);
        pb.setStringPainted(true);
        pb.setForeground(ThemeManager.accent());
        pb.setBackground(ThemeManager.isDark() ? new Color(0x222222) : new Color(0xE2E8F0));

        if (uc.completed) {
            pb.setString("100% Completed");
        } else {
            String progText = String.format("%.0f / %.0f", uc.currentProgress, ch.targetValue);
            pb.setString(progText + " (" + progressInt + "%)");
        }
        centerPanel.add(pb);

        // Bottom action button
        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.setOpaque(false);

        if (uc.completed && !uc.rewardClaimed) {
            JButton claimBtn = new JButton("Claim Reward (+" + ch.xpReward + " XP)");
            claimBtn.setBackground(new Color(0x10B981));
            claimBtn.setForeground(Color.WHITE);
            claimBtn.setFocusPainted(false);
            claimBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            claimBtn.addActionListener(e -> {
                gamification.claimChallengeReward(frame.getUser().id, ch.id);
                SoundManager.achievement();
                frame.refreshAll();
                refresh();
            });
            actionPanel.add(claimBtn, BorderLayout.CENTER);
        } else if (uc.completed) {
            JLabel doneLbl = new JLabel("✓ Completed & Claimed", SwingConstants.CENTER);
            doneLbl.setFont(doneLbl.getFont().deriveFont(Font.BOLD, 12f));
            doneLbl.setForeground(new Color(0x10B981));
            actionPanel.add(doneLbl, BorderLayout.CENTER);
        } else {
            JButton actionBtn = new JButton(getActionText(ch.requirementType));
            actionBtn.setBackground(ThemeManager.bgCard());
            actionBtn.setForeground(ThemeManager.fgPrimary());
            actionBtn.setFocusPainted(false);
            actionBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            actionBtn.addActionListener(e -> handleNavigation(ch.requirementType));
            actionPanel.add(actionBtn, BorderLayout.CENTER);
        }

        p.add(cardHeader, BorderLayout.NORTH);
        p.add(centerPanel, BorderLayout.CENTER);
        p.add(actionPanel, BorderLayout.SOUTH);

        return p;
    }

    private String getActionText(String reqType) {
        if ("LESSONS_COUNT".equalsIgnoreCase(reqType)) return "Go to Lessons";
        return "Start Typing Test";
    }

    private void handleNavigation(String reqType) {
        if ("LESSONS_COUNT".equalsIgnoreCase(reqType)) {
            frame.navigate("learn");
        } else {
            frame.navigate("test");
        }
    }
}
