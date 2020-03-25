package Builder;

import java.util.Scanner;

public class GETRequestBuilder extends RequestBuilder {
   
    public GETRequestBuilder() {}


   public GETRequestBuilder(String URL, String header, String entityBody) {
        super(URL, header, entityBody);
        // TODO Auto-generated constructor stub
    }
 
    @Override
    public String getMethod()
    {
        return "GET";
    }

    @Override
    public void parseRequest(Scanner in){
       
        String header = "";

        while ( in .hasNextLine()) {
        String temp = (String)in.nextLine();   
      
        if (temp.equals(""))
        {
                this.header = header;
                this.entityBody= "";
                return ;
        }
        else{
            header += temp + "\r\n";
        }
        
       	
    }
}


}