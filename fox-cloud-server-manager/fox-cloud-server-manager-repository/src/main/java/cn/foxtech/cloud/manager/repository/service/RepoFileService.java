package cn.foxtech.cloud.manager.repository.service;

import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.manager.repository.entity.RepoCompEntity;
import cn.foxtech.cloud.manager.repository.entity.RepoCompVerEntity;
import cn.foxtech.cloud.manager.repository.entity.RepoJarEntity;
import cn.foxtech.cloud.manager.repository.entity.RepoJarInfo;
import cn.foxtech.common.utils.file.FileNameUtils;
import cn.foxtech.common.utils.json.JsonUtils;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.osinfo.OSInfo;
import cn.foxtech.common.utils.shell.ShellUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
public class RepoFileService {

    public String getDependencyDirs(RepoCompEntity compEntity) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\repository\\dependency\\");
        sb.append(compEntity.getJarEntity().getProperty().getGroupId());
        sb.append("\\");
        sb.append(compEntity.getJarEntity().getProperty().getArtifactId());
        sb.append("\\");
        sb.append(compEntity.getJarEntity().getProperty().getVersion());
        return sb.toString();
    }

    public String getJarFileName(RepoCompEntity compEntity) {
        StringBuilder sb = new StringBuilder();
        sb.append(compEntity.getJarEntity().getProperty().getArtifactId());
        sb.append("-");
        sb.append(compEntity.getJarEntity().getProperty().getVersion());
        sb.append(".jar");
        return sb.toString();
    }

    public String getDependencyPathName(RepoCompEntity compEntity) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\repository\\dependency\\");
        sb.append(compEntity.getJarEntity().getProperty().getGroupId());
        sb.append("\\");
        sb.append(compEntity.getJarEntity().getProperty().getArtifactId());
        sb.append("\\");
        sb.append(compEntity.getJarEntity().getProperty().getVersion());
        sb.append("\\");
        sb.append(compEntity.getJarEntity().getProperty().getArtifactId());
        sb.append("-");
        sb.append(compEntity.getJarEntity().getProperty().getVersion());
        sb.append(".jar");
        return sb.toString();
    }

    public String getDecoderTempFilePathName(RepoCompEntity compEntity, String tempFileName) {
        return "\\repository\\decoder\\" + compEntity.getModelName() + "\\" + compEntity.getModelVersion() + "\\" + tempFileName;
    }

    public void copyDependency(RepoCompEntity compEntity, String tempFileName) throws IOException, InterruptedException {
        if (MethodUtils.hasEmpty(tempFileName)) {
            return;
        }
        if (!tempFileName.toLowerCase().endsWith(".jar")) {
            return;
        }

        File dir = new File("");
        String absolutePath = dir.getAbsolutePath();

        String srcFile = absolutePath + getDecoderTempFilePathName(compEntity, tempFileName);
        String dstFile = absolutePath + getDependencyPathName(compEntity);
        String dstDir = absolutePath + this.getDependencyDirs(compEntity);


        if (OSInfo.isWindows()) {
            srcFile = srcFile.replace("/", "\\");
            dstFile = dstFile.replace("/", "\\");
            dstDir = dstDir.replace("/", "\\");

            // 预创建目录
            File dirs = new File(dstDir);
            if (!dirs.exists()) {
                dirs.mkdirs();
            }

            ShellUtils.executeCmd("copy /y \"" + srcFile + "\" \"" + dstFile + "\"");
        }
        if (OSInfo.isLinux()) {
            srcFile = srcFile.replace("\\", "/");
            dstFile = dstFile.replace("\\", "/");
            dstDir = dstDir.replace("\\", "/");

            // 预创建目录
            File dirs = new File(dstDir);
            if (!dirs.exists()) {
                dirs.mkdirs();
            }

            ShellUtils.executeShell("cp -rf '" + srcFile + "' '" + dstFile + "'");
        }


    }

    private Map<String, Object> scanJarInfo(String jarFile) {
        File file = new File("");

        try {
            String cmd = "java -Dfile.encoding=UTF-8 -jar " + file.getAbsolutePath() + "\\bin\\fox-cloud-server-manager-jarinfo.jar " + jarFile + " " + file.getAbsolutePath() + "\\repository\\dependency";

            List<String> lines = new ArrayList<>();
            if (OSInfo.isWindows()) {
                cmd = cmd.replace("/", "\\");
                lines = ShellUtils.executeCmd(cmd);
            }
            if (OSInfo.isLinux()) {
                cmd = cmd.replace("\\", "/");
                lines = ShellUtils.executeShell(cmd);
            }

            Map<String, Object> data = new HashMap<>();
            for (String line : lines) {
                String[] items = line.split(":");
                if (items.length < 2) {
                    continue;
                }

                data.put(items[0], line.substring(items[0].length() + 1));
            }


            return data;

        } catch (Exception e) {
            throw new ServiceException("读取jar文件信息失败:" + e.getMessage());
        }
    }

    public void packJar2Tar(String modelType, String filePath, String tarFileName, String jarFileName) throws IOException, InterruptedException {
        if (OSInfo.isWindows()) {
            filePath = filePath.replace("/", "\\");

            // 打包成tar文件
            ShellUtils.executeCmd("tar -cvf " + filePath + "\\" + tarFileName + " -C " + filePath + " " + jarFileName);

            // 删除临时的jar文件
            ShellUtils.executeCmd("del /q " + filePath + "\\" + jarFileName);
            return;
        }
        if (OSInfo.isLinux()) {
            filePath = filePath.replace("\\", "/");

            // 打包成tar文件
            ShellUtils.executeShell("tar -cvf " + filePath + "/" + tarFileName + " -C " + filePath + " " + jarFileName);

            // 删除临时的jar文件
            ShellUtils.executeShell("rm -f " + filePath + "/" + jarFileName);
            return;
        }


        return;
    }

    public void moveFile(String modelType, String modelName, String modelVersion, RepoCompVerEntity verEntity) throws IOException, InterruptedException {
        File file = new File("");
        String path = file.getAbsolutePath();

        String modelDir = path + "/repository/" + modelType + "/" + modelName + "/" + modelVersion;
        String fileDir = modelDir + "/" + verEntity.getVersion();
        if (OSInfo.isWindows()) {
            // 删除可能存在的目录
            fileDir = fileDir.replace("/", "\\");
            ShellUtils.executeCmd("mkdir " + fileDir);

            modelDir = modelDir.replace("/", "\\");
            ShellUtils.executeCmd("move " + modelDir + "\\" + verEntity.getPathName() + " " + fileDir);
        }
        if (OSInfo.isLinux()) {
            ShellUtils.executeShell("mkdir -p '" + fileDir + "'");

            ShellUtils.executeShell("mv '" + modelDir + "/" + verEntity.getPathName() + "' '" + fileDir + "'");
        }
    }

    /**
     * 删除可能存在的垃圾文件
     *
     * @param compEntity
     * @throws IOException
     * @throws InterruptedException
     */
    public void removeFile(RepoCompEntity compEntity, String modelDir) throws IOException, InterruptedException {
        // 根据组件的版本信息，构造应该存在的文件名
        Set<String> fileNames = new HashSet<>();
        for (RepoCompVerEntity entity : compEntity.getVersions()) {
            String fullName = modelDir + "/" + entity.getVersion() + "/" + entity.getPathName();
            fullName = fullName.replace("/", "\\");
            fileNames.add(fullName);
        }

        // 获得本地文件名
        List<String> localList = FileNameUtils.findFileList(modelDir, true, true);

        // 检查：残存的本地垃圾，并删除
        for (String localName : localList) {
            localName = localName.replace("/", "\\");
            if (fileNames.contains(localName)) {
                continue;
            }

            if (OSInfo.isWindows()) {
                localName = localName.replace("/", "\\");
            }
            if (OSInfo.isLinux()) {
                localName = localName.replace("\\", "/");
            }

            // 检查：是否为目录
            File file = new File(localName);
            if (file.isDirectory()) {
                continue;
            }

            // 删除垃圾文件
            file.delete();
        }
    }

    /**
     * 清理残留物：上传的过程中，可以因为各种原因的失败，形成中间过程的残留物文件
     * 该操作是作为最后回收阶段的垃圾回收，所以不能再抛出异常了
     *
     * @param modelName
     * @param modelType
     * @param modelVersion
     */
    public void cleanResidue(String modelName, String modelType, String modelVersion) {
        try {
            if (MethodUtils.hasEmpty(modelName, modelType, modelVersion)) {
                throw new ServiceException("body参数缺失:modelName, modelType, modelVersion");
            }

            File dir = new File("");
            String modelDir = dir.getAbsolutePath() + "\\repository\\" + modelType + "\\" + modelName + "\\" + modelVersion;

            // 获得本地文件名
            List<String> localList = FileNameUtils.findFileList(modelDir, false, true);

            // 检查：残存的本地垃圾，并删除
            for (String localName : localList) {
                localName = localName.replace("/", "\\");

                // 检查：是否为目录
                File file = new File(localName);
                if (file.isDirectory()) {
                    continue;
                }

                // 删除垃圾文件
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收用户上传的文件,并按指定的文件名保存数据,文件扩展名为用户的原扩展名
     *
     * @param multipartFile
     * @param compEntity
     * @param filePath
     * @param fileName
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public String saveFile(RepoCompEntity compEntity, MultipartFile multipartFile, String filePath, String fileName) throws IOException, InterruptedException {
        if (multipartFile.isEmpty()) {
            throw new ServiceException("上传文件为空");
        }

        // 上传方的原始文件名
        String originalFilename = multipartFile.getOriginalFilename();
        // 扩展名
        String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (MethodUtils.hasEmpty(extName)) {
            throw new ServiceException("上传文件必须为jar/tar文件");
        }
        if (!".jar".equalsIgnoreCase(extName) && !".tar".equalsIgnoreCase(extName) && !".gz".equalsIgnoreCase(extName)) {
            throw new ServiceException("上传文件必须jar/tar/gz文件");
        }
        if (".jar".equalsIgnoreCase(extName) && !compEntity.getModelType().equals("decoder")) {
            throw new ServiceException(compEntity.getModelType() + "的上传文件必须jar文件");
        } else if (".tar".equalsIgnoreCase(extName) && !compEntity.getModelType().equals("service") && !compEntity.getModelType().equals("template") && !compEntity.getModelType().equals("webpack")) {
            throw new ServiceException(compEntity.getModelType() + "的上传文件必须tar文件");
        } else if (".gz".equalsIgnoreCase(extName) && !compEntity.getModelType().equals("system")) {
            throw new ServiceException(compEntity.getModelType() + "的上传文件必须gz文件");
        }


        File localFile = new File(filePath);
        if (!localFile.exists()) {
            localFile.mkdirs();
        }

        File dest = new File(filePath + "/" + fileName + extName);
        multipartFile.transferTo(dest);


        // 解码器的jar文件打包为tar文件处理
        if (compEntity.getModelType().equals("decoder")) {
            this.convertJarFile(compEntity, filePath, fileName, extName);
            return fileName + ".tar";
        }

        return fileName + extName;


    }

    private String convertJarFile(RepoCompEntity compEntity, String filePath, String fileName, String extName) throws IOException, InterruptedException {
        if (!".jar".equalsIgnoreCase(extName)) {
            return fileName + extName;
        }

        // 扫描jar文件的关键信息
        Map<String, Object> jarInfo = this.scanJarInfo(filePath + "\\" + fileName + extName);

        // 取出jar信息
        RepoJarEntity jarEntity = new RepoJarEntity();
        jarEntity.getProperty().setGroupId((String) jarInfo.getOrDefault("pom.properties.groupId", ""));
        jarEntity.getProperty().setArtifactId((String) jarInfo.getOrDefault("pom.properties.artifactId", ""));
        jarEntity.getProperty().setVersion((String) jarInfo.getOrDefault("pom.properties.version", ""));

        if (MethodUtils.hasEmpty(jarEntity.getProperty().getGroupId(),jarEntity.getProperty().getArtifactId(),jarEntity.getProperty().getVersion())){
            throw new ServiceException("该jar文件非法，无法读取到META-INF中的pom.properties的版本信息!");
        }

        String dependencies = (String) jarInfo.get("pom.xml.dependencies");
        if (MethodUtils.hasEmpty(dependencies)) {
            dependencies = "[]";
        }
        List<Map<String, Object>> dependencyList = JsonUtils.buildObjectWithoutException(dependencies, List.class);
        if (dependencyList == null) {
            dependencyList = new ArrayList<>();
        }
        for (Map<String, Object> dependency : dependencyList) {
            RepoJarInfo property = new RepoJarInfo();
            property.setGroupId((String) dependency.getOrDefault("groupId", ""));
            property.setArtifactId((String) dependency.getOrDefault("artifactId", ""));
            property.setVersion((String) dependency.getOrDefault("version", ""));

            jarEntity.getDependencies().add(property);
        }

        // 填入信息
        compEntity.setNamespace((String) jarInfo.getOrDefault("jarSpace", ""));
        compEntity.setManufacturer((String) jarInfo.getOrDefault("manufacturer", ""));
        compEntity.setDeviceType((String) jarInfo.getOrDefault("deviceType", ""));
        compEntity.setJarEntity(jarEntity);
        String jarVer = (String) jarInfo.get("jarVer");

        // 如果jarVer非空，说明这是尽力遵循命名规范的fox-edge解码器，这时候要检查版本一致性
        if (!MethodUtils.hasEmpty(jarVer) && !compEntity.getModelVersion().equalsIgnoreCase(jarVer)) {
            throw new ServiceException("jar文件中的接口版本不匹配，记录版本=" + compEntity.getModelVersion() + "，而jar文件的类名中的vxx信息=" + jarVer);
        }

        if (OSInfo.isWindows()) {
            String tempName = filePath + "\\" + this.getJarFileName(compEntity);
            tempName = tempName.replace("/", "\\");

            // 删除可能存在的临时文件
            ShellUtils.executeCmd("del /q " + tempName);

            File srcJarFile = new File(filePath + "\\" + fileName + extName);
            File dstJarFile = new File(filePath + "\\" + this.getJarFileName(compEntity));
            if (!srcJarFile.renameTo(dstJarFile)) {
                throw new ServiceException("修改文件名失败!");
            }
        }
        if (OSInfo.isLinux()) {
            String tempName = filePath + "/" + this.getJarFileName(compEntity);

            // 删除可能存在的临时文件
            ShellUtils.executeShell("rm -f " + tempName);

            File srcJarFile = new File(filePath + "/" + fileName + extName);
            File dstJarFile = new File(filePath + "/" + this.getJarFileName(compEntity));
            if (!srcJarFile.renameTo(dstJarFile)) {
                throw new ServiceException("修改文件名失败!");
            }
        }


        // 把当前文件复制到Dependency目录,作为后续其他jar包的依赖项目
        this.copyDependency(compEntity, this.getJarFileName(compEntity));

        // 将jar打包成tar文件
        this.packJar2Tar(compEntity.getModelType(), filePath, fileName + ".tar", this.getJarFileName(compEntity));

        return fileName + ".tar";
    }
}
