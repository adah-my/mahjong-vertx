package mahjong.game.cache.bean;

import mahjong.game.util.MahjongUtil;


/**
 * @author muyi
 * @description:
 * @date 2020-11-10 20:44:11
 */
public class FuluCacheBean {

    /**
     * 每一组副露必为吃碰杠三种之一
     */
    public FuluTypeCacheBean type;

    /**
     * 每一组副露必然由3或4张牌组成，记录这些牌的编号
     */
    public int[] tileNumbers = null;

    /**
     * 对一组吃出来的副露，其中一张牌是特殊的
     * 用该角标记录哪张是吃的
     * 比如三四五条，chiIndex=0，则说明三条是吃来的
     */
    public int chiIndex = -1;

    /**
     * 对于碰出来的副露，用该值记录碰牌的来源
     * 比如pengSource=0，则说明是東风玩家打出来的碰牌
     */
    public int pengSource = -1;

    /**
     * 对于杠出来的副露，还要具体区分明暗杠
     * 明杠：不论大明杠还是加杠，如碰一样应该有来源
     * 用该值记录来源的玩家编号
     * 比如gangSource=0，则说明東风玩家打出的牌被杠
     * 如果该编号等于自身编号，则说明这是一个暗杠
     */
    public int gangSource = -1;
    /**
     * 副露牌的来源
     */
    public String titleSource;

    /**
     * 初始化fulu
     *
     * @param type
     * @param tileNumbers
     * @param index
     */
    public FuluCacheBean(FuluTypeCacheBean type, int[] tileNumbers, int index, String titleSource) {
        this.type = type;
        this.tileNumbers = tileNumbers;
        this.titleSource = titleSource;
        if (type == FuluTypeCacheBean.CHI) {
            this.chiIndex = index;
        } else if (type == FuluTypeCacheBean.PENG) {
            this.pengSource = index;
        } else if (type == FuluTypeCacheBean.GANG) {
            this.gangSource = index;
        } else {
            System.out.println("fulu初始化错误");
        }
    }

    @Override
    public String toString() {
        return "FuluCacheBean{" +
                "type=" + type.name() +
                ", tileNumbers=" + MahjongUtil.printTiles(tileNumbers) +
                '}';
    }
}
