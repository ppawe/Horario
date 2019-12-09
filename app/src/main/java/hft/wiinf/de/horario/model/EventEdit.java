package hft.wiinf.de.horario.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "eventedit")
public class EventEdit extends Model {
    @Column
    private Edit edit;
    @Column
    private Event event;
    @Column
    private int version;

    public EventEdit() {
        super();
    }

    public EventEdit(Edit edit, Event event, int version) {
        this.edit = edit;
        this.event = event;
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Edit getEdit() {
        return edit;
    }

    public void setEdit(Edit edit) {
        this.edit = edit;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
