package com.wonderpush.sdk;

import org.junit.Assert;
import org.junit.Test;

public class SimpleVersionTest {
    @Test
    public void testValid() {
        Assert.assertFalse(new SimpleVersion("toto").isValid());
        Assert.assertFalse(new SimpleVersion("vtoto").isValid());
        Assert.assertFalse(new SimpleVersion(".1").isValid());
        Assert.assertFalse(new SimpleVersion("1.").isValid());
        Assert.assertFalse(new SimpleVersion("v1.").isValid());
        Assert.assertFalse(new SimpleVersion("1.v2").isValid());

        Assert.assertTrue(new SimpleVersion("0").isValid());
        Assert.assertTrue(new SimpleVersion("1").isValid());
        Assert.assertTrue(new SimpleVersion("1234").isValid());
        Assert.assertTrue(new SimpleVersion("1.0").isValid());
        Assert.assertTrue(new SimpleVersion("0.1.0.0.0.0.0.1").isValid());
        Assert.assertTrue(new SimpleVersion("v0").isValid());
        Assert.assertTrue(new SimpleVersion("v1").isValid());
        Assert.assertTrue(new SimpleVersion("v1234").isValid());
        Assert.assertTrue(new SimpleVersion("v1.0").isValid());
        Assert.assertTrue(new SimpleVersion("v0.1.0.0.0.0.0.1").isValid());
    }

    @Test
    public void testCompare() {
        // Decrease
        Assert.assertEquals(new SimpleVersion("1.0").compareTo(new SimpleVersion("0.1")), 1);
        Assert.assertEquals(new SimpleVersion("2.0").compareTo(new SimpleVersion("1.0")), 1);
        Assert.assertEquals(new SimpleVersion("2.0.2").compareTo(new SimpleVersion("2.0.1")), 1);
        Assert.assertEquals(new SimpleVersion("2.0.1").compareTo(new SimpleVersion("2.0")), 1);

        // Increase
        Assert.assertEquals(new SimpleVersion("0.1").compareTo(new SimpleVersion("1.0")), -1);
        Assert.assertEquals(new SimpleVersion("1.0").compareTo(new SimpleVersion("2.0")), -1);
        Assert.assertEquals(new SimpleVersion("2.0.1").compareTo(new SimpleVersion("2.0.2")), -1);
        Assert.assertEquals(new SimpleVersion("2.0").compareTo(new SimpleVersion("2.0.1")), -1);

        // Equivalent
        Assert.assertEquals(new SimpleVersion("2.0.0").compareTo(new SimpleVersion("2.0")), 0);
        Assert.assertEquals(new SimpleVersion("2.0.0").compareTo(new SimpleVersion("2")), 0);
        Assert.assertEquals(new SimpleVersion("2.1.0").compareTo(new SimpleVersion("2.1")), 0);
        Assert.assertEquals(new SimpleVersion("1.0").compareTo(new SimpleVersion("1")), 0);
        Assert.assertEquals(new SimpleVersion("v2.0.0").compareTo(new SimpleVersion("2.0")), 0);
        Assert.assertEquals(new SimpleVersion("v2.0.0").compareTo(new SimpleVersion("2")), 0);
        Assert.assertEquals(new SimpleVersion("v2.1.0").compareTo(new SimpleVersion("2.1")), 0);
        Assert.assertEquals(new SimpleVersion("v1.0").compareTo(new SimpleVersion("1")), 0);
    }

    @Test
    public void testCompareInvalid() {
        Assert.assertEquals(new SimpleVersion("2.0").compareTo(new SimpleVersion("invalid")), 1);
        Assert.assertEquals(new SimpleVersion("invalid").compareTo(new SimpleVersion("2.0")), -1);
        Assert.assertEquals(new SimpleVersion("invalid").compareTo(new SimpleVersion("other")), 0);
    }

    @Test
    public void testToString() {
        Assert.assertEquals("1", new SimpleVersion("1.0").toString());
        Assert.assertEquals("1.0.1", new SimpleVersion("1.0.1").toString());
        Assert.assertEquals("1.1", new SimpleVersion("1.1.0").toString());
    }
}
