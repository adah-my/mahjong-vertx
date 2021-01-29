package mahjong.game.util;

import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.*;

/**
 * @author muyi
 * @description:
 * @date 2020-11-19 19:48:26
 */
public class MahjongGameThreadPools {

    /**
     * AI辅助执行线程池
     */
    public static ScheduledThreadPoolExecutor robotThreadPool;

    static {
        robotThreadPool = new ScheduledThreadPoolExecutor(
                8,
                new DefaultThreadFactory("robot-action")
        );
        robotThreadPool.setMaximumPoolSize(16);
    }

    /**
     * 提交任务
     *
     * @param command
     */
    public static void executeByHelpThreadPool(Runnable command) {
        robotThreadPool.execute(command);
    }

    /**
     * 提交延时任务
     *
     * @param command
     * @param delay
     * @param unit
     * @return
     */
    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return MahjongGameThreadPools.robotThreadPool.schedule(command, delay, unit);
    }

}
