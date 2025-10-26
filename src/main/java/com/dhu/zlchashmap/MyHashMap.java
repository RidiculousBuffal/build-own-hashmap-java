package com.dhu.zlchashmap;

import java.util.Objects;

public class MyHashMap<K, V> {
    static final int DEFAULT_CAPACITY = 16; //默认初始容量
    static final int MAXIMUM_CAPACITY = 1 << 30; // 最大表容量
    static final float LOAD_FACTOR = 0.75f; // 负载因子0.75
    static final int TREEIFY_THRESHOLD = 8;//链表转树
    static final int UNTREEIFY_THRESHOLD = 6;//树转链表
    static final int MIN_TREEIFY_CAPACITY = 64; //树化最小容量

    volatile Node<K, V>[] table;//长度为2的幂
    int size;//当前键值对数量
    float loadFactor;//当前负载因子
    int threshold; // 修正：threshold 表示触发扩容的元素数量（capacity * loadFactor）
    int capacity;  // 修正：显式记录当前容量（table.length）

    public MyHashMap() {
        this.loadFactor = LOAD_FACTOR;
        this.capacity = DEFAULT_CAPACITY;
        // 修正：threshold 应为 capacity * loadFactor，而不是直接设为 DEFAULT_CAPACITY
        this.threshold = (int) (this.capacity * this.loadFactor);
    }

    public MyHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                    loadFactor);
        this.loadFactor = loadFactor;
        this.capacity = tableSizeFor(initialCapacity);
        // 修正：threshold = capacity * loadFactor
        this.threshold = (int) (this.capacity * this.loadFactor);
    }

    static final int hash(Object key) {
        /*
        假设 h = 0x12345678（二进制 0001 0010 0011 0100 0101 0110 0111 1000）
        h >>> 16 = 0x00001234（二进制 0000 0000 0000 0001 0010 0011 0100）
        h ^ (h >>> 16) = 0x1234444C（按位异或后的结果，示意）
        * */
        if (key == null) {
            return 0;
        } else {
            int h = key.hashCode();
            int shifted = h >>> 16; //>>> 是无符号右移（logical right shift）运算符。它把操作数的二进制位向右移动指定的位数，并在左侧用 0 填充（不保留符号位）
            int mixed = h ^ shifted;
            return mixed;
        }
    }

    public V get(Object key) {
        // 修正：table 可能为 null（还未初始化），要先检查
        if (table == null || size == 0) {
            return null;
        }
        int index = calculateIndex(key, table.length);
        Node<K, V> head = table[index];
        if (head == null) return null;
        Node<K, V> kvNode = head.find(hash(key), key);
        return kvNode == null ? null : kvNode.val;
    }

    //保证容量是2的幂次
    static final int tableSizeFor(int cap) {
        // 修正：使用标准方法得到 >= cap 的最小 2 的幂
        int n = cap - 1;
        n |= n >>> 1; //n |= n >>> 1 等价于  n = n | (n >>> 1);
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        n = (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
        return n;
    }

    protected int calculateIndex(Object key, int length) {
        int hash = hash(key);
        return (length - 1) & hash;
    }

    //扩容表,容量翻倍
    final void resize() {
        // 保存旧的引用
        Node<K, V>[] oldTable = this.table;
        int oldCap = (oldTable == null) ? 0 : oldTable.length;
        int newSize;
        if (oldCap == 0) {
            // 初次扩容/初始化
            newSize = this.capacity;
            // 如果 table 还未创建，根据 capacity 创建
            Node<K, V>[] newTable = new Node[newSize];
            this.table = newTable;
            return;
        } else {
            // 新容量 = 旧容量*2;
            newSize = oldCap << 1;
            if (newSize > MAXIMUM_CAPACITY) {
                newSize = MAXIMUM_CAPACITY;
            }
        }
        // 新建newTable 并遍历旧的表
        @SuppressWarnings("unchecked")
        Node<K, V>[] newTable = new Node[newSize];
        for (int i = 0; i < oldCap; i++) {
            Node<K, V> current = oldTable[i];
            // 对于空,跳过
            if (current == null) {
                continue;
            } else if (current.next == null) {
                // 只有一个元素,重新计算index;
                int index = calculateIndex(current.key, newSize);
                // 修正：确保断开 old next 指向（虽然 current.next==null 时通常已经是 null）
                current.next = null;
                newTable[index] = current;
            } else {
                // 若是链表,则按照 hash & oldCap 判断是高位还是低位
                Node<K, V> loHead = null, loTail = null;
                Node<K, V> hiHead = null, hiTail = null;
                Node<K, V> next;
                while (current != null) {
                    next = current.next;
                    // 修正：判断条件应是 (current.hash & oldCap) != 0
                    // 旧实现 (current.hash & oldTable.length) == 1 是错误的比较
                    if ((current.hash & oldCap) != 0) {
                        if (hiTail == null) {
                            hiHead = current;
                        } else {
                            hiTail.next = current;
                        }
                        hiTail = current;
                    } else {
                        if (loTail == null) {
                            loHead = current;
                        } else {
                            loTail.next = current;
                        }
                        loTail = current;
                    }
                    current = next;
                }
                if (loTail != null) {
                    loTail.next = null;
                }
                if (hiTail != null) {
                    hiTail.next = null;
                }
                newTable[i] = loHead;
                newTable[i + oldCap] = hiHead;
            }
        }
        this.table = newTable;// 替换为新表
        this.capacity = newSize;
        // 修正：更新 threshold = capacity * loadFactor
        this.threshold = (int) (this.capacity * this.loadFactor);
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        V oldValue = null;
        //1. 检查table是否为空,为空则初始化
        if (table == null) {
            // 修正：表在首次 put 时初始化，使用 capacity 字段作为初始容量
            table = (Node<K, V>[]) new Node[this.capacity];
        }
        //2. 计算hash并且定位index;
        int index = calculateIndex(key, table.length);
        //3. 若桶为空,则直接插入新节点
        if (table[index] == null) { // 修正：这里应判断具体桶是否为空，而不是 isEmpty()
            Node<K, V> node = new Node<K, V>(hash(key), key, value);
            table[index] = node;
            size++;
        } else {
            //4. 若非空,插入链表末尾或替换已有 key
            Node<K, V> head = table[index];
            Node<K, V> prev = null;
            boolean replaced = false;
            while (head != null) {
                if (Objects.equals(head.key, key)) { // 修正：使用 Objects.equals 避免 NPE
                    oldValue = head.val;
                    head.val = value;
                    replaced = true;
                    break;
                }
                prev = head;
                head = head.next;
            }
            if (!replaced) {
                // prev 此时为链表尾
                if (prev != null) {
                    prev.next = new Node<K, V>(hash(key), key, value);
                }
                size++;
            }
        }

        // 修正：扩容判断应该基于 size 与 threshold（threshold 已为 capacity * loadFactor）
        if (size > threshold) {
            resize();
        }
        return oldValue;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public V remove(Object key) {
        if (isEmpty() || table == null) {
            return null;
        }
        // 定位index;
        int index = calculateIndex(key, table.length);
        Node<K, V> head = table[index];
        Node<K, V> prev = null;
        while (head != null) {
            if (Objects.equals(head.key, key)) {
                // 找到节点,摘链
                V val = head.val;
                if (prev == null) {
                    // 修正：处理头节点被删除的情况
                    table[index] = head.next;
                } else {
                    prev.next = head.next;
                }
                size--;
                return val;
            }
            prev = head;
            head = head.next;
        }
        return null;
    }

    public int size() {
        return size;
    }

    public boolean containsKey(Object key) {
        // 修正：仅判断 get(key) != null 会把 value 为 null 的键判为不存在
        if (table == null || size == 0) return false;
        int index = calculateIndex(key, table.length);
        Node<K, V> head = table[index];
        if (head == null) return false;
        return head.find(hash(key), key) != null;
    }
}
