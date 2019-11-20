package hft.wiinf.de.horario.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.TabActivity;
import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.EventPersonController;
import hft.wiinf.de.horario.controller.InvitationController;
import hft.wiinf.de.horario.controller.NotificationController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.model.AcceptedState;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.InvitationString;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.ReceivedHorarioSMS;
import hft.wiinf.de.horario.model.Repetition;

/**
 * The type Sms receiver extends a {@link BroadcastReceiver} and reacts each time the phone of the user receives an SMS.
 * The SMS itself is checked against typical RegEx in SQL Injections.
 * After that, the SMS is parsed, for details, look in the methods.
 */
public class SmsReceiver extends BroadcastReceiver {
    private String TAG = SmsReceiver.class.getSimpleName();

    /**
     * Instantiates a new Sms receiver.
     */
    public SmsReceiver() {
    }

    /**
     * Checks if the SMS in question is relevant for the app and continues working on it.
     * The SMS is relevant if the first and last characters are equal to ":Horario: or :HorarioInvitation:".
     * If it is relevant, it is put into an {@link ArrayList} of {@link ReceivedHorarioSMS}
     *
     * @param context, the {@link Context}
     * @param intent,  the {@link Intent}
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the SMS data bound to intent
        Bundle bundle = intent.getExtras();
        SmsMessage[] receivedSMSArray;
        ArrayList<ReceivedHorarioSMS> unreadHorarioSMS = new ArrayList<ReceivedHorarioSMS>();
        boolean isSMSValidAndParseable = false;
        if (bundle != null) {

            // Retrieve the received SMS PDUs from the intent extras
            Object[] pdus = (Object[]) bundle.get("pdus");
            receivedSMSArray = new SmsMessage[pdus.length];

            // For every SMS PDU received create an SMS Message and save it into an array
            for (int i = 0; i < receivedSMSArray.length; i++) {
                // Convert Object array
                receivedSMSArray[i] = SmsMessage.createFromPdu((byte[]) pdus[i], "3gpp");
            }
            //Because of the SMS 160 character limit, long SMS may be received in multiple parts
            //if so the parts are saved in the previousMessages array so they can be concatenated and checked later
            List<String> previousMessages = new ArrayList<>();
            for (int i = 0; i < receivedSMSArray.length; i++) {
                /*collect all the Horario SMS*/
                String message = receivedSMSArray[i].getMessageBody();
                //this part handles Event acceptances and rejections
                // if a valid acceptance or rejection is found a ReceivedHorarioSMS is created from it and saved to a list on which parseHorarioSMSAndUpdate() is called later
                if (message.length() > 9 && message.substring(0, 9).equals(":Horario:") && message.substring(message.length() - 9).equals(":Horario:")) {
                    previousMessages.clear();
                    String number = (receivedSMSArray[i].getOriginatingAddress());
                    String[] parsedSMS = message.substring(9, message.length() - 9).split(",");
                    if (!checkForResponseRegexOk(parsedSMS)) {
                        Log.d("Corrupt SMS Occurence!", message);
                        break;
                    }
                    if (parsedSMS[1].equalsIgnoreCase("1")) {
                        if (parsedSMS.length == 3) {
                            unreadHorarioSMS.add(new ReceivedHorarioSMS(number, true, Integer.parseInt(parsedSMS[0]), null, parsedSMS[2]));
                        }
                    } else {
                        if (parsedSMS.length == 4) {
                            unreadHorarioSMS.add(new ReceivedHorarioSMS(number, false, Integer.parseInt(parsedSMS[0]), parsedSMS[3], parsedSMS[2]));
                        }
                    }
                    isSMSValidAndParseable = true;
                }
                // this part handles invitations
                // if a valid invitation is found an InvitationString is created from the message
                // if the event in question is not in the past an Event to which the user is invited is created from the InvitationString
                // finally calls the method that sends a notification about the notification to the user
                else if (message.length() > 19 && message.substring(0, 19).equals(":HorarioInvitation:")) {
                    previousMessages.clear();
                    previousMessages.add(message);
                    StringBuilder fullmessageBuilder = new StringBuilder();
                    for (String previousMessage : previousMessages) {
                        fullmessageBuilder.append(previousMessage);
                    }
                    if (InvitationController.checkForInvitationRegexOk(fullmessageBuilder.toString())) {
                        InvitationString newInvitationString = new InvitationString(fullmessageBuilder.toString().replaceAll(":HorarioInvitation:", ""),
                                new Date(), receivedSMSArray[i].getOriginatingAddress());
                        String eventDateTimeString = newInvitationString.getStartTime() + " " + newInvitationString.getStartDate();
                        SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
                        try {
                            Date eventDateTime = format.parse(eventDateTimeString);

                            if (eventDateTime.after(newInvitationString.getDateReceived()) && newInvitationString.getRepetitionAsRepetition() == Repetition.NONE
                                    || newInvitationString.getRepetitionAsRepetition() != Repetition.NONE &&
                                    newInvitationString.getEndDateAsDate().after(newInvitationString.getDateReceived())) {
                                if (!InvitationController.eventAlreadySaved(newInvitationString)) {
                                    Event invitedEvent = EventController.createInvitedEventFromInvitation(newInvitationString);
                                    Person creator = PersonController.addOrGetPerson(newInvitationString.getCreatorPhoneNumber(), newInvitationString.getCreatorName());
                                    creator.setName(newInvitationString.getCreatorName());
                                    NotificationController.sendInvitationNotification(context, newInvitationString, invitedEvent);
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // this part handles multipart SMS Invitations
                // because of the character limit for excuses, acceptances and rejections will never be sent as a multipart SMS
                // otherwise identical to the branch above
                else if (previousMessages.size() != 0) {
                    previousMessages.add(message);
                    StringBuilder fullmessageBuilder = new StringBuilder();
                    for (String previousMessage : previousMessages) {
                        fullmessageBuilder.append(previousMessage);
                    }
                    if (InvitationController.checkForInvitationRegexOk(fullmessageBuilder.toString())) {
                        InvitationString newInvitationString = new InvitationString(fullmessageBuilder.toString().replaceAll(":HorarioInvitation:", ""), new Date(), receivedSMSArray[i].getOriginatingAddress());
                        String eventDateTimeString = newInvitationString.getStartTime() + " " + newInvitationString.getStartDate();
                        SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
                        try {
                            Date eventDateTime = format.parse(eventDateTimeString);
                            if (eventDateTime.after(newInvitationString.getDateReceived()) && newInvitationString.getRepetitionAsRepetition() == Repetition.NONE
                                    || newInvitationString.getRepetitionAsRepetition() != Repetition.NONE &&
                                    newInvitationString.getEndDateAsDate().after(newInvitationString.getDateReceived())) {
                                if (!InvitationController.eventAlreadySaved(newInvitationString)) {
                                    Event invitedEvent = EventController.createInvitedEventFromInvitation(newInvitationString);
                                    Person creator = PersonController.addOrGetPerson(newInvitationString.getCreatorPhoneNumber(), newInvitationString.getCreatorName());
                                    creator.setName(newInvitationString.getCreatorName());
                                    NotificationController.sendInvitationNotification(context, newInvitationString, invitedEvent);
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        previousMessages.clear();
                    }
                }
            }
            if (isSMSValidAndParseable) {
                parseHorarioSMSAndUpdate(unreadHorarioSMS, context);
            }

        }
    }

    /**
     * Takes the received message checks for potential SQL Injections and validity of the content
     *
     * @param smsTextSplit, an {@link java.util.Arrays} of {@link String}
     * @return {@code true} if the SMS in question is valid and ready for the next method.
     */
    private boolean checkForResponseRegexOk(String[] smsTextSplit) {
        // RegEx: NO SQL Injections allowed PLUS check if SMS is valid
        // smsTextSplit[0]= CreatorEventId, should be only number greater than 0
        // smsTextSplit[1]= boolean for acceptance, should be only 0 or 1
        // smsTextSplit[2]= String for name, only Chars and points
        // smsTextSplit[3]= Excuse as String, needs to be split again by "!" and checked on two strings
        if (smsTextSplit.length == 3 || smsTextSplit.length == 4) {
            boolean isAcceptance = smsTextSplit.length == 3;

            //Make Patterns
            Pattern pattern_onlyGreatherThan0 = Pattern.compile("^[^0\\D]\\d*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Pattern pattern_only0Or1 = Pattern.compile("([01])", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Pattern pattern_onlyAlphanumsAndPointsAndWhitespaces = Pattern.compile("(\\w|\\s|\\.)*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            //Make the matchers
            Matcher m_pattern_onlyGreatherThan0 = pattern_onlyGreatherThan0.matcher(smsTextSplit[0]);
            Matcher m_pattern_only0Or1 = pattern_only0Or1.matcher(smsTextSplit[1]);
            Matcher m_pattern_onlyAlphanumsAndPointsAndWhitespaces = pattern_onlyAlphanumsAndPointsAndWhitespaces.matcher(smsTextSplit[2]);
            Matcher m_pattern_onlyAlphanumsAndPointsAndWhitespacesRejectionReason = null;
            Matcher m_pattern_onlyAlphanumsAndPointsAndWhitespacesRejectionNote = null;

            //Do only if it is a rejection of an event
            if (!isAcceptance) {
                String[] excuseSplit = smsTextSplit[3].split("!");
                if (excuseSplit.length == 2) {
                    m_pattern_onlyAlphanumsAndPointsAndWhitespacesRejectionReason = pattern_onlyAlphanumsAndPointsAndWhitespaces.matcher(excuseSplit[0]);
                    m_pattern_onlyAlphanumsAndPointsAndWhitespacesRejectionNote = pattern_onlyAlphanumsAndPointsAndWhitespaces.matcher(excuseSplit[1]);

                } else {
                    return false;
                }
            }

            if (!m_pattern_onlyGreatherThan0.matches()) {
                Log.d("SMSRECEIVER", "Unvalid Id Part");
                return false;
            }
            if (!m_pattern_only0Or1.matches()) {
                Log.d("SMSRECEIVER", "Unvalid Acceptance boolean part");
                return false;
            }
            if (!m_pattern_onlyAlphanumsAndPointsAndWhitespaces.matches()) {
                Log.d("SMSRECEIVER", "Unvalid Alphanum/Dot/Whitespace sequence in name of participant");
                return false;
            }
            if (!isAcceptance) {
                if (!m_pattern_onlyAlphanumsAndPointsAndWhitespacesRejectionReason.matches()) {
                    Log.d("SMSRECEIVER", "REASONUnvalid Alphanum/Dot/Whitespace sequence in name of participant");
                    return false;
                }
                if (!m_pattern_onlyAlphanumsAndPointsAndWhitespacesRejectionNote.matches()) {
                    Log.d("SMSRECEIVER", "NOTEUnvalid Alphanum/Dot/Whitespace sequence in name of participant");
                    return false;
                }
            }
            return true;
        } else {
            //SMS is not split correctly -> wrong syntax therefore corrupt SMS
            return false;
        }
    }

    /**
     * Iterates through the {@link List} of ReceivedHorarioSMS and creates or gets a {@link Person} that sent the SMS
     * For each {@link Event} an {@link hft.wiinf.de.horario.model.EventPerson} is created and its status is set according to the value in the SMS
     * For more information, look into the code comments.
     *
     * @param unreadSMS, a {@link List} of {@link ReceivedHorarioSMS} to parse
     * @param context,   the {@link Context}
     */
    private void parseHorarioSMSAndUpdate(List<ReceivedHorarioSMS> unreadSMS, Context context) {
        for (ReceivedHorarioSMS singleUnreadSMS : unreadSMS) {
            //if the phone number already has a Person associated with it get that Person else create one
            Person person = PersonController.getPersonViaPhoneNumber(singleUnreadSMS.getPhonenumber());
            if (person == null) {
                person = new Person(singleUnreadSMS.getPhonenumber(), singleUnreadSMS.getName());
            }
            String savedContactExisting;
            savedContactExisting = lookForSavedContact(singleUnreadSMS.getPhonenumber(), context);

            /*Replace name of the Person if they are saved in the user's contacts*/
            if (savedContactExisting != null) {
                person.setName(savedContactExisting);
            } else {
                person.setName(singleUnreadSMS.getName());
            }
            person.save();
            //check if an Event with the given ID exists in the database
            Long eventIdInSMS = Long.valueOf(singleUnreadSMS.getCreatorEventId());
            if (!EventController.checkIfEventIsInDatabaseViaId(eventIdInSMS)) {
                addNotification(context, 1, person.getName(), singleUnreadSMS.isAcceptance());
                break;
            }
            //Check if is SerialEvent or not
            if (isSerialEvent(eventIdInSMS)) {
                List<Event> myEvents = EventController.getMyEventsFromReceivedCreatorEventId(eventIdInSMS);
                if (singleUnreadSMS.isAcceptance()) {
                    //acceptance
                    for (Event event : myEvents) {
                        //add the Event-Person relationship and set its status to accepted
                        EventPersonController.addOrGetEventPerson(event, person, AcceptedState.ACCEPTED);
                        EventPersonController.changeStatus(event, person, AcceptedState.ACCEPTED, null);
                    }
                } else {
                    //cancellation
                    for (Event event : myEvents) {
                        //add the Event-Person relationship and set its status to rejected
                        EventPersonController.addOrGetEventPerson(event, person, AcceptedState.REJECTED);
                        EventPersonController.changeStatus(event, person, AcceptedState.REJECTED, singleUnreadSMS.getExcuse());
                    }
                }
            } else {
                //it is a single event
                if (singleUnreadSMS.isAcceptance()) {
                    //acceptance
                    Event event = EventController.getEventById(eventIdInSMS);
                    if (event != null) {
                        //add the Event-Person relationship and set its status to accepted
                        EventPersonController.addOrGetEventPerson(event, person, AcceptedState.ACCEPTED);
                        EventPersonController.changeStatus(event, person, AcceptedState.ACCEPTED, null);
                    }
                } else {
                    //cancellation
                    Event event = EventController.getEventById(eventIdInSMS);
                    if (event != null) {
                        //add the Event-Person relationship and set its status to rejected
                        EventPersonController.addOrGetEventPerson(event, person, AcceptedState.REJECTED);
                        EventPersonController.changeStatus(event, person, AcceptedState.REJECTED, singleUnreadSMS.getExcuse());
                    }
                }
            }

        }
    }

    /**
     * checks the database for a potential startEvent in the database
     *
     * @param eventIdInSMS, the {@link Long} number of the event
     * @return {@code true} if it is a serial {@link Event}
     */
    private boolean isSerialEvent(Long eventIdInSMS) {
        try {
            Event x = EventController.getEventById(eventIdInSMS).getStartEvent();
            return x != null;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * removes all the symbols in a phone number
     * do not use this to save phone numbers into the database
     * phone numbers should be saved in E.164 format
     * @// TODO: 14.10.19 actually just remove this entirely if you find the time because it only works for german numbers
     *
     * @param number, a {@link String}
     * @return a {@link String} of the shorter number
     */
    private String shortenPhoneNumber(String number) {
        /*Take out all the chars not being numbers and return the numbers after "1" (German mobile number!!!)*/
        number = number.replace("(", "");
        number = number.replace(")", "");
        number = number.replace("+", "");
        number = number.replace("-", "");
        number = number.replace(" ", "");
        try {
            number = number.substring(number.indexOf("1"));
            Log.d("SHORTIFY Nummer", number);
            return number;
        } catch (StringIndexOutOfBoundsException variablenname) {
            // Ausländische Nummer
        }
        return "100000000";
    }

    /**
     * Get all the contacts, see if number is identical after "shortening" it, if identical, return the name
     *
     * @param address, a {@link String} of the number
     * @param context, the {@link Context}
     * @return a {@link String} of the renamed contact
     */
    private String lookForSavedContact(String address, Context context) {
        /*Get all the contacts, see if number is identical after "shortifying" it, if identical, replace the name*/
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        // if there are more than 0 contacts
        if ((c != null ? c.getCount() : 0) > 0) {
            while (c.moveToNext()) {
                String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
                String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                // if more than 0 contacts have phone numbers
                if (c.getInt(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    if (pCur != null) {
                        while (pCur.moveToNext()) {
                            // check if the saved number is equal to the received number, if so return the name
                            String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            phoneNo = shortenPhoneNumber(phoneNo);
                            address = shortenPhoneNumber(address);
                            if (phoneNo.equals(address)) {
                                pCur.close();
                                return name;
                            }
                        }
                        pCur.close();
                    }
                }
            }
            c.close();
        }
        if (c != null) {
            c.close();
        }
        return null;
    }

    /**
     * Creates a notification with the text in case the received creatorEventId in the SMS doesn't exist in the database
     *
     * @param context, a {@link Context}
     * @param id,      some {@link int} required
     * @param person,  a {@link String} of the name of the person in question
     */
    private void addNotification(Context context, int id, String person, boolean isAcceptance) {
        String contentText;
        if (isAcceptance) {
            contentText = "Horario hat festgestellt, dass Du eine Benachrichtigung zu einem Termin bekommen hast, der nicht mehr vorhanden ist." +
                    "Vermutlich hast Du Horario neu installiert, bitte kontaktiere doch folgende Person, um ihren zuletzt zugesagten Termin zu überprüfen: " +
                    person;
        } else {
            contentText = "Horario hat festgestellt, dass Du eine Benachrichtigung zu einem Termin bekommen hast, der nicht mehr vorhanden ist." +
                    "Vermutlich hast Du Horario neu installiert, bitte kontaktiere doch folgende Person, um ihren zuletzt abgesagten Termin zu überprüfen: " +
                    person;
        }
        String title = "Ups!";
        Intent notificationIntent = new Intent(context, TabActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // as of android version 26 a NotificationChannel needs to be created before setting a notification
        if (Build.VERSION.SDK_INT >= 26) {
            // Add as notification
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // The id of the channel.
            String channel_id = String.valueOf(id);
            //The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(channel_id, title, importance);
            // Configure the notification channel.
            mChannel.setDescription(contentText);
            mChannel.enableLights(true);
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            mChannel.setLightColor(Color.GREEN);
            mChannel.enableVibration(false);
            manager.createNotificationChannel(mChannel);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, channel_id)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(title)
                            .setContentText(contentText).setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(contentText));

            builder.setContentIntent(contentIntent);
            manager.notify(id, builder.build());
        } else {
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, "")
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(title)
                            .setContentText(contentText).setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(contentText));

            builder.setContentIntent(contentIntent);

            // Add as notification
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(id, builder.build());
        }
    }
}
