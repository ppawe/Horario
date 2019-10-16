package hft.wiinf.de.horario.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.EventPersonController;
import hft.wiinf.de.horario.controller.NotificationController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.model.AcceptedState;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.Repetition;

/**
 * a fragment used for creating new events
 * has editText views for each field of {@link Event} for the user to enter that are validated at submission
 * creating a new event requires the user's permission to read their phone number because it is used as
 * an identifier and a means for people to reply with an acceptance or rejection to a potential invitation
 */
public class NewEventFragment extends Fragment {

    // calendar objects to save the startTime / end Time / endOfRepetition, default: values - today
    private Calendar startTime = Calendar.getInstance();
    private Calendar endTime = Calendar.getInstance();
    private Calendar endOfRepetition = Calendar.getInstance();
    // elements of the gui
    private EditText editText_description, edittext_shortTitle, edittext_room, edittext_date,
            edittext_startTime, editText_endTime, edittext_userName, editText_endOfRepetition;
    private TextView textView_endofRepetiton;
    private Spinner spinner_repetition;
    private CheckBox checkBox_serialEvent;
    private Button button_save;
    //person object of the user, to get the user name
    private Person me;
    private DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
    private DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.GERMAN);
    private int counter = 0;
    private int PERMISSION_REQUEST_READ_PHONE_STATE = 0;
    private AlertDialog.Builder mAlertDialogBuilder;
    private EditText mPhoneNumber;
    private AlertDialog mAlertDialog;
    private Dialog mDialog;
    private DatePickerDialog mDatePickerDialog;
    private TimePickerDialog mTimePickerDialog;

    /**
     * inflates the fragment_new_event.xml layout and returns its view
     *
     * @param inflater           a LayoutInflater used for inflating layouts into views
     * @param container          the parent view of the fragment
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     * @return the view created from inflating the layout
     */
    @Nullable
    @Override
    //create the view
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_event, container, false);
    }


    /**
     * initializes all the view variables and sets onCLickListener for the editText views
     * as well as some user navigation behaviour
     *
     * @param view               the view created from the layout in onCreateView()
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     */
    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        // set the second and millisecond of the calendar objects to 0 as (dates and) times are only compared by hour and minute, seconds dont matter
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);
        endTime.set(Calendar.SECOND, 0);
        endTime.set(Calendar.MILLISECOND, 0);
        endOfRepetition.set(Calendar.SECOND, 0);
        endOfRepetition.set(Calendar.MILLISECOND, 0);
        // get / initialize  the needed gui objects as fields of the class
        edittext_shortTitle = view.findViewById(R.id.newEvent_textEdit_shortTitle);
        editText_description = view.findViewById(R.id.newEvent_editText_description);
        edittext_room = view.findViewById(R.id.newEvent_textEdit_room);
        edittext_date = view.findViewById(R.id.newEvent_editText_Date);
        edittext_startTime = view.findViewById(R.id.newEvent_editText_startTime);
        editText_endTime = view.findViewById(R.id.newEvent_textEdit_endTime);
        edittext_userName = view.findViewById(R.id.unewEvent_textEdit_userName);
        checkBox_serialEvent = view.findViewById(R.id.newEvent_checkBox_SerialEvent);
        spinner_repetition = view.findViewById(R.id.newEvent_spinner_repetition);
        editText_endOfRepetition = view.findViewById(R.id.newEvent_textEdit_endOfRepetition);
        textView_endofRepetiton = view.findViewById(R.id.newEvent_textView_endOfRepetiton);

        editText_description.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        editText_description.setRawInputType(InputType.TYPE_CLASS_TEXT);
        button_save = view.findViewById(R.id.newEvent_button_save);

        // when the keyboard is closed after the text edit room, there should be no focus
        edittext_room.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Context ctx = getContext();
                    assert ctx != null;
                    InputMethodManager mngr = (InputMethodManager) ctx.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    assert mngr != null;
                    mngr.hideSoftInputFromWindow(edittext_room.getWindowToken(), 0);
                    edittext_room.clearFocus();
                    return true;
                }
                return false;
            }
        });
        //for each fields with a date: 1. don't open keyboard on focus, when it gets focus or the user
        // clicks on the field: open date/time picker and save the date
        edittext_date.setShowSoftInputOnFocus(false);
        edittext_date.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    getDate();
            }
        });
        edittext_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDate();
            }
        });
        edittext_startTime.setShowSoftInputOnFocus(false);
        edittext_startTime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    getStartTime();
            }
        });
        edittext_startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getStartTime();
            }
        });
        editText_endTime.setShowSoftInputOnFocus(false);
        editText_endTime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    getEndTime();
            }
        });
        editText_endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getEndTime();
            }
        });
        edittext_userName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Context ctx = getContext();
                    assert ctx != null;
                    InputMethodManager mngr = (InputMethodManager) ctx.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    assert mngr != null;
                    mngr.hideSoftInputFromWindow(edittext_userName.getWindowToken(), 0);
                    edittext_userName.clearFocus();
                    return true;
                }
                return false;
            }
        });
        // on click on serial event checkbox change visibility of the repetition and repetition end field,
        checkBox_serialEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkSerialEvent();
            }
        });
        // sets the choice possibilities of the repetition spinner (set in string resource-file as array event-repetition)
        Context ctx = getContext();
        assert ctx != null;
        ArrayAdapter repetitionAdapter = ArrayAdapter.createFromResource(getContext(), R.array.event_repetitions, android.R.layout.simple_spinner_item);
        //set the appearance of one choice possibility
        repetitionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_repetition.setAdapter(repetitionAdapter);
        //set weekly selected until the user selects something different
        spinner_repetition.setSelection(2);
        //don't open keyboard on focus,
        editText_endOfRepetition.setShowSoftInputOnFocus(false);
        // when it gets focus or the user clicks on the field: open date/time picker and save the date
        editText_endOfRepetition.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    getEndOfRepetition();
            }
        });
        editText_endOfRepetition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getEndOfRepetition();
            }
        });
        // when the keyboard is closed after the text edit room, there should be no focus
        editText_endOfRepetition.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Context ctx = getContext();
                    assert ctx != null;
                    InputMethodManager mngr = (InputMethodManager) ctx.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    assert mngr != null;
                    mngr.hideSoftInputFromWindow(editText_endOfRepetition.getWindowToken(), 0);
                    editText_endOfRepetition.clearFocus();
                    return true;
                }
                return false;
            }
        });
        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClickSave();
                new EventOverviewFragment().update();
                CalendarFragment.updateCompactCalendar();
            }
        });
        //get the user, if it is saved in the db, the user name is read
        me = PersonController.getPersonWhoIam();
        if (me == null)
            me = new Person(true, "", "");
        edittext_userName.setText(me.getName());
    }

    //if the checkbox serial event is checked, repetition possibilities and the endOfrepetition is shown, else not

    /**
     * checks if the serial event checkbox is checked and hides or reveals the serial event options accordingly
     */
    private void checkSerialEvent() {
        if (checkBox_serialEvent.isChecked()) {
            textView_endofRepetiton.setVisibility(View.VISIBLE);
            editText_endOfRepetition.setVisibility(View.VISIBLE);
            spinner_repetition.setVisibility(View.VISIBLE);

        } else {
            textView_endofRepetiton.setVisibility(View.GONE);
            editText_endOfRepetition.setVisibility(View.GONE);
            spinner_repetition.setVisibility(View.GONE);

        }
    }

    /**
     * opens a dialog that allows the user to pick a valid date and sets it to the date field
     */
    private void getDate() {
        //close keyboard if it's open
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            Context ctx = getContext();
            assert ctx != null;
            InputMethodManager mngr = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            assert mngr != null;
            mngr.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
        // create a listener for the date picker dialog: update the date parts (year, month, date) of start and end time with the selected values
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                startTime.set(year, month, dayOfMonth);
                endTime.set(year, month, dayOfMonth);
                //format the choosen time as HH:mm and write it into the date text field
                edittext_date.setText(dateFormat.format(startTime.getTime()));
            }
        };
        mDatePickerDialog = new DatePickerDialog(this.getContext(), listener, startTime.get(Calendar.YEAR),
                startTime.get(Calendar.MONTH), startTime.get(Calendar.DAY_OF_MONTH));
        mDatePickerDialog.show();
    }

    /**
     * opens a dialog that allows the user to pick a valid time and sets it to the startTime field
     */
    private void getStartTime() {
        //close keyboard if it's open
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            Context ctx = getContext();
            assert ctx != null;
            InputMethodManager mngr = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            assert mngr != null;
            mngr.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
        // create a listener for the time picker dialog: update the start time with the selected values
        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                startTime.set(Calendar.MINUTE, minute);
                //format the choosen time as HH:mm and write it into the start time text field
                edittext_startTime.setText(timeFormat.format(startTime.getTime()));
            }
        };
        //open a time picker to let the user choose a time, use the saved start time as initial
        // value (initial value of startTime: now)
        mTimePickerDialog = new TimePickerDialog(this.getContext(), listener, startTime.get(Calendar.HOUR_OF_DAY),
                startTime.get(Calendar.MINUTE), true);
        mTimePickerDialog.show();
    }

    /**
     * opens a dialog that allows the user to pick a valid time and sets it to the endTime field
     */
    private void getEndTime() {
        //close keyboard if it's open
        Activity activity = getActivity();
        assert activity != null;
        if (activity.getCurrentFocus() != null) {
            Context ctx = getContext();
            assert ctx != null;
            InputMethodManager mngr = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            assert mngr != null;
            mngr.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
        // create a listener for the time picker dialog: update the end time and the time for the end of repetition (for the comparing later) with the selected values
        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                endTime.set(Calendar.MINUTE, minute);
                endOfRepetition.set(Calendar.HOUR_OF_DAY, hourOfDay);
                endOfRepetition.set(Calendar.MINUTE, minute);
                //format the choosen time as HH:mm and write it into the end time text field
                editText_endTime.setText(timeFormat.format(endTime.getTime()));
            }
        };
        //open a time picker to let the user choose a time, use the saved end time as initial value
        // (initial value of endTime: now)
        mTimePickerDialog = new TimePickerDialog(this.getContext(), listener, endTime.get(Calendar.HOUR_OF_DAY),
                endTime.get(Calendar.MINUTE), true);
        mTimePickerDialog.show();
    }

    /**
     * opens a dialog that allows the user to pick a valid date and sets it to the endDate field
     */
    private void getEndOfRepetition() {
        //close keyboard if it's open
        Activity activity = getActivity();
        assert activity != null;
        if (activity.getCurrentFocus() != null) {
            Context ctx = getContext();
            assert ctx != null;
            InputMethodManager mngr = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            assert mngr != null;
            mngr.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
        // create a listener for the time picker dialog: update the date part (year, month, day) of the end of repetition with the selected values
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                endOfRepetition.set(year, month, dayOfMonth);
                editText_endOfRepetition.setText(dateFormat.format(endOfRepetition.getTime()));
            }
        };
        //open a date picker to let the user choose a date, use the saved end of repetition as initial value (initial value of endTime: now)
        Context ctx = getContext();
        assert ctx != null;
        //open a date picker to let the user choose a date, use the saved end of repetition as initial
        // value (initial value of endTime: now)
        mDatePickerDialog = new DatePickerDialog(this.getContext(), listener, endOfRepetition.get(Calendar.YEAR),
                endOfRepetition.get(Calendar.MONTH), endOfRepetition.get(Calendar.DAY_OF_MONTH));
        mDatePickerDialog.show();
    }


    /**
     * checks if the entries are valid and the user has a valid phone number
     * if not try to get the phone number, else save the event
     */
    private void onButtonClickSave() {
        if (checkValidity()) {
            if (me.getPhoneNumber() == null || !me.getPhoneNumber().matches("\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$"))
                checkPhonePermission();
            else
                saveEvent();

        }
    }

    //read the needed parameters / textfield and save the event

    /**
     * builds a new {@link Event} from the user's entries and sets a new alarm notification for it
     */
    private void saveEvent() {
        //save the new user name
        me.setName(edittext_userName.getText().toString());
        PersonController.savePerson(me);
        Event event = new Event(me);
        event.setDescription(editText_description.getText().toString());
        event.setStartTime(startTime.getTime());
        event.setEndTime(endTime.getTime());
        event.setShortTitle(edittext_shortTitle.getText().toString());
        event.setRepetition(getRepetition());
        event.setPlace(edittext_room.getText().toString());
        // only save the end of repetition if the repetition is not none, if it's an serial event
        // (repetition not none) save it as an serial event, else as an "normal" event
        if (event.getRepetition() != Repetition.NONE) {
            event.setEndDate(endOfRepetition.getTime());
            EventController.saveSerialevent(event);
            List<Event> savedEvents = EventController.findFollowUpEvents(event.getId());
            for (Event singleEvent : savedEvents) {
                EventPersonController.addOrGetEventPerson(singleEvent, me, AcceptedState.ACCEPTED);
            }
        } else {
            EventController.saveEvent(event);
        }
        EventPersonController.addOrGetEventPerson(event, me, AcceptedState.ACCEPTED);
        if (!EventController.createdEventsYet()) {
            Long date = System.currentTimeMillis();
            saveReadDate(String.valueOf(date));
        }
        openSavedSuccessfulDialog(event.getId());
        NotificationController.setAlarmForNotification(getContext(), event);
    }

    /**
     * I do not know what this does quite yet
     * saves the date to some file but what for?
     * @param date the date that's being saved
     */
    private void saveReadDate(String date) {
        FileOutputStream outputStream;
        try {
            Context ctx = getContext();
            assert ctx != null;
            outputStream = getContext().openFileOutput("lastReadDate.txt", Context.MODE_PRIVATE);
            outputStream.write(date.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //clear all entries and open a dialog where the user can choose what to do next

    /**
     * clears all entries and opens a dialog with buttons that allows the user to either create a new
     * {@link Event} or show the newly created event's QR Code
     * @param eventId the event id of the newly created event
     */
    private void openSavedSuccessfulDialog(final long eventId) {
        clearEntrys();
        Context ctx = getContext();
        assert ctx != null;
        mDialog = new Dialog(ctx);
        mDialog.setContentView(R.layout.dialog_savingsucessfull);
        mDialog.setCancelable(true);
        mDialog.show();
        //create a new event: only close the dialog
        mDialog.findViewById(R.id.savingSuccessful_button_new).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        mDialog.findViewById(R.id.savingSuccessful_button_qrcode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle whichFragment = getArguments();
                mDialog.dismiss();
                QRGeneratorFragment qrFrag = new QRGeneratorFragment();
                assert whichFragment != null;
                Bundle bundle = new Bundle();
                bundle.putLong("eventId", eventId);
                bundle.putString("fragment", whichFragment.getString("fragment"));
                qrFrag.setArguments(bundle);
                assert whichFragment.getString("fragment") != null;
                assert getActivity() != null;
                if (whichFragment.getString("fragment").equals("EventOverview")) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.eventOverview_frameLayout, qrFrag, "QrGeneratorEO")
                            .addToBackStack("QrGeneratorEO")
                            .commit();
                } else {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.calendar_frameLayout, qrFrag, "QrGeneratorCA")
                            .addToBackStack("QrGeneratorCA")
                            .commit();
                }
            }
        });
    }

    /**
     * clears all editText views and unchecks the serial event checkbox
     */
    private void clearEntrys() {
        edittext_shortTitle.setText("");
        editText_description.setText("");
        edittext_room.setText("");
        edittext_date.setText("");
        edittext_startTime.setText("");
        editText_endTime.setText("");
        checkBox_serialEvent.setChecked(false);
        spinner_repetition.setSelected(false);
        editText_endOfRepetition.setText("");
        checkSerialEvent();
    }

    //checks if the entrys are valid and opens a toast if not return value: boolean if everything is ok

    /**
     * checks if all entries are valid
     * @return true if fields are valid; false if not
     */
    private boolean checkValidity() {
        if (editText_description.getText().toString().equals("") || edittext_shortTitle.getText().toString().equals("") || edittext_date.getText().toString().equals("") || edittext_startTime.getText().toString().equals("") || editText_endTime.getText().toString().equals("") || edittext_userName.getText().toString().equals("") || edittext_room.getText().toString().equals("")) {
            Toast.makeText(getContext(), R.string.empty_fields, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getRepetition() != Repetition.NONE && editText_endOfRepetition.getText().toString().equals("")) {
            Toast.makeText(getContext(), R.string.empty_fields, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (edittext_shortTitle.getText().toString().matches(" +.*")) {
            Toast.makeText(getContext(), R.string.shortTitle_spaces, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (edittext_shortTitle.getText().toString().contains("|")) {
            Toast.makeText(getContext(), R.string.shortTitle_peek, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (editText_description.getText().toString().matches(" +.*")) {
            Toast.makeText(getContext(), R.string.description_spaces, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (editText_description.getText().toString().contains("|")) {
            Toast.makeText(getContext(), R.string.description_peek, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (edittext_room.getText().toString().matches(" +.*")) {
            Toast.makeText(getContext(), R.string.place_spaces, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (edittext_room.getText().toString().contains("|")) {
            Toast.makeText(getContext(), R.string.room_peek, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (editText_description.getText().length() > 200) {
            Toast.makeText(getContext(), R.string.description_too_long, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (edittext_shortTitle.getText().length() > 50) {
            Toast.makeText(getContext(), R.string.shortTitle_too_long, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (edittext_room.getText().length() > 50) {
            Toast.makeText(getContext(), R.string.room_too_long, Toast.LENGTH_SHORT).show();
            return false;
        }
        //read the current date and time to compare if the start time is in the past, set seconds and milliseconds to 0 to ensure a ight compare (seonds and milliseconds doesn't matter)
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        if (startTime.before(now)) {
            Toast.makeText(getContext(), R.string.startTime_past, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (endTime.before(startTime)) {
            Toast.makeText(getContext(), R.string.endTime_before_startTime, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (edittext_userName.length() > 50) {
            Toast.makeText(getContext(), R.string.username_too_long, Toast.LENGTH_SHORT).show();
            return false;

        }
        if (edittext_userName.getText().toString().startsWith(" ")) {
            Toast.makeText(getContext(), R.string.username_spaces, Toast.LENGTH_SHORT).show();
            return false;


        }
        if (!edittext_userName.getText().toString().matches("(\\w|\\.)(\\w|\\s|\\.)*")) {
            Toast.makeText(getContext(), R.string.noValidUsername, Toast.LENGTH_SHORT).show();
            return false;
        }

        //if it is and repeating event and the end of the repetition is before the end time of the first event
        if (getRepetition() != Repetition.NONE && endOfRepetition.before(endTime)) {
            Toast.makeText(getContext(), R.string.endOfRepetition_before_endTime, Toast.LENGTH_SHORT).show();
            return false;
        }
        Calendar minEndDate = (Calendar) startTime.clone();
        minEndDate.set(Calendar.HOUR, 0);
        minEndDate.set(Calendar.MINUTE, 0);
        minEndDate.set(Calendar.SECOND, 0);
        minEndDate.set(Calendar.MILLISECOND, 0);
        switch (getRepetition()) {
            case DAILY:
                minEndDate.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case WEEKLY:
                minEndDate.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case MONTHLY:
                minEndDate.add(Calendar.MONTH, 1);
                break;
            case YEARLY:
                minEndDate.add(Calendar.YEAR, 1);
                break;
            default:
                break;
        }
        if (getRepetition() != Repetition.NONE && endOfRepetition.before(minEndDate)) {
            Toast.makeText(getContext(), "Das Ende der Wiederholung liegt vor Ende der Mindestlänge des Wiederholungsintervalls", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //get the right repetition

    /**
     * converts the numerical value of the spinner to the corresponding {@link Repetition}
     * @return Repetition corresponding to the user's choice
     */
    private Repetition getRepetition() {
        //if the check box isnt checked return none
        if (!checkBox_serialEvent.isChecked()) {
            return Repetition.NONE;
        }
        switch (spinner_repetition.getSelectedItemPosition()) {
            case 0:
                return Repetition.YEARLY;
            case 1:
                return Repetition.MONTHLY;
            case 2:
                return Repetition.WEEKLY;
            default:
                return Repetition.DAILY;

        }
    }

    /**
     * checks if app has permission to read the user's phone number
     * if it does not the permission is requested else it attempts to read it
     */
    private void checkPhonePermission() {
        //Check if User has permission to start to scan, if not it's start a RequestLoop
        if (!isPhonePermissionGranted()) {
            requestPhonePermission();
        } else {
            readPhoneNumber();
        }
    }

    /**
     * checks if the app has permission to read the user's phone number
     * @return true if it has the needed permission; false if not
     */
    private boolean isPhonePermissionGranted() {
        return ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * requests permission to read the user's phone number
     * the result is processed by {@link #onRequestPermissionsResult(int, String[], int[])}
     */
    private void requestPhonePermission() {
        //For Fragment: requestPermissions(permissionsList,REQUEST_CODE);
        //For Activity: ActivityCompat.requestPermissions(this,permissionsList,REQUEST_CODE);
        requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_READ_PHONE_STATE);
    }

    /**
     * processes whether or not the user granted the app permission to read their phone number
     * if they said no and didn't check "never ask again" it simply asks again 2 times
     * then simply asks the user to enter their phone number manually
     * if they granted the permission the phone number is read
     * @param requestCode the code of the permission request
     * @param permissions the permissions that the user was asked for
     * @param grantResults the results for the requests
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_READ_PHONE_STATE) {
            // for each permission check if the user granted/denied them you may want to group the
            // rationale in a single dialog,this is just an example
            for (int i = 0, len = permissions.length; i < len; i++) {

                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Activity activity = getActivity();
                    assert activity != null;
                    // user rejected the permission
                    boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE);
                    if (!showRationale) {
                        // user also CHECKED "never ask again" you can either enable some fall back,
                        // disable features of your app or open another dialog explaining again the
                        // permission and directing to the app setting

                        mAlertDialogBuilder = new AlertDialog.Builder(getActivity());
                        mAlertDialogBuilder.setTitle(R.string.accessWith_NeverAskAgain_deny)
                                .setMessage(R.string.sendSMS_accessDenied_withCheckbox)
                                .setPositiveButton(R.string.sendSMS_manual, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        openDialogAskForPhoneNumber();
                                    }
                                })
                                .create();
                        mAlertDialog = mAlertDialogBuilder.show();
                    } else if (counter < 1) {
                        // user did NOT check "never ask again" this is a good place to explain the user
                        // why you need the permission and ask if he wants // to accept it (the rationale)
                        mAlertDialogBuilder = new AlertDialog.Builder(getActivity());
                        mAlertDialogBuilder.setTitle(R.string.requestPermission_firstTryRequest)
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
                                        openDialogAskForPhoneNumber();
                                    }
                                })
                                .create();
                        mAlertDialog = mAlertDialogBuilder.show();
                    } else if (counter == 1) {
                        mAlertDialogBuilder = new AlertDialog.Builder(getActivity());
                        mAlertDialogBuilder.setTitle(R.string.sendSMS_lastTry)
                                .setMessage(R.string.lastTry_phoneNumber)
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
                                        openDialogAskForPhoneNumber();
                                    }
                                })
                                .create();
                        mAlertDialog = mAlertDialogBuilder.show();
                    } else {
                        openDialogAskForPhoneNumber();
                    }
                } else {
                    readPhoneNumber();
                }
            }
        }
    }

    // method to read the phone number of the user

    /**
     * @// TODO: 05.09.19 remove test regex (\\(555\\)521-5554|\\(555\\)521-5556)
     * reads the phone number of the user from the Sim card
     * if the phone number couldn't be read the user is asked to enter it manually
     */
    @SuppressLint({"MissingPermission", "HardwareIds"})
    private void readPhoneNumber() {
        //if permission is granted read the phone number
        Context ctx = getContext();
        assert ctx != null;
        if (ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            TelephonyManager telephonyManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            String phoneNumber = "";
            //check if the sim is in the phone
            if (telephonyManager != null) phoneNumber = telephonyManager.getLine1Number();
            //delete spaces and add a + if phoneNumber starts without a 0
            if (phoneNumber != null) {
                phoneNumber = phoneNumber.replaceAll(" ", "");
                //phone number starts with county number but no + or 00 (rg 491023 for a german number)if (phoneNumber.matches("[1-9][0-9]+"))
                phoneNumber = "+" + phoneNumber;
            }
            me.setPhoneNumber(phoneNumber);
            //if the number could not been read, open a dialog
            if (me.getPhoneNumber() == null || !me.getPhoneNumber().matches("\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|\n" +
                    "2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|\n" +
                    "4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$") && !me.getPhoneNumber().matches("(\\(555\\)521-5554|\\(555\\)521-5556)")) {
                openDialogAskForPhoneNumber();
            } else {
                Toast.makeText(getContext(), R.string.thanksphoneNumber, Toast.LENGTH_SHORT).show();
                saveEvent();
            }
        } else {
            if (me.getPhoneNumber() == null || !me.getPhoneNumber().matches("\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|\n" +
                    "2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|\n" +
                    "4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$")) {
                Toast.makeText(getContext(), R.string.notAbleToReadPhoneNumberCauseOfNoFunctionForThat, Toast.LENGTH_SHORT).show();
                openDialogAskForPhoneNumber();
            }
        }
    }

    /**
     * @// TODO: 05.09.19 remove test regex (\\(555\\)521-5554|\\(555\\)521-5556)
     *opens a dialog where the user is asked to enter their phone number
     * the entered number is validated and upon success the event is saved
     */
    private void openDialogAskForPhoneNumber() {
        mAlertDialogBuilder = new android.app.AlertDialog.Builder(getActivity());
        mAlertDialogBuilder.setView(R.layout.dialog_askingforphonenumber);
        mAlertDialogBuilder.setCancelable(true);
        mAlertDialog = mAlertDialogBuilder.create();
        mAlertDialog.show();
        mPhoneNumber = mAlertDialog.findViewById(R.id.dialog_EditText_telephonNumber);
        mPhoneNumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String inputText = v.getText().toString().replaceAll("\\s", "");
                //on click: read out the textfield, save the person and close the keyboard
                if (actionId == EditorInfo.IME_ACTION_DONE && inputText.matches("\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$") || inputText.matches("(\\(555\\)521-5554|\\(555\\)521-5556)")) {
                    me.setPhoneNumber(mPhoneNumber.getText().toString());
                    PersonController.savePerson(me);
                    Toast.makeText(getContext(), R.string.thanksphoneNumber, Toast.LENGTH_SHORT).show();
                    mAlertDialog.dismiss();
                    saveEvent();

                } else {
                    //show a toast if the number does not fit the regex
                    Toast.makeText(getContext(), R.string.wrongNumberFormat, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
        //if the dialog is canceled save nothing
        mAlertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Toast toast = Toast.makeText(getContext(), R.string.event_save_notSuccessful, Toast.LENGTH_SHORT);
                toast.show();
                Activity activity = getActivity();
                assert activity != null;
                if (activity.getCurrentFocus() != null) {
                    Context ctx = getContext();
                    assert ctx != null;
                    InputMethodManager mngr = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    assert mngr != null;
                    mngr.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
                }
            }
        });
    }

    /**
     * if the app is paused all dialogs are closed
     */
    public void onPause() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        } else if (mDialog != null) {
            mDialog.dismiss();
        } else if (mDatePickerDialog != null) {
            mDatePickerDialog.dismiss();
        } else if (mTimePickerDialog != null) {
            mTimePickerDialog.dismiss();
        }
        super.onPause();
    }
}