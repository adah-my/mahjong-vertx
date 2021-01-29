package mahjong.game.model.impl;

import mahjong.core.common.CmdsResponseBody;
import mahjong.core.model.UserModel;
import mahjong.game.cache.GameCache;
import mahjong.game.cache.bean.MahjongPlayerCacheBean;
import mahjong.game.cache.bean.MahjongGameCacheBean;
import mahjong.game.util.AIPlayerUtil;
import mahjong.game.util.MahjongGameThreadPools;
import mahjong.game.util.MahjongUtil;
import mahjong.game.model.GameModel;
import mahjong.util.GuideUtil;

import java.util.*;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 18:53:22
 */
public class GameModelImpl implements GameModel {

    private final UserModel userModel;
    private final GameCache gameCache;

    private volatile static GameModel instance;

    private GameModelImpl() {
        userModel = UserModel.getInstance();
        gameCache = GameCache.getInstance();
    }

    /**
     * @return GuoBiaoModel
     */
    public static GameModel getInstance() {
        if (instance == null) {
            synchronized (GameModelImpl.class) {
                if (instance == null) {
                    instance = new GameModelImpl();
                }
            }
        }
        return instance;
    }

    /**
     * 玩家全部准备,开始游戏
     *
     * @param tableOwner
     * @param mahjongRule
     * @param plays
     * @param isPlaysRobot
     * @param robotLevel
     */
    @Override
    public void startGame(String tableOwner, String mahjongRule, List<String> plays, HashMap<String, Boolean> isPlaysRobot, HashMap<String, Integer> robotLevel) {
        ArrayList<MahjongPlayerCacheBean> mahjongPlayers = new ArrayList<MahjongPlayerCacheBean>();
        if (plays.size() != 4) {
            throw new RuntimeException("玩家不足四人，开启游戏失败");
        } else {
            // 先添加庄家
            for (String playerId : plays) {
                GuideUtil.userGuide.put(playerId, 4);
                MahjongPlayerCacheBean playerCacheBean = new MahjongPlayerCacheBean(playerId, isPlaysRobot.get(playerId));
                mahjongPlayers.add(playerCacheBean);
                if (isPlaysRobot.get(playerId)) {
                    // 设置AI等级
                    playerCacheBean.setAILevel(robotLevel.get(playerId));
                }
            }
            MahjongUtil.newMahjongGame(tableOwner, mahjongRule, plays, mahjongPlayers);
        }
    }

    /**
     * 麻将牌输出
     *
     * @param userId
     */
    @Override
    public void printMahjongHandTiles(String userId) {

        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        MahjongUtil.outputPlayersTiles(userId, mahjongGame);
        MahjongUtil.outputOthersPlayersTiles(userId, mahjongGame);

    }

    /**
     * 用户退出游戏
     *
     * @param userId
     */
    @Override
    public void playerBackToTable(String userId) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        MahjongPlayerCacheBean player = mahjongGame.getPlayers().get(userId);

        // 用户托管AI 默认托管等级2AI
        player.setAIPlayer(true);
        player.setAILevel(2);
        ArrayList<String> playersId = mahjongGame.getPlayersId();
        for (String playerId : playersId) {
            if (!mahjongGame.getPlayers().get(playerId).isAIPlayer()) {
                userModel.writeToUser(playerId, "== 玩家：" + userId + "退出游戏，他的牌组将由AI托管~");
                userModel.writeToUser(playerId, GuideUtil.dividingLine());
            }
        }
        // 判断是否轮到玩家出牌
        if (userId.equals(mahjongGame.getPlayersId().get(mahjongGame.getCurrentOutTilePlayer()))) {
            // 判断玩家是否已经摸牌
            if (mahjongGame.isUserDrawTile()) {
                MahjongGameThreadPools.executeByHelpThreadPool(()->{
                    AIPlayerUtil.robotOutTile(player, mahjongGame);
                });
            } else {
                MahjongGameThreadPools.executeByHelpThreadPool(()->{
                    AIPlayerUtil.robotAction(mahjongGame.getZhuangId());
                });
            }
        }

        int robotCount = 0;
        String realPlayerId = "";
        for (MahjongPlayerCacheBean playerBean : mahjongGame.getPlayers().values()) {
            if (playerBean.isAIPlayer()) {
                robotCount++;
            } else {
                realPlayerId = playerBean.getUserId();
            }
        }
        // 当机器人数量为4，游戏内无真人玩家
        if (robotCount != 4) {
            gameCache.delMahjongGame(userId);
            if (mahjongGame.getZhuangId().equals(userId)) {
                ArrayList<CmdsResponseBody> newPlayerBody = AIPlayerUtil.getNewPlayerBody(mahjongGame);
                newPlayerBody.get(0).getMessages().add("庄家："+mahjongGame.getZhuangId()+" 离开游戏，玩家："+realPlayerId+" 成为新庄家！！");
                newPlayerBody.get(0).getMessages().add(GuideUtil.dividingLine());
                mahjongGame.setZhuangId(realPlayerId);
                userModel.writeToUser(newPlayerBody);
            }
        }else {
            // 局内全部为机器人，销毁游戏
            mahjongGame.setRobotActionThreadDown();
            gameCache.delMahjongGame(userId);
            System.out.println("游戏内无真人玩家，游戏销毁！");
        }

    }

}
