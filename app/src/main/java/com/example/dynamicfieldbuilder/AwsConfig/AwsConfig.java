package com.example.dynamicfieldbuilder.AwsConfig;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;

import java.io.File;

public class AwsConfig {
    AmazonS3 s3Client;
    final static String bucket = "bucketmobile";
    private TransferUtility transferUtility;
    private TextView file_url;
    final static String CognitoPoolID = "xxxxxxxxxxxxxxxxxxxxxx";
    Context mContext;

    public TransferUtility getTransferUtility() {
        return transferUtility;
    }

    public AwsConfig(Context mContext){
        this.mContext=mContext;
        s3credentialsProvider();
        // callback method to call the setTransferUtility method
        setTransferUtility();
    }

    public void s3credentialsProvider() {
        // Initialize the AWS Credential
        CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider =
                new CognitoCachingCredentialsProvider(
                        mContext,
                        CognitoPoolID, // Identity Pool ID
                        Regions.AP_SOUTH_1
                        // Region
                );
        createAmazonS3Client(cognitoCachingCredentialsProvider);
    }

    public void createAmazonS3Client(CognitoCachingCredentialsProvider
                                             credentialsProvider) {

        // Create an S3 client
        s3Client = new AmazonS3Client(credentialsProvider);

        // Set the region of your S3 bucket
        s3Client.setRegion(Region.getRegion(Regions.AP_SOUTH_1));
    }



    public void setTransferUtility() {
        transferUtility = new TransferUtility(s3Client,
                mContext);
    }


    public TransferObserver getTransferObjectForLisener(String imagePath,String filename) {
        TransferNetworkLossHandler.getInstance(mContext);
        Log.e("Store", imagePath);
        TransferObserver transferObserver = transferUtility.upload(
                bucket,          /* The bucket to upload to */
                filename,
                new File(imagePath)/* The key for the uploaded object */
                /* The file where the data to upload exists */
                , CannedAccessControlList.PublicRead
        );

        return transferObserver;
    }
}
