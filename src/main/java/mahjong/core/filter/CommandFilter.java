package mahjong.core.filter;

import mahjong.util.CommandReflactUtil;

/**
 * @author muyi
 * @description: 过滤命令，判断命令是否合法
 * @date 2020-10-26 17:11:35
 */
public class CommandFilter {


    /**
     * 过滤命令
     * @param commands
     */
    public static boolean doFilter(String commands){

        if (CommandReflactUtil.isRealCommand(commands)){
            return false;
        }else{
            return true;
        }

    }

}
