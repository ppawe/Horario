package hft.wiinf.de.horario.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.EventPersonController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.controller.SendSmsController;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.Person;

public class MyOwnEventDetailsFragment extends Fragment {

    Event event;
    private Button myOwnEventDetailsButtonShowQR;
    private Button myOwnEventDetailsButtonShowAcceptances;
    private Button myOwnEventDetailsButtonSendInvite;
    private RelativeLayout rLayout_myOwnEvent_helper;
    private ConstraintLayout myOwnEventDetails_constraintLayout;
    private TextView myOwnEventeventDescription;
    private TextView myOwnEventYourAppointment;
    private Event selectedEvent;
    private StringBuffer eventToStringBuffer;
    private Activity activity;
    private String sendToNumber = null;

    public MyOwnEventDetailsFragment() {
        // Required empty public constructor
    }

    // Get the EventIdResultBundle (Long) from the CalenderFragment to Start later a DB Request
    @SuppressLint("LongLogTag")
    private Long getEventID() {
        Bundle MYEventIdBundle = getArguments();
        assert MYEventIdBundle != null;
        return MYEventIdBundle.getLong("EventId");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_own_event_details, container, false);
        myOwnEventDetailsButtonShowAcceptances = view.findViewById(R.id.myOwnEventDetailsButtonShowAcceptances);
        myOwnEventDetailsButtonShowQR = view.findViewById(R.id.myOwnEventDetailsButtonShowQR);
        myOwnEventDetailsButtonSendInvite = view.findViewById(R.id.myOwnEventDetailsButtonSendInvite);
        rLayout_myOwnEvent_helper = view.findViewById(R.id.myOwnEvent_relativeLayout_helper);
        myOwnEventeventDescription = view.findViewById(R.id.myOwnEventeventDescription);
        myOwnEventYourAppointment = view.findViewById(R.id.myOwnEventyourAppointmentText);
        myOwnEventDetails_constraintLayout = view.findViewById(R.id.myOwnEventDetails_constraintLayout);
        setSelectedEvent(EventController.getEventById(getEventID()));
        buildDescriptionEvent(EventController.getEventById(getEventID()));
        activity = getActivity();


        myOwnEventDetailsButtonSendInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //checks if the event's startTime is in the future
                if (selectedEvent.getStartTime().after(new Date())) {
                    //opens input dialog for phone number
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setView(R.layout.dialog_askingforphonenumber);
                    builder.setCancelable(true);
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                    TextView text = dialog.findViewById(R.id.dialog_textView_telephoneNumber);
                    text.setText(R.string.enter_recipient_number);
                    EditText numberView = dialog.findViewById(R.id.dialog_EditText_telephonNumber);
                    numberView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                            String inputText = textView.getText().toString().replaceAll("\\s", "");
                            //this regex checks valid country codes
                            if (actionId == EditorInfo.IME_ACTION_DONE && inputText.matches("\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$") || inputText.matches("(\\(555\\)521-5554|\\(555\\)521-5556)")) {
                                sendToNumber = inputText;
                                dialog.dismiss();
                                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 2);
                                }
                                //check if the number has already been invited or accepted the invitation
                                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                                        getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                                    List<Person> participants = EventPersonController.getEventParticipants(selectedEvent);
                                    boolean alreadyInvited = false;
                                    for(Person participant : participants){
                                        if(participant.getPhoneNumber().equals(sendToNumber)){
                                            alreadyInvited = true;
                                        }
                                    }
                                    if (alreadyInvited || PersonController.getPersonViaPhoneNumber(sendToNumber) != null &&
                                            EventPersonController.personIsInvitedToEvent(selectedEvent, PersonController.getPersonViaPhoneNumber(sendToNumber))) {
                                        Toast.makeText(getContext(), "Person nimmt bereits teil oder wurde bereits eingeladen.", Toast.LENGTH_SHORT).show();
                                        dialog.cancel();
                                    }else {
                                        //send the invitation
                                        new SendSmsController().sendInvitationSMS(getContext(), selectedEvent, sendToNumber);
                                    }
                                } else {
                                    Toast.makeText(getContext(), R.string.sending_sms_impossible, Toast.LENGTH_SHORT).show();
                                    Log.d("louis", getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ? "true" : "false");
                                }
                            }
                            return false;
                        }
                    });
                }else{
                    Toast.makeText(getContext(), R.string.event_is_in_past,Toast.LENGTH_SHORT).show();
                }
            }
        });
        myOwnEventDetailsButtonShowAcceptances.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ParticipantsListFragment participantsFragment = new ParticipantsListFragment();
                Bundle bundleparticipants = new Bundle();
                bundleparticipants.putLong("EventId", getEventID());
                participantsFragment.setArguments(bundleparticipants);

                Bundle whichFragment = getArguments();

                if (whichFragment.get("fragment").equals("EventOverview")) {
                    FragmentTransaction fragmentTransaction = Objects.requireNonNull(getFragmentManager()).beginTransaction();
                    fragmentTransaction.replace(R.id.eventOverview_frameLayout, participantsFragment, "MyOwnEventDetails");
                    fragmentTransaction.addToBackStack("MyOwnEventDetails");
                    fragmentTransaction.commit();
                } else {
                    FragmentTransaction fragmentTransaction = Objects.requireNonNull(getFragmentManager()).beginTransaction();
                    fragmentTransaction.replace(R.id.calendar_frameLayout, participantsFragment, "MyOwnEventDetails");
                    fragmentTransaction.addToBackStack("MyOwnEventDetails");
                    fragmentTransaction.commit();
                }
            }
        });

        // Open the QRGeneratorFragment to Show the QRCode form this Event.
        myOwnEventDetailsButtonShowQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle whichFragment = getArguments();
                QRGeneratorFragment qrFrag = new QRGeneratorFragment();
                Bundle bundle = new Bundle();
                bundle.putLong("eventId", getEventID());
                bundle.putString("fragment", whichFragment.getString("fragment"));
                qrFrag.setArguments(bundle);

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

        return view;
    }


    public Event getSelectedEvent() {
        return selectedEvent;
    }

    private void setSelectedEvent(Event selectedEvent) {
        this.selectedEvent = selectedEvent;
    }

    private void buildDescriptionEvent(Event selectedEvent) {
        //Put StringBuffer in an Array and split the Values to new String Variables
        //Index: 0 = CreatorID; 1 = StartDate; 2=date of event (for serial events) 3 = EndDate; 4 = StartTime; 5 = EndTime;
        //       6 = Repetition; 7 = ShortTitle; 8 = Place; 9 = Description;  10 = EventCreatorName
        String[] eventStringBufferArray = String.valueOf(stringBufferGenerator()).split("\\|");
        String startDate = eventStringBufferArray[1].trim();
        String currentDate = eventStringBufferArray[2].trim();
        String endDate = eventStringBufferArray[3].trim();
        String startTime = eventStringBufferArray[4].trim();
        String endTime = eventStringBufferArray[5].trim();
        String repetition = eventStringBufferArray[6].toUpperCase().trim();
        String shortTitle = eventStringBufferArray[7].trim();
        String place = eventStringBufferArray[8].trim();
        String description = eventStringBufferArray[9].trim();
        String eventCreatorName = eventStringBufferArray[10].trim();

        // Change the DataBase Repetition Information in a German String for the Repetition Element
        // like "Daily" into "täglich" and so on
        switch (repetition) {
            case "YEARLY":
                repetition = "Jährlich";
                break;
            case "MONTHLY":
                repetition = "Monatlich";
                break;
            case "WEEKLY":
                repetition = "Wöchentlich";
                break;
            case "DAILY":
                repetition = "Täglich";
                break;
            case "NONE":
                repetition = "";
                break;
            default:
                repetition = getString(R.string.without_repetition);
        }

        // Check the EventCreatorName and is it itself Change the eventCreaterName to "Your Self"
        if (eventCreatorName.equals(PersonController.getPersonWhoIam().getName())) {
            eventCreatorName = getString(R.string.yourself);
        }
        // Event shortTitel in Headline with StartDate
        String yourEvent = "Dein Termin" + "\n" + shortTitle;
        myOwnEventYourAppointment.setText(yourEvent);
        // Check for a Repetition Event and Change the Description Output with and without
        // Repetition Element inside.
        if (repetition.equals("")) {
            String text = getString(R.string.event_date) + currentDate + "\n" + getString(R.string.time) + startTime + getString(R.string.until)
                    + endTime + getString(R.string.clock) + "\n" + getString(R.string.place) + place + "\n" + "\n" + getString(R.string.eventDetails)
                    + description;
            myOwnEventeventDescription.setText(text);
        } else {
            String text = getString(R.string.event_date) + startDate
                    + "\n" + getString(R.string.time) + startTime + getString(R.string.until) + endTime + getString(R.string.clock) + "\n" + getString(R.string.place) + place + "\n" + "Wiederholung: " + repetition + getString(R.string.until) + endDate + "\n\n" + getString(R.string.eventDetails)
                    + description;
            myOwnEventeventDescription.setText(text);
        }
    }

    private StringBuffer stringBufferGenerator() {

        //Modify the Dateformat form den DB to get a more readable Form for Date and Time disjunct
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm");

        //Splitting String Element is the Pipe Symbol (on the Keyboard ALT Gr + <> Button = |)
        String stringSplitSymbol = " | "; //

        // Merge the Data Base Information to one Single StringBuffer with the Format:
        // CreatorID (not EventID!!), StartDate, currentDate, EndDate, StartTime, EndTime, Repetition, ShortTitle
        // Place, Description and Name of EventCreator
        eventToStringBuffer = new StringBuffer();
        eventToStringBuffer.append(selectedEvent.getId() + stringSplitSymbol);
        if (selectedEvent.getStartEvent() == null)
            eventToStringBuffer.append(simpleDateFormat.format(selectedEvent.getStartTime()) + stringSplitSymbol);
        else
            eventToStringBuffer.append(simpleDateFormat.format(selectedEvent.getStartEvent().getStartTime()) + stringSplitSymbol);
        eventToStringBuffer.append(simpleDateFormat.format(selectedEvent.getStartTime()) + stringSplitSymbol);
        eventToStringBuffer.append(simpleDateFormat.format(selectedEvent.getEndDate()) + stringSplitSymbol);
        eventToStringBuffer.append(simpleTimeFormat.format(selectedEvent.getStartTime()) + stringSplitSymbol);
        eventToStringBuffer.append(simpleTimeFormat.format(selectedEvent.getEndTime()) + stringSplitSymbol);
        eventToStringBuffer.append(selectedEvent.getRepetition() + stringSplitSymbol);
        eventToStringBuffer.append(selectedEvent.getShortTitle() + stringSplitSymbol);
        eventToStringBuffer.append(selectedEvent.getPlace() + stringSplitSymbol);
        eventToStringBuffer.append(selectedEvent.getDescription() + stringSplitSymbol);
        eventToStringBuffer.append(selectedEvent.getCreator().getName());

        return eventToStringBuffer;

    }
}


