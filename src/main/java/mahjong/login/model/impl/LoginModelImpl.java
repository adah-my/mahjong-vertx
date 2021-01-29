package mahjong.login.model.impl;

import mahjong.login.cache.LoginCache;
import mahjong.login.cache.bean.LoginCacheBean;
import mahjong.login.model.LoginModel;

;

/**
 * @author muyi
 * @description:
 * @date 2020-11-02 16:11:23
 */
public class LoginModelImpl implements LoginModel {

    LoginCache loginCache;

    private volatile static LoginModel instance;

    private LoginModelImpl() {
        loginCache = LoginCache.getInstance();
    }

    /**
     *
     * @return LoginModel
     */
    public static LoginModel getInstance() {
        if (instance == null) {
            synchronized (LoginModelImpl.class) {
                if (instance == null) {
                    instance = new LoginModelImpl();
                }
            }
        }
        return instance;
    }


    /**
     * 用户下线
     *
     * @param userId
     */
    @Override
    public void logout(String userId) {
        LoginCacheBean loginCacheBean = loginCache.getOnlineUserById(userId);
        if (loginCacheBean == null) {
            System.out.println("用户：" + userId + " 已经下线");
        } else {
            loginCache.delOnlineUserById(userId);
            System.out.println("用户：" + userId + " 已经下线");
        }
    }

    /**
     * 判断是否注册用户
     *
     * @param userId
     * @param password
     * @return
     */
    @Override
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
     * 判断用户是否在线
     *
     * @param userId
     * @return
     */
    @Override
    public boolean isUserOnline(String userId) {
        return loginCache.getOnlineUserById(userId) != null;
    }
}
