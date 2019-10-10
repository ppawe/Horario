package hft.wiinf.de.horario.controller;

import android.support.annotation.NonNull;

import com.activeandroid.query.Select;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import hft.wiinf.de.horario.model.AcceptedState;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.InvitationString;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.Repetition;

public class EventController {
    //saves (update or create)an event
    public static void saveEvent(@NonNull Event event) {
        if (event.getCreatorEventId() < 0)
            event.setCreatorEventId(event.save());
        event.save();
    }

    public static void deleteEvent(@NonNull Event event) {
        if (event.getId() != null) {
            //delete also the repeating events if applicable
            List<Event> repeatingEvents = EventController.findRepeatingEvents(event.getId());
            for (Event repeatingEvent : repeatingEvents) {
                repeatingEvent.delete();
            }
            event.delete();
        }
    }

    public static Event getEventById(@NonNull Long id) {
        return Event.load(Event.class, id);
    }

    // get one of your own events via its Id and all the serial events following it
    public static List<Event> getMyEventsByCreatorEventId(@NonNull Long creatorEventId) {
        return new Select().from(Event.class).where("creator = 1 AND startEvent=?", creatorEventId).execute();
    }

    //find the list of events that start in the given period (enddate is not included!)
    public static List<Event> findEventsByTimePeriod(Date startDate, Date endDate) {
        return new Select().from(Event.class).where("starttime between ? AND ?", startDate.getTime(), endDate.getTime() - 1).orderBy("startTime,endTime,shortTitle").execute();
    }

    //needs changing
    public static List<Event> findMyAcceptedEvents() {
        return new Select().from(Event.class).where("accepted=?", AcceptedState.ACCEPTED).orderBy("startTime,endTime,shortTitle").execute();
    }

    public static List<Event> getAllEvents() {
        return new Select().from(Event.class).orderBy("startTime,endTime,shortTitle").execute();
    }

    public static boolean createdEventsYet() {
        return new Select().from(Event.class).exists();
    }

    public static List<Event> findMyAcceptedEventsInTheFuture() {
        List<Event> events = EventPersonController.getAllAcceptedEventsForPerson(PersonController.getPersonWhoIam());
        Iterator<Event> i = events.iterator();
        Date now = new Date();
        while (i.hasNext()) {
            Event event = i.next();
            if (event.getStartTime().before(now)) {
                i.remove();
            }
        }
        return events;
    }

    //find all events that point to the given event as an start event
    public static List<Event> findRepeatingEvents(@NonNull Long eventId) {
        return new Select().from(Event.class).where("startevent=?", eventId).orderBy("startTime,endTime,shortTitle").execute();
    }

    // saves a serial event, firstEvent="StartEvent",
    public static void saveSerialevent(Event firstEvent) {
        int fieldNumber;
        //determine field number of calendar object that should be updated laer (day, month or year)
        switch (firstEvent.getRepetition()) {
            case DAILY:
                fieldNumber = Calendar.DAY_OF_MONTH;
                break;
            case WEEKLY:
                fieldNumber = Calendar.WEEK_OF_YEAR;
                break;
            case MONTHLY:
                fieldNumber = Calendar.MONTH;
                break;
            default:
                fieldNumber = Calendar.YEAR;
        }
        //save first event;
        saveEvent(firstEvent);
        firstEvent.setStartEvent(firstEvent);
        saveEvent(firstEvent);

        for (int i = 1; ; i++) {
            //copy first event in new temporary event
            Event repetitionEvent = new Event(firstEvent.getCreator());
            repetitionEvent.setPlace(firstEvent.getPlace());
            repetitionEvent.setDescription(firstEvent.getDescription());
            repetitionEvent.setRepetition(firstEvent.getRepetition());
            repetitionEvent.setEndDate(firstEvent.getEndDate());
            repetitionEvent.setShortTitle(firstEvent.getShortTitle());
            repetitionEvent.setStartEvent(firstEvent);
            repetitionEvent.setCreatorEventId(firstEvent.getCreatorEventId());
            //copy the start and end time of the start event into a temporary variable, add 1 to the corresponding field and save the new value into the next event
            Calendar temporary = new GregorianCalendar();
            temporary.setTime(firstEvent.getStartTime());
            temporary.add(fieldNumber, i);
            repetitionEvent.setStartTime(temporary.getTime());
            temporary.setTime(firstEvent.getEndTime());
            temporary.add(fieldNumber, i);
            repetitionEvent.setEndTime(temporary.getTime());
            //if end of repetition is overruned, stopp else save the new Event;
            if (repetitionEvent.getStartTime().after(firstEvent.getEndDate()))
                break;
            //save the new event
            saveEvent(repetitionEvent);
        }

    }

    // needs replacing - doesn't do what it says it does
    // only way to identify an event is the creator's phoneNumber and creatorEventId or simply its Id
    public static Event checkIfEventIsInDatabase(String description, String shortTitle,
                                                 String place,
                                                 Calendar startTime, Calendar endTime) {
        return new Select()
                .from(Event.class)
                .where("description = ?", description)
                .where("shortTitle = ?", shortTitle)
                .where("place = ?", place)
                .where("startTime = ?", startTime.getTimeInMillis())
                .where("endTime = ?", endTime.getTimeInMillis())
                .executeSingle();
    }

    public static boolean checkIfEventIsInDatabaseThroughId(Long eventIdInSMS) {
        return new Select().from(Event.class).where("Id=?", eventIdInSMS).exists();
    }

    public static Event getEventViaPhoneAndCreatorEventId(String phoneNumber, String creatorEventId) {
        Person creator = PersonController.getPersonViaPhoneNumber(phoneNumber);
        if (creator != null) {
            return new Select().from(Event.class).where("creator = ?", creator.getId()).and("creatorEventId = ?", creatorEventId).executeSingle();
        }
        return null;
    }

    public static boolean isEventSaved(String phoneNumber, String creatorEventId) {
        Person creator = PersonController.getPersonViaPhoneNumber(phoneNumber);
        if (creator != null) {
            return new Select().from(Person.class).where("creator = ?", creator.getId()).and("creatorEventId = ?", creatorEventId).exists();
        }
        return false;
    }

    public static Event createInvitedEventFromInvitation(InvitationString invitationString) {

        Event event = new Event();
        event.setRepetition(invitationString.getRepetitionAsRepetition());
        event.setCreator(PersonController.addPerson(invitationString.getReceivedFromNumber() != null ? invitationString.getReceivedFromNumber() : invitationString.getCreatorPhoneNumber(), invitationString.getCreatorName()));
        event.setCreatorEventId(Long.valueOf(invitationString.getCreatorEventId()));
        event.setDescription(invitationString.getDescription());
        event.setEndDate(invitationString.getEndDateAsDate());
        event.setEndTime(invitationString.getEndTimeAsDate());
        event.setStartTime(invitationString.getStartTimeAsDate());
        event.setShortTitle(invitationString.getTitle());
        event.setPlace(invitationString.getPlace());
        if (event.getRepetition() == Repetition.NONE) {
            EventController.saveEvent(event);
            EventPersonController.addOrGetEventPerson(event, event.getCreator(), AcceptedState.ACCEPTED);
            EventPersonController.addOrGetEventPerson(event, PersonController.getPersonWhoIam(), AcceptedState.INVITED);
        } else {
            EventController.saveSerialevent(event);
            List<Event> repeatingEvents = findRepeatingEvents(event.getId());
            Person creator = event.getCreator();
            Person me = PersonController.getPersonWhoIam();
            for (Event singleEvent : repeatingEvents) {
                EventPersonController.addOrGetEventPerson(singleEvent, creator, AcceptedState.ACCEPTED);
                EventPersonController.addOrGetEventPerson(singleEvent, me, AcceptedState.INVITED);
            }
        }
        return event;
    }
}