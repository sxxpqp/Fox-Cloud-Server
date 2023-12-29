package cn.foxtech.cloud.repository.service;

import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.repo.comp.files.constants.ConstantRepoComp;
import cn.foxtech.cloud.repo.comp.files.entity.RepoCompEntity;
import cn.foxtech.cloud.repo.comp.files.entity.RepoCompVerEntity;
import cn.foxtech.cloud.repo.comp.files.service.RepoCompService;
import cn.foxtech.cloud.repo.comp.files.service.RepoFileService;
import cn.foxtech.common.utils.md5.MD5Utils;
import cn.foxtech.common.utils.method.MethodUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@Component
public class RepoUploadService {
    @Autowired
    private RepoFileService fileService;

    @Autowired
    private RepoCompService compService;

    /**
     * 上传组件
     *
     * @param file         从web上传的MultipartFile，或者是，本地git目录复制过来的File
     * @param modelName
     * @param modelType
     * @param modelVersion
     * @param component
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public Map<String, Object> uploadComponent(String commitKey, Object file, String modelName, String modelType, String modelVersion, String component) throws IOException, InterruptedException {
        if (!(file instanceof MultipartFile) && !(file instanceof File)) {
            throw new ServiceException("file 必须是 MultipartFile 或 File ");
        }

        String userName = SecurityUtils.getUsername();
        if (MethodUtils.hasEmpty(userName)) {
            throw new ServiceException("获得登录用户信息失败!");
        }

        // 检查：是否至少包含以下几个参数
        if (MethodUtils.hasEmpty(modelName, modelType, modelVersion, component)) {
            throw new ServiceException("body参数缺失:modelName, modelType, modelVersion, component");
        }

        // 规范化命名：只有decoder才允许多办法，其他都是只能单一版本，也就是v1
        modelVersion = this.makeModelVersion(modelType, modelVersion);

        // 查找指定的实体
        RepoCompEntity compEntity = this.compService.queryRepoCompEntity(modelType, modelName, modelVersion);
        if (compEntity == null) {
            throw new ServiceException("指定的实体不存在!");
        }

        if (!userName.equals("admin")) {
            if (MethodUtils.hasEmpty(commitKey)) {
                throw new ServiceException("只有下列人员才可以提交：admin和持有commitKey的人员：" + compEntity.getGroupName());
            }

            // 验证提交密码
            if (!SecurityUtils.matchesPassword(commitKey, compEntity.getCommitKey())) {
                throw new ServiceException("commitKey验证不通过！");
            }
        }


        File dir = new File("");
        String path = dir.getAbsolutePath();


        // 保存文件
        Long time = System.currentTimeMillis();
        String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(time);
        String fileName = modelName + "-" + currentTime;
        String srcFileName = "";
        if (file instanceof MultipartFile) {
            srcFileName = ((MultipartFile) file).getOriginalFilename();
            fileName = this.fileService.saveFile(compEntity, (MultipartFile) file, path + "/repository/" + modelType + "/" + modelName + "/" + modelVersion, fileName);
        }
        if (file instanceof File) {
            srcFileName = ((File) file).getName();
            fileName = this.fileService.saveFile(compEntity, (File) file, path + "/repository/" + modelType + "/" + modelName + "/" + modelVersion, fileName);
        }

        // 计算MD5验证码
        File md5File = new File(path + "/repository/" + modelType + "/" + modelName + "/" + modelVersion + "/" + fileName);
        String md5Txt = MD5Utils.getMD5Txt(md5File);
        long fileSize = md5File.length();

        // 最新的master版本号
        RepoCompVerEntity verEntity = this.compService.makeVersion(compEntity, component, fileName, md5Txt, fileSize);

        // 保存信息到数据库
        this.compService.updateRepoCompVerEntity(compEntity);


        // 将上传的临时文件，移动到指定目录
        this.fileService.moveFile(modelType, modelName, modelVersion, verEntity);

        // 删除可能存在的历史垃圾文件
        String modelDir = path + "/repository/" + modelType + "/" + modelName + "/" + modelVersion;
        this.fileService.removeFile(compEntity, modelDir);


        Map<String, Object> data = new HashMap<>();
        data.put("name", srcFileName);
        data.put("url", fileName);

        return data;
    }

    private String makeModelVersion(String modelType, String modelVersion) {
        if (!modelType.equals(ConstantRepoComp.field_value_model_type_decoder)) {
            return "v1";
        }

        return modelVersion;
    }
}
