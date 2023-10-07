package cn.foxtech.cloud.common.utils.jar.reader.utils;

import cn.foxtech.common.utils.Maps;
import cn.foxtech.common.utils.jar.info.JarInfoReader;
import cn.foxtech.common.utils.json.JsonUtils;
import cn.foxtech.common.utils.osinfo.OSInfo;
import cn.foxtech.common.utils.xml.XmlReader;
import cn.foxtech.device.protocol.RootLocation;
import cn.foxtech.device.protocol.v1.core.annotation.FoxEdgeDeviceType;

import java.util.*;

public class FoxEdgeUtils {
    public static void scanJar(String jarFilePath, String dependencyPath) {
        loadDependencies(jarFilePath, dependencyPath);

        printlnJarInfo1(jarFilePath);
        printlnJarInfo2(jarFilePath);
        printlnJarInfo3(jarFilePath);
    }

    public static void printlnJarInfo1(String jarFilePath) {
        try {
            Properties properties = JarInfoReader.readPomProperties(jarFilePath);
            System.out.println("pom.properties.version:" + properties.getProperty("version", ""));
            System.out.println("pom.properties.groupId:" + properties.getProperty("groupId", ""));
            System.out.println("pom.properties.artifactId:" + properties.getProperty("artifactId", ""));
        } catch (Exception e) {
            System.out.println("pom.properties.version:" + "");
            System.out.println("pom.properties.groupId:" + "");
            System.out.println("pom.properties.artifactId:" + "");

        }
    }

    public static void printlnJarInfo2(String jarFilePath) {
        try {
            List dependencies = readDependencies(jarFilePath);
            String json = JsonUtils.buildJson(dependencies);
            System.out.println("pom.xml.dependencies:" + json);
        } catch (Exception e) {
            System.out.println("pom.xml.dependencies:" + "[]");

        }
    }

    public static List readDependencies(String jarFilePath) {
        try {
            String xml = JarInfoReader.readPomXml(jarFilePath);
            Map<String, Object> jarXml = XmlReader.parse(xml);
            return (List) Maps.getOrDefault(jarXml, "project", "dependencies", "dependency", new ArrayList<>());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void printlnJarInfo3(String jarFilePath) {
        try {
            Set<String> classNames = JarInfoReader.readClassName(jarFilePath);

            // 检查：类的名称是否合法
            if (!checkValidity(classNames)) {
                println("", "", null);
                return;
            }

            // 获得版本名称
            String jarVer = getJarVer(classNames);
            if (jarVer.isEmpty()) {
                println("", jarVer, null);
                return;
            }

            // 获得空间名称
            String jarSpace = getJarSpace(classNames);
            if (jarSpace.isEmpty()) {
                println(jarSpace, jarVer, null);
                return;
            }

            // 下面是解码器的组件：装载jar包到进程空间，准备获得注解信息
            JarLoaderUtils.loadJar(jarFilePath);

            // 获得注解信息
            FoxEdgeDeviceType annotation = getDeviceTypeAnnotation();

            // 打印信息
            println(jarSpace, jarVer, annotation);

        } catch (Exception e) {
            println("", "", null);
        }
    }

    public static void println(String jarSpace, String jarVer, FoxEdgeDeviceType annotation) {
        System.out.println("jarSpace:" + jarSpace);
        System.out.println("jarVer:" + jarVer);
        if (annotation != null) {
            System.out.println("manufacturer:" + annotation.manufacturer());
            System.out.println("deviceType:" + annotation.value());
            System.out.println("description:" + annotation.description());
            return;
        } else {
            System.out.println("manufacturer:" + "");
            System.out.println("deviceType:" + "");
            System.out.println("description:" + "");
        }
    }

    /**
     * 检查：是否为cn.foxtech.device.protocol.v1.xx.*格式的类名称
     *
     * @param classNames
     * @return
     */
    public static boolean checkValidity(Set<String> classNames) {
        String pack = RootLocation.class.getPackage().getName();
        String rootLocationName = RootLocation.class.getName();

        for (String className : classNames) {
            if (!className.startsWith(pack + ".")) {
                return false;
            }

            // 检查：是否为RootLocation类
            if (className.equals(rootLocationName)) {
                continue;
            }


            String subName = className.substring(pack.length() + 1);
            String[] items = subName.split("\\.");
            if (items.length < 3) {
                return false;
            }

            // 是否以v打头
            if (!items[0].startsWith("v")) {
                return false;
            }
        }

        return true;
    }

    private static String tryGetJarVer(Set<String> classNames) {
        String pack = RootLocation.class.getPackage().getName();
        String rootLocationName = RootLocation.class.getName();

        for (String className : classNames) {
            // 检查：是否为RootLocation类
            if (className.equals(rootLocationName)) {
                continue;
            }


            String subName = className.substring(pack.length() + 1);
            String[] items = subName.split("\\.");
            return items[0];
        }

        return "";
    }

    private static String getJarVer(Set<String> classNames) {
        String pack = RootLocation.class.getPackage().getName();
        String rootLocationName = RootLocation.class.getName();

        String first = tryGetJarVer(classNames);
        if (first.isEmpty()) {
            return first;
        }

        for (String className : classNames) {
            // 检查：是否为RootLocation类
            if (className.equals(rootLocationName)) {
                continue;
            }


            String subName = className.substring(pack.length() + 1);
            String[] items = subName.split("\\.");
            if (!items[0].equals(first)) {
                return "";
            }
        }

        return first;
    }

    /**
     * 获得相同的包名称
     *
     * @param classNames
     * @return
     */
    private static String getJarSpace(Set<String> classNames) {
        String pack = RootLocation.class.getPackage().getName();
        String rootLocationName = RootLocation.class.getName();

        String jarVer = tryGetJarVer(classNames);

        int minLength = Integer.MAX_VALUE;
        List<String[]> list = new ArrayList<>();
        for (String className : classNames) {
            // 检查：是否为RootLocation类
            if (className.equals(rootLocationName)) {
                continue;
            }


            String subName = className.substring(pack.length() + jarVer.length() + 2);
            String[] items = subName.split("\\.");
            if (items.length < 2) {
                continue;
            }

            if (minLength > items.length - 1) {
                minLength = items.length - 1;
            }

            list.add(items);
        }

        if (list.isEmpty()) {
            return "";
        }

        String nameSpace = "";
        for (int i = 0; i < minLength; i++) {

            // 檢查：是否相同
            boolean same = true;
            String name = list.get(0)[i];
            for (String[] items : list) {
                if (!name.equals(items[i])) {
                    same = false;
                    break;
                }
            }

            if (same) {
                nameSpace += "." + name;
                continue;
            }

            break;
        }

        if (nameSpace.isEmpty()) {
            return "";
        }

        nameSpace = nameSpace.substring(1);
        return nameSpace;
    }

    public static FoxEdgeDeviceType getDeviceTypeAnnotation() {
        try {
            String pack = RootLocation.class.getPackage().getName();

            Set<Class<?>> classSet = JarLoaderUtils.getClasses(pack);
            for (Class<?> aClass : classSet) {
                // 是否为解码器类型
                if (!aClass.isAnnotationPresent(FoxEdgeDeviceType.class)) {
                    continue;
                }

                // 设备级别的处理：
                FoxEdgeDeviceType typeAnnotation = aClass.getAnnotation(FoxEdgeDeviceType.class);
                return typeAnnotation;
            }
        } catch (Throwable e) {
            e.getMessage();

        }

        return null;
    }

    public static void loadDependencies(String jarFilePath, String dependencyPath) {
        List<Map<String, Object>> dependencies = (List<Map<String, Object>>) FoxEdgeUtils.readDependencies(jarFilePath);
        for (Map<String, Object> dependency : dependencies) {
            String groupId = (String) dependency.get("groupId");
            String artifactId = (String) dependency.get("artifactId");
            String version = (String) dependency.get("version");


            String jarPath = dependencyPath + "\\" + groupId + "\\" + artifactId + "\\" + version + "\\" + artifactId + "-" + version + ".jar";
            if (OSInfo.isWindows()){
                jarPath = jarPath.replace("/","\\");
            }
            if (OSInfo.isLinux()){
                jarPath = jarPath.replace("\\","/");
            }

            JarLoaderUtils.loadJar(jarPath);
        }
    }


}
