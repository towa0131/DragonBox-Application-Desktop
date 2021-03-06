package main.java.Utils;

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class PEQuery {
    final static byte HANDSHAKE = 9;
    final static byte STAT = 0;
    private BasicStat bs;
    private FullStat fs;
    public int timeout=5000;

    String serverAddress = "localhost";
    int queryPort = 25565; // the default minecraft query port

    int localPort = 25566; // the local port we're connected to the server on

    private DatagramSocket socket = null; // prevent socket already bound
    // exception
    private int token;

    public PEQuery(String address, int port) {
        serverAddress = address;
        queryPort = port;
    }

    public boolean hand(String result) {
        Request req = new Request();
        req.type = HANDSHAKE;
        req.sessionID = generateSessionID();
        int val = 11 - req.toBytes().length; // should be 11 bytes total
        byte[] input = Utils.padArrayEnd(req.toBytes(), val);

        if (sendUDP(input)!=null) {
            return true;
        } else {
            return false;
        }
    }

	private void handshake() {
		Request req = new Request();
		req.type = HANDSHAKE;
		req.sessionID = generateSessionID();

		int val = 11 - req.toBytes().length;
		byte[] input = Utils.padArrayEnd(req.toBytes(), val);
		byte[] result = sendUDP(input);
		token = Integer.parseInt(new String(result).trim());
	}

	public FullStat fullStat() {
		long t1=System.currentTimeMillis();
		handshake();
		t1 = System.currentTimeMillis() - t1;

		Request req = new Request();
		req.type = STAT;
		req.sessionID = generateSessionID();
		req.setPayload(token);
		req.payload = Utils.padArrayEnd(req.payload, 4);

		byte[] send = req.toBytes();

		byte[] result = sendUDP(send);

		return new FullStat(result);
	}

    public byte[] sendUDP(byte[] input) {
        try {
            while (socket == null) {
                try {
                    socket = new DatagramSocket(localPort); // create the socket
                } catch (BindException e) {
                    ++localPort; // increment if port is already in use
                }
            }

            // create a packet from the input data and send it on the socket
            InetAddress address = InetAddress.getByName(serverAddress);
            DatagramPacket packet1 = new DatagramPacket(input, input.length,
                    address, queryPort);
            socket.send(packet1);

            // receive a response in a new packet
            byte[] out = new byte[1024 * 100]; // TODO guess at max size
            DatagramPacket packet = new DatagramPacket(out, out.length);
            socket.setSoTimeout(timeout); // one half second timeout
            socket.receive(packet);

            return out;
        } catch (SocketException e) {
            return null;
        } catch (SocketTimeoutException e) {
            return null;
        } catch (UnknownHostException e) {
            return null;
        } catch (Exception e) // any other exceptions that may occur
        {
            return null;
        }
    }

    private int generateSessionID() {
		/*
		 * Can be anything, so we'll just use 1 for now. Apparently it can be
		 * omitted altogether. TODO: increment each time, or use a random int
		 */
        return 1;
    }

    @Override
    public void finalize() {
        socket.close();
    }
}