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
    /**
     * the {@link EventPerson} object representing the relationship between the given {@link Person} and the {@link Event}
     *
     * @param event  the event part of the relationship
     * @param person the person part of the relationship
     * @return an EventPerson representing the relationship between the parameters
     */
    public static EventPerson getEventPerson(@NonNull Event event, @NonNull Person person) {

        EventPerson eventPerson = null;
        if (event.getId() != null && person.getId() != null) {
            eventPerson = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("person = ?", person.getId()).executeSingle();
        }
        return eventPerson;
    }

    /**
     * gets a list of {@link EventPerson} objects representing the relationships between the given {@link Person} and the {@link Event}s that point to the given event as their startEvent
     *
     * @param startEvent the event part of the relationship as well as the startEvent of the events in the serial event chain
     * @param person     the person part of the relationship
     * @return the list of EventPerson objects
     */
    private static List<EventPerson> getEventPeopleForSerialEvent(@NonNull Event startEvent, @NonNull Person person) {
        List<Event> events = EventController.findFollowUpEvents(startEvent.getId());
        List<EventPerson> eventPeople = new ArrayList<>();
        for (Event event : events) {
            eventPeople.add(EventPersonController.addOrGetEventPerson(event, person));
        }
        return eventPeople;
    }

    /**
     * gets the {@link EventPerson} for the given {@link Event} and {@link Person}
     * if there isn't one, creates one
     * @param event the event part of the relationship
     * @param person the person part of the relationship
     * @return the EventPerson representing the relationship between the given event and person
     */
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

    /**
     * gets the {@link EventPerson} for the given {@link Event} and {@link Person}
     * if there isn't one, creates one with the given {@link AcceptedState}
     * @param event the event part of the relationship
     * @param person the person part of the relationship
     * @param status the state to be set if the relationship doesn't exist
     * @return the EventPerson representing the relationship between the given event and person
     */
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

    /**
     * gets the {@link EventPerson} for the given {@link Event} and {@link Person}
     * if there isn't one, creates one with the given {@link AcceptedState} and rejectionReason
     * @param event the event part of the relationship
     * @param person the person part of the relationship
     * @param status the state to be set if the relationship doesn't exist
     * @param rejectionReason the reason for rejection to be set if the relationship doesn't exist
     * @return the EventPerson representing the relationship between the given event and person
     */
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

    /**
     * deletes the relationship between the given {@link Event} and {@link Person} from the database
     * @param event the event part of the relationship
     * @param person the person part of the relationship
     */
    public static void deleteEventPerson(@NonNull Event event, @NonNull Person person) {
        EventPerson eventPerson = getEventPerson(event, person);
        if (eventPerson != null && eventPerson.getId() != null) {
            eventPerson.delete();
        }
    }

    /**
     * gets a list of {@link Person} objects that have accepted the given {@link Event}
     * @param event the event for which the list should be retrieved
     * @return the list of people who have accepted the event
     */
    public static List<Person> getEventParticipants(@NonNull Event event) {
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("status = ?", AcceptedState.ACCEPTED).execute();
        List<Person> participants = new ArrayList<>();
        for (EventPerson eventParticipant : eventPeople) {
            participants.add(eventParticipant.getPerson());
        }
        return participants;
    }

    /**
     * gets a list of {@link Person} objects that have rejected the given {@link Event}
     * @param event the event for which the list should be retrieved
     * @return the list of people who have rejected the event
     */
    public static List<Person> getEventCancellations(@NonNull Event event) {
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("status = ?", AcceptedState.REJECTED).execute();
        List<Person> cancellations = new ArrayList<>();
        for (EventPerson eventCancellation : eventPeople) {
            cancellations.add(eventCancellation.getPerson());
        }
        return cancellations;
    }

    /**
     * gets a list of {@link Person} objects that have yet to accept or reject the given {@link Event}
     * @param event the event for which the list is to be retrieved
     * @return the list of pending people
     */
    public static List<Person> getEventPendingPeople(@NonNull Event event) {
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("event = ?", event.getId()).and("status = ?", AcceptedState.WAITING).execute();
        List<Person> pendingPeople = new ArrayList<>();
        for (EventPerson eventPendingPerson : eventPeople) {
            pendingPeople.add(eventPendingPerson.getPerson());
        }
        return pendingPeople;
    }

    /**
     * checks if the given {@link Person} participates in the given {@link Event}
     * @param event the event part of the relationship
     * @param person the person part of the relationship
     * @return boolean representing whether the person participates in the event
     */
    public static boolean personParticipatesInEvent(@NonNull Event event, @NonNull Person person) {
        return new Select().from(EventPerson.class).where("event = ?", event.getId()).and("person = ?", person.getId()).and("status = ?", AcceptedState.ACCEPTED).exists();
    }

    /**
     * checks if the given {@link Person} is invited to the given {@link Event}
     * @param event the event part of the relationship
     * @param person the person part of the relationship
     * @return boolean representing whether the person is invited to the given event
     */
    public static boolean personIsInvitedToEvent(@NonNull Event event, @NonNull Person person) {
        return new Select().from(EventPerson.class).where("event = ?", event.getId()).and("person = ?", person.getId()).and("status = ?", AcceptedState.INVITED).exists();
    }

    /**
     * changes the {@link AcceptedState} status of the given {@link Event}-{@link Person} relationship to the given state
     * @param event the event part of the relationship
     * @param person the person part of the relationship
     * @param status the new state of the relationship
     * @param rejectionReason if the state is set to rejected this is the given reason for the rejection
     * @return
     */
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

    /**
     * changes the {@link AcceptedState} status of the given {@link Event}-{@link Person} relationship to the given state
     * if a rejectionReason is given and the status is set to rejected it is set as well
     * the same is done for any relationships between the person and any events in the same serial event chain as the given event
     * @param event the event part of the relationship
     * @param person the person part of the relationship
     * @param status the new state of the relationship
     * @param rejectionReason if the state is set to rejected this is the given reason for the rejection
     */
    public static void changeStatusForSerial(@NonNull Event event, @NonNull Person person, AcceptedState status, String rejectionReason) {
        if (event.getRepetition() != Repetition.NONE) {
            List<EventPerson> eventPeople = getEventPeopleForSerialEvent(event, person);
            for (EventPerson eventPerson : eventPeople) {
                changeStatus(eventPerson.getEvent(), eventPerson.getPerson(), status, rejectionReason);
            }
        }
    }

    /**
     * gets a list of all {@link Event}s the given {@link Person} is associated with in the database
     * @param person the person for which the list should be retrieved
     * @return the list of events
     */
    public static List<Event> getAllEventsForPerson(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).execute();
        for (EventPerson eventPerson : eventPeople) {
            events.add(eventPerson.getEvent());
        }
        return events;
    }

    /**
     * gets a list of {@link Event}s the given {@link Person} accepted
     * @param person the person for which the list should be retrieved
     * @return the list of events
     */
    public static List<Event> getAllAcceptedEventsForPerson(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).and("status = ?", AcceptedState.ACCEPTED).execute();
        for (EventPerson eventPerson : eventPeople) {
            events.add(eventPerson.getEvent());
        }
        return events;
    }

    /**
     * gets a list of {@link Event}s which the given {@link Person} has saved but not responded to
     * @param person the person for which the list should be retrieved
     * @return the list of events
     */
    private static List<Event> getAllPendingEventsForPerson(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).and("status = ?", AcceptedState.WAITING).execute();
        for (EventPerson eventPerson : eventPeople) {
            events.add(eventPerson.getEvent());
        }
        return events;
    }

    /**
     * gets a list of {@link Event}s which the given {@link Person} has saved but not responded to, ignoring any serial events that aren't the start event of their chain
     * @param person the person for which the list should be retrieved
     * @return the list of events
     */
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

    /**
     * gets a list of {@link Event}s to which the given {@link Person} has been invited to
     * @param person the person for which the list should be retrieved
     * @return the list of events the person has been invited to
     */
    private static List<Event> getAllInvitedEventsForPerson(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).and("status = ?", AcceptedState.INVITED).execute();
        for (EventPerson eventPerson : eventPeople) {
            events.add(eventPerson.getEvent());
        }
        return events;
    }

    /**
     * gets a list of {@link Event}s to which the given {@link Person} has been invited to, ignoring any serial events that aren't the start event of their chain
     * @param person the person for which the list should be retrieved
     * @return the list of events
     */
    public static List<Event> getAllInvitedEventsForPersonWithoutSerials(Person person) {
        List<Event> events = new ArrayList<>();
        List<EventPerson> eventPeople = new Select().from(EventPerson.class).where("person = ?", person.getId()).and("status = ?", AcceptedState.INVITED).execute();
        for (EventPerson eventPerson : eventPeople) {
            Event event = eventPerson.getEvent();
            if (event.getRepetition() == Repetition.NONE || event.getStartEvent().getId().equals(event.getId())) {
                events.add(eventPerson.getEvent());
            }
        }
        return events;
    }

    /**
     * deletes all past {@link Event}s that the given {@link Person} has saved but not responded to
     * @param person the person for which all pending events should be deleted
     */
    public static void deleteExpiredPendingEventsForPerson(Person person) {
        List<Event> events = getAllPendingEventsForPerson(person);
        Date now = new Date();
        for (Event event : events) {
            if (event.getStartTime().before(now) && event.getRepetition() == Repetition.NONE || event.getRepetition() != Repetition.NONE && event.getEndRepetitionDate().before(now)) {
                EventController.deleteEvent(event);
            }
        }
    }

    /**
     * deletes all past {@link Event}s that the given {@link Person} has been invited to
     * @param person the person for which all past invitations should be deleted
     */
    private static void deleteExpiredInvitedEventsForPerson(Person person) {
        List<Event> events = getAllInvitedEventsForPerson(person);
        Date now = new Date();
        for (Event event : events) {
            if (event.getStartTime().before(now) && event.getRepetition() == Repetition.NONE || event.getRepetition() != Repetition.NONE && event.getEndRepetitionDate().before(now)) {
                EventController.deleteEvent(event);
            }
        }
    }


    /**
     * gets the number of current {@link Event}s that a {@link Person} has been invited to
     * @param person the person for which the number of invitations should be retrieved
     * @return the number of events the person has been invited to
     */
    public static int getNumberOfInvitedEventsForPerson(Person person) {
        deleteExpiredInvitedEventsForPerson(person);
        return getAllInvitedEventsForPersonWithoutSerials(person).size();
    }
}
