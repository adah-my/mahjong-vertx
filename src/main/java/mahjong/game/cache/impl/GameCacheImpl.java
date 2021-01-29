package mahjong.game.cache.impl;

import mahjong.game.cache.GameCache;
import mahjong.game.cache.bean.MahjongGameCacheBean;
import mahjong.game.cache.bean.MahjongGamesCacheBean;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 18:51:41
 */
public class GameCacheImpl implements GameCache {

    private final MahjongGamesCacheBean gamesBean;

    private volatile static GameCache instance;

    private GameCacheImpl() {

        gamesBean = MahjongGamesCacheBean.getInstance();
    }

    /**
     *
     * @return GuoBiaoCache
     */
    public static GameCache getInstance() {
        if (instance == null) {
            synchronized (GameCacheImpl.class) {
                if (instance == null) {
                    instance = new GameCacheImpl();
                }
            }
        }
        return instance;
    }

    /**
     * 通过玩家id获取游戏局
     * @param userId
     * @return
     */
    @Override
    public MahjongGameCacheBean getMahjongGame(String userId) {
         return gamesBean.getMahjongGame(userId);
    }

    /**
     * 添加用户麻将映射
     * @param playerId
     * @param mahjongGame
     */
    @Override
    public void addUserMahjongGame(String playerId, MahjongGameCacheBean mahjongGame) {
        gamesBean.addUserMahjongGame(playerId,mahjongGame);
    }

    /**
     * 删除游戏映射
     * @param userId
     */
    @Override
    public void delMahjongGame(String userId) {
        gamesBean.delMahjongGame(userId);
    }


}
