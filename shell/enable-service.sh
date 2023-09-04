#!/bin/bash
APP_HOME=/opt/fox-cloud-server

#切换当前目录
cd $APP_HOME

#开启启动脚本

#1.将minio.service复制到系统目录/etc/systemd/system/
/bin/cp -rf $APP_HOME/shell/fox-cloud.service /etc/systemd/system/fox-cloud.service

#2.将文件从dos格式转换为linux格式
dos2unix  /etc/systemd/system/fox-cloud.service  >/dev/null 2>&1
dos2unix  $APP_HOME/shell/startup.sh  >/dev/null 2>&1
dos2unix  $APP_HOME/shell/shutdown.sh  >/dev/null 2>&1
#3.重新装载/etc/systemd/system/fox-cloud.service到/lib/systemd/system/fox-cloud.service
systemctl daemon-reload
#4.新增执行权限
sudo chmod +x $APP_HOME/shell/startup.sh
sudo chmod +x $APP_HOME/shell/shutdown.sh
#5.停止服务脚本
systemctl stop fox-cloud.service
#6.启动服务脚本
systemctl start fox-cloud.service
#7.将服务脚本配置问开机启动
systemctl enable fox-cloud.service
#8.显示minio信息
ps -aux|grep $APP_HOME/bin/fox-cloud

