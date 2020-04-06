package UDPServer;

import Packet.*;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import protocol.receiveBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;



public class UDPServer {

    private final int DATA = 0;
    private final int SYN = 1;
    private final int SYNACK = 2;
    private final int ACK = 3;
    private final int NACK = 4;
    private final int FIN = 5;

    private static final Logger logger = LoggerFactory.getLogger(UDPServer.class);

    String payload;
    private DatagramChannel channel;
    private SocketAddress router;
    Hashtable<String, ServerWorker> _clients = new Hashtable<String, ServerWorker>();
    
    ByteBuffer buf;
    
    private int port;
    private serverArgument _args;

    public UDPServer(int newPort, serverArgument args)
    {
        this.port = newPort;
        this._args = args;
    }

   
    public void listenAndServe()  
    {
        router = new InetSocketAddress("localhost", 3000); 

        for (; ; ) 
        {
            Packet request = receiveBuffer.ReadNextPacket(channel, logger); 
            
            if(request != null)
            {
                if(request.getSequenceNumber() == 5000)
            {
                System.out.println("stop");
            }
                switch (request.getType()) {                    
                    case SYN:
                        processSyn(request);
                        break;
                    case DATA: 
                        processData(request);
                        break;
                    case ACK:  
                        processAck(request);
                        break; 
                    case FIN:
                        processFin(request);  
                        break;
                    default:
                        break;
                }
        }
        
            this.processResend();
        }
    }

    public boolean init(int port)
    {
        try 
        {
            channel = DatagramChannel.open(); 
            channel.bind(new InetSocketAddress(port));
            logger.info("EchoServer is listening at {}", channel.getLocalAddress());
            buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);             
            return true;
        } 
        catch (Exception e) 
        {
            return false;
        }
        
    }
    // return final sequence number

    private void sendMessage (Packet packet)
    {
        try {
            if(this.channel != null)
            {
            channel.send(packet.toBuffer(), router);
            }     
        } catch (Exception e) {
            //TODO: handle exception
        }
       
    }   

    private void processSyn(Packet packet){
        logger.info("processing SYN ", packet);
        if (packet.getSequenceNumber() != 100)
        {
            logger.info("SYN rejected sequence number is not 100 ", packet);
            return ;
        }

        if (_clients.containsKey(packet.getClientId()))
        {
            if(_clients.get(packet.getClientId()).getStatus() != ServerWorker.CREATED)
            {
                logger.info("SYN rejected client is already in communication with server ", packet);
                return ;
            }
            // skip any other computation
        }

        String payload = "SYN has been acknowledged by server";  
        logger.info("SYN has been acknowledged by server ", packet);
        Packet response = new Packet (SYNACK, packet.getSequenceNumber(), packet.getPeerAddress() , packet.getPeerPort(), payload.getBytes());
        sendMessage(response);

        if (!_clients.containsKey(packet.getClientId()))
        {
            this._clients.put(packet.getClientId(), 
                        new ServerWorker(new ServerLock(), this._args.getPath(), false, packet.getClientId(), response.getSequenceNumber(),
                                                            packet.getPeerAddress(), packet.getPeerPort()));
        }
    }

    private void processData(Packet packet)
    {
        logger.info("processing Data ", packet);
        if (!this._clients.containsKey(packet.getClientId()))
        {
            logger.info("Client has not done a handhsake", packet);
            return ;
        }

        this._clients.get(packet.getClientId()).processData(packet, channel, router);
    }
    
    private void processAck(Packet packet){
        logger.info("Processing Ack ", packet);
        if (!_clients.containsKey(packet.getClientId()))
        {
            logger.info("SYNACK rejected client has never started communication with server ", packet);
            return ;
            // skip any other computation
        }   
        if (packet.getSequenceNumber() == 101)
        {
            logger.info("ACK has been acknowledged by server ", packet);
            return ;
        }
        else
        {
            this._clients.get(packet.getClientId()).processAck(packet);
        }
    }

    private void processFin(Packet packet)
    {
        logger.info("Processing FIN ", packet);               

        Packet response = new Packet (ACK, packet.getSequenceNumber(), packet.getPeerAddress() , packet.getPeerPort(), new byte[0]);
        sendMessage(response);
        if (_clients.containsKey(packet.getClientId()))
        {
            this._clients.remove(packet.getClientId());
        }
        logger.info("FIN has been acknowledged by server ", packet);
        return ;         
    }

    private void processResend()
    {
        for (ServerWorker worker : this._clients.values()) {
            worker.process(channel, router);
        }   
    }

    public int getPort()
    {
        return this.port;
    }
}