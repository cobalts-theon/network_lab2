:; ./run_client.sh; exit $?
@echo off
chcp 65001 >nul
if not exist bin (
    call build.bat
)
java -cp bin client.client
