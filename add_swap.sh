#!/bin/bash
set -e

echo "=== 当前内存状态 ==="
free -h
echo ""

SWAPFILE="/swapfile"
SIZE="2G"

if swapon --show | grep -q "$SWAPFILE"; then
    echo "[已跳过] swap 已存在并启用"
    free -h
    exit 0
fi

if [ -f "$SWAPFILE" ]; then
    echo "[警告] $SWAPFILE 已存在但未启用，尝试启用..."
    sudo swapon "$SWAPFILE"
    echo "[完成] swap 已启用"
    free -h
    exit 0
fi

echo "[1/4] 创建 ${SIZE} swap 文件..."
sudo fallocate -l "$SIZE" "$SWAPFILE"

echo "[2/4] 设置权限..."
sudo chmod 600 "$SWAPFILE"

echo "[3/4] 格式化 swap..."
sudo mkswap "$SWAPFILE"

echo "[4/4] 启用 swap..."
sudo swapon "$SWAPFILE"

# 写入 fstab 实现开机自动挂载
if ! grep -q "$SWAPFILE" /etc/fstab; then
    echo "$SWAPFILE none swap sw 0 0" | sudo tee -a /etc/fstab
    echo "[额外] 已添加到 /etc/fstab，开机自动挂载"
fi

echo ""
echo "=== 完成！当前内存状态 ==="
free -h
echo ""
echo "swap 已就绪 ✓"
