package 红黑树;

import java.util.Scanner;

/**
 * @author zhoudbw
 * 测试红色树
 */
public class RBTreeTest {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RBTree<String, Object> rbt = new RBTree<>();
        while (true) {
            System.out.println("请输入key：");
            String key = scanner.next();
            System.out.println();
            rbt.insert(key, null);

            TreeOperation.show(rbt.getRoot());
        }
    }
    /**
     * 问题：输入数据过大时会出现。
     *
     * 请输入key：
     * 19
     *
     *                         4B
     *                     /       \
     *                 2B              6B
     *              /     \         /     \
     *           1B          3R  5B          8R
     *         /   \       /   \           /   \
     *       0R      19R 29B     39B     7B      9B
     *                  /                       /
     *                 28R                     88R
     * 请输入key：
     * 200
     *
     * Exception in thread "main" java.lang.NullPointerException
     * 	at 红黑树.RBTree$RBNode.access$000(RBTree.java:362)
     * 	at 红黑树.RBTree.rightRotate(RBTree.java:344)
     * 	at 红黑树.RBTree.insertFixUp(RBTree.java:218)
     * 	at 红黑树.RBTree.insert(RBTree.java:167)
     * 	at 红黑树.RBTree.insert(RBTree.java:114)
     * 	at 红黑树.RBTreeTest.main(RBTreeTest.java:17)
     */
}
