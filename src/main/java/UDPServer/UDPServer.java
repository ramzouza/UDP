package UDPServer;

import Packet.*;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
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

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.acceptsAll(asList("port", "p"), "Listening port")
                .withOptionalArg()
                .defaultsTo("8007");

        OptionSet opts = parser.parse(args);
        int port = Integer.parseInt((String) opts.valueOf("port"));
        UDPServer server = new UDPServer();
        if(server.init(port))
        {
        server.listenAndServe();
        }
    }


    private void listenAndServe()  {

        try{

            for (; ; ) {
                buf.clear();

                router =new InetSocketAddress("localhost",3000); 
                channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                Packet request = Packet.fromBuffer(buf);
                buf.flip();
                switch (request.getType()) {                    
                    case DATA: 
                        processData(request);
                        break;
                    case SYN:
                        processSyn(request);
                        break;
                    case SYNACK:                       
                    case ACK:  
                        processAck(request);
                        break;
                    case NACK:  
                    
                    case FIN:  
                
                    default:
                        break;
                        //channel.close();
            //channel = null;
 
                }
            }
        }
                /*
				payload= new String(packet.getPayload(), UTF_8);


                String[] args = payload.split("\\s+");

                System.out.println("\n\n");
                for (String string : args) {
                    System.out.println(string);
                }        
				if(args[0].contains("get")) {
					// Perform GET operation
					
				}
				else if(args[0].contains("post")) {
					// Perform POST operation
					
				}
				else {
					payload="Must ask for a GET or POST request";
				}

                logger.info("Packet: {}", packet);
                logger.info("Payload: {}", payload);
                logger.info("Router: {}", router);

            //sendResponse(packet);
            }
        }
                       */
        
        catch (IOException e)
        {

        }
    }

    private boolean init(int port)
    {
        try {
            channel = DatagramChannel.open(); 
            channel.bind(new InetSocketAddress(port));
            logger.info("EchoServer is listening at {}", channel.getLocalAddress());
            buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);  
            
            return true;
        } catch (Exception e) {
            return false;
        }
        
    }
    // return final sequence number
    private long sendMessage (PacketTypes type, long sequenceNumber, InetAddress peerAddress, int peerPort, byte[] payload)    {
      
        if (payload == null || payload.length < Packet.Max_PAYLOAD) {
            
            sequenceNumber = sequenceNumber ++;
            sendMessage(new Packet (type,sequenceNumber, peerAddress , peerPort , payload));
            return sequenceNumber;
        }

        else if (payload.length == Packet.Max_PAYLOAD)
        {
            sequenceNumber = sequenceNumber ++;
            sendMessage(new Packet (type,sequenceNumber, peerAddress , peerPort , payload));
            sequenceNumber = sequenceNumber ++;
            sendMessage(new Packet (type,sequenceNumber, peerAddress , peerPort , new byte[0]));
            return sequenceNumber;                           
        }

        else if (payload.length > Packet.Max_PAYLOAD)
        {
            sequenceNumber = sequenceNumber ++;
            sendMessage(new Packet (type,sequenceNumber, peerAddress , peerPort , Arrays.copyOf(payload, Packet.Max_PAYLOAD)));
            sequenceNumber = sequenceNumber ++;
            return sendMessage(type,sequenceNumber, peerAddress , peerPort , Arrays.copyOfRange(payload, Packet.Max_PAYLOAD + 1, payload.length));
            
        }       
        return 0;
    }

    private void  sendMessage (Packet packet)
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
        if (_clients.containsKey(packet.getClientId()))
        {
            logger.info("SYN rejected client is already in communication with server ", packet);
            return ;
            // skip any other computation
        }
       
        String payload = "SYN has been acknowledged by server";  
        logger.info("SYN has been acknowledged by server ", packet);
        Packet response = new Packet (SYNACK, packet.getSequenceNumber() + 1, packet.getPeerAddress() , packet.getPeerPort(), payload.getBytes());
        _clients.put(packet.getClientId(), new ServerWorker(new ServerLock(), "rootfolder", false, packet.getClientId(), response.getSequenceNumber()));
        sendMessage(response);
    }

    private void processAck(Packet packet){
        if (!_clients.containsKey(packet.getClientId()))
        {
            logger.info("SYNACK rejected client has never started communication with server ", packet);
            return ;
            // skip any other computation
        }      
        logger.info("ACK has been acknowledged by server ", packet);
        ServerWorker temp = _clients.get(packet.getClientId());
        temp.set_type(PacketTypes.ACK);
    }
   
    private void processData(Packet packet)
    {
        if (!_clients.containsKey(packet.getClientId()))
        {
            logger.info("Client has not done a handhsake", packet);
            return ;
        }
        ServerWorker sw = _clients.get(packet.getClientId());
        if(sw.get_type() != PacketTypes.ACK )
        {
            logger.info("Client has not completed a handhsake", packet);
            return ;
        }
        
        if (packet.getPayload().length != 0)
        {
            sendMessage(PacketTypes.ACK,packet.getSequenceNumber(), packet.getPeerAddress() , packet.getPeerPort(), new byte[0]);
            //String temp = new String(packet.getPayload(), UTF_8);
            sw.appendData(packet.getPayload());
        }
        if (packet.getPayload().length != Packet.Max_PAYLOAD)
        {
            sw.Process();
            sendMessage(PacketTypes.DATA,packet.getSequenceNumber() + 1, packet.getPeerAddress() , packet.getPeerPort(), sw.get_response().verboseToString(true).getBytes());                   
        }
        

    }
    

}