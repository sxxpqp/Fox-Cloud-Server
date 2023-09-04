#==================================================安装JDK===================================================#

# 更新软件
apt-get update    
# 安装vsftpd
apt-get install openjdk-11-jdk -y
#查看安装状态
java -version


#==================================================安装JDK===================================================#

#==================================================安装FTP===================================================#

# 更新软件
apt-get update    
# 安装vsftpd
apt-get install vsftpd -y
 
# 设置开机启动并启动ftp服务
systemctl enable vsftpd
systemctl start vsftpd

#查看其运行状态
systemctl  status vsftpd
#重启服务
systemctl  restart vsftpd

#==================================================安装FTP===================================================#



#================================================安装mongodb单机版本=========================================#

#参考：https://blog.csdn.net/Dwj1212/article/details/123451532

#更新源
apt-get update  
#安装
apt-get install mongodb -y

#进入mongo，创建root账号,密码：12345678
mongo
> db.createUser({user: "root",pwd: "12345678", roles: [ { role: "root", db: "admin" } ]});
> db.auth("root", "12345678");
> exit

#配置外部访问
vim /etc/mongodb.conf
#将bindIp：127.0.0.1 修改为 bindIp：0.0.0.0 位置在11行左右

#重启服务
service mongodb restart

#================================================安装mongodb单机版本=========================================#


#=================================================安装redis==================================================#
#参考：https://gu-han-zhe.blog.csdn.net/article/details/117538180

#更新
apt-get update

#安装redis
apt install redis-server -y

#检查安装结果
systemctl status redis-server

#修改配置文件
vim /etc/redis/redis.conf
#1.注释掉 bind 127.0.0.1 ::1   位置在69行左右
#2.修改protected-mode为no      位置在88行左右
#3.修改requirepass为12345678   位置在507行左右

#重启redis
systemctl restart redis-server
#=================================================安装redis==================================================#

#=================================================安装samba==================================================#
#参考：https://blog.csdn.net/dslobo/article/details/108175737

#更新
apt-get update

#安装redis
apt-get install samba -y

#修改配置文件
vim /etc/samba/smb.conf  
#增加下面的配置
#------------------#
[share]
# 设置共享目录
path = /
# 设置访问用户 
valid users = root
# 设置读写权限
writable = yes  
#------------------#

#创建samba用户
smbpasswd -a root

#重启samba
service smbd restart
#=================================================安装redis==================================================#



#=================================================ubuntu磁盘扩容=============================================#
#ubuntu22安装后，一半磁盘空间未使用的解决
#参考文章：https://blog.csdn.net/weixin_37830416/article/details/120792705
#参考文章：https://blog.csdn.net/weixin_43302340/article/details/120341241

#显示存在的卷组，Alloc PE是已经分配的磁盘空间，Free PE是尚未分配的磁盘空间
vgdisplay

#显查看磁盘目录：可以看到正在使用的磁盘/dev/mapper/ubuntu--vg-ubuntu--lv
df -h

#全部空间都给这个盘
lvextend -l +100%FREE /dev/mapper/ubuntu--vg-ubuntu--lv

#重新计算磁盘大小
resize2fs /dev/mapper/ubuntu--vg-ubuntu--lv

#再次显查看磁盘目录，可以看到/dev/mapper/ubuntu--vg-ubuntu--lv已经把那部分磁盘空间利用上了
df -h

#=================================================ubuntu磁盘扩容=============================================#

#=============================================ubuntu虚拟内存扩容=============================================#
#参考文章：https://www.ngui.cc/el/743554.html?action=onClick

#察看当前swap分区大小
free -h

#查看swap分区挂载位置，默认是/swap.img
cat /proc/swaps

#停止原来的交换分区
#注意：这要等一段时间
swapoff /swap.img

#删除原来的分区文件
rm /swap.img

#重新建立分区文件swapfile：我的物理内存是4G，所以这里准备新建的swap分区是10G，bs x count = 1024 × 10000000 = 10G
#注意：这要等一段时间，可能要十分钟，需要耐心等待
dd if=/dev/zero of=/swap.img bs=1024 count=10000000

#启用
chmod 600 /swap.img
mkswap -f /swap.img
swapon /swap.img

#检查结果
free -h
cat /proc/swaps

#=============================================ubuntu虚拟内存扩容=============================================#


#===============================================ubuntu定时任务===============================================#
#检查是否安装了cron
ps -aux|grep cron

#安装cron
#apt-get install cron

#1、进入编辑模式
crontab -e

#2、进入编辑：按ctrl+O，此时会出现File Name To Write: /temp/crontab.xxxxxx的提示，然后按回车键


#3、输入下面内容
#3.1 每天08:00执行一次该命令
0 8 * * * echo 3 > /proc/sys/vm/drop_caches
#3.2 定时同步时钟源
0 8 * * * ntpdate cn.pool.ntp.org

#4、保存编辑：按ctrl+X，此时会提示是否保存，然后选中Y

#5、检查编辑的定时任务，是否包含刚才的新增内容
crontab -l

#===============================================ubuntu定时任务===============================================#


#===============================================ubuntu设置时区===============================================#
#查看当前时区
date -R

#将时区设置为上海时区
timedatectl   set-timezone   Asia/Shanghai

#修改时间为24小时制
vim /etc/default/locale
#增加下面一行
LC_TIME=en_DK.UTF-8

#重启计算机
reboot

#===============================================ubuntu设置时区===============================================#