package UDPServer;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;

import Builder.GETRequestBuilder;
import Builder.POSTRequestBuilder;
import Builder.RequestBuilder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;


public class Server {

	private int port = 8080;
	private ServerSocket server= null;
	private static Socket socket;
	static InputStreamReader inputReader;
	static BufferedReader br;
	static PrintWriter PrintWriter;
	

	public Server(int newPort, String newPath) {
		this.port = newPort;
	}

	public void initSocket() {
		try {
			if (availableAndAllowed(port)) {
				server = new ServerSocket();
				String s = "127.0.0.1";
				SocketAddress binAddress = new InetSocketAddress(s, port);
				server.bind(binAddress);
			} else {
				System.exit(0);
			}

		} catch (IOException e) {
			System.out.println("Creation of the server socket has crashed unexpedectly");
		}
	}

	private boolean availableAndAllowed(int port) {
		if (port < 1024 || port > 65535) {
			System.out.println("--------------Port " + port + " is outside the allowed range");
			return false;
		}

		System.out.println("--------------Listening on port " + port);
		Socket s = null;
		try {
			s = new Socket("localhost", port);

			// If the code makes it this far without an exception it means
			// something is using the port and has responded.
			System.out.println("--------------Port " + port + " is not available");
			return false;
		} catch (IOException e) {
			return true;
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
					throw new RuntimeException("You should handle this error.", e);
				}
			}
		}
	}

	public Socket accept() {
		try {
			return server.accept();
		} catch (Exception e) {
			// TODO: handle exception
		}

		return null;
	}

	public static Socket getSocket() {
		return socket;
	}

	public static void setSocket(Socket socket) {
		Server.socket = socket;
	}

}
