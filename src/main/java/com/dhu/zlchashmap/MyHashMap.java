package com.dhu.zlchashmap;

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
    int threshold;

    public MyHashMap() {
        this.loadFactor = LOAD_FACTOR;
        this.threshold = DEFAULT_CAPACITY;
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
        this.threshold = tableSizeFor(initialCapacity);
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
        int index = calculateIndex(key, table.length);
        Node<K, V> head = table[index];
        Node<K, V> kvNode = head.find(hash(key), key);
        return kvNode == null ? null : kvNode.val;

    }

    //保证容量是2的幂次
    static final int tableSizeFor(int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    protected int calculateIndex(Object key, int length) {
        int hash = hash(key);
        return (length - 1) & hash;
    }

    //扩容表,容量翻倍
    final void resize() {
        //保存旧的引用
        Node<K, V>[] oldTable = this.table;
        //新容量 = 旧容量*2;
        int newSize = oldTable.length << 1;
        // 新建newTable 并遍历旧的表
        Node<K, V>[] newTable = new Node[newSize];
        for (int i = 0; i < oldTable.length; i++) {
            Node<K, V> current = oldTable[i];
            // 对于空,跳过
            if (current == null) {
                continue;
            } else if (current.next == null) {
                // 只有一个元素,重新计算index;
                int index = calculateIndex(current.key, newSize);
                newTable[index] = current;
            } else {
                // 若是链表,则按照 hash & oldCap 判断是高位还是低位
                Node<K, V> loHead = null, loTail = null;
                Node<K, V> hiHead = null, hiTail = null;
                while (current != null) {
                    boolean isHigh = (current.hash & oldTable.length) == 1;
                    if (isHigh) {
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
                    current = current.next;
                }
                if (loTail != null) {
                    loTail.next = null;
                }
                if (hiTail != null) {
                    hiTail.next = null;
                }
                newTable[i] = loHead;
                newTable[i + oldTable.length] = hiHead;
            }
        }
        this.table = newTable;// 替换为新表
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        V oldValue = null;
        //1. 检查table是否为空,为空则初始化
        if (isEmpty()) {
            table = (Node<K, V>[]) new Node[threshold];
        }
        //2. 计算hash并且定位index;
        int index = calculateIndex(key, table.length);
        //3. 若桶为空,则直接插入新节点
        if (isEmpty()) {
            Node<K, V> node = new Node<K, V>(hash(key), key, value);
            table[index] = node;
        } else {
            //4. 若非空,插入链表末尾
            Node<K, V> head = table[calculateIndex(key, table.length)];
            while (head != null) {
                if (head.key.equals(key)) {
                    oldValue = head.val;
                    head.val = value;
                    break;
                }
                if (head.next == null) {
                    head.next = new Node<K, V>(hash(key), key, value);
                }
                head = head.next;
            }
        }

        size++;
        if (size > threshold * loadFactor) {
            resize();
        }
        return oldValue;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public V remove(Object key) {
        if (isEmpty()) {
            return null;
        }
        // 定位index;
        int index = calculateIndex(key, table.length);
        Node<K, V> head = table[index];
        while (head != null) {
//             找到节点,摘链
            if (head.next != null && head.next.key.equals(key)) {
                Node<K, V> next = head.next;
                head.next = next.next;
                size--;
                return next.val;
            }
            head = head.next;
        }
        return null;
    }

    public int size() {
        return size;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }
}
