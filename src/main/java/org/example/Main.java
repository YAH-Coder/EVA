package org.example;

import org.example.client.CLIClient;
import org.example.client.PerformanceClient;
import org.example.client.TcpClient;

public class Main {

    public static void main(String[] args) {
        TicketShopStringReader ticketShopStringReader = new TicketShopStringReader();
        Server server = new Server(8080, ticketShopStringReader);
        server.start();

//        TcpClient tcpClient = new TcpClient("172.24.130.151",8080);
//        tcpClient.connect();
//        tcpClient.sendMessage("ce;Event1;Location1;2026-10-01T10:00:00;100");
//        System.out.println(tcpClient.receiveMessage());
//        tcpClient.disconnect();

        TicketShopInterface ticketShopClient = new TicketShopClient(8080);
//        CLIClient CLIClient = new CLIClient(ticketShopClient);
//        CLIClient.start();

        long start = System.currentTimeMillis();
        PerformanceClient performanceClient = new PerformanceClient(ticketShopClient);
        performanceClient.createEvents(100, 1000);
        performanceClient.createCustomers(1000);
        performanceClient.buyTickets(1);
        performanceClient.createEvents(100, 2000);
        performanceClient.buyTickets(2);
        long end = System.currentTimeMillis();
        System.out.println("Total time: " + (end - start) + "ms");

        server.stop();

//        long start = System.currentTimeMillis();
//        PerformanceClientParallel performanceClientParallel = new PerformanceClientParallel(ticketShop);
//        performanceClientParallel.createEvents(100, 1000);
//        performanceClientParallel.createCustomers(1000);
//        performanceClientParallel.buyTickets(1);
//        performanceClientParallel.createEvents(100, 2000);
//        performanceClientParallel.buyTickets(2);
//        long endParallel = System.currentTimeMillis();
//        System.out.println("Total time parallel: " + (endParallel - start) + "ms");

//        long start = System.currentTimeMillis();
//        PerformanceClientParallelVirtualThreads performanceClientParallelVirtualThreads = new PerformanceClientParallelVirtualThreads(ticketShop);
//        performanceClientParallelVirtualThreads.createEvents(100, 1000);
//        performanceClientParallelVirtualThreads.createCustomers(1000);
//        performanceClientParallelVirtualThreads.buyTickets(1);
//        performanceClientParallelVirtualThreads.createEvents(100, 2000);
//        performanceClientParallelVirtualThreads.buyTickets(2);
//        long endParallelVirtualThreads = System.currentTimeMillis();
//        System.out.println("Total time parallel virtual threads: " + (endParallelVirtualThreads - start) + "ms");

//        TicketShopStringReader ticketShopStringReader = new TicketShopStringReader();
//        ticketShopStringReader.execute("ce;Event1;Location1;2026-10-01T10:00:00;100;5");
//        ticketShopStringReader.execute("c;Event2;Location2;2026-10-02T10:00:00;200");
//        ticketShopStringReader.execute("ce;Event3;Location3;2026-10-03T10:00:00;300");
//        ticketShopStringReader.execute("ce;Event4;Location4;2026-10-04T10:00:00;400");
    }
}