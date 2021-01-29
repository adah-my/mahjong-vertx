package mahjong.table.cmds;

import mahjong.core.common.CmdsResponseBody;
import mahjong.core.model.UserModel;
import mahjong.game.model.GameModel;
import mahjong.game.util.MahjongConstant;
import mahjong.login.model.LoginModel;
import mahjong.table.cache.MahjongTableCache;
import mahjong.table.cache.bean.MahjongTableBean;
import mahjong.table.util.MahjongTableThreadPool;
import mahjong.util.GuideUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 12:16:18
 */
public class MahjongTableCmds {

    private final LoginModel loginModel;
    private final MahjongTableCache mahjongTableCache;
    private final UserModel userModel;
    private final GameModel gameModel;
    /**
     * 日志
     */
    public static final Logger log = Logger.getLogger(MahjongTableCmds.class.getName());

    public MahjongTableCmds() {
        userModel = UserModel.getInstance();
        loginModel = LoginModel.getInstance();
        mahjongTableCache = MahjongTableCache.getInstance();
        gameModel = GameModel.getInstance();
    }

    /**
     * 进入游戏大厅
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> mahjong(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (isInTable(userId)) {
            body.getMessages().add("您已在牌桌中，如需加入其他游戏大厅，请先退出当前牌桌！");
        } else if (commands.length != 2 || !isInputCorrect(commands[1])) {
            body.getMessages().add("请输入正确的命令！");
        } else {
            // 2.1 设置用户选择的麻将规则
            mahjongTableCache.setUserMahjongRule(userId, getMahjongRule(commands[1]));
            GuideUtil.userGuide.put(userId, 2);

            // 3.返回用户进入牌桌数据
            body.getMessages().add("进入" + getMahjongRuleStr(commands[1]) + "麻将大厅");
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "进入大厅日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 加入牌桌
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> join(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (!isInHall(userId)) {
            body.getMessages().add("请先进入游戏大厅！");
        } else if (commands.length != 2) {
            body.getMessages().add("请输入正确的命令！");
        } else {
            // 2.用户加入牌桌
            String userRule = mahjongTableCache.getUserRule(userId);
            MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userRule, commands[1]);
            if (mahjongTable == null) {
                // 2-1 牌桌不存在则创建牌桌
                mahjongTable = new MahjongTableBean(commands[1], userRule, userId);
                mahjongTableCache.addUserTable(userId, mahjongTable);
                mahjongTableCache.addMahjongTable(mahjongTable);
                // 3.返回用户创建牌桌数据
                body.getMessages().add("成功创建牌桌：" + mahjongTable.getTableName());
                GuideUtil.userGuide.put(userId, 3);
            } else if (mahjongTable.getNumberofPlay() == 4) {
                // 2-2 人数已满
                body.getMessages().add("加入牌桌失败，牌桌人数已满");
            } else {
                // 2-3 用户加入牌桌
                mahjongTable.playerJoinTable(userId);
                mahjongTableCache.addUserTable(userId, mahjongTable);
                if (mahjongTable.getPlays().size() == 4) {
                    readyToGame(mahjongTable);
                }
                // 3.返回用户进入牌桌数据
                CmdsResponseBody playerJoinBody = new CmdsResponseBody();
                playerJoinBody.getMessages().add("玩家：" + userId + "加入牌桌：" + mahjongTable.getTableName());
                playerJoinBody.getUserIds().addAll(mahjongTable.getPlays());
                bodys.add(playerJoinBody);
                GuideUtil.userGuide.put(userId, 3);
            }

        }

        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "进房日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 发起准备
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> ready(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
            GuideUtil.setUserGuide(body.getMessages(), userId);
        } else if (!isInTable(userId)) {
            body.getMessages().add("请先进入牌桌！");
            GuideUtil.setUserGuide(body.getMessages(), userId);
        } else if (commands.length != 1) {
            body.getMessages().add("请输入正确的命令！");
            GuideUtil.setUserGuide(body.getMessages(), userId);
        } else if (isUserReady(userId)) {
            body.getMessages().add("您已准备！！！");
            GuideUtil.setUserGuide(body.getMessages(), userId);
        } else {
            // 2.用户发起准备
            MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
            mahjongTable.ready(userId);
            // 2.1判断是否满足开始游戏条件
            readyToGame(mahjongTable);
            // 3.返回用户发起准备数据
            body.getMessages().add("玩家：" + userId + " 发起游戏准备！");
            body.getUserIds().addAll(mahjongTable.getPlays());
            body.getUserIds().remove(userId);
        }
        body.getUserIds().add(userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "发起准备日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 添加机器人
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> add(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (!isInTable(userId)) {
            body.getMessages().add("请先进入牌桌！");
        } else if (commands.length != 3 || !isAddCorrect(commands)) {
            body.getMessages().add("请输入正确的命令！");
        } else if (!isTableOwner(userId)) {
            body.getMessages().add("您不是房主，没有这个权限！");
        } else if (isTableFull(userId)) {
            body.getMessages().add("牌桌已满，无需再添加机器人！");
        } else {
            // 2 添加机器人
            int robotLevel = Integer.parseInt(commands[2]);
            MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
            String robotName = mahjongTable.robotJoinTable(robotLevel);
            // 2.1 判断是否满足条件开始游戏
            if (mahjongTable.canCountdown()) {
                readyToGame(mahjongTable);
            }

            // 3.返回用户进入牌桌数据
            body.getMessages().add("玩家：" + userId + " 添加机器人：" + robotName + "(等级" + commands[2] + ")");
            body.getMessages().add("== 牌桌内人数为：" + mahjongTable.getNumberofPlay() + "人");
            body.getUserIds().addAll(mahjongTable.getPlays());
            body.getUserIds().remove(userId);
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "发起准备日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 机器人等级详情
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> robot(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (!isInTable(userId)) {
            body.getMessages().add("请先进入牌桌！");
        } else if (commands.length != 1) {
            body.getMessages().add("请输入正确的命令！");
        } else if (!isTableOwner(userId)) {
            body.getMessages().add("您不是房主，没有这个权限！");
        } else {

            // 3.返回机器人等级数据
            body.getMessages().add("麻将机器人：ai等级越高，胡牌的几率越高哦！添加适合你的ai吧：");
            body.getMessages().add("== (等级1)麻将新手AI：add robot 1 ");
            body.getMessages().add("== (等级2)麻将老手AI：add robot 2 (推荐)");
            body.getMessages().add("== (等级3)麻将高手AI：add robot 3 ");
            body.getMessages().add(GuideUtil.dividingLine());

        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "查看机器人日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 踢出用户
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> kick(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.参数校验
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (!isInTable(userId)) {
            body.getMessages().add("请先进入牌桌！");
        } else if (commands.length != 2) {
            body.getMessages().add("请输入正确的命令！");
        } else if (!isTableOwner(userId)) {
            body.getMessages().add("您不是房主，没有这个权限！");
        } else if (!isUserInTable(userId, commands[1])) {
            body.getMessages().add("牌桌内无此用户！");
        } else if (userId.equals(commands[1])) {
            body.getMessages().add("无法操作自己！");
        } else if (isRobot(userId, commands[1])) {
            // 2-1 移除机器人
            MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
            mahjongTable.removePlayer(commands[1]);
            // 3.返回移出机器人数据
            body.getMessages().add("移除机器人" + commands[1] + "成功！！");
            CmdsResponseBody allBody = new CmdsResponseBody();
            allBody.getMessages().add("玩家：" + userId + " 移除机器人" + commands[1]);
            allBody.getUserIds().addAll(mahjongTable.getPlays());
            bodys.add(allBody);
        } else {
            // 2-2 移出用户
            MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
            mahjongTable.removePlayer(commands[1]);
            mahjongTableCache.delUserMahjongTable(commands[1]);
            GuideUtil.userGuide.put(commands[1], 2);

            // 3.返回移出用户数据
            CmdsResponseBody allBody = new CmdsResponseBody();
            body.getMessages().add("踢出玩家成功！");
            allBody.getMessages().add("玩家：" + commands[1] + " 已被踢出牌桌！");
            allBody.getUserIds().add(commands[1]);
            allBody.getUserIds().addAll(mahjongTable.getPlays());
            bodys.add(allBody);
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "发起准备日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 返回主界面
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> back(String userId, String[] commands) {
        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();
        MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);

        // 1.校验参数
        if ("".equals(userId) || !loginModel.isUserOnline(userId)) {
            body.getMessages().add("您还没有登陆，请先登录！");
        } else if (GuideUtil.userGuide.get(userId) == 1) {
            body.getMessages().add("您正在主界面中，无法继续退出！");
        } else if (GuideUtil.userGuide.get(userId) == 2) {
            mahjongTableCache.delUserRule(userId);
            body.getMessages().add("退出大厅成功！");
            GuideUtil.backPrevious(userId);
        } else if (GuideUtil.userGuide.get(userId) == 3) {
            // 2-1 用户退出牌桌
            userBacktoHall(userId, bodys);
            GuideUtil.backPrevious(userId);
            // 3 返回用户退出牌桌数据
            CmdsResponseBody otherBody = new CmdsResponseBody();
            otherBody.getUserIds().addAll(mahjongTable.getPlays());
            otherBody.getMessages().add("玩家：" + userId + " 离开牌桌！！");
            otherBody.getMessages().add(GuideUtil.dividingLine());
            bodys.add(otherBody);
            body.getMessages().add("退出牌桌成功！");
        } else {
            // 2-2 用户退出游戏
            boolean userBackFlag = mahjongTableCache.getUserBackFlag(userId);
            if (userBackFlag) {
                GuideUtil.userGuide.put(userId, 2);
                gameModel.playerBackToTable(userId);
                mahjongTableCache.delUserMahjongTable(userId);
                mahjongTable.changePlayerToRobot(userId);
                // 2-2.1 判断牌桌内是否还存在真人玩家，不存在则销毁牌桌
                if (!mahjongTable.hasRealPlayer()) {
                    mahjongTableCache.delMahjongTable(mahjongTable.getMahjongRule(), mahjongTable);
                    System.out.println("牌桌内无真人玩家，牌桌销毁！");
                }
                // 3.返回用户退出游戏数据
                body.getMessages().add("退出游戏成功！");
            } else {
                body.getMessages().add("您在与其他玩家进行游戏，确定要退出吗！！");
                body.getMessages().add("== 再次输入 back 命令将会退出当前游戏！！");
            }
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "退出日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 倒计时开始游戏
     *
     * @param mahjongTable
     */
    private void countDown(MahjongTableBean mahjongTable) {

        Integer count = MahjongTableThreadPool.getScheduleCount(mahjongTable.getTableName());
        System.out.println(count);
        List<String> unPlayerReady = mahjongTable.isAllPlayerReady();
        if (unPlayerReady.size() == 0 && mahjongTable.getPlays().size() == 4) {
            // 开始游戏
            System.out.println("开始游戏");
            gameModel.startGame(mahjongTable.getTableOwner(), mahjongTable.getMahjongRule(), mahjongTable.getPlays(), mahjongTable.getIsPlaysRobot(), mahjongTable.getRobotLevel());
            MahjongTableThreadPool.cancelScheduleFuture(mahjongTable.getTableName());
        } else if (mahjongTable.getPlays().size() != 4) {
            for (String playerId : mahjongTable.getPlays()) {
                if (!mahjongTable.getIsPlaysRobot().get(playerId)) {
                    userModel.writeToUser(playerId, "== 房间人数不足4人，开始游戏失败！！");
                    userModel.writeToUser(playerId, GuideUtil.dividingLine());
                }
            }
            mahjongTable.setReadyFlag(false);
            MahjongTableThreadPool.cancelScheduleFuture(mahjongTable.getTableName());
        }

        // 判断是否还有未准备玩家
        unPlayerReady = mahjongTable.isAllPlayerReady();
        if (mahjongTable.getPlays().size() == 4 && unPlayerReady.size() != 0 && count == 0) {
            System.out.println("开始失败");
            for (String player : mahjongTable.getPlays()) {
                userModel.writeToUser(player, "== 玩家：" + unPlayerReady.toString() + "未准备，已被移出牌桌！");
            }
            for (String playerId : unPlayerReady) {
                userModel.writeToUser(playerId, "== 您已被移出牌桌！");
                mahjongTableCache.delUserMahjongTable(playerId);
                mahjongTable.delUserByUserId(playerId);
                GuideUtil.userGuide.put(playerId, 2);
            }
            if (unPlayerReady.contains(mahjongTable.getTableOwner())) {
                String tableOwner = mahjongTable.getTableOwner();
                // 房主退出牌桌,随机选中房内玩家成为房主
                String newTableOwner = mahjongTable.selectNewTableOwner();
                if ("".equals(newTableOwner)) {

                } else {
                    ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
                    CmdsResponseBody playBody = new CmdsResponseBody();
                    playBody.getMessages().add("房主" + tableOwner + "离开牌桌");
                    playBody.getMessages().add("== 玩家" + newTableOwner + "成为新房主");
                    playBody.getMessages().add(GuideUtil.dividingLine());
                    playBody.setUserIds(mahjongTable.getPlays());
                    bodys.add(playBody);
                    userModel.writeToUser(bodys);
                }
            }
            mahjongTable.setReadyFlag(false);
        }

    }

    /**
     * 复用规则名常量
     *
     * @param command
     * @return
     */
    private String getMahjongRule(String command) {
        if (MahjongConstant.GUOBIAO.equals(command)) {
            return MahjongConstant.GUOBIAO;
        } else if (MahjongConstant.GUANGDONG.equals(command)) {
            return MahjongConstant.GUANGDONG;
        } else if (MahjongConstant.SICHUAN.equals(command)) {
            return MahjongConstant.SICHUAN;
        }
        return "";
    }

    /**
     * 获取规则
     *
     * @param mahjongRule
     * @return
     */
    private String getMahjongRuleStr(String mahjongRule) {
        String mahjongRuleStr = "";
        if (MahjongConstant.GUOBIAO.equals(mahjongRule)) {
            mahjongRuleStr = "国标";
        } else if (MahjongConstant.GUANGDONG.equals(mahjongRule)) {
            mahjongRuleStr = "广东";
        } else if (MahjongConstant.SICHUAN.equals(mahjongRule)) {
            mahjongRuleStr = "四川";
        }
        return mahjongRuleStr;
    }

    /**
     * 判断用户是否已经准备
     *
     * @param userId
     * @return
     */
    private boolean isUserReady(String userId) {
        MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
        if (mahjongTable.canCountdown()) {
            readyToGame(mahjongTable);
        }
        return mahjongTable.getPlaysReady().get(userId);
    }

    /**
     * 准备协同开始倒计时
     *
     * @param mahjongTable
     */
    private void readyToGame(MahjongTableBean mahjongTable) {

        if (mahjongTable.canCountdown() && !mahjongTable.isReadyFlag()) {
            mahjongTable.setReadyFlag(true);
            for (String player : mahjongTable.getPlays()) {
                userModel.writeToUser(player, "== 条件满足，开始游戏倒计时！");
                userModel.writeToUser(player, GuideUtil.dividingLine());
            }
            MahjongTableThreadPool.scheduleAtFixedRateTenTimes(() -> {
                countDown(mahjongTable);
            }, 1, 1, TimeUnit.SECONDS, mahjongTable.getTableName());
        }
    }

    /**
     * 判断add命令是否输入正确
     *
     * @param commands
     * @return
     */
    private boolean isAddCorrect(String[] commands) {
        if ("robot".equals(commands[1])) {
            try {
                int robotLevel = Integer.parseInt(commands[2]);
                if (robotLevel >= 1 && robotLevel <= 3) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    /**
     * 判断是否
     *
     * @param userId
     * @param robotName
     * @return
     */
    private boolean isRobot(String userId, String robotName) {
        MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
        return mahjongTable.getIsPlaysRobot().get(robotName);
    }

    /**
     * 用户退出牌桌
     *
     * @param userId
     * @param bodys
     */
    private void userBacktoHall(String userId, ArrayList<CmdsResponseBody> bodys) {
        MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
        mahjongTable.delUserByUserId(userId);
        mahjongTableCache.delUserMahjongTable(userId);
        // 牌桌人数为0，则销毁牌桌
        if (mahjongTable.getNumberofPlay() == 0) {
            System.out.println("牌桌人数为0，销毁牌桌");
            mahjongTableCache.delMahjongTable(mahjongTable.getMahjongRule(), mahjongTable);
        } else if (userId.equals(mahjongTable.getTableOwner())) {
            // 房主退出牌桌,随机选中房内玩家成为房主
            String newTableOwner = mahjongTable.selectNewTableOwner();
            if ("".equals(newTableOwner)) {

            } else {
                CmdsResponseBody playBody = new CmdsResponseBody();
                playBody.getMessages().add("房主" + userId + "离开牌桌");
                playBody.getMessages().add("== 玩家" + newTableOwner + "成为新房主");
                playBody.getMessages().add(GuideUtil.dividingLine());
                playBody.setUserIds(mahjongTable.getPlays());
                bodys.add(playBody);
            }
        }
    }

    /**
     * 判断牌桌是否已满
     *
     * @param userId
     * @return
     */
    private boolean isTableFull(String userId) {
        MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
        return mahjongTable.getPlays().size() == 4;
    }

    /**
     * 判断用户输入是否正确
     *
     * @param rule
     * @return
     */
    private boolean isInputCorrect(String rule) {
        boolean flag;
        if (MahjongConstant.GUOBIAO.equals(rule)) {
            flag = true;
        } else if (MahjongConstant.GUANGDONG.equals(rule)) {
            flag = true;
        } else if (MahjongConstant.SICHUAN.equals(rule)) {
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }

    /**
     * 判断用户是否在游戏大厅中
     *
     * @param userId
     * @return
     */
    private boolean isInHall(String userId) {
        Integer userGuide = GuideUtil.userGuide.get(userId);
        return userGuide == 2;
    }

    /**
     * 判断用户是否在游戏大厅中
     *
     * @param userId
     * @return
     */
    private boolean isInTable(String userId) {
        Integer userGuide = GuideUtil.userGuide.get(userId);
        return userGuide == 3;
    }

    /**
     * 判断用户是否是房主
     *
     * @param userId
     * @return
     */
    private boolean isTableOwner(String userId) {
        MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
        return userId.equals(mahjongTable.getTableOwner());
    }

    /**
     * 判断用户是否存在牌桌内
     *
     * @param userId
     * @param user
     * @return
     */
    private boolean isUserInTable(String userId, String user) {
        MahjongTableBean mahjongTable = mahjongTableCache.getMahjongTable(userId);
        return mahjongTable.getPlays().contains(user);
    }

}
