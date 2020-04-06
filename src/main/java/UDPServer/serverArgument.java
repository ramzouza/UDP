package UDPServer;

import Builder.*;


public class serverArgument {
    private boolean verbose;
    private String port;
    private String path;

    public serverArgument() {
        this.verbose = false;
        this.port = "8007";
        this.path = "/";
    }
   
    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getPort(){
        return port;
    }

    public void setPort(String newPort)
    {
        this.port=newPort;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


}

