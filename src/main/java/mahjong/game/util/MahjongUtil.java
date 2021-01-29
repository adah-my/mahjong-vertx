package mahjong.game.util;

import mahjong.core.common.CmdsResponseBody;
import mahjong.core.model.UserModel;
import mahjong.game.cache.GameCache;
import mahjong.game.cache.bean.FuluCacheBean;
import mahjong.game.cache.bean.FuluTypeCacheBean;
import mahjong.game.cache.bean.MahjongGameCacheBean;
import mahjong.game.cache.bean.MahjongPlayerCacheBean;
import mahjong.table.model.MahjongModel;
import mahjong.util.GuideUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author muyi
 * @description:
 * @date 2020-11-11 10:15:24
 */
public class MahjongUtil {

    private static final UserModel userModel;
    private static final GameCache gameCache;
    private final static Logger log = Logger.getLogger(MahjongUtil.class.getName());

    static {
        userModel = UserModel.getInstance();
        gameCache = GameCache.getInstance();
    }

    /**
     * 辅助函数，换位
     *
     * @param arr
     * @param i
     * @param j
     */
    public static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    /**
     * 格式化牌堆
     *
     * @param tiles
     */
    public static String printTiles(List<Integer> tiles) {

        StringBuilder tilesStr = new StringBuilder();
        for (Integer index : tiles) {
            tilesStr.append(MahjongConstant.GUOBIAO_PAI[index]).append("(").append(index).append(") ");
        }
        return tilesStr.toString();

    }

    /**
     * 格式化牌堆
     *
     * @param tiles
     */
    public static String printTiles(int[] tiles) {

        StringBuilder tilesStr = new StringBuilder();
        for (int i = 0; i < tiles.length; i++) {
            tilesStr.append(MahjongConstant.GUOBIAO_PAI[tiles[i]]).append("(").append(tiles[i]).append(") ");
        }
        return tilesStr.toString();

    }

    /**
     * 格式化单牌
     *
     * @param tileIndex
     */
    public static String printTile(int tileIndex) {
        return MahjongConstant.GUOBIAO_PAI[tileIndex] + "(" + tileIndex + ")";
    }

    /**
     * 判断三个号码是否为顺子
     *
     * @param tiles
     */
    public static boolean isChi(int[] tiles) {
        // 排除花牌影响
        if (tiles[2] > 60) {
            return false;
        }
        return Math.abs((tiles[0] - tiles[1]) * (tiles[1] - tiles[2]) * 2 + 1) == 3;
    }

    /**
     * 格式化输出玩家客户端
     *
     * @param userId
     * @param mahjongTable
     */
    public static void outputPlayersTiles(String userId, MahjongGameCacheBean mahjongTable) {
        MahjongPlayerCacheBean player = mahjongTable.getPlayers().get(userId);
        // 手牌
        userModel.writeToUser(userId, GuideUtil.dividingLine());
        userModel.writeToUser(userId, "== 手牌(" + player.getPlayerTiles().size() + ")： " + printTiles(player.getPlayerTiles()));
        // 副露牌
        StringBuilder fuluMessage = new StringBuilder("== 副露牌： ");
        if (player.getFuluTiles().size() == 0) {
            fuluMessage.append("无");
        } else {
            for (FuluCacheBean fulu : player.getFuluTiles()) {
                int[] arr = getNewArr(fulu.tileNumbers);
                fuluMessage.append(printTiles(arr));
            }
        }
        userModel.writeToUser(userId, fuluMessage.toString());
        if (MahjongConstant.GUOBIAO.equals(mahjongTable.getMahjongRule())) {
            // 花牌
            String huaMessage = "== 花牌： ";
            if (player.getHuaTiles().size() == 0) {
                huaMessage += "无";
            } else {
                huaMessage += printTiles(player.getHuaTiles());
            }
            userModel.writeToUser(userId, huaMessage);
        }
        userModel.writeToUser(userId, GuideUtil.dividingLine());
    }

    /**
     * 返回新数组
     *
     * @param tileNumbers
     * @return
     */
    public static int[] getNewArr(int[] tileNumbers) {
        int[] arr = new int[tileNumbers.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = tileNumbers[i];
        }
        Arrays.sort(arr);
        return arr;
    }

    /**
     * 判断是否胡牌，输入一张可能点炮或自摸的牌
     *
     * @param tile
     * @param playerHandTiles
     * @return
     */
    public static boolean canHu(int tile, ArrayList<Integer> playerHandTiles) {
        // 不能直接操作手牌，临时创建一个检查器
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) playerHandTiles.clone();
        // 把这张有可能使之胡的牌加入检查器
        inspector.add(tile);

        return canHu(inspector);
    }

    /**
     * 判断手牌是否胡牌
     *
     * @param playerHandTiles
     * @return
     */
    public static boolean canHu(ArrayList<Integer> playerHandTiles) {
        // 不能直接操作手牌，临时创建一个检查器
        @SuppressWarnings("unchecked")
        ArrayList<Integer> inspector = (ArrayList<Integer>) playerHandTiles.clone();
        // 把这张有可能使之胡的牌加入检查器
        Collections.sort(inspector);
        // 先检查特殊胡牌牌型
        if (huShiSanYao(inspector)) {
            return true;
        }
        if (huQiDuier(inspector)) {
            return true;
        }
        if (huStandard(inspector)) {
            return true;
        }
        // 全都不能胡，返回假
        return false;
    }

    /**
     * 十三幺（国士无双）：三种一九牌，七种字牌全都有，再加其中任意一张
     *
     * @param inspector
     * @return
     */
    public static boolean huShiSanYao(ArrayList<Integer> inspector) {
        // 不是十四张一定不能胡十三幺 再看包不包含一对儿一样的
        if (inspector.size() != 14 || !containsPair(inspector)) {
            return false;
        }
        // 再看是不是六种一九、七种字牌都有
        return inspector.contains(11) && inspector.contains(19)
                && inspector.contains(21) && inspector.contains(29)
                && inspector.contains(31) && inspector.contains(39)
                && inspector.contains(41) && inspector.contains(43) && inspector.contains(45) && inspector.contains(47)
                && inspector.contains(51) && inspector.contains(53) && inspector.contains(55);
    }

    /**
     * 判断七小对
     *
     * @param inspector
     * @return
     */
    public static boolean huQiDuier(ArrayList<Integer> inspector) {
        if (inspector.size() != 14) {
            return false;
        }
        // 胡七小对的逻辑：01张一样，23张一样...以此类推
        for (int i = 0; i < inspector.size(); i += 2) {
            if (!inspector.get(i).equals(inspector.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断标准胡牌，即3n+2的形式
     *
     * @param inspector
     * @return
     */
    public static boolean huStandard(ArrayList<Integer> inspector) {
        // 第一步，检查有没有一对做将，没有将一定不胡
        if (!containsPair(inspector)) {
            return false;
        }
        // 接下来，把一对将剔除出去，检查剩下的能不能全部组成顺子和刻子
        // 因为一副牌可能含有很多对儿，任何一对都有做将的可能，所以要把所有情况都讨论
        ArrayList<ArrayList<Integer>> insPairRemoved = removePair(inspector);
        // 对已经剔除了对儿的多种情况，分别检查
        // 检查逻辑如下：如果是空的，能胡；如果含刻子或顺子，剔除后再检查；如果剩下杂乱的，不能胡
        for (int i = 0; i < insPairRemoved.size(); i++) {
            ArrayList<Integer> remain = insPairRemoved.get(i);
            remain = removeShunKe(remain);
            if (remain.size() == 0) {
                return true;
            }
        }
        // 全部检查完毕还不能胡，返回不能胡
        return false;
    }

    /**
     * 检查牌中是否包含对子
     *
     * @param inspector
     * @return
     */
    private static boolean containsPair(ArrayList<Integer> inspector) {
        //检查一副牌是否含有至少一对（做将）
        //以这副牌已排序为前提，至少含有一对儿的条件就是第0张和第1张一样，或第1张和第2张一样...以此类推
        for (int i = 0; i < inspector.size() - 1; i++) {
            if (inspector.get(i).equals(inspector.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 帮助函数，找出所有可能的将，剔除出去，返回很多副牌
     *
     * @param inspector
     * @return
     */
    private static ArrayList<ArrayList<Integer>> removePair(ArrayList<Integer> inspector) {
        ArrayList<ArrayList<Integer>> res = new ArrayList<ArrayList<Integer>>();
        for (int i = 0; i < inspector.size() - 1; i++) {
            if (inspector.get(i).equals(inspector.get(i + 1))) {
                @SuppressWarnings("unchecked")
                ArrayList<Integer> insWithoutPair = (ArrayList<Integer>) inspector.clone();
                insWithoutPair.remove(i);
                //特别注意！这是Arraylist变迭代边删除时的常见问题！删除了一个元素，size就变了，而循环变量不作处理就会导致遍历不完全或者越界
                //重要信息！因为删除后角标变了，所以再删除这个位置就等同于删除它的下一个元素
                insWithoutPair.remove(i);
                res.add(insWithoutPair);
            }
        }
        return res;
    }

    /**
     * 辅助函数：判断对子的情况
     * 此函数不断递归剔除其中成型的顺子和刻子
     *
     * @param insWithoutPair
     * @return
     */
    private static ArrayList<Integer> removeShunKe(ArrayList<Integer> insWithoutPair) {
        // 对于能胡的牌，最后结果是空手牌；对于不能胡的牌，结果是3n张无关联的牌
        if (insWithoutPair.size() <= 2) {
            // base case,空手牌
            return insWithoutPair;
        }

        if (insWithoutPair.get(0).equals(insWithoutPair.get(1)) && insWithoutPair.get(1).equals(insWithoutPair.get(2))) {
            // 前三张牌相同，剔除这个刻子，然后继续检查
            int firstTile = insWithoutPair.get(0);
            insWithoutPair.remove(Integer.valueOf(firstTile));
            insWithoutPair.remove(Integer.valueOf(firstTile));
            insWithoutPair.remove(Integer.valueOf(firstTile));
            return removeShunKe(insWithoutPair);
        } else {
            // 前三张不同，那么有可能是个顺子，有可能是杂牌
            int firstTile = insWithoutPair.get(0);
            // 同时存在N,N+1,N+2;说明这是个顺子
            if (insWithoutPair.contains(firstTile + 1) && insWithoutPair.contains(firstTile + 2)) {
                // 剔除这个顺子，继续检查
                insWithoutPair.remove(Integer.valueOf(firstTile));
                insWithoutPair.remove(Integer.valueOf(firstTile + 1));
                insWithoutPair.remove(Integer.valueOf(firstTile + 2));
                return removeShunKe(insWithoutPair);
            }
        }
        // 既不是刻子也不是顺子，就是杂牌，没有再检查的必要了
        return insWithoutPair;
    }

    /**
     * 判断是否能吃,返回一个数组，0代表能不能吃，123代表能不能左中右吃
     *
     * @param tile
     * @return
     */
    public static boolean[] canChi(int tile, ArrayList<Integer> playerTiles) {
        boolean leftChi = playerTiles.contains(tile + 1) && playerTiles.contains(tile + 2);
        boolean midChi = playerTiles.contains(tile + 1) && playerTiles.contains(tile - 1);
        boolean rightChi = playerTiles.contains(tile - 1) && playerTiles.contains(tile - 2);
        boolean finalRes = leftChi || midChi || rightChi;
        boolean[] res = {finalRes, leftChi, midChi, rightChi};
        return res;
    }

    /**
     * 判断玩家是否可以碰牌
     *
     * @param latestTileIndex
     * @param playerTiles
     * @return
     */
    public static boolean canPeng(int latestTileIndex, ArrayList<Integer> playerTiles) {
        int frequency = getFrequency(latestTileIndex, playerTiles);
        return frequency >= 2;
    }

    /**
     * 判断玩家是否可以杠牌
     *
     * @param latestTileIndex
     * @param playerTiles
     */
    public static boolean canGang(int latestTileIndex, ArrayList<Integer> playerTiles) {
        int frequency = getFrequency(latestTileIndex, playerTiles);
        return frequency >= 3;
    }

    /**
     * 获取牌的数量
     *
     * @param latestTileIndex
     * @param playerTiles
     * @return
     */
    private static int getFrequency(int latestTileIndex, ArrayList<Integer> playerTiles) {
        return Collections.frequency(playerTiles, latestTileIndex);
    }

    /**
     * 添加所有玩家的牌列表
     *
     * @param mahjongTable
     * @param messages
     */
    public static void printAllPlayerTiles(MahjongGameCacheBean mahjongTable, List<String> messages) {
        messages.add(GuideUtil.dividingLine());
        for (MahjongPlayerCacheBean player : mahjongTable.getPlayers().values()) {
            messages.add("== " + player.getUserId() + "的手牌：" + printTiles(player.getPlayerTiles()));
            String fuluMessage = "";
            if (player.getFuluTiles().size() == 0) {
                fuluMessage += "无";
            } else {
                for (FuluCacheBean fulu : player.getFuluTiles()) {
                    fuluMessage += printTiles(fulu.tileNumbers);
                }
            }
            messages.add("== " + player.getUserId() + "的副露牌：" + fuluMessage);
            if (MahjongConstant.GUOBIAO.equals(mahjongTable.getMahjongRule())) {
                if (player.getHuaTiles().size() == 0) {

                    messages.add("== " + player.getUserId() + "的花牌：无");
                } else {
                    messages.add("== " + player.getUserId() + "的花牌：" + printTiles(player.getHuaTiles()));
                }
            }
            messages.add(GuideUtil.dividingLine());
        }
    }

    /**
     * 判断是否能暗杠
     *
     * @param playerTiles
     * @return
     */
    public static int canAnGang(ArrayList<Integer> playerTiles) {
        for (int i = 0; i < playerTiles.size() - 3; i++) {
            // 排序情况下，间隔2个相等则有四个相等的牌
            if (playerTiles.get(i).equals(playerTiles.get(i + 3))) {
                return playerTiles.get(i);
            }
        }
        return 0;
    }

    /**
     * 输出其他玩家的副露牌和花牌
     *
     * @param userId
     * @param mahjongTable
     */
    public static void outputOthersPlayersTiles(String userId, MahjongGameCacheBean mahjongTable) {

        for (String playerId : mahjongTable.getPlayersId()) {
            MahjongPlayerCacheBean player = mahjongTable.getPlayers().get(playerId);

            if (!player.getUserId().equals(userId)) {
                // 副露牌
                String fuluMessage = "== 玩家(" + playerId + ")副露牌： ";
                if (player.getFuluTiles().size() == 0) {
                    fuluMessage += "无";
                } else {
                    for (FuluCacheBean fulu : player.getFuluTiles()) {
                        if (fulu.type == FuluTypeCacheBean.GANG && fulu.gangSource == 0) {
                            fuluMessage += "暗杠 暗杠 暗杠 暗杠 ";
                        } else {
                            int[] arr = getNewArr(fulu.tileNumbers);
                            fuluMessage += printTiles(arr);
                        }
                    }
                }
                String huaMessage = "";
                if (MahjongConstant.GUOBIAO.equals(mahjongTable.getMahjongRule())) {
                    // 花牌
                    huaMessage = "花牌： ";
                    if (player.getHuaTiles().size() == 0) {
                        huaMessage += "无";
                    } else {
                        huaMessage += printTiles(player.getHuaTiles());
                    }
                }
                userModel.writeToUser(userId, fuluMessage + "     " + huaMessage);
            }
        }
        userModel.writeToUser(userId, GuideUtil.dividingLine());
    }

    /**
     * 本局游戏结束
     *
     * @param mahjongTable
     */
    public static void thisMahjongGameOver(MahjongGameCacheBean mahjongTable) {

        backToRoom(mahjongTable);

        if (mahjongTable.hasAIPlayer()) {
            // 牌桌内有机器人，返回牌桌
            MahjongUtil.backToRoom(mahjongTable);

        } else {
            // 继续游戏
            for (String playerId : mahjongTable.getPlayersId()) {
                userModel.writeToUser(playerId, "== 检测到牌桌内无机器人，10秒后若无玩家退出则自动开启下局游戏！！");
            }

            // 检查是否有玩家退出
            if (mahjongTable.hasAIPlayer()) {
                if (mahjongTable.hasAIPlayer()) {
                    for (MahjongPlayerCacheBean player : mahjongTable.getPlayers().values()) {
                        if (!player.isAIPlayer()) {
                            userModel.writeToUser(player.getUserId(), "== 检测到玩家退出，返回牌桌！！");
                            GuideUtil.userGuide.put(player.getUserId(), 3);
                        }
                    }
                }
            } else {
                for (MahjongPlayerCacheBean playerBean : mahjongTable.getPlayers().values()) {
                    playerBean.shuffle();
                }
                ArrayList<MahjongPlayerCacheBean> players = new ArrayList<>(mahjongTable.getPlayers().values());
                // 无玩家退出
                newMahjongGame(mahjongTable.getZhuangId(), mahjongTable.getMahjongRule(), mahjongTable.getPlayersId(), players);
            }
        }

    }

    /**
     * 创建牌桌游戏
     *
     * @param roomOwner
     * @param mahjongRule
     * @param plays
     * @param mahjongPlayers
     */
    public static void newMahjongGame(String roomOwner, String mahjongRule, List<String> plays, ArrayList<MahjongPlayerCacheBean> mahjongPlayers) {
        // 创建麻将游戏
        MahjongGameCacheBean mahjongTable = new MahjongGameCacheBean(mahjongPlayers, roomOwner, mahjongRule);
        String mahjongRuleStr = getMahjongRuleString(mahjongRule);

        // 添加映射
        for (MahjongPlayerCacheBean player : mahjongPlayers) {
            String playerId = player.getUserId();
            if (!player.isAIPlayer()) {
                gameCache.addUserMahjongGame(playerId, mahjongTable);

                // 格式化输出
                userModel.writeToUser(playerId, GuideUtil.dividingLine());
                userModel.writeToUser(playerId, "== 游戏开始，本牌桌麻将规则为：" + mahjongRuleStr + "麻将   本轮游戏庄家为：" + mahjongTable.getZhuangId());
                List<String> list = GuideUtil.guideGuobiaoMahjong(playerId, new ArrayList<String>());
                for (String message : list) {
                    userModel.writeToUser(playerId, message);
                }
            }
        }

        userModel.writeToUser(mahjongTable.getZhuangId(), "== 发牌结束，你是本轮的庄家，请出牌！！");
        userModel.writeToUser(mahjongTable.getZhuangId(), GuideUtil.dividingLine());

        log.info(mahjongPlayers.toString());
    }

    /**
     * 返回规则
     *
     * @param mahjongRule
     * @return
     */
    private static String getMahjongRuleString(String mahjongRule) {
        String mahjongRuleStr = "";
        if (MahjongConstant.GUOBIAO.equals(mahjongRule)) {
            mahjongRuleStr = "国标";
        } else if (MahjongConstant.GUANGDONG.equals(mahjongRule)) {
            mahjongRuleStr = "广东";
        } else if (MahjongConstant.SICHUAN.equals(mahjongRule)) {
            mahjongRuleStr = "四川";
        } else {
            System.out.println("不该进入这个选项，拍剧初始化错误");
        }
        return mahjongRuleStr;
    }

    /**
     * 玩家胡牌
     *
     * @param mahjongGame
     * @param playerId
     * @param newPlayerBody
     */
    public static void playerHuTiles(MahjongGameCacheBean mahjongGame, String playerId, List<CmdsResponseBody> newPlayerBody) {
        // 输出所有玩家的牌
        if (!MahjongConstant.SICHUAN.equals(mahjongGame.getMahjongRule())) {
            newPlayerBody.get(0).getMessages().add(GuideUtil.dividingLine());
            newPlayerBody.get(0).getMessages().add("== 玩家：" + playerId + "和牌了！！！");
            newPlayerBody.get(0).getMessages().add("== 玩家：" + playerId + "和牌了！！！");
            newPlayerBody.get(0).getMessages().add("== 玩家：" + playerId + "和牌了！！！");
            newPlayerBody.get(0).getMessages().add(GuideUtil.dividingLine());
            mahjongGame.getPlayersHu().add(playerId);
            printAllPlayerTiles(mahjongGame, newPlayerBody.get(0).getMessages());
            userModel.writeToUser(newPlayerBody);
            backToRoom(mahjongGame);
        } else {
            // 四川牌血战到底：和走三家才算结束
            mahjongGame.playerHuPaiInSiChuan(playerId);
            ArrayList<String> playersHu = mahjongGame.getPlayersHu();
            newPlayerBody.get(0).getMessages().add(GuideUtil.dividingLine());
            newPlayerBody.get(0).getMessages().add("== 四川麻将血战模式：和牌达到三家或者牌堆全部摸光才结束！！！");
            if (playersHu.size() >= 3) {
                mahjongGame.setRobotActionThreadDown();
                newPlayerBody.get(0).getMessages().add("== 和牌玩家已达三人，血战模式结束！！！");
                StringBuilder message = new StringBuilder("== 已和牌玩家：");
                for (String playerHu : playersHu) {
                    message.append(playerHu).append("  ");
                }
                newPlayerBody.get(0).getMessages().add(message.toString());
                newPlayerBody.get(0).getMessages().add(GuideUtil.dividingLine());
                newPlayerBody.get(0).getMessages().add("== 玩家：" + playersHu.get(0) + "获得了胜利！！");
                userModel.writeToUser(newPlayerBody);
                backToRoom(mahjongGame);
            } else {

                StringBuilder message = new StringBuilder("== 已和牌玩家：");
                for (String playerHu : playersHu) {
                    message.append(playerHu).append("  ");
                }
                newPlayerBody.get(0).getMessages().add(message.toString());
                newPlayerBody.get(0).getMessages().add(GuideUtil.dividingLine());
                userModel.writeToUser(newPlayerBody);
                noticeNextPlayer(mahjongGame);
            }
        }
    }

    /**
     * 用户返回牌桌
     *
     * @param mahjongTable
     */
    public static void backToRoom(MahjongGameCacheBean mahjongTable) {

        MahjongGameThreadPools.schedule(() -> {
            // 牌桌内有机器人，返回牌桌
            for (String playerId : mahjongTable.getPlayersId()) {
                if (!mahjongTable.getPlayers().get(playerId).isAIPlayer()) {
                    // 销毁游戏，删除所有真人玩家和游戏的映射
                    gameCache.delMahjongGame(playerId);
                    userModel.writeToUser(playerId, "== 本局游戏结束，返回牌桌！！");
                    GuideUtil.userGuide.put(playerId, 3);
                }
            }
            ArrayList<String> playersHu = mahjongTable.getPlayersHu();
            if (playersHu.size() != 0 && !"".equals(getRealWinner(mahjongTable))) {
                String realWinner = getRealWinner(mahjongTable);
                MahjongModel.getInstance().setNewTableOwner(realWinner);
                ArrayList<CmdsResponseBody> newPlayerBody = AIPlayerUtil.getNewPlayerBody(mahjongTable);
                newPlayerBody.get(0).getMessages().add("玩家：" + realWinner + " 获得了胜利，成为了牌桌的庄家！！");
                newPlayerBody.get(0).getMessages().add(GuideUtil.dividingLine());
                userModel.writeToUser(newPlayerBody);
            }
            // 剔除牌桌内的机器人
            MahjongModel.getInstance().removeRobotFromTable(mahjongTable.getZhuangId());
        }, 50, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取真人赢家
     *
     * @param mahjongTable
     * @return
     */
    private static String getRealWinner(MahjongGameCacheBean mahjongTable) {
        ArrayList<String> playersHu = mahjongTable.getPlayersHu();
        for (int i = 0; i < playersHu.size(); i++) {
            String playerId = playersHu.get(i);
            if (!mahjongTable.getPlayers().get(playerId).isAIPlayer()) {
                return playerId;
            }
        }
        return "";
    }

    /**
     * 通知下家出牌
     *
     * @param mahjongTable
     */
    public static void noticeNextPlayer(MahjongGameCacheBean mahjongTable) {
        // 如果下家不为机器人，发出通知
        MahjongPlayerCacheBean nextPlayer = mahjongTable.getPlayers().get(mahjongTable.currentUserId());
        if (!nextPlayer.isAIPlayer() && !mahjongTable.isAllTilesDraw()) {
            List<CmdsResponseBody> nextPlayerBody = new ArrayList<CmdsResponseBody>();
            CmdsResponseBody nextBody = new CmdsResponseBody();
            nextBody.getUserIds().add(nextPlayer.getUserId());
            nextBody.getMessages().add("你的回合，请使用命令 draw 摸牌，再出牌");
            nextBody.getMessages().add(GuideUtil.dividingLine());
            nextPlayerBody.add(nextBody);
            userModel.writeToUser(nextPlayerBody);
        }
    }

    public static void main(String[] args) {

        int i = 1;
        CALL:
        if (i == 1){
            i = 2;
            if (i == 2){
                break CALL;
            }
            i = 3;

        }
        System.out.println(i);
    }

}
