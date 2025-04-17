package org.example;

import java.util.Date;

public class Main {


    public static void main(String[] args) {
        EventService eventService = new EventService();
        try {
            eventService.add("Concert", "Berlin", new Date(125, 3, 18), 100);
            eventService.add("Theater", "Hamburg", new Date(125, 3, 20), 1600);
            eventService.add("Conference", "Munich", new Date(125, 3, 30), 1890);

            Event[] events = eventService.getAllEvents();
            eventService.delete(events[0].getId());
            for (Event event : events) {
                System.out.println(event);
            }

        } catch (Exception e) {
            System.out.println(e);
        }

//        PrimeNumberGenerator test = new PrimeNumberGenerator(1_000_000_000L);
//        for (int i = 0; i < 100; i++) {
//            System.out.println(test.iterator().next());
//        }
    }
}