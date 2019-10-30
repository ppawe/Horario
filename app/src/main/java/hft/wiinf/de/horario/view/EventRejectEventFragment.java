/**
 * This is a fragment to reject an event and try to send a message (sms) to organizer.
 *
 * @author Team: Horario
 */

package hft.wiinf.de.horario.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.TabActivity;
import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.EventPersonController;
import hft.wiinf.de.horario.controller.NotificationController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.controller.SendSmsController;
import hft.wiinf.de.horario.model.AcceptedState;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.Person;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Fragment for rejecting {@link Event}s
 * displays the event's details, a spinner with reasons for the rejection and a mandatory editText field
 * for more detailed information about why the event was rejected
 */
public class EventRejectEventFragment extends Fragment {

    private static final String TAG = "EventRejectEvent";
    private EditText reason_for_rejection;
    private TextView reject_event_header;
    private TextView reject_event_description;
    private Spinner spinner_reason;
    private Button button_reject_event;
    private Button button_dialog_delete;
    private Button button_dialog_back;
    private AlertDialog mDialog;
    private Event selectedEvent;
    private Event event;
    private StringBuffer eventToStringBuffer;

    private String phNumber;
    private String rejectMessage;
    private String shortTitle;
    private Long creatorEventId;

    public EventRejectEventFragment() {

    }

    /**
     * inflates fragment_event_reject_event.xml into a view
     * @param inflater           a LayoutInflater used for inflating layouts into views
     * @param container          the parent view of the fragment
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     * @return the view created from inflating the layout
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_reject_event, container, false);
    }

    /**
     * initializes the view variables, sets onClickListeners for the buttons and initializes the form
     *
     * @param view               the view created in onCreateView
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //initialize GUI-Elements
        reason_for_rejection = view.findViewById(R.id.reject_event_editText_note);
        reason_for_rejection.setImeOptions(EditorInfo.IME_ACTION_DONE);
        reason_for_rejection.setRawInputType(InputType.TYPE_CLASS_TEXT);
        reject_event_description = view.findViewById(R.id.reject_event_textView_description);
        reject_event_header = view.findViewById(R.id.reject_event_textView_header);
        spinner_reason = view.findViewById(R.id.reject_event_spinner_reason);
        button_reject_event = view.findViewById(R.id.reject_event_button_reject);
        button_dialog_delete = view.findViewById(R.id.dialog_button_event_delete);
        button_dialog_back = view.findViewById(R.id.dialog_button_event_back);
        setSelectedEvent(EventController.getEventById(getEventID()));
        buildDescriptionEvent(EventController.getEventById(getEventID()));

        //initialize adapter
        ArrayAdapter reasonAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.reason_for_rejection, android.R.layout.simple_spinner_item);
        reasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_reason.setAdapter(reasonAdapter);

        //Make EditText-Field editable
        reason_for_rejection.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                reason_for_rejection.setFocusable(true);
                reason_for_rejection.setFocusableInTouchMode(true);
                return false;
            }
        });

        reason_for_rejection.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            //on click: close the keyboard after input is done
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                reason_for_rejection.setFocusable(false);
                reason_for_rejection.setFocusableInTouchMode(false);
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;

            }
        });

        button_reject_event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkForInput()) {
                    askForPermissionToDelete();
                }
            }
        });
    }

    /**
     * This method creates an AlertDialog to ask for final confirmation (yes or no).
     * This method return nothing. Next Steps depends on what is clicked (yes or no).
     * If "yes", method is restarting the TabActivity and calendar shows up, the status of the event
     * is changed to rejected and an SMS notifying the creator of the event of the rejection is sent.
     * If "no", method is going back to layout from EventRejectEventFragment.
     *
     */
    private void askForPermissionToDelete() {
        //Build dialog
        final AlertDialog.Builder dialogAskForFinalDecission = new AlertDialog.Builder(getContext());
        dialogAskForFinalDecission.setView(R.layout.dialog_afterrejectevent);
        dialogAskForFinalDecission.setTitle(R.string.titleDialogRejectEvent);
        dialogAskForFinalDecission.setCancelable(true);

        mDialog = dialogAskForFinalDecission.create();
        mDialog.show();

        //button listener on both buttons
        mDialog.findViewById(R.id.dialog_button_event_delete)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        event = EventController.getEventById((getEventID()));
                        //SMS
                        creatorEventId = event.getCreatorEventId();
                        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.SEND_SMS}, 2);
                        }
                        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                            //If an Event of a recurring event is cancelled, all events
                            // of the recurring event are deleted. This way the user can Scan the
                            // Event again and confirm it again.
                            if (event.getStartEvent() != null && event.getStartEvent().getId().equals(event.getId())) {
                                EventPersonController.changeStatusForSerial(event.getStartEvent(), PersonController.getPersonWhoIam(), AcceptedState.REJECTED, rejectMessage);
                            } else {
                                EventPersonController.changeStatus(event, PersonController.getPersonWhoIam(), AcceptedState.REJECTED, rejectMessage);
                            }
                            //delete alarm for notification
                            NotificationController.deleteAlarmNotification(getContext(), event);
                            rejectMessage = spinner_reason.getSelectedItem().toString() + "!" + reason_for_rejection.getText().toString();
                            new SendSmsController().sendSMS(getContext(), event.getCreator().getPhoneNumber(), rejectMessage, false, creatorEventId, event.getShortTitle());
                            Toast.makeText(getContext(), R.string.reject_event_hint, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Um einen Termin abzusagen benötigen wir die Berechtigung SMS zu senden", Toast.LENGTH_LONG).show();
                        }
                        //restart Activity
                        Intent intent = new Intent(getActivity(), TabActivity.class);
                        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
        //if button "no" has been clicked, cancel dialog.
        mDialog.findViewById(R.id.dialog_button_event_back)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialog.cancel();
                    }
                });

    }

    /**
     * Method to get the Id of the selected {@link Event} passed to this fragment from {@link SavedEventDetailsFragment},
     * {@link AcceptedEventDetailsFragment} or {@link TabActivity} during the FragmentTransaction
     * @return the Id of the selected event
     */
    private Long getEventID() {
        Bundle MYEventIdBundle = getArguments();
        return MYEventIdBundle.getLong("EventId");
    }


    private void setSelectedEvent(Event selectedEvent) {
        this.selectedEvent = selectedEvent;
    }

    /**
     * Sets the Event's description
     * @param selectedEvent: the event the user selected for rejection
     */
    private void buildDescriptionEvent(Event selectedEvent) {
        Person creator = selectedEvent.getCreator();
        String text = creator.getName() + " ("+ creator.getPhoneNumber() + ")\n" + selectedEvent.getShortTitle();
        reject_event_header.setText(text);
        // Check for a Repetition Event and Change the Description Output with and without
        // Repetition Element inside.
        reject_event_description.setText(EventController.createEventDescription(selectedEvent));
    }

    /**
     * This method checks if user input is valid. If input is not valid show Toast
     * @return false: if input is not valid; true: if input is valid
     */
    private boolean checkForInput() {
        if (reason_for_rejection.getText().length() == 0) {
            Toast.makeText(getContext(), R.string.reject_event_reason, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (reason_for_rejection.getText().length() > 100) {
            Toast.makeText(getContext(), R.string.reject_event_reason_free_text_field_to_long, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (reason_for_rejection.getText().toString().startsWith(" ")) {
            Toast.makeText(getContext(), R.string.reject_event_reason_free_text_field_empty, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!reason_for_rejection.getText().toString().matches("(\\w|\\.)(\\w|\\s|\\.)*")) {
            Toast.makeText(getContext(), R.string.reject_event_reason_special_characters, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * closes the "are you sure?" dialog whenever the application is paused
     */
    @Override
    public void onPause() {
        if (mDialog != null) {
            mDialog.dismiss();
        }

        super.onPause();

    }
}
