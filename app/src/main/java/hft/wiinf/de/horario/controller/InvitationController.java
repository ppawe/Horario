package hft.wiinf.de.horario.controller;

import android.support.annotation.NonNull;

import com.activeandroid.query.Select;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.InvitationString;
import hft.wiinf.de.horario.model.Person;

public class InvitationController {
    public static void saveInvitation(@NonNull InvitationString invitationString) {
        if (invitationString.getInvitation() != null) {
            invitationString.save();
        }
    }

    public static void deleteInvitation(@NonNull InvitationString invitationString) {
        invitationString.delete();
    }

    public static List<InvitationString> getAllInvitations() {
        return new Select().from(InvitationString.class).orderBy("dateReceived").execute();
    }

    public static boolean alreadyInvited(@NonNull InvitationString invitationString) {
        List<InvitationString> invitationStrings = new Select().from(InvitationString.class).where("invitationString = ?", invitationString.getInvitation()).execute();
        return invitationStrings.size() > 0;
    }
    public static int getNumberOfInvitations(){
        deleteExpiredInvitations();
        return new Select().from(InvitationString.class).count();
    }

    public static boolean eventAlreadySaved(@NonNull InvitationString invitationString) {
        List<Person> creator = new Select().from(Person.class).where("phoneNumber = ?", invitationString.getCreatorPhoneNumber()).execute();
        if(creator.size() != 0) {
            List<Event> events = new Select().from(Event.class).where("creator = ?", creator.get(0).getId()).and("creatorEventId = ?", invitationString.getCreatorEventId()).execute();
            return events.size() > 0;
        }
        return false;
    }

    public static InvitationString getInvitationById(String id) {
        return new Select().from(InvitationString.class).where("id = ?", id).executeSingle();
    }

    public static void deleteExpiredInvitations(){
        List<InvitationString> invitationStrings = getAllInvitations();
        Date now = new Date();
        for (InvitationString invitationString : invitationStrings) {
            String dateString = invitationString.getStartTime() + " " + invitationString.getStartDate();
            SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
            try {
                Date eventDate = format.parse(dateString);
                if(now.after(eventDate)){
                    invitationString.delete();
                }
            }catch(ParseException e){
                e.printStackTrace();
            }
        }
    }
}
