package mahjong.login.util;


import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author muyi
 * @description: 序列化工具
 * @date 2020-10-23 10:56:41
 */
public class SerializationUtil {

    /**
     * 路径
     */
    private static String filePath = "src/main/resources/users.txt";

    /**
     * 将文件中的用户反序列化到map中
     *
     * @return
     */
    public static ConcurrentHashMap<String, String> reader() {

        try {
            FileInputStream fileInputStream=new FileInputStream(new File(filePath));
            ObjectInputStream objectInputStream=new ObjectInputStream(fileInputStream);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String,String> map=(ConcurrentHashMap<String,String>)objectInputStream.readObject();
            return map;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 将注册的用户序列化到文件中
     *
     * @param map
     */
    public static  void write(ConcurrentHashMap<String, String> map) {

        FileOutputStream fileOutputStream = null;
        try {
            // 创建对象输出流
            fileOutputStream = new FileOutputStream(new File(filePath));
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(map);
            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (IOException e) {

            e.printStackTrace();

        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("aaa","aaa");
        map.put("bbb","bbb");
        map.put("ccc","ccc");

        write(map);

        ConcurrentHashMap<String, String> reader = reader();

        System.out.println(reader);
    }


}
