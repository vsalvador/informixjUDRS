import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

public final class IfxLogger {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String DEFAULT_LOG_DIR = "/tmp";
    private static final String DEFAULT_LOG_BASE = "IfxLogger";

    // Use volatile for visibility across threads, though initialized in static block
    private static volatile String logDirectory = DEFAULT_LOG_DIR;
    private static volatile String logBaseName  = DEFAULT_LOG_BASE;

    // Use a lock for thread-safe file writing
    private static final ReentrantLock lock = new ReentrantLock();

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {

        Properties p  = new Properties();
        String envDir = System.getenv("INFORMIXDIR");
        
        if (envDir == null || envDir.isEmpty()) {
            // Optionally log to stderr or use a fallback
            System.err.println("INFORMIXDIR not set. Using default log config.");
            return;
        }

        String configPath = envDir + "/etc/ifxlogger.properties";
        File   configFile = new File(configPath);

        if (configFile.exists() && configFile.isFile()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                p.load(in);
                String dir  = p.getProperty("log.directory");
                String base = p.getProperty("log.basename");
                
                if (dir != null && !dir.trim().isEmpty()) {
                    logDirectory = dir.trim();
                }
                if (base != null && !base.trim().isEmpty()) {
                    logBaseName = base.trim();
                }
            } catch (IOException e) {
                System.err.println("Failed to load ifxlogger.properties: " + e.getMessage());
            }
        } else {
            System.out.println("Config file not found: " + configPath + ". Using defaults.");
        }

        // Ensure directory exists
        try {
            new File(logDirectory).mkdirs();
        } catch (SecurityException e) {
            System.err.println("Cannot create log directory: " + logDirectory);
        }
    }

   /**
    * Logs a message to the daily log file.
    * Uses a lock to ensure atomic writes and avoid file corruption.
    * Note: For high-throughput scenarios, consider using a queue and a dedicated writer thread.
    */
    public static void log(String message) {
       if (message == null) {
           return;
       }

       // Use a lock to synchronize file access without blocking the entire JVM
       lock.lock();
       try {
           String fileName = String.format("%s/%s_%s.log",
                   logDirectory,
                   logBaseName,
                   LocalDate.now().format(DATE));

           // Append mode: true
           try (BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true)))
{
               out.write(message);
               out.newLine();
               out.flush(); // Ensure data is written to disk
           } catch (IOException e) {
               // Log to stderr as a fallback, but do not throw
               System.err.println("IfxLogger failed to write log: " + e.getMessage());
           }
       } finally {
           lock.unlock();
       }
   }
}