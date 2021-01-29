package mahjong.login.cache;

import mahjong.login.cache.bean.LoginCacheBean;
import mahjong.login.cache.impl.LoginCacheImpl;

/**
 * @author muyi
 * @description:
 * @date 2020-11-02 16:29:08
 */
public interface LoginCache {

    static LoginCache getInstance() {
        return LoginCacheImpl.getInstance();
    }
    /**
     *  根据Id查询注册用户
     * @param userId
     * @return
     */
    LoginCacheBean getRegisterUserById(String userId);

    /**
     * 根据Id获取查询用户
     *
     * @param userId
     * @return
     */
    LoginCacheBean getOnlineUserById(String userId);

    /**
     * 判断用户名是否存在
     *
     * @param userId
     * @return
     */
    boolean isUsernameExist(String userId);

    /**
     * 判断用户在列表之中
     *
     * @param userId
     * @return
     */
    boolean isUserOnline(String userId);

    /**
     * 通过Id删除在线用户
     *
     * @param userId
     * @return
     */
    int delOnlineUserById(String userId);

    /**
     * 添加注册用户
     *
     * @param userId
     * @param password
     * @return
     */
    int addRegisterUser(String userId, String password);

    /**
     * 添加在线用户
     *
     * @param userId
     * @param password
     * @return
     */
    int addOnlineUser(String userId, String password);

    /**
     * 判断是否为空
     *
     * @param userId
     * @param password
     * @return
     */
    boolean isUserEmpty(String userId, String password);

    /**
     * 判断用户名是否为空
     *
     * @param userId
     * @return
     */
    boolean isUsernameEmpty(String userId);
}
