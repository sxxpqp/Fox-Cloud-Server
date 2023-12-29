package cn.foxtech.cloud.repository.controller;

import cn.craccd.mongoHelper.utils.CriteriaAndWrapper;
import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.comp.files.constants.ConstantRepoComp;
import cn.foxtech.cloud.repo.comp.files.constants.ConstantRepoCompVer;
import cn.foxtech.cloud.repo.comp.files.entity.RepoCompEntity;
import cn.foxtech.cloud.repo.comp.files.entity.RepoCompVerEntity;
import cn.foxtech.cloud.repo.comp.files.service.RepoCompService;
import cn.foxtech.cloud.repo.comp.files.service.RepoFileService;
import cn.foxtech.cloud.repo.comp.script.constants.ConstantRepoCompScript;
import cn.foxtech.cloud.repo.group.entity.RepoGroupEntity;
import cn.foxtech.cloud.repo.group.service.RepoGroupService;
import cn.foxtech.cloud.repository.service.RepoUploadService;
import cn.foxtech.common.utils.bean.BeanMapUtils;
import cn.foxtech.common.utils.method.MethodUtils;
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
import sun.awt.OSInfo;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/repository/component")
public class RepoCompFilesController {
    @Autowired
    private RepoCompService compService;

    @Autowired
    private RepoGroupService groupService;

    @Autowired
    private RepoFileService fileService;

    @Autowired
    private RepoUploadService uploadService;


    private String makeModelVersion(String modelType, String modelVersion) {
        if (!modelType.equals(ConstantRepoComp.field_value_model_type_decoder)) {
            return "v1";
        }

        return modelVersion;
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("page")
    public AjaxResult getPage(@RequestBody Map<String, Object> body) {
        return this.getList(body, true);
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("entities")
    public AjaxResult getEntities(@RequestBody Map<String, Object> body) {
        return this.getList(body, false);
    }

    public AjaxResult getList(Map<String, Object> body, boolean isPage) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 当前用户所属的组信息
            Set<String> groupIds = this.groupService.queryGroupIds(username);

            // 构造查询过滤器
            CriteriaAndWrapper criteriaWrapper = this.compService.buildWrapper(username, groupIds, body);

            // 查询数据
            List<String> filterKeys = new ArrayList<>();
            filterKeys.add(ConstantRepoCompScript.field_commit_key);


            if (isPage) {
                Map<String, Object> data = this.compService.queryPageList(criteriaWrapper, body);

                List<RepoCompEntity> list = (List<RepoCompEntity>) data.get("list");

                // 扩展属主信息
                Map<String, RepoGroupEntity> groupMap = this.groupService.queryGroupEntityMap(username);
                this.compService.extendGroupName(list, groupMap);

                data.put("list", BeanMapUtils.objectToMap(list, filterKeys));
                return AjaxResult.success(data);
            } else {
                // 查询数据
                List<RepoCompEntity> entityList = this.compService.queryEntityList(criteriaWrapper);

                // 过滤数据
                entityList = this.compService.extendAndFilter(entityList);

                return AjaxResult.success(BeanMapUtils.objectToMap(entityList, filterKeys));
            }


        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("groupName")
    public AjaxResult getGroupName(@RequestBody Map<String, Object> body) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            String modelType = (String) body.get(ConstantRepoComp.field_model_type);
            String modelName = (String) body.get(ConstantRepoComp.field_model_name);
            String modelVersion = (String) body.get(ConstantRepoComp.field_model_version);
            if (MethodUtils.hasEmpty(modelType, modelName, modelVersion)) {
                throw new ServiceException("body参数缺失: id");
            }

            List<RepoCompEntity> compEntityList = this.compService.queryEntityList(body);
            if (compEntityList.isEmpty()) {
                throw new ServiceException("找不到指定的组件!");
            }

            String compId = compEntityList.get(0).getId();
            String groupId = compEntityList.get(0).getGroupId();


            // 查询数据
            List<String> ids = new ArrayList<>();
            ids.add(groupId);
            Map<String, String> groupNames = this.groupService.queryGroupNames(ids);

            // 返回数据
            Map<String, String> result = new HashMap<>();
            result.put(ConstantRepoComp.field_id, compId);
            result.put(ConstantRepoComp.field_group_id, groupId);
            result.put(ConstantRepoComp.field_group_name, groupNames.get(groupId));
            return AjaxResult.success(result);

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

            // 查询数据
            this.compService.insertRepoCompEntity(userName, body);

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
            String modelVersion = (String) body.get(ConstantRepoComp.field_model_version);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType, modelVersion)) {
                throw new ServiceException("body参数缺失: modelName, modelType, modelVersion");
            }

            // 规范化命名：只有decoder才允许多办法，其他都是只能单一版本，也就是v1
            modelVersion = this.makeModelVersion(modelType, modelVersion);


            // 查询数据
            this.compService.updateRepoCompEntity(userName, body);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @DeleteMapping("entity")
    public AjaxResult delete(String modelName, String modelType, String modelVersion) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType, modelVersion)) {
                throw new ServiceException("body参数缺失:modelName, modelType, modelVersion");
            }

            // 规范化命名：只有decoder才允许多办法，其他都是只能单一版本，也就是v1
            modelVersion = this.makeModelVersion(modelType, modelVersion);

            // 查询数据
            this.compService.deleteRepoCompEntity(userName, modelName, modelType, modelVersion);

            // 删除文件
            File file = new File("");
            String path = file.getAbsolutePath();
            String modelDir = path + "/repository/" + modelType + "/" + modelName + "/" + modelVersion;
            if (OSInfo.getOSType().equals(OSInfo.OSType.WINDOWS)) {
                // 删除可能存在的目录
                modelDir = modelDir.replace("/", "\\");
                ShellUtils.executeCmd("rd /s /q " + modelDir);
            }
            if (OSInfo.getOSType().equals(OSInfo.OSType.LINUX)) {
                ShellUtils.executeShell("rm -rf '" + modelDir + "'");
            }

            // 删除空的父目录
            this.deleteEmptyParentDir(modelDir);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 删除空的父目录
     *
     * @param fileDir
     * @throws IOException
     * @throws InterruptedException
     */
    private void deleteEmptyParentDir(String fileDir) throws IOException, InterruptedException {
        Path path = Paths.get(fileDir);
        File file = path.getParent().toFile();
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }

        // 是否为空目录
        if (file.list().length != 0) {
            return;
        }

        // 切换目录
        fileDir = file.getAbsolutePath();

        if (OSInfo.getOSType().equals(OSInfo.OSType.WINDOWS)) {
            // 删除可能存在的目录
            fileDir = fileDir.replace("/", "\\");
            ShellUtils.executeCmd("rd /s /q " + fileDir);
        }
        if (OSInfo.getOSType().equals(OSInfo.OSType.LINUX)) {
            ShellUtils.executeShell("rm -rf '" + fileDir + "'");
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
            String modelVersion = (String) body.get(ConstantRepoComp.field_model_version);
            String version = (String) body.get(ConstantRepoComp.field_version);
            String stage = (String) body.get(ConstantRepoCompVer.field_stage);
            String newStage = (String) body.get(ConstantRepoCompVer.field_new_stage);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType, modelVersion, version, stage, newStage)) {
                throw new ServiceException("body参数缺失:modelName, modelType, modelVersion, version, stage, newStage");
            }

            // 规范化命名：只有decoder才允许多办法，其他都是只能单一版本，也就是v1
            modelVersion = this.makeModelVersion(modelType, modelVersion);

            // 查找指定的实体
            RepoCompEntity compEntity = this.compService.queryRepoCompEntity(modelType, modelName, modelVersion);
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
    public AjaxResult deleteVersion(String modelName, String modelType, String modelVersion, String version, String stage) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType, modelVersion, version, stage)) {
                throw new ServiceException("body参数缺失:modelName, modelType, modelVersion, version, stage");
            }

            // 规范化命名：只有decoder才允许多办法，其他都是只能单一版本，也就是v1
            modelVersion = this.makeModelVersion(modelType, modelVersion);

            // 查找指定的实体
            RepoCompEntity compEntity = this.compService.queryRepoCompEntity(modelType, modelName, modelVersion);
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
            String fileDir = modelDir + "/" + compEntity.getModelVersion() + "/" + version;

            // 删除该版本对应的本地文件
            String fileName = fileDir + "/" + pathName;
            File delFile = new File(fileName);
            delFile.delete();

            // 如果版本位空，那么删除空目录
            if (compEntity.getVersions().isEmpty()) {
                if (OSInfo.getOSType().equals(OSInfo.OSType.WINDOWS)) {
                    // 删除可能存在的目录
                    fileDir = fileDir.replace("/", "\\");
                    ShellUtils.executeCmd("rd /s /q " + fileDir);
                }
                if (OSInfo.getOSType().equals(OSInfo.OSType.LINUX)) {
                    ShellUtils.executeShell("rm -rf '" + fileDir + "'");
                }
            }


            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequiresPermissions("monitor:repo:query")
    @PutMapping("commitKey")
    public AjaxResult updateCommitKey(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 查询数据
            CriteriaAndWrapper criteriaAndWrapper = this.compService.buildWrapper(body);
            this.compService.updateRepoCompEntity(userName, criteriaAndWrapper, body);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public AjaxResult uploadVersion(@RequestParam("file") MultipartFile uploadFile, @RequestParam("modelName") String modelName, @RequestParam("modelType") String modelType, @RequestParam("modelVersion") String modelVersion, @RequestParam("component") String component, @RequestParam("commitKey") String commitKey) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(modelName, modelType, modelVersion, component)) {
                throw new ServiceException("body参数缺失:modelName, modelType, modelVersion, component");
            }

            // 上传文件
            Map<String, Object> data = this.uploadService.uploadComponent(commitKey, uploadFile, modelName, modelType, modelVersion, component);


            return AjaxResult.success(data);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        } finally {
            this.fileService.cleanResidue(modelName, modelType, modelVersion);
        }
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
            String modelVersion = (String) body.get(ConstantRepoComp.field_model_version);
            String version = (String) body.get(ConstantRepoComp.field_version);
            String stage = (String) body.get(ConstantRepoCompVer.field_stage);
            if (MethodUtils.hasEmpty(modelName, modelVersion, version, modelType, stage)) {
                throw new ServiceException("参数不能为空:modelName, modelVersion, version, modelType, stage");
            }

            // 规范化命名：只有decoder才允许多办法，其他都是只能单一版本，也就是v1
            modelVersion = this.makeModelVersion(modelType, modelVersion);

            // 查询版本
            RepoCompVerEntity verEntity = this.compService.queryRepoCompVerEntity(userName, modelName, modelType, modelVersion, version, stage);
            if (verEntity == null) {
                throw new ServiceException("找不到该版本信息");
            }

            File file = new File("");
            String fileName = file.getAbsolutePath() + "/repository/" + modelType + "/" + modelName + "/" + modelVersion + "/" + verEntity.getVersion() + "/" + verEntity.getPathName();

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
