# SITM-MIO Distributed Processing System

Sistema distribuido Master-Worker para análisis de rendimiento del Sistema Integrado de Transporte Metropolitano de Cali (SITM-MIO) con experimentos de escalabilidad y procesamiento de datagramas GPS.

## 🚀 Características Principales

- **🔧 Arquitectura Master-Worker** - Procesamiento distribuido con workers configurables
- **📊 Experimentos de Escalabilidad** - 100K, 1M, 10M datagramas con 1-3 workers
- **⚡ Workers Persistentes** - Conexiones reutilizadas para mayor eficiencia
- **🎯 Configuración Dinámica** - Workers esperados ajustables por experimento
- **📈 Análisis de Rendimiento** - Throughput, speedup, eficiencia automática
- **📊 Visualización de Datos** - Gráficos profesionales y reportes detallados
- **💾 Guardado Continuo** - Resultados en tiempo real en archivos CSV

## 📋 Requisitos

- **Java 17** o superior
- **Gradle 8.12+** (opcional, se incluye wrapper)
- **Python 3.7+** (para generación de gráficos)
- **4GB+ RAM** (para experimentos a gran escala)

## 🏗️ Estructura del Proyecto

```
FinalProjectIngesoft4-SITM-MIO/
├── 📁 app/src/main/java/org/mio/
│   ├── 📁 processing/                    # Sistema distribuido
│   │   ├── 📁 config/
│   │   │   ├── MasterConfig.java         # Configuración del Master
│   │   │   └── WorkerConfig.java         # Configuración del Worker
│   │   ├── 📁 master/
│   │   │   ├── MasterNodeService.java    # Lógica principal del Master
│   │   │   └── WorkerConnection.java    # Gestión de conexiones
│   │   ├── 📁 worker/
│   │   │   └── WorkerNodeService.java   # Lógica principal del Worker
│   │   └── 📁 experiments/
│   │       ├── ExperimentRunner.java     # Experimentos simulados
│   │       └── RealExperimentRunner.java # Experimentos reales
│   ├── 📁 model/                         # Modelos de datos
│   │   ├── Arc.java                      # Arco entre paradas
│   │   ├── ArcSpeed.java                 # Velocidad por arco
│   │   ├── Datagram.java                 # Datagrama GPS
│   │   ├── Stop.java                     # Parada de transporte
│   │   ├── Line.java                     # Línea de transporte
│   │   └── LineStop.java                 # Relación línea-parada
│   ├── 📁 graph/                         # Sistema de grafos
│   │   ├── Graph.java                    # Estructura del grafo
│   │   └── GraphBuilder.java             # Constructor desde CSV
│   ├── 📁 geo/                          # Sistema geográfico
│   │   └── CoordinateMapper.java         # Mapeo de coordenadas
│   ├── 📁 util/                         # Utilitarios
│   │   ├── CsvReader.java                # Lector genérico CSV
│   │   ├── FileCsvReader.java            # Lector de archivos CSV
│   │   └── BatchCsvReader.java           # Lector por lotes
│   ├── 📁 ui/                           # Interfaz gráfica (opcional)
│   │   ├── AppWindow.java                # Ventana principal
│   │   ├── MapPanel.java                 # Panel del mapa
│   │   ├── ControlPanel.java             # Controles de filtrado
│   │   └── InfoPanel.java                # Panel de información
│   └── 📁 visual/                       # Visualización
│       └── MapRenderer.java              # Renderizado de mapas
├── 📁 app/build/libs/                    # JARs ejecutables
│   ├── sitm-master-1.0.jar              # Master Node
│   ├── sitm-worker-1.0.jar              # Worker Node
│   ├── sitm-experiments-1.0.jar          # Experimentos simulados
│   └── sitm-real-experiments-1.0.jar     # Experimentos reales
├── 📁 dataset/                           # Datos de entrada
│   ├── datagrams4history.csv            # Datagramas GPS históricos
│   ├── stops-241.csv                    # Paradas del SITM-MIO
│   ├── linestops-241.csv                # Secuencia de paradas
│   └── lines-241.csv                    # Información de líneas
├── 📄 run-real-experiments.bat          # Script de experimentos
├── 📄 generate_charts.py                # Script de gráficos
└── 📄 README.md                         # Este archivo
```

## 🎯 Modos de Ejecución

### 🧪 Opción 1: Experimentos Simulados (Rápido)

Ideal para pruebas rápidas y desarrollo (~2 minutos):

```bash
# Ejecutar experimentos simulados
java -jar app/build/libs/sitm-experiments-1.0.jar

# Generar gráficos (después de que termine)
python generate_charts.py experiment_results_YYYYMMDD_HHMMSS.csv
```

### 🔬 Opción 2: Experimentos Reales (Datos Auténticos)

Para análisis completo con workers reales (6-13 horas):

```bash
# Usar script automatizado (recomendado)
run-real-experiments.bat

# O ejecutar manualmente
java -jar app/build/libs/sitm-real-experiments-1.0.jar

# Generar gráficos al final
python generate_charts.py real_experiment_results.csv
```

### 🖥️ Opción 3: Master-Worker Manual

Para control manual del sistema distribuido:

#### Paso 1: Iniciar el Master
```bash
# Terminal 1 - Master Node
java -jar app/build/libs/sitm-master-1.0.jar "dataset/datagrams4history.csv"
```

#### Paso 2: Iniciar Workers (terminales separadas)
```bash
# Terminal 2 - Worker 1
java -jar app/build/libs/sitm-worker-1.0.jar 1

# Terminal 3 - Worker 2
java -jar app/build/libs/sitm-worker-1.0.jar 2

# Terminal 4 - Worker 3
java -jar app/build/libs/sitm-worker-1.0.jar 3
```

### 🎮 Opción 4: Interfaz Gráfica

Para visualización interactiva del SITM-MIO:

```bash
# Usando Gradle
./gradlew run

# O directamente
java -jar app/build/libs/app.jar
```

## 📊 Configuraciones de Experimentos

### 🔧 Parámetros Actuales

```java
DATAGRAM_SIZES = {100_000, 1_000_000, 10_000_000}  // Escalas de datos
WORKER_COUNTS = {1, 2, 3}                           // Workers por experimento
BATCH_SIZES = {5_000, 10_000}                      // Tamaño de lotes
```

### 📈 18 Combinaciones Totales

#### 100K Datagramas (6 experimentos - 12-30 min)
- 100K + 1 Worker + 5K Lote
- 100K + 1 Worker + 10K Lote
- 100K + 2 Workers + 5K Lote
- 100K + 2 Workers + 10K Lote
- 100K + 3 Workers + 5K Lote
- 100K + 3 Workers + 10K Lote

#### 1M Datagramas (6 experimentos - 1-2 horas)
- 1M + 1 Worker + 5K Lote
- 1M + 1 Worker + 10K Lote
- [4 combinaciones más con 2-3 workers]

#### 10M Datagramas (6 experimentos - 6-12 horas)
- 10M + 1 Worker + 5K Lote
- 10M + 1 Worker + 10K Lote
- [4 combinaciones más con 2-3 workers]

## ⚡ Optimizaciones Activas

- **🔄 Workers Persistentes** - Sin reconexión entre experimentos
- **⏱️ Pausa Reducida** - 5 segundos vs 30 segundos anteriores
- **💾 Guardado Continuo** - CSV actualizado después de cada experimento
- **🎛️ Configuración Dinámica** - Workers esperados ajustables
- **📊 Monitoreo en Tiempo Real** - Progreso visible durante ejecución

## 📈 Salida del Sistema

### 📊 Archivos Generados

#### CSV de Resultados
- `experiment_results_YYYYMMDD_HHMMSS.csv` (simulados)
- `real_experiment_results.csv` (reales)

#### Gráficos Automáticos
- `scalability_workers.png` - Tiempo vs número de workers
- `batch_size_performance.png` - Rendimiento vs tamaño de lote
- `throughput_analysis.png` - Throughput por escala
- `performance_heatmap.png` - Mapa de calor de rendimiento
- `experiment_report.md` - Informe completo con análisis

### 📋 Métricas Calculadas

- **Tiempo Total** - Duración completa del experimento
- **Tiempo de Procesamiento** - Solo cómputo
- **Throughput** - Datagramas/segundo
- **Speedup** - Ganancia con workers adicionales
- **Eficiencia** - Speedup / número de workers
- **Lotes Necesarios** - Eficiencia de procesamiento

## 🛠️ Construcción y Desarrollo

### Compilar el Proyecto

```bash
# Compilar todos los JARs
./gradlew buildAllJars

# O individualmente
./gradlew masterJar      # sitm-master-1.0.jar
./gradlew workerJar      # sitm-worker-1.0.jar
./gradlew experimentsJar # sitm-experiments-1.0.jar
./gradlew realExperimentsJar # sitm-real-experiments-1.0.jar
```

### Limpiar y Reconstruir

```bash
# Limpiar todo
./gradlew clean

# Limpiar y compilar
./gradlew clean buildAllJars
```

## 🔧 Configuración Avanzada

### Personalizar Experimentos

Edita `RealExperimentRunner.java` para modificar:

```java
// Cambiar escalas de datagramas
private static final int[] DATAGRAM_SIZES = {50_000, 500_000, 5_000_000};

// Cambiar números de workers
private static final int[] WORKER_COUNTS = {1, 2, 4, 8};

// Cambiar tamaños de lote
private static final int[] BATCH_SIZES = {1_000, 5_000, 20_000};
```

### Configuración de Red

Para workers en diferentes máquinas:

```bash
# Worker remoto
java -jar sitm-worker-1.0.jar 1 192.168.1.100 8080

# Master con IP específica
java -jar sitm-master-1.0.jar "dataset/datagrams.csv" 0.0.0.0 8080
```

## 📊 Ejemplo de Ejecución

### Salida de Experimentos Reales

```
=== SISTEMA DE EXPERIMENTOS REALES SITM-MIO ===
Configuraciones de prueba (PROCESAMIENTO REAL):
- Datagramas: [100000, 1000000, 10000000]
- Workers: [1, 2, 3]
- Lotes: [5000, 10000]

ADVERTENCIA: ESTOS EXPERIMENTOS USAN WORKERS REALES - TOMARAN TIEMPO REAL
   100K datagramas: ~2-5 minutos por configuración
   1M datagramas: ~10-20 minutos por configuración
   10M datagramas: ~60-120 minutos por configuración
   Tiempo total estimado: 6-13 horas

=== INICIANDO EXPERIMENTOS REALES ===
=== INICIANDO WORKERS PERSISTENTES ===
Iniciando 3 workers para todos los experimentos...
✓ Todos los workers persistentes iniciados

--- EXPERIMENTO 1/18 ---
Datagramas: 100000
Workers: 1
Tamaño lote: 5000
INICIANDO PROCESAMIENTO REAL...
  ✓ Procesamiento REAL completado en 89 segundos

--- EXPERIMENTO 2/18 ---
Datagramas: 100000
Workers: 1
Tamaño lote: 10000
INICIANDO PROCESAMIENTO REAL...
  ✓ Procesamiento REAL completado en 82 segundos

[continúa hasta el experimento 18/18...]
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **Error de versión de Java**
   ```bash
   java -version  # Debe ser Java 17+
   ```

2. **Workers no se conectan**
   - Verificar que el Master esté iniciado primero
   - Revisar firewall en el puerto 8080

3. **OutOfMemory en experimentos grandes**
   - Aumentar memoria JVM: `-Xmx4g`
   - Reducir tamaño de lote en configuración

4. **Archivos CSV no encontrados**
   - Verificar ruta en `MasterConfig.java`
   - Usar ruta absoluta si es necesario

### Verificación del Sistema

```bash
# Verificar JARs generados
ls -la app/build/libs/

# Probar Master solo
java -jar app/build/libs/sitm-master-1.0.jar --help

# Probar Worker solo
java -jar app/build/libs/sitm-worker-1.0.jar --help
```

## 📊 Métricas de Rendimiento

### Rendimiento Típico

- **100K datagramas:** 2-5 minutos por configuración
- **1M datagramas:** 10-20 minutos por configuración
- **10M datagramas:** 60-120 minutos por configuración
- **Throughput:** 1,000-5,000 datagramas/segundo
- **Speedup:** 1.5-2.5x con 3 workers
- **Eficiencia:** 50-85% con workers adicionales

### Optimizaciones Implementadas

- **25% más rápido** con workers persistentes
- **Sin reconexiones** entre experimentos
- **Guardado continuo** para recuperación
- **Procesamiento por lotes** para evitar OOM

## 📚 Referencias y Documentación

- **Master-Worker Pattern** - Arquitectura distribuida clásica
- **Java TCP Sockets** - Comunicación red eficiente
- **CSV Processing** - Manejo de grandes volúmenes de datos
- **Performance Analysis** - Métricas de escalabilidad

## 📄 Licencia

Proyecto desarrollado para fines académicos - Sistema de Transporte SITM-MIO.

---

**🚀 ¡Listo para usar!** Elige el modo de ejecución que mejor se adapte a tus necesidades:

- **🧪 Desarrollo rápido:** Experimentos simulados (2 min)
- **🔬 Análisis completo:** Experimentos reales (6-13 horas)
- **🖥️ Control manual:** Master-Worker interactivo
- **📊 Visualización:** Interfaz gráfica del SITM-MIO
