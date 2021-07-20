# 06 longAccumulate()方法

## 06.01 调用longAccumulate()的情况

```
01 Cells未初始化，也就是多线程写入base发生了竞争（然后应该：重试 | 初始化Cells）
02 说明当前线程对应下标的Cell为空，需要创建（需要longAccumulate()支持）
03 表示cas失败，意味着当前线程对应的cell有竞争（然后应该：重试 | 扩容）
```

## 06.02 longAccumulate()方法详解

```java
/**
 * Handles cases of updates involving initialization, resizing,
 * creating new Cells, and/or contention. See above for
 * explanation. This method suffers the usual non-modularity
 * problems of optimistic retry code, relying on rechecked sets of
 * reads.
 *
 * @param x the value 就是add()方法的增量x
 * @param fn the update function, or null for add (this convention
 * avoids the need for an extra field or function in LongAdder). 操作算法的接口，可以通过这个实现这个类，去扩展我们想要实现的算法
 * @param wasUncontended false if CAS failed before call 表示是否真正的发生过竞争。
 * 一般情况下，传递的都是true。只有如下情况它传递的是false：
    Cells数组已经创建出来了，并且在同一个cell上发生竞争。修改失败的线程会拿到才会是false。
    因为add()方法传递的是uncontended:uncontended = a.cas(v = a.value, v + x)
 */
final void longAccumulate(long x, LongBinaryOperator fn,
                          boolean wasUncontended) {
    int h; // 表示线程的Hash值。
    if ((h = getProbe()) == 0) { // ==0 说明还没有为当前线程分配hash值。
        // 分配Hash值的逻辑。
        // 给当前线程分配给hash值。current()方法，并不难懂，可以看看。
        ThreadLocalRandom.current(); // force initialization
        // 取出当前Hash值，赋值给h
        h = getProbe();
        // 将wasUncontended设置为true了，为什么？
        // 如果当前线程没有hash值，那么线程会把数据写到那个Cell里面呢？
        // 0与任何数做位运算都为0，那么会将数据写道第一位Cell里面。
        // 两个线程都写入Cells[0],那么就会发生竞争，竞争失败了，就会进入到longAccumulate()方法中。
        // 进入longAccumulate()之后，首先就会分配一个Hash值（也就是发生了竞争之后，才会首先分配Hash值。）
        // 这样再去重试的时候，就不一定是打到0位上了。
        // ——> 因为默认情况下，当前线程肯定时写入到cells[0]位置的，不把这次当作真正的竞争。
        wasUncontended = true;
    }

    // 表示扩容的意向，false-一定不扩容；true-可能会扩容，也不是一定。
    boolean collide = false;                // True if last slot nonempty

    // for循环内什么条件都没有，是“自旋”
    for (;;) {

        // 创建了一堆局部变量：
        // as 表示cells引用
        // a 表示当前线程命中的cell
        // n 表示cells数组的长度
        // v 表示期望值
        Cell[] as; Cell a; int n; long v;
         // case1：表示cells已经初始化了，当前线程应该将数据写入对应的cells中。
        if ((as = cells) != null && (n = as.length) > 0) {

            // 进入case1：
            //       02 说明当前线程对应下标的Cell为空，需要创建（需要longAccumulate()支持）
            //       03 表示cas失败，意味着当前线程对应的cell有竞争（然后应该：重试 | 扩容）

            // case1.1：true-表示当前线程对应的下标位置的cell为null，需要创建Cell，也就是new的过程。
            if ((a = as[(n - 1) & h]) == null) {

                // true-表示当前锁未被占用 false-表示锁被占用
                if (cellsBusy == 0) {       // Try to attach new Cell

                    // 那当前x创建Cell
                    Cell r = new Cell(x);   // Optimistically create

                    // cellsBusy == 0：true-表示当前锁未被占用 false-表示锁被占用
                    // casCellsBusy()：true-表示当前线程获取锁成功 false-获取锁失败
                    if (cellsBusy == 0 && casCellsBusy()) {
                        // 是否创建成功的标记，只有真正创建成功才是true
                        boolean created = false;
                        try {               // Recheck under lock
                            // rs表示当前cells引用
                            // m表示当前cells长度
                            // j表示当前线程命中的cell小标
                            Cell[] rs; int m, j;

                            // (rs = cells) != null && (m = rs.length) > 0 ：恒成立的
                            // rs[j = (m - 1) & h] == null)：为了防止其他线程初始化过该位置，然后当前线程再次初始化该位置，导致丢失数据。
                            if ((rs = cells) != null &&
                                (m = rs.length) > 0 &&
                                rs[j = (m - 1) & h] == null) {
                                rs[j] = r;
                                created = true;
                            }
                        } finally {
                            cellsBusy = 0;
                        }
                        if (created)
                            break;
                        continue;           // Slot is now non-empty
                    }
                }
                // false的时候，强制将扩容意向改为了false
                // 因为还没有开始写呢，就说写不进去，不合理，所以控制一下不扩容
                collide = false;
            }

            // case1.2：只有一种情况到这里 uncontended==false
            // Cells数组已经创建出来了，并且在同一个cell上发生竞争。修改失败的线程会拿到才会是false。
            else if (!wasUncontended)       // CAS already known to fail
                wasUncontended = true;      // Continue after rehash

            // case1.3：当前线程rehash过hash值，然后新命中的cell不为空。
            // true-写入成功，退出循环
            // false-rehash之后命中的新的cell，也有竞争，也失败了。重试第1次
            else if (a.cas(v = a.value, ((fn == null) ? v + x :
                                         fn.applyAsLong(v, x))))
                break;

            // case1.4：
            // 条件1：n>=NCPU true-改变扩容意向，不扩容了。 false-cells数组还可以扩容
            // 条件2：cells!=as true-其他线程已经重试过了，当前线程rehash之后重试即可。
            else if (n >= NCPU || cells != as)
                collide = false;            // At max size or stale

            // case1.5：!collide true-设置扩容意向为true，但是不一定真的发生扩容。
            else if (!collide)
                collide = true;

            // case1.6：在case1.5之后，最后还是会rehash，那么继续从case1.1判断，在case1.3开始重试，那么此时已经重试了2次了
            // 还是失败，来到这里。—— 真正扩容的逻辑
            // 条件1：cellsBusy==0 true-表示无锁状态，当前线程可以去竞争这把锁。
            // 条件2：casCellsBusy() true-表示当前线程获取锁成功，可以执行扩容逻辑。false-当前时刻，有其他线程在做扩容操作，等待。
            else if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    // cells==as？假设现在有A，B两个线程，A执行完cellsBusy == 0，失去了CPU的使用权
                    // 线程B，拿到CPU，执行到最后了。
                    // 现在线程A，又拿到了CPU，如果不做判断，那么就重复扩容了，线程B已经扩容过了，A再去扩容，
                    // 那么cells引用也变化了。
                    if (cells == as) {      // Expand table unless stale
                        Cell[] rs = new Cell[n << 1]; // 左移一位 * 2

                        // 把原先数组内的元素，挨个遍历放入到新的数组中
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    // 释放锁
                    cellsBusy = 0;
                }
                collide = false;
                continue;                   // Retry with expanded table
            }

            // 重置当前线程的Hash值，此时当前线程再去写值，就可能不是0了。
            h = advanceProbe(h);
        }

        // case2：前置条件cells还未初始化，as == null，下面需要初始化cell
        // cellsBusy == 0，true-表示当前未加锁
        // cells == as，再次对比一下的原因？因为其他线程可能会在我们给as赋值之后改变cells的值。
        // casCellsBusy()，true-表示获取锁成功，会把cellsBusy的值设置为1，false表示其他线程正在持有这把锁。
        else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
            boolean init = false;
            try {                           // Initialize table

                // 为什么这里又对比cells==as?
                // 现在有t1和t2两个线程，现在t1执行了cellsBusy == 0 && cells == as失去cpu的使用权
                // 紧接着t2拿到了cpu的使用权，执行了cellsBusy == 0 && cells == as && casCellsBusy()，直到最后释放cellsBusy。
                // 然后开始t1执行，如果不做cells==as的判断，那么可能出现cells被覆盖，从而丢失数据。
                // ——> 防止其他线程已经初始化cells了，当前线程再次初始化，导致丢失数据。
                if (cells == as) {
                    Cell[] rs = new Cell[2];
                    rs[h & 1] = new Cell(x);
                    cells = rs;
                    init = true;
                }
            } finally {
                cellsBusy = 0;
            }
            if (init)
                break;
        }

        // case3：
        // 1.当前cellsBusy处于加锁状态，表示其他线程正在初始化cells，所以当前线程将值累加到base。
        // 2.cells被其他线程初始化后，当前线程需要将数据累加到base。
        else if (casBase(v = base, ((fn == null) ? v + x :
                                    fn.applyAsLong(v, x))))
            break;                          // Fall back on using base
    }
}     
```

## 06.03 LongAdder -> sum()


```java
/**
* Returns the current sum.  The returned value is <em>NOT</em> an
* atomic snapshot; invocation in the absence of concurrent
* updates returns an accurate result, but concurrent updates that
* occur while the sum is being calculated might not be
* incorporated.
*
* @return the sum
*/
public long sum() {
    Cell[] as = cells; Cell a;
    long sum = base;
    if (as != null) {
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                sum += a.value;
        }
    }
    return sum;
}
// 这个类的方法是不精确的，如果想要精确还是要用，AtomicLong
```
