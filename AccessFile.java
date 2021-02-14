import java.io.*;
import java.util.*;

public class AccessFile {

    public String authUserFilePath;
    public Boolean authTypeBasic;
    public String authName;
    public String require;

    public AccessFile(String path){
        try(
            BufferedReader inputStream = new BufferedReader(new FileReader(path));
        ) {
            //System.out.println("Parsing Access File"); 
            String l;
            while ((l = inputStream.readLine()) != null) {
                //System.out.println(l); 
                StringTokenizer st = new StringTokenizer(l); 
                String directive = st.nextToken();

                if (directive.equals("AuthUserFile")) {
                    this.authUserFilePath = st.nextToken(); 
                    this.authUserFilePath = authUserFilePath.replace("\"", "");
                    
                } else if (directive.equals("AuthType")) {
                    String arg = st.nextToken();
                    if(arg == "Basic"){
                        this.authTypeBasic = true;
                    } else
                        this.authTypeBasic = false; 

                } else if(directive.equals("AuthName")){
                    this.authName = ""; 
                    while (st.hasMoreTokens()) {
                        this.authName += st.nextToken(); 
                    }

                } else if(directive.equals("Require")){
                    String arg = st.nextToken();
                    if(arg == "valid-user"){
                        
                    }
                }
            }

        } catch (FileNotFoundException e) {
            // the .htaccess file does not exist
            System.out.println("ht access file does not exist");
        } catch(IOException ioe) {
            // error reading the file
        }
    }

}