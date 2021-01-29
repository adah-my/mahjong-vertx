package mahjong.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * @author muyi
 * @description: 命令反射工具类
 * @date 2020-10-26 16:58:16
 */
public class CommandReflactUtil {

    /**
     * 路径
     */
    private static String proPath = "command.properties";
    private static Properties properties;
    private static HashMap<String,Object> cmdsobjs;
    private static HashMap<String,Method> commandMethods;


    // 获取Properties流对象
    static {
        try {
            //创建properties对象
            properties = new Properties();

            //通过本类的类加载器将properties文件中的内容加载到properties对象中
            ClassLoader classLoader = CommandReflactUtil.class.getClassLoader();
            properties.load(classLoader.getResourceAsStream(proPath));

            cmdsobjs = new HashMap<String, Object>();
            commandMethods = new HashMap<String, Method>();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 判断配置文件中是否存在该命令
     *
     * @param command
     * @return
     */
    public static boolean isRealCommand(String command) {

        String packageName = properties.getProperty(command);
        return packageName != null;

    }


    /**
     * 通过反射执行对应命令的方法
     *
     * @param commands
     */
    public static Object invoke(String userId,String[] commands) {

        try {

            String packageName = properties.getProperty(commands[0] + "-package");

            Object cmdsObj = cmdsobjs.get(packageName);

            Method method = commandMethods.get(commands[0]);

            return method.invoke(cmdsObj,userId,commands);

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } finally {
        }
        return null;

    }

    /**
     * 加载配置文件中所有命令类与方法进入内存
     */
    public static void getCmdsObjectsMethods() {
        Set<String> cmdsClasses = new HashSet<>();
        Set<String> methodNames = new HashSet<>();

        for (String key: properties.stringPropertyNames()){
            if (key.contains("package")){
                cmdsClasses.add(properties.getProperty(key));
            }else{
                methodNames.add(properties.getProperty(key));
            }
        }

        // 获取所有命令的obj
        for (String cmdsClass: cmdsClasses){
            try {
                Class<?> clazz = Class.forName(cmdsClass);
                Object cmdsObj = clazz.getDeclaredConstructor().newInstance();
                cmdsobjs.put(cmdsClass, cmdsObj);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        // 获取所有命令的Method
        for (String methodName: methodNames){
            try {
                String classPackage = properties.getProperty(methodName + "-package");
                Class<?> clazz = Class.forName(classPackage);
                Method method = clazz.getDeclaredMethod(methodName, String.class, String[].class);
                commandMethods.put(methodName, method);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        System.out.println(cmdsobjs.toString());
        System.out.println(commandMethods.toString());

    }

}
