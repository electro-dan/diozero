package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Sample applications
 * Filename:     TM1637Test.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.pmw.tinylog.Logger;

import com.diozero.devices.TM1637;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * TM1637 serial to 7-segment digits test. To run:
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.TM1637Test 0 0 0}</li>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.TM1637Test 0 0 0}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.TM1637Test 0 0 0}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.TM1637Test 0 0 0}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.TM1637Test 0 0 0}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.1.jar com.diozero.sampleapps.TM1637Test 0 0 0}</li>
 * </ul>
 */
public class TM1637Test {
	public static void main(String[] args) {
		if (args.length < 2) {
			Logger.error("Usage: {} <dio-pin> <clk-pin> <num-digits>", TM1637Test.class.getName());
			System.exit(2);
		}

		int sdaPin = Integer.parseInt(args[0]);
		int clkPin = Integer.parseInt(args[1]);
		int maxDigits = Integer.parseInt(args[2]);

		test(sdaPin, clkPin, maxDigits);
	}
	
	public static void test(int sdaPin, int clkPin, int maxDigits) {
		try (TM1637 tm1637 = new TM1637(sdaPin, clkPin, maxDigits)) {
			tm1637.init();
			
            Logger.info("Test");
            tm1637.test();
			SleepUtil.sleepSeconds(2);
            
            Logger.info("Lower brightness");
            tm1637.changeBrightness(1);
			
            Logger.info("Display text");
            tm1637.displayString("24Hr");
			SleepUtil.sleepSeconds(2);
			
            Logger.info("Display time 24H");
			tm1637.displayTime(true, true);
			SleepUtil.sleepSeconds(2);
			
            Logger.info("Display text");
			tm1637.displayString("12Hr");
			SleepUtil.sleepSeconds(2);
			
            Logger.info("Display time 12H");
			tm1637.displayTime(false, true);
			SleepUtil.sleepSeconds(2);
			
            tm1637.displayString("Date");
			SleepUtil.sleepSeconds(2);
			tm1637.displayDate(false); // UK format
			SleepUtil.sleepSeconds(2);
			
            Logger.info("Raise brightness");
            tm1637.changeBrightness(7);

            tm1637.displayString("Int");
			SleepUtil.sleepSeconds(2);
			tm1637.displayInteger(1637);
			SleepUtil.sleepSeconds(2);
			
            tm1637.displayString("-Int");
			SleepUtil.sleepSeconds(2);
			tm1637.displayInteger(-33);

            Logger.info("Display off");
			SleepUtil.sleepSeconds(2);
			tm1637.displayOff();
            
            Logger.info("Display on");
			SleepUtil.sleepSeconds(2);
			tm1637.displayOn();

            tm1637.scrollString("Hi again", 500);
            SleepUtil.sleepSeconds(2);
            tm1637.displayOff();

		} catch (RuntimeIOException ioe) {
			Logger.error(ioe, "Error: {}", ioe);
		}
	}
}
