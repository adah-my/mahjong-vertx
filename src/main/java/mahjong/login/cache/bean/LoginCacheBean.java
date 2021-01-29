package mahjong.login.cache.bean;

/**
 * @author muyi
 * @description: 用户实体
 * @date 2020-10-28 11:54:37
 */
public class LoginCacheBean {

    /**
     * 用户名
     */
    private String username;
    /**
     * 用户密码
     */
    private String password;

    public LoginCacheBean(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
