import java.util.HashMap;
import java.util.Base64;
import java.io.*; 
import java.nio.charset.Charset;
import java.security.MessageDigest;

import java.io.IOException;

public class Htpassword {
  private HashMap<String, String> passwords;

  public Htpassword( String filename ) throws IOException {

    this.passwords = new HashMap<String, String>();
    try(
        BufferedReader inputStream = new BufferedReader(new FileReader(filename));
    ) {
      String l;
      while ((l = inputStream.readLine()) != null) {
        this.parseLine(l); 
      }
    }
  }

  public void parseLine( String line ) {
    String[] tokens = line.split( ":" );

    if( tokens.length == 2 ) {
      passwords.put( tokens[ 0 ], tokens[ 1 ].replace( "{SHA}", "" ).trim() );
    }
  }

  public boolean isAuthorized( String authInfo , Request req) {
    // authInfo is provided in the header received from the client
    // as a Base64 encoded string.
    String credentials = new String(
      Base64.getDecoder().decode( authInfo ),
      Charset.forName( "UTF-8" )
    );

    // The string is the key:value pair username:password
    String[] tokens = credentials.split( ":" );
    req.username = tokens[0]; 

    // TODO: implement this
    if (this.passwords.containsKey(tokens[0])) {
        return this.verifyPassword(tokens[0], tokens[1]); 
    } else {
      // invalid credentials entered
      return false; 
    }
  }

  private boolean verifyPassword( String username, String password ) {
    // encrypt the password, and compare it to the password stored
    // in the password file (keyed by username)
    // TODO: implement this - note that the encryption step is provided as a
    // method, below

    String encryptedPassword = this.encryptClearPassword(password); 
    //System.out.println("user entered username: " + username + " password: " + password); 
    //System.out.println("encrypted: " + encryptedPassword); 
    return (encryptedPassword.equals(this.passwords.get(username))); 
  }

  private String encryptClearPassword( String password ) {
    // Encrypt the cleartext password (that was decoded from the Base64 String
    // provided by the client) using the SHA-1 encryption algorithm
    try {
      MessageDigest mDigest = MessageDigest.getInstance( "SHA-1" );
      byte[] result = mDigest.digest( password.getBytes() );

     return Base64.getEncoder().encodeToString( result );
    } catch( Exception e ) {
      return "";
    }
  }
}