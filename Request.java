import java.util.*; 
import java.io.*;  
import java.nio.file.*; 
import java.net.*; 
import java.time.*;
import java.nio.charset.*; 

public class Request {  

    public Map<String, String> Headers; 
    public String http_method; 

    public String og_path;
    public String path; // this is the path
    public Map<String, String> queryParams; 
    public String queryString = ""; 
    
    public String version; 

    List<Byte> body; 
    public Boolean error; 
    public Boolean is_script; 

    public String host; 
    public String requestLine; 
    public String username = "-"; 

    // create method Boolean is_uri_alised(), if it is, also modify the path, and resolve the full URI
    public Boolean is_uri_aliased(Map<String, String> Alias){
        
        int i = 0; 
        while (i < path.length()) {
            if (path.charAt(i) == '/')
                break;
            else
                i++; 
        }

        if (i == path.length())
            return false;       // it is not script aliased 

        
        String prefix = "/" + path.substring(0, i + 1); 
        if (Alias.containsKey(prefix)) {
            // so it is uri aliased
            String full_path = Alias.get(prefix) + path.substring(i + 1, path.length()); 
            path = full_path; 

            is_script = false; 
            return true; 
        }

        return false; 
    }



    public Boolean is_script_aliased(Map<String, String> ScriptAlias){
        int i = 0; 
        while (i < path.length()) {
            if (path.charAt(i) == '/')
                break;
            else
                i++; 
        }

        if (i == path.length())
            return false;       // it is not script aliased 

        
        String prefix = "/" + path.substring(0, i + 1); 
        if (ScriptAlias.containsKey(prefix)) {
            // so it is uri aliased
            String full_path = ScriptAlias.get(prefix) + path.substring(i + 1, path.length()); 
            path = full_path; 

            is_script = true; 
            return true; 
        }

        is_script = false; 
        return false; 
    }

    
    public void resolve_document_root(String doc_root){
        String full_path = doc_root + path;
        is_script = false; 
        path = full_path;
    }


    // create method void is_file(), if it is a file, we're good to go, otherwise, check directory index
    public void resolve_absolute_path(String directory_index) {

        Path file = new File(path).toPath();

        if (Files.exists(file)) {
            if (Files.isDirectory(file)) {
                //System.out.println("It's a directory"); 
                path = path + directory_index; 
            }
        }
    }

    /*
        If at any point, there is an error in parsing the request, break, and set the error flag to true; 
        Otherwise, we go through the entire parseing process, and set it to false
    */

    public Request(InputStream in) throws IOException {

        char c;
        // read the request line 
        String requestLine = ""; 
        queryParams = new HashMap<>(); 
        while (true) {
            c = (char) in.read(); 
            if (c == '\r') {
                this.requestLine = requestLine; 
                c = (char) in.read(); // get the \n as well 
                StringTokenizer st = new StringTokenizer(requestLine); 
                http_method = st.nextToken(); 

                String URI = st.nextToken(); 
                String[] uriParts = URI.split("\\?");

                if (uriParts.length == 2) {
                    // there is a query string 
                    String queryString = uriParts[1]; 
                    this.queryString = uriParts[1]; 
                    /*
                    StringTokenizer queries = new StringTokenizer(queryString, "&"); 
                    while (queries.hasMoreTokens()) {
                        String query = queries.nextToken(); 
                        String[] queryParts = query.split("="); 
                        String key = queryParts[0]; 
                        String val = queryParts[1]; 
                        queryParams.put(key, val); 
                    }
                    */
                } 

                path = uriParts[0]; 
                og_path = path; 
                path = path.substring(1, path.length()); 
                version = st.nextToken(); 
                break; 
            }
            requestLine += c; 
        }   

        // read the headers 
        String header_line = ""; 
        Headers = new HashMap<>(); 
        while (true) {
            c = (char) in.read(); 
            if (c == '\r') {
                c = (char) in.read(); // get the \n as well 
                if (header_line.equals("")) {
                    // we are at the end of the headers
                    break; 
                }

                // we are at the end of the line, parse the header 
                //System.out.println("header_line: " + header_line); 
                String[] tokens = header_line.split(": "); 
                String header_name = tokens[0]; 
                String header_val = tokens[1]; 
                
                //System.out.println("header_name: " + header_name + " --- header_val: " + header_val); 
                Headers.put(header_name, header_val); 

                header_line = ""; 
                continue; 
            }
            header_line += c; 
        }

        // if there is a body, read that
        body = new ArrayList<>(); 
        if (Headers.containsKey("Content-Length")) {
            Integer length = Integer.parseInt(Headers.get("Content-Length")); 
            byte d; 
            while (length > 0) {
                d = (byte) in.read(); 
                body.add(d);
                length--; 
            }
        }
    }

}


