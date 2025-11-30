# SITM-MIO Cali Visualizador

Proyecto para visualizar las rutas y paradas del Sistema Integrado de Transporte Metropolitano de Cali (SITM-MIO).

## Requisitos

- Java 17 o superior
- Gradle 8.12+

## Ejecución del Proyecto

### Opción 1: Usando Gradle Wrapper (Recomendado)

```bash
# Ejecutar el proyecto (modo consola + interfaz gráfica)
./gradlew run

# Si estás en Windows
gradlew.bat run
```

### Opción 2: Usando Gradle instalado localmente

```bash
# Ejecutar el proyecto
gradle run

# Compilar el proyecto
gradle build

# Limpiar y compilar
gradle clean build
```

### Opción 3: Compilación manual (si tienes Java 17 instalado)

```bash
# Compilar todas las clases
javac -cp "app/src/main/java" app/src/main/java/org/mio/*.java app/src/main/java/org/mio/*/*.java

# Ejecutar la aplicación
java -cp "app/src/main/java" org.mio.Main
```

## Funcionalidades

El programa ejecuta dos modos automáticamente:

1. **Modo Consola**: Muestra todos los arcos del grafo generados, ordenados por ruta y secuencia
2. **Modo Gráfico**: Abre una ventana interactiva con:
   - Mapa visual de las rutas del SITM-MIO
   - Panel de control para filtrar por línea y orientación
   - Panel de información con detalles de las paradas

## Estructura del Proyecto

```
src/main/java/org/mio/
├── Main.java              # Clase principal que inicia la aplicación
├── graph/
│   ├── Graph.java         # Estructura del grafo
│   └── GraphBuilder.java  # Constructor del grafo desde CSV
├── model/
│   ├── Arc.java           # Arco entre paradas
│   ├── Stop.java          # Parada de transporte
│   └── LineStop.java      # Relación línea-parada
├── ui/
│   ├── AppWindow.java     # Ventana principal
│   ├── MapPanel.java      # Panel del mapa
│   ├── ControlPanel.java  # Controles de filtrado
│   └── InfoPanel.java     # Panel de información
└── util/
    └── CsvReader.java     # Lector de archivos CSV
```

## Datos

Los archivos CSV con los datos del SITM-MIO deben estar en:
- `app/src/main/resources/stops-241.csv` - Paradas del sistema
- `app/src/main/resources/linestops-241.csv` - Secuencia de paradas por línea
- `app/src/main/resources/lines-241.csv` - Información de las líneas

## Configuración

La configuración del proyecto está en:
- `app/build.gradle` - Configuración de Gradle, dependencias y clase principal
- Java 17 configurado en el toolchain
- Clase principal: `org.mio.Main`

## Troubleshooting

### Problemas comunes:

1. **Error de versión de Java**: Asegúrate de tener Java 17 instalado
2. **Error de Gradle**: Si el wrapper no funciona, usa `gradle run` directamente
3. **Archivos CSV no encontrados**: Verifica que los archivos CSV estén en la carpeta `resources`
4. **Error de clase principal**: El build.gradle está configurado para `org.mio.Main`

### Verificar instalación:

```bash
# Verificar versión de Java
java -version

# Verificar versión de Gradle
gradle --version
```

## Licencia

Proyecto desarrollado para fines académicos.
