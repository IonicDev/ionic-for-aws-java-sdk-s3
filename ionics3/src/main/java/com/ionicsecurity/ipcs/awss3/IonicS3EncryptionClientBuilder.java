package com.ionicsecurity.ipcs.awss3;

import com.ionicsecurity.ipcs.awss3.IonicEncryptionMaterialsProvider;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.AwsSyncClientParams;
import com.amazonaws.services.s3.AmazonS3Builder;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptionMaterialsProvider;

public class IonicS3EncryptionClientBuilder extends AmazonS3Builder<IonicS3EncryptionClientBuilder, AmazonS3Encryption> {
    private EncryptionMaterialsProvider encryptionMaterials;
    private CryptoConfiguration cryptoConfig = new CryptoConfiguration();

    /**
     * @return Create new instance of builder with all defaults set.
     */
    public static IonicS3EncryptionClientBuilder standard() {
        return new IonicS3EncryptionClientBuilder().withCredentials(new DefaultAWSCredentialsProviderChain());
    }

    /**
     * @return Default client using the {@link com.amazonaws.auth.DefaultAWSCredentialsProviderChain}
     * and {@link com.amazonaws.regions.DefaultAwsRegionProviderChain} chain
     */
    public static AmazonS3Encryption defaultClient() {
        return standard().withEncryptionMaterials(IonicEncryptionMaterialsProvider.standard()).build();
    }

    /**
     * Sets the encryption materials to be used to encrypt and decrypt data
     * @param encryptionMaterials a provider for the encryption materials
     */
    public void setEncryptionMaterials(EncryptionMaterialsProvider encryptionMaterials) {
        this.encryptionMaterials = encryptionMaterials;
    }

    /**
     * Sets the encryption materials to be used to encrypt and decrypt data
     * @param encryptionMaterials A provider for the encryption materials to be used to encrypt and decrypt data.
     * @return this object for method chaining
     */
    public IonicS3EncryptionClientBuilder withEncryptionMaterials(EncryptionMaterialsProvider encryptionMaterials) {
        setEncryptionMaterials(encryptionMaterials);
        return this;
    }

    /**
     * Sets the crypto configuration whose parameters will be used to encrypt and decrypt data.
     * @param cryptoConfig crypto configuration
     */
    public void setCryptoConfiguration(CryptoConfiguration cryptoConfig) {
        this.cryptoConfig = cryptoConfig;
    }

    /**
     * Sets the crypto configuration whose parameters will be used to encrypt and decrypt data.
     * @param cryptoConfig crypto configuration
     * @return this object for method chaining
     */
    public IonicS3EncryptionClientBuilder withCryptoConfiguration(CryptoConfiguration cryptoConfig) {
        setCryptoConfiguration(cryptoConfig);
        return this;
    }

    /**
     * Construct a synchronous implementation of AmazonS3Encryption using the current builder configuration.
     *
     * @return Fully configured implementation of AmazonS3Encryption.
     */
    @Override
    protected AmazonS3Encryption build(AwsSyncClientParams clientParams) {
        IonicS3EncryptionClientParams params = new IonicS3EncryptionClientParams(clientParams);
        if (this.encryptionMaterials != null && this.encryptionMaterials instanceof IonicEncryptionMaterialsProvider)
        {
            params.materialsProvider = this.encryptionMaterials;
        }
        else
        {
            throw new IllegalArgumentException(
                    "IonicS3EncryptionClientBuilder.encryptionMaterials must" +
                            "be set to an instance of IonicEncryptionMaterialsProvider" +
                    "to build an AmazonS3Encryption.");
        }
        params.cryptoConfig = this.cryptoConfig;
        if (params.cryptoConfig == null)
        {
            params.cryptoConfig = new CryptoConfiguration();
        }
        return new IonicS3EncryptionClient(params);
    }
}