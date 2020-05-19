package com.wonderpush.sdk;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;

public class SimpleVersion implements Comparable<SimpleVersion> {
    private int[] tokens;
    private boolean valid = false;
    public SimpleVersion(String versionString) {
        ArrayList<Integer> tokens = new ArrayList<>();
        StringBuffer currentIntegerString = null;
        for (int i = 0; versionString != null && i < versionString.length(); i++) {
            char c = versionString.charAt(i);
            if (c == 'v' && i == 0) continue;

            if (!isNumeric(c)) {
                if (currentIntegerString == null) return; // invalid
                tokens.add(Integer.parseInt(currentIntegerString.toString()));
                currentIntegerString = null;
                continue;
            }

            if (currentIntegerString == null) currentIntegerString = new StringBuffer();
            currentIntegerString.append(c);
        }
        if (currentIntegerString == null) return; // invalid
        tokens.add(Integer.parseInt(currentIntegerString.toString()));

        valid = true;
        // remove trailing zeros
        while (tokens.size() > 0 && tokens.get(tokens.size() - 1) == 0) tokens.remove(tokens.size() - 1);
        this.tokens = new int[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) this.tokens[i] = tokens.get(i);
    }

    private static boolean isNumeric(char c) {
        return c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9';
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < this.tokens.length; i++) {
            if (i > 0) result.append('.');
            result.append(Integer.toString(this.tokens[i]));
        }
        return result.toString();
    }

    @Override
    public int compareTo(SimpleVersion other) {
        if (!valid) {
            if (other.isValid()) return -1;
            else return 0;
        } else if (!other.isValid()) {
            return 1;
        }

        for (int i = 0; i < Math.max(this.tokens.length, other.tokens.length); i++) {
            int thisValue = i < this.tokens.length ? this.tokens[i] : 0;
            int otherValue = i < other.tokens.length ? other.tokens[i] : 0;
            if (thisValue < otherValue) return -1;
            else if (otherValue < thisValue) return 1;
        }
        return 0;
    }
}
