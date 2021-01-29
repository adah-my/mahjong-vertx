package mahjong.table.cache.impl;

import mahjong.table.cache.MahjongTableCache;
import mahjong.table.cache.bean.MahjongTableBean;
import mahjong.table.cache.bean.MahjongTablesCacheBean;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 12:17:14
 */
public class MahjongTableCacheImpl implements MahjongTableCache {

    /**
     * 牌桌内存bean
     */
    private final MahjongTablesCacheBean mahjongTablesBean;

    private volatile static MahjongTableCache instance;

    private MahjongTableCacheImpl() {
        mahjongTablesBean = MahjongTablesCacheBean.getInstance();
    }

    /**
     * @return LoginCache
     */
    public static MahjongTableCache getInstance() {
        if (instance == null) {
            synchronized (MahjongTableCacheImpl.class) {
                if (instance == null) {
                    instance = new MahjongTableCacheImpl();
                }
            }
        }
        return instance;
    }

    /**
     * 设置用户选择的麻将规则
     *
     * @param userId
     * @param rule
     */
    @Override
    public void setUserMahjongRule(String userId, String rule) {
        mahjongTablesBean.setUserMahjongRule(userId, rule);
    }

    /**
     * 获取对应规则的麻将牌桌列表
     *
     * @param mahjongRule
     * @return
     */
    @Override
    public CopyOnWriteArrayList<MahjongTableBean> getMahjongTables(String mahjongRule) {
        return mahjongTablesBean.getMahjongTables(mahjongRule);
    }

    /**
     * 获取用户麻将规则
     *
     * @param userId
     * @return
     */
    @Override
    public String getUserRule(String userId) {
        return mahjongTablesBean.getUserRule(userId);
    }

    /**
     * 根据规则与牌桌名返回对应的牌桌
     *
     * @param userRule
     * @param tableName
     * @return
     */
    @Override
    public MahjongTableBean getMahjongTable(String userRule, String tableName) {
        CopyOnWriteArrayList<MahjongTableBean> mahjongTableBeans = mahjongTablesBean.getMahjongTables(userRule);
        for (MahjongTableBean table : mahjongTableBeans) {
            if (table.getTableName().equals(tableName)) {
                return table;
            }
        }
        return null;
    }

    /**
     * 删除用户已确定的规则
     *
     * @param userId
     */
    @Override
    public void delUserRule(String userId) {
        mahjongTablesBean.delUserRule(userId);
    }

    /**
     * 获取用户所在的牌桌
     *
     * @param userId
     * @return
     */
    @Override
    public MahjongTableBean getMahjongTable(String userId) {
        return mahjongTablesBean.getMahjongTable(userId);
    }

    /**
     * 删除用户与牌桌的映射
     *
     * @param userId
     */
    @Override
    public void delUserMahjongTable(String userId) {
        mahjongTablesBean.delUserMahjongTable(userId);
    }

    /**
     * 销毁对应牌桌
     *
     * @param mahjongRule
     * @param mahjongTable
     */
    @Override
    public void delMahjongTable(String mahjongRule, MahjongTableBean mahjongTable) {
        mahjongTablesBean.delMahjongTable(mahjongRule, mahjongTable);
    }

    /**
     * 添加用户牌桌映射
     *
     * @param userId
     * @param mahjongTable
     */
    @Override
    public void addUserTable(String userId, MahjongTableBean mahjongTable) {
        mahjongTablesBean.addUserTable(userId, mahjongTable);
    }

    /**
     * 将牌桌添加进牌桌列表
     *
     * @param mahjongTable
     */
    @Override
    public void addMahjongTable(MahjongTableBean mahjongTable) {
        mahjongTablesBean.addMahjongTable(mahjongTable);
    }

    /**
     * 用户退出确认
     *
     * @param userId
     * @return
     */
    @Override
    public boolean getUserBackFlag(String userId) {
        return mahjongTablesBean.getUserBackFlag(userId);
    }

}
