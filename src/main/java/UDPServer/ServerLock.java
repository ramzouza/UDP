package UDPServer;

import java.util.Dictionary;
import java.util.Hashtable;

public class ServerLock {
    
    // 1 - read operation
    // -1 - write operation
    Hashtable<String, Integer> _files = new Hashtable<String, Integer>();
    
    public ServerLock()
    {
        this._files.put("C:\\Github\\Network\\Network\\Files\\reader.txt".toLowerCase(), 2);
        this._files.put("C:\\Github\\Network\\Network\\Files\\writer.txt".toLowerCase(), -1);
    }

    public boolean CanRead(String fileName) 
    { 
        fileName = fileName.toLowerCase();
        synchronized(this) 
        { 
            if (this._files.containsKey(fileName))
            {
                int status = this._files.get(fileName);
                if (status > 0)
                {
                    // others are reading go ahead.
                    this._files.put(fileName, status + 1);
                    return true;
                }
                
                if (status == -1)
                {
                    // write is progressing
                    return false;
                }
            }

            this._files.put(fileName, 1);
            return true;
        } 
    } 

    public void CompleteRead(String fileName) 
    { 
        fileName = fileName.toLowerCase();
        synchronized(this) 
        { 
            if (this._files.containsKey(fileName))
            {
                int status = this._files.get(fileName);
                if (status > 1) {
                    this._files.put(fileName, status - 1);
                } else if (status == 1){
                    this._files.remove(fileName);
                }
            }
        } 
    } 

    public boolean CanWrite(String fileName) 
    { 
        fileName = fileName.toLowerCase();
        synchronized(this) 
        { 
            if (this._files.containsKey(fileName))
            {
                // others are reading or writting
                return false;
            }

            this._files.put(fileName, -1);
            return true;
        } 
    } 

    public void CompleteWrite(String fileName) 
    { 
        fileName = fileName.toLowerCase();
        synchronized(this) 
        { 
            if (this._files.containsKey(fileName))
            {
                int status = this._files.get(fileName);
                if (status == -1) {
                    this._files.remove(fileName);
                }
            }
        } 
    } 

}
