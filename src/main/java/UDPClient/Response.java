package UDPClient;

import java.nio.file.Path;
import java.nio.file.Paths;

import Builder.RequestBuilder;

public class Response {
    private String version;
    private String code;
    private String phrase;
    private String header;
    private String entityBody;
    private String fileName;
    private String contentDisposition;

    public Response(RequestBuilder req){
        this.version = req.getVersion();
        this.entityBody = "";
        this.phrase = "";
        this.header = req.getHeader();
    }
    
    public Response(String version, String code, String phrase, String header, String entityBody) {
        this.version = version;
        this.code = code;
        this.phrase = phrase;
        this.header = header;
        this.entityBody = entityBody;
    }
    
    public String verboseToString(boolean autoCreateHeaders) {
       String headers = this.header;
       if (autoCreateHeaders && this.entityBody != null && this.entityBody.length() > 0 )
       {
            headers += "Content-Type: " + this.GetContentType() + "\r\n";
            headers += "Content-Length:" + this.entityBody.length() + "\r\n";
            if (this.contentDisposition != null && this.contentDisposition.length() > 0)
            {
                headers += "Content-Disposition: " + this.contentDisposition + "\r\n";
            }
       }

        return this.version + " " + this.code + " " + this.phrase + "\r\n" +  headers  + "\r\n" + entityBody ;
    }

    @Override
    public String toString() {
       
        return  entityBody ;
    }


    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void appendHeader(String header) {
        this.header += header;
    }

    public String getEntityBody() {
        return entityBody;
    }

    public void setEntityBody(String entityBody) {
        this.entityBody = entityBody;
    }

    public void appendEntityBody(String entityBody) {
        if (this.entityBody.length() > 0)
        {
            this.entityBody += "\r\n";
        }

        this.entityBody += entityBody;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean IsBinaryType()
    {
        if(this.fileName == null || this.fileName.length() == 0)
        {
            return false;
        }

        String extension = "";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1);
        }

        switch(extension.toLowerCase())
        {
            case "txt":;
            case "htm":
            case "html": return false;
            default: return true;
        }
    }

    private String GetContentType()
    {
        if(this.fileName == null || this.fileName.length() == 0)
        {
            return "text";
        }

        fileName = Paths.get(fileName).getFileName().toString();

        String extension = "";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1);
        }

        this.contentDisposition = "inline";

        switch(extension.toLowerCase())
        {
            case "txt": return "text";
            case "htm":
            case "html": return "text/html";
            case "png": return "image/png";
            case "jpeg":
            case "jpg": return "image/jpeg";
            case "svg": return "image/svg";
            case "gif": return "image/gif";
            case "doc": 
            case "dot": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "dotx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.template";
            case "docm": return "application/vnd.ms-word.document.macroEnabled.12";
            case "dotm": return "application/vnd.ms-word.template.macroEnabled.12";
            case "xls": 
            case "xlt": 
            case "xla": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xltx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.template";
            case "xlsm": return "application/vnd.ms-excel.sheet.macroEnabled.12";
            case "xltm": return "application/vnd.ms-excel.template.macroEnabled.12";
            case "xlam": return "application/vnd.ms-excel.addin.macroEnabled.12";
            case "xlsb": return "application/vnd.ms-excel.sheet.binary.macroEnabled.12";
            case "ppt": 
            case "pot": 
            case "pps": 
            case "ppa": contentDisposition = "attachment; filename=\"" + fileName + "\"";   return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "potx": return "application/vnd.openxmlformats-officedocument.presentationml.template";
            case "ppsx": return "application/vnd.openxmlformats-officedocument.presentationml.slideshow";
            case "ppam": return "application/vnd.ms-powerpoint.addin.macroEnabled.12";
            case "pptm": return "application/vnd.ms-powerpoint.presentation.macroEnabled.12";
            case "potm": return "application/vnd.ms-powerpoint.template.macroEnabled.12";
            case "ppsm": return "application/vnd.ms-powerpoint.slideshow.macroEnabled.12";
            case "mdb": contentDisposition = "attachment; filename=\"" + fileName + "\""; return "application/vnd.ms-access";
            case "zip": contentDisposition = "attachment; filename=\"" + fileName + "\""; return "application/zip, application/octet-stream";
            default: contentDisposition = "attachment; filename=\"" + fileName + "\""; return "application";
        }
    }
}