import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Builder.GETRequestBuilder;
import Builder.POSTRequestBuilder;
import Builder.RequestBuilder;
import UDPClient.UDPClient;

public class httpc {

    static Argument parameter = new Argument();
    private static final Logger logger = LoggerFactory.getLogger(httpc.class);

    public static void main(String[] args) {     
      args = new String[] { "POST" , "-h", "Content-Length:12", "-d", "Hello world!" , "http://localhost:8007/bob.txt"};
//      args = new String[] { "GET" , "http://httpbin.org/get?course=networking&assignment=1"};
        for (int i = 0; i < args.length; i++) {
                    if (args[0].equals("help")) {
                        try {
                            args[1] = args[1];

                        } catch (Exception e) {
                            System.out.println(help(""));
                            System.exit(0);
                        }
                        System.out.println(help(args[1]));
                        System.exit(0);
                    }

        }
        initArgument(args);
        RequestBuilder req = null;
        if(parameter.getRequestType().equals("GET "))
        {
             req = new GETRequestBuilder(parameter.getURL(), parameter.getHeader(),parameter.getBody());
        }
        else if (parameter.getRequestType().equals("POST "))
        {
             req = new POSTRequestBuilder(parameter.getURL(), parameter.getHeader(),parameter.getBody());
        }
        else 
        {
            System.out.println("The request Type is not accepted");
            System.out.println(help(""));
            System.exit(0);
        }
      
        UDPClient net = new UDPClient(req, parameter.getURL(), parameter.getRouter());
        net.request();
       

        if (parameter.isOutputToFile()) {
           outputToFile(net,args[args.length-1],parameter.isVerbose());
            System.exit(0);
        }

        if (parameter.isVerbose()) {
            logger.info("Server response\n",net.getRes().verboseToString(false));
            System.out.println(net.getRes().verboseToString(false));
        } 
        else {
            logger.info(net.getRes().verboseToString(true));
        //    System.out.println(net.getRes().toString());
        }

    }

    public static void initArgument(String[] args) {        

        if (contains(args, "-v")) {
            parameter.setVerbose(true);
        }
        if (contains(args, "-h")) {
            setHeaders(args);
        }
        if (contains(args, "-d")) {
            parameter.setData(true);
            setBodyD(args);
        }
        if (contains(args, "-f")) {
            parameter.setFile(true);
            setBodyF(args);
        }
        if(contains(args,"-r")){
            setRouter(args);
        }
        if (args[args.length - 2].equals("-o")) {
            parameter.setOutputToFile(true);            
        }
        setURL(args);

        if (parameter.isData() && parameter.isFile()) {
            System.out.println("Request is not appropriate contains both a file and inline data");
            System.exit(0);
        }
        if (parameter.getRequestType().equals("GET ") && (parameter.isData() || parameter.isFile())) {
            System.out.println(" GET Request cannot be used with a file or inline data");
            System.exit(0);
        }

    }

    public static String help(String temp) {
        switch (temp) {
        case "":
            return "\n\nusage: httpc get [-v] [-h key:value] URL"
                    + "httpc is a curl-like application but supports HTTP protocol only. \nUsage: \nhttpc command [arguments] \nThe commands are:\n"
                    + "get executes a HTTP GET request and prints the response. \n"
                    + "post executes a HTTP POST request and prints the response. \n" + "help prints this screen. \n"
                    + "Use \"httpc help [command]\" for more information about a command.";
        case "get":
            return "Get executes a HTTP GET request for a given URL.\n"
                    + "-v Prints the detail of the response such as protocol, status and headers.\n"
                    + "-h key:value Associates headers to HTTP Request with the format 'key:value'.";
        case "post":
            return "usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL \n"
                    + "Post executes a HTTP POST request for a given URL with inline data or from file. \n"
                    + "-v Prints the detail of the response such as protocol, status and headers. \n"
                    + "-h key:value Associates headers to HTTP Request with the format 'key:value'.\n"
                    + "-d string Associates an inline data to the body HTTP POST request.\n -f file Associates the content of a file to the body HTTP POST request.\n"
                    + "Either [-d] or [-f] can be used but not both.";
        default:
            return " The help you are trying to get does not exist";
        }
    }

    public static Boolean contains(String[] arguments, String character) {
        for (String element : arguments) {
            if (character.equals(element)) {
                return true;
            }
        }
        return false;
    }

    public static void setHeaders(String[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equalsIgnoreCase("-h")) {
                parameter.addHeader(arguments[i + 1] + "\r\n");
            }
        }
    }

    public static void setURL(String[] arguments) {

        if (parameter.isOutputToFile()) {
            parameter.setURL(arguments[arguments.length - 3]);
        } else {
            parameter.setURL(arguments[arguments.length - 1]);

        }

        parameter.setRequestType(arguments[0].toUpperCase() + " ");
    }

    public static void setRouter(String[] arguments){
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equalsIgnoreCase("-r")) {
                parameter.setRouter(arguments[i+1]);
            }
        }
    }

    public static void setBodyD(String[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equalsIgnoreCase("-d")) {
                parameter.setBody(arguments[i + 1]);
            }
        }
    }
  
    public static void setBodyF(String[] arguments) {
        int index = 0;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equals("-f")) {
                index = i + 1;
                break;
            }          
        }
        try {
            // pass the path to the file as a parameter
            File file = new File("C:\\Github\\Network\\" + arguments[index]);
            Scanner sc = new Scanner(file);
            String temp = "";

            while (sc.hasNextLine()) {
                temp += sc.nextLine();
            }
            parameter.setBody(temp);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            System.out.println(e);
            System.out.println("The file given does not exist");
            System.exit(0);
        }

    }

    public static void outputToFile(UDPClient net, String outputFile, Boolean verbose) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Github\\Network\\"+outputFile));
            if(verbose)
            {
                writer.write(net.getRes().verboseToString(false));
            }
            else {
                writer.write(net.getRes().toString());
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Writing output to file has failed");
        }
    }
    
}






   