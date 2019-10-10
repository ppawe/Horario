package hft.wiinf.de.horario.controller;

import android.support.annotation.NonNull;

import com.activeandroid.query.Select;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hft.wiinf.de.horario.model.AcceptedState;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.EventPerson;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.Repetition;

public class EventPersonController {
    public static EventPerson getEventPerson(@NonNull Event event, @NonNull Person person) {

        EventPerson eventPerson = null;
        if (event.getId() != null && person.getId() != null) {
            eventPerson = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("person = ?", person.getId()).executeSingle();
        }
        return eventPerson;
    }

    private static List<EventPerson> getEventPeopleForSerialEvent(@NonNull Event startEvent, @NonNull Person person) {
        List<Event> events = EventController.findRepeatingEvents(startEvent.getId());
        List<EventPerson> eventPeople = new ArrayList<>();
        for (Event event : events) {
            eventPeople.add(EventPersonController.addOrGetEventPerson(event, person));
        }
        return eventPeople;
    }

    private static EventPerson addOrGetEventPerson(@NonNull Event event, @NonNull Person person) {
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

    public static EventPerson addOrGetEventPerson(@NonNull Event event, @NonNull Person person, @NonNull AcceptedState status) {
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

    public static EventPerson addOrGetEventPerson(@NonNull Event event, @NonNull Person person, @NonNull AcceptedState status, @NonNull String rejectionReason) {
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
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("status = ?", AcceptedState.ACCEPTED).execute();
        List<Person> participants = new ArrayList<>();
        for (EventPerson eventParticipant : eventPeople) {
            participants.add(eventParticipant.getPerson());
        }
        return participants;
    }

    public static List<Person> getEventCancellations(@NonNull Event event) {
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("status = ?", AcceptedState.REJECTED).execute();
        List<Person> cancellations = new ArrayList<>();
        for (EventPerson eventCancellation : eventPeople) {
            cancellations.add(eventCancellation.getPerson());
        }
        return cancellations;
    }

    public static List<Person> getEventPendingPeople(@NonNull Event event) {
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("status = ?", AcceptedState.WAITING).execute();
        List<Person> pendingPeople = new ArrayList<>();
        for (EventPerson eventPendingPerson : eventPeople) {
            pendingPeople.add(eventPendingPerson.getPerson());
        }
        return pendingPeople;
    }

    public static boolean personParticipatesInEvent(@NonNull Event event, @NonNull Person person) {
        return new Select().from(EventPerson.class).where("event = ?", event.getId()).and("person = ?", person.getId()).and("status = ?", AcceptedState.ACCEPTED).exists();
    }

    public static boolean personIsInvitedToEvent(@NonNull Event event, @NonNull Person person) {
        return new Select().from(EventPerson.class).where("event = ?", event.getId()).and("person = ?", person.getId()).and("status = ?", AcceptedState.WAITING).exists();
    }

    public static EventPerson changeStatus(@NonNull Event event, @NonNull Person person, AcceptedState status, String rejectionReason) {
        EventPerson eventPerson = getEventPerson(event, person);
        if (eventPerson != null && eventPerson.getId() != null && event.getId() != null && person.getId() != null) {
            switch (status) {
                case ACCEPTED:
                case INVITED:
                case WAITING:
                    eventPerson.setRejectionReason("");
                    break;
                case REJECTED:
                    eventPerson.setRejectionReason(rejectionReason);
                    break;
            }
            eventPerson.setStatus(status);
            eventPerson.save();
        }
        return eventPerson;
    }

    public static void changeStatusForSerial(@NonNull Event event, @NonNull Person person, AcceptedState status, String rejectionReason) {
        if (event.getRepetition() != Repetition.NONE) {
            List<EventPerson> eventPeople = getEventPeopleForSerialEvent(event, person);
            for (EventPerson eventPerson : eventPeople) {
                changeStatus(eventPerson.getEvent(), eventPerson.getPerson(), status, rejectionReason);
            }
        }
    }

    public static List<Event> getAllEventsForPerson(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).execute();
        for (EventPerson eventPerson : eventPeople) {
            events.add(eventPerson.getEvent());
        }
        return events;
    }

    public static List<Event> getAllAcceptedEventsForPerson(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).and("status = ?", AcceptedState.ACCEPTED).execute();
        for (EventPerson eventPerson : eventPeople) {
            events.add(eventPerson.getEvent());
        }
        return events;
    }

    private static List<Event> getAllPendingEventsForPerson(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).and("status = ?", AcceptedState.WAITING).execute();
        for (EventPerson eventPerson : eventPeople) {
            events.add(eventPerson.getEvent());
        }
        return events;
    }

    public static List<Event> getAllPendingEventsForPersonWithoutSerials(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).and("status = ?", AcceptedState.WAITING).execute();
        for (EventPerson eventPerson : eventPeople) {
            Event event = eventPerson.getEvent();
            if (event.getStartEvent().getId().equals(event.getId())) {
                events.add(eventPerson.getEvent());
            }
        }
        return events;
    }

    private static List<Event> getAllInvitedEventsForPerson(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).and("status = ?", AcceptedState.INVITED).execute();
        for (EventPerson eventPerson : eventPeople) {
            events.add(eventPerson.getEvent());
        }
        return events;
    }

    public static List<Event> getAllInvitedEventsForPersonWithoutSerials(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).and("status = ?", AcceptedState.INVITED).execute();
        for (EventPerson eventPerson : eventPeople) {
            Event event = eventPerson.getEvent();
            if (event.getStartEvent().getId().equals(event.getId())) {
                events.add(eventPerson.getEvent());
            }
        }
        return events;
    }

    public static void deleteExpiredPendingEventsForPerson(Person person) {
        List<Event> events = getAllPendingEventsForPerson(person);
        Date now = new Date();
        for (Event event : events) {
            if (event.getStartTime().before(now) && event.getRepetition() == Repetition.NONE || event.getRepetition() != Repetition.NONE && event.getEndDate().before(now)) {
                EventController.deleteEvent(event);
            }
        }
    }

    private static void deleteExpiredInvitedEventsForPerson(Person person) {
        List<Event> events = getAllInvitedEventsForPerson(person);
        Date now = new Date();
        for (Event event : events) {
            if (event.getStartTime().before(now) && event.getRepetition() == Repetition.NONE || event.getRepetition() != Repetition.NONE && event.getEndDate().before(now)) {
                EventController.deleteEvent(event);
            }
        }
    }


    public static int getNumberOfInvitedEventsForPerson(Person person) {
        deleteExpiredInvitedEventsForPerson(person);
        return getAllInvitedEventsForPersonWithoutSerials(person).size();
    }
}
