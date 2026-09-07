#!/bin/bash
if [ ! -d "bin" ]; then
    ./build.sh
fi
echo "Đang khởi động P2P Server..."
java -cp bin server.server
