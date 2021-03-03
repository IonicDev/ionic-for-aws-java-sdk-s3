/*
 * (c) 2019-2021 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.AgentErrorModuleConstants;
import com.ionic.sdk.error.IonicException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class TestUtils {

    static Logger log = LogManager.getLogger();

    protected static String testBucketEnv = "IONIC_S3_TEST_BUCKET";
    protected static String testBucketProp = "testBucket";
    protected static String testObjectKeyProp = "objectKey";
    protected static String testPayloadStringProp = "payloadString";
    protected static String testPersistorProp = "persistorPath";
    protected static String testFileSourceProp = "fileSource";
    protected static String testFileDestProp = "fileDest";
    protected static String testDirSourceProp = "dirSource";
    protected static String testDirDestProp = "dirDest";
    protected static String testPolicyDeniedProp = "policyDenied";
    protected static String testRetriesCountProp = "retriesCount";
    protected static String testRetriesDelayProp = "retriesDelay";

    protected static String defaultPayload = "Hello World.";

    protected static String getTestBucket() {
        String bucket = System.getProperty(testBucketProp);
        if (bucket == null) {
            bucket = System.getenv(testBucketEnv);
        }
        return bucket;
    }

    protected static String getTestPayload() {
        String string = System.getProperty(testPayloadStringProp);
        if (string == null) {
            string = defaultPayload;
        }
        return string;
    }

    protected static String getTestObjectKey() {
        return System.getProperty(testObjectKeyProp);
    }

    protected static File getSourceFile() {
        File file = null;
        String filepath = System.getProperty(testFileSourceProp);
        if (filepath != null) {
            file = new File(filepath);
            if (file.exists() == false) {
                return null;
            }
        }
        return file;
    }

    protected static File getDestFile() {
        File file = null;
        String filepath = System.getProperty(testFileDestProp);
        if (filepath != null) {
            file = new File(filepath);
        }
        return file;
    }

    protected static File getSourceDirectory() {
        File file = null;
        String filepath = System.getProperty(testDirSourceProp);
        if (filepath != null) {
            file = new File(filepath);
            if (! (file.isDirectory() && file.exists())) {
                return null;
            }
        }
        return file;
    }

    protected static File getDestDirectory() {
        File file = null;
        String filepath = System.getProperty(testDirDestProp);
        if (filepath != null) {
            file = new File(filepath);
        }
        return file;
    }

    protected static int getRetriesCount() {
        String retries = System.getProperty(testRetriesCountProp);
        if (retries != null) {
            return Integer.parseInt(retries);
        } else {
            return 0;
        }
    }

    protected static int getRetryDelay() {
        String delay = System.getProperty(testRetriesDelayProp);
        if (delay != null) {
            return Integer.parseInt(delay);
        } else {
            return 0;
        }
    }

    protected static DeviceProfilePersistorPlainText getPersistor() throws IonicException {
        DeviceProfilePersistorPlainText ptPersistor = null;
        log.info("Attempting to fetch persistor path from properties");
        String ptPersitorPath = System.getProperty(testPersistorProp);
        if (ptPersitorPath == null) {
            log.info("Attempting to load persistor from default location");
            ptPersitorPath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";
        }
        if (Files.exists(Paths.get(ptPersitorPath))) {
            return new DeviceProfilePersistorPlainText(ptPersitorPath);
        } else {
            log.error("Failed to load persistor from " + ptPersitorPath);
            throw new IonicException(AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE);
        }
    }

    protected static Agent getAgent() throws IonicException {
        log.info("Constructing Ionic Agent with Persisor");
        Agent agent = new Agent();
        agent.initialize(getPersistor());
        return agent;
    }

    protected static IonicEncryptionMaterialsProvider getIEMP() throws IonicException {
        log.info("Constructing Ionic Encryption Materials Provider with Persistor");
        return new IonicEncryptionMaterialsProvider(getPersistor());
    }

    protected static Boolean awsCredsAvailable() {
        log.info("Checking availability of AWS Credentials");
        try {
            DefaultAWSCredentialsProviderChain.getInstance().getCredentials();
            new DefaultAwsRegionProviderChain().getRegion();
        } catch (SdkClientException e) {
            log.warn("AWS Credentials not available: " + e.getMessage());
            return false;
        }
        return true;
    }

    protected static Boolean directoryContentsMatch(File a, File b, Boolean recurisve) throws IOException {
        if (!(a.exists() && a.isDirectory() && b.exists() && b.isDirectory())){
            return false;
        }

        HashMap<String,File> aFileMap = fileMapFromArray(a.listFiles());
        HashMap<String,File> bFileMap = fileMapFromArray(b.listFiles());

        HashSet<String> fileNames = new HashSet<String>();
        fileNames.addAll(aFileMap.keySet());
        fileNames.addAll(bFileMap.keySet());

        for (String key : fileNames) {
            if (recurisve) {
                if ((aFileMap.containsKey(key) && bFileMap.containsKey(key)) == false) {
                    return false;
                }
                if (aFileMap.get(key).isFile()) {
                    if (FileUtils.contentEquals(aFileMap.get(key), bFileMap.get(key)) == false) {
                        return false;
                    }
                } else if (aFileMap.get(key).isDirectory()) {
                    if (directoryContentsMatch(aFileMap.get(key), bFileMap.get(key), true) == false) {
                        return false;
                    }
                } else {
                    return false;
                }

            } else {
                if (aFileMap.containsKey(key) && aFileMap.get(key).isFile()) {
                    if (bFileMap.containsKey(key) == false) {
                        return false;
                    } else if (FileUtils.contentEquals(aFileMap.get(key), bFileMap.get(key)) == false) {
                        return false;
                    }
                } else if (bFileMap.containsKey(key) && bFileMap.get(key).isFile()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static HashMap<String,File> fileMapFromArray(File[] fileArray) {
        HashMap<String,File> map = new HashMap<String,File>();
            for (int i = 0; i < fileArray.length; i++) {
                map.put(fileArray[i].getName(), fileArray[i]);
            }
        return map;
    }

    protected static String getTestDirectoryString(String testMethodName) {
        File file = FileSystems.getDefault().getPath("." + File.separator + testMethodName)
                .toAbsolutePath().toFile();
        file.mkdirs();
        return file.toString();
    }

    protected static File generateTestFile(String directory, String fileName, int sizeInMb)
            throws FileNotFoundException, IOException {
        String fullPath = directory + File.separator + fileName;
        File file = new File(fullPath);
        file.createNewFile();
        if (sizeInMb > 0) {
            long newSize = sizeInMb * 1024 * 1024;
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            try
            {
                raf.setLength(newSize);
            }
            finally
            {
                raf.close();
            }
        }
        return file;
    }

}
