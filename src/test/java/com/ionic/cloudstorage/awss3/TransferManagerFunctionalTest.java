/*
 * (c) 2019 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import static org.junit.Assert.*;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.ionic.sdk.error.IonicException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class TransferManagerFunctionalTest {

    private static IonicEncryptionMaterialsProvider iemp = null;
    private static IonicS3EncryptionClient ionicS3Client = null;
    private static AmazonS3 s3Client = null;
    private static TransferManager transferManager = null;

    private static String testBucket = null;

    @BeforeClass
    public static void setup() {
        if (TestUtils.awsCredsAvailable()) {
            try {
                iemp = TestUtils.getIEMP();
                ionicS3Client = (IonicS3EncryptionClient)IonicS3EncryptionClientBuilder.standard()
                    .withEncryptionMaterials(iemp).build();
                transferManager = TransferManagerBuilder.standard().withS3Client(ionicS3Client).build();
            } catch (IonicException e) {
                // Catch any IonicExceptions thrown during setup and null related objects so
                // that dependent tests are each skipped during the preconditions check.
                iemp = null;
                ionicS3Client = null;
                transferManager = null;
            }
            s3Client = AmazonS3ClientBuilder.defaultClient();
        }
        testBucket = TestUtils.getTestBucket();
    }

    @Before
    public void preconditions() {
        Assume.assumeNotNull(iemp);
        Assume.assumeNotNull(ionicS3Client);
        Assume.assumeNotNull(s3Client);
        Assume.assumeNotNull(transferManager);
        Assume.assumeNotNull(testBucket);
    }

    @Test
    public void uploadAndDownloadFile() throws IOException, InterruptedException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testUploadAndDownloadFile";
        }

        File sourceFile = TestUtils.getSourceFile();
        File destFile = TestUtils.getDestFile();
        Assume.assumeNotNull(sourceFile);
        Assume.assumeNotNull(destFile);

        Upload upload = transferManager.upload(testBucket, key, sourceFile);
        upload.waitForUploadResult();

        Download download = transferManager.download(testBucket, key, destFile);
        download.waitForCompletion();

        assertTrue("Downloaded File did not match original File",
            FileUtils.contentEquals(sourceFile, destFile));

        S3Object obj = s3Client.getObject(testBucket, key);

        assertFalse("Test file was not encrypted at rest",
            IOUtils.contentEquals(obj.getObjectContent(), FileUtils.openInputStream(sourceFile)));
    }

    @Test
    public void uploadAndDownloadFileComparison() throws IOException, InterruptedException {
        String key = TestUtils.getTestObjectKey();
        if (key == null) {
            key = "testUploadAndDownloadFileComparison";
        }

        byte[] rawBytes = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32};
        SecretKeySpec secretKey = new SecretKeySpec(rawBytes, "AES");
        EncryptionMaterials materials = new EncryptionMaterials(secretKey);
        StaticEncryptionMaterialsProvider provider = new StaticEncryptionMaterialsProvider(materials);
        AmazonS3 s3Client = AmazonS3EncryptionClientBuilder.standard().withEncryptionMaterials(provider).build();
        TransferManager s3transferManager = TransferManagerBuilder.standard().withS3Client(s3Client).build();

        File sourceFile = TestUtils.getSourceFile();
        File destFile = TestUtils.getDestFile();
        Assume.assumeNotNull(sourceFile);
        Assume.assumeNotNull(destFile);

        Upload upload = s3transferManager.upload(testBucket, key, sourceFile);
        upload.waitForUploadResult();

        Download download = s3transferManager.download(testBucket, key, destFile);
        download.waitForCompletion();

        assertTrue("Downloaded File did not match original File",
            FileUtils.contentEquals(sourceFile, destFile));

        S3Object obj = s3Client.getObject(testBucket, key);

        assertTrue("File was not encrypted at rest",
            IOUtils.contentEquals(obj.getObjectContent(), FileUtils.openInputStream(sourceFile)));
    }

    @Test
    public void uploadAndDownloadDirectory() throws IOException, InterruptedException {
        String bucketDirectory = TestUtils.getTestObjectKey();
        if (bucketDirectory == null) {
            bucketDirectory = "testUploadAndDownloadDirectory";
        }

        File sourceDir = TestUtils.getSourceDirectory();
        File destDir = TestUtils.getDestDirectory();
        Assume.assumeNotNull(sourceDir);
        Assume.assumeNotNull(destDir);

        MultipleFileUpload upload = transferManager.uploadDirectory(testBucket, bucketDirectory, sourceDir, true);
        upload.waitForCompletion();

        MultipleFileDownload download = transferManager.downloadDirectory(testBucket, bucketDirectory, destDir);
        download.waitForCompletion();

        assertTrue("Downloaded directory contents did not match source directory",
            TestUtils.directoryContentsMatch(sourceDir, new File(destDir, bucketDirectory), true));
    }

    @Test
    public void uploadFileList() throws IOException, InterruptedException {
        String bucketDirectory = TestUtils.getTestObjectKey();
        if (bucketDirectory == null) {
            bucketDirectory = "testUploadFileList";
        }

        File sourceDir = TestUtils.getSourceDirectory();
        File destDir = TestUtils.getDestDirectory();
        Assume.assumeNotNull(sourceDir);
        Assume.assumeNotNull(destDir);

        List<File> fileList = Arrays.asList(sourceDir.listFiles());

        MultipleFileUpload upload = transferManager
            .uploadFileList(testBucket, bucketDirectory, sourceDir, fileList);
        upload.waitForCompletion();

        MultipleFileDownload download = transferManager.downloadDirectory(testBucket, bucketDirectory, destDir);
        download.waitForCompletion();

        assertTrue("Downloaded directory contents did not match source directory",
            TestUtils.directoryContentsMatch(sourceDir, new File(destDir, bucketDirectory), false));
    }
}
