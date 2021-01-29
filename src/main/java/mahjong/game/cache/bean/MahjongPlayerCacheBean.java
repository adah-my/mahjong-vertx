package mahjong.game.cache.bean;

import mahjong.game.util.MahjongUtil;

import java.util.ArrayList;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 20:29:20
 */
public class MahjongPlayerCacheBean {

    /**
     * 玩家Id
     */
    private String userId;
    /**
     * 人机标志
     */
    private boolean isAIPlayer;
    /**
     * AI等级
     */
    private int AILevel;
    /**
     * 玩家的手牌
     */
    private ArrayList<Integer> playerTiles;
    /**
     * 副露堆
     * 每位玩家都拥有一个副露堆，里面存放着副露对象
     */
    private ArrayList<FuluCacheBean> fuluTiles;
    /**
     * 花牌堆
     * 每位玩家都拥有一个花牌堆，里面存放着花牌的编号
     */
    private ArrayList<Integer> huaTiles;
    /**
     * 弃牌堆
     * 每位玩家自己面前拥有弃牌堆
     * 按时间顺序记录打出的牌
     * 但是被吃碰杠拿走的牌进入他人的副露而非弃牌堆
     */
    private ArrayList<Integer> discardTiles;

    public MahjongPlayerCacheBean(String userId) {
        this.userId = userId;
        playerTiles = new ArrayList<Integer>();
        fuluTiles = new ArrayList<FuluCacheBean>();
        huaTiles = new ArrayList<Integer>();
        discardTiles = new ArrayList<Integer>();
        isAIPlayer = false;
    }
    public MahjongPlayerCacheBean(String userId , boolean isAIPlayer) {
        this.userId = userId;
        playerTiles = new ArrayList<Integer>();
        fuluTiles = new ArrayList<FuluCacheBean>();
        huaTiles = new ArrayList<Integer>();
        discardTiles = new ArrayList<Integer>();
        this.isAIPlayer = isAIPlayer;
        this.AILevel = 0;
    }

    public int getAILevel() {
        return AILevel;
    }

    public void setAILevel(int AILevel) {
        this.AILevel = AILevel;
    }

    public boolean isAIPlayer() {
        return isAIPlayer;
    }

    public void setAIPlayer(boolean AIPlayer) {
        isAIPlayer = AIPlayer;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ArrayList<Integer> getPlayerTiles() {
        return playerTiles;
    }

    public void setPlayerTiles(ArrayList<Integer> playerTiles) {
        this.playerTiles = playerTiles;
    }

    public ArrayList<FuluCacheBean> getFuluTiles() {
        return fuluTiles;
    }

    public void setFuluTiles(ArrayList<FuluCacheBean> fuluTiles) {
        this.fuluTiles = fuluTiles;
    }

    public ArrayList<Integer> getHuaTiles() {
        return huaTiles;
    }

    public void setHuaTiles(ArrayList<Integer> huaTiles) {
        this.huaTiles = huaTiles;
    }

    public ArrayList<Integer> getDiscardTiles() {
        return discardTiles;
    }

    public void setDiscardTiles(ArrayList<Integer> discardTiles) {
        this.discardTiles = discardTiles;
    }

    /**
     * 重置玩家牌堆
     */
    public void shuffle(){
        playerTiles = new ArrayList<Integer>();
        fuluTiles = new ArrayList<FuluCacheBean>();
        huaTiles = new ArrayList<Integer>();
        discardTiles = new ArrayList<Integer>();
    }

    @Override
    public String toString() {
        return "MahjongPlayerCacheBean{" +
                "AIPlayer=" + isAIPlayer  +
                ", userId=" + userId  +
                ", playerTiles=" + MahjongUtil.printTiles(playerTiles) +
                ", fuluTiles=" + fuluTiles +
                ", huaTiles=" + MahjongUtil.printTiles(huaTiles) +
                ", discardTiles=" + MahjongUtil.printTiles(discardTiles) +
                '}';
    }
}
