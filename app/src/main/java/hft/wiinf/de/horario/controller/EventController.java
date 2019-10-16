package hft.wiinf.de.horario.controller;

import android.support.annotation.NonNull;

import com.activeandroid.query.Select;

import java.util.ArrayList;
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

/**
 * Class with several static methods related to the saving, deletion and retrieval of {@link Event}s from the database
 */
public class EventController {

    /**
     * ! Use this instead of Event.save() which does not set the creatorEventId !
     * saves the {@link Event} to the database and if the creatorEventId is not set (such as when creating a new event)
     * sets the creatorEventId to the new Id of the event and saves it again
     *
     * @param event
     */
    public static void saveEvent(@NonNull Event event) {
        if (event.getCreatorEventId() < 0)
            event.setCreatorEventId(event.save());
        event.save();
    }

    /**
     * deletes an Event from the database if it is saved there as well as all events that point to this event as their startEvent
     * in case of a serial event this means that if the deleted event is the start event of the chain all events in the chain are deleted as well
     *
     * @param event
     */
    public static void deleteEvent(@NonNull Event event) {
        if (event.getId() != null) {
            //delete also the repeating events if applicable
            List<Event> repeatingEvents = EventController.findFollowUpEvents(event.getId());
            for (Event repeatingEvent : repeatingEvents) {
                repeatingEvent.delete();
            }
            event.delete();
        }
    }

    /**
     * gets an event with the given id from the database
     * @param id the id of the event
     * @return the event with the given id
     */
    public static Event getEventById(@NonNull Long id) {
        return Event.load(Event.class, id);
    }

    /**
     * get all events created by the user with the given creatorEventId as their Id or pointing to that event as their startEvent
     *
     * @param creatorEventId the Id of the looked for event
     * @return a list of events created by the user with the given creatorEventId as their Id or pointing to that event as their startEvent
     */
    public static List<Event> getMyEventsFromReceivedCreatorEventId(@NonNull Long creatorEventId) {
        if (getEventById(creatorEventId).getRepetition() == Repetition.NONE) {
            List<Event> eventList = new ArrayList<>();
            if (getEventById(creatorEventId).getCreator() == PersonController.getPersonWhoIam()) {
                eventList.add(getEventById(creatorEventId));
            }
            return eventList;
        }
        return new Select().from(Event.class).where("startEvent=?", creatorEventId).and("creator = ?", PersonController.getPersonWhoIam()).execute();
    }

    /**
     * find all {@link Event}s that start and end between the given points in time
     * @param startDate the starting point
     * @param endDate the end point
     * @return a list of events taking place between startDate and endDate
     */
    public static List<Event> findEventsByTimePeriod(Date startDate, Date endDate) {
        return new Select().from(Event.class).where("starttime between ? AND ?", startDate.getTime(), endDate.getTime() - 1).orderBy("startTime,endTime,shortTitle").execute();
    }


    /**
     *
     * @return a list of all {@link Event}s in the database
     */
    public static List<Event> getAllEvents() {
        return new Select().from(Event.class).orderBy("startTime,endTime,shortTitle").execute();
    }

    /**
     *
     * @return boolean representing whether or not any {@link Event}s have been created yet
     */
    public static boolean createdEventsYet() {
        return new Select().from(Event.class).exists();
    }

    /**
     *
     * @return a list of {@link Event}s that the user has accepted and are taking place in the future
     */
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

    /**
     * ! Only use this if the {@link Event} with that Id is the startEvent of the serial event chain, else use findAllEventsForSerialEvent !
     * finds all Events that point to the given event Id as their startEvent (this includes the startEvent itself)
     *
     * @param eventId the Id of the event for which the follow up events are to be found
     * @return a list of follow up events including the given start event
     */
    public static List<Event> findFollowUpEvents(@NonNull Long eventId) {
        return new Select().from(Event.class).where("startevent=?", eventId).orderBy("startTime,endTime,shortTitle").execute();
    }

    /**
     * ! Use this instead of findFollowUpEvents if the event is not the startEvent of the serial event chain !
     * find all events that are part of the same serial event chain as the given event
     *
     * @param event the event that is part of a serial event chain
     * @return a list of events that are part of the same serial event chain
     */
    public static List<Event> findAllEventsForSerialEvent(Event event) {
        if (event == null || event.getRepetition() == Repetition.NONE) {
            return null;
        }
        return new Select().from(Event.class).where("startevent = ?", event.getStartEvent()).orderBy("startTime").execute();
    }

    /**
     * Saves the given {@link Event} as well as creating and saving follow up events if it is a serial event
     *
     * @param firstEvent the event to be saved
     */
    public static void saveSerialevent(@NonNull Event firstEvent) {
        int fieldNumber;
        //determine field number of calendar object that should be updated later (day, month or year)
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
        if (firstEvent.getRepetition() != Repetition.NONE && firstEvent.getRepetition() != null) {
            firstEvent.setStartEvent(firstEvent);
            saveEvent(firstEvent);

            //somebody doesn't know what a while loop is
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
                //if end of repetition is reached, stop else save the new Event;
                if (repetitionEvent.getStartTime().after(firstEvent.getEndDate()))
                    break;
                //save the new event
                saveEvent(repetitionEvent);
            }
        }

    }

    public static boolean checkIfEventIsInDatabaseViaId(Long eventIdInSMS) {
        return new Select().from(Event.class).where("Id=?", eventIdInSMS).exists();
    }

    /**
     * used to get an event if you do not know its Id such as when receiving an invitation and checking if it is already in your database
     * @param phoneNumber the number of of the creator of the event
     * @param creatorEventId the Id of the event in the creator's database
     * @return
     */
    public static Event getEventViaPhoneAndCreatorEventId(String phoneNumber, String creatorEventId) {
        Person creator = PersonController.getPersonViaPhoneNumber(phoneNumber);
        if (creator != null) {
            return new Select().from(Event.class).where("creator = ?", creator.getId()).and("creatorEventId = ?", creatorEventId).executeSingle();
        }
        return null;
    }

    /**
     * this can probably replace some usages of getEventViaPhoneAndCreatorEventId()
     * @param phoneNumber the phone number of the creator of the event
     * @param creatorEventId the Id of the event in the creator's database
     * @return boolean representing whether or not an event with the specified creatorEventId for the Person with the phoneNumber exists
     */
    public static boolean isEventSaved(String phoneNumber, String creatorEventId) {
        Person creator = PersonController.getPersonViaPhoneNumber(phoneNumber);
        if (creator != null) {
            return new Select().from(Person.class).where("creator = ?", creator.getId()).and("creatorEventId = ?", creatorEventId).exists();
        }
        return false;
    }

    /**
     * Creates an {@link Event} from a received invitation and saves it
     * Saves a {@link Person} object for the creator to the Database
     * Creates an {@link hft.wiinf.de.horario.model.EventPerson} for the creator and the user
     * Checking the {@link InvitationString} for validity should happen before calling this method
     *
     * @param invitationString Invitation received via SMS or QR-Code
     * @return The Event object that matches the specifications of the invitationString
     */
    public static Event createInvitedEventFromInvitation(InvitationString invitationString) {

        Event event = new Event();
        event.setRepetition(invitationString.getRepetitionAsRepetition());
        event.setCreator(PersonController.addOrGetPerson(invitationString.getReceivedFromNumber() != null ? invitationString.getReceivedFromNumber() : invitationString.getCreatorPhoneNumber(), invitationString.getCreatorName()));
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
            List<Event> repeatingEvents = findFollowUpEvents(event.getId());
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