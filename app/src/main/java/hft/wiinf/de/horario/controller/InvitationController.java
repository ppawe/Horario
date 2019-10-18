package hft.wiinf.de.horario.controller;

import android.support.annotation.NonNull;

import com.activeandroid.query.Select;

import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.InvitationString;
import hft.wiinf.de.horario.model.Person;

public class InvitationController {

    /**
     * checks if the {@link Event} specified in the given {@link InvitationString} already exists in the database
     *
     * @param invitationString the invitationString to be checked
     * @return boolean representing whether the event is already saved
     */
    public static boolean eventAlreadySaved(@NonNull InvitationString invitationString) {
        Person creator = new Select()
                .from(Person.class)
                .where("phoneNumber = ?", invitationString.getCreatorPhoneNumber())
                .or("phoneNumber = ?", invitationString.getReceivedFromNumber())
                .executeSingle();
        if (creator != null) {
            return new Select().from(Event.class).where("creator = ?", creator.getId()).and("creatorEventId = ?", invitationString.getCreatorEventId()).exists();
        }
        return false;
    }
}
