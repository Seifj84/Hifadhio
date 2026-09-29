package com.seiftech.hifadhio.data;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CollectionsAndTagsTest {

    @Test
    public void testTitleOverridePreservesOriginalTitle() {
        ContentItem item = new ContentItem();
        item.setUrl("https://www.youtube.com/watch?v=12345");
        item.setOriginalTitle("Original Extracted Video Title");
        item.setTitle("My Custom Notes Title");

        // Display title should prefer user customized title
        assertEquals("My Custom Notes Title", item.getDisplayTitle());
        // Original title is preserved
        assertEquals("Original Extracted Video Title", item.getOriginalTitle());
    }

    @Test
    public void testDisplayTitleFallbackHierarchy() {
        ContentItem item = new ContentItem();
        item.setUrl("https://instagram.com/reel/abcde123");
        item.setPlatform("Instagram");

        // 1. When neither title nor originalTitle is set -> intelligent platform fallback
        assertEquals("Instagram Reel", item.getDisplayTitle());

        // 2. When only originalTitle is set -> originalTitle
        item.setOriginalTitle("Extracted Reel Caption");
        assertEquals("Extracted Reel Caption", item.getDisplayTitle());

        // 3. When custom title is set -> custom title takes priority
        item.setTitle("Mastering Kotlin Coroutines");
        assertEquals("Mastering Kotlin Coroutines", item.getDisplayTitle());
    }

    @Test
    public void testTagNormalization() {
        String raw = "ai, #design,   tutorial   #productivity";
        List<String> tags = new ArrayList<>();
        String[] split = raw.split("[,\\s]+");
        for (String s : split) {
            String clean = s.trim();
            if (!clean.isEmpty()) {
                if (!clean.startsWith("#")) clean = "#" + clean;
                if (!tags.contains(clean)) tags.add(clean);
            }
        }

        assertEquals(4, tags.size());
        assertTrue(tags.contains("#ai"));
        assertTrue(tags.contains("#design"));
        assertTrue(tags.contains("#tutorial"));
        assertTrue(tags.contains("#productivity"));
    }

    @Test
    public void testCollectionNameValidation() {
        ContentItem item = new ContentItem();
        assertEquals("Inbox", item.getCollectionName());

        item.setCollectionName("  ");
        assertEquals("Inbox", item.getCollectionName());

        item.setCollectionName(null);
        assertEquals("Inbox", item.getCollectionName());

        item.setCollectionName("Design Systems");
        assertEquals("Design Systems", item.getCollectionName());
    }
}
