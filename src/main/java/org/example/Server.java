package org.example;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private Socket socket;
    private ServerSocket serverSocket;
    private DataInputStream dataInputStream;
    private  TicketShopStringReader ticketShopStringReader;
    private Thread serverThread;
    private boolean running;

    public Server(int port, TicketShopStringReader ticketShopStringReader) {
        this.ticketShopStringReader = ticketShopStringReader;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void start() {
        if (serverThread == null || !serverThread.isAlive()) {
            running = true;
            serverThread = new Thread(this);
            serverThread.start();
            System.out.println("Server thread started");
        } else {
            System.out.println("Server is already running");
        }
    }

    public void stop() {
        running = false;
        if (serverThread != null) {
            serverThread.interrupt();
            System.out.println("Server thread stopped");
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server connections: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                System.out.println("Waiting for client connections...");
                socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                
                String line = reader.readLine();
                System.out.println("Received from client: " + line);
                
                String response = ticketShopStringReader.execute(line);
                
                writer.println(response);
                System.out.println("Sent to client: " + response);
            } catch (IOException e) {
                if (!running) {
                    break;
                }
                System.err.println("Error handling client connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}