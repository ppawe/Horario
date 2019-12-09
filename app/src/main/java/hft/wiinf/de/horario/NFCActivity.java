package hft.wiinf.de.horario;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.InvitationController;
import hft.wiinf.de.horario.controller.NotificationController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.InvitationString;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.model.Repetition;
import hft.wiinf.de.horario.view.MyOwnEventDetailsFragment;

public class NFCActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            //This will refer back to createNdefMessage for what it will send
            mNfcAdapter.setNdefPushMessageCallback(this, this);

            //This will be called if the message is sent successfully
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            handleNFCIntent(getIntent());
        }
    }

    private void handleNFCIntent(Intent NfcIntent) {
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(NfcIntent.getAction())) {
            return;
        }
        Parcelable[] receivedArray = NfcIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

        if (receivedArray == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        NdefRecord[] records = ((NdefMessage) receivedArray[0]).getRecords();
        for (NdefRecord record : records) {
            String string = new String(record.getPayload());
            if (!string.equals(getPackageName())) {
                sb.append(string);
            }
        }
        if (!InvitationController.checkForInvitationRegexOk(sb.toString())) {
            return;
        }
        InvitationString newInvitationString = new InvitationString(sb.toString().replaceAll(":HorarioInvitation:", ""), new Date());
        String eventDateTimeString = newInvitationString.getStartTime() + " " + newInvitationString.getStartDate();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm dd.MM.yyyy");
        try {
            Date eventDateTime = format.parse(eventDateTimeString);

            if (eventDateTime.after(newInvitationString.getDateReceived()) && newInvitationString.getRepetitionAsRepetition() == Repetition.NONE
                    || newInvitationString.getRepetitionAsRepetition() != Repetition.NONE &&
                    newInvitationString.getEndDateAsDate().after(newInvitationString.getDateReceived()) &&
                    !InvitationController.eventAlreadySaved(newInvitationString)) {
                Event invitedEvent = EventController.createInvitedEventFromInvitation(newInvitationString);
                Person creator = PersonController.addOrGetPerson(newInvitationString.getCreatorPhoneNumber(), newInvitationString.getCreatorName());
                creator.setName(newInvitationString.getCreatorName());
                Intent intent = new Intent(this, TabActivity.class);
                intent.putExtra("id", String.valueOf(invitedEvent.getId()));
                startActivity(intent);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            handleNFCIntent(intent);
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        if (getIntent().getStringExtra("id") != null) {
            return new NdefMessage(createRecords());
        }
        return null;
    }

    private NdefRecord[] createRecords() {
        NdefRecord[] records = new NdefRecord[2];
        Event event = EventController.getEventById(Long.valueOf(getIntent().getStringExtra("id")));
        String payload = "HorarioInvitation:" + EventController.createEventInvitation(event) + "HorarioInvitation:";
        records[0] = NdefRecord.createMime("text/plain", payload.getBytes(Charset.forName("UTF-8")));
        records[1] = NdefRecord.createApplicationRecord(getPackageName());
        return records;
    }

    @Override
    public void onNdefPushComplete(NfcEvent nfcEvent) {
        Toast.makeText(this, "Einladung wurde verschickt", Toast.LENGTH_LONG).show();
        finishActivity(MyOwnEventDetailsFragment.NFC_REQUEST);
    }
}
