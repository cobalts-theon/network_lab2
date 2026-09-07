#!/bin/bash
if [ ! -d "bin" ]; then
    ./build.sh
fi
echo "Đang khởi động P2P Client..."
java -cp bin client.client
