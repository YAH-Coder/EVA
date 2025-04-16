package org.example;

import lombok.Getter;

import java.util.Date;

@Getter
public class Event {
    private String name;
    private String location;
    private Date date;
    private int nmbTickets;

    public Event(String name, String location, Date date, int nmbTickets) {
        this.name = name;
        this.location = location;
        this.date = date;
        this.nmbTickets = nmbTickets;
    }

    @Override
    public String toString() {
        return "Name: " + name + "\n" + "Location: " + location + "\n" + "Date: " + date + "\n" + "Number of Tickets: " + nmbTickets;
    }
}