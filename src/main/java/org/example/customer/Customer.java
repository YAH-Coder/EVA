package org.example.customer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public class Customer {
    private long id;
    private String username;
    private String email;
    private LocalDateTime birthday;
    private HashMap<Long, HashSet<Long>> tickets;

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

    public Customer(Customer other) {
        this(other.id, other.username, other.email, other.birthday);
    }

    public void addTicket(long eventId, long ticketId) {
        if (tickets.containsKey(eventId)) {
            if (tickets.get(eventId).size() < 5) {
                tickets.get(eventId).add(ticketId);
            } else {
                throw new RuntimeException("Can't purchase more than 5 tickets for a single event");
            }
        }
    }

    public void remooveTicket(long eventId, long ticketId) {
        if (tickets.containsKey(eventId)) {
            tickets.get(eventId).remove(ticketId);
        } else {
            throw new RuntimeException("Event with id=" + eventId + " not found");
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        String uname = Objects.requireNonNull(username, "username must not be null").trim();
        if (uname.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        this.username = uname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        String emailTrimmed = Objects.requireNonNull(email, "email must not be null").trim();
        if (emailTrimmed.isBlank() || !checkEmail(emailTrimmed)) {
            throw new IllegalArgumentException("Invalid or blank email");
        }
        this.email = emailTrimmed;
    }

    public LocalDateTime getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDateTime birthday) {
        Objects.requireNonNull(birthday, "birthday must not be null");
        if (!checkBirthday(birthday)) {
            throw new IllegalArgumentException("Customer must be at least 18 years old");
        }
        this.birthday = birthday;
    }

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