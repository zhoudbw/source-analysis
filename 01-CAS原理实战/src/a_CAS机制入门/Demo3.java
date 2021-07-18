package a_CAS机制入门;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 模拟我们CAS.txt中的需求
 *
 * @author zhoudbw
 */

public class Demo3 {

    /**
     * 总访问量
     * 添加volatile，保证多线程之间，count是具有可见性的。
     * 线程在取count的值的时候，是从主存中拉取，而不是从当前缓存中取的。
     * ###　不理解自己查呀
     */
    volatile static int count = 0;


    public static void request() throws InterruptedException {
        // 模拟耗时5毫秒
        TimeUnit.MILLISECONDS.sleep(5);

//        count++;
        // 表示期望值
        int expectCount;
        while (!compareAndSwap(expectCount = getCount(), expectCount + 1)) {}
    }


    /**
     * 对count++ 的第三步进行的升级
     * @param expectCount 期望值
     * @param newCount 需要给count赋值的新值
     * @return 成功返回true，失败返回false
     */
    public static synchronized boolean compareAndSwap(int expectCount, int newCount) {
        // 判断count当前值是否和期望值expectCount一致，如果一致，将newCount赋值给count
        if (getCount() == expectCount) {
            count = newCount;
            return true;
        }
        return false;
    }

    public static int getCount() {
        return count;
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
        // 运行结果1：main，耗时：68，count=1000
        // 运行结果2：main，耗时：67，count=1000
        // 运行结果3：main，耗时：69，count=1000
        /**
         * 我们给上述的compareAndSwap()方法起个简称 —— CAS
         */
    }
}

