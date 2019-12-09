package hft.wiinf.de.horario;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.EventPersonController;
import hft.wiinf.de.horario.controller.NoScanResultExceptionController;
import hft.wiinf.de.horario.controller.NotificationController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.controller.ScanResultReceiverController;
import hft.wiinf.de.horario.controller.SendSmsController;
import hft.wiinf.de.horario.model.AcceptedState;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.InvitationString;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.Repetition;
import hft.wiinf.de.horario.view.CalendarActivity;
import hft.wiinf.de.horario.view.CalendarFragment;
import hft.wiinf.de.horario.view.EventOverviewActivity;
import hft.wiinf.de.horario.view.EventOverviewFragment;
import hft.wiinf.de.horario.view.EventRejectEventFragment;
import hft.wiinf.de.horario.view.InvitationFragment;
import hft.wiinf.de.horario.view.SettingsActivity;

import static com.activeandroid.Cache.getContext;

/**
 * The main activity of Horario. This activity contains the tabs with the 3 main fragments
 * {@link EventOverviewActivity}, {@link CalendarActivity}, {@link SettingsActivity} that can be switched
 * between via a TabLayout. Also acts as a handler for QR Code scan results from {@link hft.wiinf.de.horario.view.QRScanFragment}
 * and {@link Event}s that the user has been invited to and interacted with in {@link InvitationFragment} or a notification,
 * presenting the user with a dialog allowing the user to accept, reject or save the event.
 * At the first start of the app requests several essential permissions and information from the user
 * such as their user name, permission to send and read SMS or access the user's contacts
 */
public class TabActivity extends AppCompatActivity implements ScanResultReceiverController, InvitationFragment.OnListFragmentInteractionListener {

    private static final String TAG = "TabActivity";
    private static final int PERMISSION_REQUEST_READ_PHONE_STATE = 0;
    private int PERMISSION_REQUEST_RECEIVE_SMS = 1;
    private int PERMISSION_REQUEST_READ_CONTACTS = 2;
    private SectionsPageAdapter mSectionsPageAdapter;
    private ViewPager mViewPager;
    private TabLayout tabLayout;
    private static int startTab;
    private Person personMe;
    private Person personEventCreator;
    private Event invitedEvent;

    private Event singleEvent;
    //Index: 0 = CreatorID; 1 = StartDate; 2 = EndDate; 3 = StartTime; 4 = EndTime;
    //       5 = Repetition; 6 = ShortTitle; 7 = Place; 8 = Description;  9 = EventCreatorName
    private String eventCreatorEventId, eventStartDate, eventEndDate, eventStartTime, eventEndTime, eventRepetition, eventShortTitle, eventPlace,
            eventDescription, eventCreatorName, eventCreatorPhoneNumber;
    private String hourOfDay, minutesOfDay, year, month, day;

    private Calendar myStartTime = Calendar.getInstance();
    private Calendar myEndTime = Calendar.getInstance();
    private Calendar myEndDate = Calendar.getInstance();

    private int buttonId = 0;
    private int counter = 0;
    private int counterSMS = 0;
    private int counterCONTACTS = 0;


    /**
     * if a new intent is received (such as one received from an invitation notification)
     *
     * @param intent the newly received intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    /**
     * called when a list item has been interacted with in {@link InvitationFragment}
     * opens the dialog that allows the user to decide what to do with the {@link Event}
     * they have been invited to
     *
     * @param event the event that the user selected in the list of invitations
     */
    @Override
    public void onListFragmentInteraction(Event event) {

        this.invitedEvent = event;

        if (mViewPager.getCurrentItem() == 0) {
            openActionDialogAfterScanning("EventOverview");
        } else {
            openActionDialogAfterScanning("Calendar");
        }

    }

    /**
     * initializes the database and the app's layout/views, selects the user's preferred start tab
     * and asks the user for essential information/permissions if they are not yet granted
     * @param savedInstanceState the saved state of the activity from before a system event changed it
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Start DB
        ActiveAndroid.initialize(this);

        setContentView(R.layout.activity_tab);


        //read startTab out of db, default=1(calendar tab)
        personMe = PersonController.getPersonWhoIam();
        if (personMe == null)
            personMe = new Person(true, "", "");
        startTab = personMe.getStartTab();

        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        //Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        setupViewPager(mViewPager);

        tabLayout = findViewById(R.id.tabBarLayout);
        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.ic_dateview);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_calendarview);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_settings);


        askForSMSPermissions();


        if (personMe == null || personMe.getName().isEmpty()) {
            openDialogAskForUsername();
        }


        myStartTime.set(Calendar.SECOND, 0);
        myStartTime.set(Calendar.MILLISECOND, 0);
        myEndTime.set(Calendar.SECOND, 0);
        myEndTime.set(Calendar.MILLISECOND, 0);
        myEndDate.set(Calendar.SECOND, 0);
        myEndDate.set(Calendar.MILLISECOND, 0);
    }

    /**
     * this shouldn't exist
     * actually just calls {@link #checkSMSPermissions()}
     */
    private void askForSMSPermissions() {
        checkSMSPermissions();
    }

    /**
     * checks if the app has permission to read SMS
     * if not ask for them, else check if the app has permission to read contacts
     */
    private void checkSMSPermissions() {
        if (!areSMSPermissionsGranted()) {
            requestSMSPermissions();
        } else {
            counterSMS = 5;
            checkContactsPermission();
        }
    }

    /**
     * checks if the app has permission to read contacts
     * if not requests the permission
     */
    private void checkContactsPermission() {
        if (!areContactPermissionsGranted()) {
            requestContactPermissions();
        } else {
            counterCONTACTS = 5;
        }
    }


    /**
     * checks if the app has permission to receive SMS
     * @return true if the app has the permission, false if not
     */
    private boolean areSMSPermissionsGranted() {
        int sms = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (sms != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        }
        return listPermissionsNeeded.isEmpty();
    }

    /**
     * checks if the app has permission to read contacts
     * @return true if the app has the permission, false if not
     */
    private boolean areContactPermissionsGranted() {
        int contacts = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (contacts != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        return listPermissionsNeeded.isEmpty();
    }

    /**
     * if the necessary SMS permission isn't granted, request the permission from the user
     */
    private void requestSMSPermissions() {
        int sms = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (sms != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        }

        if (!listPermissionsNeeded.isEmpty()) {

            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, PERMISSION_REQUEST_RECEIVE_SMS);
        }
    }

    /**
     * if the necessary contact permission isn't granted, request the permission from the user
     */
    private void requestContactPermissions() {
        int contacts = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (contacts != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
        }
    }

    /**
     * changes the currently active tab fragment to the last active one as defined by fragmentResource
     * or restarts the app if none is defined
     * @param fragmentResource defines the last active tab
     */
    private void restartApp(String fragmentResource) {
        //check from which Fragment (EventOverview or Calendar) are the Scanner was called
        switch (fragmentResource) {
            case "EventOverview":
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
                fr.replace(R.id.eventOverview_frameLayout, new EventOverviewFragment());
                fr.commit();
                Objects.requireNonNull(tabLayout.getTabAt(0)).select();
                break;
            case "Calendar":
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                FragmentTransaction frCA = getSupportFragmentManager().beginTransaction();
                frCA.replace(R.id.calendar_frameLayout, new CalendarFragment());
                frCA.commit();
                Objects.requireNonNull(tabLayout.getTabAt(1)).select();
                break;
            default:
                Toast.makeText(this, R.string.ups_an_error, Toast.LENGTH_SHORT).show();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
        }

    }


    /**
     * This method creates a Dialog displaying the currently selected {@link Event} after scanning is successful.
     * After that, the user has three choices to click on.
     * To save and accept event, save without accepting or rejecting event. A Listener checks which button has been
     * clicked. After click, checkIfEventIsInPast() is called.
     *
     * @param whichFragmentTag tag of the tab fragment that was last opened
     */
    @SuppressLint({"ResourceType", "SetTextI18n"})
    private void openActionDialogAfterScanning(final String whichFragmentTag) {
        //Create the Dialog with the GUI Elements initial
        final Dialog afterScanningDialogAction = new Dialog(this);
        afterScanningDialogAction.setContentView(R.layout.dialog_afterscanning);
        afterScanningDialogAction.setCancelable(true);
        afterScanningDialogAction.show();

        TextView qrScanner_result_description = afterScanningDialogAction.findViewById(R.id.dialog_qrScanner_textView_description);
        TextView qrScanner_result_headline = afterScanningDialogAction.findViewById(R.id.dialog_qrScanner_textView_headline);
        Button qrScanner_reject = afterScanningDialogAction.findViewById(R.id.dialog_qrScanner_button_eventRecject);
        Button qrScanner_result_eventSave = afterScanningDialogAction.findViewById(R.id.dialog_qrScanner_button_eventSave);
        final Button qrScanner_result_abort = afterScanningDialogAction.findViewById(R.id.dialog_qrScanner_button_about);
        Button qrScanner_result_toCalender = afterScanningDialogAction.findViewById(R.id.dialog_qrScanner_button_toCalender);
        Button qrScanner_result_eventSave_without_assign = afterScanningDialogAction.findViewById(
                (R.id.dialog_qrScanner_button_eventSaveOnly));

        //Set the Cancel and BackToCalenderButtons to Invisible
        qrScanner_result_abort.setVisibility(View.GONE);
        qrScanner_result_toCalender.setVisibility(View.GONE);

        afterScanningDialogAction.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    restartApp(whichFragmentTag);
                    dialog.cancel();
                    return true;
                }
                return false;
            }
        });

        try {
            // Button to Save the Event and send for assent the Event a SMS  to the EventCreator
            afterScanningDialogAction.findViewById(R.id.dialog_qrScanner_button_eventSave)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            buttonId = 1;
                            if (!checkIfEventIsInPast()) {
                                decideWhatToDo(afterScanningDialogAction);
                            } else {
                                //Restart the TabActivity an Reload all Views
                                Intent intent = getIntent();
                                finish();
                                startActivity(intent);
                            }
                        }
                    });

            //Button to Save the Event but don't send for assent the Event a SMS to the EventCreator
            afterScanningDialogAction.findViewById(R.id.dialog_qrScanner_button_eventSaveOnly)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            buttonId = 2;
                            if (!checkIfEventIsInPast()) {
                                decideWhatToDo(afterScanningDialogAction);
                            } else {
                                //Restart the TabActivity an Reload all Views
                                Intent intent = getIntent();
                                finish();
                                startActivity(intent);
                            }
                        }
                    });

            //Button to Reject the Event und send a Reject SMS to the EventCreator
            afterScanningDialogAction.findViewById(R.id.dialog_qrScanner_button_eventRecject)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            buttonId = 3;
                            if (!checkIfEventIsInPast()) {
                                decideWhatToDo(afterScanningDialogAction);
                            } else {
                                //Restart the TabActivity an Reload all Views
                                Intent intent = getIntent();
                                finish();
                                startActivity(intent);
                            }
                        }
                    });

            DateFormat hourFormat = new SimpleDateFormat("HH:mm");
            DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            eventCreatorEventId = String.valueOf(invitedEvent.getCreatorEventId());
            eventStartDate = dateFormat.format(invitedEvent.getStartTime());
            eventEndDate = dateFormat.format(invitedEvent.getEndRepetitionDate());
            eventStartTime = hourFormat.format(invitedEvent.getStartTime());
            eventEndTime = hourFormat.format(invitedEvent.getEndTime());
            eventShortTitle = invitedEvent.getShortTitle();
            eventPlace = invitedEvent.getPlace();
            eventDescription = invitedEvent.getDescription();
            eventCreatorName = invitedEvent.getCreator().getName();
            eventCreatorPhoneNumber = invitedEvent.getCreator().getPhoneNumber();

            // Event eventShortTitle in Headline with eventCreatorName
            qrScanner_result_headline.setText(eventShortTitle + " " + getString(R.string.from) + eventCreatorName);
            qrScanner_result_description.setText(EventController.createEventDescription(invitedEvent));
            // In the CatchBlock the User see some Error Message and Restart after Clock on Button the TabActivity
        } catch (NullPointerException e) {
            restartApp(whichFragmentTag);
            afterScanningDialogAction.dismiss();
        }
    }

    // "Catch" the ScanningResult and throw the Content to the processing Method

    /**
     * creates an {@link InvitationString} from the scanned or received invitation
     * then if the invitation is for an event in the future creates an {@link Event} and opens a dialog
     * that allows the user to decide what to do with it
     * @param whichFragment which tab fragment was last opened
     * @param codeContent the content of the message or code that needs to be processed
     */
    @Override
    public void scanResultData(String whichFragment, String codeContent) {
        InvitationString invitedString = new InvitationString(codeContent, new Date());
        String eventDateTimeString = invitedString.getStartTime() + " " + invitedString.getStartDate();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm DD.MM.YYYY");
        try {
            Date eventDateTime = format.parse(eventDateTimeString);
            if (eventDateTime.after(invitedString.getDateReceived())) {
                invitedEvent = EventController.createInvitedEventFromInvitation(invitedString);
                openActionDialogAfterScanning(whichFragment);
            } else {
                Toast.makeText(getContext(), R.string.event_is_in_past, Toast.LENGTH_SHORT).show();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    // Give some error Message if the Code have not Data inside

    /**
     * displays an error message if an error occurs while scanning a QR Code
     * @param noScanData a controller for an exception that occurred
     */
    @Override
    public void scanResultData(NoScanResultExceptionController noScanData) {
        Toast.makeText(this, noScanData.getMessage(), Toast.LENGTH_SHORT).show();
    }


    //Method will be called after UI-Elements are created

    /**
     * initializes the TabLayout's behaviour on swipes(hiding the keyboard if it is open etc.)
     */
    public void onStart() {
        super.onStart();
        //Select calendar by default
        Objects.requireNonNull(tabLayout.getTabAt(startTab)).select();
        //Listener that will check when a Tab is selected, unselected and reselected
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            //Do something if Tab is selected. Parameters: selected Tab.--- Info: tab.getPosition() == x for check which Tab
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                /*if (tab.getPosition() == 1) {
                    CalendarFragment.update(CalendarFragment.selectedMonth);
                    EventOverviewFragment.update();
                }*/
            }

            //Do something if Tab is unselected. Parameters: selected Tab.--- Info: tab.getPosition() == x for check which Tab
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //Close the keyboard on a tab change
                //close keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.hideSoftInputFromWindow(Objects.requireNonNull(mSectionsPageAdapter.getItem(tab.getPosition())
                        .getView()).getApplicationWindowToken(), 0);
                //check if settings Tab is unselected
                if (tab.getPosition() == 2) {
                    getSupportFragmentManager().popBackStack();
                } else if (tab.getPosition() == 1) {
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
                    fr.replace(R.id.calendar_frameLayout, new CalendarFragment());
                    fr.commit();
                } else if (tab.getPosition() == 0) {
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
                    fr.replace(R.id.eventOverview_frameLayout, new EventOverviewFragment());
                    fr.commit();
                }
            }

            //Do something if Tab is reselected. Parameters: selected Tab.--- Info: tab.getPosition() == x for check which Tab
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //check if settings Tab is unselected
                //close keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.hideSoftInputFromWindow(Objects.requireNonNull(mSectionsPageAdapter.getItem(tab.getPosition())
                        .getView()).getApplicationWindowToken(), 0);
                if (tab.getPosition() == 2) {
                    getSupportFragmentManager().popBackStack();
                } else if (tab.getPosition() == 1) {
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
                    fr.replace(R.id.calendar_frameLayout, new CalendarFragment());
                    fr.commit();
                } else if (tab.getPosition() == 0) {
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
                    fr.replace(R.id.eventOverview_frameLayout, new EventOverviewFragment());
                    fr.commit();
                }
            }
        });
        try {
            if (getIntent() != null && getIntent().getStringExtra("id") != null) {
                invitedEvent = EventController.getEventById(Long.valueOf(getIntent().getStringExtra("id")));
                if (EventPersonController.getEventPerson(invitedEvent, PersonController.getPersonWhoIam()).getStatus() == AcceptedState.INVITED) {
                    if (invitedEvent != null) {
                        openActionDialogAfterScanning("EventOverview");
                    }
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    // Add the Fragments to the PageViewer

    /**
     * add the tab fragments to the ViewPager
     * @param viewPager the ViewPager that manages the activity's tabs
     */
    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = mSectionsPageAdapter;
        adapter.addFragment(new EventOverviewActivity(), "");
        adapter.addFragment(new CalendarActivity(), "");
        adapter.addFragment(new SettingsActivity(), "");
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * this method opens a Dialog to ask for username if that has not already happened.
     */
    private void openDialogAskForUsername() {
        final AlertDialog.Builder dialogAskForUsername = new AlertDialog.Builder(this);
        dialogAskForUsername.setView(R.layout.dialog_askforusername);
        dialogAskForUsername.setTitle(R.string.titleDialogUsername);
        dialogAskForUsername.setCancelable(true);

        final AlertDialog alertDialogAskForUsername = dialogAskForUsername.create();
        alertDialogAskForUsername.show();

        final EditText username = alertDialogAskForUsername.findViewById(R.id.dialog_EditText_Username);

        Objects.requireNonNull(username).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String dialog_inputUsername;
                dialog_inputUsername = v.getText().toString();

                //RegEx: no whitespace at the beginning
                Pattern pattern_username = Pattern.compile("(\\w|\\.)(\\w|\\s|\\.){0,49}");
                Matcher matcher_username = pattern_username.matcher(dialog_inputUsername);

                if (actionId == EditorInfo.IME_ACTION_DONE && matcher_username.matches() && dialog_inputUsername.length() <= 50) {
                    personMe.setName(dialog_inputUsername);
                    PersonController.savePerson(personMe);
                    Toast toast = Toast.makeText(v.getContext(), R.string.thanksForUsername, Toast.LENGTH_SHORT);
                    toast.show();
                    alertDialogAskForUsername.dismiss();
                    return false;
                } else if (dialog_inputUsername.isEmpty()) {
                    Toast.makeText(getContext(), R.string.username_empty, Toast.LENGTH_SHORT).show();
                } else if (dialog_inputUsername.length() > 50) {
                    Toast.makeText(getContext(), R.string.username_too_long, Toast.LENGTH_SHORT).show();
                } else if (dialog_inputUsername.startsWith(" ")) {
                    Toast.makeText(getContext(), R.string.username_spaces, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(v.getContext(), R.string.noValidUsername, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    /**
     * defines the app's behaviour when the back button is pressed
     * on back press the previous fragment is opened or if there are no previous fragments the app is exited
     */
    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * This method is called if event is not a serial Event
     * This method converts a String (eventStartDate and eventStartTime: from scan result) to a Date
     *
     * @return Date
     */
    private Calendar getStartTimeEvent() {
        //eventStartDate from qr scanner
        String[] startDateStringBufferArray = eventStartDate.split("\\.");
        day = startDateStringBufferArray[0].trim();
        month = startDateStringBufferArray[1].trim();
        year = startDateStringBufferArray[2].trim();

        //eventStartTime from qr scanner
        String[] startTimeStringBufferArray = eventStartTime.split(":");
        hourOfDay = startTimeStringBufferArray[0].trim();
        minutesOfDay = startTimeStringBufferArray[1].trim();

        //set eventStartDate and eventStartTime in one variable
        myStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourOfDay));
        myStartTime.set(Calendar.MINUTE, Integer.parseInt(minutesOfDay));
        myStartTime.set(Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day));
        return myStartTime;
    }

    /**
     * This method is called if event is not a serial Event
     * This method converts a String (eventStartDate and eventEndTime: from scan result) to a Date
     *
     * @return Date
     */
    private Calendar getEndTimeEvent() {
        String[] startDateStringBufferArray = eventStartDate.split("\\.");
        day = startDateStringBufferArray[0].trim();
        month = startDateStringBufferArray[1].trim();
        year = startDateStringBufferArray[2].trim();

        String[] endTimeStringBufferArray = eventEndTime.split(":");
        hourOfDay = endTimeStringBufferArray[0].trim();
        minutesOfDay = endTimeStringBufferArray[1].trim();

        myEndTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourOfDay));
        myEndTime.set(Calendar.MINUTE, Integer.parseInt(minutesOfDay));
        myEndTime.set(Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day));
        return myEndTime;
    }

    /**
     * This method is called if event is a serial event
     * This method converts a String (eventEndDate and eventEndTime: from scan result) to a Date
     *
     * @return Date
     */
    private Calendar getEndDateEvent() {
        String[] endDateStringBufferArray = eventEndDate.split("\\.");
        day = endDateStringBufferArray[0].trim();
        month = endDateStringBufferArray[1].trim();
        year = endDateStringBufferArray[2].trim();

        String[] endTimeStringBufferArray = eventEndTime.split(":");
        hourOfDay = endTimeStringBufferArray[0].trim();
        minutesOfDay = endTimeStringBufferArray[1].trim();

        myEndDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourOfDay));
        myEndDate.set(Calendar.MINUTE, Integer.parseInt(minutesOfDay));
        myEndDate.set(Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day));
        return myEndDate;
    }

    /**
     * This method decides if event can be saved, rejected or accepted
     */
    private void dialogListener() {
        final AlertDialog.Builder dialogAskForFinalDecission = new AlertDialog.Builder(this);
        dialogAskForFinalDecission.setView(R.layout.dialog_afterscanningbuttonclick);
        dialogAskForFinalDecission.setTitle(R.string.titleDialogFinalDecission);
        dialogAskForFinalDecission.setCancelable(true);

        final AlertDialog alertDialogAskForFinalDecission = dialogAskForFinalDecission.create();
        //open Dialog with yes or no after button click (accept, save, reject)
        alertDialogAskForFinalDecission.show();
        Objects.requireNonNull(alertDialogAskForFinalDecission.findViewById(R.id.dialog_event_final_decission_accept))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Calendar variables for checking eventStartTime and eventEndTime
                        Calendar checkStartTime = getStartTimeEvent();
                        Calendar checkEndTime = getEndTimeEvent();

                        //check if Event is n Database or not
                        singleEvent = EventController.getEventViaPhoneAndCreatorEventId(eventCreatorPhoneNumber, eventCreatorEventId);

                        //if event is in database
                        if (singleEvent != null && EventPersonController.getEventPerson(singleEvent, PersonController.getPersonWhoIam()) != null &&
                                EventPersonController.getEventPerson(singleEvent, PersonController.getPersonWhoIam()).getStatus() != AcceptedState.REJECTED &&
                                EventPersonController.getEventPerson(singleEvent, PersonController.getPersonWhoIam()).getStatus() != AcceptedState.INVITED) {
                            //finish and restart the activity
                            /*Intent intent = getIntent();
                            finish();
                            startActivity(intent);*/
                            alertDialogAskForFinalDecission.dismiss();
                            //write Toast, event is in database
                            EventController.deleteEvent(invitedEvent);
                            Toast toast = Toast.makeText(v.getContext(), R.string.eventIsInDatabase, Toast.LENGTH_LONG);
                            toast.show();

                        }
                        else {
                            alertDialogAskForFinalDecission.dismiss();
                            updateEventStatus();
                        }
                    }
                });
        //if Button "nein": cancel dialog
        Objects.requireNonNull(alertDialogAskForFinalDecission.findViewById(R.id.dialog_event_final_decission_reject))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialogAskForFinalDecission.cancel();
                    }
                });
    }

    /**
     * This method saves and accepts or just saves the event
     */
    private void updateEventStatus() {
        //check if event is serialevent
        if (invitedEvent.getRepetition() != Repetition.NONE) {
            if (buttonId == 1) {
                EventPersonController.changeStatusForSerial(invitedEvent, PersonController.getPersonWhoIam(), AcceptedState.ACCEPTED, null);
                List<Event> events = EventController.findFollowUpEvents(invitedEvent.getId());
                for (Event event : events) {
                    NotificationController.setAlarmForNotification(getApplicationContext(), event);
                }
                sendSMS(invitedEvent);
                Toast.makeText(getContext(), R.string.save_event, Toast.LENGTH_SHORT).show();
            } else if (buttonId == 2) {
                EventPersonController.changeStatusForSerial(invitedEvent, PersonController.getPersonWhoIam(), AcceptedState.WAITING, null);
                Toast.makeText(getContext(), R.string.save_event, Toast.LENGTH_SHORT).show();
            }

        } else {
            //save the one event
            if (buttonId == 1) {
                EventPersonController.changeStatus(invitedEvent, PersonController.getPersonWhoIam(), AcceptedState.ACCEPTED, null);
                sendSMS(invitedEvent);
                NotificationController.setAlarmForNotification(getApplicationContext(), invitedEvent);
                Toast.makeText(getContext(), R.string.save_event, Toast.LENGTH_SHORT).show();
            } else if (buttonId == 2) {
                EventPersonController.changeStatus(invitedEvent, PersonController.getPersonWhoIam(), AcceptedState.WAITING, null);
                Toast.makeText(getContext(), R.string.save_event, Toast.LENGTH_SHORT).show();
            }
        }
        if (buttonId != 3) {
            //Restart the TabActivity an Reload all Views
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    /**
     * This method is called only if after scanning and the button for rejection has been clicked.
     * It rejects the event. For that it checks whether event is in database or not. If it is not, it saves
     * the event with AcceptedState.REJECTED. This method calls "EventRejectEventFragment" for rejection
     *
     * @param afterScanningDialogAction to close this Dialog
     */
    private void rejectEventInvitation(Dialog afterScanningDialogAction) {

        //check if Event is n Database or not
        afterScanningDialogAction.cancel();
        EventRejectEventFragment eventRejectEventFragment = new EventRejectEventFragment();
        Bundle bundleAcceptedEventId = new Bundle();

        //finish and restart the activity
        bundleAcceptedEventId.putLong("EventId", invitedEvent.getId());

        bundleAcceptedEventId.putString("fragment", "AcceptedEventDetails");
        eventRejectEventFragment.setArguments(bundleAcceptedEventId);
        FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
        fr.replace(R.id.calendar_frameLayout, eventRejectEventFragment, "RejectEvent");
        fr.addToBackStack("RejectEvent");
        fr.commit();
    }

    private void sendSMS(Event event) {
        //SMS
        String reject_message = "";
        new SendSmsController().sendSMS(getApplicationContext(), event.getCreator().getPhoneNumber(), reject_message, true, event.getCreatorEventId(), event.getShortTitle());
    }

    /**
     * This method checks if organizer is in database or not. If not, save new person. If it is in
     * database then set Creator with his id.
     *
     * @param event  to know which event has to be checked
     * @param person new Person to save
     */
    private void checkIfPersonIsInDatabase(Event event, Person person) {
        //if publisher is in database
        if (personEventCreator != null) {
            event.setCreator(personEventCreator);
        } else {
            //if publisher is not in database: save a new person
            person.setName(eventCreatorName);
            person.setPhoneNumber(eventCreatorPhoneNumber);
            person.save();
            event.setCreator(person);
        }

    }

    /**
     * This method checks if event is in past.
     *
     * @return false if event is in future
     */
    private boolean checkIfEventIsInPast() {
        //read the current date and time to compare if the End of the Event is in the past (Date & Time),
        // set seconds and milliseconds to 0 to ensure a ight compare (seonds and milliseconds doesn't matter)
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        boolean test = getStartTimeEvent().before(now);
        Log.i("STARTZEIT", getStartTimeEvent().getTime().toString());
        Log.i("EVENTZEIT", now.getTime().toString());
        if (invitedEvent.getRepetition() == Repetition.NONE) {
            if (getStartTimeEvent().getTime().before(now.getTime())) {
                Toast.makeText(this, R.string.startTime_afterScanning_past, Toast.LENGTH_SHORT).show();
                return true;
            } else {
                return false;
            }

        } else {
            if (getEndDateEvent().getTime().before(now.getTime())) {
                Toast.makeText(this, R.string.startTime_afterScanning_past, Toast.LENGTH_SHORT).show();
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * This method decides what to do after click on save, accept or reject event.
     *
     * @param afterScanningDialogActionn
     */
    private void decideWhatToDo(Dialog afterScanningDialogActionn) {
        if (!checkIfEventIsInPast()) {
            Person person = PersonController.getPersonWhoIam();
            if (person == null) {
                openDialogAskForUsername();
            } else if (buttonId == 1 || buttonId == 2) {
                dialogListener();
                afterScanningDialogActionn.dismiss();
            } else if (buttonId == 3) {
                rejectEventInvitation(afterScanningDialogActionn);
            }
        } else {
            //Restart the TabActivity an Reload all Views
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    /**
     * Check if User has permission to read the user's phone number,
     * if not request the permission, else read the number
     */
    private void checkPhonePermission() {
        if (!isPhonePermissionGranted()) {
            requestPhonePermission();
        } else {
            readPhoneNumber();
        }
    }

    /**
     * checks if the app has permission to read the user's phone number
     * @return true if it has the permission, false if not
     */
    private boolean isPhonePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * requests permission to read the user's phone number
     */
    private void requestPhonePermission() {
        //For Fragment: requestPermissions(permissionsList,REQUEST_CODE);
        //For Activity: ActivityCompat.requestPermissions(this,permissionsList,REQUEST_CODE);
        requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_READ_PHONE_STATE);
    }

    /**
     * processes the user's response to a request for a permission
     * upon denial asks again up to 2 times unless the user requests to never be asked again
     * @param requestCode the code of the request
     * @param permissions the list of permission that were requested
     * @param grantResults the list of results of the requests
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_PHONE_STATE) {
            // for each permission check if the user granted/denied them you may want to group the
            // rationale in a single dialog,this is just an example
            for (int i = 0, len = permissions.length; i < len; i++) {

                if (grantResults.length > 0
                        && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // user rejected the permission
                    boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE);
                    if (!showRationale) {
                        // user also CHECKED "never ask again" you can either enable some fall back,
                        // disable features of your app or open another dialog explaining again the
                        // permission and directing to the app setting

                        new android.support.v7.app.AlertDialog.Builder(this)
                                .setTitle(R.string.accessWith_NeverAskAgain_deny)
                                .setMessage(R.string.sendSMS_accessDenied_withCheckbox)
                                .setPositiveButton(R.string.sendSMS_manual, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        openDialogAskForUsername();
                                    }
                                })
                                .create().show();
                    } else if (counter < 1) {
                        // user did NOT check "never ask again" this is a good eventPlace to explain the user
                        // why you need the permission and ask if he wants // to accept it (the rationale)
                        new android.support.v7.app.AlertDialog.Builder(this)
                                .setTitle(R.string.requestPermission_firstTryRequest)
                                .setMessage(R.string.phoneNumber_explanation)
                                .setPositiveButton(R.string.oneMoreTime, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        counter++;
                                        checkPhonePermission();
                                    }
                                })
                                .setNegativeButton(R.string.sendSMS_manual, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //open keyboard
                                        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                                        openDialogAskForUsername();
                                    }
                                })
                                .create().show();
                    } else if (counter == 1) {
                        new android.support.v7.app.AlertDialog.Builder(this)
                                .setTitle(R.string.sendSMS_lastTry)
                                .setMessage(R.string.phoneNumber_explanation)
                                .setPositiveButton(R.string.oneMoreTime, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        counter++;
                                        checkPhonePermission();
                                    }
                                })
                                .setNegativeButton(R.string.sendSMS_manual, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                                        openDialogAskForUsername();
                                    }
                                })
                                .create().show();
                    } else {
                        openDialogAskForUsername();
                        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }
                } else {
                    readPhoneNumber();
                }
            }

        } else if (requestCode == PERMISSION_REQUEST_RECEIVE_SMS) {
            if (counterSMS != 5) {
                // for each permission check if the user granted/denied them you may want to group the
                // rationale in a single dialog,this is just an example
                for (int i = 0; i < 1; i++) {

                    if (grantResults.length > 0
                            && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        // user rejected the permission
                        boolean showRationale;
                        if (counterSMS == 5) {
                            showRationale = true;
                        } else {
                            showRationale = shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS);
                        }
                        if (!showRationale) {
                            // user also CHECKED "never ask again" you can either enable some fall back,
                            // disable features of your app or open another dialog explaining again the
                            // permission and directing to the app setting
                            new AlertDialog.Builder(this)
                                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            return keyCode == KeyEvent.KEYCODE_BACK;
                                        }
                                    })
                                    .setTitle(R.string.accessWith_NeverAskAgain_deny)
                                    .setMessage(R.string.requestSMSPermission_accessDenied_withCheckbox)
                                    .setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .create().show();
                        } else if (counterSMS < 1) {
                            // user did NOT check "never ask again" this is a good eventPlace to explain the user
                            // why you need the permission and ask if he wants // to accept it (the rationale)
                            new AlertDialog.Builder(this)
                                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                dialog.cancel();
                                                return true;
                                            }
                                            return false;
                                        }
                                    })
                                    .setTitle(R.string.requestPermission_firstTryRequest)
                                    .setMessage(R.string.requestPermission_askForSMSPermission)
                                    .setPositiveButton(R.string.requestPermission_againButton, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            counterSMS++;
                                            checkSMSPermissions();
                                        }

                                    })
                                    .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            checkContactsPermission();
                                            counterSMS = 0;
                                        }
                                    })
                                    .create().show();
                        } else if (counterSMS == 1) {
                            new AlertDialog.Builder(this)
                                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            return keyCode == KeyEvent.KEYCODE_BACK;
                                        }
                                    })
                                    .setTitle(R.string.requestPermission_lastTryRequest)
                                    .setMessage(R.string.requestPermission_askForSMSPermission)
                                    .setPositiveButton(R.string.requestPermission_againButton, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            counterSMS++;
                                            checkSMSPermissions();
                                        }
                                    })
                                    .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            checkContactsPermission();
                                            counterSMS = 0;
                                        }
                                    })
                                    .create().show();
                        }
                    } else {
                        counterSMS = 5;
                        checkSMSPermissions();
                    }
                }

            }
        } else if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (counterCONTACTS != 5) {
                // for each permission check if the user granted/denied them you may want to group the
                // rationale in a single dialog,this is just an example
                for (int i = 0; i < 1; i++) {

                    if (grantResults.length > 0
                            && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        // user rejected the permission
                        boolean showRationale;
                        if (counterCONTACTS == 5) {
                            showRationale = true;
                        } else {
                            showRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS);
                        }
                        if (!showRationale) {
                            // user also CHECKED "never ask again" you can either enable some fall back,
                            // disable features of your app or open another dialog explaining again the
                            // permission and directing to the app setting
                            new AlertDialog.Builder(this)
                                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            return keyCode == KeyEvent.KEYCODE_BACK;
                                        }
                                    })
                                    .setTitle(R.string.accessWith_NeverAskAgain_deny)
                                    .setMessage(R.string.requestContactPermission_accessDenied_withCheckbox)
                                    .setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    })
                                    .create().show();
                        } else if (counterCONTACTS < 1) {
                            // user did NOT check "never ask again" this is a good eventPlace to explain the user
                            // why you need the permission and ask if he wants // to accept it (the rationale)
                            new AlertDialog.Builder(this)
                                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            if (keyCode == KeyEvent.KEYCODE_BACK) {

                                                dialog.cancel();
                                                return true;
                                            }
                                            return false;
                                        }
                                    })
                                    .setTitle(R.string.requestPermission_firstTryRequest)
                                    .setMessage(R.string.requestPermission_askForContactsPermission)
                                    .setPositiveButton(R.string.requestPermission_againButton, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            counterCONTACTS++;
                                            checkContactsPermission();
                                        }
                                    })
                                    .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            counterCONTACTS = 0;
                                        }
                                    })
                                    .create().show();
                        } else if (counterCONTACTS == 1) {
                            new AlertDialog.Builder(this)
                                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            return keyCode == KeyEvent.KEYCODE_BACK;
                                        }
                                    })
                                    .setTitle(R.string.requestPermission_lastTryRequest)
                                    .setMessage(R.string.requestPermission_askForContactsPermission)
                                    .setPositiveButton(R.string.requestPermission_againButton, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            counterCONTACTS++;
                                            checkContactsPermission();
                                        }
                                    })
                                    .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            counterCONTACTS = 0;
                                        }
                                    })
                                    .create().show();
                        }
                    } else {
                        counterCONTACTS = 5;
                    }
                }
            }
        }
    }

    // method to read the phone number of the user

    /**
     * reads the phone number of the SIM card of the user and adds it to their profile
     * if the number can't be read the user is asked to enter it manually
     */
    private void readPhoneNumber() {
        //if permission is granted read the phone number
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        @SuppressLint("MissingPermission") String phoneNumber = telephonyManager.getLine1Number();
        //delete spaces and add a plus before the number if it begins without a 0
        if (phoneNumber != null)
            phoneNumber = phoneNumber.replaceAll(" ", "");
        if (phoneNumber.matches("[1-9][0-9]+"))
            phoneNumber = "+" + phoneNumber;
        personMe.setPhoneNumber(phoneNumber);
        if (personMe.getPhoneNumber() == null || !personMe.getPhoneNumber().matches("^\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$")) {
            Toast.makeText(this, R.string.telephonenumerNotRead, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), R.string.thanksphoneNumber, Toast.LENGTH_SHORT).show();
            if (this.getCurrentFocus() != null) {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);

            }
            if (personMe.getName().isEmpty())
                openDialogAskForUsername();
            else {
                PersonController.savePerson(personMe);
                dialogListener();
            }
        }
    }
}
