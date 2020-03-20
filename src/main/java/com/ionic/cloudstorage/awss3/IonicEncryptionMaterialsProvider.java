/*
 * (c) 2017-2020 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as
 * the Terms & Conditions (https://dev.ionic.com/use) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.EncryptionMaterialsProvider;
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.agent.request.createkey.CreateKeysResponse;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IonicEncryptionMaterialsProvider class.
 */
public class IonicEncryptionMaterialsProvider implements EncryptionMaterialsProvider {
    // String constants
    static final String KEYIDKEY = "ionic-key-id";
    static final String IONICVERSIONKEY = "ionic-s3-version";
    static final String IONICKEYREQUUID = "ionic-kam-uuid";

    private static final String HOME = System.getProperty("user.home");

    private boolean enabledMetadataCapture = false;

    private KeyAttributesMap defaultAttributes = new KeyAttributesMap();
    private Agent agent = new Agent();

    private ConcurrentHashMap<String, CreateKeysRequest.Key> requestKeyMap =
            new ConcurrentHashMap<String, CreateKeysRequest.Key>();
    private ConcurrentHashMap<String, GetKeysResponse.Key> responseKeyMap =
            new ConcurrentHashMap<String, GetKeysResponse.Key>();

    /**
     * Default constructor for IonicEncryptionMaterialsProvider.
     */
    public IonicEncryptionMaterialsProvider() {}

    /**
     * Constructor for IonicEncryptionMaterialsProvider that takes an Agent to
     * use for Ionic API calls.
     *
     * @param agent an {@link com.ionic.sdk.agent.Agent} object.
     */
    public IonicEncryptionMaterialsProvider(Agent agent) {
        this.agent = agent;
    }

    /**
     * Constructor for IonicEncryptionMaterialsProvider that takes a persistor
     * and uses it's active profile to construct the inernal Agent.
     * Deprecated. Use {@link #IonicEncryptionMaterialsProvider(Agent)} instead.
     * @param persistor a
     * {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase} object.
     * @throws IonicException on errors
     */
    @Deprecated
    public IonicEncryptionMaterialsProvider(DeviceProfilePersistorBase persistor)
            throws IonicException {
        setPersistor(persistor);
    }

    /**
     * Create new instance of IonicEncryptionMaterialsProvider with the default PlainText
     * Persistor. Note: PlainText persistors should be used for development only. See Ionic
     * devportal for more on profile persistor types
     * https://dev.in.ionicsecurity.com/devportal/develop/platform/concepts/profile-persistor
     *
     * @return a {@link com.ionic.cloudstorage.awss3.IonicEncryptionMaterialsProvider} object.
     * @throws IonicException if $HOME/.ionicsecurity/profiles.pt does not exist or is malformed.
     * @throws IOException if $HOME/.ionicsecurity/profiles.pt is inaccessible due to file
     *     permissions.
     */
    public static IonicEncryptionMaterialsProvider standard() throws IonicException, IOException {
        // Load a plain-text device profile (SEP) from disk
        DeviceProfilePersistorPlainText persistor = new DeviceProfilePersistorPlainText();
        String profilePath =
                    Paths.get(HOME + "/.ionicsecurity/profiles.pt").toFile().getCanonicalPath();
        persistor.setFilePath(profilePath);

        return new IonicEncryptionMaterialsProvider(persistor);
    }

    /**
     * Not implemented.
     * @deprecated No op.
     */
    @Override
    public void refresh() {}

    /**
     * Produces EncryptionMaterials by creating a new encryption key via IDC.
     *
     * @return a {@link com.amazonaws.services.s3.model.EncryptionMaterials} object.
     */
    public EncryptionMaterials getEncryptionMaterials() {
        try {
            return generateEncryptionMaterials(new HashMap<String, String>());
        } catch (IonicException e) {
            AmazonS3Exception s3Exception = new AmazonS3Exception(e.getLocalizedMessage(), e);
            s3Exception.setStatusCode(e.getReturnCode());
            s3Exception.setErrorCode(String.valueOf(e.getReturnCode()));
            s3Exception.setServiceName("Ionic Security");
            throw s3Exception;
        }
    }

    /**
     * Produces EncryptionMaterials by either creating a new encryption key
     * via IDC if no Ionic_Key_ID key is present in materialsDescription or by attempting to
     * retrieve an existing key from IDC with its KeyId.
     */
    @Override
    public EncryptionMaterials getEncryptionMaterials(Map<String, String> materialsDescription) {
        return getEncryptionMaterialsInternal(materialsDescription);
    }

    private EncryptionMaterials getEncryptionMaterialsInternal(Map<String, String> desc) {
        try {
            if (desc == null || desc.get(KEYIDKEY) == null) {
                return generateEncryptionMaterials(desc);
            }
            return retrieveEncryptionMaterials(desc);
        } catch (IonicException e) {
            AmazonS3Exception s3Exception = new AmazonS3Exception(e.getLocalizedMessage(), e);
            s3Exception.setStatusCode(e.getReturnCode());
            s3Exception.setErrorCode(String.valueOf(e.getReturnCode()));
            s3Exception.setServiceName("Ionic Security");
            throw s3Exception;
        }
    }

    private EncryptionMaterials generateEncryptionMaterials(Map<String, String> desc)
            throws IonicException {
        KeyAttributesMap kam = new KeyAttributesMap();
        if (desc != null && enabledMetadataCapture) {
            for (Map.Entry<String, String> entry : desc.entrySet()) {
                if (entry.getKey() != IONICKEYREQUUID) {
                    ArrayList<String> collection = new ArrayList<String>();
                    collection.add(entry.getValue());
                    kam.put(entry.getKey(), collection);
                }
            }
        }

        kam.putAll(defaultAttributes);
        CreateKeysRequest.Key reqKey = new CreateKeysRequest.Key("");
        if (desc != null) {
            String uuid = desc.get(IONICKEYREQUUID);
            if (uuid != null) {
                reqKey = retrieveRequestKey(uuid);
                kam.putAll(reqKey.getAttributesMap());
            }
        }
        Agent agent = Agent.clone(this.agent);
        CreateKeysResponse.Key ionicKey = null;
        ionicKey = agent.createKey(kam, reqKey.getMutableAttributesMap()).getFirstKey();
        EncryptionMaterials materials = new EncryptionMaterials(ionicKey.getSecretKey());
        materials.addDescription(KEYIDKEY, ionicKey.getId());
        materials.addDescription(IONICVERSIONKEY, "1.0.0");
        return materials;
    }

    private EncryptionMaterials retrieveEncryptionMaterials(Map<String, String> desc)
            throws IonicException {
        String ionicKeyId = desc.get(KEYIDKEY);
        Agent agent = Agent.clone(this.agent);
        GetKeysResponse.Key ionicKey;
        ionicKey = agent.getKey(ionicKeyId).getFirstKey();
        storeResponseKey(ionicKey);
        EncryptionMaterials materials = new EncryptionMaterials(ionicKey.getSecretKey());
        return materials;
    }

    /**
     * Returns enabledMetadataCapture.
     *
     * @return a boolean.
     */
    public boolean isEnabledMetadataCapture() {
        return enabledMetadataCapture;
    }

    /**
     * Sets enabledMetadataCapture, while true S3 requests to store
     * objects with client side Encryption will have their userMetadata parsed and passed as ionic
     * attributes when content encryption keys are generated.
     *
     * @param enabledMetadataCapture a boolean.
     */
    public void setEnabledMetadataCapture(boolean enabledMetadataCapture) {
        this.enabledMetadataCapture = enabledMetadataCapture;
    }

    /**
     * Sets the default Attributes to be applied to all Agent.keyCreate() requests.
     *
     * @param defaultAttributes a {@link com.ionic.sdk.agent.key.KeyAttributesMap} object.
     */
    public void setDefaultAttributes(KeyAttributesMap defaultAttributes) {
        this.defaultAttributes = new KeyAttributesMap(defaultAttributes);
    }

    /**
     * Gets the defaultAttributes Map.
     *
     * @return a {@link com.ionic.sdk.agent.key.KeyAttributesMap} object.
     */
    public KeyAttributesMap getDefaultAttributes() {
        return new KeyAttributesMap(defaultAttributes);
    }

    /**
     * Sets the MetadataMap used for IDC interactions.
     *
     * @param map a {@link com.ionic.sdk.agent.data.MetadataMap} object.
     */
    @Deprecated
    public void setIonicMetadataMap(MetadataMap map) {
        this.agent.setMetadata(map);
    }

    /**
     * Gets the MetadataMap used for IDC interactions.
     *
     * @return a {@link com.ionic.sdk.agent.data.MetadataMap} object.
     */
    @Deprecated
    public MetadataMap getIonicMetadataMap() {
        return this.agent.getMetadata();
    }

    /**
     * Overwrites the internal Agent with a new Agent constructed with persistor.
     * Deprecated: Use {@link #setAgent(Agent)} instead.
     *
     * @param persistor a {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase}
     *     object.
     * @throws IonicException if persistor does not have an Active Profile.
     */
    @Deprecated
    public void setPersistor(DeviceProfilePersistorBase persistor) throws IonicException {
        Agent agent = new Agent(persistor);
        this.setAgent(agent);
    }

    /**
     * Sets the inernal Agent to agent.
     * @param agent the {@link com.ionic.sdk.agent.Agent} to be used for IDC transactions.
     */
    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    /**
     * Gets a refrence to the internal Agent object.
     * @return an {@link com.ionic.sdk.agent.Agent}.
     */
    public Agent getAgent() {
        return this.agent;
    }

    protected String storeRequestKey(CreateKeysRequest.Key key) {
        String uuid = UUID.randomUUID().toString();
        this.requestKeyMap.put(uuid, key);
        return uuid;
    }

    private CreateKeysRequest.Key retrieveRequestKey(String uuid) {
        return this.requestKeyMap.remove(uuid);
    }

    private void storeResponseKey(GetKeysResponse.Key key) {
        this.responseKeyMap.put(key.getId(), key);
    }

    protected GetKeysResponse.Key retrieveResponseKey(String keyId) {
        return this.responseKeyMap.remove(keyId);
    }
}
