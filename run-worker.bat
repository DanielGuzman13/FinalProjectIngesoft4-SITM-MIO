@echo off
set WORKER_ID=%1
if "%WORKER_ID%"=="" set WORKER_ID=1
@echo off
chcp 65001 >nul
echo ==========================================
echo ===     WORKER NODE %WORKER_ID% - SITM-MIO     ===
echo ==========================================
echo.
echo === INICIANDO WORKER %WORKER_ID% ===
echo.
java -jar app\build\libs\sitm-worker-1.0.jar %WORKER_ID%
echo.
echo === WORKER %WORKER_ID% FINALIZADO ===
pause
