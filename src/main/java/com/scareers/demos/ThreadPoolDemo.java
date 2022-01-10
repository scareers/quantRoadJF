package com.scareers.demos;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.scareers.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * description:
 * <p>
 * 总结:
 * -- execute(Runnable)
 * 1. 不需要返回值时, 使用 Runable,
 * 2. 只需要循环, 执行对象即可
 * 3. 默认并行, 将不进行等待, 直接执行主线程 后面的代码,   当然,程序退出需要所有子线程执行完毕
 * 4. 如果要求等待所有线程执行完毕, 需要 使用 latch 机制!!! 任务逻辑 finally  latch.countDown().   初始化数量为任务数量
 * -- submit(Callable)
 * 1.需要返回值
 * 2.循环submit(Callable),          每次调用将瞬间返回 Future<T>对象, 需要使用 AL 对 futures进行保存!!
 * ...不可 直接f.get(), 这样会导致串行
 * 3.submit循环执行完成后,  因通常需要等待线程全部执行完毕,
 * ... 因此 遍历暂存的 futures, 调用 f.get() 拿到结果.  当然, 在循环中, f.get() 后的代码, 约等于是串行的. 而非并行,尽量不执行耗时代码
 * 4.循环调用f.get() 已经达成了等待, 因此 等待所有子线程结束, 再跑主线程,  并不需要 latch 机制帮助!!!
 * -- pool.shutdown();
 * 5.如果使用了Callable, 但是又不需要读取返回值, 即不调用 f.get(), 则需要配合 latch, 才能等待所有线程执行完成
 * 这样才会正常退出程序
 *
 * @author: admin
 * @date: 2021/11/10  0010-10:25
 */
public class ThreadPoolDemo {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
//        testRunable();
        testCallable();
    }

    public static void testRunable() throws InterruptedException {
        final TimeInterval timer = new TimeInterval();
        timer.start("1");

        List<Integer> indexes = CommonUtil.range(100);
        CountDownLatch latch = new CountDownLatch(indexes.size());
        ThreadPoolExecutor pool = new ThreadPoolExecutor(100,
                200, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        for (Integer index : indexes) {
            pool.execute(new TaskTest2(index, latch));
        }
        latch.await();
        Console.log(timer.intervalRestart());
        pool.shutdown();
    }

    public static void testCallable() throws InterruptedException, ExecutionException {
        final TimeInterval timer = new TimeInterval();
        timer.start("1");

        List<Integer> indexes = CommonUtil.range(100);
        CountDownLatch latch = new CountDownLatch(indexes.size());
        ThreadPoolExecutor pool = new ThreadPoolExecutor(100,
                200, 10000, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());

        ArrayList<String> results = new ArrayList<>();
        ArrayList<Future<String>> futures = new ArrayList<>();
        for (Integer index : indexes) {
            Future<String> f = pool
                    .submit(new TaskTest(index, latch));
            futures.add(f);
        }

//        for (Future f : futures) {
//            results.add((String) f.get());
//            Thread.sleep(2);  // 这些代码 将循环耗时
//        }
//        latch.await();

        Console.log(results);
        Console.log(timer.intervalRestart());
        pool.shutdown();
    }
}

class TaskTest implements Callable<String> {
    Integer i;
    CountDownLatch latch;


    public TaskTest(Integer i, CountDownLatch latch) {
        this.i = i;
        this.latch = latch;
    }

    @Override
    public String call() throws Exception {
        try {
            Thread.sleep(RandomUtil.randomInt(1000));
            Console.log(StrUtil.format("task {} finish", i));
            return StrUtil.format("task {} finish", i);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            latch.countDown();
            return StrUtil.format("task {} finish", i);
        }
    }
}


class TaskTest2 implements Runnable {
    Integer i;
    CountDownLatch latch;


    public TaskTest2(Integer i, CountDownLatch latch) {
        this.i = i;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(RandomUtil.randomInt(1000));
            Console.log(StrUtil.format("task {} finish", i));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            latch.countDown();
        }
    }
}

