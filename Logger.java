import java.util.*; 
import java.io.*; 
import java.net.*; 
import java.nio.file.*; 
import static java.nio.file.StandardOpenOption.*;
import java.time.*;
import java.nio.charset.*; 
import java.time.format.DateTimeFormatter;
import java.nio.file.attribute.FileTime; 

public class Logger {

    public String logFile; 

    public Logger(String _logFile) throws IOException {
        this.logFile = _logFile; 
        
        File f = new File(logFile); 
        File parDir = new File(f.getParent()); 
        parDir.mkdirs();
    }

    public synchronized void log(Request req, Integer Status, Integer contentLength) throws IOException {
        String host = req.host; 
        String username = req.username; 
        String requestLine = req.requestLine;

        DateTimeFormatter clgFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
        String logDateTime = clgFormatter.format(ZonedDateTime.now(ZoneId.systemDefault())).toString(); 

        Path p = Paths.get(this.logFile); 
        try (
           PrintWriter outputStream = new PrintWriter(
               new BufferedOutputStream(Files.newOutputStream(p, CREATE, APPEND)));
        ) {

            String cl = "-";
            if (contentLength > 0)
                cl = contentLength.toString(); 

            String logEntry = host + " - " + username + " " + logDateTime + " " + requestLine + " " + Status + 
                " " + cl; 
            
            System.out.println(logEntry); 
            outputStream.println(logEntry); 
        }
    }
}