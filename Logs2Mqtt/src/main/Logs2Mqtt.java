import java.io.File;
import java.io.FileInputStream;
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

    public static void main(String[] args) {
        System.out.println("Iniciando servicio de lectura y limpieza de logs...");

        while (true) {
            List<File> archivosLog = obtenerArchivosLogOrdenados();

            if (archivosLog.isEmpty()) {
                System.out.println("No se encontraron archivos de log. Esperando...");
                esperar(5000);
                continue;
            }

            String nombreArchivoHoy = PREFIJO_ARCHIVO + LocalDate.now().format(FORMATO_FECHA) + SUFIJO_ARCHIVO;

            for (File archivo : archivosLog) {
                System.out.println("Npmbre arcjivo: " + archivo.getName() + " Nombre esperado: " + nombreArchivoHoy);
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

    private static boolean procesarFichero(File archivo, long offsetInicial, boolean esArchivoDeHoy) {
        File archivoControl = obtenerArchivoControl(archivo);

        try (RandomAccessFile raf = new RandomAccessFile(archivo, "r")) {
            if (offsetInicial < raf.length()) {
                raf.seek(offsetInicial);
            }

            while (true) {
                // Verificar si el día cambió mientras procesábamos el archivo de hoy
                if (esArchivoDeHoy && !archivo.getName().equals(PREFIJO_ARCHIVO + LocalDate.now().format(FORMATO_FECHA) + SUFIJO_ARCHIVO)) {
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

                        // ==========================================
                        // FIN LOGICA DE PROCESAMIENTO DE La LINEA DEL FICHERO
                        // ==========================================
                    }

                    guardarOffset(archivoControl, raf.getFilePointer());
                } else {
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
        } catch (IOException e) {
            System.err.println("Error procesando " + archivo.getName() + ": " + e.getMessage());
            esperar(2000);
            return false;
        }
    }

    private static void loadConfiguration() {

        Properties p = new Properties();

        try (FileInputStream in = new FileInputStream(System.getenv("INFORMIXDIR") + "/etc/ifxlogger.properties")) {
            p.load(in);

            DIRECTORIO_LOGS = p.getProperty("log.directory", "/tmp");
            PREFIJO_ARCHIVO = p.getProperty("log.basename", "IfxLogger");
        } catch (Exception e) {
            // Keep defaults
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
}
