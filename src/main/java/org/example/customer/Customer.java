package org.example.customer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

/**
 * Represents a customer in the ticket shop system.
 * Each customer has a unique ID, username, email, birthday, and a collection of tickets they have purchased.
 */
public class Customer {
    private long id;
    private String username;
    private String email;
    private LocalDateTime birthday;
    private HashMap<Long, HashSet<Long>> tickets;

    /**
     * Constructs a new Customer instance.
     *
     * @param id The unique ID of the customer.
     * @param username The username of the customer. Must not be null or blank.
     * @param email The email address of the customer. Must be a valid format and not null or blank.
     * @param birthday The birthday of the customer. The customer must be at least 18 years old.
     * @throws IllegalArgumentException if username, email, or birthday are invalid.
     * @throws NullPointerException if username, email, or birthday are null.
     */
    public Customer(long id, String username, String email, LocalDateTime birthday) {
        this.id = id;

        this.username = Objects.requireNonNull(username, "username must not be null").trim();
        if (this.username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }

        String emailTrimmed = Objects.requireNonNull(email, "email must not be null").trim();
        if (emailTrimmed.isBlank() || !checkEmail(emailTrimmed)) {
            throw new IllegalArgumentException("Invalid or blank email");
        }
        this.email = emailTrimmed;

        Objects.requireNonNull(birthday, "birthday must not be null");
        if (!checkBirthday(birthday)) {
            throw new IllegalArgumentException("Customer must be at least 18 years old");
        }
        this.birthday = birthday;
        this.tickets = new HashMap<Long, HashSet<Long>>();
    }

    /**
     * Adds a ticket to the customer's collection for a specific event.
     * A customer can have a maximum of 5 tickets per event. This method is synchronized.
     *
     * @param eventId The ID of the event for which the ticket is being added.
     * @param ticketId The ID of the ticket to add.
     * @throws RuntimeException if the customer tries to purchase more than 5 tickets for the event.
     */
    public synchronized void addTicket(long eventId, long ticketId) { // Added synchronized
        if (tickets.containsKey(eventId)) {
            if (tickets.get(eventId).size() < 5) {
                tickets.get(eventId).add(ticketId);
            } else {
                throw new RuntimeException("Can't purchase more than 5 tickets for a single event");
            }
        } else {
            // If the eventId is not in the map, create a new HashSet for it
            HashSet<Long> newTicketSet = new HashSet<>();
            newTicketSet.add(ticketId);
            tickets.put(eventId, newTicketSet);
        }
    }

    /**
     * Removes a ticket from the customer's collection for a specific event.
     * This method is synchronized.
     *
     * @param eventId The ID of the event from which the ticket is being removed.
     * @param ticketId The ID of the ticket to remove.
     * @throws RuntimeException if the event with the given ID is not found in the customer's tickets.
     */
    public synchronized void removeTicket(long eventId, long ticketId) { // Renamed and added synchronized
        if (tickets.containsKey(eventId)) {
            tickets.get(eventId).remove(ticketId);
        } else {
            throw new RuntimeException("Event with id=" + eventId + " not found");
        }
    }

    /**
     * Gets the username of the customer.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the customer.
     *
     * @param username The new username. Must not be null or blank.
     * @throws IllegalArgumentException if the username is blank.
     * @throws NullPointerException if the username is null.
     */
    public void setUsername(String username) {
        String uname = Objects.requireNonNull(username, "username must not be null").trim();
        if (uname.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        this.username = uname;
    }

    /**
     * Gets the email address of the customer.
     *
     * @return The email address.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the customer.
     *
     * @param email The new email address. Must be a valid format and not null or blank.
     * @throws IllegalArgumentException if the email is invalid or blank.
     * @throws NullPointerException if the email is null.
     */
    public void setEmail(String email) {
        String emailTrimmed = Objects.requireNonNull(email, "email must not be null").trim();
        if (emailTrimmed.isBlank() || !checkEmail(emailTrimmed)) {
            throw new IllegalArgumentException("Invalid or blank email");
        }
        this.email = emailTrimmed;
    }

    /**
     * Gets the birthday of the customer.
     *
     * @return The birthday.
     */
    public LocalDateTime getBirthday() {
        return birthday;
    }

    /**
     * Sets the birthday of the customer.
     *
     * @param birthday The new birthday. The customer must be at least 18 years old.
     * @throws IllegalArgumentException if the customer is not at least 18 years old.
     * @throws NullPointerException if the birthday is null.
     */
    public void setBirthday(LocalDateTime birthday) {
        Objects.requireNonNull(birthday, "birthday must not be null");
        if (!checkBirthday(birthday)) {
            throw new IllegalArgumentException("Customer must be at least 18 years old");
        }
        this.birthday = birthday;
    }

    /**
     * Gets the unique ID of the customer.
     *
     * @return The customer ID.
     */
    public long getId() {
        return id;
    }

    private boolean checkEmail(String email) {
        String[] result1 = email.split("@");
        if (result1.length != 2) {
            return false;
        }
        String[] result2 = result1[1].split("\\.");
        if (result2.length < 2) {
            return false;
        }
        if (result2[result2.length - 1].chars().allMatch(Character::isLetter)) {
            return true;
        }
        return false;
    }

    private boolean checkBirthday(LocalDateTime birthday) {
        return birthday.isBefore(LocalDateTime.now().minusYears(18));
    }

    @Override
    public String toString() {
        return "Customer:" +
                "id: " + id + '\n' +
                "username: " + username + '\n' +
                "email: " + email + '\n' +
                "birthday: " + birthday;
    }
}