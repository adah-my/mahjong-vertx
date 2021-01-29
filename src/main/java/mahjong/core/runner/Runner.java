package mahjong.core.runner;

import io.vertx.core.Vertx;
import mahjong.core.verticle.TelnetVerticle;

/**
 * @author muyi
 * @description:
 * @date 2020-10-26 16:51:08
 */
public class Runner {

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(TelnetVerticle.class.getName());

    }
}
                                                                                