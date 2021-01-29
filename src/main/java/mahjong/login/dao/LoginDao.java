package mahjong.login.dao;

import mahjong.login.dao.impl.LoginDaoImpl;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 11:44:25
 */
public interface LoginDao {

    static LoginDao getInstance() {
        return LoginDaoImpl.getInstance();
    }



}
