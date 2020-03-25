package Builder;



public class POSTRequestBuilder extends RequestBuilder {
   
    public POSTRequestBuilder() {}

    
    public POSTRequestBuilder(String URL, String header, String entityBody) {
        super(URL, header, entityBody);
        
    }

@Override
    public String getMethod()
    {
        return "POST";
    }


public Boolean verifyRequest(){
        
        // THIS MUST BE CODED
        return true;
    }

    @Override
    public String toString() {

        String headers = this.header;
        if (headers != null && !headers.contains("Content-Length:") && this.entityBody != null && this.entityBody.length() > 0 )
          {
            headers += "Content-Length: " + entityBody.length() + "\r\n";
          }
        return "POST " + this.URL + " " + this.version + "\r\n" +  headers +"\r\n" + this.entityBody + "\r\n";
    } 
 }