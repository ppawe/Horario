package hft.wiinf.de.horario.controller;

import android.support.annotation.NonNull;
import android.util.Log;

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

    /**
     * Checks if a message matches a valid invitation
     *
     * @param message The message to be checked
     * @return A boolean value representing whether or not the message passed all the checks
     */
    public static boolean checkForInvitationRegexOk(String message) {
        if (!message.matches(":HorarioInvitation:.*:HorarioInvitation:")) {
            return false;
        }
        message = message.replaceAll(":HorarioInvitation:", "");
        String[] splitMessage = message.split(" \\| ");
        if (splitMessage.length == 12) {
            //check if id is valid
            if (!splitMessage[0].matches("^[^0\\D]\\d*$")) {
                Log.d("invalidInvitation", splitMessage[0]);
                return false;
            }
            // check if startDate is valid. Pattern only matches valid dates in DD.MM.YYYY format and
            // includes a check for leap years so it correctly matches 29.02.YYYY
            if (!splitMessage[1].matches("^(?:(?:31(\\.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\.)(?:0?[1,3-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$")) {
                Log.d("invalidInvitation", splitMessage[1]);
                return false;
            }
            // check if endDate is valid
            if (!splitMessage[2].matches("^(?:(?:31(\\.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\.)(?:0?[1,3-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$")) {
                Log.d("invalidInvitation", splitMessage[2]);
                return false;
            }
            if (!splitMessage[3].matches("^([01]\\d|2[0-3]):([0-5]\\d)$")) {
                Log.d("invalidInvitation", splitMessage[3]);
                return false;
            }
            if (!splitMessage[4].matches("^([01]\\d|2[0-3]):([0-5]\\d)$")) {
                Log.d("invalidInvitation", splitMessage[4]);
                return false;
            }
            if (!splitMessage[5].matches("^[^\\s|][^|]*$")) {
                Log.d("invalidInvitation", splitMessage[5]);
                return false;
            }
            if (!splitMessage[6].matches("^(?:(?:31(\\.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\.)(?:0?[1,3-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$")) {
                Log.d("invalidInvitation", splitMessage[2]);
                return false;
            }
            if (!splitMessage[7].matches("^[^\\s|][^|]*$")) {
                Log.d("invalidInvitation", splitMessage[6]);
                return false;
            }
            if (!splitMessage[8].matches("^[^\\s|][^|]*$")) {
                Log.d("invalidInvitation", splitMessage[7]);
                return false;
            }
            if (!splitMessage[9].matches("^[^\\s|][^|]*$")) {
                Log.d("invalidInvitation", splitMessage[8]);
                return false;
            }
            if (!splitMessage[10].matches("^[^\\s|][^|]*$")) {
                Log.d("invalidInvitation", splitMessage[9]);
                return false;
            }
            if (!splitMessage[11].matches("^\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$")) {
                Log.d("invalidInvitation", splitMessage[10]);
                return false;
            }
            return true;
        }
        Log.d("test", "tst");
        return false;
    }
}
