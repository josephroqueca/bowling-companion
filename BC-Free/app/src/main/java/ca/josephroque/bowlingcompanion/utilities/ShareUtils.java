package ca.josephroque.bowlingcompanion.utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

import ca.josephroque.bowlingcompanion.R;

/**
 * Created by josephroque on 15-03-26.
 * <p/>
 * Location ca.josephroque.bowlingcompanion.utilities
 * in project Bowling Companion
 */
public class ShareUtils
{

    private static final String TAG = "ShareUtils";

    public static void showShareDialog(final Activity activity, final long seriesId)
    {
        final CharSequence[] options = {"Save", "Share"};
        AlertDialog.Builder shareBuilder = new AlertDialog.Builder(activity);
        shareBuilder.setTitle("Save to device or share?")
                .setSingleChoiceItems(options, 0, null)
                .setPositiveButton(R.string.dialog_okay, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        int selectedItem = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                        if (selectedItem == 0)
                            saveSeriesToDevice(activity, seriesId);
                        else
                            shareSeries(activity, seriesId);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    private static void shareSeries(Activity activity, long seriesId)
    {
        new ShareSeriesTask().execute(activity, seriesId);
    }

    private static void saveSeriesToDevice(final Activity activity, final long seriesId)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                final Uri imageUri = ImageUtils.insertImage(activity.getContentResolver(),
                        ImageUtils.createImageFromSeries(activity, seriesId),
                        String.valueOf(System.currentTimeMillis()),
                        "Series: " + seriesId);

                activity.runOnUiThread(new Runnable()
                {
                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public void run()
                    {
                        MediaScannerConnection.scanFile(activity,
                                new String[]{imageUri.getPath()}, null,
                                new MediaScannerConnection.OnScanCompletedListener()
                                {
                                    public void onScanCompleted(String path, Uri uri)
                                    {
                                        Log.i("ExternalStorage", "Scanned " + path + ":");
                                        Log.i("ExternalStorage", "-> uri=" + uri);
                                    }
                                });

                        Toast toast;
                        if (imageUri != null)
                            toast = Toast.makeText(activity, "Image successfully saved!", Toast.LENGTH_SHORT);
                        else
                            toast = Toast.makeText(activity, "Unable to save image", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
            }
        }).start();
    }

    private static class ShareSeriesTask extends AsyncTask<Object, Void, Object[]>
    {
        @Override
        public Object[] doInBackground(Object... params)
        {
            Activity activity = (Activity)params[0];
            long seriesId = (Long)params[1];
            Bitmap image = ImageUtils.createImageFromSeries(activity, seriesId);
            Uri imageUri = ImageUtils.insertImage(activity.getContentResolver(),
                    image,
                    String.valueOf(System.currentTimeMillis()),
                    "Series: " + seriesId);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            OutputStream outStream = null;
            try
            {
                outStream = activity.getContentResolver()
                        .openOutputStream(imageUri);
                image.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            }
            catch (Exception e)
            {
                Log.w(TAG, "Unable to create stream from image");
            }
            finally
            {
                if (outStream != null)
                {
                    try
                    {
                        outStream.close();
                    }
                    catch (IOException ex)
                    {
                        Log.w(TAG, "Error closing outStream");
                    }
                }
            }

            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            return new Object[]{activity, shareIntent};
        }

        @Override
        public void onPostExecute(Object[] params)
        {
            Activity activity = (Activity)params[0];
            Intent shareIntent = (Intent)params[1];

            activity.startActivity(Intent.createChooser(shareIntent, "Share Image"));
        }
    }
}