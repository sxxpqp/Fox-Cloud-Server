#!/bin/bash
APP_HOME=/opt/fox-cloud-server
IP_ADDR=172.16.50.34

#切换当前目录
cd $APP_HOME

#启动命令
nohup \
java -jar \
-Dspring.profiles.active=prod \
$APP_HOME/bin/\
fox-cloud-server-mqtt-broker-1.0.0.jar \
--spring.redis.host=$IP_ADDR \
--spring.redis.port=7379 \
--spring.redis.password=12345678 \
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/$APP_HOME/logs/error.hprof \
 >$APP_HOME/logs/start_mqtt-broker.out 2>&1 & \
 
 
 




