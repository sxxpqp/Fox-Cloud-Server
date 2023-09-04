# fox-cloud-server-receiver

#### 介绍
``` 
云端接收器
它的作用对来自各个边缘节点的数据，进行接收保存到云端的redis
它在将全部边缘节点的数据，统一汇聚到云端的redis后，方便云端数据进行统一查询出来
``` 

#### 软件架构
软件架构说明


#### 安装教程

1.  该服务职能在LINUX环境运行，所以需要远程调试
2.  远程调试的配置，不要参考简单网上的配置，因为linux的默认端口是环回端口，无法远程连接
3.  linux侧的启动命令，必须强制指明IP地址，例如:
java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=192.168.241.128:5005 fox-device-adapter-service-0.0.1.jar

#### 使用说明
``` 
1、查询时间戳
http://127.0.0.1:9501/cloud/receiver/timestamp
POST
发送：
{
    "edgeId": "BFEBFBFF000406E3",
    "entityTypeList": ["DeviceEntity"]
}
返回：
{
    "msg": "操作成功",
    "code": 200,
    "data": {
        "DeviceEntity": "1662203638314"
    }
}

2、查询reset标记
http://127.0.0.1:9501/cloud/receiver/reset
POST
发送：
{
    "edgeId": "BFEBFBFF000406E3",
    "operate": "get",
    "entityTypeList": ["DeviceEntity","DeviceValueEntity"]
}
返回：
{
    "msg": "操作成功",
    "code": 200,
    "data": {
        "DeviceEntity": false,
        "DeviceValueEntity": false
    }
}

3、设备reset标记
http://127.0.0.1:9501/cloud/receiver/reset
POST
发送：
{
    "edgeId": "BFEBFBFF000406E3",
    "operate": "set",
    "entityTypeList": ["DeviceEntity","DeviceValueEntity"]
}
返回：
{
    "msg": "操作成功",
    "code": 200,
    "data": {}
}

3、发送同步数据-初始化
http://127.0.0.1:9501/cloud/receiver/entity
POST
发送：
{
    "edgeId": "边缘服务器-01号",
    "entityType": "DeviceEntity",
    "timeStamp": "1b1df78266b9449c9d5705f821a2b4c2",
    "data": {
        "reset": [
            {
                "deviceType": "刘日威解码器",
                "createTime": 1652275102255,
                "commTime": 1652275102255,
                "channelType": "simulator",
                "updateTime": 1652275102255,
                "channelName": "channel-simulator",
                "id": 1,
                "deviceName": "阿威的单板"
            }
        ]
    }
}
返回：
{
    "msg": "操作成功",
    "code": 200
}


3、发送同步数据-初始化
http://127.0.0.1:9501/cloud/receiver/entity
POST
发送：
{
    "edgeId": "边缘服务器-01号",
    "entityType": "DeviceEntity",
    "timeStamp": "1b1df78266b9449c9d5705f821a2b4c2",
    "data": {
        "insert": [
            {
                "deviceType": "刘日威解码器",
                "createTime": 1652275102255,
                "commTime": 1652275102255,
                "channelType": "simulator",
                "updateTime": 1652275102255,
                "channelName": "channel-simulator",
                "id": 1,
                "deviceName": "阿威的单板"
            }
        ],
        "update": [
            {
                "deviceType": "BASS260ZJ",
                "createTime": 1652275102255,
                "commTime": 1652275102174,
                "channelType": "simulator",
                "updateTime": 1652275102174,
                "channelName": "channel-simulator",
                "id": 2,
                "deviceName": "浙江移动门禁设备"
            }
        ],
        "delete": ["浙江移动-丽水移动-丹霞山5号基站-4号电源设备:748","浙江移动-丽水移动-丹霞山5号基站-4号电源设备:742"
        ]
    }
}
返回：
{
    "msg": "操作成功",
    "code": 200
}

``` 

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

