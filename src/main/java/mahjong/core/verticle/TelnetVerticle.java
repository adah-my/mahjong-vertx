package mahjong.core.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import mahjong.core.common.CmdsResponseBody;
import mahjong.core.common.Constant;
import mahjong.core.config.Config;
import mahjong.core.filter.CommandFilter;
import mahjong.core.model.UserModel;
import mahjong.login.model.LoginModel;
import mahjong.table.model.MahjongModel;
import mahjong.util.CommandReflactUtil;
import mahjong.util.GuideUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author muyi
 * @description:
 * @date 2020-10-26 16:50:20
 */
public class TelnetVerticle extends AbstractVerticle {

    UserModel userModel;
    LoginModel loginModel;
    MahjongModel mahjongModel;
    /**
     * 日志
     */
    public static final Logger log = Logger.getLogger(TelnetVerticle.class.getName());


    @Override
    public void start() {
        NetServerOptions options = new NetServerOptions().setPort(8080).setTcpKeepAlive(true);
        NetServer server = vertx.createNetServer(options);

        server.connectHandler(socket -> {
            socket.write("连接telnet麻将游戏服务器成功》》》" + Constant.ENTER, Constant.ENCODING);
            sayHello(socket);

            // 记录用户的命令
            String userInput = "";
            ArrayList<String> userInputRecord = new ArrayList<String>(1);
            userInputRecord.add(userInput);

            socket.handler(buffer -> {

                if (Constant.ENTER.equals(buffer.toString())) {
                    // 接收到回车，开始执行命令

                    // 获取命令和参数
                    String[] commands = userInputRecord.get(0).split(" ");

                    // 根据handlerId获取userId
                    String userId = userModel.getUserIdByHandlerId(socket.writeHandlerID());
                    // 初始化命令
                    log.info("用户：" + userId + " 命令：" + userInputRecord.get(0));
                    userInputRecord.set(0, "");

                    // 过滤命令
                    if (CommandFilter.doFilter(commands[0])) {
                        List<String> msg = new ArrayList<String>();
                        msg.add("不存在该命令");
                        GuideUtil.setUserGuide(msg, userId);
                        socketWrite(socket, msg);
                    } else {

                        // 预先存储用户socket
                        saveUserSocket(socket, commands, userId);
                        // 调用worker线程执行对应命令并返回数据到客户端
                        handlerBlockingTask(socket, commands, userId);
                    }

                    // 用户直接断开连接
                    socket.closeHandler(handler -> {
                        String username = userModel.getUserIdByHandlerId(socket.writeHandlerID());
                        MahjongModel.getInstance().userBack(userId);
                        loginModel.logout(username);
                        userModel.removeUserId(username);
                    });

                } else if (Constant.Backspace.equals(buffer.toString())) {
                    // 退格
                    if (!"".equals(userInputRecord.get(0))) {
                        // 删除最后一个字符
                        String substring = userInputRecord.get(0).substring(0, userInputRecord.get(0).length() - 1);
                        userInputRecord.add(0, substring);
                    }

                } else {
                    // 添加用户最新输入的字符到记录
                    String temp = userInputRecord.get(0) + buffer.getString(0, buffer.length(), Constant.ENCODING);
                    userInputRecord.add(0, temp);

                }

            });

        }).listen(res -> {
            if (res.succeeded()) {
                System.out.println("telnet服务器开始监听...");
                // 加载配置文件,与开启周期任务
                Config config = Config.getInstance();
                config.loadConfig();
                config.startTimerTack();
                // 反序列化用户
                loginModel = LoginModel.getInstance();
                // 加载配置文件中所有类与方法进入内存
                CommandReflactUtil.getCmdsObjectsMethods();
                userModel = UserModel.getInstance();
                mahjongModel = MahjongModel.getInstance();

            } else {
                res.cause().printStackTrace();
                System.out.println("服务器关闭成功.");
            }
        });

    }

    /**
     * 调用worker线程执行对应命令并返回数据到客户端
     * @param socket
     * @param commands
     * @param userId
     */
    public void handlerBlockingTask(NetSocket socket, String[] commands, String userId) {
        vertx.executeBlocking(future -> {
            Object bodys = CommandReflactUtil.invoke(userId, commands);
            future.complete(bodys);
        }, res -> {
            if (res.succeeded()) {
                handlerResponseBody(socket, res.result());
            } else {
                res.cause().printStackTrace();
            }
        });
    }

    /**
     * 用户执行登录命令时预先存储
     * @param socket
     * @param commands
     */
    private void saveUserSocket(NetSocket socket, String[] commands, String userId) {
        if ("login".equals(commands[0]) && commands.length == 3){
            if (loginModel.isRegisterUser(commands[1],commands[2])){
                mahjongModel.userBack(commands[1]);
                userModel.putUserId(commands[1], socket);
            }
        }
    }

    /**
     * 处理逻辑层返回体,输出信息到客户端
     *
     * @param bodysObj
     */
    private void handlerResponseBody(NetSocket currentSocket, Object bodysObj) {
        @SuppressWarnings("unchecked")
        List<CmdsResponseBody> bodys = (List<CmdsResponseBody>)bodysObj;
        if (bodys == null){
            System.out.println("返回体为空！");
        }else{
            // 输出信息到对应目标用户
            for (CmdsResponseBody body : bodys){
                List<String> userIds = body.getUserIds();
                if (userIds.size() != 0){
                    if ("".equals(userIds.get(0))){
                        // 如果userIds为空，则输出到当前用户
                        socketWrite(currentSocket, body.getMessages());
                    }else {
                        // 不为空，则输出到对应用户
                        for (String userId : userIds){
                            NetSocket userSocket = userModel.getSocketByUserId(userId);
                            socketWrite(userSocket, body.getMessages());
                        }
                    }
                }

            }
        }
    }



    /**
     * 输出messages中的内容到socket
     *
     * @param socket
     * @param messages
     */
    private void socketWrite(NetSocket socket, List<String> messages) {
        if (messages == null) {
            System.out.println("messages为空！");
        } else if(socket == null){
            
        }else {
            for (int i = 0; i < messages.size(); i++) {
                if (i == 0) {
                    socket.write("== " + messages.get(i) + " ==" + Constant.ENTER, Constant.ENCODING);
                } else {
                    socket.write(messages.get(i) + Constant.ENTER, Constant.ENCODING);
                }
            }
        }

    }

    /**
     * 连接服务器的初始引导
     *
     * @param socket
     */
    public void sayHello(NetSocket socket) {
        socket
                .write("您继续以下操作：" + Constant.ENTER, Constant.ENCODING)
                .write("注册(没有账号注册一个)：register muyi 123456 " + Constant.ENTER, Constant.ENCODING)
                .write("登录(登录你的账号)：login muyi 123456 " + Constant.ENTER, Constant.ENCODING);
    }








}




