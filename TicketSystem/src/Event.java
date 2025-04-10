import java.util.Date;

public class Event {
    private int ID;
    private String name;
    private String location;
    private Date date;
    private int nmbTickets;

    public Event(String name, String location, Date date, int nmbTickets) {
        this.name = name;
        this.location = location;
        if(date.toInstant().isAfter())
        this.date = date;
        if(nmbTickets < 0) {
            throw new RuntimeException("nmbTickets cannot be less than 0");
        }
        this.nmbTickets = nmbTickets;
    }

    @Override
    public String toString() {
        return "Name: " + name + "\n" + "Location: " + location + "\n" + "Date: " + date + "\n" + "Number of Tickets: " + nmbTickets;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public Date getDate() {
        return date;
    }

    public int getNmbTickets() {
        return nmbTickets;
    }
    public int getID() {
        return ID;
    }
}
