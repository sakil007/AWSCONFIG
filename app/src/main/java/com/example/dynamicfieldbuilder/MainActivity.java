package com.example.dynamicfieldbuilder;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.Preference;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.example.dynamicfieldbuilder.AwsConfig.AwsConfig;
import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.utils.ImageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.amazonaws.mobileconnectors.s3.transferutility.TransferType.UPLOAD;


public class MainActivity extends AppCompatActivity implements TransferListener{
    private static final int REQUEST_CODE_GET_JSON = 101;
    private static final int RESULT_LOAD_IMG_fromCamera = 111;
    private static final int RESULT_LOAD_IMG = 112;
    Context mContext;
    private Uri uriSavedImage;
    Button clickImage,uploadImage,resume;
    AmazonS3 s3Client;
    String bucket = "bucketmobile";
    TransferUtility transferUtility;
    private String imagePath;
    private String filename;
    private TextView file_url;
    private TransferObserver transferObserver;
    AwsConfig awsConfig;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // callback method to call credentialsProvider method.

        // callback method to call the setTransferUtility method
        mContext=this;
        /*Intent intent = new Intent(mContext, JsonFormActivity.class);
        intent.putExtra("json",loadJSONFromAsset());
        startActivityForResult(intent, REQUEST_CODE_GET_JSON);*/
        clickImage=findViewById(R.id.click);
        uploadImage=findViewById(R.id.upload);
        resume=findViewById(R.id.resume);
        file_url=findViewById(R.id.file_url);
        awsConfig();
        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));

        clickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageSelection();
            }
        });

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImageToAws();

            }
        });

        resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<TransferObserver> observerList = awsConfig.getTransferUtility().getTransfersWithType(UPLOAD);
               // Log.e("", "Upload transfers count: " + observerList.size());
                if(observerList.size()>0)
                    awsConfig.getTransferUtility().resume(observerList.get(0).getId()); //resume any uploads that were paused on app termination
            }
        });
    }

    private void uploadImageToAws() {
        transferObserver= awsConfig.getTransferObjectForLisener(imagePath,filename);
        transferObserver.setTransferListener(this);
    }

    private void awsConfig() {
        awsConfig=new AwsConfig(this);
    }

    private void imageSelection() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery","Capture Video", "Cancel"};
        //final CharSequence[] options = {"Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo!");
        builder.setCancelable(false);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    openImageChooser();
                } else if (options[item].equals("Capture Video")) {
                    openVideoCapture();
                } else if (options[item].equals("Choose from Gallery")) {
                    dialog.dismiss();
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    galleryIntent.setType("*/*");
                    galleryIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "video/*"});
                   startActivityForResult(galleryIntent, RESULT_LOAD_IMG);

                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }
    void openVideoCapture() {

        Intent takeVideoFromCameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(takeVideoFromCameraIntent, 12);
           // takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoMediaFile));

    }

    void openImageChooser() {

        File imagesFolder = new File(Environment.getExternalStorageDirectory(), "SoulAce");
        // <----
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs();
        }
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName = "SoulAce" + timeStamp + ".jpg";
        mediaFile = new File(imagesFolder.getPath() + File.separator + mImageName);
        uriSavedImage = Uri.fromFile(mediaFile);
        Log.e("TAG", uriSavedImage.getPath());
        /*Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        camera.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);*/
        //= new Intent(android.provider.MediaStore.ACTION_
        // IMAGE_CAPTURE);
        //  startActivityForResult(camera, 1);


        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
        } else {
            File file = new File(uriSavedImage.getPath());
            Uri photoUri = FileProvider.getUriForFile(this, this.getPackageName() + ".provider", file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", PICK_IMAGE_CAMERA);
        } else {
            intent.putExtra("android.intent.extras.CAMERA_FACING", PICK_IMAGE_CAMERA);
        }*/
        if (intent.resolveActivity(this.getPackageManager()) != null) {
            startActivityForResult(intent, RESULT_LOAD_IMG_fromCamera);
        }
    }


    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = mContext.getAssets().open("Pattern.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       if (requestCode == REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
           Log.d("TAG", data.getStringExtra("json"));
       }
       else if (requestCode == 12 && resultCode == Activity.RESULT_OK && null != data){

           try {
               File newfile;
               AssetFileDescriptor videoAsset = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
               FileInputStream in = videoAsset.createInputStream();

               File filepath = Environment.getExternalStorageDirectory();
               File dir = new File(filepath.getAbsolutePath() + "/" +"soulace" + "/");
               if (!dir.exists()) {
                   dir.mkdirs();
               }

               newfile = new File(dir, "SoulAce"+System.currentTimeMillis()+".mp4");

               if (newfile.exists()) newfile.delete();

               OutputStream out = new FileOutputStream(newfile);

               // Copy the bits from instream to outstream
               byte[] buf = new byte[1024];
               int len;

               while ((len = in.read(buf)) > 0) {
                   out.write(buf, 0, len);
               }
               in.close();
               out.close();
               Log.v("", "Copy file successful.");
               imagePath = newfile.getPath();
               filename=imagePath.substring(imagePath.lastIndexOf("/")+1);
           } catch (Exception e) {
               e.printStackTrace();
           }


       }
       else if (requestCode == RESULT_LOAD_IMG && resultCode == Activity.RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            // No need for null check on cursor
            Cursor cursor = this.getContentResolver()
                    .query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            imagePath = cursor.getString(columnIndex);
            filename=imagePath.substring(imagePath.lastIndexOf("/")+1);
            Log.e("TAG",imagePath);
            cursor.close();
        }
        if (requestCode == RESULT_LOAD_IMG_fromCamera && resultCode == Activity.RESULT_OK && null != data) {
            Uri selectedImage = uriSavedImage;
            imagePath = selectedImage.getPath();
            filename=imagePath.substring(imagePath.lastIndexOf("/")+1);
            Log.e("TAG",imagePath);
            // cursor.close();
        }
        file_url.setText("File Url: "+filename );
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onStateChanged(int id, TransferState state) {
        Toast.makeText(getApplicationContext(), "State Change " + state,
                Toast.LENGTH_SHORT).show();

        if (state == TransferState.COMPLETED) {
            String url = "https://"+bucket+".s3.amazonaws.com/" + transferObserver.getKey();
            Log.e("URL :", url);
            transferObserver.cleanTransferListener();
//we just need to share this File url with Api service request.
        }
        //   Log.e("status=", "fsf"+transferObserver.getKey()+" sdf ="+bytesCurrent);
    }

    @Override
    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
        //float percentage = (float) (bytesCurrent / bytesTotal * 100);
        float bytesMax=(float)bytesTotal / (1024 * 1024);
        DecimalFormat df = new DecimalFormat("####0.00");

        Toast.makeText(getApplicationContext(), "Progressing " +df.format(bytesMax)+ " / "+"" +df.format((float)bytesCurrent / (1024 * 1024)),
                Toast.LENGTH_SHORT).show();
        Log.e("status=", "total"+bytesTotal+" current ="+bytesCurrent);
    }

    @Override
    public void onError(int id, Exception ex) {
        Log.e("error", "error");
        transferObserver.cleanTransferListener();
    }

}
