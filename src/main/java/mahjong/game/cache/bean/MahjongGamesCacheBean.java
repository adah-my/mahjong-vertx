package mahjong.game.cache.bean;


import java.util.concurrent.ConcurrentHashMap;

/**
 * @author muyi
 * @description: 用户游戏映射bean
 * @date 2020-11-20 11:40:02
 */
public class MahjongGamesCacheBean {

    /**
     * 用户麻将局映射
     */
    private final ConcurrentHashMap<String, MahjongGameCacheBean> mahjongGames;

    private volatile static MahjongGamesCacheBean instance;

    private MahjongGamesCacheBean() {
        mahjongGames = new ConcurrentHashMap<String, MahjongGameCacheBean>();
    }

    /**
     *
     * @return MahjongGamesCacheBean
     */
    public static MahjongGamesCacheBean getInstance() {
        if (instance == null) {
            synchronized (MahjongGamesCacheBean.class) {
                if (instance == null) {
                    instance = new MahjongGamesCacheBean();
                }
            }
        }
        return instance;
    }

    /**
     * 监控内存状态
     */
    public void getCache(){
        System.out.println(mahjongGames.toString());
    }


    /**
     * 通过玩家id获取游戏局
     * @param userId
     * @return
     */
    public MahjongGameCacheBean getMahjongGame(String userId) {
        return mahjongGames.get(userId);
    }

    /**
     * 添加用户麻将映射
     * @param playerId
     * @param mahjongGame
     */
    public void addUserMahjongGame(String playerId, MahjongGameCacheBean mahjongGame) {
        mahjongGames.put(playerId,mahjongGame);
    }

    /**
     * 删除游戏映射
     * @param userId
     */
    public void delMahjongGame(String userId) {
        mahjongGames.remove(userId);
    }

}
