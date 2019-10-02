package hft.wiinf.de.horario.controller;

import android.support.annotation.NonNull;

import com.activeandroid.query.Select;

import java.util.ArrayList;
import java.util.List;

import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.EventPerson;
import hft.wiinf.de.horario.model.Person;

public class EventPersonController {
    public static EventPerson getEventPerson(@NonNull Event event, @NonNull Person person) {

        EventPerson eventPerson = null;
        if (event.getId() != null && person.getId() != null) {
            eventPerson = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("person = ?", person.getId()).executeSingle();
        }
        return eventPerson;
    }

    public static EventPerson addEventPerson(@NonNull Event event, @NonNull Person person) {
        EventPerson eventPerson = getEventPerson(event, person);
        if (eventPerson == null) {
            if (event.getId() == null) {
                event.save();
            }
            if (person.getId() == null) {
                person.save();
            }
            eventPerson = new EventPerson(person, event);
            eventPerson.save();
        }
        return eventPerson;
    }

    public static EventPerson addEventPerson(@NonNull Event event, @NonNull Person person, String status) {
        EventPerson eventPerson = getEventPerson(event, person);
        if (eventPerson == null) {
            if (event.getId() == null) {
                event.save();
            }
            if (person.getId() == null) {
                person.save();
            }
            eventPerson = new EventPerson(person, event, status);
            eventPerson.save();
        }
        return eventPerson;
    }

    public static EventPerson addEventPerson(@NonNull Event event, @NonNull Person person, String status, String rejectionReason) {
        EventPerson eventPerson = getEventPerson(event, person);
        if (eventPerson == null) {
            if (event.getId() == null) {
                event.save();
            }
            if (person.getId() == null) {
                person.save();
            }
            eventPerson = new EventPerson(person, event, status, rejectionReason);
            eventPerson.save();
        }
        return eventPerson;
    }

    public static void deleteEventPerson(@NonNull Event event, @NonNull Person person) {
        EventPerson eventPerson = getEventPerson(event, person);
        if (eventPerson != null && eventPerson.getId() != null) {
            eventPerson.delete();
        }
    }

    public static List<Person> getEventParticipants(@NonNull Event event) {
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("status = accepted").execute();
        List<Person> participants = new ArrayList<>();
        for (EventPerson eventParticipant : eventPeople) {
            participants.add(eventParticipant.getPerson());
        }
        return participants;
    }

    public static List<Person> getEventCancellations(@NonNull Event event) {
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("status = cancelled").execute();
        List<Person> cancellations = new ArrayList<>();
        for (EventPerson eventCancellation : eventPeople) {
            cancellations.add(eventCancellation.getPerson());
        }
        return cancellations;
    }

    public static List<Person> getEventPendingPeple(@NonNull Event event) {
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("status = pending").execute();
        List<Person> pendingPeople = new ArrayList<>();
        for (EventPerson eventPendingPerson : eventPeople) {
            pendingPeople.add(eventPendingPerson.getPerson());
        }
        return pendingPeople;
    }

    public static boolean personParticipatesInEvent(@NonNull Event event, @NonNull Person person) {
        return new Select().from(EventPerson.class).where("event = ?", event.getId()).and("person = ?", person.getId()).and("status = accepted").exists();
    }

    public static boolean personIsInvitedToEvent(@NonNull Event event, @NonNull Person person) {
        return new Select().from(EventPerson.class).where("event = ?", event.getId()).and("person = ?", person.getId()).and("status = pending").exists();
    }

    public static EventPerson changeStatus(@NonNull Event event, @NonNull Person person, String status, String rejectionReason) {
        EventPerson eventPerson = getEventPerson(event, person);
        if (eventPerson != null && eventPerson.getId() != null && event.getId() != null && person.getId() != null) {
            switch (status) {
                case "accepted":
                case "pending":
                    eventPerson.setRejectionReason("");
                    break;
                case "cancelled":
                    eventPerson.setRejectionReason(rejectionReason);
                    break;
            }
            eventPerson.setStatus(status);
            eventPerson.save();
        }
        return eventPerson;
    }

}
