package org.example.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress;

public class TcpClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String host;
    private int port;
    private boolean connected;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.connected = false;
    }

    public TcpClient(int port) {
        this("localhost", port);
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            System.out.println("Connected to server at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendMessage(String message) {
        if (!connected) {
            System.err.println("Not connected to server. Call connect() first.");
            return false;
        }
        
        try {
            out.println(message);
            return true;
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String receiveMessage() {
        if (!connected) {
            System.err.println("Not connected to server. Call connect() first.");
            return null;
        }
        
        try {
            return in.readLine();
        } catch (IOException e) {
            System.err.println("Error receiving message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void disconnect() {
        if (!connected) {
            return;
        }
        
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            connected = false;
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
            e.printStackTrace();
        }
    }
}