import java.util.*;
import java.time.format.DateTimeFormatter;
import java.io.*; 
import java.nio.file.attribute.FileTime; 
import java.nio.file.*; 
import java.time.*; 

public class Tester {

    public static void main(String... args) throws IOException, InterruptedException {
        
        ProcessBuilder processBuilder = new ProcessBuilder(); 

        processBuilder.command("python", "--version"); 

        Map<String, String> env = processBuilder.environment(); 

        env.forEach((s, s2) -> {
            System.out.printf("%s %s %n", s, s2);
        });

        System.out.printf("%s %n", env.get("PATH"));
        
        String homeDir = System.getProperty("user.home");

        File fileName = new File(String.format("%s/Documents/tmp/output.txt", homeDir));

        processBuilder.redirectOutput(fileName);
        
        Process p = processBuilder.start(); 
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(p.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        }
    }
}