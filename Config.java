import java.util.*; 
import java.io.*; 
import java.nio.file.*; 

public class Config {

    private String ServerRoot; 
    private String DocumentRoot; 
    private Integer Listen; 
    private Boolean portSet = false; 
    private String LogFile; 
    private Map<String, String> ScriptAlias; 
    private Map<String, String> Alias; 
    private Map<String, String> Mime; 
    
    private String DirectoryIndex; 
    private Boolean directoryIndexExists = false; 

    private String AccessFile; 
    private Boolean accessFileExists = false; 

    public String getAccessFile() {
        if (accessFileExists)
            return AccessFile;
        else
            return ".htaccess"; // default 
    }


    public String getDirectoryIndex() {
        if (directoryIndexExists)
            return DirectoryIndex;
        else
           return "index.html"; 
    }

    public String getMimeType(String extension) {
        return Mime.get(extension); 
    }

    public String getServerRoot() {
        return ServerRoot; 
    }

    public String getDocumentRoot() {
        return DocumentRoot; 
    }

    public Integer getPort() {
        if (portSet) {
            return Listen; 
        } else {
            return 8080; 
        }
    }

    public String getLogFile() {
        return LogFile; 
    }

    public String getScriptAlias(String key) {
        return ScriptAlias.get(key); 
    }

    public Map<String, String> getScriptAliasMap(){
      return ScriptAlias;
    }

    public Map<String, String> getAliasMap() {
        return Alias; 
    }

    public String getAlias(String key) {
        return Alias.get(key); 
    }

    public Config() throws IOException {

        /// go through the lines in the mime.types file 
        Path mime = Paths.get("./conf/mime.types"); 
        Mime = new HashMap<>(); 

        try (
            BufferedReader mimeStream =  new BufferedReader(new FileReader(mime.toString()));
        ) {
            String l; 
            while ((l = mimeStream.readLine()) != null) {
                l.trim(); 
                if (l.length() == 0 || l.charAt(0) == '#') {
                    // this is a comment
                    continue; 
                }

                StringTokenizer st = new StringTokenizer(l); 
                String mime_type = st.nextToken(); 

                while (st.hasMoreTokens()) {
                    String extension = st.nextToken(); 
                    Mime.put(extension, mime_type); 
                }
            }
        }

        // go through the individual lines in the httpd.conf file 
        Path conf = Paths.get("./conf/httpd.conf"); 
        
        Alias = new HashMap<>(); 
        ScriptAlias = new HashMap<>(); 

        

        try (
            BufferedReader inputStream = new BufferedReader(new FileReader(conf.toString())); 
        ) {
            String l;
            while ((l = inputStream.readLine()) != null) {

                StringTokenizer st = new StringTokenizer(l); 
                String directive = st.nextToken();
                if (directive.equals("AccessFile")) {
                    String af = st.nextToken(); 
                    af = af.replace("\"", "");
                    AccessFile = af;
                    accessFileExists = true; 
                    
                } else if (directive.equals("DirectoryIndex")) {
                    String di = st.nextToken();
                    di = di.replace("\"", ""); 
                    DirectoryIndex = di; 
                    directoryIndexExists = true; 

                } else if (directive.equals("ServerRoot")) {
                    String sroot = st.nextToken(); 
                    sroot = sroot.replace("\"", ""); 
                    ServerRoot = sroot; 

                } else if (directive.equals("DocumentRoot")) {
                    String droot = st.nextToken();
                    droot = droot.replace("\"", ""); 
                    DocumentRoot = droot; 

                } else if (directive.equals("Listen")) {
                    Listen = Integer.parseInt(st.nextToken()); 
                    portSet = true; 

                } else if (directive.equals("LogFile")) {
                    String lfile = st.nextToken();
                    lfile = lfile.replace("\"", "");
                    LogFile = lfile; 

                } else if (directive.equals("ScriptAlias")) {
                    String route = st.nextToken(); 
                    String full_path = st.nextToken(); 
                    full_path = full_path.replace("\"", ""); 
                    ScriptAlias.put(route, full_path); 

                } else if (directive.equals("Alias")) {
                    String route = st.nextToken();
                    String full_path = st.nextToken(); 
                    full_path = full_path.replace("\"", ""); 
                    Alias.put(route, full_path); 
                    
                } else if (directive.equals("AccessFileName")) {
                    String full_path = st.nextToken(); 
                    full_path = full_path.replace("\"", ""); 
                    AccessFile = full_path;
                    accessFileExists = true; 
                }

            }
        }

    }
}