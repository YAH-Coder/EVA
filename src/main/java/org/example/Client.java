package org.example;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Client {
    private final Scanner scanner;
    private final EventService eventService;

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
        System.out.println("(q) - quit");
    }

    private void newEvent() {
        try {
            System.out.println("Enter Event Details:");

            System.out.print("Name: ");
            String name = scanner.nextLine();

            System.out.print("Location: ");
            String location = scanner.nextLine();

            System.out.print("Day (1-31): ");
            int day = Integer.parseInt(scanner.nextLine());

            System.out.print("Month (1-12): ");
            int month = Integer.parseInt(scanner.nextLine());

            System.out.print("Year (e.g. 2025): ");
            int year = Integer.parseInt(scanner.nextLine());

            System.out.print("Number of Tickets: ");
            int nmbTickets = Integer.parseInt(scanner.nextLine());

            LocalDateTime date = LocalDateTime.of(year, month, day, 0, 0);
            Event event = eventService.add(name, location, date, nmbTickets);
            System.out.println("Event created with ID: " + event.getId());
        } catch (Exception e) {
            System.out.println("Failed to create event: " + e.getMessage());
        }
    }

    private void getEventById() {
        try {
            System.out.print("Enter ID: ");
            long id = Long.parseLong(scanner.nextLine());
            System.out.println(eventService.get(id));
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (NoSuchElementException e) {
            System.out.println(e.getMessage());
        }
    }

    private void updateEventById() {
        try {
            System.out.print("Enter ID of event to update: ");
            long id = Long.parseLong(scanner.nextLine());
            Event existing = eventService.get(id);

            System.out.println("Leave blank to keep current value.");

            System.out.printf("Name [%s]: ", existing.getName());
            String name = scanner.nextLine();
            if (name.isBlank()) name = existing.getName();

            System.out.printf("Location [%s]: ", existing.getLocation());
            String location = scanner.nextLine();
            if (location.isBlank()) location = existing.getLocation();

            System.out.printf("Year [%d]: ", existing.getDate().getYear());
            String ys = scanner.nextLine();
            int year = ys.isBlank() ? existing.getDate().getYear() : Integer.parseInt(ys);

            System.out.printf("Month [%d]: ", existing.getDate().getMonthValue());
            String ms = scanner.nextLine();
            int month = ms.isBlank() ? existing.getDate().getMonthValue() : Integer.parseInt(ms);

            System.out.printf("Day [%d]: ", existing.getDate().getDayOfMonth());
            String ds = scanner.nextLine();
            int day = ds.isBlank() ? existing.getDate().getDayOfMonth() : Integer.parseInt(ds);

            System.out.printf("Number of Tickets [%d]: ", existing.getNmbTickets());
            String ts = scanner.nextLine();
            int nmbTickets = ts.isBlank() ? existing.getNmbTickets() : Integer.parseInt(ts);

            LocalDateTime date = LocalDateTime.of(year, month, day, 0, 0);
            eventService.update(id, name, location, date, nmbTickets);
            System.out.println("Event updated.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        } catch (Exception e) {
            System.out.println("Failed to update event: " + e.getMessage());
        }
    }

    private void deleteEventById() {
        try {
            System.out.print("Enter ID of event to delete: ");
            long id = Long.parseLong(scanner.nextLine());
            eventService.delete(id);
            System.out.println("Event deleted.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (NoSuchElementException e) {
            System.out.println(e.getMessage());
        }
    }

    private void deleteAllEvents() {
        eventService.deleteAll();
        System.out.println("All events deleted.");
    }

    private void showEvents() {
        eventService.printAll();
    }

    public void start() {
        help();
        while (true) {
            System.out.print("> ");
            String action = scanner.nextLine().trim().toLowerCase();
            switch (action) {
                case "n":
                    newEvent();
                    break;
                case "g":
                    getEventById();
                    break;
                case "c":
                    updateEventById();
                    break;
                case "d":
                    deleteEventById();
                    break;
                case "a":
                    showEvents();
                    break;
                case "da":
                    deleteAllEvents();
                    break;
                case "h":
                case "help":
                case "?":
                    help();
                    break;
                case "q":
                case "quit":
                case "exit":
                    System.out.println("Exiting.");
                    return;
                default:
                    System.out.println("Unknown command. Type 'h' for help.");
            }
        }
    }
}
