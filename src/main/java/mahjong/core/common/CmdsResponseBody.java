package mahjong.core.common;

import java.util.ArrayList;
import java.util.List;

/**
 * @author muyi
 * @description: 命令返回的实体类
 * @date 2020-10-28 14:59:49
 */
public class CmdsResponseBody {

    /**
     * 消息要发送的用户列表
     */
    private List<String> userIds;

    /**
     * 需要发送的消息内容列表
     */
    private List<String> messages;

    public CmdsResponseBody(){
        userIds = new ArrayList<String>();
        messages = new ArrayList<String>();
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "CmdsResponseBody{" +
                "userIds=" + userIds +
                ", messages=" + messages +
                '}';
    }
}
