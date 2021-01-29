package mahjong.game.cache.bean;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 20:38:09
 */
public enum FuluTypeCacheBean {
    /**
     * 副露牌的类型
     */
    CHI("吃"), PENG("碰"), GANG("杠");

    private String name;

    FuluTypeCacheBean(String name){
        this.name = name;
    }

}
