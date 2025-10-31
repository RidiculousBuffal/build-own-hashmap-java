import com.dhu.zlchashmap.RedBlackNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class RedBlackNodeTest {

    // 辅助方法：检查红黑树属性
    private boolean isValidRedBlackTree(RedBlackNode<Integer, String> root) {
        if (root == null) {
            return true; // 空树视为有效
        }
        if (root.isRed()) {
            return false; // 根必须是黑色
        }
        return getBlackHeightIfValid(root) != -1;
    }

    private int getBlackHeightIfValid(RedBlackNode<Integer, String> node) {
        if (node == null) {
            return 0; // null 节点黑高为 0
        }
        // 检查红色节点是否具有红色子节点
        if (node.isRed() && ((node.getLeft() != null && node.getLeft().isRed()) || (node.getRight() != null && node.getRight().isRed()))) {
            return -1; // 无效，存在红-红边
        }
        int leftBH = getBlackHeightIfValid(node.getLeft());
        if (leftBH == -1) {
            return -1; // 左子树无效
        }
        int rightBH = getBlackHeightIfValid(node.getRight());
        if (rightBH == -1) {
            return -1; // 右子树无效
        }
        if (leftBH != rightBH) {
            return -1; // 黑高不一致
        }
        // 计算当前节点的黑高
        return (node.isRed() ? 0 : 1) + leftBH;
    }

    // 辅助方法：检查二叉搜索树属性
    private boolean isBST(RedBlackNode<Integer, String> root) {
        return isBSTUtil(root, null, null);
    }

    private boolean isBSTUtil(RedBlackNode<Integer, String> node, Integer min, Integer max) {
        if (node == null) {
            return true;
        }
        int val = node.getKey(); // 使用键值进行比较
        // 检查当前节点是否在 min 和 max 范围内（严格不等，因为无重复键）
        if ((min != null && node.getKey().compareTo(min) <= 0) || (max != null && node.getKey().compareTo(max) >= 0)) {
            return false;
        }
        // 递归检查左子树和右子树
        return isBSTUtil(node.getLeft(), min, node.getKey()) && isBSTUtil(node.getRight(), node.getKey(), max);
    }

    // 虚拟节点用于调用实例方法（因为 insertNewNodeWithBalance 和 treeDelete 不依赖 this）
    private RedBlackNode<Integer, String> dummyNode = new RedBlackNode<>(0, 0, "dummy", null);

    @Test
    public void testSingleInsertion() {
        // 测试插入单个节点
        RedBlackNode<Integer, String> newNode = new RedBlackNode<>(10, 10, "ten", null);
        RedBlackNode<Integer, String> root = dummyNode.insertNewNodeWithBalance(null, newNode);

        assertNotNull(root);
        assertFalse(root.isRed()); // 根必须是黑色
        assertEquals(10, root.getKey().intValue());
        assertNull(root.getParent());
        assertNull(root.getLeft());
        assertNull(root.getRight());
        assertTrue(isValidRedBlackTree(root)); // 检查红黑树属性
        assertTrue(isBST(root)); // 检查 BST 属性
    }

    @Test
    public void testInsertTwoNodes() {
        // 测试插入两个节点（10 和 20），预期无旋转，20 为红色
        RedBlackNode<Integer, String> node10 = new RedBlackNode<>(10, 10, "ten", null);
        RedBlackNode<Integer, String> root = dummyNode.insertNewNodeWithBalance(null, node10);

        RedBlackNode<Integer, String> node20 = new RedBlackNode<>(20, 20, "twenty", null);
        root = dummyNode.insertNewNodeWithBalance(root, node20);

        assertEquals(10, root.getKey().intValue());
        assertFalse(root.isRed()); // 根 10 黑色
        assertNull(root.getLeft());
        assertNotNull(root.getRight());
        assertEquals(20, root.getRight().getKey().intValue());
        assertTrue(root.getRight().isRed()); // 20 应为红色
        assertSame(root, root.getRight().getParent());
        assertTrue(isValidRedBlackTree(root));
        assertTrue(isBST(root));

        // 测试 findNode
        RedBlackNode<Integer, String> found = root.findNode(root, 20, 20);
        assertNotNull(found);
        assertEquals(20, found.getKey().intValue());

        found = root.findNode(root, 10, 10);
        assertSame(root, found);
    }

    @Test
    public void testInsertWithRotation() {
        // 测试插入顺序导致旋转：插入 30、20、10，预期触发左-左旋转，根变为 20 黑色
        RedBlackNode<Integer, String> node30 = new RedBlackNode<>(30, 30, "thirty", null);
        RedBlackNode<Integer, String> root = dummyNode.insertNewNodeWithBalance(null, node30);

        RedBlackNode<Integer, String> node20 = new RedBlackNode<>(20, 20, "twenty", null);
        root = dummyNode.insertNewNodeWithBalance(root, node20);

        RedBlackNode<Integer, String> node10 = new RedBlackNode<>(10, 10, "ten", null);
        root = dummyNode.insertNewNodeWithBalance(root, node10);

        assertEquals(20, root.getKey().intValue());
        assertFalse(root.isRed()); // 根 20 黑色
        assertNotNull(root.getLeft());
        assertEquals(10, root.getLeft().getKey().intValue());
        assertTrue(root.getLeft().isRed()); // 10 应为红色
        assertNotNull(root.getRight());
        assertEquals(30, root.getRight().getKey().intValue());
        assertTrue(root.getRight().isRed()); // 30 应为红色
        assertTrue(isValidRedBlackTree(root));
        assertTrue(isBST(root));
    }

    @Test
    public void testInsertEqualKey() {
        // 测试插入相同键时替换值，不插入新节点
        RedBlackNode<Integer, String> node10First = new RedBlackNode<>(10, 10, "first", null);
        RedBlackNode<Integer, String> root = dummyNode.insertNewNodeWithBalance(null, node10First);

        RedBlackNode<Integer, String> node10Second = new RedBlackNode<>(10, 10, "second", null);
        root = dummyNode.insertNewNodeWithBalance(root, node10Second);

        assertSame(node10First, root); // 同一个节点对象
        assertEquals("second", root.getValue()); // 值被替换
        assertNull(root.getLeft());
        assertNull(root.getRight());
        assertTrue(isValidRedBlackTree(root));
        assertTrue(isBST(root));
    }

    @Test
    public void testDeleteLeafNode() {
        // 测试删除叶子节点：先插入 20、10、30，然后删除 10（红色叶子）
        RedBlackNode<Integer, String> node20 = new RedBlackNode<>(20, 20, "twenty", null);
        RedBlackNode<Integer, String> root = dummyNode.insertNewNodeWithBalance(null, node20);

        RedBlackNode<Integer, String> node10 = new RedBlackNode<>(10, 10, "ten", null);
        root = dummyNode.insertNewNodeWithBalance(root, node10);

        RedBlackNode<Integer, String> node30 = new RedBlackNode<>(30, 30, "thirty", null);
        root = dummyNode.insertNewNodeWithBalance(root, node30);

        // 查找要删除的节点
        RedBlackNode<Integer, String> nodeToDelete = root.findNode(root, 10, 10);
        assertNotNull(nodeToDelete);
        root = dummyNode.treeDelete(root, nodeToDelete); // 删除节点

        assertEquals(20, root.getKey().intValue());
        assertFalse(root.isRed()); // 根 20 黑色
        assertNull(root.getLeft()); // 左子树为空
        assertNotNull(root.getRight());
        assertEquals(30, root.getRight().getKey().intValue());
        assertTrue(root.getRight().isRed()); // 30 应为红色
        assertTrue(isValidRedBlackTree(root));
        assertTrue(isBST(root));
    }

    @Test
    public void testDeleteRootNode() {
        // 测试删除根节点：插入 10、5、15，然后删除 10
        RedBlackNode<Integer, String> node10 = new RedBlackNode<>(10, 10, "ten", null);
        RedBlackNode<Integer, String> root = dummyNode.insertNewNodeWithBalance(null, node10);

        RedBlackNode<Integer, String> node5 = new RedBlackNode<>(5, 5, "five", null);
        root = dummyNode.insertNewNodeWithBalance(root, node5);

        RedBlackNode<Integer, String> node15 = new RedBlackNode<>(15, 15, "fifteen", null);
        root = dummyNode.insertNewNodeWithBalance(root, node15);

        // 查找要删除的节点（根节点 10）
        RedBlackNode<Integer, String> nodeToDelete = root.findNode(root, 10, 10);
        assertNotNull(nodeToDelete);
        root = dummyNode.treeDelete(root, nodeToDelete); // 删除根节点

        // 预期新根为 15 或 5，取决于删除修复，但树应保持有效
        assertNotNull(root);
        assertFalse(root.isRed()); // 新根必须黑色
        assertTrue(isValidRedBlackTree(root));
        assertTrue(isBST(root));
    }

    @Test
    public void testDeleteWithFixup() {
        // 测试删除可能触发修复的节点：插入更多节点，确保黑高一致
        // 插入 10, 5, 15, 3, 7, 12, 18
        RedBlackNode<Integer, String> root = dummyNode.insertNewNodeWithBalance(null, new RedBlackNode<>(10, 10, "ten", null));
        root = dummyNode.insertNewNodeWithBalance(root, new RedBlackNode<>(5, 5, "five", null));
        root = dummyNode.insertNewNodeWithBalance(root, new RedBlackNode<>(15, 15, "fifteen", null));
        root = dummyNode.insertNewNodeWithBalance(root, new RedBlackNode<>(3, 3, "three", null));
        root = dummyNode.insertNewNodeWithBalance(root, new RedBlackNode<>(7, 7, "seven", null));
        root = dummyNode.insertNewNodeWithBalance(root, new RedBlackNode<>(12, 12, "twelve", null));
        root = dummyNode.insertNewNodeWithBalance(root, new RedBlackNode<>(18, 18, "eighteen", null));

        // 删除一个节点，例如 10（可能触发删除修复）
        RedBlackNode<Integer, String> nodeToDelete = root.findNode(root, 10, 10);
        assertNotNull(nodeToDelete);
        root = dummyNode.treeDelete(root, nodeToDelete);

        assertTrue(isValidRedBlackTree(root)); // 确保红黑树属性
        assertTrue(isBST(root)); // 确保 BST 属性
    }
}
