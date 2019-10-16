package hft.wiinf.de.horario.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Objects;

import hft.wiinf.de.horario.CaptureActivityPortrait;
import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.NoScanResultExceptionController;
import hft.wiinf.de.horario.controller.ScanResultReceiverController;

/**
 * fragment that starts the camera and is used for scanning QR Codes
 */
public class QRScanFragment extends Fragment implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "QRScanFragmentActivity";
    private static final int PERMISSION_REQUEST_CAMERA = 1;
    private static final int SEND_SMS_PERMISSION_CODE = 2;
    private final String noResultErrorMsg = "No scan data received!";
    //Counter for the Loop of PermissionChecks
    private int counterSMS = 0, counterCAM = 0;
    private String whichFragment, codeContent;
    private Activity mActivity;

    /**
     * this is called after the Activity is created
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    //The Scanner start with the Call form CalendarActivity directly

    /**
     * inflates the fragment_calendar_qrscan.xml layout
     *
     * @param inflater          a LayoutInflater used for inflating layouts into views
     * @param container         the parent view of the fragment
     * @param saveInstanceState the saved state of the fragment from before a system event changed it
     * @return the view created from inflating the layout
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle saveInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_qrscan, container, false);
    }

    /**
     * after the fragment's views have been created check if the user has given the app permission
     * to send SMS
     * @param view the view created in {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param savedInstanceState the saved state of the fragment from before a system event changed it
     */
    @SuppressLint("ResourceType")
    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        //Call a Method to start at first a permission Check and if this granted it start the Scanner
        //in FullScreenMode
        checkForSMSPermission();
    }

    /**
     * checks if the user has given the app permission to send SMS
     * if not asks for permission, else shows the camera used for scanning
     */
    private void checkForSMSPermission() {
        if (!isSendSmsPermissionGranted()) {
            requestSendSmsPermission();
        } else {
            counterSMS = 5;
            showCameraPreview();
        }
    }

    /**
     * starts a QR Code scan
     * the result of the scan is handled in {@link #onActivityResult(int, int, Intent)}
     */
    private void startScanner() {
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setCaptureActivity(CaptureActivityPortrait.class); //Necessary to use the intern Sensor for Orientation
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt(getString(R.string.scannerOverlayer_qrCodeScan) + "\n" +
                getString(R.string.scannerOverlay_positionYourScanner) + "\n" +
                getString(R.string.scannerOverlay_toShowTheEvent));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    /**
     * checks if the user has granted the app permission to use the camera,
     * if not asks for permission, else starts the QR Code scan
     */
    private void showCameraPreview() {
        //Check if User has permission to start to scan, if not it's start a RequestLoop
        if (!isCameraPermissionGranted()) {
            requestCameraPermission();
        } else {
            startScanner();
        }
    }

    /**
     * checks if the user has granted the app permission to use the camera
     * @return true if permission is granted, false if it is not
     */
    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * requests permission to use the camera
     */
    private void requestCameraPermission() {
        //For Fragment: requestPermissions(permissionsList,REQUEST_CODE);
        //For Activity: ActivityCompat.requestPermissions(this,permissionsList,REQUEST_CODE);
        requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
    }

    /**
     * checks if the user has granted the app permission to use send SMS
     * @return true if permission is granted, false if it is not
     */
    private boolean isSendSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission(mActivity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * requests permission to send SMS
     */
    private void requestSendSmsPermission() {
        //For Fragment: requestPermissions(permissionsList,REQUEST_CODE);
        //For Activity: ActivityCompat.requestPermissions(this,permissionsList,REQUEST_CODE);
        requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION_CODE);
    }

    /**
     * handles the result of permission requests
     * if the user denied the request and checked the "never ask again" box or has already been asked twice unsuccessfully
     * the user is sent back to their last overview fragment. If the permission was granted proceeds to attempt to show the QR scanner
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA: {
                // for each permission check if the user granted/denied them you may want to group the
                // rationale in a single dialog,this is just an example
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        // user rejected the permission
                        boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA);
                        if (!showRationale) {
                            // user also CHECKED "never ask again" you can either enable some fall back,
                            // disable features of your app or open another dialog explaining again the
                            // permission and directing to the app setting

                            new AlertDialog.Builder(mActivity)
                                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                goWhereUserComesFrom();
                                                dialog.cancel();
                                                return true;
                                            }
                                            return false;
                                        }
                                    })
                                    .setTitle(R.string.accessWith_NeverAskAgain_deny)
                                    .setMessage(R.string.requestPermission_accessDenied_withCheckbox)
                                    .setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            goWhereUserComesFrom();
                                        }
                                    })
                                    .create().show();
                        } else if (counterCAM < 1) {
                            // user did NOT check "never ask again" this is a good place to explain the user
                            // why you need the permission and ask if he wants // to accept it (the rationale)
                            new AlertDialog.Builder(mActivity)
                                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                goWhereUserComesFrom();
                                                dialog.cancel();
                                                return true;
                                            }
                                            return false;
                                        }
                                    })
                                    .setTitle(R.string.requestPermission_firstTryRequest)
                                    .setMessage(R.string.requestPermission_askForPermission)
                                    .setPositiveButton(R.string.requestPermission_againButton, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            counterCAM++;
                                            showCameraPreview();
                                        }
                                    })
                                    .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            goWhereUserComesFrom();
                                        }
                                    })
                                    .create().show();
                        } else if (counterCAM == 1) {
                            new AlertDialog.Builder(mActivity)
                                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                        @Override
                                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                goWhereUserComesFrom();
                                                dialog.cancel();
                                                return true;
                                            }
                                            return false;
                                        }
                                    })
                                    .setTitle(R.string.requestPermission_lastTryRequest)
                                    .setMessage(R.string.requestPermission_askForPermission)
                                    .setPositiveButton(R.string.requestPermission_againButton, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            counterCAM++;
                                            showCameraPreview();
                                        }
                                    })
                                    .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            goWhereUserComesFrom();
                                        }
                                    })
                                    .create().show();
                        } else {
                            goWhereUserComesFrom();
                        }
                    } else {
                        startScanner();
                    }
                }
            }
            case SEND_SMS_PERMISSION_CODE: {
                if (counterSMS != 5) {
                    // for each permission check if the user granted/denied them you may want to group the
                    // rationale in a single dialog,this is just an example
                    for (int i = 0, len = permissions.length; i < len; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            // user rejected the permission
                            boolean showRationale;
                            if (counterSMS == 5) {
                                showRationale = true;
                            } else {
                                showRationale = shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS);
                            }
                            if (!showRationale) {
                                // user also CHECKED "never ask again" you can either enable some fall back,
                                // disable features of your app or open another dialog explaining again the
                                // permission and directing to the app setting
                                new AlertDialog.Builder(mActivity)
                                        .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                            @Override
                                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                    getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                                    goWhereUserComesFrom();
                                                    dialog.cancel();
                                                    return true;
                                                }
                                                return false;
                                            }
                                        })
                                        .setTitle(R.string.accessWith_NeverAskAgain_deny)
                                        .setMessage(R.string.requestPermission_accessDenied_withCheckbox_SendSMS)
                                        .setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                goWhereUserComesFrom();
                                            }
                                        })
                                        .create().show();
                            } else if (counterSMS < 1) {
                                // user did NOT check "never ask again" this is a good place to explain the user
                                // why you need the permission and ask if he wants // to accept it (the rationale)
                                new AlertDialog.Builder(mActivity)
                                        .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                            @Override
                                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                    getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                                    goWhereUserComesFrom();
                                                    dialog.cancel();
                                                    return true;
                                                }
                                                return false;
                                            }
                                        })
                                        .setTitle(R.string.requestPermission_firstTryRequest)
                                        .setMessage(R.string.requestPermission_askForPermission_sendSMS)
                                        .setPositiveButton(R.string.requestPermission_againButton, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                counterSMS++;
                                                checkForSMSPermission();
                                            }
                                        })
                                        .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                goWhereUserComesFrom();
                                            }
                                        })
                                        .create().show();
                            } else if (counterSMS == 1) {
                                new AlertDialog.Builder(mActivity)
                                        .setOnKeyListener(new DialogInterface.OnKeyListener() {
                                            @Override
                                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                                    getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                                    goWhereUserComesFrom();
                                                    dialog.cancel();
                                                    return true;
                                                }
                                                return false;
                                            }
                                        })
                                        .setTitle(R.string.requestPermission_lastTryRequest)
                                        .setMessage(R.string.requestPermission_askForPermission_sendSMS)
                                        .setPositiveButton(R.string.requestPermission_againButton, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                counterSMS++;
                                                checkForSMSPermission();
                                            }
                                        })
                                        .setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                goWhereUserComesFrom();
                                            }
                                        })
                                        .create().show();
                            } else {
                                goWhereUserComesFrom();
                            }
                        } else {
                            counterSMS = 5;
                            showCameraPreview();
                        }
                    }
                }
            }
        }
    }


    // Push the User where he/she comes from

    /**
     * returns to {@link CalendarFragment} or {@link EventOverviewFragment} depending on where the user accessed the QR scanner from
     */
    private void goWhereUserComesFrom() {
        Bundle whichFragment = getArguments();
        if (getActivity() != null) {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            if (whichFragment.getString("fragment").equals("EventOverview")) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.eventOverview_frameLayout, new EventOverviewFragment(), "")
                        .commit();
            } else {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.calendar_frameLayout, new CalendarFragment(), "")
                        .commit();
            }
        }
    }

    /**
     * once the fragment is attached to its parent if that parent is an activity it is saved in a variable
     * @param context the parent activity the fragment is getting attached to
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }
    }


    //Check the Scanner Result

    /**
     * processes the result of the scan and calls {@link hft.wiinf.de.horario.TabActivity#scanResultData(String, String)}
     * to process the content
     * @param requestCode The integer request code originally supplied to
     * startActivityForResult()
     * @param resultCode The integer result code returned by the scanner activity through its setResult().
     * @param intent An Intent, which can return result data to the caller
     * (various data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        ScanResultReceiverController parentActivity = (ScanResultReceiverController) this.getActivity();

        // give with the ScanResult where User Comes From
        String whichFragmentTag;
        Bundle whichFragment = getArguments();
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (whichFragment.getString("fragment").equals("EventOverview")) {
            whichFragmentTag = "EventOverview";
        } else {
            whichFragmentTag = "Calendar";
        }

        if (scanningResult != null) {
            //we have a result

            codeContent = scanningResult.getContents();
            this.whichFragment = whichFragmentTag;
            // send received data
            Objects.requireNonNull(parentActivity).scanResultData(this.whichFragment, codeContent);

        } else {
            // send exception
            Objects.requireNonNull(parentActivity).scanResultData(new NoScanResultExceptionController(noResultErrorMsg));
        }
    }
}