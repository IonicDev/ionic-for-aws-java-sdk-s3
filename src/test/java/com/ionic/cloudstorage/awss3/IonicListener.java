/*
 * (c) 2019-2020 Ionic Security Inc. By using this code, I agree to the LICENSE included, as well as the
 * Terms & Conditions (https://dev.ionic.com/use.html) and the Privacy Policy
 * (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.junit.listen;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class IonicListener extends RunListener {

    static String failOnSkipProp = "failOnSkip";

    static Logger log = LogManager.getLogger();

    public void testAssumptionFailure(Failure failure) {
        log.warn(failure.getMessage());
    }

    public void testFailure(Failure failure) {
        log.error(failure.getTrace());
    }

    public void testStarted(Description description) {
        log.info("Starting test " + description.getMethodName() + " in " + description.getTestClass().getSimpleName());
    }

    public void testFinished(Description description) {
        log.info("Completed test " + description.getMethodName() + " in " + description.getTestClass().getSimpleName());
    }

}
