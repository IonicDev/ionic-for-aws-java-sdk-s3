/*
 * (c) 2018 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.awss3;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class TransferManagerTest {

    static String bucketName;

    static {
        AmazonIdentityManagementClient idtyClient = 
                (AmazonIdentityManagementClient) AmazonIdentityManagementClientBuilder.defaultClient();
        bucketName = idtyClient.getUser().getUser().getUserName().toLowerCase() + "-ipcstrials";
    }

    @Test
    public void testBasicTransferRoundTrip()
            throws AmazonClientException, InterruptedException, IOException, URISyntaxException {
        URL url = this.getClass().getResource("/testFile.txt");
        URI uri = new URI(url.toString());
        File uploadFile = new File(uri.getPath());
        assertTrue(roundTrip(uploadFile));
    }

    @Test
    public void testMultipartTransferRoundTrip()
            throws FileNotFoundException, IOException,
            AmazonClientException, InterruptedException, URISyntaxException {
        URL url = this.getClass().getResource("/largeFile.txt");
        URI uri = new URI(url.toString());
        File largeFile = new File(uri.getPath());

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(largeFile)))) {
            for (int i = 0; i < 15000; i++) {
                writer.write("Repeating ");
            }
        }

        assertTrue(roundTrip(largeFile));

        PrintWriter writer = new PrintWriter(largeFile);
        writer.print("");
        writer.close();
    }

    public boolean roundTrip(File uploadFile)
            throws IOException, AmazonClientException, InterruptedException, URISyntaxException {

        TransferManager xfrmngr = TransferManagerBuilder.standard()
                .withS3Client(IonicS3EncryptionClientBuilder.defaultClient()).build();

        try {
            Upload xfer = xfrmngr.upload(bucketName, "transferTest1", uploadFile);
            xfer.waitForCompletion();
        } catch (AmazonServiceException e) {
            fail("Transfer Manager failed an upload: " + e.getErrorMessage());
        }

        URL url = this.getClass().getResource("/transferTarget1.txt");
        URI uri = new URI(url.toString());
        File downloadTarget = new File(uri.getPath());

        try {
            Download xfer = xfrmngr.download(bucketName, "transferTest1", downloadTarget);
            xfer.waitForCompletion();
        } catch (AmazonServiceException e) {
            fail("Transfer Manager failed an download: " + e.getErrorMessage());
        }
        xfrmngr.shutdownNow();

        InputStream originalFileStream = new FileInputStream(uploadFile);
        InputStream downloadStream = new FileInputStream(downloadTarget);
        boolean result = IOUtils.contentEquals(originalFileStream, downloadStream);

        PrintWriter writer = new PrintWriter(downloadTarget);
        writer.print("");
        writer.close();
        return result;
    }
}
