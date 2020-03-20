/*
 * (c) 2017-2020 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as
 * the Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import com.amazonaws.client.AwsSyncClientParams;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptionMaterialsProvider;

/**
 * A faux implementation of com.amazonaws.services.s3.AmazonS3EncryptionClientParams.
 */
public class IonicS3EncryptionClientParams {
    EncryptionMaterialsProvider materialsProvider;
    CryptoConfiguration cryptoConfig;
    AwsSyncClientParams clientParams;
    S3ClientOptions clientOptions;

    public EncryptionMaterialsProvider getEncryptionMaterials() {
        return materialsProvider;
    }

    public CryptoConfiguration getCryptoConfiguration() {
        return cryptoConfig;
    }

    public AWSKMS getKmsClient() {
        return null;
    }

    public AwsSyncClientParams getClientParams() {
        return clientParams;
    }

    public S3ClientOptions getS3ClientOptions() {
        return clientOptions;
    }

    IonicS3EncryptionClientParams(AwsSyncClientParams clientParams) {
        this.clientParams = clientParams;
    }
}
