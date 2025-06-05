package org.example.utils;

import org.example.TicketShop;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private Socket socket;
    private ServerSocket serverSocket;
    private DataInputStream dataInputStream;
    private TicketShop ticketShop;

    public Server(int port, TicketShop ticketShop) {
        this.ticketShop = ticketShop;
        try{
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void start() {
        try{
        socket = serverSocket.accept();
        System.out.println("Client connected: " + socket.getInetAddress());
//        dataInputStream = new DataInputStream(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("Error accepting client connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
