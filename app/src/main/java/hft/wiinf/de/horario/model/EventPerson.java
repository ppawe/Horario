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
    @Column
    private String rejectionReason = "";

    public EventPerson() {
    }

    public EventPerson(Person person, Event event) {
        this.person = person;
        this.event = event;
    }

    public EventPerson(Person person, Event event, String status) {
        this.person = person;
        this.event = event;
        this.status = status;
    }

    public EventPerson(Person person, Event event, String status, String rejectionReason) {
        this.person = person;
        this.event = event;
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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
