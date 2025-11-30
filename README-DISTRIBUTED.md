# SITM-MIO Master-Worker Distributed System

##  Arquitectura Master-Worker

Sistema distribuido para calcular velocidades promedio por arco del SITM-MIO usando datagramas GPS históricos.

## Estructura del Proyecto

```
config/
├── MasterConfig.java    ← Configuración y mensajes del Master
└── WorkerConfig.java    ← Configuración y mensajes del Worker

master/
├── MasterNodeService.java ← Lógica principal del Master
└── WorkerConnection.java ← Conexión con workers

worker/
└── WorkerNodeService.java ← Lógica principal del Worker

build/libs/
├── sitm-master-1.0.jar  ← JAR ejecutable del Master
└── sitm-worker-1.0.jar  ← JAR ejecutable del Worker
```

## Ejecución del Sistema

### Opción 1: Usando Scripts (Recomendado)

#### 1. Iniciar el Master
```bash
run-master.bat
```
*El script usa automáticamente la ruta correcta del dataset*

#### 2. Iniciar Workers (en terminales separadas)
```bash
run-worker.bat 1
run-worker.bat 2
run-worker.bat 3
```

### Opción 2: Usando JARs directamente

#### 1. Iniciar el Master (con ruta explícita)
```bash
java -jar app/build/libs/sitm-master-1.0.jar "C:\Users\default.LAPTOP-M81T5L1M\Desktop\ICESI2025II\ingesoft 4\dataset\datagrams4history.csv"
```

#### 2. Iniciar Workers (en terminales separadas)
```bash
java -jar app/build/libs/sitm-worker-1.0.jar 1
java -jar app/build/libs/sitm-worker-1.0.jar 2
java -jar app/build/libs/sitm-worker-1.0.jar 3
```

### Opción 3: Desarrollo con Gradle
```bash
./gradlew run --args="org.mio.processing.master.MasterNodeService"
./gradlew run --args="org.mio.processing.worker.WorkerNodeService 1"
./gradlew run --args="org.mio.processing.worker.WorkerNodeService 2"
./gradlew run --args="org.mio.processing.worker.WorkerNodeService 3"
```

## Flujo de Ejecución

1. **Master inicia** servidor TCP en puerto 8080
2. **Workers se conectan** al Master
3. **Master espera** a todos los workers (3 por defecto)
4. **Usuario presiona Enter** para iniciar procesamiento
5. **Master distribuye** datagramas GPS a workers
6. **Workers procesan** y calculan velocidades
7. **Workers envían** resultados al Master
8. **Master agrega** y muestra resultados finales

## Qué verás en consola

### Master Console:
```
==========================================
===     MASTER NODE - SITM-MIO        ===
==========================================
Rol: COORDINADOR DISTRIBUIDO
Puerto: 8080
Estado: INICIANDO SERVIDOR TCP...
Master: ✓ SERVIDOR TCP INICIADO
Master: Workers conectados: 0/3 (esperando...)
Master: Worker 1 conectado desde localhost:8081
Master: Worker 2 conectado desde localhost:8082
Master: Worker 3 conectado desde localhost:8083
Master: ✓ TODOS LOS WORKERS CONECTADOS
Master: Presiona Enter para iniciar el procesamiento distribuido...

==========================================
===  INICIANDO PROCESAMIENTO DISTRIBUIDO ===
==========================================
Master: Workers activos: 3
Master: Archivo de datos: datagrams4history
Master: Arcos en grafo: 3452
Master: Iniciando carga y distribución de datagramas...
Master: Cargados y distribuidos 10000 datagramas
Master: Progreso distribución: 25000/50000 (50.0%)
Master: ✓ DISTRIBUCIÓN FINALIZADA
Master: Esperando resultados de workers...
Master: Worker 1: 1500 arcos procesados
Master: Worker 2: 1450 arcos procesados
Master: Worker 3: 1600 arcos procesados
Master: ✓ PROCESAMIENTO DISTRIBUIDO FINALIZADO

==========================================
===     RESULTADOS DISTRIBUIDOS        ===
==========================================
Master: Workers activos: 3 (TCP distribuido)
Master: Datagramas procesados: 50000
Master: Tiempo total: 12.45 segundos
Master: Rendimiento: 4016 datagramas/segundo
Master: Arcos con velocidad calculada: 2847
```

### Worker Console:
```
=== WORKER 1 INICIADO ===
Rol: PROCESADOR DE DATAGRAMAS
Worker 1: Conectando al Master en localhost:8080...
Worker 1: Grafo cargado - 3452 arcos
Worker 1: Listo para procesar datagramas
Estado: ESPERANDO CONEXIÓN CON MASTER...
Worker 1: ✓ CONECTADO AL MASTER
Worker 1: Esperando datagramas para procesar...
Worker 1: Procesados 1000 datagramas | Arcos con velocidad: 150 | Velocidad promedio: 25.34 km/h
Worker 1: Procesados 2000 datagramas | Arcos con velocidad: 300 | Velocidad promedio: 26.12 km/h
Worker 1: ✓ SEÑAL DE PARADA RECIBIDA
Worker 1: ✓ PROCESAMIENTO FINALIZADO
Worker 1: Total datagramas procesados: 15000
Worker 1: Total arcos con velocidad: 1200
Worker 1: Velocidad promedio general: 26.45 km/h
Worker 1: ✓ RESULTADOS ENVIADOS AL MASTER
=== WORKER 1 - FINALIZADO ===
```

## Configuración

### Master Config (MasterConfig.java)
- Puerto: 8080
- Workers esperados: 3
- Archivo de datagramas: configurable
- Intervalos de reporte: configurables

### Worker Config (WorkerConfig.java)
- IP del Master: localhost (configurable)
- Puerto del Master: 8080
- Umbrales de velocidad: 0-100 km/h
- Intervalos de reporte: configurables

## Resultados

El sistema calcula y muestra:
- Velocidades promedio por arco del SITM-MIO
- Estadísticas de procesamiento
- Top 20 arcos con mayor velocidad
- Métricas de rendimiento

## Despliegue en Red

Para workers en otros computadores:

1. Copiar `sitm-worker-1.0.jar` a cada máquina
2. Ejecutar con IP del Master:
```bash
java -jar sitm-worker-1.0.jar 1 192.168.1.100 8080
```

## Construcción

```bash
./gradlew buildAllJars
```

Genera:
- `sitm-master-1.0.jar` - JAR ejecutable del Master
- `sitm-worker-1.0.jar` - JAR ejecutable del Worker
