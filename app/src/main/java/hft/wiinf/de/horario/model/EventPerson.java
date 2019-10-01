package hft.wiinf.de.horario.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "eventperson")
public class EventPerson extends Model {

    @Column(name = "Person", onDelete = Column.ForeignKeyAction.CASCADE)
    private Person person;
    @Column(name = "Event", onDelete = Column.ForeignKeyAction.CASCADE)
    private Event event;
    @Column
    private String status;

    public EventPerson() {
    }

    public EventPerson(Person person, Event event, String status) {
        this.person = person;
        this.event = event;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
