/*
 * (c) 2017-2018 Ionic Security Inc.
 * By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.ipcs.awss3;

import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase;
import com.ionic.sdk.error.AgentErrorModuleConstants;
import com.ionic.sdk.error.IonicException;
import com.ionic.sdk.agent.data.MetadataMap;

/**
 * ISAgentPool provides a means of acquiring initialized
 * Agents in a thread safe manner that minimizes the
 * overhead of constructing and initiating Agents on
 * demand. Agents should be returned to with returnAgent()
 * when they are no longer needed for an operation.
 */
public class ISAgentPool {

    static private MetadataMap metadataMap = new MetadataMap();

    private ReentrantLock poolLock = new ReentrantLock(true);
    private Stack<Agent> agentStack = new Stack<Agent>();
    private DeviceProfilePersistorBase persistor;

    /**
     * getAgent() acquires an Agent from the AgentPool
     *
     * @return a {@link com.ionic.sdk.agent.Agent} object.
     * @throws com.ionic.sdk.error.IonicException if any.
     */
    public Agent getAgent() throws IonicException
    {
        Agent agent;

        poolLock.lock();
        if (agentStack.isEmpty() == true)
        {
            poolLock.unlock();
            agent = new Agent();
            if (persistor == null)
            {
                throw new IonicException(AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE.value());
            }
            agent.initialize(persistor);
            agent.setMetadata(metadataMap);
            return agent;
        }
        agent = agentStack.pop();
        poolLock.unlock();
        return agent;
    }

    /**
     * returnAgent() returns an Agent to the AgentPool
     * It is no longer to safe to reference the agent afterwards
     *
     * @param a a {@link com.ionic.sdk.agent.Agent} object.
     */
    public void returnAgent(Agent a)
    {
        poolLock.lock();
        agentStack.push(a);
        poolLock.unlock();
    }

    /**
     * flush() empties the AgentPool
     */
    public void flush()
    {
        poolLock.lock();
        agentStack.removeAllElements();
        poolLock.unlock();
    }

    /**
     * setPersistor() sets the persistor to use when making
     *  new agents and empties the AgentPool
     *
     * @param persistor a {@link com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase} object.
     */
    public void setPersistor(DeviceProfilePersistorBase persistor)
    {
        this.persistor = persistor;
        this.flush();
    }

    /**
     * setMetadataMap() sets the MetadataMap to be
     * used when generating new Agents
     * 
     * @param map a {@link com.ionic.sdk.agent.data.MetadataMap} object.
     */
    static void setMetadataMap(MetadataMap map)
    {
        metadataMap = map;
    }

    /**
     * getIonicMetadataMap() gets Agent.MetaDataMap
     * 
     * @return a {@link com.ionic.sdk.agent.data.MetadataMap} object.
     */
    static MetadataMap getMetadataMap()
    {
        return metadataMap;
    }

}
