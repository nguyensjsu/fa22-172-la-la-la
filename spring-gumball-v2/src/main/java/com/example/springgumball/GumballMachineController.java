package com.example.springgumball;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.extern.slf4j.Slf4j;

//These imports were taken from the HMAC Hash Example provided in Canvas
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64.Encoder;

//This import was added for the timestamp
import java.lang.*;

import com.example.gumballmachine.GumballMachine ;

@Slf4j
@Controller
@RequestMapping("/")
public class GumballMachineController {

    //This key was taken from the HMAC Hash Example provided in Canvas
    private static String key = "kwRg54x2Go9iEdl49jFENRM12Mp711QI" ;

    //This static hashing method was taken from the HMAC Hash Example provided in Canvas
    //Return value byte[] changed to String
    private static String hmac_sha256(String secretKey, String data) {
        try {
          Mac mac = Mac.getInstance("HmacSHA256") ;
          SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256") ;
          mac.init(secretKeySpec) ;
          byte[] digest = mac.doFinal(data.getBytes()) ;
          //Converted value from byte[] to String
          //Code taken from HMAC Hash Example provided in Canvas
          java.util.Base64.Encoder encoder = java.util.Base64.getEncoder() ;
          String hash = encoder.encodeToString(digest);
          return hash;
        } catch (InvalidKeyException e1) {
          throw new RuntimeException("Invalid key exception while converting to HMAC SHA256") ;
        } catch (NoSuchAlgorithmException e2) {
          throw new RuntimeException("Java Exception Initializing HMAC Crypto Algorithm") ;
        }
      }

    //Removed the parameter HTTPSession session
    @GetMapping
    public String getAction( @ModelAttribute("command") GumballCommand command, 
                            Model model) {
      
        GumballModel g = new GumballModel() ;
        g.setModelNumber( "SB102927") ;
        g.setSerialNumber( "2134998871109") ;
        model.addAttribute( "gumball", g ) ;
        
        GumballMachine gm = new GumballMachine() ;
        String message = gm.toString() ;

        //Removed setting attribute and getting session ID
        //Instead of only setting the state, I changed it so that I set the
        //state, timestamp, and hash
        //Sending state to the browser
        String state = gm.getState().getClass().getName();
        command.setState(state);

        //Converted current time in milliseconds to String for timestamp
        //Sending timestamp to the browser
        String timestamp = String.valueOf(System.currentTimeMillis());
        command.setTimestamp(timestamp);

        //The data for the hashing method is state + timestamp
        //Sending the hash_id to the browser
        String hash_id = hmac_sha256(key, state + timestamp);
        command.setHash(hash_id);

        String server_ip = "" ;
        String host_name = "" ;
        try { 
            InetAddress ip = InetAddress.getLocalHost() ;
            server_ip = ip.getHostAddress() ;
            host_name = ip.getHostName() ;
  
        } catch (Exception e) { }
  
        //Replaced session attribute with hash attribute
        model.addAttribute( "hash", hash_id ) ;
        model.addAttribute( "message", message ) ;
        model.addAttribute( "server",  host_name + "/" + server_ip ) ;

        return "gumball" ;

    }

    //Added throws Exception
    @PostMapping
    public String postAction(@Valid @ModelAttribute("command") GumballCommand command,  
                            @RequestParam(value="action", required=true) String action,
                            Errors errors, Model model, HttpServletRequest request) throws Exception {
    
        log.info( "Action: " + action ) ;
        log.info( "Command: " + command ) ;
    
        //Removed HTTPSession session = request.getSession()
        //Removed (GumballMachine) session.getAttribute("gumball"),
        //replaced with new GumballMachine (like in method getAction)
        GumballMachine gm = new GumballMachine();

        //Getting state, timestamp, and hash_id back from the browser
        String input_state = command.getState();
        String input_timestamp = command.getTimestamp();
        String input_hash_id = command.getHash();

        //Creating a hash id with the key and state + timestamp
        //If it does not match the browser's hash id, an Exception is thrown
        String input_hmac_data = input_state + input_timestamp;
        String hashing_test = hmac_sha256(key, input_hmac_data);
        if (!input_hash_id.equals(hashing_test)) {
            throw new Exception("The hash id's do not match!");
        }
        
        gm.setState( input_state ) ;

        if ( action.equals("Insert Quarter") ) {
            gm.insertQuarter() ;
        }

        if ( action.equals("Turn Crank") ) {
            command.setMessage("") ;
            gm.turnCrank() ;
        } 
       
        //Taken from getAction method
        String state = gm.getState().getClass().getName();
        command.setState(state);
        String timestamp = String.valueOf(System.currentTimeMillis());
        command.setTimestamp(timestamp);
        String hash_id = hmac_sha256(key, state + timestamp);
        command.setHash(hash_id);

        //Removed setting attribute and getting session ID
        String message = gm.toString() ;

        String server_ip = "" ;
        String host_name = "" ;
        try { 
            InetAddress ip = InetAddress.getLocalHost() ;
            server_ip = ip.getHostAddress() ;
            host_name = ip.getHostName() ;
  
        } catch (Exception e) { }
  
        //Replaced session attribute with hash attribute
        model.addAttribute( "hash", hash_id ) ;
        model.addAttribute( "message", message ) ;
        model.addAttribute( "server",  host_name + "/" + server_ip ) ;
     

        if (errors.hasErrors()) {
            return "gumball";
        }

        return "gumball";
    }
}