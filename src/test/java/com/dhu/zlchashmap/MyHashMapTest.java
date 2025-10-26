package com.dhu.zlchashmap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MyHashMapTest {
    MyHashMap<String, Integer> map;

    @BeforeEach
    void setUp() {
        map = new MyHashMap<>();
    }

    @Test
    void testPutAndGetBasic() {
        assertNull(map.get("a"));
        Integer prev = map.put("a", 1);
        assertNull(prev);
        assertEquals(1, map.get("a"));
        assertTrue(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    void testUpdateValueDoesNotIncreaseSize() {
        map.put("k", 10);
        assertEquals(1, map.size());
        Integer old = map.put("k", 20);
        assertEquals(10, old);
        assertEquals(1, map.size(), "Updating existing key should not increase size");
        assertEquals(20, map.get("k"));
    }

    @Test
    void testRemoveHeadAndMiddle() {
        map.put("h", 1);
        map.put("m", 2);
        map.put("t", 3);
        assertEquals(3, map.size());

        // remove head (depending on hash bucket, but ensure we attempt to remove an element that could be head)
        Integer removed = map.remove("h");
        assertEquals(Integer.valueOf(1), removed);
        assertNull(map.get("h"));
        assertEquals(2, map.size());

        // remove middle or tail
        Integer removed2 = map.remove("m");
        assertEquals(Integer.valueOf(2), removed2);
        assertNull(map.get("m"));
        assertEquals(1, map.size());
    }

    @Test
    void testContainsKeyWhenValueIsNull() {
        MyHashMap<String, Integer> m2 = new MyHashMap<>();
        m2.put("nullVal", null);
        // containsKey should be true even if value is null
        assertTrue(m2.containsKey("nullVal"));
        assertNull(m2.get("nullVal"));
    }

    @Test
    void testResizeTriggeredAndPreservesMappings() {
        // fill map to exceed load factor and cause resize
        MyHashMap<Integer, String> m = new MyHashMap<>(4, 0.75f);
        int initialCapacity = 4;
        int itemsToInsert = (int) (initialCapacity * 0.75f) + 2; // ensure resize
        for (int i = 0; i < itemsToInsert; i++) {
            m.put(i, "v" + i);
        }
        // all inserted keys should be retrievable
        for (int i = 0; i < itemsToInsert; i++) {
            assertEquals("v" + i, m.get(i), "Value lost after resize for key " + i);
        }
        assertEquals(itemsToInsert, m.size());
    }

    @Test
    void testGetOnNullTableShouldReturnNull() {
        // before any put, table may be null; get should not NPE
        assertNull(map.get("nonexistent"));
        assertNull(map.remove("nonexistent"));
    }

    @Test
    void testKeyWithNullKeyAndNullValueSupport() {
        MyHashMap<String, String> m = new MyHashMap<>();
        m.put(null, "nv");
        assertEquals("nv", m.get(null));
        m.put("k", null);
        assertTrue(m.containsKey("k"), "should contain key even if value is null");
        assertNull(m.get("k"));
    }
}
