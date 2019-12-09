package hft.wiinf.de.horario.controller;

import com.activeandroid.query.Select;

import java.util.ArrayList;
import java.util.List;

import hft.wiinf.de.horario.model.Edit;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.EventEdit;

public class EventEditController {
    public static List<Edit> getEditsForEventFromVersion(Event event, int versionNumber){
        List<EventEdit> eventEdits = new Select()
                .from(EventEdit.class)
                .where("event = ?", event.getId())
                .and("version > ?", versionNumber)
                .orderBy("version")
                .execute();
        List<Edit> edits = new ArrayList<>();
        for(EventEdit eventEdit: eventEdits){
            edits.add(eventEdit.getEdit());
        }
        return edits;
    }
}
