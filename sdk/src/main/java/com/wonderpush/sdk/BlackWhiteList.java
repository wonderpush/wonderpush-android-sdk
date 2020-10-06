package com.wonderpush.sdk;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

class BlackWhiteList {

    private final List<String> blackList = new ArrayList<>();
    private final List<String> whiteList = new ArrayList<>();
    BlackWhiteList(String[] rules) {
        this(Arrays.asList(rules));
    }
    BlackWhiteList(List<String> rules) {
        for (String rule : rules) {
            if (rule != null && rule.startsWith("-")) blackList.add(rule.substring(1));
            else if (rule != null) whiteList.add(rule);
        }
    }

    public boolean allow(String item) {
        for (String rule : whiteList) {
            if (itemMatchesRule(item, rule)) return true;
        }
        for (String rule : blackList) {
            if (itemMatchesRule(item, rule)) return false;
        }
        return true;
    }

    public static boolean itemMatchesRule(@NonNull String item, @NonNull String rule) {
        if (item == null || rule == null) return false;
        String[] tokens = rule.split(Pattern.quote("*"));
        StringBuffer buffer = new StringBuffer("^");
        // String.split never puts an empty string as first item
        if (rule.startsWith("*")) buffer.append(".*");
        for (int i = 0; i < tokens.length; i++) {
            buffer.append(Pattern.quote(tokens[i]));
            if (i < tokens.length - 1) buffer.append(".*");
        }
        // String.split never puts an empty string as last item
        if (rule.endsWith("*")) buffer.append(".*");
        buffer.append("$");
        boolean result = item.matches(buffer.toString());
        return result;
    }

    @NonNull
    public List<String> getBlackList() {
        return new ArrayList<>(blackList);
    }

    @NonNull
    public List<String> getWhiteList() {
        return new ArrayList<>(whiteList);
    }
}
