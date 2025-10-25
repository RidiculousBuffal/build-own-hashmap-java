package com.dhu.zlchashmap;

import java.util.Map;
import java.util.Objects;

public class Node<K, V> implements Map.Entry<K, V> {
    final int hash;
    final K key;
    volatile V val;
    volatile Node<K, V> next;

    public Node(int hash, K key, V val) {
        this.hash = hash;
        this.key = key;
        this.val = val;
    }

    public Node(int hash, K key, V val, Node<K, V> next) {
        this(hash, key, val);
        this.next = next;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return val;
    }

    @Override
    public V setValue(V value) {
        // 直接修改value 会破坏原子性,只能通过put 等方法修改
        throw new UnsupportedOperationException();
    }

    @Override
    public final int hashCode() {
        // 遵循原实现：key.hashCode() ^ val.hashCode()
        // 需要防 NPE，使用 Objects.requireNonNull 或 Objects.hashCode?
        return Objects.hashCode(key) ^ Objects.hashCode(val);
    }

    @Override
    public boolean equals(Object o) {
        Object k, v, u;
        Map.Entry<?, ?> e;
        return (
                (o instanceof Map.Entry) &&
                        (k = (e = (Map.Entry<?, ?>) o).getKey()) != null &&
                        (v = e.getValue()) != null &&
                        (k == key || k.equals(key)) && (v == (u = val) || v.equals(u))
        );
    }

    public Node<K, V> find(int h, Object k) {
        Node<K, V> e = this;
        while (e != null) {
            if (e.hash == h && Objects.equals(k, e.key)) {
                return e;
            } else {
                e = e.next;
            }
        }
        return null;
    }
}
