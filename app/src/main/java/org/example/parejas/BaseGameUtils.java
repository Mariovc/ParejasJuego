package org.example.parejas;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.IntentSender;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.GamesActivityResultCodes;

/**
 * Author: Mario Velasco Casquero
 * Email: m3ario@gmail.com
 */
public class BaseGameUtils {
    public static void showAlert(Activity activity, String message) {
        (new AlertDialog.Builder(activity)).setMessage(message)
                .setNeutralButton(android.R.string.ok, null).create().show();
    }
    public static boolean resolveConnectionFailure(Activity activity,
                                                   GoogleApiClient client, ConnectionResult result, int requestCode,
                                                   String fallbackErrorMessage) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(activity, requestCode);
                return true;
            } catch (IntentSender.SendIntentException e) {
                client.connect();
                return false;
            }
        } else {
            int errorCode = result.getErrorCode();
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
                    activity, requestCode);
            if (dialog != null) {
                dialog.show();
            } else {
                showAlert(activity, fallbackErrorMessage);
            }
            return false;
        }
    }
    public static void showActivityResultError(Activity activity,
                                               int requestCode, int actResp, int errorDescription) {
        if (activity == null) {
            return;
        }
        Dialog errorDialog;
        switch (actResp) {
            case GamesActivityResultCodes.RESULT_APP_MISCONFIGURED:
                errorDialog = makeSimpleDialog(activity,
                        activity.getString(R.string.app_misconfigured));
                break;
            case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED:
                errorDialog = makeSimpleDialog(activity,
                        activity.getString(R.string.sign_in_failed));
                break;
            case GamesActivityResultCodes.RESULT_LICENSE_FAILED:
                errorDialog = makeSimpleDialog(activity,
                        activity.getString(R.string.license_failed));
                break;
            default:
                final int errorCode =
                        GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
                errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
                        activity, requestCode, null);
                if (errorDialog == null) {
                    errorDialog = makeSimpleDialog(activity,
                            activity.getString(errorDescription));
                }
        }
        errorDialog.show();
    }
    public static Dialog makeSimpleDialog(Activity activity, String text) {
        return (new AlertDialog.Builder(activity)).setMessage(text)
                .setNeutralButton(android.R.string.ok, null).create();
    }
    public static Dialog makeSimpleDialog(Activity activity,
                                          String title, String text) {
        return (new AlertDialog.Builder(activity))
                .setTitle(title).setMessage(text)
                .setNeutralButton(android.R.string.ok, null)
                .create();
    }
}
