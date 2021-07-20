package a_LongAdder结构分析;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 对于 base + ∑cell 的值的计数方式
 * 及
 * 对于 原子类的计数方式
 * 的
 * 性能对比
 *
 * @author zhoudbw
 */
public class LongAdderVSAtomicLongDemo {

    public static void main(String[] args) {
        /**
         * 每次都是一轮
         */
        // 开启1个线程，每个线程都自增一千万次
        testAtomicLongVSLongAdder(1, 10000000);
        // 开启10个线程，每个线程都自增一千万次
        testAtomicLongVSLongAdder(10, 10000000);
        // 开启20个线程，每个线程都自增一千万次
        testAtomicLongVSLongAdder(20, 10000000);
        // 开启40个线程，每个线程都自增一千万次
        testAtomicLongVSLongAdder(40, 10000000);
        // 开启80个线程，每个线程都自增一千万次
        testAtomicLongVSLongAdder(80, 10000000);
    }

    /**
     * @param threadCount 开启线程数
     * @param times       累加次数
     */
    static void testAtomicLongVSLongAdder(final int threadCount, final int times) {
        try {
            System.out.println("threadCount：" + threadCount + "，times：" + times);
            long start = System.currentTimeMillis();
            testLongAdder(threadCount, times);
            System.out.println("LongAdder elapse：" + (System.currentTimeMillis() - start) + "ms");

            long start2 = System.currentTimeMillis();
            testAtomicLong(threadCount, times);
            System.out.println("AtomicLong elapse：" + (System.currentTimeMillis() - start2) + "ms");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void testAtomicLong(int threadCount, int times) throws InterruptedException {
        // 共享变量
        AtomicLong atomicLong = new AtomicLong();
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            list.add(new Thread(() -> {
                for (int j = 0; j < times; j++) {
                    atomicLong.incrementAndGet();
                }
            }));
        }

        for (Thread thread : list) {
            thread.start();
        }

        for (Thread thread : list) {
            thread.join();
        }
    }

    private static void testLongAdder(int threadCount, int times) throws InterruptedException {
        // 共享变量
        LongAdder longAdder = new LongAdder();
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            list.add(new Thread(() -> {
                for (int j = 0; j < times; j++) {
                    longAdder.add(1);
                }
            }));
        }

        for (Thread thread : list) {
            thread.start();
        }

        for (Thread thread : list) {
            thread.join();
        }
    }
    /**
    运行结果：
    threadCount：1，times：10000000
    LongAdder elapse：174ms
    AtomicLong elapse：66ms
    threadCount：10，times：10000000
    LongAdder elapse：360ms
    AtomicLong elapse：985ms
    threadCount：20，times：10000000
    LongAdder elapse：286ms
    AtomicLong elapse：1945ms
    threadCount：40，times：10000000
    LongAdder elapse：557ms
    AtomicLong elapse：3879ms
    threadCount：80，times：10000000
    LongAdder elapse：835ms
    AtomicLong elapse：7627ms

    从结果可以看出。LongAdder在高并发下的性能，比AtomicLong要好的多。
     */
}
