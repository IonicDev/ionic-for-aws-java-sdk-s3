/*
 * (c) 2019 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.junit.listen;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class SkipFailListener extends RunListener {

    static String failOnSkipProp = "failOnSkip";

    public void testAssumptionFailure(Failure failure) {
        String prop = System.getProperty(failOnSkipProp);
        if (prop != null && prop.equalsIgnoreCase("true")) {
            throw new RuntimeException("Test failed to meet preconditions.");
        }
    }

}
