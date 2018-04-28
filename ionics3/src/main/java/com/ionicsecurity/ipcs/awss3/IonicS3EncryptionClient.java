/*
 * (c) 2017 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.awss3;

import java.io.File;
import java.util.Map;

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

public class IonicS3EncryptionClient extends AmazonS3EncryptionClient 
implements AmazonS3Encryption{

    IonicS3EncryptionClient(IonicS3EncryptionClientParams params)
    {
        this(
                params.getClientParams().getCredentialsProvider(),
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
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest req) {
        EncryptedPutObjectRequest cryptoReq;
        if (req instanceof EncryptedPutObjectRequest)
        {
            cryptoReq = ((EncryptedPutObjectRequest)req);
        }
        else
        {
            if (req.getInputStream() != null)
            {
                cryptoReq = new EncryptedPutObjectRequest(req.getBucketName(),
                        req.getKey(), req.getInputStream(), req.getMetadata());
            } 
            else if (req.getFile() != null) 
            {
                cryptoReq = new EncryptedPutObjectRequest(req.getBucketName(), req.getKey(),
                        req.getFile());
                cryptoReq.setMetadata(req.getMetadata());
            }
            else
            {
                return super.putObject(req);
            }
        }
        ObjectMetadata objMetadata = req.getMetadata(); 
        if (objMetadata != null) 
        { 
            Map <String, String> userMetadata = objMetadata.getUserMetadata(); 
            if (userMetadata != null) 
            { 
                cryptoReq.setMaterialsDescription(userMetadata); 
            } 
        } 
        return super.putObject(cryptoReq);
    }

    @Override
    public S3Object getObject(GetObjectRequest req) {
        S3Object obj = super.getObject(req);
        if (req instanceof EncryptedGetObjectRequest)
        {
            obj.getObjectMetadata().setUserMetadata(
                    ((EncryptedGetObjectRequest) req).
                    getExtraMaterialDescription().getMaterialDescription());   
        }
        return obj;
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest req, File dest) {
        ObjectMetadata meta = super.getObject(req, dest);
        if (req instanceof EncryptedGetObjectRequest)
        {
            meta.setUserMetadata(
                    ((EncryptedGetObjectRequest) req).
                    getExtraMaterialDescription().getMaterialDescription());   
        }
        return meta;
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest req) 
    {
        EncryptedInitiateMultipartUploadRequest cryptoReq;
        if (req instanceof EncryptedInitiateMultipartUploadRequest)
        {
            cryptoReq = ((EncryptedInitiateMultipartUploadRequest)req);
        }
        else
        {
            cryptoReq = new EncryptedInitiateMultipartUploadRequest(
                    req.getBucketName(), req.getKey(), req.getObjectMetadata());
        }
        ObjectMetadata objMetadata = req.getObjectMetadata();
        if (objMetadata != null)
        {
            Map <String, String> userMetadata = objMetadata.getUserMetadata();
            if (userMetadata != null)
            {
                cryptoReq.setMaterialsDescription(userMetadata);
            }
        }
        return super.initiateMultipartUpload(cryptoReq);
    }

}
