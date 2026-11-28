package com.typex.util;

import com.typex.model.TypingResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Manages performance analytics data export in CSV, JSON, and Printable Text formats.
 */
public final class ExportManager {
    private ExportManager() {}

    public static void exportCsv(File file, List<TypingResult> results) throws IOException {
        if (file == null || results == null) return;
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("Date,Category,DurationSeconds,GrossWPM,NetWPM,Accuracy,RawAccuracy,Consistency,CorrectChars,IncorrectChars,CorrectedErrors,UncorrectedErrors,TotalKeystrokes,Backspaces\n");
            for (TypingResult r : results) {
                if (r == null) continue;
                w.write(String.format("%s,%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%d,%d%n",
                        escapeCsv(r.completedAt), escapeCsv(r.category), r.durationSeconds,
                        r.grossWpm, r.netWpm, r.accuracy, r.rawAccuracy, r.consistency,
                        r.correctChars, r.incorrectChars, r.correctedErrors, r.uncorrectedErrors,
                        r.totalKeystrokes, r.backspaces));
            }
        }
    }

    public static void exportJson(File file, List<TypingResult> results) throws IOException {
        if (file == null || results == null) return;
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("[\n");
            for (int i = 0; i < results.size(); i++) {
                TypingResult r = results.get(i);
                if (r == null) continue;
                w.write(String.format("  {\n" +
                        "    \"id\": %d,\n" +
                        "    \"userId\": %d,\n" +
                        "    \"completedAt\": \"%s\",\n" +
                        "    \"category\": \"%s\",\n" +
                        "    \"durationSeconds\": %d,\n" +
                        "    \"grossWpm\": %.2f,\n" +
                        "    \"netWpm\": %.2f,\n" +
                        "    \"accuracy\": %.2f,\n" +
                        "    \"rawAccuracy\": %.2f,\n" +
                        "    \"consistency\": %.2f,\n" +
                        "    \"correctChars\": %d,\n" +
                        "    \"incorrectChars\": %d,\n" +
                        "    \"correctedErrors\": %d,\n" +
                        "    \"uncorrectedErrors\": %d,\n" +
                        "    \"totalKeystrokes\": %d,\n" +
                        "    \"backspaces\": %d\n" +
                        "  }%s%n",
                        r.id, r.userId,
                        escapeJson(r.completedAt), escapeJson(r.category),
                        r.durationSeconds, r.grossWpm, r.netWpm, r.accuracy,
                        r.rawAccuracy, r.consistency,
                        r.correctChars, r.incorrectChars, r.correctedErrors, r.uncorrectedErrors,
                        r.totalKeystrokes, r.backspaces,
                        (i < results.size() - 1 ? "," : "")));
            }
            w.write("]\n");
        }
    }

    public static void exportPrintableReport(File file, int userId, String username,
                                             List<TypingResult> results) throws IOException {
        if (file == null || results == null) return;
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("=========================================================================\n");
            w.write("                     TYPEX — TYPING PERFORMANCE REPORT                   \n");
            w.write("=========================================================================\n");
            w.write("User: " + (username != null ? username : "Guest") + " (id=" + userId + ")\n");
            w.write("Generated: " + java.time.LocalDateTime.now() + "\n");
            w.write("Total Sessions Analyzed: " + results.size() + "\n\n");
            w.write(String.format("%-20s %-16s %8s %8s %7s %8s %11s%n",
                    "Date", "Category", "GrossWPM", "NetWPM", "Acc%", "RawAcc%", "Consistency"));
            w.write("-------------------------------------------------------------------------\n");
            for (TypingResult r : results) {
                if (r == null) continue;
                w.write(String.format("%-20s %-16s %8.1f %8.1f %6.1f%% %7.1f%% %10.1f%%%n",
                        safe(r.completedAt), safe(r.category), r.grossWpm, r.netWpm, r.accuracy,
                        r.rawAccuracy, r.consistency));
            }
            w.write("=========================================================================\n");
        }
    }

    private static String safe(String s) {
        return s == null ? "-" : s;
    }

    private static String escapeCsv(String s) {
        if (s == null) return "\"-\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
