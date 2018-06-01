package hft.wiinf.de.horario.controller;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.model.FailedSMS;
import hft.wiinf.de.horario.model.Person;
import hft.wiinf.de.horario.service.FailedSMSService;
import hft.wiinf.de.horario.utility.BundleUtility;

public class SendSmsController extends BroadcastReceiver {

    public static final String SENT = "SMS_SENT";
    public static String sms_phoneNo, sms_msg;
    public static boolean sms_acc;
    public static long sms_creatorID;
    public static Context cont;


    public static void sendSMS(final Context context, String sms_phoneNumber, String sms_message, boolean sms_accepted, long sms_creatorEventId) {
        sms_phoneNo = sms_phoneNumber;
        sms_msg = sms_message;
        sms_acc = sms_accepted;
        sms_creatorID = sms_creatorEventId;
        cont = context;

        if (!canSendSMS(context)) {
            Toast.makeText(context, context.getString(R.string.cannot_send_sms), Toast.LENGTH_SHORT).show();
        } else {

            String msg;
            Person personMe = PersonController.getPersonWhoIam();
            if (sms_accepted) {
                //SMS: :Horario:123,1,Lucas
                //(":Horario:" als Kennzeichner, 123 als creatorEventId, 1 für Zusage, Lucas als Name der Person im Handy)
                msg = ":Horario:" + sms_creatorEventId + ",1," + personMe.getName();
            } else {
                //SMS: :Horario:123,0,Lucas,Krankheit!habe die Grippe
                //(":Horario:" als Kennzeichner, 123 als creatorEventId, 0
                // für Absage, Lucas als Name der Person im Handy, Krankheit als Absagekategorie, !
                // als Kennzeichner (drin lassen!!!), habe die Grippe als persönliche Notiz)
                msg = ":Horario:" + sms_creatorEventId + ",0," + personMe.getName() + "," + sms_message;
            }

            try {
                PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);

                final SendSmsController smsUtils = new SendSmsController();
                //register for sending and delivery
                context.registerReceiver(smsUtils, new IntentFilter(SendSmsController.SENT));

                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(sms_phoneNumber, null, msg, sentPI, null);

                //we unsubscribed in 10 seconds
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        context.unregisterReceiver(smsUtils);
                    }
                }, 10000);
            }catch(Exception e){
                Toast.makeText(context, context.getString(R.string.cannot_send_sms), Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void startJobSendSMS() {
        Log.d("Tag","Florian:" + sms_msg + sms_phoneNo + sms_creatorID + sms_acc);
        FailedSMS failedSMS = new FailedSMS(sms_msg, sms_phoneNo, sms_creatorID, sms_acc);
        saveFailedSMS(failedSMS);

        Bundle sms = new Bundle();
        sms.putString("phoneNo", sms_phoneNo);
        sms.putString("message", sms_msg);
        sms.putLong("creatorID", sms_creatorID);
        sms.putBoolean("accepted", sms_acc);
        sms.putInt("id", failedSMS.getId().intValue());

        PersistableBundle persBund = BundleUtility.toPersistableBundle(sms);
        JobScheduler jobScheduler = (JobScheduler) cont.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(new JobInfo.Builder(failedSMS.getId().intValue(), new ComponentName(cont, FailedSMSService.class))
                .setExtras(persBund)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build());
    }

    public void saveFailedSMS(FailedSMS failedSMS) {
        FailedSMSController.addFailedSMS(failedSMS);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SENT)) {
            switch (getResultCode()) {
                case Activity.RESULT_OK: // Sms sent
                    Toast.makeText(context, context.getString(R.string.sms_sent), Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE: // generic failure
                    startJobSendSMS();
                    Toast.makeText(context, context.getString(R.string.sms_fail), Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE: // No service
                    startJobSendSMS();
                    Toast.makeText(context, context.getString(R.string.sms_fail), Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU: // null pdu
                    startJobSendSMS();
                    Toast.makeText(context, context.getString(R.string.sms_fail), Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF: //Radio off
                    startJobSendSMS();
                    Toast.makeText(context, context.getString(R.string.sms_fail), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    public static boolean canSendSMS(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }
}

