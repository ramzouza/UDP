import java.io.File;
import java.net.Socket;
import java.nio.file.Paths;
import UDPServer.ServerWorker;
import UDPServer.ServerLock;

import UDPServer.Server;

public class httpf {
    static serverArgument parameter = new serverArgument();
    static String DefaultWorkingDirectory = "C:\\Github\\Network\\Network\\Files";
    static ServerLock Locks = new ServerLock();
    public static void main(String[] args) {
//        args = new String[] { "-v" , "-p","200"};

        for (int i = 0; i < args.length; i++) {
            if (args[0].equals("help")) {
                try {
                    args[1] = args[1];
                } 
                catch (Exception e) {
                    System.out.println(help(""));
                    System.exit(0);
                }
                System.out.println(help(args[1]));
                System.exit(0);
            }
            else {
                
            }
        }


        initArgument(args);
        Server server = new Server(Integer.parseInt(parameter.getPort()), "localhost");
        server.initSocket();
        while (true) {
            Socket socket = server.accept();
            if (socket != null)
            {
                ServerWorker worker = new ServerWorker(httpf.Locks, parameter.getPath(), socket, parameter.isVerbose());
                Thread t = new Thread(worker);
                t.start();
                // worker.Process();
            }
        }

    }

    public static void initArgument(String[] args) {

        parameter.setPath(httpf.DefaultWorkingDirectory);

        if (contains(args, "-v")) {
            parameter.setVerbose(true);
        }

        if(contains(args, "-p")){
            setPort(args);
        }

        if(contains(args, "-d")){
            setPath(args);
            if (!isSecure()) {
                System.out.println("path is not secure or not found trying to access another directory");
                System.exit(0);
                // return error with bad request 
            }    
        }
    }


    public static boolean isSecure() {
        if (parameter.getPath() == null)
        {
            parameter.setPath(".");
            return true;
        }

        File wdir = new File(Paths.get(parameter.getPath()).toAbsolutePath().normalize().toString());
        if (!wdir.exists() || !wdir.isDirectory())
        {
            return false;
        }

        parameter.setPath(wdir.getPath());
        return true;
    }


    private static void setPath(String[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equalsIgnoreCase("-d")) {
                parameter.setPath(arguments[i + 1]);
                return;
            }
        }

        parameter.setPath(httpf.DefaultWorkingDirectory);
    }

    private static void setPort(String[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equalsIgnoreCase("-p")) {
                parameter.setPort(arguments[i + 1]);
                return;
            }
        }

        parameter.setPort("8080");
    }

    public static Boolean contains(String[] arguments, String character) {
        for (String element: arguments) {
            if (character.equals(element)) {
                return true;
            }
        }
        return false;
    }

    public static String help(String temp) {
        switch (temp) {
        case "":
            return "\n\nUsage: httpf [-v] [-p PORT] [-d PATH-TO-DIR] \n"  
                    + "-v Prints debugging messages\n"
                    + "-p Specifies the port number that the server will listen and serve at. Default is 8080\n"
                    + "-d Specifies the directory that the server will use to read/write requested files. Default is the current directory when launching the application" ;
        default:
                    return " The help you are trying to get does not exist";    
        }
    }

}