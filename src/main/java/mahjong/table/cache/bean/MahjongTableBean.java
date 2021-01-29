package mahjong.table.cache.bean;

import mahjong.table.cache.MahjongTableCache;

import java.util.*;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 12:19:13
 */
public class MahjongTableBean {

    /**
     * 牌桌名
     */
    private String tableName;
    /**
     * 牌桌规则
     */
    private String mahjongRule;
    /**
     * 庄家
     */
    private String tableOwner;
    /**
     * 牌桌内玩家
     */
    private ArrayList<String> plays;
    /**
     * 牌桌倒计时标志
     */
    private boolean isReadyFlag;
    /**
     * 玩家准备队列
     */
    private HashMap<String, Boolean> playsReady;
    /**
     * 玩家人机标记
     */
    private HashMap<String, Boolean> isPlaysRobot;
    /**
     * 人机等级标志
     */
    private HashMap<String, Integer> robotLevel;
    /**
     * 机器人id
     */
    private int robotId;

    public String getTableOwner() {
        return tableOwner;
    }

    public void setTableOwner(String tableOwner) {
        this.tableOwner = tableOwner;
    }

    public MahjongTableBean(String tableName, String mahjongRule, String userId) {
        this.tableName = tableName;
        this.mahjongRule = mahjongRule;
        this.tableOwner = userId;
        plays = new ArrayList<String>(4);
        plays.add(userId);
        playsReady = new HashMap<String, Boolean>(4);
        playsReady.put(userId, false);
        isPlaysRobot = new HashMap<String, Boolean>(4);
        isPlaysRobot.put(userId, false);
        isReadyFlag = false;
        robotId = 1;
        robotLevel = new HashMap<String, Integer>();
    }

    public String getMahjongRule() {
        return mahjongRule;
    }

    public void setMahjongRule(String mahjongRule) {
        this.mahjongRule = mahjongRule;
    }

    public String getTableName() {
        return tableName;

    }

    public HashMap<String, Boolean> getIsPlaysRobot() {
        return isPlaysRobot;
    }

    public void setIsPlaysRobot(HashMap<String, Boolean> isPlaysRobot) {
        this.isPlaysRobot = isPlaysRobot;
    }

    public boolean isReadyFlag() {
        return isReadyFlag;
    }

    public void setReadyFlag(boolean readyFlag) {
        isReadyFlag = readyFlag;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getPlays() {
        return plays;
    }

    public void setPlays(ArrayList<String> plays) {
        this.plays = plays;
    }

    public HashMap<String, Boolean> getPlaysReady() {
        return playsReady;
    }

    public HashMap<String, Integer> getRobotLevel() {
        return robotLevel;
    }

    public void setRobotLevel(HashMap<String, Integer> robotLevel) {
        this.robotLevel = robotLevel;
    }

    public int getRobotId() {
        return robotId;
    }

    public void setRobotId(int robotId) {
        this.robotId = robotId;
    }

    public void setPlaysReady(HashMap<String, Boolean> playsReady) {
        this.playsReady = playsReady;
    }

    /**
     * 获取牌桌人数
     *
     * @return
     */
    public int getNumberofPlay() {
        return plays.size();
    }

    /**
     * 删除牌桌内用户
     *
     * @param userId
     */
    public void delUserByUserId(String userId) {
        plays.remove(userId);
        playsReady.remove(userId);
    }

    /**
     * 用户准备
     *
     * @param userId
     */
    public void ready(String userId) {
        playsReady.put(userId, true);
    }

    /**
     * 判断是否满足倒计时开始游戏条件
     */
    public boolean canCountdown() {
        if (plays.size() == 4) {
            int frequency = Collections.frequency(playsReady.values(), true);
            if (frequency >= 2) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "MahjongTableBean{" +
                "tableName='" + tableName + '\'' +
                ", mahjongRule='" + mahjongRule + '\'' +
                ", tableOwner='" + tableOwner + '\'' +
                ", plays=" + plays +
                ", isReadyFlag=" + isReadyFlag +
                ", playsReady=" + playsReady +
                ", isPlaysRobot=" + isPlaysRobot +
                ", robotLevel=" + robotLevel +
                ", robotId=" + robotId +
                '}';
    }

    /**
     * 判断是否所有玩家已准备
     * 返回未准备玩家
     *
     * @return
     */
    public List<String> isAllPlayerReady() {
        ArrayList<String> unReadyPlayers = new ArrayList<String>();
        for (Map.Entry<String, Boolean> entry : playsReady.entrySet()) {
            if (!entry.getValue()) {
                unReadyPlayers.add(entry.getKey());
            }
        }
        return unReadyPlayers;
    }

    /**
     * 用户加入牌桌
     *
     * @param userId
     */
    public void playerJoinTable(String userId) {
        plays.add(userId);
        playsReady.put(userId, false);
        isPlaysRobot.put(userId, false);
    }

    /**
     * 添加机器人
     *
     * @param robotLevel
     * @return
     */
    public String robotJoinTable(int robotLevel) {
        String robotName = "Robot_" + robotId;
        plays.add(robotName);
        playsReady.put(robotName, true);
        isPlaysRobot.put(robotName, true);
        this.robotLevel.put(robotName, robotLevel);
        robotId++;
        return robotName;
    }

    /**
     * 移除玩家
     *
     * @param playerId
     */
    public void removePlayer(String playerId) {
        plays.remove(playerId);
        playsReady.remove(playerId);
        if (isPlaysRobot.get(playerId)) {
            robotLevel.remove(playerId);
        }
        isPlaysRobot.remove(playerId);
    }

    /**
     * 选择新房主
     *
     * @return
     */
    public String selectNewTableOwner() {
        @SuppressWarnings("unchecked")
        ArrayList<String> playerClone = (ArrayList<String>) plays.clone();
        Iterator<Map.Entry<String, Boolean>> iterator = isPlaysRobot.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Boolean> entry = iterator.next();
            if (entry.getValue()) {
                playerClone.remove(entry.getKey());
            }
        }

        String newtableOwner = "";
        if (playerClone.size() == 0) {
            System.out.println("只剩机器人，牌桌销毁");
            MahjongTableCache.getInstance().delMahjongTable(mahjongRule, this);
        } else {

            int size = playerClone.size();
            int randomNum = (int) (Math.random() * ((size - 1) + 1));
            newtableOwner = playerClone.get(randomNum);
            tableOwner = newtableOwner;
        }

        return newtableOwner;
    }

    /**
     * 判断牌桌内是否还有真人玩家
     *
     * @return
     */
    public boolean hasRealPlayer() {
        for (String playerId : plays) {
            if (!isPlaysRobot.get(playerId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将直接推出的玩家设为机器人
     * @param userId
     */
    public void changePlayerToRobot(String userId) {
        isPlaysRobot.put(userId,true);
        robotLevel.put(userId,2);
    }
}
