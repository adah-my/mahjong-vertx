package mahjong.game.cache;

import mahjong.game.cache.bean.MahjongGameCacheBean;
import mahjong.game.cache.impl.GameCacheImpl;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 18:51:26
 */
public interface GameCache {

    static GameCache getInstance() {
        return GameCacheImpl.getInstance();
    }

    MahjongGameCacheBean getMahjongGame(String userId);

    void addUserMahjongGame(String playerId, MahjongGameCacheBean mahjongTable);

    void delMahjongGame(String userId);
}
