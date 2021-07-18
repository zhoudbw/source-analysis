package b_CAS实现原理;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟ABA问题
 * 测试思路：
 * 开启两个线程，主线程和干扰线程。
 * 主线程和干扰线程，对共享变量进行修改。
 * 共享变量采用原子INT类型。AtomicInteger中的compareAndSet是利用CAS实现的。
 * 声明如下：
 * * Atomically sets the value to the given updated value
 * * if the current value {@code ==} the expected value.
 * *
 * * @param expect the expected value
 * * @param update the new value
 * * @return {@code true} if successful. False return indicates that
 * * the actual value was not equal to the expected value.
 * <p>
 * public final boolean compareAndSet(int expect,int update){
 * return unsafe.compareAndSwapInt(this,valueOffset,expect,update);
 * }
 * <p>
 * 这样我们就可以拿这个方法去测试了。
 * 让主线程在修改的时候休眠一下，让干扰线程把现场给改变一下，然后再改回来。
 * 然后再让主线程执行下去，如果主线程还能执行成功，说明ABA问题，主线程没有觉察到。
 *
 * @author zhoudbw
 */
public class CasAbaDemo {

    public static AtomicInteger a = new AtomicInteger(1);

    public static void main(String[] args) {
        Thread main = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("操作线程：" + Thread.currentThread().getName() + "，初始值：" + a.get());
                try {
                    // 设置期望值和新值
                    int expectNum = a.get();
                    int newNum = expectNum + 1;
                    // 主线程休眠1秒钟，让出cpu
                    Thread.sleep(1000);

                    // 休眠结束，继续执行
                    boolean isSuccess = a.compareAndSet(expectNum, newNum);
                    System.out.println("操作线程：" + Thread.currentThread().getName() + "，CAS操作：" + isSuccess);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "主线程");

        Thread other = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 为了保证主线程先于干扰线程执行，这里开始就让干扰线程睡眠
                    // 但是需要确保，在主线程被唤醒前，干扰线程对于值的修改已经结束了。
                    Thread.sleep(20);
                    // a+1, a=2
                    a.incrementAndGet();
                    System.out.println("操作线程：" + Thread.currentThread().getName() + "，【increment】，值=" + a.get());
                    // 增加之后，立刻再修改回去. a-1, a=1
                    a.decrementAndGet();
                    System.out.println("操作线程：" + Thread.currentThread().getName() + "，【decrement】，值=" + a.get());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "干扰线程");
        // 开启线程
        main.start();
        other.start();
        /**
         * 运行结果:
         *  操作线程：主线程，初始值：1
         *  操作线程：干扰线程，【increment】，值=2
         *  操作线程：干扰线程，【decrement】，值=1
         *  操作线程：主线程，CAS操作：true
         */
    }
}
