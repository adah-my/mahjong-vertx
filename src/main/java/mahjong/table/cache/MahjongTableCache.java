package mahjong.table.cache;

import mahjong.table.cache.bean.MahjongTableBean;
import mahjong.table.cache.impl.MahjongTableCacheImpl;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 12:16:54
 */
public interface MahjongTableCache {

    static MahjongTableCache getInstance() {
        return MahjongTableCacheImpl.getInstance();
    }

    void setUserMahjongRule(String userId, String rule);

    CopyOnWriteArrayList<MahjongTableBean> getMahjongTables(String command);

    String getUserRule(String userId);

    MahjongTableBean getMahjongTable(String userRule, String roomName);

    void delUserRule(String userId);

    MahjongTableBean getMahjongTable(String userId);

    void delUserMahjongTable(String userId);

    void delMahjongTable(String mahjongRule, MahjongTableBean mahjongRoom);

    void addUserTable(String userId, MahjongTableBean mahjongRoom);

    void addMahjongTable(MahjongTableBean mahjongRoom);

    boolean getUserBackFlag(String userId);

}
