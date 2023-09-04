#!/bin/bash

app_shell=aggregator-service.sh
app_name=fox-cloud-server-aggregator-1.0.0.jar

#系统参数
app_home=$1

#redis参数
app_param_redis_host=$2
app_param_redis_port=$3
app_param_redis_password=$4
#mysql参数
app_param_mongodb_host=$5
app_param_mongodb_port=$6
app_param_mongodb_username=$7
app_param_mongodb_password=$8
#nacos
nacos_server=172.27.67.186:8848


#切换当前目录
cd $app_home

#杀死进程
ids=`ps -ef | grep $app_home/bin/$app_name | grep -v 'grep' | awk '{print $2}'`
for id in $ids
do
    kill -9 $id
done


#启动命令
nohup \
java -jar \
$app_home/bin/$app_name \
--app_shell=$app_shell \
-Dspring.profiles.active=prod \
--spring.cloud.nacos.discovery.server-addr=$nacos_server --spring.cloud.nacos.config.server-addr=$nacos_server \
--spring.redis.host=$app_param_redis_host --spring.redis.port=$app_param_redis_port --spring.redis.password=$app_param_redis_password \
--spring.data.mongodb.uri=mongodb://$app_param_mongodb_username:$app_param_mongodb_password@$app_param_mongodb_host:$app_param_mongodb_port/fox-cloud-server-aggregator?connect=replicaSet\&replSetName=configsvr\&authSource=fox-cloud-server-aggregator\&safe=true\&slaveOk=true \
 >$app_home/logs/start_$app_name.out 2>&1 & \
