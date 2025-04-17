package org.example;

import java.util.Date;

public class Main {


    public static void main(String[] args) {
        EventService eventService = new EventService();
        try {
            Event event1 = new Event(1, "Concert", "Berlin", new Date(125, 3, 18), 10);
            Event event2 = new Event(2, "Theater", "Hamburg", new Date(125, 3, 20), 10);
            Event event3 = new Event(3, "Conference", "Munich", new Date(125, 3, 30), 10);

            eventService.add(event1);
            eventService.add(event2);
            eventService.add(event3);

            eventService.get(1).setName("Cinema");
            eventService.get(2).setLocation("Leipzig");
            eventService.delete(3);

            System.out.println(eventService.get(1));
            System.out.println(eventService.get(2));
        } catch (Exception e) {
            System.out.println(e);
        }

        PrimeNumberGenerator test = new PrimeNumberGenerator(1_000_000_000);
        for (int i = 0; i < 100; i++) {
            System.out.println(test.iterator().next());
        }
    }
}