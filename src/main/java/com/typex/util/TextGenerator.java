package com.typex.util;

import java.util.*;

/**
 * Text generation system for typing tests.
 * Generates varied, authentic practice text across 8 categories without repeating the same sample.
 */
public final class TextGenerator {
    private static final Random RNG = new Random();

    private static final String[] VOCABULARY = {
        "ability", "absence", "academy", "account", "achieve", "acquire", "address", "advance", "advice", "affect",
        "agency", "agenda", "airline", "airport", "alcohol", "alleged", "already", "analyst", "ancient", "another",
        "anxiety", "anybody", "applied", "arrange", "arrival", "article", "assault", "assume", "athlete", "attempt",
        "attract", "auction", "average", "balance", "banking", "barrier", "battery", "bearing", "beating", "because",
        "bedroom", "believe", "benefit", "between", "billion", "binding", "biology", "blanket", "bleeding", "blessed",
        "booking", "boredom", "bracket", "breathe", "briefly", "brother", "browser", "builder", "cabinet", "calorie",
        "campaign", "capacity", "capital", "capture", "careful", "carrier", "caution", "ceiling", "central", "century",
        "certain", "chamber", "channel", "chapter", "charity", "charter", "chronic", "circuit", "citizen", "classic",
        "climate", "closure", "clothes", "cluster", "coastal", "collect", "college", "combine", "comfort", "command",
        "comment", "compact", "company", "compare", "compete", "complex", "compose", "concept", "concern", "concert",
        "conduct", "confirm", "connect", "consent", "consist", "contact", "contain", "content", "contest", "context",
        "control", "convert", "correct", "council", "counsel", "counter", "country", "courage", "coverage", "creator",
        "crucial", "crystal", "culture", "current", "custody", "customs", "cutting", "damaged", "density", "deposit",
        "derived", "deserve", "desktop", "despite", "destroy", "develop", "devoted", "diamond", "digital", "dignity",
        "diminish", "diploma", "disable", "dispute", "diverse", "divided", "divorce", "doctrine", "document", "domestic"
    };

    private static final String[] COMMON_PHRASES = {
        "as a matter of fact", "at the end of the day", "by the same token", "for the most part",
        "in the long run", "on the other hand", "it goes without saying", "to make matters worse",
        "time and time again", "from time to time", "down to earth", "keep in mind that",
        "take into consideration", "point of view", "make a difference", "bring to the table"
    };

    private static final String[] SENTENCES = {
        "The quick brown fox jumps gracefully over the sleeping hound.",
        "Pack my box with five dozen jugs of liquid liquid quartz.",
        "Sphinx of black quartz, hear my vow and judge my quiet devotion.",
        "Clear thinking requires courage rather than intelligence and perseverance.",
        "Curiosity is the engine of achievement and deliberate progress.",
        "A smooth sea never made a skilled sailor capable of navigating storms.",
        "Simplicity is prerequisite for reliability in software design and life.",
        "The mind once stretched by a new idea never returns to its original dimensions.",
        "Action is the foundational key to all remarkable accomplishments.",
        "Precision beats speed, but precision combined with speed creates excellence.",
        "Great works are performed not by sudden strength, but by patient perseverance.",
        "The best way to predict the future is to invent it with your own hands.",
        "Knowledge becomes capability only when transformed through repetitive practice.",
        "Quiet confidence speaks louder than boisterous boasts in every room.",
        "The journey of a thousand miles begins with a single decisive step forward."
    };

    private static final String[] PARAGRAPHS = {
        "Typing is a cognitive and physical synthesis that develops with targeted feedback. When striking keys, our nervous system constructs motor memory circuits that gradually bypass conscious deliberation. In the initial phases, every movement requires visual confirmation, but sustained focus slowly trains the fingers to map directly to spatial targets on the board.",
        "Software engineering relies on clear communication as much as algorithmic complexity. Clean architecture guarantees that systems remain resilient under change and scalable across distributed infrastructure. Writing thoughtful, maintainable code is an act of empathy toward every developer who will maintain that code in future seasons.",
        "The exploration of cosmic space reflects humanity's relentless quest to transcend boundaries. From early stargazers tracking orbital mechanics across nighttime skies to interplanetary probes exploring ancient Martian valleys, discovery expands our understanding of our fragile terrestrial home.",
        "Scientific inquiry begins with keen observation followed by rigorous experimentation. Hypotheses must endure aggressive testing and peer scrutiny before crystallizing into accepted theories. This iterative discipline separates genuine insight from temporary illusion."
    };

    private static final String[] QUOTES = {
        "\"The only way to do great work is to love what you do.\" — Steve Jobs",
        "\"It does not matter how slowly you go as long as you do not stop.\" — Confucius",
        "\"Stay hungry, stay foolish. Never settle for superficial achievements.\" — Whole Earth Catalog",
        "\"Simplicity is the ultimate sophistication in engineering and art.\" — Leonardo da Vinci",
        "\"In the middle of difficulty lies opportunity waiting to be seized.\" — Albert Einstein",
        "\"Perfection is achieved not when there is nothing more to add, but nothing left to take away.\" — Antoine de Saint-Exupery",
        "\"Programs must be written for people to read, and only incidentally for machines to execute.\" — Harold Abelson"
    };

    private static final String[] BUSINESS = {
        "Dear executive team, please find attached the comprehensive quarterly performance analysis. Our revenue grew eighteen percent quarter over quarter, driven by strong enterprise platform adoption.",
        "We are scheduled to conduct our strategic planning offsite next Thursday. Please prepare feedback on budget allocation, hiring projections, and our prospective third-quarter milestones.",
        "Pursuant to section four of the service agreement, all deliverables must undergo quality assurance inspection before final deployment into the primary production environment.",
        "Our customer support satisfaction metric reached ninety-six percent this billing period. Streamlined ticket routing and automated workflow responses significantly lowered resolution times."
    };

    private static final String[] PROGRAMMING = {
        "public static <T extends Comparable<T>> void quickSort(T[] array, int low, int high) {",
        "if (low < high) { int pivotIndex = partition(array, low, high); quickSort(array, low, pivotIndex - 1); }",
        "SELECT u.username, COUNT(r.id) AS total_tests FROM users u LEFT JOIN typing_test_results r ON u.id = r.user_id GROUP BY u.id;",
        "const debounce = (func, wait) => { let timeout; return (...args) => { clearTimeout(timeout); timeout = setTimeout(() => func(...args), wait); }; };",
        "List<String> validTokens = rawInput.stream().filter(s -> !s.isBlank()).map(String::trim).toList();"
    };

    private static final String[] NUMBERS_AND_SYMBOLS = {
        "Invoice #9482-B costs $1,289.50 (with 7.5% sales tax and $35.00 shipping).",
        "Coordinates: 37°46'29.7\"N, 122°25'10.2\"W. Elevation: +142.8m [accuracy ±0.5m].",
        "Call +1 (800) 555-0199 ext #42. Password syntax requires: [A-Z], [0-9], & @!#$%.",
        "Equation: f(x) = (3x^2 + 4x - 7) / (2x + 1); evaluate for x = 12.5 & x = -3.2.",
        "Server status: 192.168.1.104:8080 returned HTTP 200 OK with latency 14.8ms."
    };

    private TextGenerator() {}

    public static String generate(String category, int minLength) {
        StringBuilder sb = new StringBuilder();
        int targetLen = Math.max(250, minLength);

        switch (category != null ? category : "Random Words") {
            case "Random Words" -> {
                List<String> list = new ArrayList<>(Arrays.asList(VOCABULARY));
                while (sb.length() < targetLen) {
                    Collections.shuffle(list, RNG);
                    for (String w : list) {
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(w);
                        if (sb.length() >= targetLen) break;
                    }
                }
            }
            case "Common English" -> {
                List<String> list = new ArrayList<>(Arrays.asList(COMMON_PHRASES));
                while (sb.length() < targetLen) {
                    Collections.shuffle(list, RNG);
                    for (String p : list) {
                        if (sb.length() > 0) sb.append(". ");
                        sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
                        if (sb.length() >= targetLen) break;
                    }
                    sb.append('.');
                }
            }
            case "Sentences" -> {
                List<String> list = new ArrayList<>(Arrays.asList(SENTENCES));
                while (sb.length() < targetLen) {
                    Collections.shuffle(list, RNG);
                    for (String s : list) {
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(s);
                        if (sb.length() >= targetLen) break;
                    }
                }
            }
            case "Paragraphs" -> {
                List<String> list = new ArrayList<>(Arrays.asList(PARAGRAPHS));
                while (sb.length() < targetLen) {
                    Collections.shuffle(list, RNG);
                    for (String p : list) {
                        if (sb.length() > 0) sb.append("\n\n");
                        sb.append(p);
                        if (sb.length() >= targetLen) break;
                    }
                }
            }
            case "Quotes" -> {
                List<String> list = new ArrayList<>(Arrays.asList(QUOTES));
                while (sb.length() < targetLen) {
                    Collections.shuffle(list, RNG);
                    for (String q : list) {
                        if (sb.length() > 0) sb.append("  ");
                        sb.append(q);
                        if (sb.length() >= targetLen) break;
                    }
                }
            }
            case "Business English" -> {
                List<String> list = new ArrayList<>(Arrays.asList(BUSINESS));
                while (sb.length() < targetLen) {
                    Collections.shuffle(list, RNG);
                    for (String b : list) {
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(b);
                        if (sb.length() >= targetLen) break;
                    }
                }
            }
            case "Programming" -> {
                List<String> list = new ArrayList<>(Arrays.asList(PROGRAMMING));
                while (sb.length() < targetLen) {
                    Collections.shuffle(list, RNG);
                    for (String code : list) {
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(code);
                        if (sb.length() >= targetLen) break;
                    }
                }
            }
            case "Numbers and Symbols" -> {
                List<String> list = new ArrayList<>(Arrays.asList(NUMBERS_AND_SYMBOLS));
                while (sb.length() < targetLen) {
                    Collections.shuffle(list, RNG);
                    for (String ns : list) {
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(ns);
                        if (sb.length() >= targetLen) break;
                    }
                }
            }
            default -> {
                return generate("Random Words", targetLen);
            }
        }
        return sb.toString().trim();
    }
}
