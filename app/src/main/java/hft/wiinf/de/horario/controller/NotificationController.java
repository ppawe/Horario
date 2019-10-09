package hft.wiinf.de.horario.controller;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.TabActivity;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.InvitationString;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.Repetition;
import hft.wiinf.de.horario.service.NotificationReceiver;

/**
 * This class will do everything related to a notification of an event
 *  - set the a alarm to let the notification happen at a certain time
 *  - delete the alarm if the event was deleted
 *  - delete all alarms if the user does not want to get notifications anymore
 *  - set all alarms after the user allows notifications
 *  If you like to start/delete one/more notifications you just have to use the specified static method and give it the right parameters
 */
public class NotificationController {
    private static final String CHANNEL_ID = "Invitations";

    private static void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Horario";
            String description = "Beschreibung";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    public static void sendInvitationNotification(Context context, InvitationString invitationString) {
        Intent intent = new Intent(context, TabActivity.class);
        intent.putExtra("id", String.valueOf(invitationString.getId()));
        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);
        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_menu_invitation)
                .setContentTitle("Horario")
                .setContentText(invitationString.getTitle() + " " + invitationString.getStartDate() + " " + invitationString.getStartTime())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify(invitationString.getId().intValue(), builder.build());
    }

    /**
     * Method is going to set the alarm x (depends on the user settings) minutes before the event
     * It will be checked if the user has notifications enabled, the event will happen in the future and if it is a repeating event (set
     * Alarm for all single one of them)
     * @param context of the active fragment/activity
     * @param event the event for what the notification (alarm) needs to be set
     */
    public static void setAlarmForNotification(Context context, Event event) {
        if (PersonController.getPersonWhoIam() != null) {
            Person notificationPerson = PersonController.getPersonWhoIam();
            //Check if user has enabled notifications --> if not do not set any alarms
            if (notificationPerson.isEnablePush()) {
                //Get the current time to check if event is in the past or in the future
                Calendar testToday = GregorianCalendar.getInstance();
                testToday.setTimeInMillis(System.currentTimeMillis());

                Intent alarmIntent;
                Date date;
                Calendar calendar;
                PendingIntent pendingIntent;
                AlarmManager manager;

                //If event has happened in the past there will be no new alarms for it
                if (event.getStartTime().after(testToday.getTime())) {
                    alarmIntent = new Intent(context, NotificationReceiver.class);
                    date = event.getStartTime();
                    calendar = GregorianCalendar.getInstance();
                    calendar.setTime(date);

                    //Put some extra Data in the Intent, to show them in the notification
                    alarmIntent.putExtra("Event", event.getShortTitle());
                    alarmIntent.putExtra("Hour", calendar.get(Calendar.HOUR_OF_DAY));
                    //Calculation of the remaining minutes, if minute is less than 10 it would show xx:5 instead of xx:05 --> add a zero (string)
                    if (calendar.get(Calendar.MINUTE) < 10) {
                        alarmIntent.putExtra("Minute", "0" + calendar.get(Calendar.MINUTE));
                    } else {
                        alarmIntent.putExtra("Minute", String.valueOf(calendar.get(Calendar.MINUTE)));
                    }
                    alarmIntent.putExtra("ID", event.getId().intValue());
                    pendingIntent = PendingIntent.getBroadcast(context, event.getId().intValue(), alarmIntent, 0);

                    //Set the alarm and calculate the time with the specified user notification time
                    manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    manager.set(AlarmManager.RTC_WAKEUP, calcNotificationTime(calendar, notificationPerson), pendingIntent);
                }
                //If it is a repeating event we have to set a alarm for every single one
                if (!event.getRepetition().equals(Repetition.NONE)) {
                    List<Event> allEvents = EventController.findRepeatingEvents(event.getId());
                    for (Event repEvent : allEvents) {
                        if (repEvent.getStartTime().after(testToday.getTime())) {
                            alarmIntent = new Intent(context, NotificationReceiver.class);
                            //Get startTime an convert into a Calender to use it
                            date = repEvent.getStartTime();
                            calendar = GregorianCalendar.getInstance();
                            calendar.setTime(date);

                            //Put extra Data which is needed for the Notification
                            alarmIntent.putExtra("Event", repEvent.getShortTitle());
                            alarmIntent.putExtra("Hour", calendar.get(Calendar.HOUR_OF_DAY));
                            if (calendar.get(Calendar.MINUTE) < 10) {
                                alarmIntent.putExtra("Minute", "0" + calendar.get(Calendar.MINUTE));
                            } else {
                                alarmIntent.putExtra("Minute", String.valueOf(calendar.get(Calendar.MINUTE)));
                            }
                            alarmIntent.putExtra("ID", repEvent.getId().intValue());
                            pendingIntent = PendingIntent.getBroadcast(context, repEvent.getId().intValue(), alarmIntent, 0);

                            //Set AlarmManager --> NotificationReceiver will be called
                            manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            manager.set(AlarmManager.RTC_WAKEUP, calcNotificationTime(calendar, notificationPerson), pendingIntent);
                        }
                    }
                }
            }
        }
    }

    /**
     * Method will delete all alarms (notifications)
     * It will be checked if it is a repeating event and if you have given a startEvent or not.
     * If it is startEvent the method will just find all repeatingEvents and delete the alarm for it
     * If it is not a startEvent it will call the same findRepeatingEvent method but with the startEvent as a parameter
     * Finding alarms depends on the EventID in the database (alarm and event have the same id)
     * @param context of the active fragment/activity
     * @param event where alarm should be deleted
     */
    public static void deleteAlarmNotification(Context context, Event event) {
        List<Event> eventsToDelete;
        //Check if we have a startEvent as a parameter
        if (event.getStartEvent() != null) {
            //if no use the DB-Field where the startEvent is referenced
            eventsToDelete = EventController.findRepeatingEvents(event.getStartEvent().getId());
            deleteStartEvent(context, event.getStartEvent());
        } else {
            //if yes use the id of the parameter
            eventsToDelete = EventController.findRepeatingEvents(event.getId());
            deleteStartEvent(context, event);
        }
        for (Event delEvent : eventsToDelete) {
            Intent alarmIntent = new Intent(context, NotificationReceiver.class);
            alarmIntent.putExtra("ID", delEvent.getId().intValue());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, delEvent.getId().intValue(), alarmIntent, 0);

            //Set AlarmManager --> NotificationReceiver will be called
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            manager.cancel(pendingIntent);
        }
    }

    /**
     * Method will be called out of "deleteAlarmNotification" and has not to be used as a single method
     * It will delete the alarm for the startEvent
     * @param context of the active fragment/activity
     * @param event where alarm should be deleted
     */
    private static void deleteStartEvent(Context context, Event event) {
        Intent alarmIntent = new Intent(context, NotificationReceiver.class);
        alarmIntent.putExtra("ID", event.getId().intValue());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, event.getId().intValue(), alarmIntent, 0);

        //Set AlarmManager --> NotificationReceiver will be called
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
    }

    /**
     * Method will delete all alarms which were set.
     * @param context of the active fragment/activity
     */
    public static void deleteAllAlarms(Context context) {
        //Get all events that are in the future to set the alarm
        List<Event> allEvents = EventController.findMyAcceptedEventsInTheFuture();
        for (Event event : allEvents) {
            Intent alarmIntent = new Intent(context, NotificationReceiver.class);

            alarmIntent.putExtra("ID", event.getId().intValue());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, event.getId().intValue(), alarmIntent, 0);

            //Set AlarmManager --> NotificationReceiver will be called
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            manager.cancel(pendingIntent);
        }
    }

    /**
     * Method will start all alarms for all Events that are accepted.
     * @param context of the active fragment/activity
     */
    public static void startAlarmForAllEvents(Context context) {
        //Get all events that are in the future to set the alarm
        List<Event> allEvents = EventController.findMyAcceptedEventsInTheFuture();
        Person person = PersonController.getPersonWhoIam();
        for (Event event : allEvents) {
            Intent alarmIntent = new Intent(context, NotificationReceiver.class);

            //Get startTime an convert into a Calender to use it
            Date date = event.getStartTime();
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime(date);

            //Put extra Data which is needed for the Notification
            alarmIntent.putExtra("Event", event.getShortTitle());
            alarmIntent.putExtra("Hour", calendar.get(Calendar.HOUR_OF_DAY));
            if (calendar.get(Calendar.MINUTE) < 10) {
                alarmIntent.putExtra("Minute", "0" + calendar.get(Calendar.MINUTE));
            } else {
                alarmIntent.putExtra("Minute", String.valueOf(calendar.get(Calendar.MINUTE)));
            }
            alarmIntent.putExtra("ID", event.getId().intValue());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, event.getId().intValue(), alarmIntent, 0);

            //Set AlarmManager --> NotificationReceiver will be called
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            manager.set(AlarmManager.RTC_WAKEUP, calcNotificationTime(calendar, person), pendingIntent);
        }
    }

    /**
     * Method will be called in every other method. You do not need to use it on its own.
     * @param cal startTime of the event as a Object from type Calendar
     * @param person the person Who I am (owner of the app)
     * @return
     */
    private static long calcNotificationTime(Calendar cal, Person person) {
        cal.add(Calendar.MINUTE, ((-1) * person.getNotificationTime()));
        return cal.getTimeInMillis();
    }
}
