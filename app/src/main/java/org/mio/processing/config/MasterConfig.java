package org.mio.processing.config;

public class MasterConfig {
    // Configuración del Master Node
    public static final int MASTER_PORT = 8080;
    public static final int EXPECTED_WORKERS = 3;
    public static final String DEFAULT_CSV_PATH = 
        "C:\\Users\\default.LAPTOP-M81T5L1M\\Desktop\\ICESI2025II\\ingesoft 4\\dataset\\datagrams4history.csv";
    
    // Mensajes del Master
    public static final String MASTER_BANNER = 
        "==========================================\n" +
        "===     MASTER NODE - SITM-MIO        ===\n" +
        "==========================================";
    
    public static final String MASTER_ROLE = "Rol: COORDINADOR DISTRIBUIDO";
    public static final String SERVER_STARTING = "Estado: INICIANDO SERVIDOR TCP...";
    public static final String SERVER_STARTED = "Master: ✓ SERVIDOR TCP INICIADO";
    public static final String WAITING_WORKERS = "Master: Esperando conexión de workers...";
    public static final String WORKER_CONNECTED = "Master: Worker %d conectado desde %s";
    public static final String ALL_WORKERS_CONNECTED = "Master: ✓ TODOS LOS WORKERS CONECTADOS";
    public static final String START_PROCESSING = "Master: Presiona Enter para iniciar el procesamiento distribuido...";
    
    // Mensajes de procesamiento
    public static final String PROCESSING_BANNER = 
        "\n==========================================\n" +
        "===  INICIANDO PROCESAMIENTO DISTRIBUIDO ===\n" +
        "==========================================";
    
    public static final String LOADING_GRAPH = "Master: Cargando grafo del SITM-MIO...";
    public static final String GRAPH_LOADED = "Master: ✓ Grafo cargado - %d arcos";
    public static final String LOADING_DATAGRAMS = "Master: Iniciando carga y distribución de datagramas...";
    public static final String DATAGRAMS_LOADED = "Master: ✓ CARGA COMPLETADA - %d datagramas";
    public static final String DISTRIBUTION_PROGRESS = "Master: Progreso distribución: %d/%d (%.1f%%)";
    public static final String DISTRIBUTION_COMPLETE = "Master: ✓ DISTRIBUCIÓN FINALIZADA";
    public static final String WAITING_RESULTS = "Master: Esperando resultados de workers...";
    public static final String SENDING_STOP_SIGNAL = "Master: Enviando señal de parada a workers...";
    public static final String PROCESSING_COMPLETE = "Master: ✓ PROCESAMIENTO DISTRIBUIDO FINALIZADO";
    public static final String AGGREGATING_RESULTS = "Master: Agregando resultados de todos los workers...";
    
    // Resultados
    public static final String RESULTS_BANNER = 
        "\n==========================================\n" +
        "===     RESULTADOS DISTRIBUIDOS        ===\n" +
        "==========================================";
    
    public static final String WORKER_RESULTS = "Master: Worker %d: %d arcos procesados";
    public static final String FINAL_SUMMARY = 
        "Master: Workers activos: %d (TCP distribuido)\n" +
        "Master: Datagramas procesados: %d\n" +
        "Master: Tiempo total: %.2f segundos\n" +
        "Master: Rendimiento: %.0f datagramas/segundo\n" +
        "Master: Arcos con velocidad calculada: %d";
    
    // Configuración de procesamiento
    public static final int PROGRESS_REPORT_INTERVAL = 10000; // cada 10,000 datagramas
    public static final int MONITOR_INTERVAL_MS = 2000; // cada 2 segundos
    public static final int WORKER_TIMEOUT_MS = 5000; // 5 segundos
}
