/*
 * (c) 2019-2021 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.awss3;

import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.junit.runner.Description;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class RetryRule implements TestRule {

    static Logger log = LogManager.getLogger();
    static String knownFault =
        "Not Found (Service: Amazon S3; Status Code: 404; Error Code: 404 Not Found;";

    @Override
    public Statement apply(Statement base, Description method) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Retry retry = method.getAnnotation(Retry.class);
                if (retry != null) {
                    int attempts = 0;
                    int retries = TestUtils.getRetriesCount();
                    int sleep = TestUtils.getRetryDelay();
                    while (true) {
                        attempts++;
                        Boolean success = true;
                        try {
                            base.evaluate();
                        } catch (Throwable t) {
                            success = false;
                            log.error("Encountered exception: " +
                                t.getClass().getCanonicalName());
                            log.error(t.getMessage());
                            if (attempts >= retries) {
                                log.error("Retries exhausted.");
                                throw t;
                            } else if (t.getMessage().startsWith(knownFault)) {
                                log.error("Attempt " + attempts + "out of " + retries
                                    + ". Retrying...");
                                if (sleep > 0) {
                                    log.warn("Sleeping for " + sleep +
                                        " milliseconds before retry.");
                                    Thread.sleep(sleep);
                                }
                            } else {
                                throw t;
                            }
                        }
                        if (success) {
                            break;
                        }
                    }
                } else {
                    base.evaluate();
                }
            }
        };
    }
}
