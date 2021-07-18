package a_CAS机制入门;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 模拟我们CAS.txt中的需求
 *
 * @author zhoudbw
 */

public class Demo2 {

    /**
     * 总访问量
     */
    static int count = 0;

    /**
     * 模拟访问的方法
     * synchronized 拿到的是Demo2的class对象的锁。static是对象级别的锁。
     * 也就是说，线程来到request()方法之前，必须拿到Java方法区中的这个Demo2的class对象的锁，才能进去。
     * 其他的线程，没有拿到这个锁的情况下，都得在request()方法外面等着。
     * 等拿到Demo2的锁的进程出去，放掉Demo2的锁，然后线程去争抢再次进入，没抢到的还在外面等着。
     * 这样就保证了串行了。
     */
    public synchronized static void request() throws InterruptedException {
        // 模拟耗时5毫秒
        TimeUnit.MILLISECONDS.sleep(5);
        count++;
    }

    public static void main(String[] args) throws InterruptedException {
        // 开始时间
        long startTime = System.currentTimeMillis();
        int threadSize = 100;

        CountDownLatch countDownLatch = new CountDownLatch(threadSize);

        for (int i = 0; i < threadSize; i++) {
            // 这里有个警告：“不要显示创建线程，请使用线程池”
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // 模拟用户行为，每个用户访问10次网站
                    try {
                        for (int j = 0; j < 10; j++) {
                            request();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        // 让数量减1
                        countDownLatch.countDown();
                    }
                }
            });
            thread.start();
        }
        // 怎么保证100个线程执行结束，在执行后面的代码呢？ ——  使用CountDownLatch
        // countDownLatch.await()，这个阻塞必须等CountDownLatch内维护的值为0，此能够不阻塞
        countDownLatch.await();

        long endTime = System.currentTimeMillis();
        System.out.println(Thread.currentThread().getName() + "，耗时：" + (endTime - startTime) + "，count=" + count);
        // 运行结果1：main，耗时：5298，count=1000
        // 运行结果2：main，耗时：5247，count=1000
        // 运行结果3：main，耗时：5527，count=1000
        /**
         * 结果倒是对了，但是耗时太长了。
         *
         * Q：耗时太长的原因是什么？
         * A：程序中的request()方法使用了synchronized关键字修饰，
         * 保证了并发情况下，request方法同一个时刻只允许一个程序进入，
         * request加锁相当于串行执行了，count的结果和我们预期的一致，致使耗时太长了。
         *
         * Q：如何解决耗时长的问题？
         * A：有些代码是没有必要串行执行的，比如说：TimeUnit.MILLISECONDS.sleep(5);
         * 关键：
         * count++ 实际上是由3步来完成的！（了解，看JVM执行引擎相关的文章）
         *   1.获取count的值，记作A： A=count
         *   2.将A值+1，得到B：B=A+1
         *   3.将B值赋值给count
         *      升级第三步的实现：
         *          1.获取锁
         *          2.获取一下count最新的值，记为LV（加锁之后的最新值）
         *     ###  3.判断LV是否等于A（期望值），如果相等，将B的值赋值给count，并返回true，否则返回false
         *          4.释放锁
         *   注：这里最好这么理解，count是从主存里面取值，而A和B都存储在缓存里，从缓存向主存中写值，一定要有线程控制机制
         *   否则会造成线程之间相互覆盖，导致最后值不能完全符合预期。
         *   这就是自旋，当不能获得预期结果的时候，不断重复之前的过程直到拿到符合预期的值为止。
         *
         * 详情见：a_CAS机制入门.Demo3
         */
    }
}

