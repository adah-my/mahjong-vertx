package mahjong.login.cache.impl;



import mahjong.login.cache.LoginCache;
import mahjong.login.cache.bean.LoginCacheBean;
import mahjong.login.util.SerializationUtil;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author muyi
 * @description:
 * @date 2020-11-02 16:12:17
 */
public class LoginCacheImpl implements LoginCache {

    /**
     * 注册用户表
     */
    private ConcurrentHashMap<String, String> registerUsers;
    /**
     * 在线用户表
     */
    private ConcurrentHashMap<String, String> onlineUsers;

    private volatile static LoginCache instance;

    private LoginCacheImpl() {
        registerUsers = SerializationUtil.reader();
        onlineUsers = new ConcurrentHashMap<>();
        System.out.println("注册用户："+registerUsers.toString());
    }

    /**
     *
     * @return LoginCache
     */
    public static LoginCache getInstance() {
        if (instance == null) {
            synchronized (LoginCacheImpl.class) {
                if (instance == null) {
                    instance = new LoginCacheImpl();
                }
            }
        }
        return instance;
    }

    /**
     *  根据Id查询注册用户
     * @param userId
     * @return
     */
    @Override
    public LoginCacheBean getRegisterUserById(String userId) {
        LoginCacheBean loginCacheBean;
        if (isUsernameEmpty(userId)) {
            loginCacheBean = null;
        } else if (!isUsernameExist(userId)) {
            loginCacheBean = null;
        } else {
            loginCacheBean = new LoginCacheBean(userId, registerUsers.get(userId));
        }
        return loginCacheBean;
    }

    /**
     * 根据Id获取查询用户
     *
     * @param userId
     * @return
     */
    @Override
    public LoginCacheBean getOnlineUserById(String userId) {
        LoginCacheBean loginCacheBean;
        if (isUsernameEmpty(userId)) {
            loginCacheBean = null;
        } else if (!isUserOnline(userId)) {
            loginCacheBean = null;
        } else {
            loginCacheBean = new LoginCacheBean(userId, onlineUsers.get(userId));
        }
        return loginCacheBean;
    }

    /**
     * 判断用户名是否存在
     *
     * @param userId
     * @return
     */
    @Override
    public boolean isUsernameExist(String userId) {
        if (isUsernameEmpty(userId)) {
            return false;
        } else {
            return registerUsers.containsKey(userId);
        }
    }

    /**
     * 判断用户在列表之中
     *
     * @param userId
     * @return
     */
    @Override
    public boolean isUserOnline(String userId) {
        if (isUsernameEmpty(userId)) {
            return false;
        } else {
            return onlineUsers.containsKey(userId);
        }
    }

    /**
     * 通过Id删除在线用户
     *
     * @param userId
     * @return
     */
    @Override
    public int delOnlineUserById(String userId) {
        int flag;
        if (isUsernameEmpty(userId)) {
            flag = -1;
        } else if (!isUserOnline(userId)) {
            flag = -1;
        } else {
            onlineUsers.remove(userId);
            flag = 1;
        }
        return flag;
    }

    /**
     * 添加注册用户
     *
     * @param userId
     * @param password
     * @return
     */
    @Override
    public int addRegisterUser(String userId, String password) {
        int flag;
        if (isUserEmpty(userId, password)) {
            flag = -1;
        } else {
            if (registerUsers.containsKey(userId)) {
                flag = -1;
            } else {
                registerUsers.put(userId, password);
                SerializationUtil.write(registerUsers);
                flag = 1;
            }

        }
        return flag;
    }

    /**
     * 添加在线用户
     *
     * @param userId
     * @param password
     * @return
     */
    @Override
    public int addOnlineUser(String userId, String password) {
        int flag;
        if (isUserEmpty(userId, password)) {
            flag = -1;
        } else {
            onlineUsers.put(userId, password);
            flag = 1;
        }
        return flag;
    }

    /**
     * 判断是否为空
     *
     * @param userId
     * @param password
     * @return
     */
    @Override
    public boolean isUserEmpty(String userId, String password) {
        boolean flag;
        if (userId == null || "".equals(userId)) {
            flag = true;
        } else if (password == null || "".equals(password)) {
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }

    /**
     * 判断用户名是否为空
     *
     * @param userId
     * @return
     */
    @Override
    public boolean isUsernameEmpty(String userId) {
        boolean flag;
        if (userId == null || "".equals(userId)) {
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }
}
