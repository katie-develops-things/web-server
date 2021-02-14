import java.util.*; 
import java.io.*; 
import java.nio.file.*; 
import java.net.*; 
import java.time.*;
import java.nio.charset.*; 

public class RequestProcessor implements Runnable {

    Socket clientSocket = null; 
    Config a = null; 
    Logger l = null; 

    public void run() {

        try (
            OutputStream out = clientSocket.getOutputStream();
            InputStream in = clientSocket.getInputStream();
        ) {

            Request req = new Request(in); 
            req.host = clientSocket.getInetAddress().toString().replace("/", ""); 

            Response res = new Response(req, out, a, l); 

            try {
                if (req.is_uri_aliased(a.getAliasMap())) {
                    // it's uri aliased
                } else if (req.is_script_aliased(a.getScriptAliasMap())){
                    // script aliased
                } else {
                    req.resolve_document_root(a.getDocumentRoot());
                }

                
                req.resolve_absolute_path(a.getDirectoryIndex()); 

                File resource = new File(req.path); 
                String cwd = resource.getParent(); 

                Path htpath = Paths.get(cwd, a.getAccessFile()); 
                
                Boolean access = true; 
                if (Files.exists(htpath)) {

                    AccessFile af = new AccessFile(htpath.toString()); 
                    if (req.Headers.containsKey("Authorization")) {
        
                        Htpassword htp = new Htpassword(af.authUserFilePath);

                        String header_val = req.Headers.get("Authorization");
                        StringTokenizer st = new StringTokenizer(header_val); 
                        String auth_type = st.nextToken();
                        String creds = st.nextToken(); 

                        if (htp.isAuthorized(creds, req)) {
                            // go ahead and access the protected resource
                        } else { 
                            access = false; 
                            res.send_403(); 
                        }

                    } else {
                        res.send_401(af.authName); 
                        access = false; 
                    }
                }


                if (access) {
                    if (req.is_script) {
                        
                        res.execScript(); 
                    } else {
                        res.process_request(); 
                    }
                }
            } catch (Exception e) {
                try {
                    res.send_500(); 
                } catch(IOException ioe) {
                    // welp we tried
                    System.out.println(ioe); 
                }
            }
            
        } catch(Exception e) {
            System.out.println("Error"); 
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close(); 
                }
            } catch (Exception e) {
                // do nothing 
            }   
        }
    }
    
    public RequestProcessor(Socket _clientSocket, Config _a, Logger _l) {
        this.clientSocket = _clientSocket; 
        this.a = _a; 
        this.l = _l; 
    }
}