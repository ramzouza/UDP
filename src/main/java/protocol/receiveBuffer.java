package protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Packet.Packet;
import Packet.PacketTypes;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import protocol.receiveBuffer;
import protocol.sendBuffer;

import Builder.RequestBuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


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
        int index = Math.toIntExact(packet.getSequenceNumber() - this._sentSequenceNumber - 1);
        if (index >= 0)
        {
            this.ensureSize(index + 1);
            this._packetList.set(index, packet);                 
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
                Packet p = new Packet(PacketTypes.ACK, packet.getSequenceNumber(), _peerAddress, _peerPort, new byte [0]); 
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

    public InetAddress getPeerAddress() {
        return _peerAddress;
    }

    public int getPeerPort() {
        return _peerPort;
    }

    public static Packet ReadNextPacket(DatagramChannel channel, Logger logger)
    {
        try {
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector,1); // OP_READ);
            //logger.info("Waiting for a packet");
            selector.select(500);
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