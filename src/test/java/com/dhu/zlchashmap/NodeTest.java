package com.dhu.zlchashmap;

import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void testGetters() {
        Node<String, Integer> n = new Node<>(123, "key", 456);
        assertEquals("key", n.getKey());
        assertEquals(456, n.getValue());
    }

    @Test
    void testSetValueThrows() {
        Node<String, Integer> n = new Node<>(1, "k", 2);
        assertThrows(UnsupportedOperationException.class, () -> n.setValue(100));
    }

    @Test
    void testHashCodeAndEquals_sameKeyValue() {
        Node<String, Integer> n1 = new Node<>(1, "k", 2);
        // 使用 AbstractMap.SimpleEntry 作为另一个 Map.Entry 实现比较
        Map.Entry<String, Integer> e = new AbstractMap.SimpleEntry<>("k", 2);

        assertEquals(n1, e);
        assertEquals(e, n1);
        assertEquals(n1.hashCode(), (ObjectsHash(n1.getKey(), n1.getValue())));
    }

    @Test
    void testEquals_different() {
        Node<String, Integer> n = new Node<>(1, "k", 2);
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("k", 3);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("other", 2);

        assertNotEquals(n, e1);
        assertNotEquals(n, e2);
    }

    @Test
    void testFindChain() {
        Node<String, Integer> n3 = new Node<>(3, "k3", 30);
        Node<String, Integer> n2 = new Node<>(2, "k2", 20, n3);
        Node<String, Integer> n1 = new Node<>(1, "k1", 10, n2);

        assertSame(n1.find(1, "k1"), n1);
        assertSame(n1.find(2, "k2"), n2);
        assertSame(n1.find(3, "k3"), n3);
        assertNull(n1.find(4, "k4"));
        assertNull(n1.find(2, "kX")); // 错的 key
    }

    // 辅助：和 Node.hashCode 的实现对应计算一个预期值
    private int ObjectsHash(Object k, Object v) {
        return (k == null ? 0 : k.hashCode()) ^ (v == null ? 0 : v.hashCode());
    }
}
