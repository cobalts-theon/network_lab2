:; ./build.sh; exit $?
@echo off
chcp 65001 >nul
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src/model/*.java src/server/*.java src/client/*.java
if %errorlevel% equ 0 (
    echo [OK] Bien dich thanh cong!
) else (
    echo [ERROR] Bien dich that bai!
)
pause
