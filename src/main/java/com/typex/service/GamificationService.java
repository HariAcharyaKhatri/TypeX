package com.typex.service;

import com.typex.dao.*;
import com.typex.model.*;

import java.util.ArrayList;
import java.util.List;

public class GamificationService {
    private final UserXPDAO userXpDao = new UserXPDAO();
    private final PracticeStreakDAO streakDao = new PracticeStreakDAO();
    private final DailyGoalDAO goalDao = new DailyGoalDAO();
    private final ChallengeDAO challengeDao = new ChallengeDAO();
    private final UserPreferenceDAO prefDao = new UserPreferenceDAO();
    private final AchievementService achievementService = new AchievementService();
    private final StatisticsDAO statsDao = new StatisticsDAO();
    private final LessonDAO lessonDao = new LessonDAO();
    private final LessonService lessonService = new LessonService();

    public static class SessionReward {
        public int xpGained;
        public boolean leveledUp;
        public int oldLevel;
        public int newLevel;
        public boolean goalCompleted;
        public List<UserChallenge> completedChallenges = new ArrayList<>();
        public List<Achievement> newlyUnlocked = new ArrayList<>();
        public int currentStreak;
        public int totalXp;

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("+").append(xpGained).append(" XP");
            if (leveledUp) {
                sb.append(" · LEVEL UP! (Level ").append(newLevel).append(")");
            }
            if (goalCompleted) {
                sb.append(" · Daily Goal Achieved!");
            }
            return sb.toString();
        }
    }

    public UserXP getUserXP(int userId) {
        return userXpDao.getForUser(userId);
    }

    public PracticeStreak getStreak(int userId) {
        return streakDao.getForUser(userId);
    }

    public DailyGoal getDailyGoal(int userId) {
        UserPreference pref = prefDao.getForUser(userId);
        return goalDao.getOrCreateToday(userId, pref);
    }

    public List<UserChallenge> getActiveChallenges(int userId) {
        return challengeDao.getCurrentChallengesForUser(userId);
    }

    public UserPreference getPreferences(int userId) {
        return prefDao.getForUser(userId);
    }

    public void savePreferences(UserPreference pref) {
        prefDao.save(pref);
    }

    public int claimChallengeReward(int userId, String challengeId) {
        int xp = challengeDao.claimReward(userId, challengeId);
        if (xp > 0) {
            userXpDao.addXP(userId, xp);
        }
        return xp;
    }

    public SessionReward recordTestCompletion(int userId, TypingResult r, boolean isNewPb) {
        SessionReward reward = new SessionReward();
        if (r == null) return reward;

        int earnedXp = 50; // Base XP for completing test
        if (r.accuracy >= 98.0) {
            earnedXp += 40;
        } else if (r.accuracy >= 95.0) {
            earnedXp += 20;
        }
        if (isNewPb) {
            earnedXp += 100; // Personal best bonus
        }

        // Streak: qualifying if duration >= 15s and total keystrokes >= 10
        if (r.durationSeconds >= 15 && r.totalKeystrokes >= 10) {
            PracticeStreakDAO.StreakUpdateResult streakRes = streakDao.recordQualifyingActivity(userId);
            reward.currentStreak = streakRes.currentStreak;
            if (streakRes.continued) {
                if (streakRes.currentStreak == 3) earnedXp += 50;
                else if (streakRes.currentStreak == 7) earnedXp += 100;
                else if (streakRes.currentStreak == 14) earnedXp += 200;
                else if (streakRes.currentStreak == 30) earnedXp += 300;
            }
        } else {
            reward.currentStreak = streakDao.getForUser(userId).currentStreak;
        }

        // Daily Goal
        boolean newlyAchieved = goalDao.recordTest(userId, r.durationSeconds, r.accuracy);
        if (newlyAchieved) {
            // Claim goal reward once
            DailyGoal dg = goalDao.getOrCreateToday(userId, prefDao.getForUser(userId));
            if (goalDao.claimReward(userId, dg.date)) {
                earnedXp += 150;
                reward.goalCompleted = true;
            }
        }

        // Challenge events
        reward.completedChallenges.addAll(challengeDao.recordEvent(userId, "TESTS_COUNT", 1.0, true));
        reward.completedChallenges.addAll(challengeDao.recordEvent(userId, "PRACTICE_MINUTES", r.durationSeconds / 60.0, true));
        reward.completedChallenges.addAll(challengeDao.recordEvent(userId, "MIN_WPM", r.netWpm, false));
        reward.completedChallenges.addAll(challengeDao.recordEvent(userId, "MIN_ACCURACY", r.accuracy, false));
        if (r.accuracy >= 100.0 && r.durationSeconds >= 15) {
            reward.completedChallenges.addAll(challengeDao.recordEvent(userId, "PERFECT_TEST", 1.0, false));
        }

        for (UserChallenge uc : reward.completedChallenges) {
            int chXp = challengeDao.claimReward(userId, uc.challengeId);
            if (chXp > 0) {
                earnedXp += chXp;
            }
        }

        // Achievements
        long totalSecs = statsDao.totalPracticeSeconds(userId);
        int totalTests = statsDao.recent(userId, 1000).stream().mapToInt(d -> d.testsCompleted).sum();
        reward.newlyUnlocked.addAll(achievementService.evaluateAfterTest(userId, r, totalTests, totalSecs));
        reward.newlyUnlocked.addAll(achievementService.evaluateStreak(userId, reward.currentStreak));
        for (Achievement a : reward.newlyUnlocked) {
            earnedXp += a.xp;
        }

        // Award total XP and check Level up
        UserXPDAO.XPResult xpRes = userXpDao.addXP(userId, earnedXp);
        reward.xpGained = earnedXp;
        reward.leveledUp = xpRes.leveledUp;
        reward.oldLevel = xpRes.oldLevel;
        reward.newLevel = xpRes.newLevel;
        reward.totalXp = xpRes.newXp;

        return reward;
    }

    public SessionReward recordLessonCompletion(int userId, Lesson lesson, double wpm, double acc, int elapsedSec, boolean passed) {
        SessionReward reward = new SessionReward();
        int earnedXp = passed ? 75 : 25; // 75 XP for pass, 25 XP for attempt

        if (passed) {
            PracticeStreakDAO.StreakUpdateResult streakRes = streakDao.recordQualifyingActivity(userId);
            reward.currentStreak = streakRes.currentStreak;
            if (streakRes.continued) {
                if (streakRes.currentStreak == 3) earnedXp += 50;
                else if (streakRes.currentStreak == 7) earnedXp += 100;
                else if (streakRes.currentStreak == 14) earnedXp += 200;
                else if (streakRes.currentStreak == 30) earnedXp += 300;
            }
        } else {
            reward.currentStreak = streakDao.getForUser(userId).currentStreak;
        }

        // Daily Goal
        boolean newlyAchieved = goalDao.recordLesson(userId, elapsedSec, passed);
        if (newlyAchieved) {
            DailyGoal dg = goalDao.getOrCreateToday(userId, prefDao.getForUser(userId));
            if (goalDao.claimReward(userId, dg.date)) {
                earnedXp += 150;
                reward.goalCompleted = true;
            }
        }

        // Challenge events
        if (passed) {
            reward.completedChallenges.addAll(challengeDao.recordEvent(userId, "LESSONS_COUNT", 1.0, true));
        }
        reward.completedChallenges.addAll(challengeDao.recordEvent(userId, "PRACTICE_MINUTES", elapsedSec / 60.0, true));
        for (UserChallenge uc : reward.completedChallenges) {
            int chXp = challengeDao.claimReward(userId, uc.challengeId);
            if (chXp > 0) {
                earnedXp += chXp;
            }
        }

        // Achievements
        int completedLessons = 0;
        boolean beginnerDone = true;
        boolean proDone = true;
        List<Lesson> all = lessonDao.findAll();
        for (Lesson l : all) {
            boolean done = lessonService.isCompleted(userId, l.id);
            if (done) completedLessons++;
            if (l.level == 1 && !done) beginnerDone = false;
            if (l.level == 5 && !done) proDone = false;
        }

        reward.newlyUnlocked.addAll(achievementService.evaluateAfterLesson(userId, completedLessons, beginnerDone, proDone));
        reward.newlyUnlocked.addAll(achievementService.evaluateStreak(userId, reward.currentStreak));
        for (Achievement a : reward.newlyUnlocked) {
            earnedXp += a.xp;
        }

        // Award total XP and check Level up
        UserXPDAO.XPResult xpRes = userXpDao.addXP(userId, earnedXp);
        reward.xpGained = earnedXp;
        reward.leveledUp = xpRes.leveledUp;
        reward.oldLevel = xpRes.oldLevel;
        reward.newLevel = xpRes.newLevel;
        reward.totalXp = xpRes.newXp;

        return reward;
    }
}
