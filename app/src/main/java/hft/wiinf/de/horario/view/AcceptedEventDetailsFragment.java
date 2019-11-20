package hft.wiinf.de.horario.view;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.Repetition;

/**
 * a fragment for displaying the details of an event the user has accepted
 * has buttons for showing the event's QR code as well as rejecting the event
 */
public class AcceptedEventDetailsFragment extends Fragment {

    Event event;
    private Button acceptedEventDetailsButtonShowQR;
    private Button acceptedEventDetailsButtonRefuseAppointment;
    private TextView acceptedEventDetailsOrganisatorText;
    private TextView acceptedEventphNumberText;
    private TextView acceptedEventeventDescription;
    private RelativeLayout rLayout_acceptedEvent_helper;
    private Event selectedEvent;
    private StringBuffer eventToStringBuffer;

    /**
     * required empty public constructor
     */
    public AcceptedEventDetailsFragment() {
    }

    /**
     * get the event's ID from the arguments passed to the fragment
     *
     * @return Long value of the event id
     */
    @SuppressLint("LongLogTag")
    private Long getEventID() {
        Bundle MYEventIdBundle = getArguments();
        assert MYEventIdBundle != null;
        return MYEventIdBundle.getLong("EventId");
    }

    /**
     * sets the global view variables and sets the onclick methods for the reject event and show QR code buttons
     *
     * @param inflater           LayoutInflater used for inflating the layout into views
     * @param container          the parent view of the fragment
     * @param savedInstanceState the saved state of the fragment from before some system event changed it
     * @return the inflated view with all the changes applied to it
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_accepted_event_details, container, false);

        acceptedEventDetailsButtonRefuseAppointment = view.findViewById(R.id.acceptedEventDetailsButtonRefuseAppointment);
        acceptedEventDetailsButtonShowQR = view.findViewById(R.id.acceptedEventDetailsButtonShowQR);
        rLayout_acceptedEvent_helper = view.findViewById(R.id.acceptedEvent_relativeLayout_helper);
        acceptedEventDetailsOrganisatorText = view.findViewById(R.id.acceptedEventDetailsOrganisatorText);
        acceptedEventphNumberText = view.findViewById(R.id.acceptedEventphNumberText);
        acceptedEventeventDescription = view.findViewById(R.id.acceptedEventeventDescription);
        setSelectedEvent(EventController.getEventById(getEventID()));
        buildDescriptionEvent(EventController.getEventById(getEventID()));

        Calendar now = Calendar.getInstance();
        if(now.getTime().after(selectedEvent.getStartTime()) && selectedEvent.getRepetition() == Repetition.NONE
                || now.getTime().after(selectedEvent.getEndRepetitionDate()) && selectedEvent.getRepetition() != Repetition.NONE){
            Drawable delete = ContextCompat.getDrawable(getContext(),R.drawable.ic_delete_black_24dp);
            acceptedEventDetailsButtonRefuseAppointment.setCompoundDrawablesWithIntrinsicBounds(null,null, delete,null);
            acceptedEventDetailsButtonRefuseAppointment.setText(R.string.delete_event);
            acceptedEventDetailsButtonRefuseAppointment.setOnClickListener(new View.OnClickListener() {
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
            acceptedEventDetailsButtonRefuseAppointment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Code for cancelling an event eg. take it out of the DB and Calendar View
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(System.currentTimeMillis());
                    if (EventController.getEventById(getEventID()).getEndTime().after(cal.getTime())) {
                        Bundle whichFragment = getArguments();
                        EventRejectEventFragment eventRejectEventFragment = new EventRejectEventFragment();
                        Bundle bundleAcceptedEventId = new Bundle();
                        bundleAcceptedEventId.putLong("EventId", getEventID());
                        bundleAcceptedEventId.putString("fragment", "AcceptedEventDetails");
                        eventRejectEventFragment.setArguments(bundleAcceptedEventId);
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
        }

        // Open the QRGeneratorFragment to Show the QRCode form this Event.
        acceptedEventDetailsButtonShowQR.setOnClickListener(new View.OnClickListener() {
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
     * gets the currently selected {@link Event}
     * @return the currently selected Event
     */
    public Event getSelectedEvent() {
        return selectedEvent;
    }

    /**
     * sets the currently selected {@link Event}
     * @param selectedEvent the event to be selected
     */
    private void setSelectedEvent(Event selectedEvent) {
        this.selectedEvent = selectedEvent;
    }

    /**
     * builds the string representing the description of the accepted {@link Event}
     * and displays it in the fragment's view
     * @param selectedEvent the currently selected accepted event
     */
    private void buildDescriptionEvent(Event selectedEvent) {
        // Event shortTitel in Headline with StartDate
        Person creator = selectedEvent.getCreator();
        String text = creator.getName() + " ("+ creator.getPhoneNumber() + ")\n" + selectedEvent.getShortTitle();
        acceptedEventDetailsOrganisatorText.setText(text);
        acceptedEventphNumberText.setText("");
        // Check for a Repetition Event and Change the Description Output with and without
        // Repetition Element inside.
        acceptedEventeventDescription.setText(EventController.createEventDescription(selectedEvent));
    }
}