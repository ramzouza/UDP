package protocol;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import Packet.Packet;
import Packet.PacketTypes;

public class receiveBuffer {
    
    private ArrayList<Packet> _packetList = new ArrayList<Packet>();
    private long _sentSequenceNumber;
    private InetAddress _peerAddress;
    private int _peerPort;

    public receiveBuffer(InetAddress peerAddress, int peerPort, long sentSequenceNumber)
    {
        this._peerAddress = peerAddress;
        this._peerPort = peerPort;
        this._sentSequenceNumber = sentSequenceNumber;
    }

    public boolean processPacket(Packet packet, DatagramChannel channel, SocketAddress router)
    {
        int index = Math.toIntExact(packet.getSequenceNumber()-this._sentSequenceNumber-1);
        if (index >= 0)
        {
            this.ensureSize(index);
            this._packetList.add(index, packet);                 
            this.sendAck(packet, channel, router);
            return this.isComplete();
        }

        return false;
    }

    public byte[] getPayload()
    {
       if (_packetList.size() == 1)
       {
           return _packetList.get(0).getPayload();
       }
       else if (_packetList.size() == 0)
       {
           return new byte[0];
       }
        int size=0;     
        
        for (Packet packet : _packetList) {
            size += packet.getPayload().length;
        }
        byte [] payload = new byte[size];
        int temp=0;
        for (Packet packet : _packetList) {
            System.arraycopy(packet.getPayload(), 0, payload, temp,packet.getPayload().length);
            temp += packet.getPayload().length;
        }
        return payload;
    }

    private boolean isComplete()
    {
        for (Packet packet : this._packetList) 
        {
            if (packet == null)
            {
                return false;
            }
        }

        if(this._packetList.get(this._packetList.size()-1).getPayload().length == Packet.Max_PAYLOAD)
        {
            return false;
        }

        return true;
    }

    private void sendAck(Packet packet, DatagramChannel channel, SocketAddress router)
    {
        try 
        {
            if (channel != null) 
            {
                Packet p = new Packet(PacketTypes.ACK, packet.getSequenceNumber(), _peerAddress, _peerPort, null); 
                channel.send(p.toBuffer(), router);
            }
        } 
        catch (Exception e) 
        {
            // TODO: handle exception
        }   
    }

    private void ensureSize(int size)
    {
        if (this._packetList.size() < size)
        {
            for (int i = 0; i < size - this._packetList.size(); i++) {
                this._packetList.add(null);
            }
        }
    }
}