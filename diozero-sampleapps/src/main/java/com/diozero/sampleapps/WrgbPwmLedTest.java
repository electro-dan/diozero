package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Sample applications
 * Filename:     RgbPwmLedTest.java  
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

import com.diozero.devices.WrgbPwmLed;
import com.diozero.util.SleepUtil;

public class WrgbPwmLedTest {
	public static void main(String[] args) {
		int red_pin, green_pin, blue_pin, white_pin;
		if (args.length > 2) {
			red_pin = Integer.parseInt(args[0]);
			green_pin = Integer.parseInt(args[1]);
			blue_pin = Integer.parseInt(args[2]);
            white_pin = Integer.parseInt(args[3]);
		} else {
			red_pin = 17;
			blue_pin = 27;
			green_pin = 22;
            white_pin = 23;
		}
        // Fixed inverted green/blue pins
		test(red_pin, green_pin, blue_pin, white_pin);
	}
	
	private static void test(int redPin, int greenPin, int bluePin, int whitePin) {
		int delay = 500;
        // Changed init to allow max pwm frequency for pigpio
		try (WrgbPwmLed led = new WrgbPwmLed(redPin, greenPin, bluePin, whitePin, 1000)) {
			Logger.info("Blue");
			led.setValues(0, 0, 1, 0);	// 0010
			SleepUtil.sleepMillis(delay);
			Logger.info("Green");
			led.setValues(0, 1, 0, 0);	// 0100
			SleepUtil.sleepMillis(delay);
			Logger.info("Red");
			led.setValues(1, 0, 0, 0);	// 1000
			SleepUtil.sleepMillis(delay);
			Logger.info("White");
			led.setValues(0, 0, 0, 1);	// 0001
			SleepUtil.sleepMillis(delay);
			Logger.info("Blue + Green");
			led.setValues(0, 1, 1, 0);	// 0110
			SleepUtil.sleepMillis(delay);
			Logger.info("Red + Blue");
			led.setValues(1, 0, 1, 0);	// 1010
			SleepUtil.sleepMillis(delay);
			Logger.info("Red + Green");
			led.setValues(1, 1, 0, 0);	// 1100
			SleepUtil.sleepMillis(delay);
			Logger.info("Red + Green + Blue");
			led.setValues(1, 1, 1, 0);	// 1110
			SleepUtil.sleepMillis(delay);
			Logger.info("Red + Green + Blue + White");
			led.setValues(1, 1, 1, 1);	// 1111
			SleepUtil.sleepMillis(delay);
			
			float step = 0.01f;
			delay = 100;
			for (float r=0; r<=1; r+=step) {
				led.setValues(r, 0, 0, 0);
				Logger.info("Red @ " + r);
                SleepUtil.sleepMillis(delay);
			}
			for (float r=1; r>=0; r-=step) {
				led.setValues(r, 0, 0, 0);
				Logger.info("Red @ " + r);
				SleepUtil.sleepMillis(delay);
			}
			for (float g=0; g<=1; g+=step) {
				led.setValues(0, g, 0, 0);
				Logger.info("Green @ " + g);
				SleepUtil.sleepMillis(delay);
			}
			for (float g=1; g>=0; g-=step) {
				led.setValues(0, g, 0, 0);
				Logger.info("Green @ " + g);
				SleepUtil.sleepMillis(delay);
			}
			for (float b=0; b<=1; b+=step) {
				led.setValues(0, 0, b, 0);
				Logger.info("Blue @ " + b);
				SleepUtil.sleepMillis(delay);
			}
			for (float b=1; b>=0; b-=step) {
				led.setValues(0, 0, b, 0);
				Logger.info("Blue @ " + b);
				SleepUtil.sleepMillis(delay);
			}
			for (float w=0; w<=1; w+=step) {
				led.setValues(0, 0, 0, w);
				Logger.info("White @ " + w);
				SleepUtil.sleepMillis(delay);
			}
			for (float w=1; w>=0; w-=step) {
				led.setValues(0, 0, 0, w);
				Logger.info("White @ " + w);
				SleepUtil.sleepMillis(delay);
			}
		}
	}
}

