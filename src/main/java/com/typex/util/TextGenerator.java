package com.typex.util;

import java.util.*;

/**
 * Text generation engine for TYPEX.
 * Features 12 rich offline categories, 4 difficulty levels, and an anti-repetition
 * tracking buffer to ensure engaging, diverse typing sessions.
 */
public final class TextGenerator {

    public static final String[] CATEGORIES = {
        "Random Words",
        "Common English",
        "Sentences",
        "Paragraphs",
        "Quotes",
        "Business English",
        "Programming",
        "Numbers and Symbols",
        "Science and Technology",
        "Geography and General Knowledge",
        "Literature and Vocabulary",
        "Academic English"
    };

    public static final String[] DIFFICULTIES = {
        "Easy", "Normal", "Hard", "Expert"
    };

    private static final Random RNG = new Random();

    // Anti-repetition ring buffer per category (last 5 served passage indexes)
    private static final Map<String, Deque<Integer>> RECENT_INDEXES = new HashMap<>();

    // 1. Vocabulary bank
    private static final String[] VOCAB_EASY = {
        "time", "year", "people", "way", "day", "man", "thing", "woman", "life", "child",
        "world", "school", "state", "family", "student", "group", "country", "problem", "hand", "part",
        "place", "case", "week", "company", "system", "program", "question", "work", "government", "number",
        "night", "point", "home", "water", "room", "mother", "area", "money", "story", "fact",
        "month", "lot", "right", "study", "book", "eye", "job", "word", "business", "issue",
        "side", "kind", "head", "house", "service", "friend", "father", "power", "hour", "game"
    };

    private static final String[] VOCAB_HARD = {
        "quintessential", "ubiquitous", "anachronistic", "juxtaposition", "perspicacity",
        "ephemeral", "idiosyncrasy", "magnanimous", "recalcitrant", "obfuscation",
        "serendipity", "surreptitious", "verisimilitude", "vicissitude", "ineffable",
        "labyrinthine", "mellifluous", "nostalgia", "panacea", "quandary",
        "resilience", "scintillating", "transient", "unfathomable", "vulnerable",
        "whimsical", "xenophobia", "yesteryear", "zealous", "cacophony"
    };

    // 2. Common English phrases & idioms
    private static final String[] COMMON_PHRASES = {
        "as a matter of fact", "at the end of the day", "by the same token", "for the most part",
        "in the long run", "on the other hand", "it goes without saying", "to make matters worse",
        "time and time again", "from time to time", "down to earth", "keep in mind that",
        "take into consideration", "point of view", "make a difference", "bring to the table",
        "under the impression", "a blessing in disguise", "call it a day", "better late than never",
        "bite the bullet", "break the ice", "burn the midnight oil", "cross that bridge when you get there",
        "every cloud has a silver lining", "hit the nail on the head", "let the cat out of the bag",
        "on cloud nine", "piece of cake", "see eye to eye", "spill the beans", "through thick and thin"
    };

    // 3. Sentences
    private static final String[] SENTENCES = {
        "The quick brown fox jumps gracefully over the sleeping hound.",
        "Pack my box with five dozen jugs of liquid quartz.",
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
        "The journey of a thousand miles begins with a single decisive step forward.",
        "Discipline is choosing between what you want now and what you want most.",
        "To know what you know and that you do not know what you do not know, that is true knowledge.",
        "Success is the sum of small efforts, repeated day in and day out without pause."
    };

    // 4. Paragraphs
    private static final String[] PARAGRAPHS = {
        "Typing is a cognitive and physical synthesis that develops with targeted feedback. When striking keys, our nervous system constructs motor memory circuits that gradually bypass conscious deliberation. In the initial phases, every movement requires visual confirmation, but sustained focus slowly trains the fingers to map directly to spatial targets on the board.",
        "Software engineering relies on clear communication as much as algorithmic complexity. Clean architecture guarantees that systems remain resilient under change and scalable across distributed infrastructure. Writing thoughtful, maintainable code is an act of empathy toward every developer who will maintain that code in future seasons.",
        "The exploration of cosmic space reflects humanity's relentless quest to transcend boundaries. From early stargazers tracking orbital mechanics across nighttime skies to interplanetary probes exploring ancient Martian valleys, discovery expands our understanding of our fragile terrestrial home.",
        "Scientific inquiry begins with keen observation followed by rigorous experimentation. Hypotheses must endure aggressive testing and peer scrutiny before crystallizing into accepted theories. This iterative discipline separates genuine insight from temporary illusion.",
        "The history of typography mirrors the evolution of human literacy and mass media. From Gutenberg's moveable metal type in Renaissance Mainz to scalable digital outlines rendered on high-density liquid crystal screens, letterforms carry cultural heritage across centuries.",
        "Architectural harmony emerges when form honors function while inspiring human emotion. Modern cities balance towering glass facades with tranquil urban sanctuaries, reminding us that built spaces shape human community, productivity, and shared purpose."
    };

    // 5. Quotes
    private static final String[] QUOTES = {
        "\"The only way to do great work is to love what you do.\" — Steve Jobs",
        "\"It does not matter how slowly you go as long as you do not stop.\" — Confucius",
        "\"Stay hungry, stay foolish. Never settle for superficial achievements.\" — Whole Earth Catalog",
        "\"Simplicity is the ultimate sophistication in engineering and art.\" — Leonardo da Vinci",
        "\"In the middle of difficulty lies opportunity waiting to be seized.\" — Albert Einstein",
        "\"Perfection is achieved not when there is nothing more to add, but nothing left to take away.\" — Antoine de Saint-Exupéry",
        "\"Programs must be written for people to read, and only incidentally for machines to execute.\" — Harold Abelson",
        "\"The greatest glory in living lies not in never falling, but in rising every time we fall.\" — Nelson Mandela",
        "\"Life is what happens when you're busy making other plans.\" — John Lennon",
        "\"Spread love everywhere you go. Let no one ever come to you without leaving happier.\" — Mother Teresa"
    };

    // 6. Business English
    private static final String[] BUSINESS = {
        "Dear executive team, please find attached the comprehensive quarterly performance analysis. Our revenue grew eighteen percent quarter over quarter, driven by strong enterprise platform adoption.",
        "We are scheduled to conduct our strategic planning offsite next Thursday. Please prepare feedback on budget allocation, hiring projections, and our prospective third-quarter milestones.",
        "Pursuant to section four of the service agreement, all deliverables must undergo quality assurance inspection before final deployment into the primary production environment.",
        "Our customer support satisfaction metric reached ninety-six percent this billing period. Streamlined ticket routing and automated workflow responses significantly lowered resolution times.",
        "The merger integration committee convened to align corporate governance protocols, harmonize human resource policies, and migrate distributed enterprise software systems.",
        "Market volatility demands prudent capital allocation and rigorous cash-flow forecasting. Proactive hedging strategies have mitigated downside exposure across international currencies."
    };

    // 7. Programming
    private static final String[] PROGRAMMING = {
        "public static <T extends Comparable<T>> void quickSort(T[] array, int low, int high) {\n    if (low < high) {\n        int pivot = partition(array, low, high);\n        quickSort(array, low, pivot - 1);\n        quickSort(array, pivot + 1, high);\n    }\n}",
        "SELECT u.username, COUNT(r.id) AS total_tests, AVG(r.net_wpm) AS avg_speed\nFROM users u\nLEFT JOIN typing_test_results r ON u.id = r.user_id\nGROUP BY u.id\nHAVING COUNT(r.id) >= 5\nORDER BY avg_speed DESC;",
        "const debounce = (func, delay) => {\n    let timerId;\n    return (...args) => {\n        clearTimeout(timerId);\n        timerId = setTimeout(() => func(...args), delay);\n    };\n};",
        "List<String> validTokens = rawInput.stream()\n    .filter(s -> !s.isBlank())\n    .map(String::trim)\n    .distinct()\n    .sorted()\n    .toList();",
        "def compute_fibonacci(n: int) -> list[int]:\n    sequence = [0, 1]\n    while len(sequence) < n:\n        sequence.append(sequence[-1] + sequence[-2])\n    return sequence[:n]",
        "class EventDispatcher {\n    private final Map<String, List<Consumer<Event>>> listeners = new ConcurrentHashMap<>();\n    public void subscribe(String topic, Consumer<Event> handler) {\n        listeners.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(handler);\n    }\n}"
    };

    // 8. Numbers and Symbols
    private static final String[] NUMBERS_AND_SYMBOLS = {
        "Invoice #9482-B costs $1,289.50 (with 7.5% sales tax and $35.00 shipping).",
        "Coordinates: 37°46'29.7\"N, 122°25'10.2\"W. Elevation: +142.8m [accuracy ±0.5m].",
        "Call +1 (800) 555-0199 ext #42. Password syntax requires: [A-Z], [0-9], & @!#$%.",
        "Equation: f(x) = (3x^2 + 4x - 7) / (2x + 1); evaluate for x = 12.5 & x = -3.2.",
        "Server status: 192.168.1.104:8080 returned HTTP 200 OK with latency 14.8ms.",
        "Financial ratio: ROI = [(Gross Gain $450,000 - Investment $320,000) / $320,000] * 100 = +40.625%."
    };

    // 9. Science and Technology
    private static final String[] SCIENCE_AND_TECH = {
        "Quantum computing leverages the principles of superposition and entanglement to execute complex calculations exponentially faster than classical Turing architectures for specific problem domains.",
        "Photosynthesis converts solar photons, ambient carbon dioxide, and groundwater into glucose and atmospheric oxygen via photosystems I and II embedded in thylakoid membranes.",
        "CRISPR-Cas9 operates as molecular scissors capable of introducing precise double-strand breaks at targeted genomic loci guided by synthetic single-guide RNA sequences.",
        "Plate tectonics describes the dynamic motion of lithospheric plates over the ductile asthenosphere, driving continental drift, oceanic trench subduction, and seismic volcanism.",
        "James Webb Space Telescope observes infrared wavelengths through an unfolding beryllium primary mirror coated in gold, peering into the universe's earliest cosmological epochs.",
        "Neural networks simulate dendritic connectivity through weighted parameter matrices, backpropagating loss gradients to iteratively adjust connection strengths during stochastic gradient descent."
    };

    // 10. Geography and General Knowledge
    private static final String[] GEOGRAPHY = {
        "The Amazon River basin spans nine South American nations and supplies approximately twenty percent of total oceanic river discharge across our terrestrial biosphere.",
        "Mount Everest towers at 8,848.86 meters above sea level within the Mahalangur Himal sub-range of the Himalayas, marking the highest topographical point on Earth.",
        "The Mariana Trench in the western Pacific Ocean descends to Challenger Deep, an oceanic abyss reaching roughly 10,994 meters below the sea surface under crushing hydrostatic pressure.",
        "Madagascar severed from the Indian subcontinent roughly eighty-eight million years ago, cultivating an isolated evolutionary sanctuary where ninety percent of wildlife is endemic.",
        "The Mediterranean Sea connects to the Atlantic Ocean through the narrow Strait of Gibraltar, separating the Iberian Peninsula of Europe from the Atlas Mountains of Morocco.",
        "Tokyo metropolitan area is the most populous megalopolis on the globe, home to over thirty-seven million residents interconnected by an intricate rail and transit network."
    };

    // 11. Literature and Vocabulary
    private static final String[] LITERATURE = {
        "It was the best of times, it was the worst of times, it was the age of wisdom, it was the age of foolishness, it was the epoch of belief, it was the epoch of incredulity.",
        "Call me Ishmael. Some years ago—never mind how long precisely—having little or no money in my purse, and nothing particular to interest me on shore, I thought I would sail about a little.",
        "All happy families are alike; each unhappy family is unhappy in its own way. Everything was in confusion in the Oblonskys' house.",
        "The autumn wind whistled through the barren orchard, carrying crisp amber leaves across ancient flagstone paths while shadows lengthened against the weathered stone facade.",
        "A solitary lantern illuminated the cobblestone alley, casting elongated silhouettes that danced in synchronization with the gentle patter of late November rain.",
        "Her poignant prose resonated with melancholy elegance, weaving intricate metaphors of transient beauty and the inexorable passage of unremembered time."
    };

    // 12. Academic English
    private static final String[] ACADEMIC = {
        "Empirical evidence gathered across longitudinal observational studies indicates a statistically significant correlation between early literacy interventions and adolescent scholastic attainment.",
        "Methodological rigor necessitates the implementation of randomized double-blind control trials to isolate confounding demographic variables and eliminate confirmation bias.",
        "The ontological framework posited by contemporary cognitive philosophers challenges Cartesian dualism by emphasizing embodied cognition and situated ecological perception.",
        "Socioeconomic stratification exacerbates educational disparities unless mitigated by progressive fiscal redistributive policies and targeted municipal infrastructure investments.",
        "The synthesis of qualitative ethnographic observations with quantitative demographic modeling provides a robust, multidimensional paradigm for demographic research.",
        "Thermodynamic entropy dictates that closed thermodynamic systems irrevocably transition toward states of maximum disorder and uniform energy dissipation."
    };

    private TextGenerator() {}

    /**
     * Backward-compatible generator defaulting to Normal difficulty.
     */
    public static String generate(String category, int minLength) {
        return generate(category, "Normal", minLength);
    }

    /**
     * Generates typing passage text with category, difficulty, minimum length,
     * and anti-repetition protection.
     */
    public static synchronized String generate(String category, String difficulty, int minLength) {
        String cat = (category != null && !category.isBlank()) ? category.trim() : "Common English";
        String diff = (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : "Normal";
        int targetLen = Math.max(250, minLength);

        StringBuilder sb = new StringBuilder();

        switch (cat) {
            case "Random Words" -> generateRandomWords(sb, diff, targetLen);
            case "Common English" -> generateFromPool(sb, COMMON_PHRASES, "Common English", targetLen, ". ");
            case "Sentences" -> generateFromPool(sb, SENTENCES, "Sentences", targetLen, " ");
            case "Paragraphs" -> generateFromPool(sb, PARAGRAPHS, "Paragraphs", targetLen, "\n\n");
            case "Quotes" -> generateFromPool(sb, QUOTES, "Quotes", targetLen, "  ");
            case "Business English" -> generateFromPool(sb, BUSINESS, "Business English", targetLen, " ");
            case "Programming" -> generateFromPool(sb, PROGRAMMING, "Programming", targetLen, "\n\n");
            case "Numbers and Symbols" -> generateFromPool(sb, NUMBERS_AND_SYMBOLS, "Numbers and Symbols", targetLen, " ");
            case "Science and Technology" -> generateFromPool(sb, SCIENCE_AND_TECH, "Science and Technology", targetLen, " ");
            case "Geography and General Knowledge" -> generateFromPool(sb, GEOGRAPHY, "Geography and General Knowledge", targetLen, " ");
            case "Literature and Vocabulary" -> generateFromPool(sb, LITERATURE, "Literature and Vocabulary", targetLen, " ");
            case "Academic English" -> generateFromPool(sb, ACADEMIC, "Academic English", targetLen, " ");
            default -> generateFromPool(sb, COMMON_PHRASES, "Common English", targetLen, ". ");
        }

        return sb.toString().trim();
    }

    private static void generateRandomWords(StringBuilder sb, String difficulty, int targetLen) {
        String[] pool = "Hard".equalsIgnoreCase(difficulty) || "Expert".equalsIgnoreCase(difficulty)
                ? VOCAB_HARD
                : VOCAB_EASY;

        List<String> list = new ArrayList<>(Arrays.asList(pool));
        while (sb.length() < targetLen) {
            Collections.shuffle(list, RNG);
            for (String w : list) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(w);
                if (sb.length() >= targetLen) break;
            }
        }
    }

    private static void generateFromPool(StringBuilder sb, String[] pool, String catKey, int targetLen, String delimiter) {
        if (pool == null || pool.length == 0) return;

        Deque<Integer> recent = RECENT_INDEXES.computeIfAbsent(catKey, k -> new ArrayDeque<>());

        // Pick an index that wasn't used in recent calls if possible
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < pool.length; i++) {
            if (!recent.contains(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            recent.clear();
            for (int i = 0; i < pool.length; i++) candidates.add(i);
        }

        int startIdx = candidates.get(RNG.nextInt(candidates.size()));
        recent.addLast(startIdx);
        if (recent.size() > Math.min(4, pool.length - 1)) {
            recent.removeFirst();
        }

        // Add starting passage
        sb.append(pool[startIdx]);

        // If target length not satisfied, shuffle other passages to append
        int curIdx = (startIdx + 1) % pool.length;
        while (sb.length() < targetLen) {
            sb.append(delimiter).append(pool[curIdx]);
            curIdx = (curIdx + 1) % pool.length;
        }
    }
}
