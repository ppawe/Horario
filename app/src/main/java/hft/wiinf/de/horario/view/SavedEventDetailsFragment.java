package hft.wiinf.de.horario.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.query.Select;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.EventPersonController;
import hft.wiinf.de.horario.controller.NotificationController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.controller.SendSmsController;
import hft.wiinf.de.horario.model.AcceptedState;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.Repetition;

/**
 * Fragment used to display details of {@link Event}s that the user has saved but neither accepted nor rejected.
 * Has Buttons that allow the user to see the event's QR Code, accept it or reject it
 */
public class SavedEventDetailsFragment extends Fragment {

    Event event;
    private Button savedEventDetailsButtonRefuseAppointment;
    private Button savedEventDetailsButtonAcceptAppointment;
    private Button savedEventDetailsButtonShowQr;
    private RelativeLayout rLayout_savedEvent_helper;
    private TextView savedEventDetailsOrganisatorText;
    private TextView savedEventphNumberText;
    private TextView savedEventeventDescription;
    private Event selectedEvent;
    private AlertDialog mAlertDialog;
    private Long creatorEventId;

    public SavedEventDetailsFragment() {
        // Required empty public constructor
    }

    // Get the EventIdResultBundle (Long) from the newEventActivity to Start later a DB Request

    /**
     * Method to get the Id of the selected {@link Event} passed to this fragment during the FragmentTransaction
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
     * Inflates the fragment_saved_event_details.xml into a view and adds OnClickListeners to the buttons
     * allowing the user to view the {@link Event}s QR Code and reject or accept the event
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
        View view = inflater.inflate(R.layout.fragment_saved_event_details, container, false);
        savedEventDetailsButtonRefuseAppointment = view.findViewById(R.id.savedEventDetailsButtonRefuseAppointment);
        savedEventDetailsButtonAcceptAppointment = view.findViewById(R.id.savedEventDetailsButtonAcceptAppointment);
        savedEventDetailsButtonShowQr = view.findViewById(R.id.savedEventDetailsButtonShowQr);
        rLayout_savedEvent_helper = view.findViewById(R.id.savedEvent_relativeLayout_helper);
        savedEventDetailsOrganisatorText = view.findViewById(R.id.savedEventDetailsOrganisatorText);
        savedEventphNumberText = view.findViewById(R.id.savedEventphNumberText);
        savedEventeventDescription = view.findViewById(R.id.savedEventeventDescription);
        setSelectedEvent(EventController.getEventById(getEventID()));
        buildDescriptionEvent(EventController.getEventById(getEventID()));


        Calendar now = Calendar.getInstance();
        if(now.getTime().after(selectedEvent.getStartTime()) && selectedEvent.getRepetition() == Repetition.NONE || now.getTime().after(selectedEvent.getEndRepetitionDate()) && selectedEvent.getRepetition() != Repetition.NONE){
            savedEventDetailsButtonRefuseAppointment.setVisibility(View.INVISIBLE);
            Drawable delete = ContextCompat.getDrawable(getContext(),R.drawable.ic_delete_48dp);
            savedEventDetailsButtonAcceptAppointment.setCompoundDrawablesWithIntrinsicBounds(null,null,delete,null);
            savedEventDetailsButtonAcceptAppointment.setText(R.string.delete_event);
            savedEventDetailsButtonAcceptAppointment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog.Builder askForConfirmation = new AlertDialog.Builder(getContext());
                    askForConfirmation.setView(R.layout.dialog_afterrejectevent);
                    askForConfirmation.setTitle("Willst du den Termin wirklich löschen?");
                    askForConfirmation.setCancelable(true);
                    final AlertDialog dialog = askForConfirmation.create();
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
            savedEventDetailsButtonRefuseAppointment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Code for cancelling an event eg. take it out of the DB and Calendar View
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(System.currentTimeMillis());
                    if (EventController.getEventById(getEventID()).getEndTime().after(cal.getTime())) {
                        Bundle whichFragment = getArguments();
                        EventRejectEventFragment eventRejectEventFragment = new EventRejectEventFragment();
                        Bundle bundle = new Bundle();
                        bundle.putLong("EventId", getEventID());
                        bundle.putString("fragment", whichFragment.getString("fragment"));
                        eventRejectEventFragment.setArguments(bundle);

                        if (whichFragment.getString("fragment").equals("EventOverview")) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.eventOverview_frameLayout, eventRejectEventFragment, "RejectEvent")
                                    .addToBackStack("RejectEvent")
                                    .commit();
                        } else {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.calendar_frameLayout, eventRejectEventFragment, "RejectEvent")
                                    .addToBackStack("RejectEvent")
                                    .commit();
                        }
                    } else {
                        Toast.makeText(getContext(), R.string.startTime_afterScanning_past, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            savedEventDetailsButtonAcceptAppointment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(System.currentTimeMillis());
                    if (cal.getTime().before(EventController.getEventById(getEventID()).getEndTime())) {
                        askForPermissionToSave();
                    } else {
                        Toast.makeText(getContext(), R.string.startTime_afterScanning_past, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Open the QRGeneratorFragment to Show the QRCode form this Event.
        savedEventDetailsButtonShowQr.setOnClickListener(new View.OnClickListener() {
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

    /**
     * asks the user to confirm their decision to accept the selected {@link Event} and sets the
     * {@link hft.wiinf.de.horario.model.EventPerson}'s status to accepted if they do so, as well as
     * sending a confirmation SMS to the creator
     */
    private void askForPermissionToSave() {
        final AlertDialog.Builder dialogAskForFinalDecission = new AlertDialog.Builder(getContext());
        dialogAskForFinalDecission.setView(R.layout.dialog_afterrejectevent);
        dialogAskForFinalDecission.setTitle(R.string.titleDialogSaveEvent);
        dialogAskForFinalDecission.setCancelable(true);

        mAlertDialog = dialogAskForFinalDecission.create();
        mAlertDialog.show();

        mAlertDialog.findViewById(R.id.dialog_button_event_delete)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Pull the EventID change the AcceptedState and Save again.
                        Event event = EventController.getEventById(getEventID());

                        //SMS
                        creatorEventId = event.getCreatorEventId();

                        //Check the Event if its a SingleEvent it set Accepted State just for this Event
                        //and send a SMS
                        if (event.getRepetition() == Repetition.NONE) {


                            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.SEND_SMS}, 2);
                            }
                            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                                new SendSmsController().sendSMS(getContext(), event.getCreator().getPhoneNumber(), null, true,
                                        creatorEventId, event.getShortTitle());
                                EventPersonController.changeStatus(event, PersonController.getPersonWhoIam(), AcceptedState.ACCEPTED, null);
                                Toast.makeText(getContext(), R.string.accept_event_hint, Toast.LENGTH_SHORT).show();
                                NotificationController.setAlarmForNotification(getContext(), event);
                            }
                            Intent intent = new Intent(getActivity(), hft.wiinf.de.horario.TabActivity.class);
                            startActivity(intent);
                            // If have the Event a Repetition it set all Events to Accepted and send a SMS
                        } else {

                            //Create a List with all Events with the same startId an set the State
                            //to Accepted
                            List<Event> findMyEventsByStartId =
                                    new Select().from(Event.class).where("startEvent = ?",
                                            String.valueOf(event.getStartEvent().getId())).execute();

                            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.SEND_SMS}, 2);
                            }
                            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                                for (Event x : findMyEventsByStartId) {
                                    EventPersonController.changeStatus(x, PersonController.getPersonWhoIam(), AcceptedState.ACCEPTED, null);
                                    NotificationController.setAlarmForNotification(getContext(), x);
                                }
                                new SendSmsController().sendSMS(getContext(), event.getCreator().getPhoneNumber(), null, true,
                                        creatorEventId, event.getShortTitle());
                                Toast.makeText(getContext(), R.string.accept_event_hint, Toast.LENGTH_SHORT).show();
                            }

                            Intent intent = new Intent(getActivity(), hft.wiinf.de.horario.TabActivity.class);
                            startActivity(intent);
                        }
                    }
                });

        mAlertDialog.findViewById(R.id.dialog_button_event_back)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAlertDialog.cancel();
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
     * @param selectedEvent: the saved event the user selected
     */
    private void buildDescriptionEvent(Event selectedEvent) {
        // Event shortTitel in Headline with StartDate
        Person creator = selectedEvent.getCreator();
        String text = creator.getName() + " ("+ creator.getPhoneNumber() + ")\n" + selectedEvent.getShortTitle();
        savedEventDetailsOrganisatorText.setText(text);
        savedEventphNumberText.setText("");
        // Check for a Repetition Event and Change the Description Output with and without
        // Repetition Element inside.
        savedEventeventDescription.setText(EventController.createEventDescription(selectedEvent));
    }

    /**
     * closes an existing dialog if the app is paused
     */
    public void onPause() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }

        super.onPause();

    }
}