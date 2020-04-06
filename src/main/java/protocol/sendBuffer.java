package protocol;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;

import Packet.Packet;
import Packet.PacketTypes;

public class sendBuffer {
    
    private ArrayList<Packet> _packetList = new ArrayList<Packet>();
    private long _lastTime = 0; // System.currentTimeMillis();
    private final long timeOut = 5000L;
    private long sequenceNumber;
    
    public sendBuffer(InetAddress peerAddress, int peerPort, byte[] payload, long initSequenceNumber) 
    {
        sequenceNumber = initSequenceNumber;
        while(true)
        {
            if (payload == null || payload.length < Packet.Max_PAYLOAD) 
            {
                this._packetList.add(new Packet(PacketTypes.DATA, ++sequenceNumber, peerAddress, peerPort, payload));
                return;
            }
            else if (payload.length == Packet.Max_PAYLOAD) {
                this._packetList.add(new Packet(PacketTypes.DATA, ++sequenceNumber, peerAddress, peerPort, payload));
                this._packetList.add(new Packet(PacketTypes.DATA, ++sequenceNumber, peerAddress, peerPort, new byte[0]));
                return;
            }
            else { // if (payload.length > Packet.Max_PAYLOAD)
                this._packetList.add(new Packet(PacketTypes.DATA, ++sequenceNumber, peerAddress, peerPort,Arrays.copyOf(payload, Packet.Max_PAYLOAD)));
                payload = Arrays.copyOfRange(payload, Packet.Max_PAYLOAD + 1, payload.length-1);    
            }
        } 
    }
    
    public void process(DatagramChannel channel, SocketAddress router)
    {
        if (System.currentTimeMillis() - this._lastTime > timeOut)
        {   
            for (Packet packet : this._packetList) 
            {
                this.sendMessage(packet, channel, router);
            }     

            this._lastTime = System.currentTimeMillis(); 
        }
    }

    public long getSequenceNumber()
    {
        return this.sequenceNumber;
    }

    public boolean ack(Packet ack)
    {
        for (Packet packet : this._packetList) 
        {
            if (packet.getSequenceNumber() == ack.getSequenceNumber())
            {
                this._packetList.remove(packet);
                break;
            }
        }

        return this.isComplete();
    }

    private boolean isComplete()
    {
        return this._packetList.size() == 0;
    }

    private void sendMessage( Packet packet, DatagramChannel channel,SocketAddress router)
    {
        try {
                if (channel != null) {
                    channel.send(packet.toBuffer(), router);
                }
            } catch (Exception e) {
                // TODO: handle exception
        }   
    }
}