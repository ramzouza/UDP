package UDPServer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Builder.GETRequestBuilder;
import Builder.POSTRequestBuilder;
import Builder.RequestBuilder;
import Packet.Packet;
import UDPClient.Response;

import protocol.receiveBuffer;
import protocol.sendBuffer;


public class ServerWorker implements Runnable  {
    public static final int CREATED = 0;
    public static final int RECEIVE_DATA = 1;
    public static final int PROCESSING = 2;
    public static final int REPLYING = 3;
    public static final int COMPLETED = 4;

    private static final Logger logger = LoggerFactory.getLogger(ServerWorker.class);

    private RequestBuilder req;
    private Response _response;
    private String _path;
    private String _rootFolder;
    private ServerLock _locks;
    private boolean _isVerbose;
    
    private String _id;
    private long _sequenceNumber;
    private byte [] _data;

    private sendBuffer _sender; 
    private receiveBuffer _receiver; 
    private int _status;

    public ServerWorker(ServerLock locks, String rootFolder, boolean isVerbose, String id, long sequenceNumber, InetAddress peerAddress, int peerPort)
    {
        this._status = ServerWorker.CREATED;
        this._locks = locks;
        this._rootFolder = rootFolder.toLowerCase();
        this._isVerbose = isVerbose;
        this._id = id;
        this._receiver = new receiveBuffer(peerAddress, peerPort, ++sequenceNumber);
    }

    public void processData(Packet packet, DatagramChannel channel, SocketAddress router)
    {
        if (this._status != ServerWorker.CREATED && this._status != ServerWorker.RECEIVE_DATA)
        {
            return;
        }

        this._status = ServerWorker.RECEIVE_DATA;

        if (this._receiver != null && this._receiver.processPacket(packet, channel, router))
        {
            //should only enter this once
            this._status = ServerWorker.PROCESSING;
            this._sequenceNumber = packet.getSequenceNumber();
            new Thread(this).start();
       }
    }

    @Override
    public void run() 
    {
        this._data = this._receiver.getPayload();
        this.processRequest();
        this._sender = new sendBuffer(this._receiver.getPeerAddress(), this._receiver.getPeerPort(), this._response.verboseToString(true).getBytes(), this._sequenceNumber);
        this._receiver = null;
        this._status = ServerWorker.REPLYING;
    }

    public void process(DatagramChannel channel, SocketAddress router)
    {
        if (this._status == ServerWorker.REPLYING && this._sender != null)
        {
            this._sender.process(channel, router);
        }
    }

    public boolean processAck(Packet ack)
    {
        if (this._sender != null && this._sender.ack(ack))
        {
            this._sender = null;
            this._status = ServerWorker.COMPLETED;
            return true;
        }

        return false;
    }

    public int getStatus ()
    {
        return this._status;
    } 

	private void processRequest()
    {
        try {
            
            InputStream inputStream = new ByteArrayInputStream(_data);         
            Scanner in = new Scanner(inputStream);

            this.evaluateFirstline( in .nextLine());
            this.req.parseRequest( in );
            this.println(this.req.toString());
 
            // process request
            this.println("starting to process the request");
            this._response = new Response(this.req);
            this._path = this.req.getUrl();
            if (this.req.getMethod().equals("GET"))
            {
                this.executeGet();
            }
            else
            {
                this.executePost();
            }
            logger.info("reponse is ***** \n\n\n " + this._response.verboseToString(true));
            
            // send response
            this.println("--- Server Response ---");
            this.println(this._response.verboseToString(true));
            
            //out.write(this._response.verboseToString(true));
            //out.close();
            this.println("\n\n Waiting for new request");    
        } catch (Exception e) {
            //TODO: handle exception
        }
    }

    private void println(String message){
        if (this._isVerbose) {
            System.out.println(message);
            }
    }

    private void evaluateFirstline(String content) {
		String[] values = content.split(" ");
		
		if (values[0].equals("GET")) {
			req = new GETRequestBuilder();
		}   
	   else if(values[0].equals("POST")){
	     req = new POSTRequestBuilder();
	   }
	   req.setURL(values[1]);
	   req.setVersion(values[2]);
    }

    private void executeGet() {
        if (!this.CheckPath())
        {
            return;
        }

        this.println("processing GET");
        
        try {
            logger.info("request path is " + this._path);
            logger.info("request path is " + Paths.get(this._path).toAbsolutePath().normalize().toString());
           File f = new File(Paths.get(this._path).toAbsolutePath().normalize().toString());
           if (!f.exists())
           {
                this._response.setCode("404");
                this._response.setPhrase("Not Found");
                return;
           }

           if (f.isDirectory())
           {
               this.ProcessDirectory(f);
           }
           else if (f.isFile())
           {
               this.ProcessFile(f);
           }
           else
           {
                this._response.setCode("400");
                this._response.setPhrase("Bad Request");
           }
        } catch (Exception e) {
            this._response.setCode("500");
            this._response.setPhrase("Internal Server Error");
        }
    }

    private void ProcessDirectory(File f)
    {
        this.println("processing GET Directory content: " + this._path);
 
        File[] allfiles = f.listFiles();
        this._response.appendEntityBody("<html><body>");
        for (File file : allfiles) {
            this._response.appendEntityBody(this.getFileEntry(file));
        }
        this._response.appendEntityBody("</body></html>");
        this._response.setFileName("list.html");
        this._response.setCode("200");
        this._response.setPhrase("OK");
    }

    private void ProcessFile(File f)
    {
        this.println("processing GET File content: " + this._path);
 
        boolean locked = false;
        try {
            if (this._locks.CanRead(this._path)){
                locked = true;
                if (f.canRead()){
                    this._response.setFileName(this._path);
                    if (this._response.IsBinaryType()){
                        byte[] data = Files.readAllBytes(Paths.get(this._path));
                        this._response.setEntityBody(Base64.getEncoder().encodeToString(data));
                    }
                    else{
                        List<String> lines =  Files.readAllLines(Paths.get(this._path), StandardCharsets.UTF_8);
                        this._response.setEntityBody(String.join(System.lineSeparator(), lines)); 
                    }
                    this._response.setCode("200");
                    this._response.setPhrase("OK");
                }
                else{
                    this._response.setCode("403");
                    this._response.setPhrase("Forbidden");
                }
            }
            else{
                this._response.setCode("403");
                this._response.setPhrase("Forbidden-Locked");
            }
        } catch (Exception e) {
            this._response.setCode("500");
            this._response.setPhrase("Internal Server Error");
        } finally{
            if (locked){
                this._locks.CompleteRead(this._path);
            }
        }
    }

    private String getFileEntry(File file)
    {
        String filePath = file.getAbsolutePath().toLowerCase();
        if (filePath.startsWith(this._rootFolder))
        {
            filePath = filePath.substring(this._rootFolder.length());
        }

        return "<a href='" + filePath + "'>" + file.getName() + "</a></br>";
    }

    private void executePost() {
        if (!this.CheckPath())
        {
            return;
        }
        
        this.println("processing POST: " + this._path);
        boolean locked = false;
        try {
           File f = new File(Paths.get(this._path).toAbsolutePath().normalize().toString());
           if (f.isDirectory())
           {
               this._response.setCode("400");
               this._response.setPhrase("Bad Request");
               return;
           }

            if (this._locks.CanWrite(this._path)){
                locked = true;
                if (f.exists() && f.isFile()){
                    if (f.canWrite()){
                        this.println("Modifing existing file");
                        Files.write(Paths.get(this._path), this.req.getEntityBody().getBytes());
                        this._response.setCode("200");
                        this._response.setPhrase("OK");
                    }
                    else{
                        this._response.setCode("403");
                        this._response.setPhrase("Forbidden");
                    }
                }
                else {
                    this.println("Creating new file");
                    Path p = Paths.get(this._path);
                    this.EnsureDirectory(p.getParent());
                    Files.write(p, this.req.getEntityBody().getBytes());
                    this._response.setCode("201");
                    this._response.setPhrase("Created");
                }
            } 
            else {
                this._response.setCode("403");
                this._response.setPhrase("Forbidden-Locked");
            }
        } catch (Exception e) {
            this._response.setCode("500");
            this._response.setPhrase("Internal Server Error");
        }
        finally{
            if (locked){
                this._locks.CompleteWrite(this._path);
            }
        }

    }

    private boolean CheckPath(){
        this.println("Checking path: " + this._path);
        if (this._path ==  null)
        {
            this._response.setCode("400");
            this._response.setPhrase("Bad Request");
            return false;
        }

        if (this._path.contains(".."))
        {
            this.println("Path contains ..");
            this._response.setCode("403");
            this._response.setPhrase("Forbidden");
            return false;
        }

        this._path = this._path.replace("/", "\\");

        Path p = Paths.get(this._path);
        if (p.isAbsolute())
        {
            this.println("Absolute paths are not allowed.");
            this._response.setCode("403");
            this._response.setPhrase("Forbidden");
            return false;
        }

        if (this._path.startsWith("\\")){
            this._path = this._rootFolder + this._path;
        }
        else {
            this._path = this._rootFolder + "\\" + this._path;
        }

        this.println("Path resolved to: " + this._path);
        return true;
    }

    public void EnsureDirectory(Path p)
    {
        if (p == null)
        {
            return;
        }

        File f =  new File(p.toAbsolutePath().normalize().toString());
        if (!f.exists())
        {
            this.EnsureDirectory(p.getParent());
            f.mkdir();
        }
    }


    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    // public long get_sequenceNumber() {
    //     return _sequenceNumber;
    // }

    // public void set_sequenceNumber(int _sequenceNumber) {
    //     this._sequenceNumber = _sequenceNumber;
    // }

    public byte [] get_data() {
        return this._data;
    }

    public Response get_response() {
        return _response;
    }

    public void set_response(Response _response) {
        this._response = _response;
    }

}
