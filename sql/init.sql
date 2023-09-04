#执行命令：mysql -u root -p12345678
#> source /opt/ruoyi-cloud/sql/init.sql;


#切换数据库
use mysql;

#查看用户权限
select host, user, plugin from user;

#创建数据库
drop database if exists `ry-cloud`;
create database `ry-cloud`;
use ry-cloud;
source /opt/ruoyi-cloud/sql/ry_20230223.sql;
source /opt/ruoyi-cloud/sql/quartz.sql;


#创建本地连接用户:'ry-cloud'@'localhost'
use mysql;
drop user if exists 'ry-cloud'@'localhost', 'ry-cloud'@'localhost';
create user 'ry-cloud'@'localhost' identified by '12345678';
grant all privileges on `ry-cloud`.* to `ry-cloud`@'localhost';
flush privileges;

#创建远程连接用户:'ry-cloud'@'%'
use mysql;
drop user if exists 'ry-cloud'@'%', 'ry-cloud'@'%';
create user 'ry-cloud'@'%' identified by '12345678';
grant all privileges on `ry-cloud`.* to 'ry-cloud'@'%';
flush privileges;

#切换数据库
use mysql;

#查看用户权限
select host, user, plugin from user;


drop database if exists `ry-config`;
use ry-config;
source /opt/ruoyi-cloud/sql/ry_config_20220929.sql;

#创建本地连接用户:'ry-config'@'localhost'
use mysql;
drop user if exists 'ry-config'@'localhost';
create user 'ry-config'@'localhost' identified by '12345678';
grant all privileges on `ry-config`.* to 'ry-config'@'localhost';
flush privileges;

#创建远程连接用户:'ry-config'@'%'
use mysql;
drop user if exists 'ry-config'@'%';
create user 'ry-config'@'%' identified by '12345678';
grant all privileges on `ry-config`.* to 'ry-config'@'%';
flush privileges;




