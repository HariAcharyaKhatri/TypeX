package com.typex.util;

import com.typex.model.TypingResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ExportManager {
    private ExportManager() {}

    public static void exportCsv(File file, List<TypingResult> results) throws IOException {
        if (file == null || results == null) return;
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("Date,Category,Duration,GrossWPM,NetWPM,Accuracy,Correct,Incorrect,Backspaces\n");
            for (TypingResult r : results) {
                if (r == null) continue;
                w.write(String.format("%s,%s,%d,%.2f,%.2f,%.2f,%d,%d,%d%n",
                        safe(r.completedAt), safe(r.category), r.durationSeconds,
                        r.grossWpm, r.netWpm, r.accuracy,
                        r.correctChars, r.incorrectChars, r.backspaces));
            }
        }
    }

    public static void exportPrintableReport(File file, int userId, String username,
                                             List<TypingResult> results) throws IOException {
        if (file == null || results == null) return;
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write("TYPEX — Typing Performance Report\n");
            w.write("User: " + (username != null ? username : "Guest") + " (id=" + userId + ")\n");
            w.write("Generated: " + java.time.LocalDateTime.now() + "\n\n");
            w.write(String.format("%-22s %-16s %8s %8s %8s%n",
                    "Date", "Category", "GrossWPM", "NetWPM", "Acc%"));
            for (TypingResult r : results) {
                if (r == null) continue;
                w.write(String.format("%-22s %-16s %8.2f %8.2f %8.2f%n",
                        safe(r.completedAt), safe(r.category), r.grossWpm, r.netWpm, r.accuracy));
            }
        }
    }

    private static String safe(String s) {
        return s == null ? "-" : s.replace(",", ";");
    }
}
