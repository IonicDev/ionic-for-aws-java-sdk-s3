/*
 * (c) 2017-2018 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.awss3;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.HashMap;

import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptedGetObjectRequest;
import com.amazonaws.services.s3.model.EncryptedInitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.EncryptedPutObjectRequest;
import com.amazonaws.services.s3.model.EncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.StringUtils;

/**
 * IonicS3EncryptionClient class.
 *
 * A Ionic backed subclass of {@link com.amazonaws.services.s3.AmazonS3EncryptionClient}.
 */
public class IonicS3EncryptionClient extends AmazonS3EncryptionClient
    implements AmazonS3Encryption {

    private IonicEncryptionMaterialsProvider iemp;

    IonicS3EncryptionClient(IonicS3EncryptionClientParams params) {
        this(params.getClientParams().getCredentialsProvider(),
             params.getEncryptionMaterials(),
             params.getClientParams().getClientConfiguration(),
             params.getCryptoConfiguration(),
             params.getClientParams().getRequestMetricCollector());
    }

    @SuppressWarnings("deprecation")
    private IonicS3EncryptionClient(
            AWSCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider,
            ClientConfiguration clientConfig,
            CryptoConfiguration cryptoConfig,
            RequestMetricCollector requestMetricCollector) {
        super(null, // KMS client
              credentialsProvider, kekMaterialsProvider, clientConfig,
              cryptoConfig, requestMetricCollector);
        this.iemp = (IonicEncryptionMaterialsProvider)kekMaterialsProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PutObjectResult putObject(PutObjectRequest req) {
        return putObject(req, new CreateKeysRequest.Key(""));
    }

    /**
     * A version of {@link #putObject(String, String, File)}
     * that takes a {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}
     * as an argument for setting {@link com.ionic.sdk.agent.key.KeyAttributesMap Attributes}
     * and mutableAttributes on the Ionic Key associated with the object.
     *
     * @param bucketName The Bucket to store the Object in.
     * @param key The key to store the Object under.
     * @param file The File to stored.
     * @param ionicKey The CreateKeysRequest.Key containing attributes for associated Ionic Key.
     * @return A {@link com.amazonaws.services.s3.model.PutObjectResult} object containing the information returned by Amazon S3 for the newly created object.
     * @see #putObject(PutObjectRequest, CreateKeysRequest.Key)
     */
    public PutObjectResult putObject(String bucketName, String key, File file, CreateKeysRequest.Key ionicKey) {
        return putObject(new PutObjectRequest(bucketName, key, file).withMetadata(new ObjectMetadata()), ionicKey);
    }

    /**
     * A version of {@link #putObject(String, String, InputStream, ObjectMetadata)}
     * that takes a {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}
     * as an argument for setting {@link com.ionic.sdk.agent.key.KeyAttributesMap Attributes}
     * and mutableAttributes on the Ionic Key associated with the object.
     *
     * @param bucketName The Bucket to store the Object in.
     * @param key The key to store the Object under.
     * @param input InputStream with the Object contents.
     * @param metadata The ObjectMetadata associated with the Object.
     * @param ionicKey The CreateKeysRequest.Key containing attributes for associated Ionic Key.
     * @return A {@link com.amazonaws.services.s3.model.PutObjectResult} object containing the information returned by Amazon S3 for the newly created object.
     * @see #putObject(PutObjectRequest, CreateKeysRequest.Key)
     */
    public PutObjectResult putObject(String bucketName, String key,
            InputStream input, ObjectMetadata metadata, CreateKeysRequest.Key ionicKey) {
        return putObject(new PutObjectRequest(bucketName, key, input, metadata), ionicKey);
    }

    /**
     * A version of {@link #putObject(String, String, String)}
     * that takes a {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}
     * as an argument for setting {@link com.ionic.sdk.agent.key.KeyAttributesMap Attributes}
     * and mutableAttributes on the Ionic Key associated with the object.
     *
     * @param bucketName The Bucket to store the Object in.
     * @param key The key to store the Object under.
     * @param content A string to store as Object content.
     * @return A {@link com.amazonaws.services.s3.model.PutObjectResult} object containing the information returned by Amazon S3 for the newly created object.
     * @see #putObject(PutObjectRequest, CreateKeysRequest.Key)
     */
    public PutObjectResult putObject(String bucketName, String key, String content, CreateKeysRequest.Key ionicKey) {
        rejectNull(bucketName, "Bucket name must be provided");
        rejectNull(key, "Object key must be provided");
        rejectNull(content, "String content must be provided");

        byte[] contentBytes = content.getBytes(StringUtils.UTF8);

        InputStream is = new ByteArrayInputStream(contentBytes);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength(contentBytes.length);

        return putObject(new PutObjectRequest(bucketName, key, is, metadata), ionicKey);
    }

    /**
     * A version of {@link #putObject(PutObjectRequest)}
     * that takes a {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}
     * as an argument for setting {@link com.ionic.sdk.agent.key.KeyAttributesMap Attributes}
     * and mutableAttributes on the Ionic Key associated with the object.
     *
     * <p>Example of creating a CreateKeysRequest.Key {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}.
     * <pre> {@code
     * KeyAttributesMap attributes = new KeyAttributesMap();
     * KeyAttributesMap mutableAttributes = new KeyAttributesMap();
     * attributes.put("Attribute_Key1", Arrays.asList("Val1","Val2","Val3"));
     * mutableAttributes.put("Mutable_Attribute_Key1", Arrays.asList("Val1","Val2","Val3"));
     * CreateKeysRequest.Key reqKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);
     * }</pre>
     * @param req The PutObjectRequest object that specifies all the parameters of this operation.
     * @param key The CreateKeysRequest.Key containing attributes for associated Ionic Key.
     * @return A {@link com.amazonaws.services.s3.model.PutObjectResult} object containing the information returned by Amazon S3 for the newly created object.
     */
    public PutObjectResult putObject(PutObjectRequest req, CreateKeysRequest.Key key) {
        EncryptedPutObjectRequest cryptoReq;
        if (req instanceof EncryptedPutObjectRequest) {
            cryptoReq = ((EncryptedPutObjectRequest)req);
        } else {
            if (req.getInputStream() != null) {
                cryptoReq = new EncryptedPutObjectRequest(req.getBucketName(),
                        req.getKey(), req.getInputStream(), req.getMetadata());
            } else if (req.getFile() != null) {
                cryptoReq = new EncryptedPutObjectRequest(req.getBucketName(), req.getKey(),
                        req.getFile());
                cryptoReq.setMetadata(req.getMetadata());
            } else {
                // Nothing to encrypt. Pass through to super.
                return super.putObject(req);
            }
        }
        HashMap<String, String> materialsDescription = new HashMap<String, String>();
        if (iemp.isEnabledMetadataCapture()) {
            ObjectMetadata objMetadata = req.getMetadata();
            if (objMetadata != null) {
                Map <String, String> userMetadata = objMetadata.getUserMetadata();
                if (userMetadata != null) {
                    materialsDescription.putAll(userMetadata);
                }
            }
        }
        materialsDescription.put(IonicEncryptionMaterialsProvider.IONICKEYREQUUID, iemp.storeRequestKey(key));
        cryptoReq.setMaterialsDescription(materialsDescription);

        return super.putObject(cryptoReq);
    }

    private GetKeysResponse.Key keyFromMetadataInternal(ObjectMetadata meta)
    {
        String matdesc = meta.getUserMetaDataOf("x-amz-matdesc");
        matdesc = matdesc.substring(1, matdesc.length());
        String[] descriptions = matdesc.split(",");
        String keyId = null;
        String pattern = "\"" + IonicEncryptionMaterialsProvider.KEYIDKEY + "\":\"";
        for (String entry : descriptions) {
            if (entry.startsWith(pattern)) {
                keyId = entry.substring(pattern.length(), pattern.length() + 11);
            }
        }
        return this.iemp.retrieveResponseKey(keyId);
    }

    /**
     * A container class that holds a pairing of {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key}
     * and {@link com.amazonaws.services.s3.model.S3Object} returned by {@link #getObjectAndKey()} methods.
     */
    public class IonicKeyS3ObjectPair {
        private GetKeysResponse.Key key;
        private S3Object object;

        private IonicKeyS3ObjectPair(GetKeysResponse.Key key, S3Object object) {
            this.key = key;
            this.object = object;
        }

        /**
         * Returns a GetKeysResponse.Key.
         * @return a {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key}
         */
        public GetKeysResponse.Key getKey() {
            return this.key;
        }

        /**
         * Returns a S3Object.
         * @return a {@link com.amazonaws.services.s3.model.S3Object}
         */
        public S3Object getS3Object() {
            return this.object;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public S3Object getObject(GetObjectRequest req) {
        S3Object obj = super.getObject(req);
        ObjectMetadata meta = obj.getObjectMetadata();
        keyFromMetadataInternal(meta);
        return obj;
    }

    /**
     * A version of {@link #getObject(String, String)} that returns a IonicKeyS3ObjectPair containing
     * the requested S3Object and the GetKeysResponse.Key for the underlying Ionic
     * {@link com.ionic.sdk.agent.Agent#getKey(String)} request.
     * @param bucketName The name of the bucket containing the desired object.
     * @param key The key under which the desired object is stored.
     * @return a {@link IonicS3EncryptionClient.IonicKeyS3ObjectPair}
     */
    public IonicKeyS3ObjectPair getObjectAndKey(String bucketName, String key) {
        return getObjectAndKey(new GetObjectRequest(bucketName, key));
    }

    /**
     * A version of {@link #getObject(GetObjectRequest)} that returns a IonicKeyS3ObjectPair containing
     * the requested S3Object and the GetKeysResponse.Key for the underlying Ionic
     * {@link com.ionic.sdk.agent.Agent#getKey(String)} request.
     * @param req The request object containing all the options on how to download the object.
     * @return a {@link IonicS3EncryptionClient.IonicKeyS3ObjectPair}
     */
    public IonicKeyS3ObjectPair getObjectAndKey(GetObjectRequest req) {
        S3Object obj = super.getObject(req);
        ObjectMetadata meta = obj.getObjectMetadata();
        return new IonicKeyS3ObjectPair(keyFromMetadataInternal(meta), obj);
    }

    /**
     * A container class that holds a pairing of {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key}
     * and {@link com.amazonaws.services.s3.model.ObjectMetadata} returned by {@link #readAllBytesAndKey()} methods.
     */
    public class IonicKeyObjectMetadataPair {
        private GetKeysResponse.Key key;
        private ObjectMetadata meta;

        private IonicKeyObjectMetadataPair(GetKeysResponse.Key key, ObjectMetadata meta) {
            this.key = key;
            this.meta = meta;
        }

        /**
         * Returns a GetKeysResponse.Key.
         * @return a {@link com.ionic.sdk.agent.request.createkey.CreateKeysResponse.Key}
         */
        public GetKeysResponse.Key getKey() {
            return this.key;
        }

        /**
         * Returns a ObjectMetadata.
         * @return a {@link com.amazonaws.services.s3.model.ObjectMetadata}
         */
        public ObjectMetadata getObjectMetadata() {
            return this.meta;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectMetadata getObject(GetObjectRequest req, File dest) {
        ObjectMetadata meta = super.getObject(req, dest);
        keyFromMetadataInternal(meta);
        return meta;
    }

    /**
     * A version of {@link #getObject(GetObjectRequest, File)} that returns a IonicKeyObjectMetadataPair containing
     * the requested ObjectMetadata and the GetKeysResponse.Key for the underlying Ionic
     * {@link com.ionic.sdk.agent.Agent#getKey(String)} request.
     * @param req The request object containing all the options on how to download the object.
     * @param dest Indicates the file (which might already exist) where to save the object content being downloading from Amazon S3.
     * @return a {@link IonicS3EncryptionClient.IonicKeyObjectMetadataPair}
     */
    public IonicKeyObjectMetadataPair getObjectAndKey(GetObjectRequest req, File dest) {
        ObjectMetadata meta = super.getObject(req, dest);
        return new IonicKeyObjectMetadataPair(keyFromMetadataInternal(meta), meta);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest req) {
        return initiateMultipartUpload(req, new CreateKeysRequest.Key(""));
    }

    /**
     * A version of {@link #initiateMultipartUpload(InitiateMultipartUploadRequest)}
     * that takes a {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}
     * as an argument for setting {@link com.ionic.sdk.agent.key.KeyAttributesMap Attributes}
     * and mutableAttributes on the Ionic Key associated with the object.
     *
     * <p>Example of creating a CreateKeysRequest.Key {@link com.ionic.sdk.agent.request.createkey.CreateKeysRequest.Key}.
     * <pre> {@code
     * KeyAttributesMap attributes = new KeyAttributesMap();
     * KeyAttributesMap mutableAttributes = new KeyAttributesMap();
     * attributes.put("Attribute_Key1", Arrays.asList("Val1","Val2","Val3"));
     * mutableAttributes.put("Mutable_Attribute_Key1", Arrays.asList("Val1","Val2","Val3"));
     * CreateKeysRequest.Key reqKey = new CreateKeysRequest.Key("", 1, attributes, mutableAttributes);
     * }</pre>
     * @param req The InitiateMultipartUploadRequest object that specifies all the parameters of this operation.
     * @param key The CreateKeysRequest.Key containing attributes for associated Ionic Key.
     * @return An InitiateMultipartUploadResult from Amazon S3.
     */
    public InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest req, CreateKeysRequest.Key key) {
        EncryptedInitiateMultipartUploadRequest cryptoReq;
        if (req instanceof EncryptedInitiateMultipartUploadRequest) {
            cryptoReq = ((EncryptedInitiateMultipartUploadRequest)req);
        } else {
            cryptoReq = new EncryptedInitiateMultipartUploadRequest(
                    req.getBucketName(), req.getKey(), req.getObjectMetadata());
        }
        HashMap<String, String> materialsDescription = new HashMap<String, String>();
        if (iemp.isEnabledMetadataCapture()) {
            ObjectMetadata objMetadata = req.getObjectMetadata();
            if (objMetadata != null) {
                Map <String, String> userMetadata = objMetadata.getUserMetadata();
                if (userMetadata != null) {
                    materialsDescription.putAll(userMetadata);
                }
            }
        }
        materialsDescription.put(IonicEncryptionMaterialsProvider.IONICKEYREQUUID, iemp.storeRequestKey(key));
        cryptoReq.setMaterialsDescription(materialsDescription);
        return super.initiateMultipartUpload(cryptoReq);
    }

    private void rejectNull(Object parameterValue, String errorMessage) {
        if (parameterValue == null) throw new IllegalArgumentException(errorMessage);
    }

}
