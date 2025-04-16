package org.example;

import java.util.Date;

public class Main {

    private static Event event1 = new Event("Event1", "Uni Leipzig", new Date(125, 3, 10), 10);

    public static void main(String[] args) {
        System.out.println(event1.getName());
        System.out.println(event1.getLocation());
        System.out.println(event1.getDate());
        System.out.println(event1.getNmbTickets());
        System.out.println(event1);
    }
}