package UDPClient;

import Packet.Packet;
import Packet.PacketTypes;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import protocol.receiveBuffer;
import protocol.sendBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Builder.RequestBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;
 


import static java.nio.channels.SelectionKey.OP_READ;

public class UDPClient {

    private final static Logger logger = LoggerFactory.getLogger(UDPClient.class);
    private static DatagramChannel channel;
    private static String payload;
    private static SocketAddress router;
    InetSocketAddress serverAddress;
    private static int reconnect = 0;

    private final int DATA = 0;
    private final static int SYN = 1;
    private final int SYN_ACK = 2;
    private final static int ACK = 3;
    private final int NACK = 4;
    private final int FIN = 5;
    private final static int ALLOWED_RECONNECT = 10;

    private String routerHost = "localhost";
    private int routerPort = 3000;
    private String serverHost = "localhost";
    private int serverPort = 8007;

    RequestBuilder request;
    private String sAddress;
    private String rAddress;
    private long currentSequenceNumber=100;
    private Response res;

    public UDPClient(RequestBuilder req, String sv, String rt) {
        this.request = req;
        this.sAddress = sv;
        this.rAddress = rt;
    }

    public void request() {
             
        verifyURL();

        router = new InetSocketAddress(routerHost, routerPort);
        serverAddress = new InetSocketAddress(serverHost, serverPort);

        runClient(router, serverAddress);
 
    }

    private void runClient(SocketAddress routerAddr, InetSocketAddress serverAddr) {
        try {
            channel = DatagramChannel.open();

            if (!connect(serverAddr) && reconnect < ALLOWED_RECONNECT) {
                logger.info("failed to establish connection with the server");
                return;
            }
            
            sendBuffer sender = new sendBuffer(serverAddress.getAddress(), serverPort, request.toString().getBytes());
            receiveBuffer receiver = new receiveBuffer(serverAddress.getAddress(), serverPort, sender.getSequenceNumber());

            while(true)
            {
                if (sender != null)
                {
                    sender.process(channel, router);
                }

                Packet p = this.retrievePacket();
                if (p != null)
                {
                    if (p.getPacketType() == PacketTypes.ACK && sender != null)
                    {
                        if (sender.ack(p))
                        {
                            sender = null;
                        }
                    }
                    else if (p.getPacketType() == PacketTypes.DATA)
                    {
                        if (receiver.processPacket(p, channel, router))
                        {
                            InputStream inputStream = new ByteArrayInputStream(receiver.getPayload());         
                            Scanner in = new Scanner(inputStream);
                            buildResponse(in);
                            break;
                        }
                    }
                } 
            }

        } catch (IOException e) {

        }
    }

    private void sendMessage(Packet packet) {
        try {
            if (channel != null) {
                channel.send(packet.toBuffer(), router);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

    }
    //handshake
    private boolean connect(InetSocketAddress serverAddr) 
    {
        // Try to receive a packet within timeout.
       
            Packet message = new Packet(SYN, currentSequenceNumber, serverAddr.getAddress(), serverAddr.getPort(), new byte[0]);
            sendMessage(message);

            Packet returned = retrievePacket();

                if (returned == null || Packet.getPacketType(returned.getType()) != PacketTypes.SYNACK) 
                {
                    return false;
                } 
                else 
                {
                    currentSequenceNumber = currentSequenceNumber +1;
                    sendMessage(new Packet(ACK, currentSequenceNumber, serverAddr.getAddress(), serverAddr.getPort(), new byte[0]));
                    return true;
                }
    }

    private boolean endConnection(InetSocketAddress serverAddr)
    {
        Packet message = new Packet(FIN, currentSequenceNumber, serverAddr.getAddress(), serverAddr.getPort(), new byte[0]);
        sendMessage(message);

        Packet returned = retrievePacket();

            if (returned == null || Packet.getPacketType(returned.getType()) != PacketTypes.ACK) 
            {
                return false;
            } 
            else 
            {
                //end the connection
                return true;
            }
    }

    private void verifyURL() {
         
        try {            
        URL sURL = new URL(sAddress);          
        if (sURL.getHost() != null && !sURL.getHost().isEmpty())
        {
            this.serverHost = sURL.getHost(); 
        }
        if (sURL.getPort() > 0 )
        {
            this.serverPort = sURL.getPort(); 
        }
        }
        catch(MalformedURLException e) {

        }
        try{
        URL rURL = new URL (rAddress);
        if (rURL.getHost() != null && !rURL.getHost().isEmpty())
        {
            this.routerHost = rURL.getHost(); 
        }
        if (rURL.getPort() > 0 )
        {
            this.routerPort = rURL.getPort(); 
        }     
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
        }
        
    }

    public void buildResponse (Scanner in){

        if(in.hasNext() && in.hasNextLine())
        {
            evaluateFirstline(in.nextLine());
        }
        String header = "";
        String entity = "";

        while ( in .hasNextLine()) {
        String temp = (String)in.nextLine();   
      
        if (temp.equals(""))
        {
                res.setHeader(header);
                while ( in .hasNextLine()) {
                    entity += (String)in.nextLine() + "\r\n";
                }
                
                res.setEntityBody(entity);
        }
        else{
            header += temp + "\r\n";
        }
        
           
    }
   
}

    public void evaluateFirstline (String content)
    {
        String [] values = content.split(" ");
        String phrase = "";
        if (values.length > 2)
        {
            phrase = values[2];
        }

        for (int i = 3; i < values.length; i++) {
            phrase += " " + values[i];
        }

        res = new Response(values[0], values[1], phrase, "", "");
    }

    public Response getRes() {
        return this.res;
    }

    public void setRes(Response res) {
        this.res = res;
    }

    private Packet retrievePacket()
    {
        try {
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            logger.info("Waiting for the response");
            selector.select(5000);
            Set<SelectionKey> keys = selector.selectedKeys();
    
            if (keys.isEmpty()) 
            {
                return null;
            } 
            else 
            {
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                channel.receive(buf);
                buf.flip();
                keys.clear();
                return  Packet.fromBuffer(buf);
            } 
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;    
    }
}

