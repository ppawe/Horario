package hft.wiinf.de.horario.view;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.EventPersonController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.model.AcceptedState;

/**
 * Fragment containing an interactive calendar capable of displaying the current date as well as marking any {@link Event}s the user accepted
 * also displays a list of all {@link Appointment}s below the calendar and has FABs for adding a new event,
 * scanning a QR Code or showing a list of invitations for the user
 */
public class CalendarFragment extends Fragment {
    private static final String TAG = "CalendarFragmentActivity";

    private static CompactCalendarView calendarCvCalendar;
    private static DateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private static Date selectedMonth;
    private static List<hft.wiinf.de.horario.model.Event> eventListCalendar = new ArrayList<>();
    private static List<hft.wiinf.de.horario.model.Event> allEvents = new ArrayList<>();
    private ListView calendarLvList;
    private TextView calendarTvMonth;
    private TextView calendarTvDay;
    private TextView calendarIsFloatMenuOpen;
    private TextView calendarTvInvitationNumber;
    private FloatingActionButton calendarFcMenu;
    private FloatingActionButton calendarFcQrScan;

    static DateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    static DateFormat dayFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
    private FloatingActionButton calendarFcNewEvent;
    private FloatingActionButton calendarFcInvitations;
    private ConstraintLayout cLayout_calendar_main;
    private Context context = null;
    private Animation ActionButtonOpen;
    private Animation ActionButtonClose;
    private Animation ActionButtonRotateRight;
    private Animation ActionButtonRotateLeft;
    private AlphaAnimation fadeIn;
    private AlphaAnimation fadeOut;

    /**
     * marks all accepted events in the database on the calendar with a dark grey dot
     */
    public static void updateCompactCalendar() {
        if(PersonController.getPersonWhoIam() != null) {
            List<hft.wiinf.de.horario.model.Event> acceptedEvents = EventPersonController.getAllAcceptedEventsForPerson(PersonController.getPersonWhoIam());
            for (int i = 0; i < acceptedEvents.size(); i++) {
                if (calendarCvCalendar.getEvents(acceptedEvents.get(i).getStartTime().getTime()).size() == 0 &&
                        EventPersonController.getEventPerson(acceptedEvents.get(i), PersonController.getPersonWhoIam()).getStatus().equals(AcceptedState.ACCEPTED)) {
                    Event event = new Event(Color.DKGRAY, acceptedEvents.get(i).getStartTime().getTime());
                    calendarCvCalendar.addEvent(event, true);
                    Calendar nextDay = Calendar.getInstance();
                    nextDay.setTime(acceptedEvents.get(i).getStartTime());
                    nextDay.add(Calendar.DATE,1);
                    while(acceptedEvents.get(i).getEndTime().after(nextDay.getTime())){
                        Event event1 = new Event(Color.DKGRAY, nextDay.getTimeInMillis());
                        calendarCvCalendar.addEvent(event1, true);
                        nextDay.add(Calendar.DATE,1);
                    }

                }
            }
        }
    }

    /**
     * updates the list of {@link hft.wiinf.de.horario.model.Event}s, the displayed date and month
     * as well as the calendar to reflect the selected date
     *
     * @param date the new selected date
     */
    private void update(Date date) {
        calendarTvDay.setText(dayFormat.format(date));
        calendarLvList.setAdapter(getAdapter(date));
        calendarTvMonth.setText(monthFormat.format(date));
        updateCompactCalendar();
    }

    /**
     * initializes the fragment's view variables and sets the initial look of the fragment
     * sets OnClickListeners for all FABs, the event list items and the calendar
     *
     * @param inflater           LayoutInflater used for inflating the layout into views
     * @param container          the parent view of the fragment
     * @param savedInstanceState the saved state of the fragment from before some system event changed it
     * @return the inflated view with all the changes applied to it
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //initialize variables
        final View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        calendarCvCalendar = view.findViewById(R.id.calendarCvCalendar);
        calendarTvMonth = view.findViewById(R.id.calendarTvMonth);
        calendarLvList = view.findViewById(R.id.calendarLvList);
        calendarTvDay = view.findViewById(R.id.calendarTvDay);
        calendarTvInvitationNumber = view.findViewById(R.id.calendar_tvInvitationNumber);
        context = this.getActivity();
        //FloatingButton
        calendarFcMenu = view.findViewById(R.id.calendar_floatingActionButtonMenu);
        calendarFcNewEvent = view.findViewById(R.id.calendar_floatingActionButtonNewEvent);
        calendarFcQrScan = view.findViewById(R.id.calendar_floatingActionButtonScan);
        calendarFcInvitations = view.findViewById(R.id.calendar_floatingActionButtonInvites);
        cLayout_calendar_main = view.findViewById(R.id.calendar_constrainLayout_main);
        calendarIsFloatMenuOpen = view.findViewById(R.id.calendar_hiddenField);

        //Animations
        ActionButtonOpen = AnimationUtils.loadAnimation(getContext(), R.anim.actionbuttonopen);
        ActionButtonClose = AnimationUtils.loadAnimation(getContext(), R.anim.actionbuttonclose);
        ActionButtonRotateRight = AnimationUtils.loadAnimation(getContext(), R.anim.actionbuttonrotateright);
        ActionButtonRotateLeft = AnimationUtils.loadAnimation(getContext(), R.anim.actionbuttonrotateleft);
        fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeOut = new AlphaAnimation(1.0f, 0.0f);

        calendarFcQrScan.hide();
        calendarFcNewEvent.hide();
        calendarFcInvitations.hide();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        selectedMonth = today.getTime();
        calendarTvMonth.setText(monthFormat.format(today.getTime())); //initialize month field

        update(today.getTime());

        calendarCvCalendar.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            //when a day get clicked, the date field will be updated and the events for the day displayed in the ListView
            public void onDayClick(Date dateClicked) {
                update(dateClicked);
                closeFABMenu();
            }

            @Override
            //handle everything when the user swipe the month
            public void onMonthScroll(Date firstDayOfNewMonth) {
                update(firstDayOfNewMonth);
                selectedMonth = firstDayOfNewMonth;
            }
        });

        //handle actions after a event entry get clicked
        calendarLvList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Appointment selectedItem = (Appointment) parent.getItemAtPosition(position);
                closeFABMenu();
                // 0 = date, 1 = accepted, 2 = waiting, 3 = own
                switch (selectedItem.getType()) {
                    case 1:
                        AcceptedEventDetailsFragment acceptedEventDetailsFragment = new AcceptedEventDetailsFragment();
                        Bundle bundleAcceptedEventId = new Bundle();
                        bundleAcceptedEventId.putLong("EventId", selectedItem.getId());
                        bundleAcceptedEventId.putString("fragment", "Calendar");
                        acceptedEventDetailsFragment.setArguments(bundleAcceptedEventId);
                        FragmentTransaction fr1 = getFragmentManager().beginTransaction();
                        fr1.replace(R.id.calendar_frameLayout, acceptedEventDetailsFragment, "CalendarFragment");
                        fr1.addToBackStack("CalendarFragment");
                        fr1.commit();
                        break;
                    case 2:
                        SavedEventDetailsFragment savedEventDetailsFragment = new SavedEventDetailsFragment();
                        Bundle bundleSavedEventId = new Bundle();
                        bundleSavedEventId.putLong("EventId", selectedItem.getId());
                        bundleSavedEventId.putString("fragment", "Calendar");
                        savedEventDetailsFragment.setArguments(bundleSavedEventId);
                        FragmentTransaction fr2 = getFragmentManager().beginTransaction();
                        fr2.replace(R.id.calendar_frameLayout, savedEventDetailsFragment, "CalendarFragment");
                        fr2.addToBackStack("CalendarFragment");
                        fr2.commit();
                        break;
                    case 3:
                        MyOwnEventDetailsFragment myOwnEventDetailsFragment = new MyOwnEventDetailsFragment();
                        Bundle bundleMyOwnEventId = new Bundle();
                        bundleMyOwnEventId.putLong("EventId", selectedItem.getId());
                        bundleMyOwnEventId.putString("fragment", "Calendar");
                        myOwnEventDetailsFragment.setArguments(bundleMyOwnEventId);
                        FragmentTransaction fr3 = getFragmentManager().beginTransaction();
                        fr3.replace(R.id.calendar_frameLayout, myOwnEventDetailsFragment, "CalendarFragment");
                        fr3.addToBackStack("CalendarFragment");
                        fr3.commit();
                        break;
                    default:
                        break;
                }

            }
        });

        calendarFcMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (calendarIsFloatMenuOpen.getText().equals("false")) {
                    showFABMenu();
                    calendarIsFloatMenuOpen.setText(R.string.wahr);
                } else {
                    closeFABMenu();
                    calendarIsFloatMenuOpen.setText(R.string.falsch);
                }
            }
        });

        calendarFcNewEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewEventFragment newEventFragment = new NewEventFragment();
                Bundle bundle = new Bundle();
                bundle.putString("fragment", "Calendar");
                newEventFragment.setArguments(bundle);

                FragmentTransaction fr = getFragmentManager().beginTransaction();
                fr.replace(R.id.calendar_frameLayout, newEventFragment, "NewEvent");
                fr.addToBackStack("NewEvent");
                fr.commit();
                closeFABMenu();
            }
        });

        calendarFcInvitations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InvitationFragment invitationFragment = new InvitationFragment();
                Bundle bundle = new Bundle();
                bundle.putString("fragment", "Calendar");
                invitationFragment.setArguments(bundle);

                FragmentTransaction fr = getFragmentManager().beginTransaction();
                fr.replace(R.id.calendar_frameLayout, invitationFragment);
                fr.addToBackStack(null);
                fr.commit();
                closeFABMenu();
            }
        });

        calendarFcQrScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QRScanFragment qrScanFragment = new QRScanFragment();
                Bundle bundle = new Bundle();
                bundle.putString("fragment", "Calendar");
                qrScanFragment.setArguments(bundle);

                FragmentTransaction fr = getFragmentManager().beginTransaction();
                fr.replace(R.id.calendar_frameLayout, qrScanFragment, "QrScan");
                fr.addToBackStack("QrScan");
                fr.commit();
                closeFABMenu();
            }
        });

        cLayout_calendar_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFABMenu();
            }
        });
        if (PersonController.getPersonWhoIam() != null) {
            calendarTvInvitationNumber.setText(String.valueOf(EventPersonController.getNumberOfInvitedEventsForPerson(PersonController.getPersonWhoIam())));
        }
        return view;
    }

    /**
     * gets all {@link hft.wiinf.de.horario.model.Event}s the user has saved, rejected or participates in for a given date
     * turns those events into {@link Appointment}s and returns an ArrayAdapter that turns those appointments
     * into views for a listview
     * @param date the date for which the events should be displayed
     * @return an ArrayAdapter that creates views for every event on the given date
     */
    private ArrayAdapter getAdapter(Date date) {
        final ArrayList<Appointment> eventsAsAppointments = new ArrayList<>();

        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTime(date);
        startOfDay.set(Calendar.SECOND, -1);
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(date);
        endOfDay.add(Calendar.DAY_OF_MONTH, 1);
        endOfDay.add(Calendar.SECOND, -1);

        allEvents = EventController.getAllEvents();

        eventListCalendar.clear();
        for (hft.wiinf.de.horario.model.Event event : allEvents) {
            if (event.getStartTime().after(startOfDay.getTime()) && event.getStartTime().before(endOfDay.getTime()) ||
            event.getStartTime().before(startOfDay.getTime()) && event.getEndTime().after(endOfDay.getTime()) ||
            event.getEndTime().after(startOfDay.getTime()) && event.getEndTime().before(endOfDay.getTime())) {
                eventListCalendar.add(event);
            }
        }
        String description;
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        for (hft.wiinf.de.horario.model.Event event : eventListCalendar) {
            start.setTime(event.getStartTime());
            end.setTime(event.getEndTime());
            if(start.get(Calendar.DAY_OF_YEAR) == end.get(Calendar.DAY_OF_YEAR) && start.get(Calendar.YEAR) == end.get(Calendar.YEAR)) {
                description = timeFormat.format(event.getStartTime()) + " - " + timeFormat.format(event.getEndTime()) + " " + event.getShortTitle();
            }else{
                DateFormat dayFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
                description = dayFormat.format(event.getStartTime()) + " - " + dayFormat.format(event.getEndTime()) + " " + event.getShortTitle();
            }
            if (event.getCreator().equals(PersonController.getPersonWhoIam())) {
                eventsAsAppointments.add(new Appointment(description, 3, event.getId(), event.getCreator()));
            } else {
                if (EventPersonController.getEventPerson(event, PersonController.getPersonWhoIam()).getStatus().equals(AcceptedState.ACCEPTED)) {
                    eventsAsAppointments.add(new Appointment(description, 1, event.getId(), event.getCreator()));
                } else if (EventPersonController.getEventPerson(event, PersonController.getPersonWhoIam()).getStatus().equals(AcceptedState.WAITING)) {
                    eventsAsAppointments.add(new Appointment(description, 2, event.getId(), event.getCreator()));
                }
            }
        }
        return new ArrayAdapter<Appointment>(context, android.R.layout.simple_list_item_1, eventsAsAppointments) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                // 0 = date, 1 = accepted, 2 = waiting, 3 = own
                if (eventsAsAppointments.get(position).getType() == 1) {
                    textView.setTextColor(Color.DKGRAY);
                    textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_mydate_approved, 0);
                } else if (eventsAsAppointments.get(position).getType() == 2) {
                    textView.setTextColor(Color.DKGRAY);
                    textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_mydate_questionmark, 0);
                } else if (eventsAsAppointments.get(position).getType() == 3) {
                    textView.setTextColor(Color.DKGRAY);
                    textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_mydate, 0);
                } else if (eventsAsAppointments.get(position).getType() == 0) {
                    textView.setTextColor(Color.BLACK);
                    textView.setBackgroundColor(Color.WHITE);
                    textView.setFocusable(false);
                }
                textView.setText(eventsAsAppointments.get(position).getDescription());
                return textView;
            }
        };
    }

    /**
     * starts animations to open all FABs and displays the invitation alert if there are any invitations
     */
    private void showFABMenu() {
        calendarFcQrScan.startAnimation(ActionButtonOpen);
        calendarFcNewEvent.startAnimation(ActionButtonOpen);
        calendarFcInvitations.startAnimation(ActionButtonOpen);
        calendarFcMenu.startAnimation(ActionButtonRotateRight);
        calendarFcQrScan.setClickable(true);
        calendarFcInvitations.setClickable(true);
        calendarFcNewEvent.setClickable(true);
        calendarIsFloatMenuOpen.setText(R.string.wahr);
        calendarFcQrScan.show();
        calendarFcNewEvent.show();
        calendarFcInvitations.show();
        calendarFcMenu.setImageResource(R.drawable.ic_plusmenu);
        if (EventPersonController.getNumberOfInvitedEventsForPerson(PersonController.getPersonWhoIam()) > 0) {
            calendarTvInvitationNumber.setText(String.valueOf(EventPersonController.getNumberOfInvitedEventsForPerson(PersonController.getPersonWhoIam())));
            calendarTvInvitationNumber.startAnimation(fadeIn);
            fadeIn.setDuration(300);
            calendarTvInvitationNumber.setVisibility(View.VISIBLE);
        }
    }

    /**
     * starts animations to close all open FABs
     */
    private void closeFABMenu() {
        if (calendarIsFloatMenuOpen.getText().equals("true")) {
            calendarFcQrScan.startAnimation(ActionButtonClose);
            calendarFcNewEvent.startAnimation(ActionButtonClose);
            calendarFcInvitations.startAnimation(ActionButtonClose);
            calendarFcMenu.startAnimation(ActionButtonRotateLeft);
            calendarFcQrScan.setClickable(false);
            calendarFcInvitations.setClickable(false);
            calendarFcNewEvent.setClickable(false);
            calendarIsFloatMenuOpen.setText(R.string.falsch);
            calendarFcInvitations.hide();
            calendarFcQrScan.hide();
            calendarFcNewEvent.hide();
            calendarFcMenu.setImageResource(R.drawable.ic_plusmenu);
            calendarTvInvitationNumber.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * this method is called every time the state of the fragment has changed and becomes active again
     * updates the invitation number alert textview
     */
    @Override
    public void onResume() {
        super.onResume();
        if (PersonController.getPersonWhoIam() != null) {
            calendarTvInvitationNumber.setText(String.valueOf(EventPersonController.getNumberOfInvitedEventsForPerson(PersonController.getPersonWhoIam())));
        }
    }
}
