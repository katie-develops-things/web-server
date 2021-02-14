import java.util.*; 
import java.io.*; 
import java.net.*; 
import java.nio.file.*; 
import java.time.*;
import java.nio.charset.*; 
import java.time.format.DateTimeFormatter;
import java.nio.file.attribute.FileTime; 


public class Response {

    Request req; 
    OutputStream out;
    Config a; 
    Logger l; 

    public void execScript() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(); 

        //System.out.println("attempting to execute script at: " + req.path); 
        processBuilder.command(req.path); 
        //processBuilder.command("python", "--version"); 

        Map<String, String> env = processBuilder.environment(); 
        env.put("SERVER_PROTOCOL", req.version);
        env.put("QUERY_STRING", req.queryString);

        req.Headers.forEach((header, val) -> {
            env.put("HTTP_" + header, val); 
        });

        Process process = processBuilder.start(); 

        OutputStream stdin = process.getOutputStream(); // this is what we use to write to the process
        InputStream stdout = process.getInputStream();  // this is what we read from the process

        if (req.http_method.equals("POST") || req.http_method.equals("PUT")) {

            // the request may have a body that we need to send to the standard input of the subprocess
            List<Byte> body = req.body; 
            
            byte[] data = new byte[body.size()];
            for (int i = 0; i < body.size(); i++) {
                data[i] = body.get(i); 
            }

            try (OutputStream file_out = new BufferedOutputStream(stdin)) {

                file_out.write(data, 0, data.length);
            }
        }

        String response = this.response_template("200 OK"); 
        //response += "Content-Length: 12\r\n\r\n";
        this.byteSend(response); 

        int c;
        while ((c = stdout.read()) != -1) {
            //System.out.println("Is this even getting executed"); 
            out.write(c);
        }
    }

    public void byteSend(String response) throws IOException {
        byte[] b = response.getBytes(Charset.forName("UTF-8")); 
        out.write(b); 
    }

    public String response_template(String status) {
        String response = "HTTP/1.1 " + status + "\r\n";
        response += ("Date: " + LocalDateTime.now().toString() + "\r\n"); 
        response += "Server: akhil-katie\r\n";
        response += "Connection: close\r\n";
        return response;
    }

    public void send_404() throws IOException {
        String response = this.response_template("404 Not Found"); 
        response += "\r\n";
        this.byteSend(response); 
        l.log(req, 404, 0);
    }

    public void send_401(String realm) throws IOException {
        String response = this.response_template("401 Unauthorized");
        response += "WWW-Authenticate: Basic realm=" + realm + ", charset=\"UTF-8\"\r\n\r\n";
        this.byteSend(response); 
        l.log(req, 401, 0);
    }

    public void send_403() throws IOException {
        //System.out.println("sending 403"); 
        String response = this.response_template("403 Forbidden");
        response += "\r\n";
        this.byteSend(response); 
        l.log(req, 403, 0);
    }

    public void send_201() throws IOException {
        String response = this.response_template("201 Created"); 
        response += "Location: " + req.og_path + "\r\n\r\n";
        this.byteSend(response); 
        l.log(req, 201, 0);
    }

    public void send_500() throws IOException {
        String response = this.response_template("500 Internal Server Error");
        response += "\r\n"; 
        this.byteSend(response); 
        l.log(req, 500, 0);
    }

    public void send_204() throws IOException {
        String response = this.response_template("204 No Content"); 
        response += "\r\n";
        this.byteSend(response); 
        l.log(req, 204, 0);
    }

    public void process_request() throws IOException {
        if (req.http_method.equals("PUT")) {
            Path write_path = Paths.get(req.path);
            List<Byte> body = req.body; 
            
            byte[] data = new byte[body.size()];
            for (int i = 0; i < body.size(); i++) {
                data[i] = body.get(i); 
            }

            File f = new File(req.path); 
            File parDir = new File(f.getParent()); 
            parDir.mkdirs();

            try (OutputStream file_out = new BufferedOutputStream(Files.newOutputStream(write_path))) {

                file_out.write(data, 0, data.length);
            } catch (IOException x) {
                System.err.println(x);
                this.send_500(); 
                return; 
            }
            this.send_201(); 

        } else if (req.http_method.equals("DELETE")) {
            Path delete_path = Paths.get(req.path);
            try {
                Files.delete(delete_path);
            } catch (NoSuchFileException x) {
                System.err.format("%s: no such" + " file or directory%n", delete_path);
            } catch (DirectoryNotEmptyException x) {
                System.err.format("%s not empty%n", delete_path);
            } catch (IOException x) {
                // File permission problems are caught here.
                System.err.println(x);
                this.send_500();
                return; 
            }
            this.send_204(); 

        } else {

            Path file = new File(req.path).toPath();
            if (!Files.exists(file)) {
                this.send_404();      
                return;   
            }

            Path path = new File(req.path).toPath(); 

            FileTime fileTime = Files.getLastModifiedTime(path);
            ZonedDateTime zdtFile = ZonedDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.UTC); 

            if (req.http_method.equals("POST")) {
                this.send_200(req.path, zdtFile); 
                return; 
            }

            if (req.Headers.containsKey("If-Modified-Since")) {
                // we may need to send a 304
                ZonedDateTime imsZDT = ZonedDateTime.parse(req.Headers.get("If-Modified-Since"), 
                    DateTimeFormatter.RFC_1123_DATE_TIME);


                if (zdtFile.isAfter(imsZDT)) {
                    // the file has since been modified
                    this.send_200(req.path, zdtFile); 
                } else {
                    // the file has not since been modified
                    this.send_304(zdtFile); 
                }
            } else {
                this.send_200(req.path, zdtFile); 
            }

        }
    }

    public void send_304(ZonedDateTime zdt) throws IOException {
        String response = this.response_template("304 Not Modfiied"); 
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");
        response += "Last-Modified: " + formatter.format(zdt) + "\r\n\r\n"; 
        this.byteSend(response); 
        l.log(req, 304, 0);
    }

    public void send_200(String path, ZonedDateTime zdt) throws IOException {
        String response = this.response_template("200 OK");
        
        int dot = path.lastIndexOf('.'); 
        if (dot > 0) {
            String fileExtension = path.substring(dot + 1); 
            String contentType = a.getMimeType(fileExtension);
            if (contentType == null) {
                response += "Content-Type: Unknown\r\n";
            } else {
                response += "Content-Type: " + contentType + "\r\n"; 
            }
        } else {
            response += "Content-Type: Unknown\r\n";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");
        response += "Last-Modified: " + formatter.format(zdt) + "\r\n"; 

        if (req.http_method.equals("HEAD")) {
            long len = new File(path).length(); 
            System.out.println("Size of file is: " + len); 
            response += "Content-Length: " + len + "\r\n\r\n";
            this.byteSend(response); 
            l.log(req, 200, 0);
            return; 
        }

        List<Byte> bfile = new ArrayList<>(); 
        try (
            FileInputStream in = new FileInputStream(path);
        ) {
            int c; 
            while ((c = in.read()) != -1)  {
                bfile.add((byte) c); 
            }
        }

        int len = bfile.size(); 
        byte[] lbfile = new byte[len]; 
        for (int i = 0; i < len; i++) {
            lbfile[i] = bfile.get(i); 
        }

        response += "Content-Length: " + len + "\r\n\r\n";

        byte[] b = response.getBytes(Charset.forName("UTF-8")); 
        
        byte[] final_res = new byte[b.length + lbfile.length];
        int i = 0, j = 0; 
        for (i = 0; i < b.length; i++) {
            final_res[j] = b[i]; 
            j++; 
        }

        for (i =0; i < lbfile.length; i++) {
            final_res[j] = lbfile[i]; 
            j++; 
        }
        
        out.write(final_res); 
        l.log(req, 200, len);
    }

    public Response(Request _req, OutputStream _out, Config _a, Logger _l) throws IOException {
        this.req = _req; 
        this.out = _out; 
        this.a = _a; 
        this.l = _l; 
    }
}