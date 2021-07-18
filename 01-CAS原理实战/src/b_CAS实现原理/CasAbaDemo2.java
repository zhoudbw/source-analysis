package b_CAS实现原理;

import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * @author zhoudbw
 */
public class CasAbaDemo2 {

    public static AtomicStampedReference<Integer> a = new AtomicStampedReference(new Integer(1), 1);

    public static void main(String[] args) {
        Thread main = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("操作线程：" + Thread.currentThread().getName() + "，初始值：" + a.getReference());
                try {
                    // 设置期望值和新值
                    Integer expectReference = a.getReference();
                    Integer newReference = expectReference + 1;
                    Integer expectStamp = a.getStamp();
                    Integer newStamp = expectStamp + 1;
                    // 主线程休眠1秒钟，让出cpu
                    Thread.sleep(1000);

                    // 休眠结束，继续执行
                    boolean isSuccess = a.compareAndSet(expectReference, newReference, expectStamp, newStamp);
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
                    a.compareAndSet(a.getReference(), (a.getReference() + 1), a.getStamp(), (a.getStamp() + 1));
                    System.out.println("操作线程：" + Thread.currentThread().getName() + "，【increment】，值=" + a.getReference());
                    a.compareAndSet(a.getReference(), (a.getReference() - 1), a.getStamp(), (a.getStamp() - 1));
                    System.out.println("操作线程：" + Thread.currentThread().getName() + "，【decrement】，值=" + a.getReference());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "干扰线程");
        // 开启线程
        main.start();
        other.start();
        /**
         * 运行结果：
         *  操作线程：主线程，初始值：1
         *  操作线程：干扰线程，【increment】，值=2
         *  操作线程：干扰线程，【decrement】，值=1
         *  操作线程：主线程，CAS操作：false
         */
    }
}
