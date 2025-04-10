
import java.util.HashMap;

;
public class EventService {
    private HashMap<Integer, Event> events = new HashMap<>();

    public EventService(HashMap<Integer, Event> events) {
        this.events = events;
    }

    public void createEvent(Event event) {
        events.put(event.getID(),event);
    }

    public Event readEvent(int id) {
        return events.get(id);
    }

    public void updateEvent(Event event) {
        events.replace(event.getID(),event);
    }
    public void deleteEvent(int id) {
        events.remove(id);
    }
}
