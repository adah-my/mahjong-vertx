package mahjong.login.model;

import mahjong.login.model.impl.LoginModelImpl;

/**
 * @author muyi
 * @description:
 * @date 2020-11-02 16:32:48
 */
public interface LoginModel {

    static LoginModel getInstance() {
        return LoginModelImpl.getInstance();
    }
    /**
     * 用户下线
     *
     * @param userId
     */
    void logout(String userId);

    /**
     * 判断是否注册用户
     *
     * @param userId
     * @param password
     * @return
     */
    boolean isRegisterUser(String userId, String password);

    /**
     * 判断用户是否在线
     *
     * @param userId
     * @return
     */
    boolean isUserOnline(String userId);
}
