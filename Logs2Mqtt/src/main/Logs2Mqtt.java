import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

public class Logs2Mqtt {
    private static String DIRECTORIO_LOGS = "/tmp";
    private static String PREFIJO_ARCHIVO = "IfxLogger";
    private static String SUFIJO_ARCHIVO = ".log";
    private static String EXTENSION_CONTROL = ".offset";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static MqttClient client = null;
    private static String     topic;
    private static int        qosLevel;

    

    public static void main(String[] args) throws Exception {
        System.out.println("Iniciando servicio de lectura y limpieza de logs...");

        loadConfiguration();

        while (true) {
            List<File> archivosLog = obtenerArchivosLogOrdenados();

            if (archivosLog.isEmpty()) {
                System.out.println("No se encontraron archivos de log en " + DIRECTORIO_LOGS + ". Esperando...");
                esperar(10000);
                continue;
            }

            String nombreArchivoHoy = PREFIJO_ARCHIVO + "_" + LocalDate.now().format(FORMATO_FECHA) + SUFIJO_ARCHIVO;

            for (File archivo : archivosLog) {
                System.out.println("Npmbre archivo: " + archivo.getName() + " Nombre esperado: " + nombreArchivoHoy);
                boolean esArchivoDeHoy = archivo.getName().equals(nombreArchivoHoy);
                long offsetGuardado = cargarOffset(archivo) - 1;

                // Si por alguna razón quedó un archivo antiguo ya procesado en el directorio, 
                // lo limpiamos inmediatamente sin volver a abrirlo
                if (!esArchivoDeHoy && offsetGuardado >= archivo.length()) {
                    eliminarFicherosProcesados(archivo);
                    continue; 
                }

                System.out.println("Abriendo: " + archivo.getName() + " desde el offset: " + offsetGuardado);
                boolean cambiarDeFichero = procesarFichero(archivo, offsetGuardado, esArchivoDeHoy);

                // Si terminamos de leer un archivo antiguo con éxito, rompemos el bucle 
                // para recargar la lista de archivos actualizada
                if (cambiarDeFichero) {
                    break; 
                }
            }
        }
    }

    private static boolean procesarFichero(File archivo, long offsetInicial, boolean esArchivoDeHoy) throws Exception {
        MqttClient mqtt = null;

        File archivoControl = obtenerArchivoControl(archivo);
        System.out.println("Archivo de control: " + archivoControl);

        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            System.out.println("Length: " + raf.length() + " Offset: " + offsetInicial);
            if (offsetInicial < raf.length()) {
                raf.seek(offsetInicial);
            }

            System.out.println("Start to read lines");

            while (true) {
                // Verificar si el día cambió mientras procesábamos el archivo de hoy
                if (esArchivoDeHoy && !archivo.getName().equals(PREFIJO_ARCHIVO + "_" + LocalDate.now().format(FORMATO_FECHA) + SUFIJO_ARCHIVO)) {
                    System.out.println("El día ha cambiado. Pasando al archivo del nuevo día.");
                    return true; 
                }

                String linea = raf.readLine();

                if (linea != null) {
                    String lineaLimpia = new String(linea.getBytes("ISO-8859-1"), "UTF-8");

                    if (lineaLimpia.equals("")) {
                        System.out.println("[" + archivo.getName() + "] ... Blank skipped");
                    } else {
                    
                        // ==========================================
                        // LÓGICA DE PROCESAMIENTO DE LA LINEA DEL FICHERO
                        // ==========================================

                        System.out.println("[" + archivo.getName() + "] " + lineaLimpia);

                        mqtt = getMqttClient();

                        String      fullTopic   = constructFullTopic(topic, lineaLimpia);
                        MqttMessage mqttMessage = new MqttMessage(lineaLimpia.getBytes());

                        mqttMessage.setQos(qosLevel);

                        client.publish(fullTopic, mqttMessage);

                        // ==========================================
                        // FIN LOGICA DE PROCESAMIENTO DE La LINEA DEL FICHERO
                        // ==========================================
                    }

                    guardarOffset(archivoControl, raf.getFilePointer());
                } else {
                    System.out.println("Readline returns null");
                    if (!esArchivoDeHoy) {
                        // Doble chequeo estricto de finalización para archivos antiguos
                        if (raf.getFilePointer() >= archivo.length()) {
                            System.out.println("Archivo antiguo completado: " + archivo.getName());
                            
                            // Cerramos explícitamente el RandomAccessFile antes de borrar para liberar el archivo en el S.O.
                            raf.close(); 
                            
                            // Procedemos a la eliminación segura de ambos archivos
                            eliminarFicherosProcesados(archivo);
                            return true; 
                        }
                    }

                    esperar(1000);
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando " + archivo.getName() + ": " + e.getMessage());
            esperar(2000);
            return false;
        } finally {
            System.out.println("Desconectando Mqtt" );
            try {
                mqtt.disconnectForcibly(1000, 1000);
            } catch (Exception ignored) {}

            try {
                mqtt.close(true);
            } catch (Exception ignored) {}

            mqtt = null;
        }
    }

    private static void loadConfiguration() {
        String propertyPath = System.getenv("INFORMIXDIR") + "/etc/ifxlogger.properties";

        System.out.println("Loading properties from " + propertyPath);

        Properties p = new Properties();

        try (FileInputStream in = new FileInputStream(propertyPath)) {
            p.load(in);

            DIRECTORIO_LOGS = p.getProperty("log.directory", "/tmp");
            PREFIJO_ARCHIVO = p.getProperty("log.basename", "IfxLogger");
        } catch (Exception e) {
            // Keep defaults
            System.err.println("Property file " + propertyPath + " not found or incorrect");
        }

        new File(DIRECTORIO_LOGS).mkdirs();
    }

    /**
     * Elimina el archivo de log y su correspondiente archivo de control (.offset)
     */
    private static void eliminarFicherosProcesados(File archivoLog) {
        File archivoControl = obtenerArchivoControl(archivoLog);
        
        System.out.println("Iniciando limpieza del histórico para: " + archivoLog.getName());

        if (archivoLog.exists()) {
            if (archivoLog.delete()) {
                System.out.println("Fichero de log eliminado: " + archivoLog.getName());
            } else {
                System.err.println("No se pudo eliminar el fichero de log (¿está abierto por otro proceso?): " + archivoLog.getName());
            }
        }

        if (archivoControl.exists()) {
            if (archivoControl.delete()) {
                System.out.println("Fichero de control eliminado: " + archivoControl.getName());
            } else {
                System.err.println("No se pudo eliminar el fichero de control: " + archivoControl.getName());
            }
        }
    }

    private static List<File> obtenerArchivosLogOrdenados() {
        File dir = new File(DIRECTORIO_LOGS);
        File[] archivos = dir.listFiles((d, name) -> name.startsWith(PREFIJO_ARCHIVO) && name.endsWith(SUFIJO_ARCHIVO));
        
        if (archivos == null) return Collections.emptyList();

        return Arrays.stream(archivos)
                .sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
                .collect(Collectors.toList());
    }

    private static File obtenerArchivoControl(File archivoLog) {
        String rutaControl = archivoLog.getAbsolutePath().replace(SUFIJO_ARCHIVO, EXTENSION_CONTROL);
        return new File(rutaControl);
    }

    private static long cargarOffset(File archivoLog) {
        File archivoControl = obtenerArchivoControl(archivoLog);
        if (!archivoControl.exists()) return 1;
        try {
            String contenido = new String(Files.readAllBytes(Paths.get(archivoControl.getAbsolutePath()))).trim();
            return Long.parseLong(contenido);
        } catch (Exception e) {
            return 0;
        }
    }

    private static void guardarOffset(File archivoControl, long offset) {
        try {
            Files.write(Paths.get(archivoControl.getAbsolutePath()), String.valueOf(offset).getBytes());
        } catch (IOException e) {
            System.err.println("Error al guardar archivo de control: " + e.getMessage());
        }
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }



    public static synchronized MqttClient getMqttClient() throws Exception {

        if (client == null || !client.isConnected()) {
             String brokerUrl = null;
             String clientId = "Krakatoa";
             String username = null;
             String password = null;

             System.out.println("Connecting to MQTT broker");

             // Load configuration from mqtt.properties
             Properties properties = new Properties();

            String configFilePath = System.getenv("INFORMIXDIR") + "/etc/mqtt.properties";

            try (InputStream input = new FileInputStream(configFilePath)) {

                properties.load(input);

                brokerUrl = properties.getProperty("brokerUrl");
                clientId = properties.getProperty("clientId");
                username = properties.getProperty("username");
                password = properties.getProperty("password");
                topic = properties.getProperty("topic");

                // Optional Quality of Service (default=0)
                qosLevel = Integer.parseInt(
                    properties.getProperty("qosLevel", "0")
                );
            } catch (IOException ex) {
                System.err.println( "Error loading configuration file [" + configFilePath + "]: " + ex.getMessage());
                System.exit(1);
            }


            /*
             * If no clientId has been configured,
             * use the local hostname.
             */
            if (clientId == null || clientId.trim().isEmpty()) {
                 clientId = "Krakatoa";
            }

            /*
             * Create MQTT client instance.
             *
             * Parameters:
             *   brokerUrl -> MQTT broker endpoint
             *   clientId  -> MQTT client identifier
             */
            client = new MqttClient(
                brokerUrl,
                clientId,
                new MemoryPersistence()
                //new MqttDefaultFilePersistence(System.getenv("INFORMIXDIR"))
            );

            /*
             * Configure MQTT connection.
             *
             * Clean session means that subscriptions and
             * session state are not preserved between
             * connections.
             */

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(false);
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(5);

            /*
             * Configure authentication if password is defined.
             */
            if (password != null) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }

            // Connect to MQTT broker
            client.connect(options);
        }

        return client;
    }

    private static String constructFullTopic(
        String topic,
        String jsonMessage
    ) {

        String fullTopic = topic;

        // Only process replacements if placeholders exist
        if (topic.contains("%")) {

            try {
                // Parse JSON payload
                JSONObject jsonObject = new JSONObject(jsonMessage);

                /*
                 * Replace every placeholder found
                 * in the topic template.
                 */
                for (String key : jsonObject.keySet()) {

                    String value = String.valueOf(jsonObject.get(key));

                    fullTopic = fullTopic.replace( "%" + key, value);
                }

            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                System.err.println("Parsing bad JSON: " + jsonMessage);
            }
        }

        return fullTopic;
    }

}
