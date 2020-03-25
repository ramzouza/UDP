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
    private Dictionary<String,ServerWorker> workers ;
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

                router = channel.receive(buf);

                // Parse a packet from the received raw data.
                buf.flip();
                Packet packet = Packet.fromBuffer(buf);
                buf.flip();
                switch (packet.getPacketType()) {
                    
                    case DATA: 
                        //processData(packet);
                    case SYN:  
   
                    case SYNACK:  
                    case ACK:  
                    case NACK:  
                    case FIN:  
                
                    default:
                        break;
                }





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

            //channel.close();
            //channel = null;
        } 
        catch (IOException e)
        {

        }
    }

    // return final sequence number
    private long sendMessage (PacketTypes type, long sequenceNumber, InetAddress peerAddress, int peerPort, byte[] payload)    {
      
        if (payload == null || payload.length < Packet.Max_PAYLOAD) {
            sequenceNumber = sequenceNumber ++;
            sendMessage(new Packet(type, sequenceNumber, peerAddress, peerPort, payload));
            return sequenceNumber;
        }

        else if (payload.length == Packet.Max_PAYLOAD)
        {
            sequenceNumber = sequenceNumber ++;
            sendMessage(new Packet(type, sequenceNumber, peerAddress, peerPort, payload));
            sequenceNumber = sequenceNumber ++;
            sendMessage(new Packet(type, sequenceNumber, peerAddress, peerPort, new byte[0]));            
            return sequenceNumber;                           
        }

        else if (payload.length > Packet.Max_PAYLOAD)
        {
            sequenceNumber = sequenceNumber ++;
            sendMessage(new Packet(type, sequenceNumber, peerAddress, peerPort,  Arrays.copyOf(payload, Packet.Max_PAYLOAD)));
            sequenceNumber = sequenceNumber ++;
            sendMessage(type, sequenceNumber, peerAddress, peerPort,  Arrays.copyOf(payload, Packet.Max_PAYLOAD+1, payload.length));
            return sequenceNumber;
        }


       Packet packet = new Packet(type, sequenceNumber++, peerAddress, peerPort, payload);                   
       return 0;
        
    }

    private void  sendMessage (Packet packet)
    {
        try {
            if(this.channel != null)
            {
            Packet resp = packet.toBuilder()
                    .setPayload(payload.getBytes())
                    .create();
            channel.send(resp.toBuffer(), router);
        
            }     
        } catch (Exception e) {
            //TODO: handle exception
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

   

}