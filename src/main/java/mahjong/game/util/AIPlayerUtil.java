package mahjong.game.util;

import mahjong.core.common.CmdsResponseBody;
import mahjong.core.model.UserModel;
import mahjong.game.cache.GameCache;
import mahjong.game.cache.bean.FuluCacheBean;
import mahjong.game.cache.bean.FuluTypeCacheBean;
import mahjong.game.cache.bean.MahjongGameCacheBean;
import mahjong.game.cache.bean.MahjongPlayerCacheBean;
import mahjong.util.GuideUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author muyi
 * @description:
 * @date 2020-11-12 20:46:29
 */
public class AIPlayerUtil {

    private final static GameCache guoBiaoCache;
    private final static UserModel userModel;
    private final static Logger log = Logger.getLogger(AIPlayerUtil.class.getName());

    static {
        guoBiaoCache = GameCache.getInstance();
        userModel = UserModel.getInstance();

    }

    /**
     * 人机摸牌
     *
     * @param zhuangId
     */
    public static void robotAction(String zhuangId) {
        MahjongGameCacheBean mahjongTable = guoBiaoCache.getMahjongGame(zhuangId);

        if (mahjongTable == null) {
            return;
        }
        int currentOutTilePlayer = mahjongTable.getCurrentOutTilePlayer();
        ArrayList<String> playersId = mahjongTable.getPlayersId();
        MahjongPlayerCacheBean player = mahjongTable.getPlayers().get(playersId.get(currentOutTilePlayer));
        String robotId = player.getUserId();

        // 判断牌堆是否已经摸完
        if (mahjongTable.isAllTilesDraw()) {
        } else {

            // 机器人摸牌
            String tileStr = mahjongTable.playerDrawTile(robotId);
            List<CmdsResponseBody> drawBody = getNewPlayerBody(mahjongTable);
            List<String> drawMessages = drawBody.get(0).getMessages();
            log.info("机器人(等级" + player.getAILevel() + ")" + robotId + "摸牌：" + tileStr);
            log.info("机器人(等级" + player.getAILevel() + ")" + robotId + "手牌(" + player.getPlayerTiles().size() + ")：" + MahjongUtil.printTiles(player.getPlayerTiles()));
            drawMessages.add("玩家：" + robotId + "摸牌，正在等待出牌");
            userModel.writeToUser(drawBody);

            if (MahjongUtil.canHu(player.getPlayerTiles())) {
                // 机器人和牌
                MahjongUtil.playerHuTiles(mahjongTable, robotId, getNewPlayerBody(mahjongTable));
            } else if (MahjongUtil.canAnGang(player.getPlayerTiles()) != 0) {
                // 判断是否能暗杠，可以杠默认直接暗杠
                mahjongTable.playerAnGangSuccess(player.getUserId(), MahjongUtil.canAnGang(player.getPlayerTiles()));
                List<CmdsResponseBody> newPlayerBody = getNewPlayerBody(mahjongTable);
                newPlayerBody.get(0).getMessages().add("用户：" + robotId + "暗杠，从牌底摸牌！！正在等待出牌~");
                newPlayerBody.get(0).getMessages().add(GuideUtil.dividingLine());
                userModel.writeToUser(newPlayerBody);
                robotOutTile(player, mahjongTable);
            } else {
                // 没有和牌，继续出牌
                robotOutTile(player, mahjongTable);
            }
        }
    }

    /**
     * 判断上个出牌玩家是否机器人
     *
     * @param mahjongTable
     * @return
     */
    private static boolean isShangRobot(MahjongGameCacheBean mahjongTable) {
        ArrayList<String> playersId = mahjongTable.getPlayersId();
        int currentOutTilePlayer = mahjongTable.getCurrentOutTilePlayer();
        ArrayList<String> playersHu = mahjongTable.getPlayersHu();
        int i = 0;
        do {
            if (currentOutTilePlayer == 0) {
                currentOutTilePlayer = 3;
            } else {
                currentOutTilePlayer--;
            }
            i++;
        } while (playersHu.size() != 0 && playersHu.contains(playersId.get(currentOutTilePlayer)) && i < 4);

        return mahjongTable.getPlayers().get(playersId.get(currentOutTilePlayer)).isAIPlayer();
    }

    /**
     * 人机出牌策略
     *
     * @param player
     * @param mahjongTable
     */
    public static void robotOutTile(MahjongPlayerCacheBean player, MahjongGameCacheBean mahjongTable) {
        String robotId = player.getUserId();
        // 根据出牌牌策略，获取要出的牌
        int outTile = AIPlayerGetOutTiles(mahjongTable, player);
        log.info("机器人(等级" + player.getAILevel() + ")" + robotId + "出牌：" + MahjongUtil.printTile(outTile));

        // 机器人出牌
        List<CmdsResponseBody> outOutBody = getNewPlayerBody(mahjongTable);
        List<String> outTileMessages = outOutBody.get(0).getMessages();
        if (outTile > 60) {
            // 出花牌
            mahjongTable.outHuaTiles(robotId, outTile);
            outTileMessages.add("玩家：" + robotId + " 打出了一张花牌：" + MahjongUtil.printTile(outTile));
            outTileMessages.add("== 玩家：" + robotId + " 从牌尾摸一张牌！！");
            outTileMessages.add(GuideUtil.dividingLine());
            userModel.writeToUser(outOutBody);
            // 继续出牌
            robotOutTile(player, mahjongTable);
        } else {
            // 出正常牌
            mahjongTable.playerOutTile(robotId, outTile);
            outTileMessages.add("玩家：" + robotId + " 出牌：" + MahjongUtil.printTile(outTile));
            if (mahjongTable.isAllTilesDraw()) {
                MahjongUtil.printAllPlayerTiles(mahjongTable, outTileMessages);
                MahjongUtil.backToRoom(mahjongTable);
                outTileMessages.add("== 牌堆已全部摸起，本局游戏结束，返回牌桌！！");
            } else {
                outTileMessages.add("== 下家为玩家：" + mahjongTable.getPlayersId().get(mahjongTable.getCurrentOutTilePlayer()));
            }
            outTileMessages.add(GuideUtil.dividingLine());
            userModel.writeToUser(outOutBody);

            MahjongUtil.noticeNextPlayer(mahjongTable);
        }
    }

    /**
     * AI思考函数，适用于所以需要打出一张牌的情况，包括正常摸牌，吃碰杠后打牌
     * 算法：遍历所有能打出的牌，看打出哪张之后得分最高，就打那种
     *
     * @param player
     * @return 返回想要打出牌的索引
     */
    private static int AIPlayerGetOutTiles(MahjongGameCacheBean mahjongTable, MahjongPlayerCacheBean player) {
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) player.getPlayerTiles().clone();
        Collections.sort(inspector);
        // 优先出花牌                                                           
        if (inspector.get(inspector.size() - 1) > 60) {
            return inspector.get(inspector.size() - 1);
        }
        // 分离AI等级随即返回手牌
        int randomNum = (int) (1 + Math.random() * (3 - 1 + 1));
        if (player.getAILevel() >= randomNum) {
            double highestScore = 0.0;
            int bestMoveIndex = -1;
            for (int i = 0; i < inspector.size(); i++) {
                //假想打出这张牌
                Integer current = inspector.get(i);
                inspector.remove(current);
                double score = evaluate(inspector, mahjongTable, player);
                if (score > highestScore) {
                    highestScore = score;
                    bestMoveIndex = i;
                }
                //把假想打出去的这张牌加回来
                inspector.add(current);
                Collections.sort(inspector);
            }
            return player.getPlayerTiles().get(bestMoveIndex);

        } else {
            int randomTile = (int) (Math.random() * inspector.size());
            return inspector.get(randomTile);

        }

    }

    /**
     * 计算出牌后牌型的得分
     * 算法简介：先判断是否听，如果听，直接算听得分，否则算x1听...以此类推
     * 关于得分倍率：保证上一级的得分一定高于下一级的最高得分，所以可以只算上一级，优化计算量
     * 由于基础得分以100为单位，最高分500；所以x3听的倍率是500
     * 由于x3听最多有136张牌，简单记为150，所以x2听得分倍率500*150=75,000
     * 以此类推，x1听：11,250,000
     * 听：1,687,500,000
     *
     * @param pai
     * @return
     */
    public static double evaluate(ArrayList<Integer> pai, MahjongGameCacheBean mahjongTable, MahjongPlayerCacheBean player) {

        double res = 0.0;
        int tingNum = ting(pai, mahjongTable);
        // 已经听牌了，得分是听牌数*倍率
        if (tingNum != 0) {
            res = 11250000 * tingNum;
            return res;
        } else {
            int x1tingNum = x1ting(pai, mahjongTable);
            // 差1听
            if (x1tingNum != 0) {
                res = 75000 * x1tingNum;
                return res;
            } else {
                // 差2听
                res = calculateRegular(pai, player);
                return res;
            }
        }
    }

    /**
     * 判断是否听牌
     * 返回值是int，代表听牌的张数（不是种类）：比如双面听25万，则返回场上未出现的25万有多少张
     * 听牌，就是再来一张加入手牌就胡的状态，遍历所有的牌，假想把他们加入手牌，如果胡了，那这一种牌就是听的牌之一，记录这种牌还有多少
     *
     * @param pai
     * @param mahjongTable
     * @return
     */
    public static int ting(ArrayList<Integer> pai, MahjongGameCacheBean mahjongTable) {

        int res = 0;
        int[] effectiveCode = mahjongTable.getEffectiveCode();
        for (int i = 0; i < effectiveCode.length; i++) {
            if (MahjongUtil.canHu(effectiveCode[i], pai)) {
                res += mahjongTable.getDiscardCounnt().get(effectiveCode[i]) == null ? 4 : 4 - mahjongTable.getDiscardCounnt().get(effectiveCode[i]);
                // discardCount记录的是明牌，剩下还能打出的是4-这个值
            }
        }
        return res;
    }

    /**
     * 判断是否差一张就听牌，返回多少张牌能让你进入听牌状态
     * 外循环遍历手牌，删除当前遍历的手牌，内循环放入所有有效牌，这之后的手牌如果是听牌状态，则之前就是差1听状态
     * 记录下这种有效牌还剩的张数
     *
     * @param pai
     * @param mahjongTable
     * @return
     */
    public static int x1ting(ArrayList<Integer> pai, MahjongGameCacheBean mahjongTable) {
        int res = 0;
        int[] effectiveCode = mahjongTable.getEffectiveCode();
        HashSet<Integer> helpfulTile = new HashSet<Integer>();
        for (int i = 0; i < pai.size(); i++) {
            @SuppressWarnings("unchecked")
            ArrayList<Integer> inspector = (ArrayList<Integer>) pai.clone();
            // 删除当前遍历的牌
            inspector.remove(pai.get(i));
            for (int k : effectiveCode) {
                // 添加一张有效牌
                inspector.add(k);
                // 这张牌可以使当前状态进入听
                if (ting(inspector, mahjongTable) != 0) {
                    // 把该牌加入有用牌集合，最后统计总数
                    helpfulTile.add(k);
                }
                // 添加的这张牌检查完毕，撤销这个动作
                inspector.remove(new Integer(k));
            }
        }
        for (int tile : helpfulTile) {
            res += mahjongTable.getDiscardCounnt().get(tile) == null ? 4 : 4 - mahjongTable.getDiscardCounnt().get(tile);
        }
        return res;
    }

    /**
     * 计算单张得分函数
     * 单张的判定方式：不成对儿、刻的字牌都是单张，每个记-2分
     * 和左右间隔两张及以上的万条筒也是单张，每张记-1.x分，其中x是该牌数值和5的距离
     * 比如单牌一万记-1.4分，因为1到5差4；单牌5万记-1.0分，以此类推
     *
     * @param pai
     * @return
     */
    public static double calculateSingle(ArrayList<Integer> pai) {
        double res = 0.0;
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) pai.clone();
        Collections.sort(inspector);
        for (int i = 0; i < inspector.size(); i++) {
            int current = inspector.get(i);
            // 大于40说明是字牌
            if (current >= 40) {
                // 遍历一遍，看看有没有一样的，没有说明是单张
                int count = 0;
                for (Integer integer : inspector) {
                    if (integer.equals(current)) {
                        count++;
                    }
                }
                // 如果singe还是true，说明是单张，要扣分
                boolean single = !(count > 1);
                if (single) {
                    res -= 2.0;
                }
            } else {
                // 不是字牌，用万条筒的单牌判断方式
                // 1、9单独判断，因为1、9往左右两个就越界了
                boolean single = true;
                if (current % 10 == 1) {
                    single = !(inspector.contains(current + 1) || inspector.contains(current + 2));
                } else if (current % 10 == 9) {
                    single = !(inspector.contains(current - 1) || inspector.contains(current - 2));
                } else {
                    single = !(inspector.contains(current + 1) || inspector.contains(current + 2)
                            || inspector.contains(current - 1) || inspector.contains(current - 2));
                }
                //如果是单牌，要计分
                if (single) {
                    double distance = Math.abs(current % 10 - 5);
                    res -= 1 + distance / 10;
                }
            }
        }
        return res;
    }

    /**
     * 对二连算分
     *
     * @param pai
     * @return
     */
    public static double calculateErlian(ArrayList<Integer> pai) {
        // 每个二连记+10分
        return 10 * countErlian(pai);
    }

    /**
     * 对对子计分
     *
     * @param pai
     * @return
     */
    public static double calculatePair(ArrayList<Integer> pai) {
        // 首个对子记+100分，剩余对子+10每个
        int count = countPair(pai);
        if (count == 0) {
            return 0;
        }
        return 100 + 10 * (count - 1);
    }

    /**
     * 对空1算分
     *
     * @param pai
     * @return
     */
    public static double calculateKong1(ArrayList<Integer> pai) {
        // 每个空1记+5分
        return 5 * countKong1(pai);
    }

    /**
     * 对顺子算分
     *
     * @param pai
     * @return
     */
    public static double calculateShunzi(ArrayList<Integer> pai) {
        // 每个顺子+100分
        return 100 * countShunzi(pai);
    }

    /**
     * 对刻子算分
     *
     * @param pai
     * @return
     */
    public static double calculateKezi(ArrayList<Integer> pai) {
        // 每个刻子+100分
        return 100 * countKezi(pai);
    }

    /**
     * 对副露算分
     *
     * @param player
     * @return
     */
    public static double calculateFulu(MahjongPlayerCacheBean player) {
        // 每套副露+100分
        return 100 * countFulu(player);
    }

    /**
     * 对离胡牌还很远的牌型，算基本得分(总分)
     *
     * @param pai
     * @param player
     * @return
     */
    public static double calculateRegular(ArrayList<Integer> pai, MahjongPlayerCacheBean player) {
        // 也就是对上述计分方式做总和
        return calculateShunzi(pai) + calculateKezi(pai) + calculateFulu(player) + calculatePair(pai) +
                calculateErlian(pai) + calculateKong1(pai) + calculateSingle(pai);
    }

    /**
     * 数给定的手牌、虚拟手牌中有多少个三连（顺子）
     * 判断逻辑：先排序，再遍历，第i张若为n，查看是否存在n+1和n+2
     * 数完后删除当前发现的顺子，比如2345万算一个顺，要么算234要么算345
     *
     * @param pai
     * @return
     */
    public static int countShunzi(ArrayList<Integer> pai) {
        int res = 0;
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) pai.clone();
        Collections.sort(inspector);
        for (int i = 0; i < inspector.size(); i++) {
            int current = inspector.get(i);
            // 同时存在N,N+1,N+2;说明这是个顺子
            if (inspector.contains(current + 1) && inspector.contains(current + 2)) {
                res++;
                inspector.remove(Integer.valueOf(current));
                inspector.remove(Integer.valueOf(current + 1));
                inspector.remove(Integer.valueOf(current + 2));
                // 中途删除过数据，角标回跳，避免检查遗漏或重复
                i--;
            }
        }
        return res;
    }

    /**
     * 数给定的手牌、虚拟手牌中有多少个三同样牌（刻子）
     * 判断逻辑：复制，排序，遍历，看当前牌的后两张和当前是否一样
     * 注意四张一样的算一个刻子，因为开杠后这只是一套成型的
     *
     * @param pai
     * @return
     */
    public static int countKezi(ArrayList<Integer> pai) {
        int res = 0;
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) pai.clone();
        Collections.sort(inspector);
        for (int i = 0; i < inspector.size() - 2; i++) {
            Integer current = inspector.get(i);
            if (current.equals(inspector.get(i + 1)) && current.equals(inspector.get(i + 2))) {
                res++;
                inspector.remove(current);
                inspector.remove(current);
                inspector.remove(current);
                // 中途删除过数据，角标回跳，避免检查遗漏或重复
                i--;
            }
        }
        return res;
    }

    /**
     * /数出自己的副露中有几套成型牌组，不论顺子还是刻子
     *
     * @param player
     * @return
     */
    public static int countFulu(MahjongPlayerCacheBean player) {
        return player.getFuluTiles().size();
    }

    /**
     * 数有多少对儿，刻子算一对儿，四张算两对儿
     *
     * @param pai
     * @return
     */
    public static int countPair(ArrayList<Integer> pai) {
        int res = 0;
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) pai.clone();
        Collections.sort(inspector);
        for (int i = 0; i < inspector.size() - 1; i++) {
            Integer current = inspector.get(i);
            if (current.equals(inspector.get(i + 1))) {
                res++;
                inspector.remove(current);
                inspector.remove(current);
                // 中途删除过数据，角标回跳，避免检查遗漏或重复
                i--;
            }
        }
        return res;
    }

    /**
     * 数有多少二连，比如45万
     * 不算顺子，比如345万不含二连
     * 判断逻辑：遍历，含N和N+1，但是不含N+2和N-1
     *
     * @param pai
     * @return
     */
    public static int countErlian(ArrayList<Integer> pai) {
        int res = 0;
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) pai.clone();
        Collections.sort(inspector);
        removeShunzi(inspector);
        for (int i = 0; i < inspector.size(); i++) {
            int current = inspector.get(i);
            if (inspector.contains(current + 1) &&
                    !inspector.contains(current + 2) && !inspector.contains(current - 1)) {
                res++;
            }
        }
        return res;
    }

    /**
     * 数有多少中间嵌张的牌型，比如24万
     * 判断逻辑：删除顺子，遍历，含N和N+2，不含N+1
     * 字牌跳过，字牌不连
     *
     * @param pai
     * @return
     */
    public static int countKong1(ArrayList<Integer> pai) {

        int res = 0;
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) pai.clone();
        Collections.sort(inspector);
        removeShunzi(inspector);
        for (int i = 0; i < inspector.size(); i++) {
            int current = inspector.get(i);
            if (current > 40) {
                continue;
            }
            if (inspector.contains(current + 2) && !inspector.contains(current + 1)) {
                res++;
            }
        }
        return res;
    }

    /**
     * 去除成型顺子
     *
     * @param pai
     */
    private static void removeShunzi(ArrayList<Integer> pai) {
        for (int i = 0; i < pai.size(); i++) {
            int current = pai.get(i);
            // 同时存在N,N+1,N+2;说明这是个顺子
            if (pai.contains(current + 1) && pai.contains(current + 2)) {
                // 剔除这个顺子，继续检查
                pai.remove(Integer.valueOf(current));
                pai.remove(Integer.valueOf(current + 1));
                pai.remove(Integer.valueOf(current + 2));
                // 中途删除过数据，角标回跳，避免检查遗漏或重复
                i--;
            }
        }
    }

    /**
     * 返回给玩家的输出列表
     *
     * @param mahjongGame
     * @return
     */
    public static ArrayList<CmdsResponseBody> getNewPlayerBody(MahjongGameCacheBean mahjongGame) {
        @SuppressWarnings("unchecked")
        ArrayList<String> playersId = (ArrayList<String>) mahjongGame.getPlayersId().clone();
        for (MahjongPlayerCacheBean player : mahjongGame.getPlayers().values()) {
            if (player.isAIPlayer()) {
                playersId.remove(player.getUserId());
            }
        }

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();
        body.getUserIds().addAll(playersId);
        bodys.add(body);
        return bodys;
    }

    /**
     * 判断机器人是否能够吃杠碰
     *
     * @param zhuangId
     * @return 不能则返回空
     */
    public static ArrayList<String> canSpecialAction(String zhuangId) {
        ArrayList<String> actions = new ArrayList<String>();
        MahjongGameCacheBean mahjongTable = guoBiaoCache.getMahjongGame(zhuangId);
        if (mahjongTable == null) {
            return null;
        }
        int latestTileIndex = mahjongTable.getLatestTileIndex();
        // 广东牌只能自摸和牌
        if (!MahjongConstant.GUANGDONG.equals(mahjongTable.getMahjongRule())) {
            // 判断是否可以胡
            Iterator<MahjongPlayerCacheBean> playerHuIterator = getPlayer(mahjongTable);
            while (playerHuIterator.hasNext()) {
                MahjongPlayerCacheBean player = playerHuIterator.next();
                if (!player.isAIPlayer()) {
                    // 不是机器人
                    continue;
                } else {
                    // 判断是否能胡
                    if (MahjongUtil.canHu(latestTileIndex, player.getPlayerTiles())) {
                        // 可以胡
                        actions.add(MahjongConstant.HU);
                        actions.add(player.getUserId());
                        return actions;

                    }
                }
            }
        }

        // 判断是否可以杠碰
        Iterator<MahjongPlayerCacheBean> playerPengGangIterator = getPlayer(mahjongTable);
        while (playerPengGangIterator.hasNext()) {
            MahjongPlayerCacheBean player = playerPengGangIterator.next();
            if (!player.isAIPlayer()) {
                // 不是机器人
                continue;
            } else {
                // 判断是否能杠
                if (MahjongUtil.canGang(latestTileIndex, player.getPlayerTiles())) {
                    // 可以杠
                    // 等级3AI才可以思考
                    if (player.getAILevel() == 3) {
                        if (think_gang(latestTileIndex, player.getPlayerTiles(), mahjongTable, player)) {
                            // 确定杠
                            actions.add(MahjongConstant.GANG);
                            actions.add(player.getUserId());
                            return actions;
                        }
                    } else {
                        // 等级2AI才会杠
                        actions.add(MahjongConstant.GANG);
                        actions.add(player.getUserId());
                        return actions;
                    }
                }
                // 判断是否能碰
                if (MahjongUtil.canPeng(latestTileIndex, player.getPlayerTiles())) {
                    // 可以碰
                    // 等级3AI才可以思考
                    if (player.getAILevel() == 3) {
                        if (think_peng(latestTileIndex, player.getPlayerTiles(), mahjongTable, player)) {
                            // 确定碰
                            actions.add(MahjongConstant.PENG);
                            actions.add(player.getUserId());
                            return actions;
                        }
                    } else {
                        // 等级2AI才会碰
                        actions.add(MahjongConstant.PENG);
                        actions.add(player.getUserId());
                        return actions;
                    }
                }
            }
        }

        // 国标麻将才能吃牌
        String mahjongRule = mahjongTable.getMahjongRule();
        if (MahjongConstant.GUOBIAO.equals(mahjongRule)) {
            // 判断是否可以吃
            Iterator<MahjongPlayerCacheBean> playerChiIterator = getPlayer(mahjongTable);
            while (playerChiIterator.hasNext()) {
                MahjongPlayerCacheBean player = playerChiIterator.next();
                if (!player.isAIPlayer()) {
                    // 不是机器人
                    continue;
                } else {
                    // 判断是否是上家且能吃
                    if (isShangJia(player.getUserId(), mahjongTable)) {

                        if (MahjongUtil.canChi(latestTileIndex, player.getPlayerTiles())[0]) {
                            // 可以吃，判断是否要吃
                            int chiType = think_chi(latestTileIndex, player.getPlayerTiles(), mahjongTable, player);
                            if (chiType != 0) {
                                // 确定吃牌
                                actions.add(MahjongConstant.CHI);
                                actions.add(player.getUserId());
                                actions.add(chiType + "");
                                return actions;
                            }
                        }
                    }
                }
            }
        }
        // 没有动作，返回空
        return null;
    }

    /**
     * 返回玩家的迭代对象
     *
     * @param mahjongTable
     * @return
     */
    private static Iterator<MahjongPlayerCacheBean> getPlayer(MahjongGameCacheBean mahjongTable) {
        ArrayList<MahjongPlayerCacheBean> players = new ArrayList<>();
        int currentPlayerId = mahjongTable.getCurrentOutTilePlayer();
        for (int i = 0; i < 3; i++) {
            MahjongPlayerCacheBean player = mahjongTable.getPlayers().get(mahjongTable.getPlayersId().get(currentPlayerId));
            if (!mahjongTable.getPlayersHu().contains(player.getUserId())) {
                players.add(player);
            }
            if (currentPlayerId >= 3) {
                currentPlayerId = 0;
            } else {
                currentPlayerId++;
            }
        }
        return players.iterator();
    }

    /**
     * 判断是否上家打出的牌
     *
     * @param userId
     * @return
     */
    private static boolean isShangJia(String userId, MahjongGameCacheBean mahjongTable) {
        String currentPlayer = mahjongTable.getPlayersId().get(mahjongTable.getCurrentOutTilePlayer());
        return userId.equals(currentPlayer);
    }

    /**
     * 思考要不要吃，怎么吃
     *
     * @param tile
     * @return
     */
    public static int think_chi(int tile, ArrayList<Integer> playerTiles, MahjongGameCacheBean mahjongTable, MahjongPlayerCacheBean player) {
        boolean[] canChi = MahjongUtil.canChi(tile, playerTiles);
        double scoreNoChi = 0.0;
        double scoreZuoChi = 0.0;
        double scoreZhongChi = 0.0;
        double scoreYouChi = 0.0;
        scoreNoChi = evaluate(playerTiles, mahjongTable, player);
        //模拟左吃
        if (canChi[1]) {
            @SuppressWarnings("unchecked")
            ArrayList<Integer> inspector = (ArrayList<Integer>) playerTiles.clone();
            inspector.remove(new Integer(tile + 1));
            inspector.remove(new Integer(tile + 2));
            scoreZuoChi = bestScore(playerTiles, mahjongTable, player);
            if (scoreZuoChi < 500) {
                scoreZuoChi += 100;
            }
        }
        //中吃
        if (canChi[2]) {
            @SuppressWarnings("unchecked")
            ArrayList<Integer> inspector = (ArrayList<Integer>) playerTiles.clone();
            inspector.remove(new Integer(tile + 1));
            inspector.remove(new Integer(tile - 2));
            scoreZhongChi = bestScore(playerTiles, mahjongTable, player);
            if (scoreZhongChi < 500) {
                scoreZhongChi += 100;
            }
        }
        //右吃
        if (canChi[3]) {
            @SuppressWarnings("unchecked")
            ArrayList<Integer> inspector = (ArrayList<Integer>) playerTiles.clone();
            inspector.remove(new Integer(tile - 1));
            inspector.remove(new Integer(tile - 2));
            scoreYouChi = bestScore(playerTiles, mahjongTable, player);
            if (scoreYouChi < 500) {
                scoreYouChi += 100;
            }
        }
        double max = 0.0;
        max = Math.max(scoreNoChi, scoreZuoChi);
        max = Math.max(max, scoreZhongChi);
        max = Math.max(max, scoreYouChi);
        if (max == scoreNoChi) {
            return 0;
        } else if (max == scoreZuoChi) {
            return 1;
        } else if (max == scoreZhongChi) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * 思考要不要杠
     *
     * @param tile
     * @return
     */
    public static boolean think_gang(int tile, ArrayList<Integer> playerTiles, MahjongGameCacheBean mahjongTable, MahjongPlayerCacheBean player) {
        // 用前后得分比较决定要不要杠
        double currentScore = evaluate(playerTiles, mahjongTable, player);
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) playerTiles.clone();
        // 假想我杠这张牌，则手牌中的这个暗刻要去掉，变为副露
        inspector.remove(new Integer(tile));
        inspector.remove(new Integer(tile));
        inspector.remove(new Integer(tile));
        //考虑此时的评分
        double scoreAfterGang = evaluate(playerTiles, mahjongTable, player);
        // 这个评分需要加上几部分：开杠后副露里有杠得分，杠之后你多一次摸打机会、杠开机会等等
        // 补分原则：只有算分结果是基础牌型才要补分
        if (scoreAfterGang < 500) {
            scoreAfterGang += 100;
        }
        // 这50分一定要补，因为是摸打机会分
        scoreAfterGang += 50;
        // 如果之后分更高，就要杠
        if (scoreAfterGang >= currentScore) {
            return true;
        }
        return false;
    }

    /**
     * 思考要不要碰
     *
     * @param tile
     * @return
     */
    public static boolean think_peng(int tile, ArrayList<Integer> playerTiles, MahjongGameCacheBean mahjongTable, MahjongPlayerCacheBean player) {
        // 判断逻辑：前后分数比较
        double scoreNoPeng = evaluate(playerTiles, mahjongTable, player);
        // 思考碰之后的分数
        double scorePeng = 0.0;
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) playerTiles.clone();
        // 碰之后，这个对子转换为副露，还要打出一张牌
        inspector.remove(new Integer(tile));
        inspector.remove(new Integer(tile));
        // 模拟打出一张牌
        for (int i = 0; i < inspector.size(); i++) {
            Integer current = inspector.get(i);
            inspector.remove(current);
            double score = evaluate(playerTiles, mahjongTable, player);
            if (score > scorePeng) {
                scorePeng = score;
            }
            inspector.add(current);
            Collections.sort(inspector);
        }
        // 还要补上副露里刻子的100分
        if (scorePeng < 500) {
            scorePeng += 100;
        }
        if (scorePeng >= scoreNoPeng) {
            return true;
        }
        return false;
    }

    /**
     * 模拟所有能打出的牌，返回打出后能得的最高分
     *
     * @param playerTiles
     * @return
     */
    private static double bestScore(ArrayList<Integer> playerTiles, MahjongGameCacheBean mahjongTable, MahjongPlayerCacheBean player) {
        double bestScore = 0.0;
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) playerTiles.clone();
        for (int i = 0; i < inspector.size(); i++) {
            Integer current = inspector.get(i);
            inspector.remove(current);
            double score = evaluate(playerTiles, mahjongTable, player);
            if (score > bestScore) {
                bestScore = score;
            }
            inspector.add(current);
            Collections.sort(inspector);
        }
        return bestScore;
    }

    /**
     * AI胡吃杠碰
     *
     * @param zhuangId
     * @param action
     */
    public static void robotSpecialAction(String zhuangId, ArrayList<String> action) {

        MahjongGameCacheBean mahjongTable = guoBiaoCache.getMahjongGame(zhuangId);
        switch (action.get(0)) {
            case MahjongConstant.HU:
                robotHuAction(mahjongTable, action);
                break;
            case MahjongConstant.PENG:
                robotPengAction(mahjongTable, action);
                break;
            case MahjongConstant.GANG:
                robotGangAction(mahjongTable, action);
                break;
            case MahjongConstant.CHI:
                robotChiAction(mahjongTable, action);
                break;
            default:
                System.out.println("不该进入这个Action分支，出错！！");
                break;
        }
    }

    /**
     * AI吃牌
     *
     * @param mahjongTable
     * @param action
     */
    private static void robotChiAction(MahjongGameCacheBean mahjongTable, ArrayList<String> action) {
        String playerId = action.get(1);
        int latestTileIndex = mahjongTable.getLatestTileIndex();
        FuluCacheBean fulu = null;
        int[] fuluTiles = new int[3];
        if ("1".equals(action.get(2))) {
            // 左吃
            fuluTiles = new int[]{latestTileIndex + 1, latestTileIndex + 2, latestTileIndex};
        } else if ("2".equals(action.get(2))) {
            // 中吃
            fuluTiles = new int[]{latestTileIndex - 1, latestTileIndex + 1, latestTileIndex};
        } else if ("3".equals(action.get(2))) {
            // 右吃
            fuluTiles = new int[]{latestTileIndex - 2, latestTileIndex - 1, latestTileIndex};
        } else {
            System.out.println("不该进入这个吃牌分支，出错！！");
        }
        FuluCacheBean fuluCacheBean = new FuluCacheBean(FuluTypeCacheBean.CHI, fuluTiles, 3, playerId);
        mahjongTable.playerChiSuccess(playerId, fuluCacheBean);
        MahjongPlayerCacheBean player = mahjongTable.getPlayers().get(playerId);

        List<CmdsResponseBody> newPlayerBody = getNewPlayerBody(mahjongTable);
        List<String> messages = newPlayerBody.get(0).getMessages();
        messages.add("玩家：" + playerId + " 吃牌！！！ 副露牌：" + MahjongUtil.printTiles(MahjongUtil.getNewArr(fuluTiles)));
        afterAIAction(mahjongTable, playerId, player, newPlayerBody, messages);
        log.info("玩家：" + playerId + " 吃牌！！！ 副露牌：" + MahjongUtil.printTiles(MahjongUtil.getNewArr(fuluTiles)));
    }

    /**
     * AI杠牌
     *
     * @param mahjongTable
     * @param action
     */
    private static void robotGangAction(MahjongGameCacheBean mahjongTable, ArrayList<String> action) {
        String playerId = action.get(1);
        mahjongTable.playerGangSuccess(playerId);
        MahjongPlayerCacheBean player = mahjongTable.getPlayers().get(playerId);

        List<CmdsResponseBody> newPlayerBody = getNewPlayerBody(mahjongTable);
        List<String> messages = newPlayerBody.get(0).getMessages();
        messages.add("玩家：" + playerId + " 杠牌！！！ 副露牌：" + MahjongUtil.printTiles(player.getFuluTiles().get(player.getFuluTiles().size() - 1).tileNumbers));
        afterAIAction(mahjongTable, playerId, player, newPlayerBody, messages);
        log.info("玩家：" + playerId + " 杠牌！！！ 副露牌：" + MahjongUtil.printTiles(player.getFuluTiles().get(player.getFuluTiles().size() - 1).tileNumbers));
    }

    /**
     * AI碰牌
     *
     * @param mahjongTable
     * @param action
     */
    private static void robotPengAction(MahjongGameCacheBean mahjongTable, ArrayList<String> action) {
        String playerId = action.get(1);
        mahjongTable.playerPengSuccess(playerId);
        MahjongPlayerCacheBean player = mahjongTable.getPlayers().get(playerId);

        List<CmdsResponseBody> newPlayerBody = getNewPlayerBody(mahjongTable);
        List<String> messages = newPlayerBody.get(0).getMessages();
        messages.add("玩家：" + playerId + " 碰牌！！！ 副露牌：" + MahjongUtil.printTiles(player.getFuluTiles().get(player.getFuluTiles().size() - 1).tileNumbers));
        afterAIAction(mahjongTable, playerId, player, newPlayerBody, messages);
        log.info("玩家：" + playerId + " 碰牌！！！ 副露牌：" + MahjongUtil.printTiles(player.getFuluTiles().get(player.getFuluTiles().size() - 1).tileNumbers));
    }

    /**
     * 在吃杠碰之后
     *
     * @param mahjongTable
     * @param playerId
     * @param player
     * @param newPlayerBody
     * @param messages
     */
    private static void afterAIAction(MahjongGameCacheBean mahjongTable, String playerId, MahjongPlayerCacheBean player, List<CmdsResponseBody> newPlayerBody, List<String> messages) {
        messages.add("== 正在等待玩家：" + playerId + " 出牌");
        // 控制流程速度

        MahjongGameThreadPools.schedule(()->{
            userModel.writeToUser(newPlayerBody);
        },1,TimeUnit.SECONDS);
        MahjongGameThreadPools.schedule(()->{
            robotOutTile(player, mahjongTable);
        },2,TimeUnit.SECONDS);
    }

    /**
     * AI和牌
     *
     * @param mahjongTable
     * @param action
     */
    private static void robotHuAction(MahjongGameCacheBean mahjongTable, ArrayList<String> action) {
        String playerId = action.get(1);
        List<CmdsResponseBody> newPlayerBody = getNewPlayerBody(mahjongTable);
        newPlayerBody.get(0).getMessages().add("玩家：" + playerId + "和牌了！！！");
        newPlayerBody.get(0).getMessages().add("== 玩家：" + playerId + "和牌了！！！");
        newPlayerBody.get(0).getMessages().add(GuideUtil.dividingLine());
        newPlayerBody.get(0).getMessages().add("== 玩家：" + playerId + "手牌：" + MahjongUtil.printTiles(mahjongTable.getPlayers().get(playerId).getPlayerTiles()));
        MahjongUtil.playerHuTiles(mahjongTable, playerId, newPlayerBody);
        log.info("玩家：" + playerId + "胡牌！！！ 手牌：" + MahjongUtil.printTiles(mahjongTable.getPlayers().get(playerId).getPlayerTiles()));
    }

    /**
     * 玩家辅助线程, 判断玩家手牌是否能吃碰杠，且给与玩家提示
     *
     * @param tileIndex
     * @param playerId
     * @param mahjongTable
     */
    public static void helpPlayer(int tileIndex, String playerId, MahjongGameCacheBean mahjongTable) {

        for (MahjongPlayerCacheBean player : mahjongTable.getPlayers().values()) {
            String actionPlayerId = player.getUserId();
            if (!player.isAIPlayer() && !playerId.equals(actionPlayerId) && !mahjongTable.getPlayersHu().contains(actionPlayerId)) {
                ArrayList<String> action = getPlayerHelperAction(tileIndex, player, mahjongTable);
                if (action != null) {
                    // 输出玩家手牌
                    userModel.writeToUser(actionPlayerId, "== 手牌(" + player.getPlayerTiles().size() + ")： " + MahjongUtil.printTiles(player.getPlayerTiles()));
                    userModel.writeToUser(actionPlayerId, GuideUtil.dividingLine());
                    switch (action.get(0)) {
                        case MahjongConstant.HU:
                            userModel.writeToUser(actionPlayerId, "== 胡牌条件满足！你可以使用命令 hu 和牌！！！");
                            break;
                        case MahjongConstant.PENG:
                            userModel.writeToUser(actionPlayerId, "== 碰牌条件满足！你可以使用命令 peng 碰牌！！！");
                            break;
                        case MahjongConstant.GANG:
                            userModel.writeToUser(actionPlayerId, "== 杠牌条件满足！你可以使用命令 gang 杠牌！！！");
                            break;
                        case MahjongConstant.CHI:
                            if ("1".equals(action.get(2))) {
                                userModel.writeToUser(actionPlayerId, "== 吃牌条件满足！你可以使用命令 chi " + (tileIndex + 1) + " " + (tileIndex + 2) + " 吃牌！！！");
                            } else if ("2".equals(action.get(2))) {
                                userModel.writeToUser(actionPlayerId, "== 吃牌条件满足！你可以使用命令 chi " + (tileIndex - 1) + " " + (tileIndex + 1) + " 吃牌！！！");
                            } else if ("3".equals(action.get(2))) {
                                userModel.writeToUser(actionPlayerId, "== 吃牌条件满足！你可以使用命令 chi " + (tileIndex - 2) + " " + (tileIndex - 1) + " 吃牌！！！");
                            }
                            break;
                        default:
                            System.out.println("不该进入这个Action分支，出错！！");
                            break;
                    }
                    userModel.writeToUser(actionPlayerId, GuideUtil.dividingLine());
                }
            }
        }
    }

    /**
     * 判断玩家的牌是否满足胡吃碰杠的条件
     *
     * @param tileIndex
     * @param player
     * @param mahjongTable
     * @return
     */
    private static ArrayList<String> getPlayerHelperAction(int tileIndex, MahjongPlayerCacheBean player, MahjongGameCacheBean mahjongTable) {
        String playerId = player.getUserId();
        String mahjongRule = mahjongTable.getMahjongRule();
        ArrayList<Integer> playerTiles = player.getPlayerTiles();
        ArrayList<String> action = new ArrayList<String>();

        if (!MahjongConstant.GUANGDONG.equals(mahjongRule) && MahjongUtil.canHu(tileIndex, playerTiles)) {
            action.add(MahjongConstant.HU);
            action.add(playerId);
        } else if (MahjongUtil.canGang(tileIndex, playerTiles)) {
            action.add(MahjongConstant.GANG);
            action.add(playerId);
        } else if (MahjongUtil.canPeng(tileIndex, playerTiles)) {
            action.add(MahjongConstant.PENG);
            action.add(playerId);
        } else if (MahjongConstant.GUOBIAO.equals(mahjongRule) && isShangJia(playerId, mahjongTable) && MahjongUtil.canChi(tileIndex, playerTiles)[0]) {
            action.add(MahjongConstant.CHI);
            action.add(playerId);
            int chiType = think_chi(tileIndex, player.getPlayerTiles(), mahjongTable, player);
            if (chiType == 0) {
                return null;
            } else {
                action.add(chiType + "");
            }
        }
        if (action.size() == 0) {
            return null;
        } else {
            return action;
        }
    }

}
