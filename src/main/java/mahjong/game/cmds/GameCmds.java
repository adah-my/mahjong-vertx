package mahjong.game.cmds;

import mahjong.core.common.CmdsResponseBody;
import mahjong.game.cache.GameCache;
import mahjong.game.cache.bean.FuluCacheBean;
import mahjong.game.cache.bean.FuluTypeCacheBean;
import mahjong.game.cache.bean.MahjongGameCacheBean;
import mahjong.game.cache.bean.MahjongPlayerCacheBean;
import mahjong.game.util.AIPlayerUtil;
import mahjong.game.util.MahjongUtil;
import mahjong.login.model.LoginModel;
import mahjong.util.GuideUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 18:51:00
 */
public class GameCmds {

    private final LoginModel loginModel;
    private final GameCache gameCache;

    public static final Logger log = Logger.getLogger(GameCmds.class.getName());

    public GameCmds() {
        loginModel = LoginModel.getInstance();
        gameCache = GameCache.getInstance();
    }

    /**
     * 出牌
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> out(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (isNotInGame(userId)) {
            body.getMessages().add("您不在游戏中，请先进入游戏！");
        } else if (commands.length != 2 || isInputNotCorrect(userId, commands[1])) {
            body.getMessages().add("请输入正确的命令！");
        } else if (isHuAlready(userId)) {
            body.getMessages().add("您已胡牌，无法进行此操作！");
        } else if (!isTernUserRound(userId)) {
            body.getMessages().add("还未到你的出牌回合！");
        } else if (!gameCache.getMahjongGame(userId).isUserDrawTile()) {
            body.getMessages().add("你还未摸牌，请先摸牌再出牌！");
        } else if (!isPlayerHasTile(userId, Integer.parseInt(commands[1]))) {
            body.getMessages().add("手牌中不存在：" + MahjongUtil.printTile(Integer.parseInt(commands[1])) + " ！！");
        } else if (isAllTilesDraw(userId)) {
            // 2-1 排队所有牌已被摸起
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            int tileCode = mahjongGame.outHuaTiles(userId, Integer.parseInt(commands[1]));
            // 2-1.1 游戏结束，用户返回牌桌
            playerHelperThread(mahjongGame);

            // 3.返回最后出牌信息与用户返回牌桌信息
            body.getMessages().add("玩家：" + userId + " 出牌：" + MahjongUtil.printTile(tileCode));
            body.getMessages().add(GuideUtil.dividingLine());
            body.getMessages().add("== 游戏结束！！牌堆所有牌都已被摸起！！！");
            body.getUserIds().addAll(getRealPlayer(mahjongGame));
            body.getUserIds().remove(userId);
        } else if (Integer.parseInt(commands[1]) > 60) {
            // 2-2.1 用户出牌为花牌
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            int tileCode = mahjongGame.outHuaTiles(userId, Integer.parseInt(commands[1]));
            body.getMessages().add("打出一张花牌，从牌尾摸牌，你摸到：" + MahjongUtil.printTile(tileCode));
            // 2-2.2 判断是否自摸和牌
            if (mahjongGame.playerCanHuByHandTiles(userId)) {
                MahjongUtil.playerHuTiles(mahjongGame, userId, AIPlayerUtil.getNewPlayerBody(mahjongGame));
            } else {
                // 3.返回用户打出花牌数据
                body.getMessages().add("== 请继续使用命令 out 出牌！！");
                bodys = AIPlayerUtil.getNewPlayerBody(mahjongGame);
                bodys.get(0).getMessages().add("玩家：" + userId + " 打出了一张花牌：" + MahjongUtil.printTile(Integer.parseInt(commands[1])));
                bodys.get(0).getMessages().add("== 玩家：" + userId + " 从牌尾摸一张牌！！");
            }

        } else {
            // 2-3 用户正常出牌
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            int tileCode = Integer.parseInt(commands[1]);
            mahjongGame.playerOutTile(userId, tileCode);

            // 3. 返回用户正常出牌数据
            bodys = AIPlayerUtil.getNewPlayerBody(mahjongGame);
            bodys.get(0).getMessages().add("玩家：" + userId + " 出牌：" + MahjongUtil.printTile(tileCode));
            bodys.get(0).getMessages().add("== 下家为玩家：" + mahjongGame.getPlayersId().get(mahjongGame.getCurrentOutTilePlayer()));
            bodys.get(0).getMessages().add(GuideUtil.dividingLine());
            MahjongPlayerCacheBean nextPlayer = mahjongGame.getPlayers().get(mahjongGame.currentUserId());
            if (!nextPlayer.isAIPlayer()) {
                CmdsResponseBody nextPlayerBody = new CmdsResponseBody();
                nextPlayerBody.getMessages().add("你的回合，请使用命令 draw 摸牌，再出牌");
                nextPlayerBody.getMessages().add(GuideUtil.dividingLine());
                nextPlayerBody.getUserIds().add(mahjongGame.currentUserId());
                bodys.add(nextPlayerBody);
            }
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "出牌日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 摸牌
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> draw(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (isNotInGame(userId)) {
            body.getMessages().add("您不在游戏中，请先进入游戏！");
        } else if (commands.length != 1) {
            body.getMessages().add("请输入正确的命令！");
        } else if (isHuAlready(userId)) {
            body.getMessages().add("您已胡牌，无法进行此操作！");
        } else if (!isTernUserRound(userId)) {
            body.getMessages().add("还未到你的出牌回合！");
        } else if (gameCache.getMahjongGame(userId).isUserDrawTile()) {
            body.getMessages().add("该回合已摸牌，无法重复摸牌！");
        } else if (gameCache.getMahjongGame(userId).isAllTilesDraw()) {
            body.getMessages().add("牌堆已经全部摸起！");
        } else if (playerCanHu(userId)) {
            // 2-1 用户自摸和牌
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            String tileStr = mahjongGame.playerDrawTile(userId);
            // 3.返回用户自摸和牌数据
            body.getMessages().add("你摸到的牌为：" + tileStr);
            MahjongUtil.playerHuTiles(mahjongGame, userId, AIPlayerUtil.getNewPlayerBody(mahjongGame));
        } else {
            // 2-2 用户正常摸牌
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            String tileStr = mahjongGame.playerDrawTile(userId);

            // 3.返回用户摸牌数据
            body.getMessages().add("你摸到的牌为：" + tileStr);
            body.getMessages().add("== 请使用命令 out 出牌！");
            bodys = AIPlayerUtil.getNewPlayerBody(mahjongGame);
            bodys.get(0).getMessages().add("玩家：" + userId + "摸牌，正在等待出牌");
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "摸牌日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 吃
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> chi(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (isNotInGame(userId)) {
            body.getMessages().add("您不在游戏中，请先进入游戏！");
        } else if (!isInGuoBiaoMahjong(userId)) {
            body.getMessages().add("只有国标麻将才能吃牌！");
        } else if (isHuAlready(userId)) {
            body.getMessages().add("您已胡牌，无法进行此操作！");
        } else if (gameCache.getMahjongGame(userId).isUserDrawTile()) {
            body.getMessages().add("玩家出牌回合无法吃牌！");
        } else if (commands.length != 3 || isInputNotCorrect(userId, commands[1]) || isInputNotCorrect(userId, commands[2])) {
            body.getMessages().add("请输出正确的值！");
        } else if (!isShangJia(userId)) {
            body.getMessages().add("只能吃上家的牌！");
        } else if (!canChi(userId, commands)) {
            body.getMessages().add("你打出的牌无法吃掉该牌！");
        } else {
            // 2.1 用户吃牌
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            int tile1 = Integer.parseInt(commands[1]);
            int tile2 = Integer.parseInt(commands[2]);
            int latestTileIndex = gameCache.getMahjongGame(userId).getLatestTileIndex();
            int[] chiTiles = {tile1, tile2, latestTileIndex};
            FuluCacheBean fuluCacheBean = new FuluCacheBean(FuluTypeCacheBean.CHI, chiTiles, 2, userId);
            mahjongGame.playerChiSuccess(userId, fuluCacheBean);

            // 3.返回用户吃牌数据
            body.getMessages().add("吃牌成功！！请使用命令 out 出牌！！");
            bodys = AIPlayerUtil.getNewPlayerBody(mahjongGame);
            bodys.get(0).getMessages().add("玩家：" + userId + " 吃牌！！！ 副露牌：" + MahjongUtil.printTiles(MahjongUtil.getNewArr(chiTiles)));
            bodys.get(0).getMessages().add("== 正在等待玩家：" + userId + " 出牌");

        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "吃牌日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 碰
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> peng(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (isNotInGame(userId)) {
            body.getMessages().add("您不在游戏中，请先进入游戏！");
        } else if (commands.length != 1) {
            body.getMessages().add("请输入正确的命令！");
        } else if (isHuAlready(userId)) {
            body.getMessages().add("您已胡牌，无法进行此操作！");
        } else if (gameCache.getMahjongGame(userId).isUserDrawTile()) {
            body.getMessages().add("玩家出牌回合无法碰牌！");
        } else if (!canPeng(userId)) {
            body.getMessages().add("无法碰牌，手牌不满足碰牌的条件！");
        } else {
            // 2.用户碰牌
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            int currenTile = mahjongGame.getLatestTileIndex();
            mahjongGame.playerPengSuccess(userId);

            // 3.返回用户碰牌数据
            body.getMessages().add("碰牌成功！！请使用命令 out 出牌！！");
            bodys = AIPlayerUtil.getNewPlayerBody(mahjongGame);
            bodys.get(0).getMessages().add("玩家：" + userId + " 碰牌！！！ 副露牌：" + MahjongUtil.printTiles(new int[]{currenTile, currenTile, currenTile}));
            bodys.get(0).getMessages().add("== 正在等待玩家：" + userId + " 出牌");
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "碰牌日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 杠
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> gang(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (isNotInGame(userId)) {
            body.getMessages().add("您不在游戏中，请先进入游戏！");
        } else if (commands.length > 2) {
            body.getMessages().add("请输入正确的命令！");
        } else if (isHuAlready(userId)) {
            body.getMessages().add("您已胡牌，无法进行此操作！");
        } else if (gameCache.getMahjongGame(userId).isUserDrawTile() && !isTernUserRound(userId)) {
            body.getMessages().add("玩家出牌回合无法杠牌！");
        } else if (commands.length == 2 && canAnGang(userId, commands) && isTernUserRound(userId) && gameCache.getMahjongGame(userId).isUserDrawTile()) {
            // 2-1 用户暗杠
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            int tileIndex = mahjongGame.playerAnGangSuccess(userId, Integer.parseInt(commands[1]));

            // 2-1.1 判断是否自摸和牌
            if (mahjongGame.playerCanHuByHandTiles(userId)) {
                MahjongUtil.playerHuTiles(mahjongGame, userId, AIPlayerUtil.getNewPlayerBody(mahjongGame));
            } else {
                bodys = AIPlayerUtil.getNewPlayerBody(mahjongGame);
                bodys.get(0).getMessages().add("玩家：" + userId + " 暗杠！！正在等待出牌");

            }
            // 3.返回用户进入游戏数据
            body.getMessages().add("暗杠成功！！");
            body.getMessages().add("== 牌尾摸牌：你摸到的牌为：" + MahjongUtil.printTile(tileIndex));
            body.getMessages().add("== 请使用命令 out 出牌！！");

        } else if (commands.length == 2) {
            body.getMessages().add("无法暗杠！");
        } else if (!canGang(userId)) {
            body.getMessages().add("无法杠牌，手牌不满足杠牌的条件！");
        } else {
            // 2-2 用户明杠
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            int tileIndex = mahjongGame.playerGangSuccess(userId);

            // 2-2.1 判断是否自摸和牌
            if (mahjongGame.playerCanHuByHandTiles(userId)) {
                MahjongUtil.playerHuTiles(mahjongGame, userId, AIPlayerUtil.getNewPlayerBody(mahjongGame));
            } else {
                bodys = AIPlayerUtil.getNewPlayerBody(mahjongGame);
                bodys.get(0).getMessages().add("玩家：" + userId + " 明杠：" + MahjongUtil.printTile(mahjongGame.getLatestTileIndex()) + "！！正在等待出牌");
            }
            // 3.返回用户进入游戏数据
            body.getMessages().add("明杠成功！！");
            body.getMessages().add("== 牌尾摸牌：你摸到的牌为：" + MahjongUtil.printTile(tileIndex));
            body.getMessages().add("== 请使用命令 out 出牌！！");
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "杠牌日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 胡
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> hu(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (isNotInGame(userId)) {
            body.getMessages().add("您不在游戏中，请先进入游戏！");
        } else if (commands.length != 1) {
            body.getMessages().add("请输入正确的命令！");
        } else if (isHuAlready(userId)) {
            body.getMessages().add("您已胡牌，无法进行此操作！");
        } else if (isGuangdongMahjong(userId)) {
            body.getMessages().add("广东麻将不支持胡牌，只能自摸和牌！！！");
        } else if (gameCache.getMahjongGame(userId).isUserDrawTile()) {
            body.getMessages().add("玩家出牌回合无法胡牌！");
        } else if (!canHuWithLatesTile(userId)) {
            body.getMessages().add("手牌不满足胡牌的条件！！！");
        } else {
            // 2.用户胡牌
            MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
            mahjongGame.setRobotActionThreadDown();

            // 3.返回用户进入游戏数据
            MahjongUtil.playerHuTiles(mahjongGame, userId, AIPlayerUtil.getNewPlayerBody(mahjongGame));

        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "胡牌日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 玩家辅助线程
     *
     * @param mahjongGame
     */
    public void playerHelperThread(MahjongGameCacheBean mahjongGame) {
        MahjongUtil.backToRoom(mahjongGame);
    }

    /**
     * 获得真人玩家列表
     *
     * @param mahjongGame
     * @return
     */
    private ArrayList<String> getRealPlayer(MahjongGameCacheBean mahjongGame) {
        @SuppressWarnings("unchecked")
        ArrayList<String> playersId = (ArrayList<String>) mahjongGame.getPlayersId().clone();
        for (MahjongPlayerCacheBean player : mahjongGame.getPlayers().values()) {
            if (player.isAIPlayer()) {
                playersId.remove(player.getUserId());
            }
        }
        return playersId;
    }

    /**
     * 判断牌堆的所有牌都已经被摸
     *
     * @param userId
     * @return
     */
    private boolean isAllTilesDraw(String userId) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        return mahjongGame.isAllTilesDraw();
    }

    /**
     * 判断是否为国标麻将
     *
     * @param userId
     * @return
     */
    private boolean isInGuoBiaoMahjong(String userId) {
        String mahjongRule = gameCache.getMahjongGame(userId).getMahjongRule();
        return "guobiao".equals(mahjongRule);
    }

    /**
     * 判断是否上家打出的牌
     *
     * @param userId
     * @return
     */
    private boolean isShangJia(String userId) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        String currentPlayer = mahjongGame.getPlayersId().get(mahjongGame.getCurrentOutTilePlayer());
        return userId.equals(currentPlayer);
    }

    /**
     * 判断是否已经胡牌
     *
     * @param userId
     * @return
     */
    private boolean isHuAlready(String userId) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        return mahjongGame.getPlayersHu().contains(userId);
    }

    /**
     * 判断是否广东麻将
     *
     * @param userId
     * @return
     */
    private boolean isGuangdongMahjong(String userId) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        return "guangdong".equals(mahjongGame.getMahjongRule());
    }

    /**
     * 判断用户是否能胡牌
     *
     * @param userId
     * @return
     */
    private boolean canHuWithLatesTile(String userId) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        MahjongPlayerCacheBean player = mahjongGame.getPlayers().get(userId);
        int latestTileIndex = mahjongGame.getLatestTileIndex();
        return MahjongUtil.canHu(latestTileIndex, player.getPlayerTiles());
    }

    /**
     * 四川血战到底胡牌
     *
     * @param userId
     * @param mahjongGame
     * @param body
     * @param bodys
     */
    private void siChuanHuPai(String userId, MahjongGameCacheBean mahjongGame, CmdsResponseBody body, ArrayList<CmdsResponseBody> bodys) {
        boolean playerHuPaiInSiChuan = mahjongGame.playerHuPaiInSiChuan(userId);
        CmdsResponseBody newBody = new CmdsResponseBody();
        newBody.getMessages().add("玩家：" + userId + "和牌！！");
        newBody.getMessages().add("== 玩家：" + userId + "和牌！！");
        newBody.getMessages().add(GuideUtil.dividingLine());
        newBody.getMessages().add("== 玩家(" + userId + ")手牌：" + MahjongUtil.printTiles(mahjongGame.getPlayers().get(userId).getPlayerTiles()));

        if (playerHuPaiInSiChuan) {
            newBody.getMessages().add("== 游戏结束！！恭喜玩家：" + mahjongGame.getPlayersHu().get(0) + "获得本局游戏的胜利！！！");
            playerHelperThread(mahjongGame);
        } else {
            newBody.getMessages().add("== 四川麻将血战模式：和牌达到三家或者牌堆全部摸光才结束！！！");
        }
        newBody.getUserIds().addAll(getRealPlayer(mahjongGame));
        bodys.add(newBody);
    }

    /**
     * 玩家获胜反馈
     *
     * @param body
     */
    private void playerWinMessagesOnlyPlayer(CmdsResponseBody body) {
        body.getMessages().add("== 恭喜你！自摸和牌了！！！！！你获得了本局游戏的胜利！！！");
        body.getMessages().add(GuideUtil.dividingLine());
    }

    /**
     * 玩家获胜其他玩家反馈
     *
     * @param userId
     */
    private ArrayList<CmdsResponseBody> playerWinMessages(String userId) {
        ArrayList<CmdsResponseBody> bodys = AIPlayerUtil.getNewPlayerBody(gameCache.getMahjongGame(userId));
        bodys.get(0).getMessages().add("玩家：" + userId + " 和牌！！！获得了本局游戏的胜率！！！");
        bodys.get(0).getMessages().add("== 请不要气馁，继续新一局游戏吧！！！");
        bodys.get(0).getUserIds().remove(userId);
        return bodys;
    }

    /**
     * 添加发送userId以外的所有玩家
     *
     * @param userId
     * @param messages
     * @param bodys
     */
    private void addMessageToPlayersNoUser(String userId, ArrayList<String> messages, ArrayList<CmdsResponseBody> bodys) {

        CmdsResponseBody allBody = new CmdsResponseBody();
        for (String msg : messages) {
            allBody.getMessages().add(msg);
        }
        allBody.getMessages().add(GuideUtil.dividingLine());
        MahjongGameCacheBean mahjongTable = gameCache.getMahjongGame(userId);
        allBody.getUserIds().addAll(getRealPlayer(mahjongTable));
        bodys.add(allBody);
        allBody.getUserIds().remove(userId);
    }

    /**
     * 判断玩家是否自摸和牌
     *
     * @param userId
     * @return
     */
    private boolean playerCanHu(String userId) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        return mahjongGame.playerCanHu(userId);
    }

    /**
     * 判断该id牌是否存在玩家手牌中
     *
     * @param userId
     * @param tileIndex
     * @return
     */
    private boolean isPlayerHasTile(String userId, int tileIndex) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        MahjongPlayerCacheBean player = mahjongGame.getPlayers().get(userId);
        return player.getPlayerTiles().contains(tileIndex);
    }

    /**
     * 判断用户是否可以暗杠
     *
     * @param userId
     * @return
     */
    private boolean canAnGang(String userId, String[] commands) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        MahjongPlayerCacheBean player = mahjongGame.getPlayers().get(userId);
        int frequency;
        try {
            frequency = Collections.frequency(player.getPlayerTiles(), Integer.parseInt(commands[1]));
        } catch (NumberFormatException e) {
            return false;
        }
        return frequency == 4;
    }

    /**
     * 判断玩家是否可以杠牌
     *
     * @param userId
     * @return
     */
    private boolean canGang(String userId) {
        int frequency = getFrequency(userId);
        return frequency >= 3;
    }

    /**
     * 获取玩家手牌中当前id的数量
     *
     * @param userId
     * @return
     */
    private int getFrequency(String userId) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        int latestTileIndex = mahjongGame.getLatestTileIndex();
        MahjongPlayerCacheBean player = mahjongGame.getPlayers().get(userId);
        return Collections.frequency(player.getPlayerTiles(), latestTileIndex);
    }

    /**
     * 判断玩家是否可以碰牌
     *
     * @param userId
     * @return
     */
    private boolean canPeng(String userId) {
        int frequency = getFrequency(userId);
        return frequency >= 2;
    }

    /**
     * 判断用户打出的牌是否可以吃掉最新出的牌
     *
     * @param userId
     * @param commands
     * @return
     */
    private boolean canChi(String userId, String[] commands) {
        int tile1 = Integer.parseInt(commands[1]);
        int tile2 = Integer.parseInt(commands[2]);
        int latestTileIndex = gameCache.getMahjongGame(userId).getLatestTileIndex();
        return MahjongUtil.isChi(new int[]{tile1, tile2, latestTileIndex});
    }

    /**
     * 判断是否为用户的出牌回合
     *
     * @param userId
     * @return
     */
    private boolean isTernUserRound(String userId) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        return userId.equals(mahjongGame.currentUserId());
    }

    /**
     * 判断用户输入的操作值是否不合法
     *
     * @param userId
     * @param operationCodeStr
     * @return
     */
    private boolean isInputNotCorrect(String userId, String operationCodeStr) {
        MahjongGameCacheBean mahjongGame = gameCache.getMahjongGame(userId);
        try {
            int operationCode = Integer.parseInt(operationCodeStr);
            List<Integer> codes = Arrays.stream(mahjongGame.getEffectiveCode()).boxed().collect(Collectors.toList());
            if (codes.contains(operationCode)) {
                return false;
            }
        } catch (Exception e) {
        }
        return true;
    }

    /**
     * 判断是否在游戏里
     *
     * @param userId
     * @return
     */
    private boolean isNotInGame(String userId) {
        Integer guideNum = GuideUtil.userGuide.get(userId);
        return guideNum != 4;
    }

}
