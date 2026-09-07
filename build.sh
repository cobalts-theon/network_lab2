#!/bin/bash
echo "Đang biên dịch ứng dụng Chat P2P Hybrid..."
mkdir -p bin
javac -encoding UTF-8 -d bin src/model/*.java src/server/*.java src/client/*.java
if [ $? -eq 0 ]; then
    echo "[OK] Biên dịch thành công!"
else
    echo "[ERROR] Biên dịch thất bại! Vui lòng kiểm tra JDK."
fi
