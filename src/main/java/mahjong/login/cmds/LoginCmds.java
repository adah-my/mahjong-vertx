package mahjong.login.cmds;


import mahjong.core.common.CmdsResponseBody;
import mahjong.core.model.UserModel;
import mahjong.login.cache.LoginCache;
import mahjong.login.cache.bean.LoginCacheBean;
import mahjong.login.dao.LoginDao;
import mahjong.util.GuideUtil;
import mahjong.util.RedisUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author muyi
 * @description: 登录命令
 * @date 2020-10-26 16:34:50
 */
public class LoginCmds {

    /**
     * 注册登录用户列表
     */
    private LoginCache loginCache;
    /**
     * 用户通讯
     */
    private UserModel userModel;
    RedisUtil redis;
    LoginDao loginDao;
    /**
     * 日志
     */
    public static final Logger log = Logger.getLogger(LoginCmds.class.getName());

    public LoginCmds() {
        userModel = UserModel.getInstance();
        loginCache = LoginCache.getInstance();
        redis = RedisUtil.getInstance();
    }

    /**
     * 用户注册
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> register(String userId, String[] commands) {
        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.校验参数
        if (!"".equals(userId)) {
            body.getMessages().add("您已经登陆，如需注册账号，请先登出！");
        } else if (commands.length != 3) {
            body.getMessages().add("请输入正确的命令！例：register aaa aaa");
        } else if (isUserNameExist(commands[1])) {
            body.getMessages().add("用户名已存在！");
        } else {

            // 2.将用户信息添加到注册用户列表
            loginCache.addRegisterUser(commands[1], commands[2]);
            redis.hset("registerUsers",commands[1], commands[2]);

            // 3.设置返回用户注册数据
            body.getMessages().add("注册账号成功");
            body.getMessages().add("== 用户账号：" + commands[1] + " 密码：" + commands[2] + " ==");
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户注册日志：" + bodys.toString());

        return bodys;

    }

    /**
     * 用户登录
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> login(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.校验参数
        if (!"".equals(userId)) {
            body.getMessages().add("您已经登陆，无需重复登录！");
        } else if (commands.length != 3) {
            body.getMessages().add("请输入正确的命令！例：login aaa aaa");
        } else if (!isRegisterUser(commands[1], commands[2])) {
            body.getMessages().add("账号或密码输入错误！");
        } else {

            // 2.将用户存进在线列表
            loginCache.addOnlineUser(commands[1], commands[2]);

            // 3.设置返回用户登录数据
            body.getMessages().add(commands[1] + "登录账号成功，进入主界面");
            // 将用户界面设为主界面
            GuideUtil.userGuide.put(commands[1], 1);
            userId = commands[1];
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "登录日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 查看个人信息
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> view(String userId, String[] commands) {
        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();

        // 1.校验参数
        if ("".equals(userId) || !isUserOnline(userId)) {
            body.getMessages().add("您还没有登陆，请先登录！");
        } else if (GuideUtil.userGuide.get(userId) == 2) {
            body.getMessages().add("您正在聊天房间中，如需查看个人信息，请先退出房间！");
        } else if (commands.length != 1) {
            body.getMessages().add("请输入正确的命令格式！");
        } else {

            // 2.根据用户Id查询用户信息
            LoginCacheBean loginCacheBean = loginCache.getRegisterUserById(userId);

            // 3.设置返回用户信息
            body.getMessages().add("用户账号：" + userId + " 密码：" + loginCacheBean.getPassword());
        }
        body.getUserIds().add(userId);
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "查看信息日志：" + bodys.toString());

        return bodys;
    }

    /**
     * 用户登出
     *
     * @param commands
     * @return
     */
    public List<CmdsResponseBody> logout(String userId, String[] commands) {

        ArrayList<CmdsResponseBody> bodys = new ArrayList<>();
        CmdsResponseBody body = new CmdsResponseBody();
        body.getUserIds().add("");

        // 1.校验参数
        if ("".equals(userId) || !isUserOnline(userId)) {
            body.getMessages().add("您还没有登录，请先登录！");
        } else if (GuideUtil.userGuide.get(userId) == 2) {
            body.getMessages().add("您正在聊天房间中，如需登出，请先退出房间！");
        } else {

            // 2.删除User与socket映射,登出并关闭连接
            userModel.removeUserId(userId);
            loginCache.delOnlineUserById(userId);

            // 3.设置返回用户登出数据
            GuideUtil.userGuide.put(userId, 0);
            body.getMessages().add("用户登出成功！");
        }
        GuideUtil.setUserGuide(body.getMessages(), userId);
        bodys.add(body);

        // 4.输出日志
        log.info("用户" + userId + "登出日志：" + bodys.toString());

        return bodys;

    }

    /**
     * 判断用户是否在线
     *
     * @param userId
     * @return
     */
    public boolean isUserOnline(String userId) {
        return loginCache.getOnlineUserById(userId) != null;
    }

    /**
     * 判断是否注册用户
     *
     * @param userId
     * @param password
     * @return
     */
    public boolean isRegisterUser(String userId, String password) {
        boolean flag;
        LoginCacheBean loginCacheBean = loginCache.getRegisterUserById(userId);
        if (loginCacheBean == null) {
            flag = false;
        } else {
            flag = password.equals(loginCacheBean.getPassword());
        }
        return flag;
    }

    /**
     * 判断用户名是否存在
     *
     * @param userId
     * @return
     */
    public boolean isUserNameExist(String userId) {
        return loginCache.getRegisterUserById(userId) != null;
    }

}
