#!/bin/bash
# 检查是否提供了参数
if [ -z "$1" ]; then
    echo -e "\033[31mError: 缺少文件路径参数.\033[0m"
    exit 1
fi
# 判断参数是文件还是目录
if [ -f "$1" ]; then
    echo -e "\033[35m$1\033[0m is a file."
else
    echo -e "\033[31m$1\033[0m 不是文件."
    exit 1
fi
# 当前脚本运行进程号
script_pid=$$
# 获取文件名
file_name=$(basename "$1")
echo -e "文件名: ""\033[35m$file_name\033[0m"
# 获取文件路径
directory=$(dirname "$1")
echo -e "文件路径: ""\033[33m$directory\033[0m"
# shellcheck disable=SC2164
cd "$directory"
ams stop
# 监听升级jar包进程是否关闭
pidList=$(pgrep -f "$file_name")
echo -e "搜索到的文件进程号: ""\033[32m$pidList\033[0m"

for pid in $pidList; do
  if [ "$pid" -ne "$script_pid" ]; then
    echo -e "进程 $pid 不是当前脚本进程."
    if ps -p "$pid" > /dev/null; then
      echo -e "有效的 PID: \033[32m$pid\033[0m"
      pid_directory=$(pwdx "$pid")
      echo -e "进程号对应文件路径: ""${pid_directory#*:}"
      if [ "${pid_directory#*:}" == "$directory" ] ; then
         while true
          do
            if ps -p "$pid" > /dev/null; then
              echo -e "进程号 \033[32m$pid\033[0m 未关闭！"
            else
              echo -e "进程号 \033[32m$pid\033[0m 已关闭！"
              break
            fi
          done
      fi
    else
      echo -e "无效的 PID: \033[31m$pid\033[0m"
    fi
  else
    echo -e "进程 $pid 是当前脚本进程."
  fi
done
echo -e "\033[31m进程不存在！\033[0m"
# 获取最新的备份文件
latest_file=$(find "$directory" -type f -name "${file_name}20*" -exec ls -t {} + | head -n 1)

latest_file_name=$(basename "$latest_file")
# 获取文件名
echo -e "本次升级前最新的备份文件名: ""\033[35m$latest_file_name\033[0m"

rm -rf "$1"

mv "$latest_file_name" "$file_name"

ams start