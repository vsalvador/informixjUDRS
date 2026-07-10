import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public final class IfxLogger {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static String logDirectory = "/tmp";
    private static String logBaseName = "IfxLogger";

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {

        Properties p = new Properties();

        try (FileInputStream in = new FileInputStream(System.getenv("INFORMIXDIR") + "/etc/ifxlogger.properties")) {
            p.load(in);

            logDirectory = p.getProperty("log.directory", "/tmp");
            logBaseName  = p.getProperty("log.basename", "IfxLogger");
        } catch (Exception e) {
            // Keep defaults
        }

        new File(logDirectory).mkdirs();
    }

    public static synchronized void log(String message) {

        String fileName = String.format("%s/%s_%s.log",
                logDirectory,
                logBaseName,
                LocalDate.now().format(DATE));

        try (BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true))) {
            out.write(message);
            out.newLine();
        } catch (IOException e) {
            // Never propagate logging failures into Informix
        }
    }
}