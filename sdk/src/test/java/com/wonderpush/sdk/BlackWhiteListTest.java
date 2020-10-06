package com.wonderpush.sdk;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BlackWhiteListTest {
    @Test
    public void testParseMinus() {
        List<String> rules = new ArrayList<>();
        rules.add("plus");
        rules.add("-minus");
        rules.add("-minus");
        rules.add("plus");
        BlackWhiteList blackWhiteList = new BlackWhiteList(rules);

        List<String> expectedBlackList = new ArrayList<>();
        expectedBlackList.add("minus");
        expectedBlackList.add("minus");

        assertEquals(expectedBlackList, blackWhiteList.getBlackList());

        List<String> expectedWhiteList = new ArrayList<>();
        expectedWhiteList.add("plus");
        expectedWhiteList.add("plus");
        assertEquals(expectedWhiteList, blackWhiteList.getWhiteList());
    }

    @Test
    public void testItemMatches() {
        assertTrue(BlackWhiteList.itemMatchesRule("foobar", "foo*"));
        assertTrue(BlackWhiteList.itemMatchesRule("foo", "*"));
        assertTrue(BlackWhiteList.itemMatchesRule("", "*"));

        assertFalse(BlackWhiteList.itemMatchesRule(null, "*"));
        assertFalse(BlackWhiteList.itemMatchesRule("toto", null));

        assertTrue(BlackWhiteList.itemMatchesRule("foo", "foo*"));
        assertTrue(BlackWhiteList.itemMatchesRule("foobar", "foo*"));
        assertFalse(BlackWhiteList.itemMatchesRule("barfoo", "foo*"));

        assertTrue(BlackWhiteList.itemMatchesRule("foo", "*foo"));
        assertFalse(BlackWhiteList.itemMatchesRule("foobar", "*foo"));
        assertTrue(BlackWhiteList.itemMatchesRule("barfoo", "*foo"));

        assertTrue(BlackWhiteList.itemMatchesRule("foo", "*foo*"));
        assertTrue(BlackWhiteList.itemMatchesRule("foobar", "*foo*"));
        assertTrue(BlackWhiteList.itemMatchesRule("barfoo", "*foo*"));
        assertFalse(BlackWhiteList.itemMatchesRule("bar", "*foo*"));
        assertFalse(BlackWhiteList.itemMatchesRule("fobaro", "*foo*"));

        assertFalse(BlackWhiteList.itemMatchesRule("foo", "foo*bar"));
        assertFalse(BlackWhiteList.itemMatchesRule("barfoo", "foo*bar"));
        assertTrue(BlackWhiteList.itemMatchesRule("fooobar", "foo*bar"));
        assertTrue(BlackWhiteList.itemMatchesRule("foobar", "foo*bar"));
        assertFalse(BlackWhiteList.itemMatchesRule("foobarbaz", "foo*bar"));
        assertFalse(BlackWhiteList.itemMatchesRule("bazfoobar", "foo*bar"));

        assertTrue(BlackWhiteList.itemMatchesRule("foobarbaz", "foo*bar*baz"));
        assertTrue(BlackWhiteList.itemMatchesRule("foo123bar123baz", "foo*bar*baz"));
        assertFalse(BlackWhiteList.itemMatchesRule("foo1bar1baz1", "foo*bar*baz"));
        assertFalse(BlackWhiteList.itemMatchesRule("1foo1bar1baz", "foo*bar*baz"));

        assertTrue(BlackWhiteList.itemMatchesRule("foo...totobar", "foo...*bar"));
        assertTrue(BlackWhiteList.itemMatchesRule("foo...bar", "foo...*bar"));
        assertFalse(BlackWhiteList.itemMatchesRule("fooabcbar", "foo...*bar"));
        assertFalse(BlackWhiteList.itemMatchesRule("foo...bar1", "foo...*bar"));
    }

    @Test
    public void testAllow() {
        BlackWhiteList blackWhiteList;

        // "*" in the white list disallows everything, except those in the whitelist
        blackWhiteList = new BlackWhiteList(new String[]{"-*", "@APP_OPEN", "@PRESENCE"});

        assertTrue(blackWhiteList.allow("@APP_OPEN"));
        assertTrue(blackWhiteList.allow("@PRESENCE"));
        assertFalse(blackWhiteList.allow("foo"));
        assertFalse(blackWhiteList.allow("bar"));

        // "*" in the white list allows everything, no exception
        blackWhiteList = new BlackWhiteList(new String[] {"*", "-@APP_OPEN", "-@PRESENCE"});

        assertTrue(blackWhiteList.allow("@APP_OPEN"));
        assertTrue(blackWhiteList.allow("@PRESENCE"));
        assertTrue(blackWhiteList.allow("foo"));
        assertTrue(blackWhiteList.allow("bar"));

        // some black, some white
        blackWhiteList = new BlackWhiteList(new String[] {"@PRESENCE", "-@APP_OPEN"});

        assertFalse(blackWhiteList.allow("@APP_OPEN"));
        assertTrue(blackWhiteList.allow("@PRESENCE"));
        assertTrue(blackWhiteList.allow("foo"));
        assertTrue(blackWhiteList.allow("bar"));

        // some black, some white
        blackWhiteList = new BlackWhiteList(new String[] {"sometoto", "-some*"});

        assertFalse(blackWhiteList.allow("sometiti"));
        assertTrue(blackWhiteList.allow("sometoto"));
        assertFalse(blackWhiteList.allow("some"));

    }
}
