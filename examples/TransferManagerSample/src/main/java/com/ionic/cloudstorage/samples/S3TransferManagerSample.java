/*
 * (c) 2017-2020 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as
 * the Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.samples;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.MultipleFileDownload;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.ionic.cloudstorage.awss3.IonicEncryptionMaterialsProvider;
import com.ionic.cloudstorage.awss3.IonicS3EncryptionClient;
import com.ionic.cloudstorage.awss3.IonicS3EncryptionClientBuilder;
import com.ionic.cloudstorage.awss3.Version;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import java.io.File;
import java.lang.InterruptedException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class S3TransferManagerSample {

    static final Opt[] r = {Opt.RECUSIVE};
    static final Opt[] f = {Opt.FORCE};
    static final Opt[] rf = {Opt.RECUSIVE, Opt.FORCE};
    static final Opt[] none = {};

    enum Action {
        LIST("ls", 2, r),
        GET("get", 3, f),
        PUT("put", 3, rf),
        VERSION("version", 1, none),;

        final String verb;
        final int requiredArgs;
        final Set<Opt> options;

        Action(String verb, int requiredArgs, Opt[] options) {
            this.verb = verb;
            this.requiredArgs = requiredArgs;
            this.options = Collections.unmodifiableSet(new HashSet<Opt>(Arrays.asList(options)));
        }

        Opt getOptForString(String string) {
            for (Opt option : this.options) {
                if (option.string.equals(string)) {
                    return option;
                }
            }
            return null;
        }

        Opt getOptForChar(char flag) {
            for (Opt option : this.options) {
                if (option.flag == flag) {
                    return option;
                }
            }
            return null;
        }
    }

    enum Opt {
        RECUSIVE('r', "--recursive"),
        FORCE('f', "--force"),;

        final char flag;
        final String string;

        Opt(char flag, String string) {
            this.flag = flag;
            this.string = string;
        }
    }

    private static final String HOME = System.getProperty("user.home");

    // Clients
    private static AmazonS3 s3client = AmazonS3ClientBuilder.defaultClient();
    private static TransferManager transferManager = TransferManagerBuilder.standard()
            .withS3Client(setUpIonicS3EncryptionClient()).build();

    // options
    static boolean force = false;
    static boolean recursive = false;

    public static void main(String[] args) {

        Action action = null;
        int requiredArgs = 0;

        // Command Line Processing
        if (args.length > 0) {
            // Determine Action
            for (Action a : Action.values()) {
                if (a.verb.equals(args[0])) {
                    if (a.requiredArgs > args.length) {
                        usage();
                        System.exit(0);
                    }
                    action = a;
                    break;
                }
            }
            if (action == null) {
                usage();
                System.exit(0);
            }

        } else {
            usage();
            System.exit(0);
        }

        // Process Options for the selected action
        // Calls unsupportedOption() if options are invalid
        List<String> options = Arrays.asList(args).subList(action.requiredArgs, args.length);
        parseOptions(action, options);

        AmazonS3URI as3Uri = null;
        try {
            switch (action) {
                case LIST:
                    as3Uri = new AmazonS3URI(args[1]);
                    list(args[1], recursive);
                    break;
                case GET:
                    as3Uri = new AmazonS3URI(args[1]);
                    get(args[1], args[2], force);
                    break;
                case PUT:
                    as3Uri = new AmazonS3URI(args[2]);
                    put(args[1], args[2], recursive, force);
                    break;
                case VERSION:
                    System.out.println(Version.getFullVersion());
                    System.exit(0);
                    break;
                default:
                    usage();
            }
            System.exit(0);
        } catch (AmazonS3Exception e) {
            int statusCode = e.getStatusCode();
            if (statusCode == 404) {
                String errorCode = e.getErrorCode();
                if (errorCode.equals("NoSuchKey")) {
                    System.out.println("Key: " + as3Uri.getKey() + " does not exist in Bucket: "
                            + as3Uri.getBucket());
                } else if (errorCode.equals("NoSuchBucket")) {
                    System.out.println("Bucket: " + as3Uri.getBucket() + " does not exist");
                } else {
                    System.out.println(e.getClass() + ": " + e.getMessage());
                }
            } else if (statusCode == 40024) {
                System.out.println("Permission Denied by Ionic Policy.");
            } else {
                System.out.println(e.getMessage());
            }
            System.exit(1);
        } catch (Exception e) {
            System.out.println(e.getClass() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static AmazonS3Encryption setUpIonicS3EncryptionClient() {
        // Load a plain-text device profile (SEP) from disk
        try {
            String profilePath = Paths.get(HOME + "/.ionicsecurity/profiles.pt").toFile()
                    .getCanonicalPath();
            Agent agent = new Agent(new DeviceProfilePersistorPlainText(profilePath));
            agent.setMetadata(getMetadataMap());
            return IonicS3EncryptionClientBuilder.standard().withIonicAgent(agent).build();
        } catch (Exception e) {
            System.out.println("Exception occurred while loading profile.");
            System.out.println(e.getClass() + ": " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    private static void list(String s3path, boolean recursive) {
        AmazonS3URI as3Uri = new AmazonS3URI(s3path);
        ObjectListing objectListing =
                getObjectsForS3Path(as3Uri.getBucket(), as3Uri.getKey(), recursive, true);
        if (recursive == false) {
            // If not listing object keys recursivly list common prefixes (child psuedo directories)
            List<String> commonPrefixes = objectListing.getCommonPrefixes();
            for (String prefix : commonPrefixes) {
                System.out.println("s3://" + as3Uri.getBucket() + "/" + prefix);
            }
        }
        for (S3ObjectSummary object : objectListing.getObjectSummaries()) {
            // List object keys under the s3path
            System.out.println("s3://" + as3Uri.getBucket() + "/" + object.getKey());
        }
    }

    private static void put(String sourceDirectoryPath, String s3path, boolean recursive,
            boolean force)
                throws InterruptedException {
        AmazonS3URI as3Uri = new AmazonS3URI(s3path);
        File sourceDirectory = new File(sourceDirectoryPath);
        if (sourceDirectory.exists() == false) {
            System.err.println(sourceDirectoryPath + " does not exist");
            System.exit(1);
        }
        if (sourceDirectory.isDirectory() == false) {
            System.err.println(sourceDirectoryPath + " is not a directory");
            System.exit(1);
        }
        if (force) {
            // Upload with overwriting
            MultipleFileUpload transfer = transferManager.uploadDirectory(as3Uri.getBucket(),
                    as3Uri.getKey(), sourceDirectory, recursive);
            transfer.waitForCompletion();
            return;
        } else {
            /*
             * Check for possible collisions by comparing files to upload against existing objects
             * in bucket.
             */
            String keyPrefix = as3Uri.getKey();
            List<S3ObjectSummary> objectSummary =
                    getObjectsForS3Path(as3Uri.getBucket(), keyPrefix, recursive, false)
                    .getObjectSummaries();
            HashSet<String> objectSubKeys = new HashSet<>();
            for (S3ObjectSummary object : objectSummary) {
                objectSubKeys.add(object.getKey());
            }
            HashSet<String> files = null;
            if (recursive) {
                files = getFileStringsRecursively(keyPrefix, sourceDirectory);
            } else {
                List<String> fileList =  Arrays.asList(sourceDirectory.list());
                files = new HashSet();
                for (String file : fileList) {
                    files.add(keyPrefix + file);
                }
            }
            ArrayList<String> collidingKeys = new ArrayList();
            for (String file : files) {
                if (objectSubKeys.contains(file)) {
                    collidingKeys.add(file);
                }
            }
            if (collidingKeys.size() == 0) {
                // No collisions, proceed with upload
                MultipleFileUpload transfer = transferManager
                        .uploadDirectory(as3Uri.getBucket(), keyPrefix, sourceDirectory, recursive);
                transfer.waitForCompletion();
                return;
            } else {
                // Report collisions
                System.out.println("The following objects already exist inside the bucket \'"
                        + as3Uri.getBucket() + "\' :");
                for (String key : collidingKeys) {
                    System.out.println("\t" + key);
                }
                System.out.println("To overwrite these objects rerun this command with the --force"
                        + " option");
            }
        }
    }

    private static void get(String s3path, String destinationPath, boolean force)
            throws InterruptedException {
        AmazonS3URI as3Uri = new AmazonS3URI(s3path);
        String keyPrefix = as3Uri.getKey();
        File destinationDirectory = new File(destinationPath);
        if (destinationDirectory.exists() == false) {
            destinationDirectory.mkdirs();
        }
        if (destinationDirectory.isDirectory() == false) {
            System.err.println(destinationDirectory + " is not a directory");
            System.exit(1);
        }
        if (destinationDirectory.list().length == 0 || force) {
            MultipleFileDownload transfer =
                    transferManager.downloadDirectory(as3Uri.getBucket(), as3Uri.getKey(),
                    destinationDirectory);
            transfer.waitForCompletion();
            return;
        }
        // Compare objects to download against files present in destination Directory
        HashSet<String> files = getFileStringsRecursively("", destinationDirectory);
        List<S3ObjectSummary> objectSummary =
                getObjectsForS3Path(as3Uri.getBucket(), as3Uri.getKey(), true, false)
                .getObjectSummaries();
        HashSet<String> objectSubKeys = new HashSet<>();
        for (S3ObjectSummary object : objectSummary) {
            objectSubKeys.add(object.getKey());
        }
        ArrayList<String> collidingFiles = new ArrayList();
        for (String file : files) {
            if (objectSubKeys.contains(file)) {
                collidingFiles.add(file);
            }
        }
        if (collidingFiles.size() == 0) {
            MultipleFileDownload transfer = transferManager.downloadDirectory(as3Uri.getBucket(),
                    as3Uri.getKey(), destinationDirectory);
            transfer.waitForCompletion();
        } else {
            // Report collisions
            System.out.println("The following files already exist inside the directory \'"
                    + destinationDirectory + "\' :");
            for (String file : collidingFiles) {
                System.out.println("\t" + file);
            }
            System.out.println("To overwrite these files rerun this command with the --force "
                    + "option");
            return;
        }
    }

    private static HashSet<String> getFileStringsRecursively(String prefix, File file) {
        HashSet<String> fileStrings = new HashSet<String>();
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            ArrayList<File> children = new ArrayList<File>(Arrays.asList(files));
            for (File child : children) {
                if (child.isDirectory()) {
                    fileStrings.addAll(getFileStringsRecursively(prefix + child.getName()
                            + '/', child));
                } else {
                    fileStrings.add(prefix + child.getName());
                }
            }
        }
        return fileStrings;
    }

    private static ObjectListing getObjectsForS3Path(String bucket, String prefix,
            boolean recursive, boolean ls) {
        if (ls && prefix.endsWith("/") == false) {
            prefix = prefix.concat("/");
        }
        if (recursive) {
            return s3client.listObjects(new ListObjectsRequest(bucket, prefix, null, null, null));
        } else {
            return s3client.listObjects(new ListObjectsRequest(bucket, prefix, null, "/", null));
        }
    }

    private static void usage() {
        System.out.println("put <localPath> <s3:uri> [--force, --recursive]");
        System.out.println("get <s3:uri> <localPath> [--force]");
        System.out.println("ls <s3:uri> [--recursive]");
        System.out.println("version");
    }

    public static MetadataMap getMetadataMap() {
        MetadataMap applicationMetadata = new MetadataMap();
        applicationMetadata.set("ionic-application-name", "IonicS3TransferManagerExample");
        applicationMetadata.set("ionic-application-version", Version.getFullVersion());
        applicationMetadata.set("ionic-client-type", "IPCS S3 Java");
        applicationMetadata.set("ionic-client-version", Version.getFullVersion());

        return applicationMetadata;
    }

    // Argument Parsing Methods
    private static void parseOptions(Action action, List<String> args) {
        for (String option : args) {
            if (option.startsWith("--")) {
                Opt o = action.getOptForString(option);
                if (o != null) {
                    setOption(o);
                } else {
                    unsupportedOption(action, option);
                }
            } else if (option.startsWith("-")) {
                for (int i = 1; i < option.length(); i++) {
                    Opt o = action.getOptForChar(option.charAt(i));
                    if (o != null) {
                        setOption(o);
                    } else {
                        unsupportedOption(action, "-" + option.charAt(i));
                    }
                }
            } else {
                unsupportedOption(action, option);
            }
        }
    }

    private static void unsupportedOption(Action action, String option) {
        System.err.println(option + " is not a supported option for the command " + action.verb);
        usage();
        System.exit(1);
    }

    private static void setOption(Opt option) {
        if (option == Opt.RECUSIVE) {
            recursive = true;
        } else if (option == Opt.FORCE) {
            force = true;
        }
    }
}
