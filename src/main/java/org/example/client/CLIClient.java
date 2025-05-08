package org.example.client;

import org.example.TicketShop;
import org.example.customer.Customer;
import org.example.customer.CustomerServiceInterface;
import org.example.event.Event;
import org.example.event.EventServiceInterface;
import org.example.ticket.Ticket;
import org.example.ticket.TicketServiceInterface;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class CLIClient {
    private final Scanner scanner;
    private final EventServiceInterface eventService;
    private final CustomerServiceInterface customerService;
    private final TicketServiceInterface ticketService;

    public CLIClient(TicketShop ticketShop) {
        scanner = new Scanner(System.in);
        eventService = ticketShop.getEventServiceInterface();
        customerService = ticketShop.getCustomerServiceInterface();
        ticketService = ticketShop.getTicketServiceInterface();
    }

    private void help() {
        helpEvent();
        helpCustomer();
        helpTicket();
        helpGeneral();
    }

    private void helpEvent() {
        System.out.println("(n) - create new Event");
        System.out.println("(g) - get Event by ID");
        System.out.println("(c) - change Event by ID");
        System.out.println("(d) - delete Event by ID");
        System.out.println("(a) - show all Events");
        System.out.println("(da) - delete all Events");
    }

    private void helpCustomer() {
        System.out.println("(nc) - create new Customer");
        System.out.println("(gc) - get Customer by ID");
        System.out.println("(cc) - change Customer by ID");
        System.out.println("(dc) - delete Customer by ID");
        System.out.println("(ac) - show all Customers");
        System.out.println("(dac) - delete all Customers");
    }

    private void helpTicket() {
        System.out.println("(nt) - create new Ticket");
        System.out.println("(gt) - get Ticket by ID");
        System.out.println("(dt) - delete Ticket by ID");
        System.out.println("(gta) - show all Tickets");
        System.out.println("(dta) - delete all Tickets");
    }

    private void helpGeneral() {
        System.out.println("(h) - show help");
        System.out.println("(he) - show event help");
        System.out.println("(hc) - show customer help");
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

    private void newCustomer() {
        try {
            System.out.println("Enter Customer Details:");
            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("e-mail: ");
            String eMail = scanner.nextLine();
            System.out.print("Day (1-31): ");
            int day = Integer.parseInt(scanner.nextLine());
            System.out.print("Month (1-12): ");
            int month = Integer.parseInt(scanner.nextLine());
            System.out.print("Year (e.g. 2025): ");
            int year = Integer.parseInt(scanner.nextLine());
            LocalDateTime date = LocalDateTime.of(year, month, day, 0, 0);
            Customer customer = customerService.add(username, eMail, date);
            System.out.println("Customer created with ID: " + customer.getId());
        } catch (Exception e) {
            System.out.println("Failed to create customer: " + e.getMessage());
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

    private void getCustomerById() {
        try {
            System.out.print("Enter ID: ");
            long id = Long.parseLong(scanner.nextLine());
            System.out.println(customerService.get(id));
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

    private void updateCustomerById() {
        try {
            System.out.print("Enter ID of customer to update: ");
            long id = Long.parseLong(scanner.nextLine());
            Customer existing = customerService.get(id);
            System.out.println("Leave blank to keep current value.");
            System.out.printf("Name [%s]: ", existing.getUsername());
            String name = scanner.nextLine();
            if (name.isBlank()) name = existing.getUsername();
            System.out.printf("e-mail [%s]: ", existing.getEmail());
            String eMail = scanner.nextLine();
            if (eMail.isBlank()) eMail = existing.getEmail();
            System.out.printf("Year [%d]: ", existing.getBirthday().getYear());
            String ys = scanner.nextLine();
            int year = ys.isBlank() ? existing.getBirthday().getYear() : Integer.parseInt(ys);
            System.out.printf("Month [%d]: ", existing.getBirthday().getMonthValue());
            String ms = scanner.nextLine();
            int month = ms.isBlank() ? existing.getBirthday().getMonthValue() : Integer.parseInt(ms);
            System.out.printf("Day [%d]: ", existing.getBirthday().getDayOfMonth());
            String ds = scanner.nextLine();
            int day = ds.isBlank() ? existing.getBirthday().getDayOfMonth() : Integer.parseInt(ds);
            LocalDateTime date = LocalDateTime.of(year, month, day, 0, 0);
            customerService.update(id, name, eMail, date);
            System.out.println("Customer updated.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        } catch (Exception e) {
            System.out.println("Failed to update customer: " + e.getMessage());
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

    private void deleteCustomerById() {
        try {
            System.out.print("Enter ID of customer to delete: ");
            long id = Long.parseLong(scanner.nextLine());
            customerService.delete(id);
            System.out.println("Customer deleted.");
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

    private void deleteAllCustomers() {
        customerService.deleteAll();
        System.out.println("All customers deleted.");
    }

    private void showEvents() {
        Event[] events = eventService.getAll();
        for (Event event : events) {
            System.out.println(event);
        }
    }

    private void showCustomers() {
        Customer[] customers = customerService.getAll();
        for (Customer customer : customers) {
            System.out.println(customer);
        }
    }

    private void newTicket() {
        try {
            System.out.println("Enter customer Id: ");
            long customerId = Long.parseLong(scanner.nextLine());
            System.out.println("Enter customer Id: ");
            long eventId = Long.parseLong(scanner.nextLine());
            ticketService.add(LocalDateTime.now(), customerId, eventId);
            System.out.println("Created Ticket for event wit id=" + eventId);
        } catch (Exception e) {
            System.out.println("Failed to create Ticket");
        }
    }

    private void deleteTicket() {
        try {
            System.out.println("Enter ticket Id: ");
            long ticketId = Long.parseLong(scanner.nextLine());
            ticketService.delete(ticketId);
        } catch (Exception e) {
            System.out.println("Failed to delete Ticket");
        }
    }

    private void deleteAllTickets() {
        ticketService.deleteAll();
    }

    private void getTicket() {
        try {
            System.out.println("Enter ticket Id: ");
            long ticketId = Long.parseLong(scanner.nextLine());
            System.out.println(ticketService.get(ticketId));
        } catch (Exception e) {
            System.out.println("Failed to find Ticket");
        }
    }

    private void getAllTickets() {
        Ticket[] tickets = ticketService.getAll();
        for (Ticket ticket : tickets) {
            System.out.println(ticket);
        }
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
                case "nc":
                    newCustomer();
                    break;
                case "gc":
                    getCustomerById();
                    break;
                case "cc":
                    updateCustomerById();
                    break;
                case "dc":
                    deleteCustomerById();
                    break;
                case "ac":
                    showCustomers();
                    break;
                case "dac":
                    deleteAllCustomers();
                    break;
                case "nt":
                    newTicket();
                    break;
                case "gt":
                    getTicket();
                    break;
                case "gat":
                    getAllTickets();
                    break;
                case "dt":
                    deleteTicket();
                    break;
                case "dat":
                    deleteAllTickets();
                    break;
                case "h":
                case "help":
                case "?":
                    help();
                    break;
                case "he":
                    helpEvent();
                    break;
                case "hc":
                    helpCustomer();
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