@echo off
chcp 65001 >nul
echo ==========================================
echo ===     MASTER NODE - SITM-MIO        ===
echo ==========================================
echo.

if "%~1"=="" (
    echo USO: run-master.bat [archivo_csv] [datagramas]
    echo.
    echo EJEMPLOS:
    echo   run-master.bat datagrams.csv 1000
    echo   run-master.bat datagrams.csv 10000
    echo   run-master.bat datagrams.csv 100000
    echo   run-master.bat datagrams.csv 1000000
    echo.
    echo USANDO CONFIGURACIÓN POR DEFECTO:
    echo   Archivo: datagrams4history.csv
    echo   Datagramas: 1000000
    echo.
    java -jar app\build\libs\sitm-master-1.0.jar "C:\Users\default.LAPTOP-M81T5L1M\Desktop\ICESI2025II\ingesoft 4\dataset\datagrams4history.csv" 1000000
) else (
    echo CONFIGURACIÓN:
    echo   Archivo: %1
    echo   Datagramas: %2
    echo.
    java -jar app\build\libs\sitm-master-1.0.jar "%~1" %~2
)

echo.
echo === MASTER NODE FINALIZADO ===
pause
