package mahjong.login.dao.impl;

import mahjong.login.dao.LoginDao;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 11:44:39
 */
public class LoginDaoImpl implements LoginDao {

    private volatile static LoginDao instance;

    private LoginDaoImpl() {
    }

    /**
     *
     * @return LoginCache
     */
    public static LoginDao getInstance() {
        if (instance == null) {
            synchronized (LoginDaoImpl.class) {
                if (instance == null) {
                    instance = new LoginDaoImpl();
                }
            }
        }
        return instance;
    }
}
