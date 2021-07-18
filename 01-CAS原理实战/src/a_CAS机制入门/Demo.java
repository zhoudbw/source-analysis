package a_CAS机制入门;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 模拟我们CAS.txt中的需求
 *
 * @author zhoudbw
 */

public class Demo {

    /**
     * 总访问量
     */
    static int count = 0;

    /**
     * 模拟访问的方法
     */
    public static void request() throws InterruptedException {
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
        // 运行结果1：main，耗时：77，count=985
        // 运行结果2：main，耗时：66，count=964
        // 运行结果3：main，耗时：65，count=975
        /**
         * 发现这个运行结果还不一样呢？
         *  我们想要的是1000，为什么每次都比1000少呢？
         * 问题在哪？ —— 问题出现在request()方法中，因为count++ 不是原子的。
         *
         * Q：分析一下问题出在哪里？
         * A：代码中采用的是多线陈的方式来操作count，count++会有多线程问题
         * count++ 实际上是由3步来完成的！（了解，看JVM执行引擎相关的文章）
         *      1.获取count的值，记作A： A=count
         *      2.将A值+1，得到B：B=A+1
         *      3.将B值赋值给count
         *
         * 如果有A和B两个线程，同时执行count++,他们同时执行到上面步骤的第一步，得到count是一样的，
         * 3步操作之后，count只加1，导致count结果不正确！
         *
         * Q：怎么解决结果不正确的问题？
         * A：对count++操作的时候，我们让多线程排队处理，多个线程同时到达request()方法的时候，
         * 只能允许一个线程可以进去操作，其他的线程在外面等着，等里面的处理完毕出来之后，
         * 外面再进去一个，这样操作的count++就是排队进行的，结果一定是正确的。
         *
         * Q：怎么实现排队效果？
         * A：Java中的synchronized关键字和ReentrantLock都可以实现对资源加锁，保证并发正确性，
         * 多线程的情况下可以保证杯锁住的资源被“串行”访问。
         *
         * ## 详情见 a_CAS机制入门.Demo2
         */
    }
}
