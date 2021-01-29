package mahjong.util;



import mahjong.game.cache.GameCache;
import mahjong.game.cache.bean.MahjongGameCacheBean;
import mahjong.game.model.GameModel;
import mahjong.table.cache.bean.MahjongTableBean;
import mahjong.table.model.MahjongModel;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author muyi
 * @description: 用户界面引导工具
 * @date 2020-10-28 09:58:13
 */
public class GuideUtil {

    public static HashMap<String, Integer> userGuide = new HashMap<>();

    /**
     * 根据用户所在的界面添加对应的引导
     *
     * @param msg
     * @param userId
     */
    public static void setUserGuide(List<String> msg, String userId) {
        Integer guideLevel;
        if ("".equals(userId)) {
            guideLevel = 0;
        } else if (!userGuide.containsKey(userId)) {
            guideLevel = 0;
        } else {
            guideLevel = userGuide.get(userId);
        }

        if (guideLevel == 0) {
            guideRegister(msg);
        } else if (guideLevel == 1) {
            guideLogin(msg);
        } else if (guideLevel == 2) {
            guideMahjongHall(userId,msg);
        }else if (guideLevel == 3) {
            guideMahjongTable(userId,msg);
        }else if (guideLevel == 4) {
            guideGuobiaoMahjong(userId, msg);
        }

    }

    /**
     * 返回上一级引导
     * @param userId
     */
    public static void backPrevious(String userId){
        Integer guideNum = userGuide.get(userId);
        userGuide.put(userId,guideNum-1);
    }



    /**
     * 注册引导 0
     *
     * @param msg
     */
    public static void guideRegister(List<String> msg) {
        msg.add(dividingLine());
        msg.add("== 您可以继续以下操作：");
        msg.add("== 注册(没有账号注册一个)：register muyi 123456 ");
        msg.add("== 登录(登录你的账号)：login muyi 123456 ");
        msg.add(dividingLine());
    }

    /**
     * 主界面引导 1
     *
     * @param msg
     */
    public static void guideLogin(List<String> msg) {
        msg.add(dividingLine());
        msg.add("== 您可以继续以下操作：");
        msg.add("== 查看(查看个人信息)：view  ");
        msg.add("== 麻将游戏：选择你熟悉的麻将规则来游玩吧！！");
        msg.add("== 国标麻将大厅：mahjong guobiao");
        msg.add("== 广东麻将大厅：mahjong guangdong");
        msg.add("== 四川麻将大厅：mahjong sichuan");
        msg.add("== 登出(登出当前账号)：logout ");
        msg.add(dividingLine());
    }

    /**
     * 大厅引导 2
     *
     * @param msg
     */
    public static void guideMahjongHall(String userId, List<String> msg) {
        CopyOnWriteArrayList<MahjongTableBean> mahjongTables = MahjongModel.getInstance().getMahjongTablesByUserId(userId);
        msg.add(dividingLine());
        msg.add("== 下面为可加入的游戏牌桌：");
        if (mahjongTables.size() == 0) {
            msg.add("== 暂无可加入牌桌，自己创建一个吧！");
        } else {
            for (MahjongTableBean table : mahjongTables) {
                msg.add("== 牌桌名：" + table.getTableName() + "   房主：" + table.getTableOwner() + "   牌桌人数：" + table.getNumberofPlay());
            }
        }
        msg.add(dividingLine());
        msg.add("== 您可以继续以下操作：");
        msg.add("== 进入牌桌：join table  (牌桌不存在则创建牌桌)");
        msg.add("== 退出大厅：back ");
        msg.add(dividingLine());
    }

    /**
     * 牌桌内引导  3
     *
     * @param userId
     * @param msg
     */
    private static void guideMahjongTable(String userId, List<String> msg) {
        MahjongTableBean mahjongTable = MahjongModel.getInstance().getMahjongTableByUserId(userId);
        HashMap<String, Boolean> playsReady = mahjongTable.getPlaysReady();
        List<String> plays = mahjongTable.getPlays();
        String message = "牌桌玩家：";
        for (int i = 0; i < plays.size(); i++) {
            message += plays.get(i);
            if (mahjongTable.getTableOwner().equals(plays.get(i))){
                message += "(庄家)";
            }
            if (playsReady.get(plays.get(i))){
                message += "(已准备) ";
            }else {
                message += "(未准备) ";
            }
        }
        msg.add("== 牌桌人数："+plays.size()+"   "+message);
        msg.add(dividingLine());
        msg.add("== 开始游戏：牌桌内有满足四名玩家且有两名玩家发起准备后开始10秒倒计时，10秒后未准备玩家 ");
        msg.add("== 将自动被移出牌桌，玩家全部准备后自动开始游戏！！");
        msg.add("== 发起准备：ready (游戏结束后返回房间使用 ready 可开启下一局游戏)");
        msg.add("== 机器人：robot (了解可添加的机器人AI)");
        msg.add("== 添加机器人：add robot 1(房主权限)");
        msg.add("== 踢出牌桌：例：kick user(踢出玩家user) (房主权限) ");
        msg.add("== 退出游戏：back (退出当前游戏) ");
        msg.add(dividingLine());
    }


    /**
     * 国标麻将引导  4
     *
     * @param msg
     */
    public static List<String> guideGuobiaoMahjong(String userId, List<String> msg) {
        MahjongGameCacheBean mahjongGame = GameCache.getInstance().getMahjongGame(userId);
        GameModel.getInstance().printMahjongHandTiles(userId);
        ArrayList<String> playersId = mahjongGame.getPlayersId();
        if (msg.size() != 0){
            msg.add(dividingLine());
        }
        String message = "== 游戏玩家：";
        for (String playerId : playersId){
            if (playerId.equals(mahjongGame.getZhuangId())){
                message += playerId+"(庄家)   ";
            }else {
                message += playerId+"   ";
            }
        }
        msg.add(message);
        msg.add(dividingLine());
        msg.add("== 每个括号内的值为麻将牌的操作值，请使用括号内的值操作手~ ");
        if ("guobiao".equals(mahjongGame.getMahjongRule())){
            msg.add("== 吃牌、碰牌、杠牌都需要在玩家摸牌前执行，暗杠只能在自己出牌阶段执行~ ");
        }else {
            msg.add("== 碰牌、杠牌都需要在玩家摸牌前执行，暗杠只能在自己出牌阶段执行~ ");
        }
        msg.add("== 摸牌： draw   (轮到自己的回合，摸牌)");
        msg.add("== 出牌： out 11 (将手牌中的 一萬(11) 打出去)");
        if ("guobiao".equals(mahjongGame.getMahjongRule())){
            msg.add("== 吃牌： chi 12 13  (上家出 一萬(11)或四萬(14) 你打出的牌为 二萬(12) 三萬(13)可以跟最新的牌组成顺子)");
        }
        msg.add("== 碰牌： peng   (需要手牌中存在两张相同的牌可以与最新的牌组成一副刻子)");
        msg.add("== 明杠： gang   (其他玩家打出了自己手中有三张相同的牌)");
        msg.add("== 暗杠： gang 11   (出牌阶段，手牌中存在四张 一萬(11) 则可以暗杠)");
        if (!"guangdong".equals(mahjongGame.getMahjongRule())){
            msg.add("== 胡牌： hu   (别人打出的牌能够让你吃碰，且能组成3N+2的模式)");
        }
        msg.add("== 退出游戏：back  (退出当前游戏)");
        msg.add(dividingLine());

        return msg;
    }

    /**
     * 分割线
     * @return
     */
    public static String dividingLine(){
        return "== == == == == == == == == == == == == == == == == == == == == == == == == == == == == == ==";
    }

}
