package com.seiftech.hifadhio.data;

import com.seiftech.hifadhio.R;
import java.util.Locale;

public class PlatformDetector {

    public static String detect(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "Web";
        }
        String u = url.toLowerCase(Locale.ROOT);
        if (u.contains("instagram.com") || u.contains("instagr.am")) {
            return "Instagram";
        }
        if (u.contains("tiktok.com")) {
            return "TikTok";
        }
        if (u.contains("youtube.com") || u.contains("youtu.be")) {
            return "YouTube";
        }
        if (u.contains("facebook.com") || u.contains("fb.watch") || u.contains("fb.com")) {
            return "Facebook";
        }
        if (u.contains("x.com") || u.contains("twitter.com")) {
            return "X";
        }
        if (u.contains("reddit.com") || u.contains("redd.it")) {
            return "Reddit";
        }
        if (u.contains("linkedin.com")) {
            return "LinkedIn";
        }
        return "Web";
    }

    public static int getPlatformColorRes(String platform) {
        if ("Instagram".equalsIgnoreCase(platform)) return R.color.platform_instagram;
        if ("TikTok".equalsIgnoreCase(platform)) return R.color.platform_tiktok;
        if ("YouTube".equalsIgnoreCase(platform)) return R.color.platform_youtube;
        if ("Facebook".equalsIgnoreCase(platform)) return R.color.platform_facebook;
        if ("X".equalsIgnoreCase(platform)) return R.color.platform_x;
        if ("Reddit".equalsIgnoreCase(platform)) return R.color.platform_reddit;
        return R.color.platform_web;
    }

    public static int getPlatformIconRes(String platform) {
        // Safe standard link vector fallback or platform specific
        return R.drawable.ic_link;
    }
}
