package cn.foxtech.cloud.manager.repository.controller;

import cn.craccd.mongoHelper.bean.SortBuilder;
import cn.foxtech.cloud.core.domain.AjaxResult;
import cn.foxtech.cloud.core.exception.ServiceException;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoComp;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoProduct;
import cn.foxtech.cloud.manager.repository.constants.ConstantRepoProductComp;
import cn.foxtech.cloud.manager.repository.entity.RepoCompEntity;
import cn.foxtech.cloud.manager.repository.entity.RepoProductComp;
import cn.foxtech.cloud.manager.repository.entity.RepoProductEntity;
import cn.foxtech.cloud.manager.repository.service.RepoCompService;
import cn.foxtech.cloud.manager.repository.service.RepoImageService;
import cn.foxtech.cloud.manager.repository.service.RepoProductCompService;
import cn.foxtech.cloud.manager.repository.service.RepoProductEntityService;
import cn.foxtech.common.utils.bean.BeanMapUtils;
import cn.foxtech.common.utils.method.MethodUtils;
import cn.foxtech.common.utils.uuid.UuidUtils;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/repository/product")
public class RepoProductController {

    private Long lastCleanTime = 0L;
    @Autowired
    private RepoProductEntityService productEntityService;
    @Autowired
    private RepoProductCompService productCompService;

    @Autowired
    private RepoImageService imageService;
    @Autowired
    private RepoCompService repoCompService;

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("page")
    public AjaxResult getPage(@RequestBody Map<String, Object> body) {
        try {
            // 查询数据
            Map<String, Object> data = this.productEntityService.queryProductEntityPage(body, new SortBuilder(RepoProductEntity::getWeight, Sort.Direction.DESC));

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
            List<RepoProductEntity> entityList = this.productEntityService.queryProductList(new SortBuilder(RepoProductEntity::getWeight, Sort.Direction.DESC));

            return AjaxResult.success(entityList);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @RequiresPermissions("monitor:repo:query")
    @GetMapping("entity")
    public AjaxResult getEntity(@RequestParam("uuid") String uuid) {
        try {
            String username = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(username)) {
                throw new ServiceException("获得登录用户信息失败!");
            }

            // 查询产品
            RepoProductEntity productEntity = this.productEntityService.queryProductEntity(uuid);
            if (productEntity == null) {
                throw new ServiceException("找不到该实体");
            }

            // 扩展组件的详细信息
            Map<String, Object> productMap = this.extendComps(productEntity);

            return AjaxResult.success(productMap);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    private Map<String, Object> extendComps(RepoProductEntity productEntity) {
        // 构造查询参数
        Map<String, Object> param = new HashMap<>();
        List<String> modelNames = new ArrayList<>();
        for (RepoProductComp comp : productEntity.getComps()) {
            modelNames.add(comp.getModelName());
        }
        param.put(ConstantRepoComp.field_model_names, modelNames);


        // 粗略查询批量数据（非精确查询）
        List<RepoCompEntity> compEntityList = this.repoCompService.queryEntityList(param);

        Map<String, Object> comps = new HashMap<>();
        for (RepoProductComp comp : productEntity.getComps()) {
            String key = comp.getModelType() + ":" + comp.getModelName() + ":" + comp.getModelVersion();
            comps.put(key, BeanMapUtils.objectToMap(comp));
        }

        // 拼装详细信息
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (RepoCompEntity entity : compEntityList) {
            String key = entity.getModelType() + ":" + entity.getModelName() + ":" + entity.getModelVersion();
            Map<String, Object> comp = (Map<String, Object>) comps.get(key);
            if (comp == null) {
                continue;
            }

            // 填充last版本信息
            this.repoCompService.extendLastVersion(entity);

            // 把UUID替换成产品中的UUID
            Map<String, Object> data = BeanMapUtils.objectToMap(entity);
            data.put(ConstantRepoProductComp.field_uuid, comp.get(ConstantRepoProductComp.field_uuid));

            dataList.add(data);
        }

        // 替换掉组件详细信息
        Map<String, Object> productMap = BeanMapUtils.objectToMap(productEntity);
        productMap.put(ConstantRepoProduct.field_comps, dataList);

        return productMap;
    }

    @RequiresPermissions("monitor:repo:query")
    @PostMapping("entity")
    public AjaxResult insert(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }
            if (!userName.equals("admin")) {
                throw new ServiceException("只有admin用户才允许插入数据!");
            }

            String name = (String) body.get(ConstantRepoProduct.field_model);
            String manufacturer = (String) body.get(ConstantRepoProduct.field_manufacturer);
            String url = (String) body.get(ConstantRepoProduct.field_url);
            String tags = (String) body.get(ConstantRepoProduct.field_tags);
            String image = (String) body.get(ConstantRepoProduct.field_image);
            String description = (String) body.get(ConstantRepoProduct.field_description);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(name, manufacturer, url, image)) {
                throw new ServiceException("body参数缺失: name, manufacturer, url, image");
            }

            RepoProductEntity entity = new RepoProductEntity();
            entity.setUuid(UuidUtils.randomUUID());
            entity.setManufacturer(manufacturer);
            entity.setModel(name);
            entity.setImage(image);
            entity.setUrl(url);
            entity.setTags(tags);
            entity.setDescription(description);

            // 查询数据
            this.productEntityService.insertProductEntity(entity);

            // 删除残留文件
            this.deleteRemainImageFile();

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    private void deleteRemainImageFile() {
        try {
            if (System.currentTimeMillis() - this.lastCleanTime < 24L * 3600L * 1000L) {
                return;
            }

            this.lastCleanTime = System.currentTimeMillis();

            Set<String> fileNames = new HashSet<>();
            List<RepoProductEntity> list = this.productEntityService.queryProductList(new SortBuilder(RepoProductEntity::getWeight, Sort.Direction.DESC));
            for (RepoProductEntity entity : list) {
                fileNames.add(entity.getImage());
            }

            File dir = new File("");
            File imageDir = new File(dir.getAbsolutePath() + "/repository/image/");
            if (!imageDir.isDirectory()) {
                return;
            }

            for (String imageName : imageDir.list()) {
                if (!fileNames.contains(imageName)) {
                    File image = new File(imageDir.getAbsolutePath() + "/" + imageName);
                    image.delete();
                }
            }
        } catch (Exception e) {

        }
    }

    @DeleteMapping("entity")
    public AjaxResult delete(String uuid) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }
            if (!userName.equals("admin")) {
                throw new ServiceException("只有admin用户才允许删除数据!");
            }

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(uuid)) {
                throw new ServiceException("body参数缺失: uuid");
            }


            // 查询数据
            this.productEntityService.deleteProductEntity(uuid);

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
            if (!userName.equals("admin")) {
                throw new ServiceException("只有admin用户才允许修改数据!");
            }

            String uuid = (String) body.get(ConstantRepoProduct.field_uuid);
            String manufacturer = (String) body.get(ConstantRepoProduct.field_manufacturer);
            String model = (String) body.get(ConstantRepoProduct.field_model);
            String url = (String) body.get(ConstantRepoProduct.field_url);
            String image = (String) body.get(ConstantRepoProduct.field_image);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(uuid, manufacturer, model, url, image)) {
                throw new ServiceException("body参数缺失: uuid, manufacturer, model, url, image");
            }

            // 查询数据
            this.productEntityService.updateProductEntity(body);

            // 删除残留文件
            this.deleteRemainImageFile();

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PutMapping("weight")
    public AjaxResult updateWeight(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }
            if (!userName.equals("admin")) {
                throw new ServiceException("只有admin用户才允许修改数据!");
            }

            String uuid = (String) body.get(ConstantRepoProduct.field_uuid);
            Integer weight = (Integer) body.get(ConstantRepoProduct.field_weight);

            // 检查：是否至少包含以下几个参数
            if (MethodUtils.hasEmpty(uuid, weight)) {
                throw new ServiceException("body参数缺失: uuid, weight");
            }

            // 查询数据
            this.productEntityService.updateProductEntity(body);

            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PutMapping("component")
    public AjaxResult updateCompEntity(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }
            if (!userName.equals("admin")) {
                throw new ServiceException("只有admin用户才允许修改数据!");
            }

            RepoProductComp compEntity = this.productCompService.updateCompEntity(body);
            return AjaxResult.success(compEntity);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("component")
    public AjaxResult insertCompEntity(@RequestBody Map<String, Object> body) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }
            if (!userName.equals("admin")) {
                throw new ServiceException("只有admin用户才允许修改数据!");
            }

            // 检查：指定的组件是否存在
            if (!this.existRepoComp(body)) {
                throw new ServiceException("指定的组件不存在，请检查输入信息!");
            }

            RepoProductComp compEntity = this.productCompService.insertCompEntity(body);
            return AjaxResult.success(compEntity);

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    private boolean existRepoComp(Map<String, Object> body) {
        String modelType = (String) body.get(ConstantRepoProductComp.field_model_type);
        String modelName = (String) body.get(ConstantRepoProductComp.field_model_name);
        String modelVersion = (String) body.get(ConstantRepoProductComp.field_model_version);

        if (MethodUtils.hasNull(modelType, modelName, modelVersion)) {
            throw new ServiceException("body参数缺失: modelType, modelName, modelVersion");
        }

        RepoCompEntity repoCompEntity = this.repoCompService.queryRepoCompEntity(modelType, modelName, modelVersion);
        if (MethodUtils.hasEmpty(repoCompEntity)) {
            throw new ServiceException("指定的部件不存在!");
        }

        return true;
    }

    @DeleteMapping("component")
    public AjaxResult deleteCompEntity(String productId, String uuid) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }
            if (!userName.equals("admin")) {
                throw new ServiceException("只有admin用户才允许修改数据!");
            }

            this.productCompService.deleteCompEntity(productId, uuid);
            return AjaxResult.success();

        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public AjaxResult uploadImage(@RequestParam("file") MultipartFile uploadFile) {
        try {
            String userName = SecurityUtils.getUsername();
            if (MethodUtils.hasEmpty(userName)) {
                throw new ServiceException("获得登录用户信息失败!");
            }
            if (!userName.equals("admin")) {
                throw new ServiceException("只有admin用户才允许上传文件!");
            }


            File file = new File("");
            String path = file.getAbsolutePath();


            // 保存文件
            String fileName = this.imageService.saveFile(uploadFile, path + "/repository/image");

            Map<String, Object> data = new HashMap<>();
            data.put("name", uploadFile.getOriginalFilename());
            data.put("url", fileName);

            return AjaxResult.success(data);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
