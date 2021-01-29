package mahjong.core.model.impl;

import io.vertx.core.net.NetSocket;
import mahjong.core.common.CmdsResponseBody;
import mahjong.core.common.Constant;
import mahjong.core.model.UserModel;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author muyi
 * @description:
 * @date 2020-11-02 16:53:15
 */
public class UserModelImpl implements UserModel {

    private volatile static UserModel instance;

    private ConcurrentHashMap<String, String> userHandlerMap;
    private ConcurrentHashMap<String, NetSocket> userSocketMap;

    private UserModelImpl() {
        userHandlerMap = new ConcurrentHashMap<>();
        userSocketMap = new ConcurrentHashMap<>();
    }

    /**
     * @return UserModel
     */
    public static UserModel getInstance() {
        if (instance == null) {
            synchronized (UserModelImpl.class) {
                if (instance == null) {
                    instance = new UserModelImpl();
                }
            }
        }
        return instance;
    }

    /**
     * 测试
     *
     * @return
     */
    @Override
    public String toString() {
        return "UserModel{" +
                "userHandlerMap=" + userHandlerMap +
                ", userSocketMap=" + userSocketMap +
                '}';
    }

    /**
     * 删除userId
     *
     * @param userId
     */
    @Override
    public void removeUserId(String userId) {
        userHandlerMap.remove(userId);
        userSocketMap.remove(userId);
    }

    /**
     * 添加UserId
     *
     * @param userId
     * @param socket
     */
    @Override
    public void putUserId(String userId, NetSocket socket) {
        userHandlerMap.put(userId, socket.writeHandlerID());
        userSocketMap.put(userId, socket);
    }

    /**
     * 根据handlerId获取userId
     *
     * @param handlerId
     * @return
     */
    @Override
    public String getUserIdByHandlerId(String handlerId) {
        String userId = "";
        Iterator<Map.Entry<String, String>> iterator = userHandlerMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (handlerId.equals(entry.getValue())) {
                userId = entry.getKey();
                break;
            }
        }
        return userId;
    }

    /**
     * 通过userId获取对应的socket
     *
     * @param userId
     * @return
     */
    @Override
    public NetSocket getSocketByUserId(String userId) {
        return userSocketMap.get(userId);
    }

    /**
     * 输出信息到客户端
     *
     * @param userId
     * @param message
     */
    @Override
    public void writeToUser(String userId, String message) {
        NetSocket socket = userSocketMap.get(userId);
        if (socket != null) {
            userSocketMap.get(userId).write(message + Constant.ENTER, Constant.ENCODING);
        }

    }

    /**
     * 输出到对应客户端
     * @param drawBody
     */
    @Override
    public void writeToUser(List<CmdsResponseBody> drawBody) {

        if (drawBody == null) {
            System.out.println("返回体为空！");
        } else {
            // 输出信息到对应目标用户
            for (CmdsResponseBody body : drawBody) {
                List<String> userIds = body.getUserIds();
                if (userIds == null || userIds.size() == 0 || "".equals(userIds.get(0))) {
                    // 如果userIds为空，则不输出

                } else {
                    // 不为空，则输出到对应用户
                    for (String userId : userIds) {
                        NetSocket userSocket = userSocketMap.get(userId);
                        socketWrite(userSocket, body.getMessages());
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


}
