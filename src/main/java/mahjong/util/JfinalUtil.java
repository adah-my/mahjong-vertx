package mahjong.util;

import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.druid.DruidPlugin;

/**
 * @author muyi
 * @description:
 * @date 2020-11-04 14:10:36
 */
public class JfinalUtil {

    private static String url = "jdbc:mysql://localhost:3306/telnet_db?serverTimezone=UTC&useSSL=false";
    private static String username = "root";
    private static String password = "root";

    private volatile static JfinalUtil instance;

    private JfinalUtil() {}

    public static JfinalUtil getInstance() {
        if (instance == null) {
            synchronized (JfinalUtil.class) {
                if (instance == null) {
                    instance = new JfinalUtil();
                }
            }
        }
        return instance;
    }

    public void loadPlugin(){
        DruidPlugin dp = new DruidPlugin(url, username, password);
        ActiveRecordPlugin arp = new ActiveRecordPlugin(dp);

        // 重点：手动调用start方法
        dp.start();
        arp.start();
    }



//    public static void main(String[] args) {
//
//         instance = JfinalUtil.getInstance();
//
//
//        // 创建name属性为James,age属性为25的record对象并添加到数据库
////        Record fingerRecord = new Record().set("record_id", "muyi4");
//        Record fingerRecord1 = new Record().remove("record_id","muyi4");
//        //Db.save("finger_record", fingerRecord);
//
//        // 删除id值为25的user表中的记录
//        //Db.deleteById("finger_record", "muyi4");
//
//        // 查询id值为25的Record将其name属性改为James并更新到数据库
//        //Record record = Db.findById("finger_record", "muyi3").set("user_id", "James").set("level_id","2");
//        Record record = Db.findById("finger_record", "muyi3");
//        //Db.update("finger_record", record);
//
//        // 获取user的name属性
//        String userId = record.getStr("user_id");
//        System.out.println(userId);
//        // 获取user的age属性
//        String levelId = record.getStr("level_id");
//        System.out.println(levelId);
//
//        // 查询所有年龄大于18岁的user
//        //List<Record> users = Db.find("select * from user where age > 18");
//
//        // 分页查询年龄大于18的user,当前页号为1,每页10个user
//        //Page<Record> userPage = Db.paginate(1, 10, "select *", "from user where age > ?", 18);
//    }




}
