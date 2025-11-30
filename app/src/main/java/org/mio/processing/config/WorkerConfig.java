package org.mio.processing.config;

public class WorkerConfig {
    // Configuración del Worker Node
    public static final String DEFAULT_MASTER_IP = "localhost";
    public static final int DEFAULT_MASTER_PORT = 8080;
    
    // Mensajes del Worker
    public static final String WORKER_BANNER = "=== WORKER %d INICIADO ===";
    public static final String WORKER_ROLE = "Rol: PROCESADOR DE DATAGRAMAS";
    public static final String CONNECTING_TO_MASTER = "Worker %d: Conectando al Master en %s:%d...";
    public static final String LOADING_GRAPH = "Worker %d: Cargando grafo para procesamiento...";
    public static final String GRAPH_LOADED = "Worker %d: Grafo cargado - %d arcos";
    public static final String READY_TO_PROCESS = "Worker %d: Listo para procesar datagramas";
    public static final String WAITING_FOR_MASTER = "Estado: ESPERANDO CONEXIÓN CON MASTER...";
    public static final String CONNECTED_TO_MASTER = "Worker %d: ✓ CONECTADO AL MASTER";
    public static final String WAITING_FOR_DATAGRAMS = "Worker %d: Esperando datagramas para procesar...";
    public static final String PROCESSING_STARTED = "Worker %d: Estado: PROCESANDO DATAGRAMAS...";
    
    // Mensajes de procesamiento
    public static final String PROCESSING_PROGRESS = 
        "Worker %d: Procesados %d datagramas | Arcos con velocidad: %d | Velocidad promedio: %.2f km/h";
    
    public static final String STOP_SIGNAL_RECEIVED = "Worker %d: ✓ SEÑAL DE PARADA RECIBIDA";
    public static final String PROCESSING_FINISHED = "Worker %d: ✓ PROCESAMIENTO FINALIZADO";
    public static final String FINAL_STATS = 
        "Worker %d: Total datagramas procesados: %d\n" +
        "Worker %d: Total arcos con velocidad: %d\n" +
        "Worker %d: Velocidad promedio general: %.2f km/h";
    
    public static final String SENDING_RESULTS = "Worker %d: Enviando resultados al Master...";
    public static final String RESULTS_SENT = "Worker %d: ✓ RESULTADOS ENVIADOS AL MASTER";
    public static final String WORKER_FINISHED = "=== WORKER %d - FINALIZADO ===";
    
    // Configuración de procesamiento
    public static final int PROGRESS_REPORT_INTERVAL = 1000; // cada 1,000 datagramas
    public static final int PROGRESS_TIME_INTERVAL_MS = 5000; // cada 5 segundos
    public static final int MAX_HISTORY_SIZE = 100; // máximo de datagramas por bus
    public static final double MIN_SPEED_THRESHOLD = 0.0; // km/h mínimo
    public static final double MAX_SPEED_THRESHOLD = 100.0; // km/h máximo
    public static final int MIN_SAMPLES_FOR_AVERAGE = 5; // muestras mínimas para promedio
    
    // Errores
    public static final String CONNECTION_ERROR = "Worker %d: Error de conexión: %s";
    public static final String PROCESSING_ERROR = "Worker %d: Error procesando datagrama: %s";
    public static final String DISCONNECTED = "Worker %d: Master cerró conexión";
}
