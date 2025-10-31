package com.dhu.zlchashmap;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RedBlackNode<K, V> extends Node<K, V> {
    /*
    *
    红黑树(前提是二叉搜索树):
    每个节点要么是红色，要么是黑色。
    根节点始终是黑色。
    所有叶子节点（null）视为黑色。
    如果一个节点是红色，那么它的两个子节点必须是黑色 (不存在两个连续的红色节点)。
    从任意节点到其所有叶子节点的路径上，黑色节点数量相同（黑高一致）。
    * */
    RedBlackNode<K, V> parent;
    RedBlackNode<K, V> left;
    RedBlackNode<K, V> right;
    boolean red; // 红 true 黑 false;

    RedBlackNode(int hash, K key, V val, RedBlackNode<K, V> parent) {
        super(hash, key, val, null);
        this.parent = parent;
        this.red = true; // 新插入的节点为红色
    }

    // 辅助方法
    private boolean isRed(RedBlackNode<K, V> n) {
        return n != null && n.red;
    }

    private boolean isBlack(RedBlackNode<K, V> n) {
        return n == null || !n.red;
    }

    private void setRed(RedBlackNode<K, V> n) {
        if (n != null) n.red = true;
    }

    private void setBlack(RedBlackNode<K, V> n) {
        if (n != null) n.red = false;
    }

    private void changeColor(RedBlackNode<K, V> n) {
        if (n != null) {
            n.red = !n.red;
        }
    }

    // 左旋  冲突左孩变右孩
    private RedBlackNode<K, V> rotateLeft(RedBlackNode<K, V> root, RedBlackNode<K, V> x) {
        if (x == null) return root;
        RedBlackNode<K, V> y = x.right;
        if (y == null) return root; // can't rotate

        x.right = y.left;
        if (y.left != null) y.left.parent = x;

        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else if (x == x.parent.left) {
            x.parent.left = y;
        } else {
            x.parent.right = y;
        }

        y.left = x;
        x.parent = y;
        return root;
    }

    // 右旋, 冲突右孩变左孩
    private RedBlackNode<K, V> rotateRight(RedBlackNode<K, V> root, RedBlackNode<K, V> x) {
        if (x == null) return root;
        RedBlackNode<K, V> y = x.left;
        if (y == null) return root;

        x.left = y.right;
        if (y.right != null) y.right.parent = x;

        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else if (x == x.parent.right) {
            x.parent.right = y;
        } else {
            x.parent.left = y;
        }
        y.right = x;
        x.parent = y;
        return root;
    }

    @SuppressWarnings("unchecked")
    private int compareKeys(K k1, K k2, int h1, int h2) {
        if (h1 != h2) {
            return h1 < h2 ? -1 : 1;
        }
        // hashes equal
        if (Objects.equals(k1, k2)) return 0;

        // 如果 key 可以比较并且类型相同，使用 Comparable
        if (k1 instanceof Comparable && k2 != null
                && k1.getClass() == k2.getClass()) {
            return ((Comparable) k1).compareTo(k2);
        }

        // 否则回退到一个稳定但任意的顺序：
        int s1 = System.identityHashCode(k1);
        int s2 = System.identityHashCode(k2);
        if (s1 != s2) return Integer.compare(s1, s2);

        // 极端回退：比较类名避免 identityHashCode 恰好相等时返回 0（尽量避免返回 0 除非 equals 为 true）
        String k1c = (k1 == null) ? "" : k1.getClass().getName();
        String k2c = (k2 == null) ? "" : k2.getClass().getName();
        return k1c.compareTo(k2c);
    }

    private RedBlackNode<K, V> insertFixup(RedBlackNode<K, V> root, RedBlackNode<K, V> x) {
        // x 是当前插入的节点,且为红色
        // 循环条件更稳健：x 非空且父节点存在且父节点为红
        while (x != null && isRed(x.parent)) {
            RedBlackNode<K, V> p = x.parent; // 父亲
            RedBlackNode<K, V> g = p.parent;//爷爷
            if (g == null) {
                break;
            }
            if (p == g.left) {
                RedBlackNode<K, V> uncle = g.right;
                /*
                 *             ⭕ g
                 *            /  \
                 *          ⭕ p  ⭕ uncle
                 * */
                if (isRed(uncle)) {
                    // case 1: 叔为红 -> 父叔置黑, 祖父置红, x 上移为 g
                    setBlack(p);
                    setBlack(uncle);
                    setRed(g);
                    x = g;
                } else {
                    // 叔叔为黑色 需要旋转
                    if (x == p.right) {
                        /*
                         * LR 情形：先对父作左旋，转为 LL 情形
                         */
                        root = rotateLeft(root, p);
                        // 将当前检查点移回原父（现在 p 已在 x 的下面），以便下步作为 LL 情形处理
                        x = p;
                        p = x.parent;
                    }
                    // LL 情形：父置黑，祖父置红，右旋祖父
                    setBlack(p);
                    setRed(g);
                    root = rotateRight(root, g);
                    // 旋转并重新着色后，局部性质已恢复，循环可以结束
                    break;
                }

            } else {
                RedBlackNode<K, V> uncle = g.left;
                /*
                 *             ⭕ g
                 *            /  \
                 *          ⭕ uncle  ⭕ p
                 * */
                if (isRed(uncle)) {
                    // case 1 mirror
                    setBlack(p);
                    setBlack(uncle);
                    setRed(g);
                    x = g;
                } else {
                    if (x == p.left) {
                        // RL -> 右旋父，变为 RR 情形
                        root = rotateRight(root, p);
                        x = p;
                        p = x.parent;
                    }
                    // RR 情形：父置黑，祖父置红，左旋祖父
                    setBlack(p);
                    setRed(g);
                    root = rotateLeft(root, g);
                    break;
                }
            }
        }
        if (root != null) setBlack(root);
        return root;
    }

    /**
     * 非递归的 BST 插入：按 compareKeys 决定左右，若键相等则替换值并不插入新节点
     * 返回（可能未改变的）树根
     */
    private RedBlackNode<K, V> bstInsert(RedBlackNode<K, V> root, RedBlackNode<K, V> newNode) {
        if (root == null) {
            newNode.parent = null;
            return newNode;
        }
        RedBlackNode<K, V> t = root;
        RedBlackNode<K, V> parent = null;
        int cmp = 0;
        while (t != null) {
            parent = t;
            cmp = compareKeys(newNode.key, t.key, newNode.hash, t.hash);
            if (cmp < 0) {
                t = t.left;
            } else if (cmp > 0) {
                t = t.right;
            } else {
                // 相等：替换值并返回原 root（不插入新节点）
                t.val = newNode.val;
                return root;
            }
        }
        newNode.parent = parent;
        if (cmp < 0) parent.left = newNode;
        else parent.right = newNode;
        return root;
    }

    /**
     * 插入新节点并平衡（红黑修复）
     * 注意：调用者应确保 newNode.left/right = null（通常新节点是叶子）
     */
    public RedBlackNode<K, V> insertNewNodeWithBalance(RedBlackNode<K, V> root, RedBlackNode<K, V> newNode) {
        // 新节点默认为红
        newNode.red = true;
        newNode.left = null;
        newNode.right = null;

        if (root == null) {
            // 新节点直接成为根，根必须为黑
            newNode.parent = null;
            newNode.red = false;
            return newNode;
        }

        root = bstInsert(root, newNode);
        // 如果 bstInsert 因键相等直接返回（替换值），不需要再修复颜色
        // 但 bstInsert 返回后 newNode.parent 可能为 null（如果没有插入），判断是否需要修复
        if (newNode.parent == null && root != newNode) {
            // newNode 没有插入，直接返回 root
            return root;
        }

        root = insertFixup(root, newNode);
        return root;
    }
}
