package mahjong.game.cache.bean;

import mahjong.game.util.MahjongConstant;
import mahjong.game.util.MahjongGameThreadPools;
import mahjong.game.util.MahjongUtil;
import mahjong.game.util.AIPlayerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 19:54:12
 */
public class MahjongGameCacheBean {

    /**
     * 麻将规则
     */
    private String mahjongRule;
    /**
     * 麻将牌名
     */
    public String[] effectivePai;
    /**
     * 有效编号
     */
    public int[] effectiveCode;

    /**
     * 牌库：国标牌144张；广东牌：136张；四川牌：108张牌
     */
    private int[] deck;
    /**
     * 牌库顶指针
     */
    private int top;
    /**
     * 牌库底指针
     */
    private int bottom;
    /**
     * 庄家Id
     */
    private String zhuangId;
    /**
     * 当前可出牌玩家index
     */
    private int currentOutTilePlayer;
    /**
     * 牌桌玩家id
     */
    private ArrayList<String> playersId;
    /**
     * 记录当前回合玩家是否已经摸牌
     */
    private boolean isUserDrawTile;
    /**
     * 记录最新出的牌的id
     */
    private int latestTileIndex;
    /**
     * 明牌计数器，记录所有打出来的牌，包括吃杠碰
     */
    private HashMap<Integer, Integer> discardCounnt;
    /**
     * 牌局玩家
     */
    private ConcurrentHashMap<String, MahjongPlayerCacheBean> players;
    /**
     * 机器人延时动作任务（用于吃杠碰取消机器人动作）
     */
    private ScheduledFuture<?> schedule;
    /**
     * 四川牌专用，已经和牌玩家Id
     */
    private ArrayList<String> playersHu;

    /**
     * 初始化游戏
     *
     * @param players
     */
    public MahjongGameCacheBean(List<MahjongPlayerCacheBean> players, String zhuangId, String mahjongRule) {
        this.mahjongRule = mahjongRule;
        initEffectivePai(mahjongRule);
        top = 0;
        bottom = deck.length - 1;
        playersId = new ArrayList<String>();
        this.players = new ConcurrentHashMap<String, MahjongPlayerCacheBean>(4);
        this.zhuangId = zhuangId;

        for (int i = 0; i < 4; i++) {
            String playerId = players.get(i).getUserId();
            this.players.put(playerId, players.get(i));
            playersId.add(playerId);
            if (playerId.equals(zhuangId)) {
                currentOutTilePlayer = i;
            }
        }
        isUserDrawTile = true;
        deckInit();
        shuffle();
        distribute();
        discardCounnt = new HashMap<Integer, Integer>();
        playersHu = new ArrayList<String>(4);
    }

    /**
     * 根据规则初始化牌堆
     *
     * @param mahjongRule
     */
    private void initEffectivePai(String mahjongRule) {
        if (MahjongConstant.GUOBIAO.equals(mahjongRule)) {
            deck = new int[144];
            effectivePai = MahjongConstant.GUOBIAO_PAI;
            effectiveCode = MahjongConstant.GUOBIAO_PAI_CODE;
        } else if (MahjongConstant.GUANGDONG.equals(mahjongRule)) {
            deck = new int[136];
            effectivePai = MahjongConstant.GUANGDONG_PAI;
            effectiveCode = MahjongConstant.GUANGDONG_PAI_CODE;
        } else if (MahjongConstant.SICHUAN.equals(mahjongRule)) {
            deck = new int[108];
            effectivePai = MahjongConstant.SICHUAN_PAI;
            effectiveCode = MahjongConstant.SICHUAN_PAI_CODE;
        } else {
            System.out.println("不该进入这个选项，拍剧初始化错误");
        }

    }

    public ArrayList<String> getPlayersHu() {
        return playersHu;
    }

    public void setPlayersHu(ArrayList<String> playersHu) {
        this.playersHu = playersHu;
    }

    public int[] getDeck() {
        return deck;
    }

    public void setDeck(int[] deck) {
        this.deck = deck;
    }

    public HashMap<Integer, Integer> getDiscardCounnt() {
        return discardCounnt;
    }

    public void setDiscardCounnt(HashMap<Integer, Integer> discardCounnt) {
        this.discardCounnt = discardCounnt;
    }

    public int getLatestTileIndex() {
        return latestTileIndex;
    }

    public void setLatestTileIndex(int latestTileIndex) {
        this.latestTileIndex = latestTileIndex;
    }

    public String getMahjongRule() {
        return mahjongRule;
    }

    public void setMahjongRule(String mahjongRule) {
        this.mahjongRule = mahjongRule;
    }

    public String[] getEffectivePai() {
        return effectivePai;
    }

    public void setEffectivePai(String[] effectivePai) {
        this.effectivePai = effectivePai;
    }

    public int[] getEffectiveCode() {
        return effectiveCode;
    }

    public void setEffectiveCode(int[] effectiveCode) {
        this.effectiveCode = effectiveCode;
    }



    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    public String getZhuangId() {
        return zhuangId;
    }

    public void setZhuangId(String zhuangId) {
        this.zhuangId = zhuangId;
    }

    public ConcurrentHashMap<String, MahjongPlayerCacheBean> getPlayers() {
        return players;
    }

    public void setPlayers(ConcurrentHashMap<String, MahjongPlayerCacheBean> players) {
        this.players = players;
    }

    public int getCurrentOutTilePlayer() {
        return currentOutTilePlayer;
    }

    public void setCurrentOutTilePlayer(int currentOutTilePlayer) {
        this.currentOutTilePlayer = currentOutTilePlayer;
    }

    public boolean isUserDrawTile() {
        return isUserDrawTile;
    }

    public void setUserDrawTile(boolean userDrawTile) {
        isUserDrawTile = userDrawTile;
    }

    public ArrayList<String> getPlayersId() {
        return playersId;
    }

    public void setPlayersId(ArrayList<String> playersId) {
        this.playersId = playersId;
    }

    /**
     * 洗牌
     * 重置牌库顶、底角标
     * 重置弃牌堆、副露堆、花牌堆
     */
    public void shuffle() {
        this.top = 0;
        int length = deck.length;
        this.bottom = length - 1;

        // 重置玩家牌堆
        for (MahjongPlayerCacheBean player : players.values()) {
            player.shuffle();
        }

        List<Integer> deckCopy = Arrays.stream(deck).boxed().collect(Collectors.toList());
        for (int i = 0; i < 10; i++) {
            Collections.shuffle(deckCopy);
        }
        deck = deckCopy.stream().mapToInt(Integer::valueOf).toArray();
    }

    /**
     * 发牌函数
     * 先发12张牌，跳牌阶段每位玩家一张牌，庄家两张
     */
    public void distribute() {
        // 每位玩家发12张牌
        for (int i = 0; i < 12; i++) {
            for (MahjongPlayerCacheBean player : players.values()) {
                distrbuteTites(player);
            }
        }

        for (MahjongPlayerCacheBean player : players.values()) {
            if (zhuangId.equals(player.getUserId())) {
                distrbuteTites(player);
                distrbuteTites(player);
            } else {
                distrbuteTites(player);
            }
            // 手牌排序
            Collections.sort(player.getPlayerTiles());
        }
    }

    /**
     * 给玩家分配一张牌顶的牌
     *
     * @param player
     */
    private void distrbuteTites(MahjongPlayerCacheBean player) {
        player.getPlayerTiles().add(deck[top]);
        top++;
    }

    /**
     * 玩家摸牌
     *
     * @return
     */
    public String playerDrawTile(String userId) {
        for (MahjongPlayerCacheBean player : players.values()) {
            if (player.getUserId().equals(userId)) {
                distrbuteTites(player);
                Collections.sort(player.getPlayerTiles());
                isUserDrawTile = true;
            }
        }
        return MahjongUtil.printTile(deck[top - 1]);
    }

    /**
     * 玩家出牌
     *
     * @return
     */
    public void playerOutTile(String userId, int tileIndex) {
        MahjongPlayerCacheBean player = players.get(userId);
        player.getPlayerTiles().remove(Integer.valueOf(tileIndex));
        player.getDiscardTiles().add(tileIndex);
        Collections.sort(player.getPlayerTiles());
        latestTileIndex = tileIndex;
        addDiscardCounntTiles(tileIndex);
        // 下一玩家阶段
        ternToNextPlayer();

        // 玩家辅助线程，给与玩家吃碰杠胡的提示
        MahjongGameThreadPools.schedule(()->{
            AIPlayerUtil.helpPlayer(tileIndex, userId, this);
        },500,TimeUnit.MILLISECONDS);
    }

    /**
     * 添加牌到弃牌堆
     *
     * @param tileIndex
     */
    private void addDiscardCounntTiles(int tileIndex) {
        if (discardCounnt.containsKey(tileIndex)) {
            Integer tileNum = discardCounnt.get(tileIndex);
            discardCounnt.put(tileIndex, tileNum + 1);
        } else {
            discardCounnt.put(tileIndex, 1);
        }
    }

    /**
     * 将可摸牌id指向下家,且初始化摸牌标记
     */
    private void ternToNextPlayer() {
        // 如果下家已经和牌，则继续指向下下家
        do {
            if (currentOutTilePlayer >= 3) {
                currentOutTilePlayer = 0;
            } else {
                currentOutTilePlayer++;
            }
            isUserDrawTile = false;
        } while (playersHu.contains(playersId.get(currentOutTilePlayer)));

        ArrayList<String> action = AIPlayerUtil.canSpecialAction(zhuangId);

        if (action == null) {
            // 机器人无特殊动作
            // 判断下家是否是机器人，机器人判断出牌策略自动出牌
            MahjongPlayerCacheBean player = players.get(playersId.get(currentOutTilePlayer));
            if (player.isAIPlayer()) {
                // 机器人正常出牌
                schedule = MahjongGameThreadPools.schedule(() -> {
                    AIPlayerUtil.robotAction(zhuangId);
                }, 5, TimeUnit.SECONDS);
            }
        } else {
            // 机器人 胡吃杠碰
            if (MahjongConstant.CHI.equals(action.get(0))){
                // 给与玩家杠碰抢吃的时间
                schedule = MahjongGameThreadPools.schedule(()->{
                    AIPlayerUtil.robotSpecialAction(zhuangId, action);
                },5,TimeUnit.SECONDS);
            } else {
                MahjongGameThreadPools.schedule(()->{
                    AIPlayerUtil.robotSpecialAction(zhuangId, action);
                },500,TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * 将144张牌放入牌库
     */
    public void deckInit() {
        top = 0;
        //一万到九万各四张
        for (int wan = 11; wan <= 19; wan++) {
            for (int j = 0; j < 4; j++) {
                deck[top] = wan;
                top++;
            }
        }
        //一条到九条各四张
        for (int tiao = 21; tiao <= 29; tiao++) {
            for (int j = 0; j < 4; j++) {
                deck[top] = tiao;
                top++;
            }
        }
        //一筒到九筒各四张
        for (int tong = 31; tong <= 39; tong++) {
            for (int j = 0; j < 4; j++) {
                deck[top] = tong;
                top++;
            }
        }
        // 四川牌只有数字牌
        if (!MahjongConstant.SICHUAN.equals(mahjongRule)) {
            //东南西北风各四张
            for (int feng = 41; feng <= 47; feng += 2) {
                for (int j = 0; j < 4; j++) {
                    deck[top] = feng;
                    top++;
                }
            }
            //中发白箭牌各四张
            for (int jian = 51; jian <= 55; jian += 2) {
                for (int j = 0; j < 4; j++) {
                    deck[top] = jian;
                    top++;
                }
            }
        }
        // 只有国标牌有花牌
        if (MahjongConstant.GUOBIAO.equals(mahjongRule)) {
            //花牌总计八张
            for (int hua = 61; hua <= 68; hua++) {
                deck[top] = hua;
                top++;
            }
        }

    }

    /**
     * 返回当前出牌玩家id
     *
     * @return
     */
    public String currentUserId() {
        return playersId.get(currentOutTilePlayer);
    }

    @Override
    public String toString() {
//        return "MahjongGameCacheBean{" +
//                "deck=" + Arrays.toString(deck) +
//                ", top=" + top +
//                ", bottom=" + bottom +
//                ", zhuangId='" + zhuangId + '\'' +
//                ", players=" + players +
//                '}';
        return super.toString();
    }

    /**
     * 用户吃牌
     *
     * @param userId
     * @param fuluCacheBean
     */
    public void playerChiSuccess(String userId, FuluCacheBean fuluCacheBean) {
        MahjongPlayerCacheBean player = players.get(userId);
        player.getFuluTiles().add(fuluCacheBean);
        player.getPlayerTiles().remove(Integer.valueOf(fuluCacheBean.tileNumbers[0]));
        player.getPlayerTiles().remove(Integer.valueOf(fuluCacheBean.tileNumbers[1]));
        addDiscardCounntTiles(fuluCacheBean.tileNumbers[0]);
        addDiscardCounntTiles(fuluCacheBean.tileNumbers[1]);
        setRobotActionThreadDown();
        // 调整到玩家回合
        ternToUser(userId);
    }

    /**
     * 用户碰牌
     *
     * @param userId
     */
    public void playerPengSuccess(String userId) {
        MahjongPlayerCacheBean player = players.get(userId);
        int[] tiles_peng = {latestTileIndex, latestTileIndex, latestTileIndex};
        FuluCacheBean fuluCacheBean = new FuluCacheBean(FuluTypeCacheBean.PENG, tiles_peng, 3, userId);
        player.getFuluTiles().add(fuluCacheBean);
        player.getPlayerTiles().remove(Integer.valueOf(fuluCacheBean.tileNumbers[1]));
        player.getPlayerTiles().remove(Integer.valueOf(fuluCacheBean.tileNumbers[2]));
        addDiscardCounntTiles(fuluCacheBean.tileNumbers[1]);
        addDiscardCounntTiles(fuluCacheBean.tileNumbers[2]);
        setRobotActionThreadDown();
        // 调整到玩家回合
        ternToUser(userId);
    }

    /**
     * 吃碰杠调整到用户回合
     *
     * @param userId
     */
    private void ternToUser(String userId) {
        isUserDrawTile = true;
        currentOutTilePlayer = playersId.indexOf(userId);
    }

    /**
     * 用户暗杠
     */
    public int playerAnGangSuccess(String userId, int tileIndex) {
        MahjongPlayerCacheBean player = players.get(userId);
        int[] tilesAnGang = {tileIndex, tileIndex, tileIndex, tileIndex};
        FuluCacheBean fuluCacheBean = new FuluCacheBean(FuluTypeCacheBean.GANG, tilesAnGang, 0, userId);
        player.getFuluTiles().add(fuluCacheBean);
        for (int i = 0; i < 4; i++) {
            player.getPlayerTiles().remove(Integer.valueOf(fuluCacheBean.tileNumbers[1]));
            addDiscardCounntTiles(fuluCacheBean.tileNumbers[1]);
        }
        // 牌底摸牌
        player.getPlayerTiles().add(deck[bottom]);
        Collections.sort(player.getPlayerTiles());
        bottom--;

        return deck[bottom + 1];
    }

    /**
     * 用户明杠
     *
     * @param userId
     * @return
     */
    public int playerGangSuccess(String userId) {
        MahjongPlayerCacheBean player = players.get(userId);
        int[] tiles_gang = {latestTileIndex, latestTileIndex, latestTileIndex, latestTileIndex};
        FuluCacheBean fuluCacheBean = new FuluCacheBean(FuluTypeCacheBean.GANG, tiles_gang, 4, userId);
        player.getFuluTiles().add(fuluCacheBean);
        for (int i = 0; i < 3; i++) {
            player.getPlayerTiles().remove(Integer.valueOf(fuluCacheBean.tileNumbers[1]));
        }
        setRobotActionThreadDown();
        // 牌底摸牌
        player.getPlayerTiles().add(deck[bottom]);
        Collections.sort(player.getPlayerTiles());
        bottom--;

        // 调整到玩家回合
        ternToUser(userId);
        return deck[bottom + 1];
    }

    /**
     * 玩家打出花牌
     *
     * @param userId
     * @param tileIndex
     * @return
     */
    public int outHuaTiles(String userId, int tileIndex) {
        MahjongPlayerCacheBean player = players.get(userId);
        player.getHuaTiles().add(tileIndex);
        player.getPlayerTiles().remove(Integer.valueOf(tileIndex));
        // 牌底摸牌
        player.getPlayerTiles().add(deck[bottom]);
        Collections.sort(player.getPlayerTiles());

        bottom--;
        return deck[bottom + 1];
    }

    /**
     * 判断用户是否自摸和牌
     *
     * @param userId
     * @return
     */
    public boolean playerCanHu(String userId) {
        MahjongPlayerCacheBean player = players.get(userId);
        if (isAllTilesDraw()) {
            return false;
        }
        return MahjongUtil.canHu(deck[top], player.getPlayerTiles());
    }

    /**
     * 判断用户手牌是否已自摸
     *
     * @param userId
     * @return
     */
    public boolean playerCanHuByHandTiles(String userId) {
        MahjongPlayerCacheBean player = players.get(userId);
        return MahjongUtil.canHu(player.getPlayerTiles());
    }

    /**
     * 暂停当前机器人动作
     */
    public void setRobotActionThreadDown() {
        if (schedule != null) {
            schedule.cancel(true);
        }
    }

    /**
     * 四川牌血战到底
     *
     * @param userId
     */
    public boolean playerHuPaiInSiChuan(String userId) {
        playersHu.add(userId);
        ternToNextPlayer();
        return playersHu.size() >= 3;
    }

    /**
     * 判断是否所有牌堆都已经被摸
     *
     * @return
     */
    public boolean isAllTilesDraw() {
        return top > bottom;
    }

    /**
     * 判断牌桌中是否有机器人
     */
    public boolean hasAIPlayer() {
        for (MahjongPlayerCacheBean player : players.values()) {
            if (player.isAIPlayer()) {
                return true;
            }
        }
        return false;
    }
}
