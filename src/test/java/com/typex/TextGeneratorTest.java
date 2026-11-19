package com.typex;

import com.typex.util.TextGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TextGeneratorTest {

    @Test
    void testAllCategoriesGeneration() {
        String[] categories = {
            "Random Words", "Common English", "Sentences", "Paragraphs",
            "Quotes", "Business English", "Programming", "Numbers and Symbols"
        };

        for (String cat : categories) {
            String text = TextGenerator.generate(cat, 300);
            assertNotNull(text);
            assertTrue(text.length() >= 250, "Category " + cat + " must satisfy minimum length");
            assertFalse(text.isBlank());
        }
    }

    @Test
    void testNonRepetitiveSuccessiveGenerations() {
        String t1 = TextGenerator.generate("Random Words", 300);
        String t2 = TextGenerator.generate("Random Words", 300);
        assertNotEquals(t1, t2, "Successive random word generations should differ");
    }
}
