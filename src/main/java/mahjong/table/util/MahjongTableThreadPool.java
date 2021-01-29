package mahjong.table.util;

import io.netty.util.concurrent.DefaultThreadFactory;
import io.vertx.core.Vertx;

import java.util.concurrent.*;

/**
 * @author muyi
 * @description:
 * @date 2020-11-20 15:42:50
 */
public class MahjongTableThreadPool {

    /**
     * 倒计时开始游戏线程池
     */
    public static ScheduledThreadPoolExecutor countdownThreadPool;

    public static ConcurrentHashMap<String, ScheduledFuture<?>> tableScheduledFuture;
    public static ConcurrentHashMap<String, Integer> schedulesCount;

    static {

        countdownThreadPool = new ScheduledThreadPoolExecutor(
                8,
                new DefaultThreadFactory("countdown-thread")
        );
        countdownThreadPool.setMaximumPoolSize(12);
        tableScheduledFuture = new ConcurrentHashMap<String, ScheduledFuture<?>>();
        schedulesCount = new ConcurrentHashMap<String, Integer>();
    }

    /**
     * 提交周期倒计时任务
     *
     * @param command
     * @param delay
     * @param unit
     * @return
     */
    public static void scheduleAtFixedRateTenTimes(Runnable command, long delay, long period, TimeUnit unit, String tableName) {

        ScheduledFuture<?> scheduledFuture = countdownThreadPool.scheduleAtFixedRate(() -> {

            int count = 11;
            if (tableScheduledFuture.containsKey(tableName)) {
                count = schedulesCount.get(tableName);
            } else {
                schedulesCount.put(tableName, count);
            }
            if (count > 0) {
                schedulesCount.put(tableName, --count);
                command.run();
            } else {
                tableScheduledFuture.get(tableName).cancel(true);
                tableScheduledFuture.remove(tableName);
                schedulesCount.remove(tableName);
            }

        }, delay, period, unit);

        tableScheduledFuture.put(tableName, scheduledFuture);
        schedulesCount.put(tableName, 11);
    }

    /**
     * 取消倒计时任务
     *
     * @param tableName
     */
    public static void cancelScheduleFuture(String tableName) {
        if (tableScheduledFuture.containsKey(tableName)) {
            tableScheduledFuture.get(tableName).cancel(true);
            tableScheduledFuture.remove(tableName);
            schedulesCount.remove(tableName);
        }
    }

    /**
     * 获得计时器执行次数
     *
     * @param tableName
     * @return
     */
    public static Integer getScheduleCount(String tableName) {
        return schedulesCount.get(tableName);
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        // worker阻塞线程
        vertx.executeBlocking(future -> {
            // 调用一些需要耗费显著执行时间返回结果的阻塞式API
            System.out.println(Thread.currentThread().getName());
            future.complete("aaa");
        }, res -> {

            System.out.println("The result is: " +Thread.currentThread().getName());
        });
//
//        // 延时一次性计时器
//        long timerID = vertx.setTimer(2000, id -> {
//            System.out.println("And one second later this is printed");
//        });
        // 周期计时器
//        long timerPeriodicID = vertx.setPeriodic(5000, id -> {
//            System.out.println("And every second this is printed: " + id);
//        });
//        WorkerExecutor aaa = vertx.createSharedWorkerExecutor("aaa");
//        aaa.executeBlocking(future -> {
//            System.out.println(Thread.currentThread().getName());
//            future.complete("aaa");
//        }, res -> {
//            System.out.println(res.result());
//        });
        // 取消计时器
//        vertx.cancelTimer(timerID);
//        vertx.cancelTimer(timerPeriodicID);
//
//        String a = "aaa";
//        String b = "aaa";
//        String[] commands = new String[]{"aaa", "bbb"};
//        System.out.println(a == commands[0]);
//
//        scheduleAtFixedRateTenTimes(() -> {
//            System.out.println(getScheduleCount("aaa"));
//        }, 1, 1, TimeUnit.SECONDS, "aaa");
//
//        Map map = Collections.EMPTY_MAP;
    }

}


