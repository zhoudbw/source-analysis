# 05 深入剖析LongAdder

## 05.01 纵观LongAdder

```java
public class LongAdder extends Striped64 implements Serializable {...}

继承Strip64，从我们画的思想图中看，我们来寻找Base和Cell，发现这个类中并没有这些元素。
所以，核心还是Strip64，在Strip64中我们可以看到Cell这个内部类，其中有个属性：volatile long value。
之后又有一个transient volatile Cell[] cells数组 和 transient volatile long base;
```

-> 这样就可以和我们的 “01-思想图”中的内容对应上了。



## 05.02 入口方法 —— add(long x)
* 声明如下：

```java
/**
 * Adds the given value.  将上给定的数值
 *
 * @param x the value to add  要添加的值
 */
public void add(long x) {
    // 线程进来会创建几个临时变量：
    // as 表示Cell[]的引用
    // b  表示获取的base值
    // v  表示期望值
    // m  表示Cell数组的长度
    // a  表示当前线程命中的Cell单元格（x值会加到这个单元格内）
    Cell[] as; long b, v; int m; Cell a;

    // 条件1：true-表示cells已经初始化过了，也就是我们"01-思想图"中的cells数组已经有了。隐含，当前线程应该将数据写入到对应的cell中
    //       false-表示cells未初始化，隐含，当前所有线程应该将数据写入到base中

    // 条件2：false-表示当前线程cas替换数据成功
    //       true-表示发生竞争了，可能需要重试或者扩容
    if ((as = cells) != null || !casBase(b = base, b + x)) {
        // 什么时候会进来？
        // 1. cells已经初始化过了，当前线程应该将数据写入到对应的cell中
        // 2. 线程之间发生了冲突，可能需要重试或者扩容

        // true-未竞争
        // false-发生竞争
        boolean uncontended = true;

        // as == null || (m = as.length - 1) < 0 ：
        // true-说明cells未初始化，也就是多线程写base发生竞争了
        // false-说明cells已经初始化了，当前线程应该是找自己的cell写值

        // a = as[getProbe() & m]) == null：getProbe()获取当前线程的hash值，m表示cells的长度-1，m一定是一个2的次方数，这样减1之后得到的数，是b111...
        // true-说明当前线程对应下标的cell为空，需要创建，使用longAccumulate()支持
        // false-说明当前线程对应的cell不为空，说明，下一步想要将x的值，添加到cell中

        // !(uncontended = a.cas(v = a.value, v + x))：
        // true-表示cas失败，意味着当前线程对应的cell有竞争
        // false-表示cas成功
        if (as == null || (m = as.length - 1) < 0 ||
            (a = as[getProbe() & m]) == null ||
            !(uncontended = a.cas(v = a.value, v + x)))
            // 都有哪些情况会调用longAccumulate()
            // 1. cells未初始化，也就是多线程写base发生竞争（去重试|初始化cells）
            // 2. 当前线程对应下标的cell为空，需要创建，通过longAccumulate()支持
            // 3. cas失败，意味着当前线程对应的cell有竞争（去重试|扩容）
            longAccumulate(x, null, uncontended);
    }
}

```

##　05.03 Strip64的结构

###  1.静态内部类Cell

```java
/**
* Padded variant of AtomicLong supporting only raw accesses plus CAS.
*   AtomicLong的填充变体 仅支持原始访问和cas
* JVM intrinsics note: It would be possible to use a release-only
* form of CAS here, if it were provided.
*/
@sun.misc.Contended static final class Cell {
    volatile long value; // 对应“01-思想图”中的结构：Cell中包含value。
    Cell(long x) { value = x; }
    final boolean cas(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
    } // 通过UNSAFE实现的，会通过cas将数据累加到value上面。

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    // value基于该对象的内存地址偏移量，通过“这个量+对象地址”可以找到value属性。
    private static final long valueOffset; 
    static { // 静态代码块做的事情：
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> ak = Cell.class;   // 拿到Cell的类型。
            valueOffset = UNSAFE.objectFieldOffset
                // 反射拿到value属性的值，通过objectFieldOffset()为valueOffset赋值
                (ak.getDeclaredField("value")); 
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
```

### 2.属性：NCPU

```java
/** Number of CPUS, to place bound on table size */
// 表示当前计算机CPU数量。作用：控制Cells数组的一个关键性条件
// Cell[]数组最大扩容就是CPU的长度，因为每一个CPU同一时刻只能执行一个线程。
// 如果创建一个长度为32的Cell数组，电脑的CPU为16.
// 那么同一时间，最大的并发数是16.
static final int NCPU = Runtime.getRuntime().availableProcessors();
```

### 3.Cells & Base

```java
/**
* Table of cells. When non-null, size is a power of 2.
*/
transient volatile Cell[] cells;

/**
     * Base value, used mainly when there is no contention, but also as
     * a fallback during table initialization races. Updated via CAS.
     * 没有发生过竞争时，数据会累加到 base上 | 当cells扩容时，需要将数据写到base中
     */
transient volatile long base;
```

### 4.CellsBusy

```java
/**
* Spinlock (locked via CAS) used when resizing and/or creating Cells.
* 如果要操作Cells（初始化Cells | 扩容Cells）都需要获取锁。 0 - 无锁 1 - 其他线程已经拿到这个锁了
*/
transient volatile int cellsBusy;
/**
* CASes the cellsBusy field from 0 to 1 to acquire lock.
* 通过CAS方式获取锁。
*/
final boolean casCellsBusy() {
    return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
}
```

### 5.getProbe() & advanceProbe

```java
/**
* Returns the probe value for the current thread.
* Duplicated from ThreadLocalRandom because of packaging restrictions.
* 获取当前线程的Hash值
*/
static final int getProbe() {
    return UNSAFE.getInt(Thread.currentThread(), PROBE);
}

/**
* Pseudo-randomly advances and records the given probe value for the
* given thread.
* Duplicated from ThreadLocalRandom because of packaging restrictions.
* 重置当前线程的hash值
*/
static final int advanceProbe(int probe) {
    probe ^= probe << 13;   // xorshift
    probe ^= probe >>> 17;
    probe ^= probe << 5;
    UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
    return probe;
}
```