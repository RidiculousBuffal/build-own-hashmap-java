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

    public RedBlackNode(int hash, K key, V val, RedBlackNode<K, V> parent) {
        super(hash, key, val, null);
        this.parent = parent;
        this.red = true; // 新插入的节点为红色
    }

    public RedBlackNode<K, V> getParent() {
        return parent;
    }

    public RedBlackNode<K, V> getLeft() {
        return left;
    }

    public RedBlackNode<K, V> getRight() {
        return right;
    }

    public boolean isRed() {
        return red;
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

    /* ----------------- 删除相关 ---------By gpt5 mini ----------- */

    // 查找以 x 为根的最小节点
    private RedBlackNode<K, V> minimum(RedBlackNode<K, V> x) {
        if (x == null) return null;
        while (x.left != null) x = x.left;
        return x;
    }

    // 用 v 替换 u（transplant）：把 u 的位置替换为 v（v 可能为 null）
    private RedBlackNode<K, V> transplant(RedBlackNode<K, V> root, RedBlackNode<K, V> u, RedBlackNode<K, V> v) {
        if (u.parent == null) {
            root = v;
        } else if (u == u.parent.left) {
            u.parent.left = v;
        } else {
            u.parent.right = v;
        }
        if (v != null) v.parent = u.parent;
        return root;
    }

    /**
     * 删除节点 z（必须是树中的节点），返回新的 root
     * 调用流程：root = node.treeDelete(root, z);
     */
    public RedBlackNode<K, V> treeDelete(RedBlackNode<K, V> root, RedBlackNode<K, V> z) {
        if (z == null) return root; // nothing to do

        RedBlackNode<K, V> y = z; // 要么被删除的节点，要么要被移走并替换 z 的节点（y 的原始颜色需被记录）
        boolean yOriginalRed = y.red;

        RedBlackNode<K, V> x; // x 是被移走位置的节点（可能为 null），用于 deleteFixup

        if (z.left == null) {
            x = z.right;
            root = transplant(root, z, z.right);
        } else if (z.right == null) {
            x = z.left;
            root = transplant(root, z, z.left);
        } else {
            // z 有两个子节点：找到后继 y（z 的右子树最小），把 y 的值放到 z，然后删除 y（y 最多有右子）
            y = minimum(z.right);
            yOriginalRed = y.red;
            x = y.right;
            if (y.parent == z) {
                if (x != null) x.parent = y;
            } else {
                root = transplant(root, y, y.right);
                y.right = z.right;
                if (y.right != null) y.right.parent = y;
            }
            root = transplant(root, z, y);
            y.left = z.left;
            if (y.left != null) y.left.parent = y;
            y.red = z.red; // 用 y 取代 z，继承 z 的颜色
        }

        if (!yOriginalRed) {
            // 如果被删除或移走的节点原先为黑，则可能破坏黑深度，需要修复
            root = deleteFixup(root, x, /* parent of x if x==null? */ (x == null) ? null : x.parent);
        }

        if (root != null) setBlack(root);
        return root;
    }

    /**
     * 删除修复：x 是从被删除位置“替代”出来的节点（可能为 null）
     * parent 参数：当 x 为 null 时，无法通过 x.parent 访问其父节点，因此传入 parent 以便处理。
     * 返回可能更新后的 root。
     */
    private RedBlackNode<K, V> deleteFixup(RedBlackNode<K, V> root, RedBlackNode<K, V> x, RedBlackNode<K, V> parent) {
        // x 可能为 null；在循环中我们使用 w = sibling(x) 需要知道 parent
        while ((x == null || isBlack(x)) && parent != null) {
            if (parent.left == x) {
                RedBlackNode<K, V> w = parent.right; // 兄弟
                if (isRed(w)) {
                    // case 1: 兄弟为红
                    setBlack(w);
                    setRed(parent);
                    root = rotateLeft(root, parent);
                    // 更新 w
                    w = parent.right;
                }
                // 此时 w 为黑
                if ((w.left == null || isBlack(w.left)) && (w.right == null || isBlack(w.right))) {
                    // case 2: 兄弟的两个子都是黑
                    setRed(w);
                    x = parent;
                    parent = x.parent;
                } else {
                    if (w.right == null || isBlack(w.right)) {
                        // case 3: 兄弟右子为黑，左子为红 -> 先对 w 右旋
                        setBlack(w.left);
                        setRed(w);
                        root = rotateRight(root, w);
                        w = parent.right;
                    }
                    // case 4: 兄弟的右子为红
                    if (w != null) {
                        w.red = parent.red; // w 继承 parent 的颜色
                    }
                    setBlack(parent);
                    if (w != null && w.right != null) setBlack(w.right);
                    root = rotateLeft(root, parent);
                    x = root; // 修复完成后跳出循环（让外层把 root 设黑）
                    parent = null;
                    break;
                }
            } else {
                // parent.right == x
                RedBlackNode<K, V> w = parent.left; // 兄弟
                if (isRed(w)) {
                    // case 1 mirror
                    setBlack(w);
                    setRed(parent);
                    root = rotateRight(root, parent);
                    w = parent.left;
                }
                if ((w.left == null || isBlack(w.left)) && (w.right == null || isBlack(w.right))) {
                    // case 2 mirror
                    setRed(w);
                    x = parent;
                    parent = x.parent;
                } else {
                    if (w.left == null || isBlack(w.left)) {
                        // case 3 mirror: 兄弟左子为黑，右子为红 -> 先对 w 左旋
                        setBlack(w.right);
                        setRed(w);
                        root = rotateLeft(root, w);
                        w = parent.left;
                    }
                    // case 4 mirror
                    if (w != null) {
                        w.red = parent.red;
                    }
                    setBlack(parent);
                    if (w != null && w.left != null) setBlack(w.left);
                    root = rotateRight(root, parent);
                    x = root;
                    parent = null;
                    break;
                }
            }
        }
        // 最后把 x 设为黑（若 x 为 null 则 root 已在外部设黑）
        if (x != null) setBlack(x);
        return root;
    }

    /* ----------------- 额外工具方法 -------------------- */

    /**
     * 查找树中匹配 hash 和 key 的节点（以 root 为根）
     */
    public RedBlackNode<K, V> findNode(RedBlackNode<K, V> root, int h, Object k) {
        RedBlackNode<K, V> p = root;
        while (p != null) {
            int cmp = compareKeys((K) k, p.key, h, p.hash);
            if (cmp < 0) p = p.left;
            else if (cmp > 0) p = p.right;
            else {
                // compareKeys 返回 0 时应当等价于 equals
                return p;
            }
        }
        return null;
    }
}
