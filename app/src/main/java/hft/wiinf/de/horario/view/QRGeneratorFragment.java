package hft.wiinf.de.horario.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.EventController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.model.Person;

/**
 * A fragment used for displaying an {@link Event}'s information as a QR Code which other users
 * can scan to be invited to the event. Has 2 buttons that allow the user to share the QR code or
 * go back to their previous overview fragment ({@link EventOverviewFragment} or {@link CalendarFragment})
 */
public class QRGeneratorFragment extends Fragment {

    private static final String TAG = "QRGeneratorFragmentActivity";
    private TextView mQRGenerator_textView_description, mQRGenerator_textView_headline;
    private RelativeLayout mQRGenerator_realtiveLayout_textViewFrame;
    private Button mQRGenerator_button_shareWith, mQRGenerator_button_goToWhereComesFrom;
    private ImageView mQRGenerator_imageView_qrCode;
    private BitMatrix mBitmatrix;
    private Bitmap mBitmapOfQRCode;
    private Person mPerson;
    private StringBuffer mQRGenerator_StringBuffer_Result;
    private Event mEvent;

    public QRGeneratorFragment() {
        // Required empty public constructor
    }

    // Get the EventIdResultBundle (Long) from the newEventActivity to Start later a DB Request

    /**
     * Method to get the Id of the selected {@link Event} passed to this fragment from {@link MyOwnEventDetailsFragment}
     * or {@link AcceptedEventDetailsFragment} during the FragmentTransaction
     *
     * @return the Id of the selected event
     */
    @SuppressLint("LongLogTag")
    private Long eventIdDescription() {
        Bundle qrEventIdBundle = getArguments();
        assert qrEventIdBundle != null;
        return qrEventIdBundle.getLong("eventId");
    }


    /**
     * replaced the current fragment with {@link EventOverviewFragment} or {@link CalendarFragment}
     * depending on from where the user accessed this fragment
     */
    private void goWhereUserComesFrom() {
        Bundle whichFragment = getArguments();
        Objects.requireNonNull(getFragmentManager()).popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (Objects.requireNonNull(whichFragment).getString("fragment").equals("EventOverview")) {
            Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.eventOverview_frameLayout, new EventOverviewFragment(), "")
                    .commit();
        } else {
            Objects.requireNonNull(getActivity()).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.calendar_frameLayout, new CalendarFragment(), "")
                    .commit();
        }
    }

    /**
     * inflates the fragment_qrgenerator.xml layout, initializes the view variables and gets the selected
     * {@link Event} as well as the current user
     *
     * @param inflater           a LayoutInflater used for inflating layouts into views
     * @param container          the parent view of the fragment
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     * @return the view created from inflating the layout
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qrgenerator, container, false);

        //Initial GUI
        mQRGenerator_button_shareWith = view.findViewById(R.id.qrGenerator_button_shareWith);
        mQRGenerator_button_goToWhereComesFrom = view.findViewById(R.id.qrGenerator_button_goToWhereComesFrom);
        mQRGenerator_imageView_qrCode = view.findViewById(R.id.qrGenerator_imageView_qrCode);
        mQRGenerator_realtiveLayout_textViewFrame = view.findViewById(R.id.qrGenerator_relativeLayout_textViewFrame);
        mQRGenerator_textView_description = view.findViewById(R.id.qrGenerator_textView_description);
        mQRGenerator_textView_headline = view.findViewById(R.id.qrGenerator_textView_headline);
        //mQRGenerator_relativeLayout_buttonFrame = view.findViewById(R.id.qrGenerator_relativeLayout_buttonFrame);

        // Show always Scrollbar on Description TextView
        mQRGenerator_textView_description.setMovementMethod(new ScrollingMovementMethod());

        //Create Event form the DB with the EventId (eventIdResultBundle) to put it in a StringBuffer
        mEvent = EventController.getEventById(eventIdDescription());

        mPerson = PersonController.getPersonWhoIam();

        return view;
    }



    /**
     * generates a QR Code for the selected {@link Event}
     * and sets it as the value of an image view in the fragment
     */
    private void qrBitMapGenerator() {
        //Create a CorrectionLevelHashMap for the QRCode
        // Level of Correction: L = 7%, M = 15%, Q = 25%, H = 30% (max!)
        Map<EncodeHintType, Object> correctionLevel = new HashMap<>();
        correctionLevel.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        //Change the StringBuffer to a String for Output in the ImageView
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            mBitmatrix = multiFormatWriter.encode(EventController.createEventInvitation(mEvent),
                    BarcodeFormat.QR_CODE, 200, 200, correctionLevel);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            mBitmapOfQRCode = barcodeEncoder.createBitmap(mBitmatrix);
            mQRGenerator_imageView_qrCode.setImageBitmap(mBitmapOfQRCode);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    /**
     * generates the description of the {@link Event} and the QR Code
     * sets the OnClickListeners for the buttons, one for sharing the QR Code and another for going back to the
     * overview fragment the user accessed the current fragment from
     * @param view the view created in {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     */
    @SuppressLint("LongLogTag")
    @Override
    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        try {
            qrBitMapGenerator();
            // Event shortTitle in Headline with StartDate
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            String concat = mEvent.getShortTitle() + ", " + dateFormat.format(mEvent.getStartTime());
            mQRGenerator_textView_headline.setText(concat);
            // Check for a Repetition Event and Change the Description Output with and without
            // Repetition Element inside.
            mQRGenerator_textView_description.setText(EventController.createEventDescription(mEvent));

            // In the CatchBlock the User see a SnackBar Information and was pushed where the User Comes From
        } catch (NullPointerException e) {
            Log.d(TAG, "QRGeneratorFragmentActivity:" + e.getMessage());
            mQRGenerator_button_goToWhereComesFrom.setVisibility(View.GONE);
            mQRGenerator_button_shareWith.setVisibility(View.GONE);
            Snackbar.make(Objects.requireNonNull(getActivity()).findViewById(R.id.qrGenerator_relativeLayout_textViewFrame),
                    getString(R.string.ups_an_error),
                    Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.back), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    goWhereUserComesFrom();
                }
            }).show();

        } catch (ArrayIndexOutOfBoundsException z) {
            //If there is an Exception the Views change to Invisible and SnackBar tell that's anything wrong
            // and Push him back where the User come Frome
            Log.d(TAG, "QRGeneratorFragmentActivity:" + z.getMessage());
            mQRGenerator_textView_headline.setVisibility(View.GONE);
            mQRGenerator_button_shareWith.setVisibility(View.GONE);
            mQRGenerator_button_goToWhereComesFrom.setVisibility(View.GONE);

            Snackbar.make(Objects.requireNonNull(getActivity()).findViewById(R.id.qrGenerator_relativeLayout_textViewFrame),
                    getString(R.string.ups_an_error),
                    Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.back), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    goWhereUserComesFrom();
                }
            }).show();
        }

        //Push the User Back where he/she comes From
        mQRGenerator_button_goToWhereComesFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goWhereUserComesFrom();
            }
        });

        //Open a Chooser to Share the QR-Code over one of the User Apps
        mQRGenerator_button_shareWith.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onClick(View v) {
                try {
                    File cachePath = new File(Objects.requireNonNull(getContext()).getCacheDir(), "images");
                    cachePath.mkdirs(); // don't forget to make the directory
                    FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
                    mBitmapOfQRCode.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    stream.close();

                    File imagePath = new File(getContext().getCacheDir(), "images");
                    File newFile = new File(imagePath, "image.png");
                    Uri contentUri = FileProvider.getUriForFile(getContext(), "hft.wiinf.de.horario.fileprovider", newFile);

                    if (contentUri != null) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                        shareIntent.setDataAndType(contentUri, getContext().getContentResolver().getType(contentUri));
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)));
                    }
                } catch (IOException e) {
                    Log.d(TAG, "QRGeneratorFragmentActivity:" + e.getMessage());
                }
            }
        });
    }
}