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
    static int tableSizeFor(int cap) {
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
    @SuppressWarnings("unchecked")
    final void resize() {
        Node<K, V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;

        // 1. 计算新容量和新阈值
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }
            if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_CAPACITY) {
                newThr = oldThr << 1; // 阈值也翻倍
            }
        } else if (oldThr > 0) {
            newCap = oldThr;
        } else {
            newCap = DEFAULT_CAPACITY;
            newThr = (int)(DEFAULT_CAPACITY * LOAD_FACTOR);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                    (int)ft : Integer.MAX_VALUE);
        }

        threshold = newThr;
        this.capacity = newCap;
        Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
        table = newTab;

        // 2. 将旧表中的元素移动到新表
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K, V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null; // 帮助 GC

                    if (e.next == null) {
                        // Case 1: 桶中只有一个节点
                        newTab[e.hash & (newCap - 1)] = e;
                    } else if (e instanceof RedBlackNode) {
                        // Case 2: 桶是红黑树
                        RedBlackNode<K, V> tree = (RedBlackNode<K, V>) e;
                        Node<K,V>[] lists = tree.split(oldCap); // 拆分成两个链表

                        Node<K,V> loHead = lists[0];
                        Node<K,V> hiHead = lists[1];

                        // 处理低位链表
                        if (loHead != null) {
                            int lc = 0; // low count
                            for (Node<K,V> p = loHead; p != null; p = p.next) lc++;
                            if (lc <= UNTREEIFY_THRESHOLD) {
                                newTab[j] = loHead; // 保持为链表
                            } else {
                                newTab[j] = treeify(loHead); // 重新树化
                            }
                        }

                        // 处理高位链表
                        if (hiHead != null) {
                            int hc = 0; // high count
                            for (Node<K,V> p = hiHead; p != null; p = p.next) hc++;
                            if (hc <= UNTREEIFY_THRESHOLD) {
                                newTab[j + oldCap] = hiHead; // 保持为链表
                            } else {
                                newTab[j + oldCap] = treeify(hiHead); // 重新树化
                            }
                        }
                    } else {
                        // Case 3: 桶是普通链表
                        Node<K, V> loHead = null, loTail = null;
                        Node<K, V> hiHead = null, hiTail = null;
                        Node<K, V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) { // 低位
                                if (loTail == null) loHead = e; else loTail.next = e;
                                loTail = e;
                            } else { // 高位
                                if (hiTail == null) hiHead = e; else hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);

                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
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
            // 4. 若非空, 处理冲突
            Node<K, V> p = table[index];
            if (p instanceof RedBlackNode<K, V> treeNode) { // 检查是否为树节点
                RedBlackNode<K, V> targetNode = treeNode.findNode(treeNode, hash(key), key);
                if (targetNode != null) {
                    oldValue = targetNode.val;
                    targetNode.val = value; // 仅替换值
                    return oldValue;
                }
                // 插入新节点到树中
                treeNode = treeNode.insertNewNodeWithBalance(treeNode, new RedBlackNode<>(hash(key), key, value, null));
                table[index] = treeNode; // 根节点可能改变，需要更新
                size++;

            } else { // 是链表
                Node<K, V> head = p;
                Node<K, V> prev = null;
                boolean replaced = false;
                int binCount = 0; // 计算链表长度
                while (head != null) {
                    binCount++;
                    if (Objects.equals(head.key, key) && head.hash == hash(key)) {
                        oldValue = head.val;
                        head.val = value;
                        replaced = true;
                        break;
                    }
                    prev = head;
                    head = head.next;
                }
                if (!replaced) {
                    if (prev != null) {
                        prev.next = new Node<>(hash(key), key, value);
                    }
                    size++;
                    // 检查是否需要树化
                    if (binCount + 1 >= TREEIFY_THRESHOLD) {
                        treeifyBin(table, index);
                    }
                }
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
        if (head instanceof RedBlackNode) {
            RedBlackNode<K, V> tree = (RedBlackNode<K, V>) head;
            RedBlackNode<K, V> targetNode = tree.findNode(tree, hash(key), key);
            if (targetNode == null) {
                return null;
            }
            V oldValue = targetNode.val;
            // 调用树的删除方法，它会返回新的根
            RedBlackNode<K, V> newRoot = tree.treeDelete(tree, targetNode);
            table[index] = newRoot;
            size--;
            // 检查是否需要反树化
            if (newRoot != null && newRoot.countNodes(newRoot, 0) <= UNTREEIFY_THRESHOLD) {
                table[index] = untreeifyBin(newRoot);
            }
            return oldValue;
        } else {
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
                    if (table[index] != null && size <= UNTREEIFY_THRESHOLD && table[index] instanceof RedBlackNode<K, V>) {
                        // 树转链表
                        table[index] = untreeifyBin((RedBlackNode<K, V>) table[index]);
                    }
                    return val;
                }
                prev = head;
                head = head.next;
            }
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

    private void treeifyBin(Node<K, V>[] tab, int index) {
        if (tab == null || tab.length < MIN_TREEIFY_CAPACITY) {
            resize();
            return;
        }
        Node<K, V> e = tab[index];
        if (e != null) {
            // 调用统一的 treeify 方法完成转换
            RedBlackNode<K, V> root = treeify(e);
            // 将桶的头节点替换为新的树根
            tab[index] = root;
        }
    }

    private Node<K, V> untreeifyBin(RedBlackNode<K, V> root) {
        if (root == null) return null;
        Node<K, V> head = null, tail = null;
        java.util.Deque<RedBlackNode<K, V>> stack = new java.util.ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            RedBlackNode<K, V> n = stack.pop();
            Node<K, V> newNode = new Node<>(n.hash, n.key, n.val, null);
            if (tail == null) head = newNode;
            else tail.next = newNode;
            tail = newNode;
            if (n.right != null) stack.push(n.right);
            if (n.left != null) stack.push(n.left);
        }
        return head;
    }
    final RedBlackNode<K,V> treeify(Node<K,V> head) {
        RedBlackNode<K,V> root = null;
        Node<K,V> current = head;
        while (current != null) {
            // 将 current 节点包装成 RedBlackNode
            RedBlackNode<K,V> newNode = new RedBlackNode<>(current.hash, current.key, current.val, null);

            // 插入到新树中并保持平衡
            if (root == null) {
                newNode.red = false; // 根节点是黑色
                root = newNode;
            } else {
                root = root.insertNewNodeWithBalance(root, newNode);
            }

            // 处理链表中的下一个节点
            current = current.next;
        }
        return root;
    }
}
