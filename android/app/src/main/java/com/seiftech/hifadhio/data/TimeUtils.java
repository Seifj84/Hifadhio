package com.seiftech.hifadhio.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static String formatRelativeTime(long timestamp) {
        return formatRelativeTime(timestamp, System.currentTimeMillis());
    }

    public static String formatRelativeTime(long timestamp, long now) {
        long diff = now - timestamp;
        if (diff < 0) diff = 0;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes == 1 ? "1 min ago" : minutes + " min ago";
        } else if (hours < 24) {
            return hours == 1 ? "1 hr ago" : hours + " hr ago";
        } else if (days < 7) {
            return days == 1 ? "1 day ago" : days + " days ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
