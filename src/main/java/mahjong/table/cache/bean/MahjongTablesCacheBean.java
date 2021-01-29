package mahjong.table.cache.bean;

import mahjong.game.util.MahjongConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author muyi
 * @description:
 * @date 2020-11-20 12:01:03
 */
public class MahjongTablesCacheBean {
    /**
     * 用户麻将规则列表
     */
    private final ConcurrentHashMap<String, String> userMahjongRule;
    /**
     * 麻将牌桌列表
     */
    private final HashMap<String, CopyOnWriteArrayList<MahjongTableBean>> mahjongTables;
    /**
     * 用户牌桌列表
     */
    private final ConcurrentHashMap<String, MahjongTableBean> userMahjongTable;
    /**
     * 用户退出游戏标记
     */
    private final ArrayList<String> userBackFlags;

    private volatile static MahjongTablesCacheBean instance;

    private MahjongTablesCacheBean() {
        userMahjongRule = new ConcurrentHashMap<String, String>();
        mahjongTables = new HashMap<String, CopyOnWriteArrayList<MahjongTableBean>>();
        mahjongTables.put(MahjongConstant.GUOBIAO, new CopyOnWriteArrayList<MahjongTableBean>());
        mahjongTables.put(MahjongConstant.GUANGDONG, new CopyOnWriteArrayList<MahjongTableBean>());
        mahjongTables.put(MahjongConstant.SICHUAN, new CopyOnWriteArrayList<MahjongTableBean>());
        userMahjongTable = new ConcurrentHashMap<String, MahjongTableBean>();
        userBackFlags = new ArrayList<String>();
    }

    /**
     * @return MahjongTablesCacheBean
     */
    public static MahjongTablesCacheBean getInstance() {
        if (instance == null) {
            synchronized (MahjongTablesCacheBean.class) {
                if (instance == null) {
                    instance = new MahjongTablesCacheBean();
                }
            }
        }
        return instance;
    }

    /**
     * 监控内存状态
     */
    public void userMahjongRuleCache(){
        System.out.println(userMahjongRule.toString());
    }
    /**
     * 监控内存状态
     */
    public  void userMahjongTable(){
        System.out.println(userMahjongTable.toString());
    }

    /**
     * 设置用户选择的麻将规则
     *
     * @param userId
     * @param rule
     */
    public void setUserMahjongRule(String userId, String rule) {
        userMahjongRule.put(userId, rule);
    }

    /**
     * 获取用户麻将规则
     *
     * @param userId
     * @return
     */
    public String getUserRule(String userId) {
        return userMahjongRule.get(userId);
    }

    /**
     * 删除用户已确定的规则
     *
     * @param userId
     */
    public void delUserRule(String userId) {
        userMahjongRule.remove(userId);
    }

    /**
     * 获取对应规则的麻将牌桌列表
     *
     * @param mahjongRule
     * @return
     */
    public CopyOnWriteArrayList<MahjongTableBean> getMahjongTables(String mahjongRule) {
        return mahjongTables.get(mahjongRule);
    }

    /**
     * 销毁对应牌桌
     *
     * @param mahjongRule
     * @param mahjongTable
     */
    public void delMahjongTable(String mahjongRule, MahjongTableBean mahjongTable) {
        mahjongTables.get(mahjongRule).remove(mahjongTable);
    }

    /**
     * 将牌桌添加进牌桌列表
     *
     * @param mahjongTable
     */
    public void addMahjongTable(MahjongTableBean mahjongTable) {
        CopyOnWriteArrayList<MahjongTableBean> tables = mahjongTables.get(mahjongTable.getMahjongRule());
        tables.add(mahjongTable);
    }

    /**
     * 获取用户所在的牌桌
     *
     * @param userId
     * @return
     */
    public MahjongTableBean getMahjongTable(String userId) {
        return userMahjongTable.get(userId);
    }

    /**
     * 删除用户与牌桌的映射
     *
     * @param userId
     */
    public void delUserMahjongTable(String userId) {
        userMahjongTable.remove(userId);
    }

    /**
     * 添加用户牌桌映射
     *
     * @param userId
     * @param mahjongTable
     */
    public void addUserTable(String userId, MahjongTableBean mahjongTable) {
        userMahjongTable.put(userId, mahjongTable);
    }

    /**
     * 用户退出确认
     *
     * @param userId
     * @return
     */
    public boolean getUserBackFlag(String userId) {

        if (userBackFlags.contains(userId)) {
            userBackFlags.remove(userId);
            return true;
        } else {
            userBackFlags.add(userId);
            return false;
        }
    }
}
