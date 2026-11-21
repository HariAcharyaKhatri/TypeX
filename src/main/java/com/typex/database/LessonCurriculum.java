package com.typex.database;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete 100-Lesson Curriculum for TYPEX.
 * Structured into 5 progressive levels of 20 lessons each:
 * 1. Beginner (Level 1, Lessons 1-20)
 * 2. Elementary (Level 2, Lessons 21-40)
 * 3. Intermediate (Level 3, Lessons 41-60)
 * 4. Advanced (Level 4, Lessons 61-80)
 * 5. Professional (Level 5, Lessons 81-100)
 */
public final class LessonCurriculum {

    private LessonCurriculum() {}

    public static class LessonSeed {
        public final int categoryId;
        public final int level;
        public final int orderIndex;
        public final String title;
        public final String objective;
        public final String instructions;
        public final String practiceText;
        public final double minWpm;
        public final double minAccuracy;

        public LessonSeed(int categoryId, int level, int orderIndex, String title,
                          String objective, String instructions, String practiceText,
                          double minWpm, double minAccuracy) {
            this.categoryId = categoryId;
            this.level = level;
            this.orderIndex = orderIndex;
            this.title = title;
            this.objective = objective;
            this.instructions = instructions;
            this.practiceText = practiceText;
            this.minWpm = minWpm;
            this.minAccuracy = minAccuracy;
        }
    }

    public static List<LessonSeed> getFullCurriculum() {
        List<LessonSeed> list = new ArrayList<>(100);

        // ==========================================
        // LEVEL 1: BEGINNER (Lessons 1 - 20)
        // ==========================================
        list.add(new LessonSeed(1, 1, 1, "F and J Anchor Keys",
                "Locate home anchor keys by tactile feel",
                "Place left index on F (feel the tactile bump) and right index on J. Keep hands relaxed.",
                "fff jjj fff jjj fj fj jf jf ff jj fj fff jjj fj fj", 15.0, 90.0));

        list.add(new LessonSeed(1, 1, 2, "Left-Hand Home Row: ASDF",
                "Master A, S, D, and F key strikes with proper fingers",
                "Pinky on A, ring on S, middle on D, index on F. Tap lightly without lifting your palm.",
                "aaa sss ddd fff asdf asdf fdsa fdsa sad fad dad fads", 15.0, 90.0));

        list.add(new LessonSeed(1, 1, 3, "Right-Hand Home Row: JKL;",
                "Master J, K, L, and semicolon strikes with right-hand fingers",
                "Index on J, middle on K, ring on L, pinky on semicolon. Keep wrist hovering slightly.",
                "jjj kkk lll ;;; jkl; jkl; ;lkj ;lkj ask all fall silk", 15.0, 90.0));

        list.add(new LessonSeed(1, 1, 4, "Full Home Row Integration",
                "Combine all home keys smoothly across both hands",
                "Move between hands without looking down. Rest thumbs on the spacebar.",
                "asdf jkl; asdf jkl; a s d f j k l ; sad lad ask all fall", 16.0, 90.0));

        list.add(new LessonSeed(1, 1, 5, "Spacebar Technique & Thumb Cadence",
                "Use alternating thumbs cleanly on the spacebar",
                "Strike the spacebar with your dominant thumb immediately after completing a word.",
                "a fad a lad a sad fall all asks dads fads lads skids", 16.0, 90.0));

        list.add(new LessonSeed(1, 1, 6, "Left Index Reach: G Key",
                "Extend left index from F to G and return to F",
                "Reach left index inward to G, then promptly return to F anchor position.",
                "fg fg gf gf gag flag glad gaf gas flags fads lags saga", 16.0, 90.0));

        list.add(new LessonSeed(1, 1, 7, "Right Index Reach: H Key",
                "Extend right index from J to H and return to J",
                "Reach right index inward to H, then promptly return to J anchor position.",
                "jh jh hj hj half hash flash slash shah halls dash sash", 17.0, 91.0));

        list.add(new LessonSeed(1, 1, 8, "G and H Dual Reach",
                "Coordinate center keys G and H seamlessly",
                "Alternate left index on G and right index on H without colliding hands.",
                "gh gh hg hg gag hash glad half flag shall gala dash shah", 17.0, 91.0));

        list.add(new LessonSeed(1, 1, 9, "Left Hand Reach: E Key",
                "Reach left middle finger up from D to E",
                "Strike E with left middle finger and immediately drop back to D.",
                "de de ed ed see fed led sled feed deaf seed fees lake", 17.0, 91.0));

        list.add(new LessonSeed(1, 1, 10, "Right Hand Reach: I Key",
                "Reach right middle finger up from K to I",
                "Strike I with right middle finger and return to home key K.",
                "ki ki ik ik if is kid fill kill lids silk sail fail hill", 18.0, 91.0));

        list.add(new LessonSeed(1, 1, 11, "Left Index Up: R Key",
                "Extend left index finger up and slightly inward to R",
                "Reach up to R with left index finger and return to F.",
                "fr fr rf rf far red fir fire free fear rear dare fare", 18.0, 91.0));

        list.add(new LessonSeed(1, 1, 12, "Right Index Up: U Key",
                "Extend right index finger up and slightly inward to U",
                "Reach up to U with right index finger and return to J.",
                "ju ju uj uj full hull skull dual fuel fur fuss rude sure", 18.0, 91.0));

        list.add(new LessonSeed(1, 1, 13, "Left Index High: T Key",
                "Reach left index finger up and right to T",
                "Reach from F up to T cleanly, keeping left ring and pinky in place.",
                "ft ft tf tf the tell tell tall taste fast fate tart star", 19.0, 92.0));

        list.add(new LessonSeed(1, 1, 14, "Right Index High: Y Key",
                "Reach right index finger up and left to Y",
                "Extend right index from J up to Y and return to home position.",
                "jy jy yj yj say day lay yell easy gray hay yard year", 19.0, 92.0));

        list.add(new LessonSeed(1, 1, 15, "Left Ring Up: W Key",
                "Reach left ring finger up from S to W",
                "Lift left ring finger up to W while keeping index on F.",
                "sw sw ws ws we was raw saw war wide wish walk wait wild", 19.0, 92.0));

        list.add(new LessonSeed(1, 1, 16, "Right Ring Up: O Key",
                "Reach right ring finger up from L to O",
                "Lift right ring finger up to O and drop back to L.",
                "lo lo ol ol of on or out old fold look book food wood", 20.0, 92.0));

        list.add(new LessonSeed(1, 1, 17, "Left Pinky Up: Q Key",
                "Reach left pinky up from A to Q",
                "Reach gently with left pinky to Q, avoiding wrist twisting.",
                "aq aq qa qa quit quiz quest quick equal quote squad square", 20.0, 92.0));

        list.add(new LessonSeed(1, 1, 18, "Right Pinky Up: P Key",
                "Reach right pinky up from semicolon to P",
                "Extend right pinky to P and return promptly to home row.",
                ";p ;p p; p; pat pan cup drop ship plan play pure push", 20.0, 92.0));

        list.add(new LessonSeed(1, 1, 19, "Common Two-Letter Digraphs",
                "Build muscle memory for frequent letter pairings",
                "Type these recurring digraphs with smooth, continuous rhythm.",
                "th he in er an re nd at on en ed to it ou ea hi is or ti", 20.0, 92.0));

        list.add(new LessonSeed(1, 1, 20, "Beginner Milestone Evaluation",
                "Demonstrate fluent touch typing across the home and top rows",
                "Maintain steady posture, zero glancing, and consistent cadence.",
                "the quick red fox saw two wild deer leap high over rocks", 22.0, 92.0));

        // ==========================================
        // LEVEL 2: ELEMENTARY (Lessons 21 - 40)
        // ==========================================
        list.add(new LessonSeed(2, 2, 1, "Bottom Row Left Index: V Key",
                "Reach left index finger down and right from F to V",
                "Bend left index finger downward to strike V, returning to F.",
                "fv fv vf vf live love have give view visit brave vast voice", 24.0, 92.0));

        list.add(new LessonSeed(2, 2, 2, "Bottom Row Left Index: B Key",
                "Extend left index finger down and inward to B",
                "Reach from F down-right to B without dragging your wrist.",
                "fb fb bf bf be but best build blue book bread bulb bench", 24.0, 92.0));

        list.add(new LessonSeed(2, 2, 3, "Bottom Row Right Index: M Key",
                "Reach right index finger down and left from J to M",
                "Drop right index finger to M and recover quickly to home J.",
                "jm jm mj mj me my man make time home same calm moon", 25.0, 92.0));

        list.add(new LessonSeed(2, 2, 4, "Bottom Row Right Index: N Key",
                "Reach right index finger down and inward to N",
                "Drop right index to N and snap back to J anchor position.",
                "jn jn nj nj no now new name mind nine rain fine moon", 25.0, 92.0));

        list.add(new LessonSeed(2, 2, 5, "Bottom Row Left Middle: C Key",
                "Drop left middle finger down from D to C",
                "Curl left middle finger cleanly to C and return to D.",
                "dc dc cd cd can car cat cool code clean cloud cream face", 25.0, 92.0));

        list.add(new LessonSeed(2, 2, 6, "Bottom Row Left Ring: X Key",
                "Drop left ring finger down from S to X",
                "Curl left ring finger downward to strike X without shifting palm.",
                "sx sx xs xs box fox six text next exam extra exact axis", 26.0, 92.0));

        list.add(new LessonSeed(2, 2, 7, "Bottom Row Left Pinky: Z Key",
                "Reach left pinky down from A to Z",
                "Drop left pinky to Z, keeping remaining fingers on home row.",
                "az az za za zoo zip zone zero zinc size lazy prize breeze", 26.0, 92.0));

        list.add(new LessonSeed(2, 2, 8, "Bottom Row Right Middle: Comma",
                "Reach right middle finger down from K to comma",
                "Curl right middle finger down to comma and tap smoothly.",
                "k, k, ,k ,k yes, no, well, see, run, jump, read, write,", 26.0, 93.0));

        list.add(new LessonSeed(2, 2, 9, "Bottom Row Right Ring: Period",
                "Reach right ring finger down from L to period",
                "Drop right ring finger to period. Follow periods with space.",
                "l. l. .l .l go. stop. wait. look. listen. learn. repeat.", 27.0, 93.0));

        list.add(new LessonSeed(2, 2, 10, "Bottom Row Right Pinky: Slash",
                "Reach right pinky down from semicolon to forward slash",
                "Curl right pinky to forward slash without turning your hand.",
                ";/ ;/ /; /; and/or yes/no true/false day/night input/output", 27.0, 93.0));

        list.add(new LessonSeed(2, 2, 11, "Capitalization: Right Shift Mechanics",
                "Use right pinky Shift when capitalizing left-hand letters",
                "Hold right Shift with right pinky while typing left-hand key.",
                "The First Day Saw Great Things Fall Away Gently Free", 28.0, 93.0));

        list.add(new LessonSeed(2, 2, 12, "Capitalization: Left Shift Mechanics",
                "Use left pinky Shift when capitalizing right-hand letters",
                "Hold left Shift with left pinky while typing right-hand key.",
                "Just Keep Looking Out Over New Mountains In Japan", 28.0, 93.0));

        list.add(new LessonSeed(2, 2, 13, "Alternating Shift Hand Coordination",
                "Fluidly alternate between left and right Shift keys",
                "Never use the same hand to hold Shift and strike the letter.",
                "London Paris Tokyo Berlin Rome Madrid Ottawa Sydney Oslo", 28.0, 93.0));

        list.add(new LessonSeed(2, 2, 14, "Short Vowel Combinations",
                "Build speed on recurring vowel structures",
                "Smooth out micro-pauses when transitioning across vowels.",
                "about house noise count voice cause friend peace guide", 29.0, 93.0));

        list.add(new LessonSeed(2, 2, 15, "Consonant Blends & Clusters",
                "Type common consonant clusters with fluid cadence",
                "Keep finger strikes crisp and distinct on consonant clusters.",
                "black clean drink fresh green plant stand train spring", 29.0, 93.0));

        list.add(new LessonSeed(2, 2, 16, "Punctuation in Dialogue & Sentences",
                "Integrate commas, periods, and capitalization in narrative",
                "Strike punctuation in cadence without breaking typing flow.",
                "Look ahead, breathe steadily, and type with quiet confidence.", 30.0, 93.0));

        list.add(new LessonSeed(2, 2, 17, "Common English Contractions",
                "Master apostrophe placement using right pinky",
                "Tap apostrophe with right pinky and resume typing immediately.",
                "don't can't won't shouldn't it's they're we'll you've isn't", 30.0, 93.0));

        list.add(new LessonSeed(2, 2, 18, "Paragraph Pacing & Flow",
                "Sustain uninterrupted rhythm through full descriptive sentences",
                "Read one word ahead to anticipate upcoming finger movements.",
                "Early morning sunlight warmed the valley as mist rose slowly.", 31.0, 93.0));

        list.add(new LessonSeed(2, 2, 19, "Speed Burst on High-Frequency Words",
                "Sprint across the 50 most common English words",
                "Let familiar words flow as single automatic motor units.",
                "that with have this from they which one would their there", 32.0, 94.0));

        list.add(new LessonSeed(2, 2, 20, "Elementary Milestone Evaluation",
                "Demonstrate comprehensive mastery across all letter rows",
                "Full text typing with capitals, punctuation, and smooth rhythm.",
                "Every skilled typist began with patient practice and determination.", 32.0, 94.0));

        // ==========================================
        // LEVEL 3: INTERMEDIATE (Lessons 41 - 60)
        // ==========================================
        list.add(new LessonSeed(3, 3, 1, "Left Hand Numbers: 1, 2, 3",
                "Reach left pinky, ring, and middle fingers up to numbers 1, 2, 3",
                "Reach upward without moving palm from its hover position.",
                "111 222 333 12 23 31 123 321 132 213 11 22 33 1231", 34.0, 94.0));

        list.add(new LessonSeed(3, 3, 2, "Left Index Numbers: 4 and 5",
                "Reach left index finger up to 4 and 5",
                "Reach left index from F up to 4 and slightly inward to 5.",
                "444 555 45 54 44 55 14 25 34 51 454 545 415 524 354", 34.0, 94.0));

        list.add(new LessonSeed(3, 3, 3, "Right Index Numbers: 6 and 7",
                "Reach right index finger up to 6 and 7",
                "Reach right index from J up-left to 6 and directly up to 7.",
                "666 777 67 76 66 77 767 676 716 276 367 756 647 767", 35.0, 94.0));

        list.add(new LessonSeed(3, 3, 4, "Right Hand Numbers: 8, 9, 0",
                "Reach right middle, ring, and pinky fingers to 8, 9, 0",
                "Middle to 8, ring to 9, pinky to 0. Drop back to home row.",
                "888 999 000 89 90 08 890 098 809 908 1980 2000 2024", 35.0, 94.0));

        list.add(new LessonSeed(3, 3, 5, "Full Number Row Navigation",
                "Type mixed multi-digit numbers without looking down",
                "Pace yourself deliberately on numbers to eliminate misstrikes.",
                "102 304 506 708 901 1984 2048 4096 8192 16384 32768", 36.0, 94.0));

        list.add(new LessonSeed(3, 3, 6, "Dates, Phone Numbers & Times",
                "Format realistic numerical sequences with hyphens and colons",
                "Maintain hand discipline across alternating number and symbol keys.",
                "2026-11-21 12:30 PM 555-0199 10/15/2024 08:45:00 404-876", 36.0, 94.0));

        list.add(new LessonSeed(3, 3, 7, "Currency & Decimal Precision",
                "Combine dollar signs, decimals, and number rows",
                "Hold Shift with right pinky to strike dollar sign on 4.",
                "$14.50 $99.99 $1,250.00 $450.75 $8.25 $3,400.10 $19.00", 36.0, 94.0));

        list.add(new LessonSeed(3, 3, 8, "Basic Shift Symbols: ! @ # $ %",
                "Master left-hand shifted symbols above 1, 2, 3, 4, 5",
                "Hold right Shift firmly while selecting target symbol keys.",
                "Item #1 is 100%! Call @home today. Save $50 on order #4.", 37.0, 94.0));

        list.add(new LessonSeed(3, 3, 9, "Advanced Shift Symbols: ^ & * ( )",
                "Master right-hand shifted symbols above 6, 7, 8, 9, 0",
                "Hold left Shift firmly while striking upper right symbol keys.",
                "Math: (8 * 9) & (7 ^ 2) = result; check (a + b) * (c - d).", 37.0, 94.0));

        list.add(new LessonSeed(3, 3, 10, "Brackets, Braces & Parentheses",
                "Navigate brackets and structural enclosures cleanly",
                "Use right pinky to reach [ ] { } and left shift for braces.",
                "data[0] = {id: 1, name: 'alpha', tags: ['fast', 'clean']};", 38.0, 94.0));

        list.add(new LessonSeed(3, 3, 11, "Mathematical & Comparison Operators",
                "Practice plus, equals, minus, angle brackets, and slashes",
                "Keep steady cadence when typing logic and comparison operations.",
                "if (x >= 10 && y <= 20) { total = count * 2 + bonus - 1; }", 38.0, 95.0));

        list.add(new LessonSeed(3, 3, 12, "Mixed Alphanumeric Codes & SKU IDs",
                "Type commercial identifiers with mixed case and digits",
                "Pay strict attention to every individual glyph without guessing.",
                "Model-X902 serial #TX-4482-B verified at Station-14 on 09/25.", 38.0, 95.0));

        list.add(new LessonSeed(3, 3, 13, "Complex Punctuation Cadence",
                "Handle em-dashes, semicolons, colons, and quotations",
                "Punctuation adds rhythm; treat each symbol as a musical beat.",
                "Note this: patience—though difficult—always rewards the disciplined.", 39.0, 95.0));

        list.add(new LessonSeed(3, 3, 14, "Typing Rhythm & Cadence Stability",
                "Eliminate pauses between easy words and difficult words",
                "A steady metronome pace outperforms erratic bursts every time.",
                "True typing speed is born from smooth and unhurried consistency.", 40.0, 95.0));

        list.add(new LessonSeed(3, 3, 15, "Backspace Reduction Drill",
                "Think the complete word before striking the first letter",
                "Reduce impulse backspaces by typing at 85% of your top speed.",
                "Deliberate practice demands conscious precision over hasty errors.", 40.0, 95.0));

        list.add(new LessonSeed(3, 3, 16, "Error Recovery Technique",
                "Recover focus immediately after committing a typographical error",
                "Do not panic after a mistake; reset your posture and resume.",
                "Resilience turns temporary missteps into permanent competence.", 41.0, 95.0));

        list.add(new LessonSeed(3, 3, 17, "Sustained Narrative Typing",
                "Maintain posture and focus across a 200-character paragraph",
                "Breathe evenly and let your wrists remain relaxed.",
                "The quiet library smelled of aged paper and polished mahogany shelves.", 41.0, 95.0));

        list.add(new LessonSeed(3, 3, 18, "Business Email Fragments",
                "Type realistic commercial communication snippets",
                "Practice professional vocabulary with standard salutations.",
                "Please review the attached quarterly summary before our 2 PM call.", 42.0, 95.0));

        list.add(new LessonSeed(3, 3, 19, "Technical Jargon & Compound Terms",
                "Type specialized multi-syllable terms with zero pauses",
                "Chunk syllables mentally: infra-structure, micro-service, sub-system.",
                "Distributed infrastructure optimizes high-availability microservices.", 42.0, 95.0));

        list.add(new LessonSeed(3, 3, 20, "Intermediate Milestone Evaluation",
                "Full intermediate benchmark combining text, numbers, and symbols",
                "Execute clean typing with zero hesitation across varied inputs.",
                "Order #7890 for 15 units ($450.00) shipped to Seattle, WA on 11/20.", 42.0, 95.0));

        // ==========================================
        // LEVEL 4: ADVANCED (Lessons 61 - 80)
        // ==========================================
        list.add(new LessonSeed(4, 4, 1, "Polysyllabic Word Chunkiing",
                "Type long complex words by decomposing them into syllables",
                "Keep rhythm steady even when confronting 14+ character words.",
                "Interdisciplinary synchronization conceptualization institutionalization", 44.0, 95.0));

        list.add(new LessonSeed(4, 4, 2, "Scientific & Academic Terminology",
                "Type formal academic vocabulary accurately",
                "Focus on Latin and Greek root patterns common in scholarship.",
                "Photosynthesis, thermodynamic equilibrium, and epigenetic modification.", 45.0, 95.0));

        list.add(new LessonSeed(4, 4, 3, "Formal Legal & Contractual Phrasing",
                "Type precision-dependent contractual language",
                "Legal text requires absolute zero-error fidelity on every letter.",
                "The parties hereby agree to indemnify and hold harmless the licensee.", 45.0, 95.0));

        list.add(new LessonSeed(4, 4, 4, "Executive Memorandums & Reports",
                "Type business executive summaries and board memos",
                "Clean punctuation, capitalization, and professional prose.",
                "Revenue expanded 14.8% year-over-year while operational expenses declined.", 46.0, 95.0));

        list.add(new LessonSeed(4, 4, 5, "Programming Syntax: Variables & Types",
                "Type realistic variable declarations across modern languages",
                "Navigate camelCase, snake_case, semicolons, and type annotations.",
                "final Map<String, List<Integer>> scoreCache = new ConcurrentHashMap<>();", 46.0, 95.0));

        list.add(new LessonSeed(4, 4, 6, "Programming Syntax: Functions & Lambdas",
                "Type arrow functions, lambda expressions, and method signatures",
                "Master brackets, arrows (-> and =>), and parentheses.",
                "items.stream().filter(x -> x.isValid()).map(x -> x.getName()).toList();", 47.0, 95.0));

        list.add(new LessonSeed(4, 4, 7, "Programming Syntax: JSON & Data Objects",
                "Type structured JSON objects with quotes, colons, and braces",
                "Alternate between double quotes, colons, commas, and indentation.",
                "{\"status\": 200, \"message\": \"OK\", \"data\": {\"id\": 42, \"active\": true}}", 47.0, 95.0));

        list.add(new LessonSeed(4, 4, 8, "SQL Queries & Database Statements",
                "Type SQL keywords in capitals mixed with lowercase identifiers",
                "Smoothly toggle Shift between SQL keywords and table names.",
                "SELECT u.id, u.username, SUM(s.score) FROM users u JOIN sessions s GROUP BY u.id;", 48.0, 96.0));

        list.add(new LessonSeed(4, 4, 9, "Command-Line Terminal Syntaxes",
                "Type terminal commands with flags, pipes, and file paths",
                "Navigate hyphens, forward slashes, and shell arguments.",
                "git commit -m \"feat: implement new game engine\" && mvn clean test", 48.0, 96.0));

        list.add(new LessonSeed(4, 4, 10, "Dense Punctuation Challenge",
                "Handle continuous punctuation without hesitation",
                "Punctuation density tests fine motor finger independence.",
                "\"Wait!\" she cried; \"Can't you see the error in line #42 (idx == -1)?\"", 49.0, 96.0));

        list.add(new LessonSeed(4, 4, 11, "Rapid Hand Alternation Sprints",
                "Type words where successive letters strictly alternate hands",
                "Alternating hand patterns produce the highest theoretical typing speeds.",
                "problem giant island visual formal handle monitor bicycle audit", 50.0, 96.0));

        list.add(new LessonSeed(4, 4, 12, "One-Hand Dominant Sequences",
                "Maintain speed when words require consecutive same-hand strikes",
                "Keep fingers agile on same-hand reaches: 'sweater', 'minimum'.",
                "sweater evaluate create cascade lollipop opinion million minimum", 50.0, 96.0));

        list.add(new LessonSeed(4, 4, 13, "High-Speed Bursts on Short Words",
                "Burst common words at maximum velocity",
                "Release mental speed governors and let fingers flow freely.",
                "all the world can see how fast true mastery takes flight today", 52.0, 96.0));

        list.add(new LessonSeed(4, 4, 14, "Long-Form Endurance: Paragraph 1",
                "Sustain uninterrupted focus across a 300-character block",
                "Pacing is paramount; maintain a sustainable and relaxed rhythm.",
                "Software architecture resembles urban planning: individual components must interact harmoniously while preserving autonomy.", 52.0, 96.0));

        list.add(new LessonSeed(4, 4, 15, "Long-Form Endurance: Paragraph 2",
                "Type dense philosophical prose with zero drop in accuracy",
                "Keep wrists hovering comfortably above the keyboard desk.",
                "The pursuit of excellence is not a destination but a continuous habit practiced across every quiet keystroke.", 53.0, 96.0));

        list.add(new LessonSeed(4, 4, 16, "Medical & Biological Science Terms",
                "Accurately transcribe specialized biological classifications",
                "Focus carefully on spelling patterns: -itis, -osis, -ectomy.",
                "Mitochondrial respiration produces adenosine triphosphate for cellular metabolism.", 53.0, 96.0));

        list.add(new LessonSeed(4, 4, 17, "Financial & Economic Commentary",
                "Type complex fiscal analysis with numbers, percentages, and prose",
                "Balance speed with numerical accuracy on market commentary.",
                "Central bank interest rates stabilized at 4.25% amid moderating core inflation.", 54.0, 96.0));

        list.add(new LessonSeed(4, 4, 18, "Classic Literature Transcription",
                "Type evocative prose from historic literature",
                "Immerse yourself in rich cadence and timeless vocabulary.",
                "It was the best of times, it was the worst of times, the age of wisdom and foolishness.", 54.0, 96.0));

        list.add(new LessonSeed(4, 4, 19, "Tricky Spelling & Silent Letters",
                "Master words with irregular orthography and silent consonants",
                "Type what is correct, not merely what the ear hears phonetically.",
                "Knight rhythm subtle receipt drought reign pneumonia subtle colonel", 55.0, 96.0));

        list.add(new LessonSeed(4, 4, 20, "Advanced Milestone Evaluation",
                "Comprehensive high-speed benchmark across prose, code, and symbols",
                "Maintain ≥ 55 WPM and ≥ 96% accuracy across diverse structures.",
                "Benchmark 2026: The agile developer verified 100% test coverage in 45.2s.", 55.0, 96.0));

        // ==========================================
        // LEVEL 5: PROFESSIONAL (Lessons 81 - 100)
        // ==========================================
        list.add(new LessonSeed(5, 5, 1, "Technical Documentation Standards",
                "Type developer documentation and RFC specifications",
                "Precision formatting with markdown conventions and technical clarity.",
                "The API client MUST authenticate using bearer tokens supplied in HTTP headers.", 58.0, 97.0));

        list.add(new LessonSeed(5, 5, 2, "Configuration: YAML & Properties",
                "Type configuration key-value mappings and nesting",
                "Careful alignment with colons, indentation, and booleans.",
                "server:\n  port: 8080\n  ssl:\n    enabled: true\n    cert: /etc/ssl/cert.pem", 58.0, 97.0));

        list.add(new LessonSeed(5, 5, 3, "Algorithms & Complexity Notes",
                "Type algorithmic analysis with Big-O notation and math symbols",
                "Navigate O(n log n), superscripts, subscripts, and parameters.",
                "Quicksort exhibits O(n log n) expected time and O(n^2) worst-case time complexity.", 59.0, 97.0));

        list.add(new LessonSeed(5, 5, 4, "DevOps & Infrastructure As Code",
                "Type cloud deployment scripts and orchestration definitions",
                "Complex syntax: curly braces, variable interpolations, and flags.",
                "docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=secret --name db postgres:16", 59.0, 97.0));

        list.add(new LessonSeed(5, 5, 5, "Distributed Systems Engineering",
                "Type architectural principles of distributed computing",
                "Sustained high-speed professional prose with technical accuracy.",
                "The CAP theorem posits that a distributed store can guarantee at most two of consistency, availability, and partition tolerance.", 60.0, 97.0));

        list.add(new LessonSeed(5, 5, 6, "Patent & Intellectual Property Text",
                "Type dense patent claims with legal precision",
                "Formal legal jargon with repetitive claim references.",
                "A computer-implemented method comprising generating a cryptographically secure token.", 60.0, 97.0));

        list.add(new LessonSeed(5, 5, 7, "Clinical Trial & Medical Reports",
                "Type randomized clinical trial protocol summaries",
                "Latin medical phrasing, dosages, and statistical p-values.",
                "The double-blind placebo trial demonstrated statistical significance (p < 0.001) at 50mg dosage.", 61.0, 97.0));

        list.add(new LessonSeed(5, 5, 8, "Corporate Mergers & Acquisitions",
                "Type investment banking summaries and balance sheets",
                "Corporate terminology, valuations, and fiscal percentages.",
                "The enterprise acquisition valued at $4.2B closed on schedule with regulatory approval.", 61.0, 97.0));

        list.add(new LessonSeed(5, 5, 9, "Diplomatic & Treaties Transcription",
                "Type formal international protocols and declarations",
                "Archaic and diplomatic phrasing with solemn cadence.",
                "Reaffirming faith in fundamental human rights, dignity, and the equal rights of men and women.", 62.0, 97.0));

        list.add(new LessonSeed(5, 5, 10, "Journalistic Breaking News Copy",
                "Type fast-paced news wire reporting under speed demands",
                "Direct, informative sentences packed with facts and quotations.",
                "GENEVA — International delegates reached a landmark climate accord early Saturday morning.", 62.0, 97.0));

        list.add(new LessonSeed(5, 5, 11, "High-Speed Sprint: 65+ WPM Push",
                "Push your throughput threshold past 65 words per minute",
                "Relax your shoulders and breathe steadily as keystrokes accelerate.",
                "Speed is an echo of perfect form; when form is pure, velocity becomes effortless.", 65.0, 97.0));

        list.add(new LessonSeed(5, 5, 12, "High-Speed Sprint: 70+ WPM Push",
                "Push your throughput threshold past 70 words per minute",
                "Eliminate any visual glancing; your fingers know exactly where to travel.",
                "Great achievements emerge from small daily improvements consistently compounded over time.", 70.0, 97.0));

        list.add(new LessonSeed(5, 5, 13, "Endurance Marathon: 400 Characters",
                "Maintain professional speed and accuracy across a long paragraph",
                "Pace yourself like a distance runner; do not sprint early and tire.",
                "Touch typing is the primary gateway between human thought and digital creation. When typing becomes subconscious, cognitive load drops, allowing focus to dwell entirely upon creative problem solving.", 63.0, 97.0));

        list.add(new LessonSeed(5, 5, 14, "Zero-Error Precision Drill",
                "Demand 100% precision with zero corrected backspaces",
                "Prioritize absolute correctness on every single keystroke.",
                "Discipline is choosing between what you want now and what you want most in life.", 60.0, 98.0));

        list.add(new LessonSeed(5, 5, 15, "Complex Code & Symbol Transcription",
                "Type mixed Java and Python algorithmic blocks",
                "Dense brackets, indentation, colons, arrows, and operators.",
                "public record Point(double x, double y) { public double dist() { return Math.hypot(x, y); } }", 62.0, 97.0));

        list.add(new LessonSeed(5, 5, 16, "Unfamiliar Vocabulary Blind Typing",
                "Transcribe rare and foreign loanwords without prior muscle memory",
                "Sight-read letter by letter with steady, unhurried cadence.",
                "Schadenfreude, zeitgeist, rendezvous, chiaroscuro, and quintessential idiosyncratic prose.", 60.0, 97.0));

        list.add(new LessonSeed(5, 5, 17, "High-Speed Sprint: 75+ WPM Push",
                "Push towards elite touch-typing throughput",
                "Let familiar phrases roll off your fingertips in seamless bursts.",
                "The mind moves with instantaneous clarity when all mechanical friction is eliminated.", 75.0, 97.0));

        list.add(new LessonSeed(5, 5, 18, "Professional Speed Benchmark: 80+ WPM",
                "Reach for master-tier typing speeds on fluent English",
                "Breathe calmly and maintain a state of relaxed concentration.",
                "Mastery is not an accident; it is the natural consequence of persistent and thoughtful practice.", 80.0, 97.0));

        list.add(new LessonSeed(5, 5, 19, "Comprehensive Professional Assessment",
                "Full professional benchmark combining prose, numbers, and syntax",
                "Sustain professional speed and precision across diverse character sets.",
                "In 2026, Senior Engineer #42 deployed v2.1.0 with 99.99% reliability and zero downtime.", 65.0, 98.0));

        list.add(new LessonSeed(5, 5, 20, "TYPEX Grandmaster Graduation Test",
                "The ultimate typing test certifying complete keyboard mastery",
                "Demonstrate flawless speed, rhythm, endurance, and 98%+ precision.",
                "You have mastered every row, symbol, and rhythm. The keyboard is now an extension of your thought.", 65.0, 98.0));

        return list;
    }
}
