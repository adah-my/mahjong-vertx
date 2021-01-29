package mahjong.table.model.impl;

import mahjong.core.common.CmdsResponseBody;
import mahjong.core.model.UserModel;
import mahjong.table.cache.MahjongTableCache;
import mahjong.table.cache.bean.MahjongTableBean;
import mahjong.table.cmds.MahjongTableCmds;
import mahjong.table.model.MahjongModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 12:18:21
 */
public class MahjongModelImpl implements MahjongModel {

    private final MahjongTableCache mahjongTableCache;
    private final UserModel userModel;

    private volatile static MahjongModel instance;

    private MahjongModelImpl() {
        mahjongTableCache = MahjongTableCache.getInstance();
        userModel = UserModel.getInstance();
    }

    /**
     * @return MahjongModel
     */
    public static MahjongModel getInstance() {
        if (instance == null) {
            synchronized (MahjongModelImpl.class) {
                if (instance == null) {
                    instance = new MahjongModelImpl();
                }
            }
        }
        return instance;
    }

    /**
     * 获得玩家所在的牌桌列表
     *
     * @param userId
     * @return
     */
    @Override
    public CopyOnWriteArrayList<MahjongTableBean> getMahjongTablesByUserId(String userId) {
        String userRule = mahjongTableCache.getUserRule(userId);
        return mahjongTableCache.getMahjongTables(userRule);
    }

    /**
     * 根据id获取牌桌
     *
     * @param userId
     * @return
     */
    @Override
    public MahjongTableBean getMahjongTableByUserId(String userId) {
        return mahjongTableCache.getMahjongTable(userId);
    }

    /**
     * 剔除牌桌内的机器人
     *
     * @param zhuangId
     */
    @Override
    public void removeRobotFromTable(String zhuangId) {
        MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(zhuangId);
        mahjongTable.setReadyFlag(false);
        mahjongTable.setRobotId(1);
        ArrayList<String> robots = new ArrayList<>();
        for (String playerId : mahjongTable.getRobotLevel().keySet()) {
            robots.add(playerId);
        }
        for (String playerId : robots) {
            mahjongTable.removePlayer(playerId);
        }
    }

    /**
     * 用户返回大厅
     *
     * @param userId
     */
    @Override
    public void userBack(String userId) {
        MahjongTableCmds mahjongTableCmds = new MahjongTableCmds();
        String[] back = {"back"};
        List<CmdsResponseBody> backBody = mahjongTableCmds.back(userId, back);
        if (backBody.size() >= 2) {
            userModel.writeToUser(backBody.subList(0, backBody.size() - 1));
        }
        mahjongTableCmds.back(userId, back);
        mahjongTableCache.delUserRule(userId);
    }

    /**
     * 设置牌桌庄家
     *
     * @param newTableOwnerName
     */
    @Override
    public void setNewTableOwner(String newTableOwnerName) {
        MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(newTableOwnerName);
        mahjongTable.setTableOwner(newTableOwnerName);
    }


}
