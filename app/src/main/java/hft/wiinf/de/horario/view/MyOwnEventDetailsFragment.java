package hft.wiinf.de.horario.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import hft.wiinf.de.horario.NFCActivity;
import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.EventPersonController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.controller.SendSmsController;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.Repetition;

/**
 * fragment representing an {@link Event} that the user created
 * displays details of the event and has options to invite a new user via SMS, display the event's QR Code
 * or show a list of users that have accepted, rejected or been invited to the event
 */
public class MyOwnEventDetailsFragment extends Fragment {

    public final static int NFC_REQUEST = 0;
    Event event;
    private Button myOwnEventDetailsButtonShowQR, myOwnEventDetailsButtonShowAcceptances, myOwnEventDetailsButtonSendInvite, myOwnEventDetailsButtonEdit;
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

    /**
     * Method to get the Id of the selected {@link Event} passed to this fragment from {@link CalendarFragment} during the FragmentTransaction
     *
     * @return the Id of the selected event
     */
    @SuppressLint("LongLogTag")
    private Long getEventID() {
        Bundle MYEventIdBundle = getArguments();
        assert MYEventIdBundle != null;
        return MYEventIdBundle.getLong("EventId");
    }

    /**
     * initializes the view variables and sets the onClickListeners for the buttons
     * one opens a dialog that lets you enter a phone number of a user to invite to the {@link Event}
     * one opens a list of users that have rejected or accepted the event
     * the last one opens a fragment that displays the QR Code associated with the event
     *
     * @param inflater           a LayoutInflater used for inflating layouts into views
     * @param container          the parent view of the fragment
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     * @return the view created from inflating the layout
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_own_event_details, container, false);
        myOwnEventDetailsButtonShowAcceptances = view.findViewById(R.id.myOwnEventDetailsButtonShowAcceptances);
        myOwnEventDetailsButtonShowQR = view.findViewById(R.id.myOwnEventDetailsButtonShowQR);
        myOwnEventDetailsButtonSendInvite = view.findViewById(R.id.myOwnEventDetailsButtonSendInvite);
        myOwnEventDetailsButtonEdit = view.findViewById(R.id.myOwnEventDetailsButtonEdit);
        rLayout_myOwnEvent_helper = view.findViewById(R.id.myOwnEvent_relativeLayout_helper);
        myOwnEventeventDescription = view.findViewById(R.id.myOwnEventeventDescription);
        myOwnEventYourAppointment = view.findViewById(R.id.myOwnEventyourAppointmentText);
        myOwnEventDetails_constraintLayout = view.findViewById(R.id.myOwnEventDetails_constraintLayout);
        setSelectedEvent(EventController.getEventById(getEventID()));
        buildDescriptionEvent(EventController.getEventById(getEventID()));
        activity = getActivity();


        Calendar now = Calendar.getInstance();
        if(now.getTime().after(selectedEvent.getStartTime()) && selectedEvent.getRepetition() == Repetition.NONE
                || now.getTime().after(selectedEvent.getEndRepetitionDate()) && selectedEvent.getRepetition() != Repetition.NONE){
            Drawable delete = ContextCompat.getDrawable(getContext(),R.drawable.ic_delete_black_24dp);
            myOwnEventDetailsButtonSendInvite.setCompoundDrawablesWithIntrinsicBounds(null,null, delete,null);
            myOwnEventDetailsButtonSendInvite.setText(R.string.delete_event);
            myOwnEventDetailsButtonSendInvite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final android.support.v7.app.AlertDialog.Builder askForConfirmation = new android.support.v7.app.AlertDialog.Builder(getContext());
                    askForConfirmation.setView(R.layout.dialog_afterrejectevent);
                    askForConfirmation.setTitle("Willst du den Termin wirklich löschen?");
                    askForConfirmation.setCancelable(true);
                    final android.support.v7.app.AlertDialog dialog = askForConfirmation.create();
                    dialog.show();
                    dialog.findViewById(R.id.dialog_button_event_delete).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            EventController.deleteEvent(selectedEvent);
                            dialog.cancel();
                            FragmentManager fm = getFragmentManager();
                            fm.popBackStack();
                            Toast.makeText(getContext(),"Termin wurde gelöscht", Toast.LENGTH_SHORT).show();
                        }
                    });
                    dialog.findViewById(R.id.dialog_button_event_back).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.cancel();
                        }
                    });
                }
            });
        }else {
            myOwnEventDetailsButtonSendInvite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //checks if the event's startTime is in the future
                    if (selectedEvent.getStartTime().after(new Date())) {
                        //opens dialog with invitation options
                        showOptionsDialog();
                    } else {
                        Toast.makeText(getContext(), R.string.event_is_in_past, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
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

        // Open the QRGeneratorFragment to Show the QRCode of this Event.
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

        myOwnEventDetailsButtonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditEventFragment editEventFragment = new EditEventFragment();
                Bundle bundleEdit = new Bundle();
                bundleEdit.putLong("EventId", getEventID());
                editEventFragment.setArguments(bundleEdit);

                Bundle whichFragment = getArguments();

                if (whichFragment.get("fragment").equals("EventOverview")) {
                    FragmentTransaction fragmentTransaction = Objects.requireNonNull(getFragmentManager()).beginTransaction();
                    fragmentTransaction.replace(R.id.eventOverview_frameLayout, editEventFragment, "MyOwnEventDetails");
                    fragmentTransaction.addToBackStack("MyOwnEventDetails");
                    fragmentTransaction.commit();
                } else {
                    FragmentTransaction fragmentTransaction = Objects.requireNonNull(getFragmentManager()).beginTransaction();
                    fragmentTransaction.replace(R.id.calendar_frameLayout, editEventFragment, "MyOwnEventDetails");
                    fragmentTransaction.addToBackStack("MyOwnEventDetails");
                    fragmentTransaction.commit();
                }
            }
        });

        return view;
    }

    public void showOptionsDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(R.layout.dialog_invitation_options);
        builder.setCancelable(true);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.findViewById(R.id.invitation_sms).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(R.layout.dialog_askingforphonenumber);
                builder.setCancelable(true);
                final AlertDialog dialog1 = builder.create();
                dialog1.show();
                TextView text = dialog1.findViewById(R.id.dialog_textView_telephoneNumber);
                text.setText(R.string.enter_recipient_number);
                EditText numberView = dialog1.findViewById(R.id.dialog_EditText_telephonNumber);
                numberView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        String inputText = textView.getText().toString().replaceAll("\\s", "");
                        //this regex checks valid country codes
                        if (actionId == EditorInfo.IME_ACTION_DONE && inputText.matches("\\+(9[976]\\d|8[987530]\\d|6[987]\\d|5[90]\\d|42\\d|3[875]\\d|2[98654321]\\d|9[8543210]|8[6421]|6[6543210]|5[87654321]|4[987654310]|3[9643210]|2[70]|7|1)\\d{1,14}$") || inputText.matches("(\\(555\\)521-5554|\\(555\\)521-5556)")) {
                            sendToNumber = inputText;
                            dialog1.dismiss();
                            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 2);
                            }
                            //check if the number has already been invited or accepted the invitation
                            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                                    getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                                List<Person> participants = EventPersonController.getEventParticipants(selectedEvent);
                                boolean alreadyInvited = false;
                                for (Person participant : participants) {
                                    if (participant.getPhoneNumber().equals(sendToNumber)) {
                                        alreadyInvited = true;
                                    }
                                }
                                if (alreadyInvited || PersonController.getPersonViaPhoneNumber(sendToNumber) != null &&
                                        EventPersonController.personIsInvitedToEvent(selectedEvent, PersonController.getPersonViaPhoneNumber(sendToNumber))) {
                                    Toast.makeText(getContext(), "Person nimmt bereits teil oder wurde bereits eingeladen.", Toast.LENGTH_SHORT).show();
                                    dialog1.cancel();
                                } else {
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
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.invitation_nfc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                Intent intent = new Intent(getContext(), NFCActivity.class);
                intent.putExtra("id",selectedEvent.getId());
                startActivityForResult(intent,NFC_REQUEST);
            }
        });
    }

    public Event getSelectedEvent() {
        return selectedEvent;
    }

    private void setSelectedEvent(Event selectedEvent) {
        this.selectedEvent = selectedEvent;
    }

    /**
     * This method formats the StringBuffer received from stringBufferGenerator() into a
     * String detailing the information about the chosen {@link Event}
     *
     * @param selectedEvent: the event the user selected for rejection
     */
    private void buildDescriptionEvent(Event selectedEvent) {
        // Event shortTitle in Headline with StartDate
        String yourEvent = "Dein Termin" + "\n" + selectedEvent.getShortTitle();
        myOwnEventYourAppointment.setText(yourEvent);
        // Check for a Repetition Event and Change the Description Output with and without
        // Repetition Element inside.
        myOwnEventeventDescription.setText(EventController.createEventDescription(selectedEvent));
    }

}


