package org.example.client;

import java.io.*;
import java.net.Socket;

public class TcpClient {
    private Socket socket;
    private PrintWriter out;
    private ObjectInputStream objectIn;
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

    public Object receiveObject() {
        if (!connected) {
            System.err.println("Not connected to server. Call connect() first.");
            return null;
        }
        
        try {
            // Create ObjectInputStream to read the object response
            objectIn = new ObjectInputStream(socket.getInputStream());
            return objectIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error receiving object: " + e.getMessage());
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
            if (objectIn != null) {
                objectIn.close();
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