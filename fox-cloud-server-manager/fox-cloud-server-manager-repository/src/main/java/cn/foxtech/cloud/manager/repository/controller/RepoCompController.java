package cn.foxtech.cloud.manager.repository.controller;

import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.manager.repository.constants.Constant;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoComp;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoCompVer;
import cn.foxtech.cloud.manager.repository.entity.RepoCompEntity;
import cn.foxtech.cloud.manager.repository.entity.RepoCompVerEntity;
import cn.foxtech.cloud.manager.repository.service.RepoCompService;
import cn.foxtech.common.utils.file.FileNameUtils;
import cn.foxtech.common.utils.md5.MD5Utils;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.osinfo.OSInfo;
import cn.foxtech.common.utils.shell.ShellUtils;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/repository/component")
public class RepoCompController {
    /**
     * 正则表达式：英文字母+ ‘-’和‘_’字符
     */
    public final String REGEX_PATTERN = "^([a-zA-Z0-9]+-?+_?)+[a-zA-Z0-9]{1,255}$";
    @Autowired
    private RepoCompService compService;

    private boolean validateModelType(String modelType) {
        if ("service".equals(modelType)) {
            return true;
        }
        if ("decoder".equals(modelType)) {
            return true;
        }
        if ("template".equals(modelType)) {
            return true;
        }
        if ("webpack".equals(modelType)) {
            return true;
        }
        return "system".equals(modelType);
    }

    /**
     * 验证文件名
     *
     * @param filename
     * @return
     */
    public boolean validateStringUsingRegex(String filename) {
        if (MethodUtils.hasEmpty(filename)) {
            return false;
        }

        return filename.matches(REGEX_PATTERN);
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("page")
    public AjaxResult getPage(@RequestBody Map<String, Object> body) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            Integer pageNum = (Integer) body.get(Constant.field_page_num);
            Integer pageSize = (Integer) body.get(Constant.field_page_size);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(pageNum, pageSize)) {
                throw new ServiceException("body参数缺失:pageNum, pageSize");
            }

            // 查询数据
            Map<String, Object> data = this.compService.queryPageList(username, body);

            return AjaxResult.success(data);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("entities")
    public AjaxResult getEntities(@RequestBody Map<String, Object> body) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 查询数据
            List<RepoCompEntity> entityList = this.compService.queryEntityList(username, body);

            return AjaxResult.success(entityList);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("entity")
    public AjaxResult insert(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String groupName = (String) body.get(ConstantRepoComp.field_group_name);
            String modelName = (String) body.get(ConstantRepoComp.field_model_name);
            String modelType = (String) body.get(ConstantRepoComp.field_model_type);
            String component = (String) body.get(ConstantRepoComp.field_component);
            String description = (String) body.get(ConstantRepoComp.field_description);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(groupName, modelName, modelType, component)) {
                throw new ServiceException("body参数缺失:groupName, modelName, modelType, component");
            }

            // 检查：模块名格式
            if (!this.validateStringUsingRegex(modelName)) {
                throw new ServiceException("modelName只能包含英文字符和横杠和下划线字符");
            }
            // 检查：模块类型
            if (!validateModelType(modelType)) {
                throw new ServiceException("modelType不在定义的范围内!");
            }

            RepoCompEntity entity = new RepoCompEntity();
            entity.setModelName(modelName.toLowerCase());
            entity.setModelType(modelType);
            entity.setOwnerId(userName);
            entity.setGroupName(groupName);
            entity.setComponent(component);
            entity.setDescription(description);

            // 查询数据
            this.compService.insertRepoCompEntity(entity);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PutMapping("entity")
    public AjaxResult update(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String modelName = (String) body.get(ConstantRepoComp.field_model_name);
            String modelType = (String) body.get(ConstantRepoComp.field_model_type);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType)) {
                throw new ServiceException("body参数缺失:modelName, modelType");
            }


            // 查询数据
            this.compService.updateRepoCompEntity(userName, body);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @DeleteMapping("entity")
    public AjaxResult delete(String modelName, String modelType) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType)) {
                throw new ServiceException("body参数缺失:modelName, modelType");
            }

            // 查询数据
            this.compService.deleteRepoCompEntity(userName, modelName, modelType);

            // 删除文件
            File file = new File("");
            String path = file.getAbsolutePath();
            String modelDir = path + "/repository/" + modelType + "/" + modelName;
            if (OSInfo.isWindows()) {
                // 删除可能存在的目录
                modelDir = modelDir.replace("/", "\\");
                ShellUtils.executeCmd("rd /s /q " + modelDir);
            }
            if (OSInfo.isLinux()) {
                ShellUtils.executeShell("rm -rf '" + modelDir + "'");
            }

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PutMapping("version")
    public AjaxResult updateVersion(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String modelName = (String) body.get(ConstantRepoComp.field_model_name);
            String modelType = (String) body.get(ConstantRepoComp.field_model_type);
            String version = (String) body.get(ConstantRepoComp.field_version);
            String stage = (String) body.get(ConstantRepoCompVer.field_stage);
            String newStage = (String) body.get(ConstantRepoCompVer.field_new_stage);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType, version, stage, newStage)) {
                throw new ServiceException("body参数缺失:modelName, modelType, version, stage, newStage");
            }

            // 查找指定的实体
            RepoCompEntity compEntity = this.compService.queryRepoCompEntity(modelType, modelName);
            if (compEntity == null) {
                throw new ServiceException("指定的实体不存在!");
            }

            // 验证权限
            if (!userName.equals("admin") && !compEntity.getOwnerId().equals(userName)) {
                throw new RuntimeException("没有权限删除该模块：只允许owner和admin更新该模块!");
            }

            // 查找并修改该版本
            RepoCompVerEntity find = null;
            for (RepoCompVerEntity verEntity : compEntity.getVersions()) {
                if (verEntity.getVersion().equals(version) && verEntity.getStage().equals(stage)) {
                    find = verEntity;
                    break;
                }
            }
            if (find == null) {
                throw new ServiceException("找不到指定的版本");
            }
            // 更新内容
            find.setStage(newStage);

            // 更新版本列表字段
            this.compService.updateRepoCompVerEntity(compEntity);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @DeleteMapping("version")
    public AjaxResult deleteVersion(String modelName, String modelType, String version, String stage) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType, version, stage)) {
                throw new ServiceException("body参数缺失:modelName, modelType, version, stage");
            }

            // 查找指定的实体
            RepoCompEntity compEntity = this.compService.queryRepoCompEntity(modelType, modelName);
            if (compEntity == null) {
                throw new ServiceException("指定的实体不存在!");
            }

            // 验证权限
            if (!userName.equals("admin") && !compEntity.getOwnerId().equals(userName)) {
                throw new RuntimeException("没有权限删除该模块：只允许owner和admin删除该模块!");
            }

            // 删除该版本
            String pathName = "";
            List<RepoCompVerEntity> versions = new ArrayList<>();
            for (RepoCompVerEntity verEntity : compEntity.getVersions()) {
                if (verEntity.getVersion().equals(version) && verEntity.getStage().equals(stage)) {
                    pathName = verEntity.getPathName();
                    continue;
                }
                versions.add(verEntity);
            }
            compEntity.setVersions(versions);

            // 更新版本列表字段
            this.compService.updateRepoCompVerEntity(compEntity);

            // 删除文件
            File file = new File("");
            String path = file.getAbsolutePath();
            String modelDir = path + "/repository/" + modelType + "/" + modelName;
            String fileDir = modelDir + "/" + version;

            // 删除该版本对应的本地文件
            String fileName = fileDir + "/" + pathName;
            File delFile = new File(fileName);
            delFile.delete();

            // 如果版本位空，那么删除空目录
            if (compEntity.getVersions().isEmpty()){
                if (OSInfo.isWindows()) {
                    // 删除可能存在的目录
                    fileDir = fileDir.replace("/", "\\");
                    ShellUtils.executeCmd("rd /s /q " + fileDir);
                }
                if (OSInfo.isLinux()) {
                    ShellUtils.executeShell("rm -rf '" + fileDir + "'");
                }
            }


            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public AjaxResult uploadVersion(@RequestParam("file") MultipartFile uploadFile, @RequestParam("modelName") String modelName, @RequestParam("modelType") String modelType, @RequestParam("component") String component) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType, component)) {
                throw new ServiceException("body参数缺失:modelName, modelType,component");
            }

            // 查找指定的实体
            RepoCompEntity compEntity = this.compService.queryRepoCompEntity(modelType, modelName);
            if (compEntity == null) {
                throw new ServiceException("指定的实体不存在!");
            }

            if (!userName.equals("admin") && !compEntity.getOwnerId().equals(userName)) {
                throw new RuntimeException("没有权限新增该模块：只允许owner和admin删除该模块!");
            }


            File file = new File("");
            String path = file.getAbsolutePath();


            // 保存文件
            Long time = System.currentTimeMillis();
            String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(time);
            String fileName = modelName + "-" + currentTime;
            fileName = this.saveFile(uploadFile, modelType, path + "/repository/" + modelType + "/" + modelName, fileName);


            // 计算MD5验证码
            File md5File = new File(path + "/repository/" + modelType + "/" + modelName + "/" + fileName);
            String md5Txt = MD5Utils.getMD5Txt(md5File);
            long fileSize = md5File.length();

            // 最新的master版本号
            long lastMasterVersion = this.compService.newLastMasterVersion(compEntity.getVersions());


            RepoCompVerEntity verEntity = new RepoCompVerEntity();
            verEntity.setVersion(this.compService.convertVersion(lastMasterVersion));
            verEntity.setStage(ConstantRepoCompVer.value_stage_master);
            verEntity.setComponent(component);
            verEntity.setDescription("");
            verEntity.setCreateTime(time);
            verEntity.setUpdateTime(time);
            verEntity.setPathName(fileName);
            verEntity.setMd5(md5Txt);
            verEntity.setFileSize(fileSize);

            // 追加版本
            compEntity.getVersions().add(0, verEntity);

            // 保存信息到数据库
            this.compService.updateRepoCompVerEntity(compEntity);


            // 将上传的临时文件，移动到指定目录
            this.moveFile(modelType, modelName, verEntity);

            // 删除可能存在的历史垃圾文件
            String modelDir = path + "/repository/" + modelType + "/" + modelName;
            this.removeFile(compEntity, modelDir);


            Map<String, Object> data = new HashMap<>();
            data.put("name", uploadFile.getOriginalFilename());
            data.put("url", fileName);

            return AjaxResult.success(data);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    private void moveFile(String modelType, String modelName, RepoCompVerEntity verEntity) throws IOException, InterruptedException {
        File file = new File("");
        String path = file.getAbsolutePath();

        String modelDir = path + "/repository/" + modelType + "/" + modelName;
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
    private void removeFile(RepoCompEntity compEntity, String modelDir) throws IOException, InterruptedException {
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

            // 检查：是否为目录
            File file = new File(localName);
            if (file.isDirectory()) {
                continue;
            }

            // 删除垃圾文件
            file.delete();
        }
    }

    private String saveFile(MultipartFile multipartFile, String modelType, String filePath, String fileName) throws IOException, InterruptedException {
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
        if (".jar".equalsIgnoreCase(extName) && !modelType.equals("decoder")) {
            throw new ServiceException(modelType + "的上传文件必须jar文件");
        } else if (".tar".equalsIgnoreCase(extName) && !modelType.equals("service") && !modelType.equals("template") && !modelType.equals("webpack")) {
            throw new ServiceException(modelType + "的上传文件必须tar文件");
        } else if (".gz".equalsIgnoreCase(extName) && !modelType.equals("system")) {
            throw new ServiceException(modelType + "的上传文件必须gz文件");
        }


        File localFile = new File(filePath);
        if (!localFile.exists()) {
            localFile.mkdirs();
        }

        //  场景1：tar文件，直接保存
        if (".tar".equalsIgnoreCase(extName)) {
            File dest = new File(filePath + "/" + fileName + ".tar");
            multipartFile.transferTo(dest);
            return fileName + ".tar";
        }
        //  场景2：gz文件，直接保存
        if (".gz".equalsIgnoreCase(extName)) {
            if (!originalFilename.endsWith(".tar.gz")) {
                throw new ServiceException(modelType + "的上传文件必须.tar.gz文件");
            }

            File dest = new File(filePath + "/" + fileName + ".tar.gz");
            multipartFile.transferTo(dest);
            return fileName + ".tar.gz";
        }
        // 场景3：jar文件，压缩后，再保存
        if (".jar".equalsIgnoreCase(extName)) {
            if (OSInfo.isWindows()) {
                String tempName = filePath + "\\" + originalFilename;
                tempName = tempName.replace("/", "\\");

                // 删除可能存在的临时文件
                ShellUtils.executeCmd("del /q " + tempName);

                // 生成同名的本地文件
                File dest = new File(tempName);
                multipartFile.transferTo(dest);

                // 打包成tar文件
                ShellUtils.executeCmd("tar -cvf " + filePath + "\\" + fileName + ".tar -C " + filePath + " " + originalFilename);

                // 删除临时的jar文件
                ShellUtils.executeCmd("del /q " + tempName);
                return fileName + ".tar";
            }
            if (OSInfo.isLinux()) {
                String tempName = filePath + "/" + originalFilename;

                // 删除可能存在的临时文件
                ShellUtils.executeShell("rm -f " + tempName);

                // 生成同名的本地文件
                File dest = new File(tempName);
                multipartFile.transferTo(dest);

                // 打包成tar文件
                ShellUtils.executeShell("tar -cvf " + filePath + "/" + fileName + ".tar -C " + filePath + " " + originalFilename);

                // 删除临时的jar文件
                ShellUtils.executeShell("rm -f " + tempName);
                return fileName + ".tar";
            }
        }

        return fileName + ".tar";
    }

    /**
     * 下载文件
     *
     * @param body 参数信息
     * @return 强制返回HTTP的状态码200/500，这是因为文件下载的blob模式时，一般的出错返回都在blob文件流中，前端无法识别
     */
    @PostMapping("/download")
    public ResponseEntity<AjaxResult> downloadFile(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 提取业务参数
            String modelName = (String) body.get(ConstantRepoComp.field_model_name);
            String modelType = (String) body.get(ConstantRepoComp.field_model_type);
            String version = (String) body.get(ConstantRepoComp.field_version);
            String stage = (String) body.get(ConstantRepoCompVer.field_stage);
            if (MethodUtils.hasEmpty(modelName, version, modelType, stage)) {
                throw new ServiceException("参数不能为空:modelName, version, modelType, stage");
            }

            // 查询版本
            RepoCompVerEntity verEntity = this.compService.queryRepoCompVerEntity(userName, modelName, modelType, version, stage);
            if (verEntity == null) {
                throw new ServiceException("找不到该版本信息");
            }

            File file = new File("");
            String fileName = file.getAbsolutePath() + "/repository/" + modelType + "/" + modelName + "/" + verEntity.getVersion() + "/" + verEntity.getPathName();

            this.downOneFile(fileName);

            return ResponseEntity.ok().body(AjaxResult.success());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AjaxResult.error(e.getMessage()));
        }
    }

    private void downOneFile(String fileName) throws Exception {
        HttpServletResponse resp = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();

        File download = new File(fileName);
        if (!download.exists()) {
            throw new ServiceException("服务器上的文件已经删除!");
        }


        resp.setContentType("application/x-msdownload");
        resp.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(), StandardCharsets.ISO_8859_1));
        InputStream inputStream = Files.newInputStream(download.toPath());
        ServletOutputStream ouputStream = resp.getOutputStream();
        byte[] b = new byte[1024];
        int n;
        while ((n = inputStream.read(b)) != -1) {
            ouputStream.write(b, 0, n);
        }
        ouputStream.close();
        inputStream.close();
    }
}
