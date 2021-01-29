package mahjong.core.config;

import mahjong.util.JfinalUtil;

/**
 * @author muyi
 * @description: 加载所有配置文件
 * @date 2020-11-05 11:04:03
 */
public class Config {

    private volatile static Config instance;
    private final JfinalUtil jfinalUtil;

    private Config(){
        jfinalUtil = JfinalUtil.getInstance();
    }
    /**
     * @return Config
     */
    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }

    public void loadConfig(){
        jfinalUtil.loadPlugin();
    }

    /**
     * 定时器
     */
    public void startTimerTack(){
//        new ViewCacheUtil().executeFiveMinutes();
    }

}
