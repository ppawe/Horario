package hft.wiinf.de.horario.model;

import android.support.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * represents an InvitationString the User received via some medium but hasn't responded to yet
 */

public class InvitationString {
    private String invitation;

    private Date dateReceived;

    public InvitationString(String invitation, Date dateReceived) {
        this.invitation = invitation;
        this.dateReceived = dateReceived;
    }


    public Date getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(Date dateReceived) {
        this.dateReceived = dateReceived;
    }

    public String getInvitation() {
        return invitation;
    }

    public void setInvitation(@NonNull String invitation) {
        this.invitation = invitation;
    }

    public String getCreatorEventId(){
        return invitation.split(" \\| ")[0];
    }

    public String getStartDate(){
        return invitation.split(" \\| ")[1];
    }

    public Date getStartDateAsDate() {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        try {
            return format.parse(getStartDate());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getEndDate(){
        return invitation.split(" \\| ")[2];
    }

    public Date getEndDateAsDate() {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
        try {
            return format.parse(getEndDate());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getStartTime(){
        return invitation.split(" \\| ")[3];
    }

    public Date getStartTimeAsDate() {
        String startTimeString = getStartTime() + " " + getStartDate();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
        try {
            return format.parse(startTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getEndTime(){
        return invitation.split(" \\| ")[4];
    }

    public Date getEndTimeAsDate() {
        String startTimeString = getEndTime() + " " + getStartDate();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
        try {
            return format.parse(startTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getRepetition(){
        return invitation.split(" \\| ")[5];
    }

    public Repetition getRepetitionAsRepetition() {
        switch (getRepetition()) {
            case "DAILY":
                return Repetition.DAILY;
            case "WEEKLY":
                return Repetition.WEEKLY;
            case "MONTHLY":
                return Repetition.MONTHLY;
            case "YEARLY":
                return Repetition.YEARLY;
            case "NONE":
            default:
                return Repetition.NONE;
        }
    }

    public String getTitle(){
        return invitation.split(" \\| ")[6];
    }

    public String getPlace(){
        return invitation.split(" \\| ")[7];
    }

    public String getDescription(){
        return invitation.split(" \\| ")[8];
    }

    public String getCreatorName(){
        return invitation.split(" \\| ")[9];
    }

    public String getCreatorPhoneNumber(){
        return invitation.split(" \\| ")[10];
    }


}

