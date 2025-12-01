@echo off
chcp 65001 >nul
echo ==========================================
echo === EXPERIMENTOS REALES SITM-MIO ===
echo ==========================================
echo.
echo EXPERIMENTOS REALES OPTIMIZADOS
echo    Workers persistentes - Pausa reducida - Guardado continuo
echo    Configuración dinámica de workers
echo    Tres escalas: 100K, 1M, 10M datagramas
echo.
echo Tiempos estimados de procesamiento:
echo - 100K datagramas: 2-5 minutos por configuración
echo - 1M datagramas: 10-20 minutos por configuración
echo - 10M datagramas: 60-120 minutos por configuración
echo.
echo Configuraciones a probar:
echo - Datagramas: 100K, 1M, 10M
echo - Workers: 1, 2, 3 (configurados dinámicamente por experimento)
echo - Lotes: 5K, 10K
echo.
echo Total experimentos: 18
echo Tiempo total estimado: 6-13 horas
echo.
echo OPTIMIZACIONES ACTIVAS:
echo    - Workers persistentes (sin reconexión)
echo    - Pausa reducida: 5 segundos (vs 30 antes)
echo    - Guardado automático después de cada experimento
echo    - CSV único: real_experiment_results.csv
echo    - Configuración dinámica: workers esperados por experimento
echo.
echo ADVERTENCIA: Experimentos a gran escala
echo    Asegúrate de tener suficiente tiempo y recursos
echo    Los resultados se guardarán continuamente
echo.
pause

echo.
echo === INICIANDO EXPERIMENTOS REALES COMPLETOS ===
echo.

java -jar app\build\libs\sitm-real-experiments-1.0.jar

echo.
echo === EXPERIMENTOS REALES COMPLETADOS ===
echo.
echo Archivos generados:
echo - real_experiment_results.csv (datos completos)
echo - Informe con análisis de rendimiento real
echo.
echo Para generar gráficos:
echo python generate_charts.py real_experiment_results.csv
echo.
pause
