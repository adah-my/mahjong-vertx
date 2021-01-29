package mahjong.util;

import mahjong.game.cache.bean.MahjongGamesCacheBean;
import mahjong.table.cache.MahjongTableCache;
import mahjong.table.cache.bean.MahjongTableBean;
import mahjong.table.cache.bean.MahjongTablesCacheBean;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

/**
 * @author muyi
 * @description:
 * @date 2020-11-25 15:21:38
 */
public class ViewCacheUtil {
    /**
     * 每30秒执行一次
     */
    public void executeFiveMinutes() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        long fiveMinutes = 30 * 1000;
        long initDelay = getTimeMillis("14:00:00") - System.currentTimeMillis();
        initDelay = initDelay > 0 ? initDelay : fiveMinutes + initDelay;

        executor.scheduleAtFixedRate(() -> {

            // 每30秒执行一次 内存
            System.out.println("查看内存状态~");
            MahjongTablesCacheBean.getInstance().userMahjongRuleCache();
            MahjongTablesCacheBean.getInstance().userMahjongTable();
            CopyOnWriteArrayList<MahjongTableBean> guobiao = MahjongTableCache.getInstance().getMahjongTables("guobiao");
            CopyOnWriteArrayList<MahjongTableBean> guangdong = MahjongTableCache.getInstance().getMahjongTables("guangdong");
            CopyOnWriteArrayList<MahjongTableBean> sichuan = MahjongTableCache.getInstance().getMahjongTables("sichuan");

            System.out.println(guobiao.toString());
            System.out.println(guangdong.toString());
            System.out.println(sichuan.toString());

            MahjongGamesCacheBean.getInstance().getCache();

        }, initDelay, fiveMinutes, TimeUnit.MILLISECONDS);
    }




    /**
     * 获取指定时间对应的毫秒数
     *
     * @param time
     *            "HH:mm:ss"
     * @return
     */
    private long getTimeMillis(String time) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
            DateFormat dayFormat = new SimpleDateFormat("yy-MM-dd");
            Date curDate = dateFormat.parse(dayFormat.format(new Date()) + " " + time);
            return curDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void main(String[] args) {
        System.out.println(3/2);
    }
}
