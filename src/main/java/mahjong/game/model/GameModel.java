package mahjong.game.model;

import mahjong.game.model.impl.GameModelImpl;

import java.util.HashMap;
import java.util.List;

/**
 * @author muyi
 * @description:
 * @date 2020-11-10 18:52:53
 */
public interface GameModel {
    static GameModel getInstance() {
        return GameModelImpl.getInstance();
    }

    void startGame(String owner, String mahjongRule, List<String> plays, HashMap<String, Boolean> isPlaysRobot, HashMap<String, Integer> robotLevel);
    void printMahjongHandTiles(String userId);

    void playerBackToTable(String userId);
}
