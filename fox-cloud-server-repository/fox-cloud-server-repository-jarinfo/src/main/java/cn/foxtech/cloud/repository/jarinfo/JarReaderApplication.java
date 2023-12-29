package cn.foxtech.cloud.repository.jarinfo;

import cn.foxtech.cloud.repository.jarinfo.utils.FoxEdgeUtils;

public class JarReaderApplication {
//    public static void main(String[] args) {
//        try {
//            String dependencyPath = "D:\\我的项目\\fox-cloud-server-internal-version\\repository\\dependency";
//
//            FoxEdgeUtils.scanJar("D:\\我的项目\\fox-cloud-server-internal-version\\repository\\dependency\\cn.fox-tech\\fox-edge-server-protocol-zxdu58\\1.0.3\\fox-edge-server-protocol-zxdu58-1.0.3.jar", dependencyPath);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {
        String jarFilePath = args[0];
        String dependencyPath = args[1];

        FoxEdgeUtils.scanJar(jarFilePath, dependencyPath);
    }
}
