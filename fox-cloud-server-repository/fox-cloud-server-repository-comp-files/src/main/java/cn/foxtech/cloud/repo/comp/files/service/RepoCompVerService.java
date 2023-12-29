package cn.foxtech.cloud.repo.comp.files.service;


import org.springframework.stereotype.Component;

@Component
public class RepoCompVerService {
    /**
     * 将字符串格式的版本号，转换为数值格式的版本号
     *
     * @param version
     * @return
     */
    public Long convertLong(String version) {
        String[] items = version.split("\\.");
        if (items.length != 3) {
            throw new RuntimeException("版本号必须为:xx.xx.xx格式，例如，1.0.2");
        }
        long result = 0L;
        for (String item : items) {
            result = result * 100 + Integer.parseInt(item);
        }

        return result;
    }
}
