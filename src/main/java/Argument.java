
public class Argument {
    private String requestType;
    private String header;
    private String URL;
    private String body;
    private boolean verbose;
    private boolean data;
    private boolean file;
    private boolean outputToFile;    

    Argument() {
        this.verbose = false;
        this.requestType = "";
        this.URL = "";
        this.header = "";
        this.data = false;
        this.file = false;
        this.outputToFile = false;  
           }
   
    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getHeader() {
        return header;
    }

    public void addHeader(String header) {
        this.header += header;
    }



    public String getURL() {
        return URL;
    }

    public void setURL(String uRL) {
        URL = uRL;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isData() {
        return data;
    }

    public void setData(boolean data) {
        this.data = data;
    }

    public boolean isFile() {
        return file;
    }

    public void setFile(boolean file) {
        this.file = file;
    }

    public boolean isOutputToFile() {
        return outputToFile;
    }

    public void setOutputToFile(boolean outputToFile) {
        this.outputToFile = outputToFile;
    }

}

