package mahjong.table.model;

import mahjong.table.cache.bean.MahjongTableBean;
import mahjong.table.model.impl.MahjongModelImpl;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 12:18:02
 */
public interface MahjongModel {

    static MahjongModel getInstance() {
        return MahjongModelImpl.getInstance();
    }

    CopyOnWriteArrayList<MahjongTableBean> getMahjongTablesByUserId(String userId);

    MahjongTableBean getMahjongTableByUserId(String userId);

    void removeRobotFromTable(String zhuangId);

    void userBack(String userId);

    void setNewTableOwner(String newRoomOwnerName);
}
