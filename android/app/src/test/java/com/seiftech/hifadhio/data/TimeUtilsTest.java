package com.seiftech.hifadhio.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimeUtilsTest {

    @Test
    public void testJustNow() {
        long now = 1000000000000L;
        assertEquals("Just now", TimeUtils.formatRelativeTime(now, now));
        assertEquals("Just now", TimeUtils.formatRelativeTime(now - 30 * 1000L, now));
        assertEquals("Just now", TimeUtils.formatRelativeTime(now - 59 * 1000L, now));
    }

    @Test
    public void testMinutesAgo() {
        long now = 1000000000000L;
        assertEquals("1 min ago", TimeUtils.formatRelativeTime(now - 60 * 1000L, now));
        assertEquals("5 min ago", TimeUtils.formatRelativeTime(now - 5 * 60 * 1000L, now));
        assertEquals("45 min ago", TimeUtils.formatRelativeTime(now - 45 * 60 * 1000L, now));
    }

    @Test
    public void testHoursAgo() {
        long now = 1000000000000L;
        assertEquals("1 hr ago", TimeUtils.formatRelativeTime(now - 60 * 60 * 1000L, now));
        assertEquals("3 hr ago", TimeUtils.formatRelativeTime(now - 3 * 60 * 60 * 1000L, now));
        assertEquals("23 hr ago", TimeUtils.formatRelativeTime(now - 23 * 60 * 60 * 1000L, now));
    }

    @Test
    public void testDaysAgo() {
        long now = 1000000000000L;
        assertEquals("1 day ago", TimeUtils.formatRelativeTime(now - 24 * 60 * 60 * 1000L, now));
        assertEquals("4 days ago", TimeUtils.formatRelativeTime(now - 4 * 24 * 60 * 60 * 1000L, now));
    }
}
