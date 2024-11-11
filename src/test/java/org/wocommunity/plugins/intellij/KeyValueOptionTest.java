package org.wocommunity.plugins.intellij;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class KeyValueOptionTest {
    @Test
    public void equalsTest()
    {
        KeyValueOption kvo1 = new KeyValueOption(true, "key", "value");
        KeyValueOption kvo2 = new KeyValueOption(true, "key", "value");

        assertTrue(kvo1.equals(kvo2));
    }

    @Test
    public void notEqualsTest()
    {
        KeyValueOption kvo1 = new KeyValueOption(true, "key", "value1");
        KeyValueOption kvo2 = new KeyValueOption(true, "key", "value2");

        assertFalse(kvo1.equals(kvo2));
    }
}