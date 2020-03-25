package Builder;

import java.util.Scanner;
import java.net.URL;

public abstract class RequestBuilder {
    protected String URL;
    protected String version = "HTTP/1.0"; 
    protected String header = "";
    protected String entityBody = "";

    protected RequestBuilder() {
    }
    
    protected RequestBuilder(String URL, String header, String entityBody){
        try
        {
            URL url = new URL(URL);
            this.URL = url.getFile();

            this.header = header;
            this.entityBody = entityBody;
        }
        catch(Exception e)
        {
        }
    }

	public Boolean verifyRequest(){       
        return true;
    }
    @Override
    public String toString() {
        return this.getMethod() + " " + this.URL + " " + this.version + "\r\n" +  this.header + "\r\n" + this.entityBody + "\r\n";
    }

    public void parseRequest(Scanner in){
       
        String header = "";
        String entity = "";
        int contentLength = 0;

        while ( in .hasNextLine()) {
        String temp = (String)in.nextLine();   
      
        if (temp.equals(""))
        {
                this.header = header;
                while ( in.hasNextLine()) {
                    entity += (String)in.nextLine() + "\r\n";
                    if (entity.length()>=contentLength)
                    {
                        this.entityBody = entity;
                        return;
                    }
                }
        }
        else{
            header += temp + "\r\n";
            if (temp.startsWith("Content-Length:"))
            {
                contentLength = Integer.parseInt(temp.substring(15).trim());
            }
        }
        
       	
    }
}
    public void setURL(String uRL) {
        URL = uRL;
    }
    
    public String getUrl()
    {
        return this.URL;
    }

    public String getEntityBody() {
        return this.entityBody;
    }


    public abstract String getMethod();

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getVersion()
    {
        return this.version;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
