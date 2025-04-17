package org.example;

import java.time.LocalDateTime;
import java.util.Scanner;

public class Client {
    Scanner scanner;
    EventService eventService;
    public Client() {
        scanner = new Scanner(System.in);
        eventService = new EventService();
    }

    private void help() {
        System.out.println("(n) - create new Event");
        System.out.println("(g) - get Event by ID");
        System.out.println("(c) - change Event by ID");
        System.out.println("(d) - delete Event by ID");
        System.out.println("(a) - show all Events");
        System.out.println("(da) - delete all Events");
        System.out.println("(h) - show help");
    }

    private void newEvent() {
        System.out.println("Event Details");
        System.out.print("Name: ");
        String name = scanner.nextLine();
        System.out.print("Location: ");
        String location = scanner.nextLine();
        System.out.print("Data - Day:  ");
        int day = scanner.nextInt();
        System.out.print("Data - Month:  ");
        int month = scanner.nextInt();
        System.out.print("Data - Year:  ");
        int year = scanner.nextInt();
        System.out.println("Number of Tickets:  ");
        int nmbTickets = scanner.nextInt();

        LocalDateTime date = LocalDateTime.of(year, month, day, 0, 0);
        eventService.add(name, location, date, nmbTickets);

        System.out.println("Event created!");
    }

    private void showEvents() {
        eventService.printAll();
    }

    public void start() {
        help();
        while (true) {
            String action = scanner.nextLine().toLowerCase();

            switch (action) {
                case "n":
                    newEvent();
                    break;
                case "a":
                    showEvents();
                    break;
                default:
                    help();
            }
        }
    }
}
