package cn.foxtech.cloud.manager.repository.service;

import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.uuid.UuidUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Component
public class RepoImageService {
    public String saveFile(MultipartFile multipartFile, String filePath) throws IOException {
        if (multipartFile.isEmpty()) {
            throw new ServiceException("上传文件为空");
        }

        // 上传方的原始文件名
        String originalFilename = multipartFile.getOriginalFilename();

        // 扩展名
        String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (MethodUtils.hasEmpty(extName)) {
            throw new ServiceException("上传文件必须为.JPG等图片文件");
        }


        File localFile = new File(filePath);
        if (!localFile.exists()) {
            localFile.mkdirs();
        }

        String fileName = UuidUtils.randomUUID() + extName;
        File dest = new File(filePath + "/" + fileName);
        multipartFile.transferTo(dest);

        return fileName;
    }


}
