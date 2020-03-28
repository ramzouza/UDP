package UDPClient;

import Packet.Packet;
import Packet.PacketTypes;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class UDPClient {

    private final Logger logger = LoggerFactory.getLogger(UDPClient.class);
    private DatagramChannel channel;
    private String payload;
    private SocketAddress router;
    private int reconnect = 0;

    private final int DATA = 0;
    private final int SYN = 1;
    private final int SYN_ACK = 2;
    private final int ACK = 3;
    private final int NACK = 4;
    private final int FIN = 5;
    private final int ALLOWED_RECONNECT = 10;

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.accepts("router-host", "Router hostname").withOptionalArg().defaultsTo("localhost");

        parser.accepts("router-port", "Router port number").withOptionalArg().defaultsTo("3000");

        parser.accepts("server-host", "EchoServer hostname").withOptionalArg().defaultsTo("localhost");

        parser.accepts("server-port", "EchoServer listening port").withOptionalArg().defaultsTo("8007");

        OptionSet opts = parser.parse(args);

        // Router address
        String routerHost = (String) opts.valueOf("router-host");
        int routerPort = Integer.parseInt((String) opts.valueOf("router-port"));

        // Server address
        String serverHost = (String) opts.valueOf("server-host");
        int serverPort = Integer.parseInt((String) opts.valueOf("server-port"));

        SocketAddress routerAddress = new InetSocketAddress(routerHost, routerPort);
        InetSocketAddress serverAddress = new InetSocketAddress(serverHost, serverPort);

        //runClient(routerAddress, serverAddress);
    }

    private void runClient(SocketAddress routerAddr, InetSocketAddress serverAddr)
            throws IOException {
        try {
            channel = DatagramChannel.open();

            if (!connect(routerAddr,serverAddr) && reconnect < ALLOWED_RECONNECT )
            {
                logger.info("failed to establish connection with the server");
            }
            // create packet
            String msg = "Hello World";
            Packet p = new Packet.Builder().setType(0).setSequenceNumber(1L).setPortNumber(serverAddr.getPort())
                    .setPeerAddress(serverAddr.getAddress()).setPayload(msg.getBytes()).create();
            channel.send(p.toBuffer(), routerAddr);

            logger.info("Sending \"{}\" to router at {}", msg, routerAddr);

            // Try to receive a packet within timeout.
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            logger.info("Waiting for the response");
            selector.select(5000);

            Set<SelectionKey> keys = selector.selectedKeys();
            if (keys.isEmpty()) {
                logger.error("No response after timeout");
                return;
            }

            // We just want a single response.
            ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
            SocketAddress router = channel.receive(buf);
            buf.flip();
            Packet resp = Packet.fromBuffer(buf);
            logger.info("Packet: {}", resp);
            logger.info("Router: {}", router);
            String payload = new String(resp.getPayload(), StandardCharsets.UTF_8);
            logger.info("Payload: {}", payload);

            keys.clear();
        } catch (IOException e) {

        }
    }

    private long sendMessage(PacketTypes type, long sequenceNumber, InetAddress peerAddress, int peerPort, byte[] payload) {

        if (payload == null || payload.length < Packet.Max_PAYLOAD) {
            sequenceNumber = sequenceNumber++;

            sendMessage(new Packet.Builder().setType(Packet.ToType(type)).setSequenceNumber(sequenceNumber)
                    .setPeerAddress(peerAddress).setPortNumber(peerPort).setPayload(payload).create());
            return sequenceNumber;
        }

        else if (payload.length == Packet.Max_PAYLOAD) {
            sequenceNumber = sequenceNumber++;
            sendMessage(new Packet(type, sequenceNumber, peerAddress, peerPort, payload));
            sequenceNumber = sequenceNumber++;
            sendMessage(new Packet(type, sequenceNumber, peerAddress, peerPort, new byte[0]));
            return sequenceNumber;
        }

        else if (payload.length > Packet.Max_PAYLOAD) {
            sequenceNumber = sequenceNumber++;
            sendMessage(new Packet(type, sequenceNumber, peerAddress, peerPort,
                    Arrays.copyOf(payload, Packet.Max_PAYLOAD)));
            sequenceNumber = sequenceNumber++;
            return sendMessage(type, sequenceNumber, peerAddress, peerPort,
                    Arrays.copyOfRange(payload, Packet.Max_PAYLOAD + 1, payload.length));

        }
        return 0;
    }

    private void sendMessage(Packet packet)
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
  
    private boolean connect(SocketAddress routerAddr, InetSocketAddress serverAddr)
    {
         // Try to receive a packet within timeout.
         try {

            Packet message = new Packet (SYN, 100, serverAddr.getAddress() , serverAddr.getPort(), new byte [0]);
            sendMessage(message);         

            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);
            logger.info("Waiting for the response");
            selector.select(5000);
            Set<SelectionKey> keys = selector.selectedKeys();

            if(keys.isEmpty())
            {
                return false;
            }
            else 
            {
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
                SocketAddress router = channel.receive(buf);
                buf.flip();
                keys.clear();
                message = Packet.fromBuffer(buf);

                if(Packet.getPacketType(message.getType()) != PacketTypes.SYNACK)
                {
                    return false;
                }
                else 
                {
                    sendMessage(new Packet (ACK , 100, serverAddr.getAddress() , serverAddr.getPort(), new byte [0]));
                    return true;
                }
            }

         } catch (IOException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }         
        return false;
    }

}

