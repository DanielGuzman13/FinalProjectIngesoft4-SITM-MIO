@echo off
echo ==========================================
echo ===     MASTER NODE - SITM-MIO        ===
echo ==========================================
echo.
echo Iniciando Master Node...
echo.
echo Usando archivo de datagramas:
echo C:\Users\default.LAPTOP-M81T5L1M\Desktop\ICESI2025II\ingesoft 4\dataset\datagrams4history.csv
echo.

java -jar app\build\libs\sitm-master-1.0.jar C:\Users\default.LAPTOP-M81T5L1M\Desktop\ICESI2025II\ingesoft 4\dataset\datagrams4history.csv

pause
