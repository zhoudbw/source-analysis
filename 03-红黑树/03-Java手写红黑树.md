## Java手写红黑树

1. 创建RBTree，定义颜色

2. 创建RBNode

3. 辅助方法定义：parentOf(RBNode node)，isRed(RBNode node)，isBlack(RBNode node)，setRed(RBNode node)，setBlack(RBNode node)，inOrderPrint()

4. 左旋方法定义：leftRotate(RBNode node)

   ```java
   	/**
        * 4. 左旋方法的定义
        * 左旋方法示意图，左旋x节点
        *     p                         p
        *     |                         |
        *     x                         y
        *    / \       ------->        / \
        *   lx   y                    x  ry
        *  / \                       / \
        * ly  ry                    lx ly
        * <p>
        * a. 将x的右子节点指向y的左子节点ly；并将y的左子节点ly的父节点更新为x
        * b. 当x的父节点p不为空时，更新y的父节点为x的父节点；并将x的父节点指定为y
        * （需要指定x子树的位置，以确定x是在p的左边还是右边。从而确定y是挂在p的左子节点还是右子节点）
        * c. 将x的父节点更新为y，将y的左子节点更新为x
        *
        * @param x
        */
       private void leftRotate(RBNode x) {
           RBNode y = x.right;
           // a. 将x的右子节点指向y的左子节点ly；并将y的左子节点ly的父节点更新为x(按照该顺序步骤，避免空指针问题)
           x.right = y.left;
           if (y.left != null) {
               y.left.parent = x;
           }
           // b. 当x的父节点p不为空时，更新y的父节点为x的父节点；并将x的父节点指定为y
           //   （需要指定x子树的位置，以确定x是在p的左边还是右边。从而确定y是挂在p的左子节点还是右子节点）
           if (x.parent != null) {
               y.parent = x.parent;
               if (x == x.parent.left) {
                   // 如果x位于其父节点的左侧
                   x.parent.left = y;
               } else {
                   // x位于其父节点的右侧
                   x.parent.right = y;
               }
           } else {
               // 此时x.parent==null，说明x是根节点
               // 此时需要更新树根的引用
               this.root = y;
               this.root.parent = null;
           }
           // c. 将x的父节点更新为y，将y的左子节点更新为x
           x.parent = y;
           y.left = x;
       }
   ```

5. 右旋方法定义：rightRotate(RBNode node)

   ```java
      /**
        * 5. 右旋方法的定义
        * 右旋方法示意图，右旋y节点
        *   	p                         p
        * 	    |                         |
        * 		x                         y
        * 	   / \       <-------        / \
        *    lx   y                    x  ry
        *   / \    	               / \
        *  ly  ry                    lx ly
        * <p>
        * a. 将y的左子节点指向x的右子节点ly；并更新x的右子节点ly的父节点为y
        * b. 当y的父节点p不为空时，更新x的父节点为y的父节点；并将y的父节点指定为x
        * （需要指定y子树的位置，以确定y是在p的左边还是右边。从而确定x是挂在p的左子节点还是右子节点）
        * c. 将y的父节点更新为x，将x的右子节点更新为y
        *
        * @param y
        */
       private void rightRotate(RBNode y) {
           RBNode x = y.left;
           // a. 将y的左子节点指向x的右子节点ly；并更新x的右子节点ly的父节点为y
           y.left = x.right;
           if (x.right != null) {
               x.right.parent = y;
           }
           // b. 当y的父节点p不为空时，更新x的父节点为y的父节点；并将y的父节点指定为x
           // （需要指定y子树的位置，以确定y是在p的左边还是右边。从而确定x是挂在p的左子节点还是右子节点）
           if (y.parent != null) {
               // 确定父节点了，但是挂载关系不确定
               x.parent = y.parent;
               if (y == y.left.parent) {
                   y.left.parent = x;
               } else {
                   y.parent.right = x;
               }
           } else {
               this.root = x;
               this.root.parent = null;
           }
           // c. 将y的父节点更新为x，将x的右子节点更新为y
           y.parent = x;
           x.right = y;
       }
   ```

6. 公开插入接口方法定义：insert(K key, V value)

   ```java
       /**
        * 6. 公开插入接口方法定义：insert(K key, V value)
        */
       public void insert(K key, V value) {
           RBNode node = new RBNode();
           node.setKey(key);
           node.setValue(value);
           // 插入节点一定是红色！！
           node.setColor(RED);
           insert(node);
       }
   ```

7. 内部插入接口方法定义：insert(RBNode node)

   ```java
       /**
        * 7.内部插入接口方法定义：insert(RBNode node)
        *
        * @param node
        */
       private void insert(RBNode node) {
           // (新传递进来的node是一个孤立的节点，没有和任何节点相连)
           // 插入就是将这个孤立的节点和已有的节点建立起联系
   
           // 第一步：查找当前node的父节点
           RBNode parent = null;
           RBNode x = this.root;
   
           while (x != null) {
               parent = x;
               // cmp>0 , 说明node.key大于x.key, 需要到x的右子树查找
               // cmp==0, 说明node.key等于x.key, 需要进行替换的操作
               // cmp<0 , 说明node.key小于x.key, 需要到x的左子树查找
               int cmp = node.key.compareTo(x.key);
               if (cmp > 0) {
                   // 到右子树找
                   x = x.right;
               } else if (cmp == 0) {
                   x.setValue(node.getValue());
                   return;
               } else {
                   // 到左子树找
                   x = x.left;
               }
           }
           // 上述过程执行结束，找父节点。
           node.parent = parent;
   
           if (parent != null) {
               // 判断node与parent的key谁大,确定将node放置在parent的左边还是右边
               int cmp = node.key.compareTo(parent.key);
               if (cmp > 0) {
                   // 当前node的key比parent大，需要把node放入parent的右子节点
                   parent.right = node;
               } else {
                   // 没有cmp==0的情况，==0已经替换了
                   // 当前node的key比parent小，需要把node放到parent的左子节点
                   parent.left = node;
               }
           } else {
               this.root = node;
               this.root.parent = null;
           }
   
           /*插入之后，可能会破坏红黑树的平衡，所以需要修复*/
           insertFixUp(node);
       }
   ```

8. 修正插入导致红黑树失衡的方法定义：insertFixUp(RBNode node)

   ```java
   	/**
        * 8. 修正插入导致红黑树失衡的方法定义：insertFixUp(RBNode node)
        * 插入后修复红黑树平衡的方法
        *   |---情景1：红黑树为空树，将根节点染色为黑色
        *   |---情景2：插入节点的key已经存在，不需要处理
        *   |---情景3：插入节点的父节点为黑色，因为插入的路径，黑色节点没有变化，所以红黑树依旧平衡，所以不需要处理
        *
        *   情景4：※※※
        *   |---情景4：插入节点的父节点为红色 （违反红黑树的性质：两个红色节点不能相连）
        *       |---情景4-1：叔叔节点存在，并且为红色（父-叔 双红），将爸爸和叔叔染色为黑色，将爷爷染色为红色，并且再以爷爷为当前节点，进行下一轮处理。
        *       |---情景4-2：叔叔节点不存在或者为黑色，父节点为爷爷节点的左子树
        *           |---情景4-2-1：插入节点为其父节点的左子节点（LL情况），将爸爸染色为黑色，将爷爷染色为红色，然后以爷爷节点右旋，就完成了。
        *           |---情景4-2-2：插入节点为其父节点的右子节点（LR情况），以爸爸节点进行一次左旋，得到LL双红情况（4-2-1），然后指定爸爸节点为当前节点进行下一轮处理。
        *       |---情景4-3：叔叔节点不存在或者为黑色，父节点为爷爷节点的右子树
        *           |---情景4-3-1：插入节点为其父节点的右子节点（RR情况），将爸爸染色为黑色，将爷爷染色为红色，然后以爷爷节点左旋，就完成了。
        *           |---情景4-3-2：插入节点为其父节点的左子节点（RL情况），以爸爸节点进行一次右旋，得到RR双红情况（4-3-1），然后指定爸爸节点为当前节点进行下一轮处理。
        */
       private void insertFixUp(RBNode node) {
           this.root.setColor(BLACK);
   
           // 爸爸
           RBNode parent = parentOf(node);
           // 爷爷
           RBNode gparent = parentOf(parent);
           // 情景4：插入节点的父节点为红色 （违反红黑树的性质：两个红色节点不能相连）
           if (parent != null && isRed(parent)) {
               // 如果父节点是红色，那么一定存在爷爷节点，因为根节点不可能是红色
   
               RBNode uncle = null;
               if (parent == gparent.left) {
                   // 如果爸爸节点在爷爷的左边，那么叔叔就在爷爷的右边
                   uncle = gparent.right;
                   // 情景4-1：叔叔节点存在，并且为红色（父-叔 双红）
                   if(uncle != null && isRed(uncle)) {
                       // 将爸爸和叔叔染色为黑色，将爷爷染色为红色，
                       setBlack(parent);
                       setBlack(uncle);
                       setRed(gparent);
                       // 并且再以爷爷为当前节点，进行下一轮处理。
                       insertFixUp(gparent);
                       return;
                   }
                   // 情景4-2：叔叔节点不存在或者为黑色，父节点为爷爷节点的左子树
                   if (uncle == null || isBlack(uncle)) {
                       // 情景4-2-1：插入节点为其父节点的左子节点（LL情况），
                       if (node == parent.left) {
                           // 将爸爸染色为黑色，将爷爷染色为红色，然后以爷爷节点右旋，就完成了。
                           setBlack(parent);
                           setRed(gparent);
                           rightRotate(gparent);
                           return;
                       }
                       // 情景4-2-2：插入节点为其父节点的右子节点（LR情况），
                       if (node == parent.right){
                           // 以爸爸节点进行一次左旋，得到LL双红情况（4-2-1），
                           leftRotate(parent);
                           // 然后指定爸爸节点为当前节点进行下一轮处理。
                           insertFixUp(parent);
                           return;
                       }
                   }
               } else {
                   // 父节点为爷爷节点的右子树
                   uncle = gparent.left;
   
                   // 情景4-1：叔叔节点存在，并且为红色（父-叔 双红）
                   if(uncle != null && isRed(uncle)) {
                       // 将爸爸和叔叔染色为黑色，将爷爷染色为红色，
                       setBlack(parent);
                       setBlack(uncle);
                       setRed(gparent);
                       // 并且再以爷爷为当前节点，进行下一轮处理。
                       insertFixUp(gparent);
                       return;
                   }
                   // 情景4-3：叔叔节点不存在或者为黑色，父节点为爷爷节点的右子树
                   if (uncle == null || isBlack(uncle)) {
                       // 情景4-3-1：插入节点为其父节点的右子节点（RR情况），
                       if (node == parent.right) {
                           // 将爸爸染色为黑色，将爷爷染色为红色，然后以爷爷节点左旋，就完成了。
                           setBlack(parent);
                           setRed(gparent);
                           leftRotate(gparent);
                           return;
                       }
                       // 情景4-3-2：插入节点为其父节点的左子节点（RL情况），
                       if (node == parent.left) {
                           // 以爸爸节点进行一次右旋，得到RR双红情况（4-3-1），
                           rightRotate(parent);
                           // 然后指定爸爸节点为当前节点进行下一轮处理。
                           insertFixUp(parent);
                           return;
                       }
                   }
               }
           }
       }
   ```

9. 测试红黑树正确性

   ```java
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
   ```

   

