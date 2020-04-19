package com.diozero.devices;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     TM1637.java  
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

import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.WaitableDigitalInputDevice;

/**
 * <p>Encapsulates the sort-of serial interface to the 4 or 6 digit 7-segment TM1637 LED display hardware (RobotDyn).
 * Serial isn't I2C or SPI compliant, hence has to be bit-banged</p>
 * 
 * <p>Keypad not implemented</p>
 * 
 * <p>Wiring:</p>
 * <pre>
 * 5V  .... 5v
 * GND .... Ground
 * CLK .... Clock (GPIO)
 * DIO .... Data (GPIO)
 * </pre>
 * 
 * <p>
 * Links
 * </p>
 * <ul>
 * <li><a href=
 * "https://www.mcielectronics.cl/website_MCI/static/documents/Datasheet_TM1637.pdf">Datasheet</a></li>
 * <li><a href=
 * "https://github.com/Bogdanel/Raspberry-Pi-Python-3-TM1637-Clock/blob/master/tm1637.py">Python
 * code</a></li>
 * <li><a href=
 * "https://github.com/depklyon/raspberrypi-python-tm1637/blob/master/tm1637.py">More Python
 * code</a></li>
 * <li><a href=
 * "http://www.ccsinfo.com/forum/viewtopic.php?p=57034">Conversion ASCII to 7 segment</a></li>
 * </ul>
 */
 public class TM1637 implements AutoCloseable {
	
	// Used to output the segments from numbers
    final byte displayNumtoSeg[] = {0x3f, 0x06, 0x5b, 0x4f, 0x66, 0x6d, 0x7d, 0x07, 0x7f, 0x6f};
	// Used to output the segments from ascii
	// This table, taken from http://www.ccsinfo.com/forum/viewtopic.php?p=57034 is ideal for writing the converted character out
    // The byte order has been inverted to make it compatible for TM1637
    // Index starts at 0x20 / 32 in the ASCII table (location of space character)
	final byte displayASCIItoSeg[] = {
		0x00,	// ' '
		0x00,	// '!', No seven-segment conversion for exclamation point
		0x22,	// '"', Double quote
		0x00,	// '#', Pound sign
		0x00,	// '$', No seven-segment conversion for dollar sign
		0x00,	// '%', No seven-segment conversion for percent sign
		0x00,	// '&', No seven-segment conversion for ampersand
		0x02,	// ''', Single quote
		0x39,	// '(', Same as '['
		0x0F,	// ')', Same as ']'
		0x00,	// '*', No seven-segment conversion for asterix
		0x00,	// '+', No seven-segment conversion for plus sign
		0x00,	// ',', No seven-segment conversion for comma
		0x40,	// '-', Minus sign
		0x00,	// '.', No seven-segment conversion for period
		0x00,	// '/', No seven-segment conversion for slash
		0x3F,	// '0'
		0x06,	// '1'
		0x5B,	// '2'
		0x4F,	// '3'
		0x66,	// '4'
		0x6D,	// '5'
		0x7D,	// '6'
		0x07,	// '7'
		0x7F,	// '8'
		0x6F,	// '9'
		0x00,	// ':', No seven-segment conversion for colon
		0x00,	// ';', No seven-segment conversion for semi-colon
		0x00,	// '<', No seven-segment conversion for less-than sign
		0x48,	// '=', Equal sign
		0x00,	// '>', No seven-segment conversion for greater-than sign
		0x53,	//'?', Question mark
		0x00,	// '@', No seven-segment conversion for commercial at-sign
		0x77,	// 'A'
		0x7C,	// 'B', Actually displayed as 'b'
		0x39,	// 'C'
		0x5E,	// 'D', Actually displayed as 'd'
		0x79,	// 'E'
		0x71,	// 'F'
		0x3D,	// 'G', Actually displayed as 'g'
		0x76,	// 'H'
		0x06,	// 'I', Same as '1'
		0x1E,	// 'J'
		0x00,	// 'K', No seven-segment conversion
		0x38,	// 'L'
		0x00,	// 'M', No seven-segment conversion
		0x54,	// 'N', Actually displayed as 'n'
		0x3F,	// 'O', Same as '0'
		0x73,	// 'P'
		0x00,	// 'Q', No seven-segment conversion
		0x50,	// 'R', Actually displayed as 'r'
		0x6D,	// 'S', Same as '5'
		0x07,	// 'T', Displayed as 7
		0x3E,	// 'U'
		0x00,	// 'V', No seven-segment conversion
		0x00,	// 'W', No seven-segment conversion
		0x00,	// 'X', No seven-segment conversion
		0x6E,	// 'Y'
		0x00,	// 'Z', No seven-segment conversion
		0x00,	// '['
		0x00,	// '\', No seven-segment conversion
		0x00,	// ']'
		0x00,	// '^', No seven-segment conversion
		0x00,	// '_', Underscore
		0x00,	// '`', No seven-segment conversion for reverse quote
		0x5F,	// 'a'
		0x7C,	// 'b'
		0x58,	// 'c'
		0x5E,	// 'd'
		0x7B,	// 'e'
		0x71,	// 'f', Actually displayed as 'F'
		0x3D,	// 'g'
		0x74,	// 'h'
		0x04,	// 'i'
		0x1E,	// 'j', Actually displayed as 'J'
		0x00,	// 'k', No seven-segment conversion
		0x38,	// 'l', Actually displayed as 'L'
		0x00,	// 'm', No seven-segment conversion
		0x54,	// 'n'
		0x5C,	// 'o'
		0x73,	// 'p', Actually displayed as 'P'
		0x00,	// 'q', No seven-segment conversion
		0x50,	// 'r'
		0x6D,	// 's', Actually displayed as 'S'
		0x78,	// 't'
		0x1C,	// 'u'
		0x00,	// 'v', No seven-segment conversion
		0x00,	// 'w', No seven-segment conversion
		0x00,	// 'x', No seven-segment conversion
		0x6E,	// 'y', Actually displayed as 'Y'
		0x00	// 'z', No seven-segment conversion
	};
	
	final byte COLON = (byte) 0x80;
    int iBrightness = 7; // 0 to 7
	// 0x40 [01000000] = indicate command to display data
	byte bSetData = (byte)0x40;
	// 0xC0 [11000000] = write out all bytes
	byte bSetAddr = (byte)0xC0;
	// 0x88 [10001000] - Display ON, plus brightness
	byte bSetOn = (byte)0x88;
	
	// Copy of the data to write / on the display
	byte bDigits[] = {0, 0, 0, 0, 0, 0};
	
	// GPIO pins
	int sdaPin = 22;
	int clkPin = 27;
	// Number of digits available
	int maxDigits = 4;
	
    private boolean doAckWait = false;
	private DigitalOutputDevice clkOut;
    private DigitalOutputDevice sdaOut;

	/**
	 * @param sdaPin
	 *            GPIO to which the DIO pin is connected
	 * @param clkPin
	 *            GPIO to which the CLK pin is connected
	 * @param maxDigits
	 *            The number of digits/displays on the TM1637 i.e. 4
	 * @throws RuntimeIOException
	 *             If an I/O error occurs.
	 */
    public TM1637(int sdaPin, int clkPin, int maxDigits) {
        this.sdaPin = sdaPin;
		this.clkPin = clkPin;
		// Some displays are 4 digits, some are 6 digits
		if ((maxDigits > 6) || (maxDigits <= 1))
			throw new RuntimeIOException("Maximum number of digits can only be 1 to 6");
		this.maxDigits = maxDigits;

        clkOut = new DigitalOutputDevice(clkPin);
        sdaOut = new DigitalOutputDevice(sdaPin);
    }

	/**
	 * Initialise the display
	 */
    public void init() {
        clkOut.on();
        sdaOut.on();

        clearDisplay();
    }

	/**
	 * Switch off the display
	 */
    public void displayOff() {
        // Write 0x40 [01000000] to indicate command to display data - [Write data to display register]
        doStartCondition();
        doByteWrite(bSetData);
        doStopCondition();

        doStartCondition();
        doByteWrite((byte)(0x80));
        doStopCondition();
    }
    
	/**
	 * Switch on the display (will display any characters that was previously written)
	 */
    public void displayOn() {
        changeBrightness(iBrightness);
    }
    
	/**
	 * Clear the characters on the display
	 */
    public void clearDisplay() {
		for (int i = 0; i < 6; i++)
			bDigits[i] = (byte)0;
        updateDisplay();
    }
    

	/**
	 * Displays the time
     * 
     * @param is24
	 *            If true, displays the time in 24 hr / military time format
	 * @param showDotColon
	 *            If true, the colon is displayed (useful for displays that have the colon connected)
	 */
    public void displayTime(boolean is24, boolean showDotColon) {
		LocalDateTime now = LocalDateTime.now();
        int iHour = now.getHour();
        if ((!is24) && (iHour > 12))
            iHour -= 12;
        int iMinute = now.getMinute();
        int iOut = (iHour * 100) + iMinute;

		displayInteger(iOut, showDotColon);
	}

	/**
	 * Displays the date
     * 
     * @param isMonthFirst
	 *            If true, displays the date in month first (i.e. American format)
	 */
    public void displayDate(boolean isMonthFirst) {
		displayDate(isMonthFirst, true);
	}

	/**
	 * Displays the date
     * 
     * @param isMonthFirst
	 *            If true, displays the date in month first (i.e. American format)
	 * @param showDotColon
	 *            If true, the colon is displayed (useful for displays that have the colon connected)
	 */
    public void displayDate(boolean isMonthFirst, boolean showDotColon) {
		LocalDateTime now = LocalDateTime.now();
        int iDay = now.getDayOfMonth();
        int iMonth = now.getMonthValue();

        String sData = iDay + "" + iMonth;
        if (iDay < 10)
            sData = " "+ sData;
        if (isMonthFirst) {
            sData = iMonth + "" + iDay;
            if (iMonth < 10)
                sData = " "+ sData;
        }

		displayString(sData, showDotColon);
	}

	/**
	 * Display a string (limited ASCII only)
     * 
     * @param sData
	 *            String of data to display, no longer than the number of digits on the display
	 */
    public void displayString(String sData) {
		displayString(sData, false);
	}
	
	/**
	 * Display a string (limited ASCII only), with or without colon/dots
     * 
     * @param sData
	 *            String of data to display, no longer than the number of digits on the display
	 * @param showDotColon
	 *            If true, the colon is displayed (useful for displays that have the colon connected)
	 */
    public void displayString(String sData, boolean showDotColon) {
        // If the dot/colon is to be displayed, add 0x80 to every digit data
        // Dot/Colon - some displays will have a dot enabled per segment
        // others will have a colon in the middle only (like a clock) - dots are not possible in this case
        int iPointAdd = 0;
        if (showDotColon)
            iPointAdd = COLON;

		if (sData.length() > maxDigits)
			throw new RuntimeIOException("String is too long, cannot exceed " + maxDigits);
		byte[] bData = sData.getBytes(StandardCharsets.US_ASCII);
		for (int i = 0; i < maxDigits; i++) {
			// Fill bDigits until the maxDigits, so if string is shorter, blank is written
			if (i < bData.length) {
				if ((((bData[i] - 32) & 0xFF) <= 90) || (bData[i] >= 32)) {
                    bDigits[i] = (byte)(displayASCIItoSeg[(bData[i] - 32) & 0xFF] + iPointAdd);
                } else {
                    bDigits[i] = (byte)(0 + iPointAdd);
                    Logger.warn("Could not display byte - outside of ASCII range 32 to 122 " + bData[i]);
                }
            } else
				bDigits[i] = (byte)(0 + iPointAdd);
		}
        // Do the display
        updateDisplay();
	}

	/**
	 * Display a string scrolling (limited ASCII only), with or without colon/dots
     * 
     * @param sData
	 *            String of data to display
	 * @param iSpeedMilli
	 *            Number of milliseconds to wait before scrolling to the next character. 500 to 1000 works nicely.
	 */
    public void scrollString(String sData, int iSpeedMilli) {
        String sDataFull = sData;
        String sDataOut = "";
        // Suffix with spaces
        for (int i = 0; i < maxDigits; i++)
            sDataFull += " ";
        int j = 0;
        while (j <= sData.length()) {
            sDataOut = sDataFull.substring(j, j + maxDigits);
            displayString(sDataOut, false);
            SleepUtil.sleepMillis(iSpeedMilli);
            j++;
        }
	}
	
	/**
	 * Display an integer number (as long is it isn't longer than the display)
     * 
     * @param iNumber
	 *            Integer number to display, no longer than the number of digits on the display
	 */
    public void displayInteger(int iNumber) {
        displayInteger(iNumber, false);
    }
	
	/**
	 * Display an integer number (as long is it isn't longer than the display), with or without colon/dots
     * 
     * @param iNumber
	 *            Integer number to display, no longer than the number of digits on the display
	 * @param showDotColon
	 *            If true, the colon is displayed (useful for displays that have the colon connected)
	 */
    public void displayInteger(int iNumber, boolean showDotColon) {
		int iMaxNumber = (int)((Math.pow(10, maxDigits)) - 1);
		int iMinNumber = (int)((Math.pow(-10, maxDigits - 1)) + 1);
		if ((iNumber > iMaxNumber) || (iNumber < iMinNumber))
			throw new RuntimeIOException("Cannot display range beyond " + iMaxNumber + " to " + iMinNumber);

		// This format will left pad the number with spaces
        String format = "%" + maxDigits + "d";
        String sData = String.format(format, Integer.valueOf(iNumber));
		
        // Do the display
        displayString(sData, showDotColon);
    }

	/**
	 * Display 4 numbers of data, with or without colon. Each number can only be 0 to 9
     * 
     * @param iData[]
	 *            Array of integer numbers to display, no longer than the number of digits on the display
	 * @param showDotColon
	 *            If true, the colon is displayed (useful for displays that have the colon connected)
	 */
    public void displayNumberDataSet(int iData[], boolean showDotColon) {
        // If the colon is to be displayed, add 0x80 to every digit data
        // Dot/Colon - some displays will have a dot enabled per segment
        // others will have a colon in the middle only (like a clock) - dots are not possible in this case
        int iPointAdd = 0;
        if (showDotColon)
            iPointAdd = COLON;
        
        // Loop through the max digits - validate first
        for (int i = 0; i < maxDigits; i++) {
			if ((iData[i] > 9) || (iData[i] < 0))
				throw new RuntimeIOException("Number can only be 0 to 9");
        }
        // Loop through the max digits
        for (int i = 0; i < maxDigits; i++) {
			// Set the internal byte array to the translated data
            // If the bytes sent is shorter than the display, fill the remaining characters as empty
            if (i < iData.length)
                bDigits[i] = (byte)(displayNumtoSeg[iData[i]] + iPointAdd);
            else
                bDigits[i] = (byte)(0 + iPointAdd);
        }
        // Do the display
        updateDisplay();
    }

	/**
	 * Display 1 number of data, with or without colon, in a specific digit location. Number can only be 0 to 9
     * 
     * @param iDigit
	 *            Digit to display on. 0 to maxDigits-1
     * @param iData
	 *            Integer number to display
	 * @param showDotColon
	 *            If true, the colon is displayed (useful for displays that have the colon connected)
	 */
    public void displayNumberDataDigit(int iDigit, int iData, boolean showDotColon) {
		if ((iData > 9) || (iData < 0))
			throw new RuntimeIOException("Number can only be 0 to 9");
		if ((iDigit >= maxDigits) || (iDigit < 0))
			throw new RuntimeIOException("Digit can only be 0 to " + (maxDigits-1));

		// If the colon is to be displayed, add 0x80 to every digit data
        // Dot/Colon - some displays will have a dot enabled per segment
        // others will have a colon in the middle only (like a clock) - dots are not possible in this case
        int iPointAdd = 0;
        if (showDotColon)
            iPointAdd = COLON;
        
        // Set the internal byte array to the translated data
        bDigits[iDigit] = (byte)(displayNumtoSeg[iData] + iPointAdd);
        // Do the display
        updateDisplay();
    }

	/**
	 * Simple test. Displays e.g. 0123 for 4 digit displays, 012345 for 6 digit displays
     */
    public void test() {
        // displays 012345
		for (int i = 0; i < maxDigits; i++)
			bDigits[i] = displayNumtoSeg[i];
        updateDisplay();
    }

	/**
	 * Change the brightness of the display
     * 
     * @param iBrightness
	 *            Brightness to change to, from 0 to 7
     */
    public void changeBrightness(int iBrightness) {
		if ((iBrightness > 7) || (iBrightness < 0))
			throw new RuntimeIOException("Brightness must be 0 to 7");
        this.iBrightness = iBrightness;
        // Brightness - 0 is lowest, 7 is highest
        // 1/16  = 0 [000]
        // 2/16  = 1 [001]
        // 4/16  = 2 [010]
        // 10/16 = 3 [011]
        // 11/16 = 4 [100]
        // 12/16 = 5 [101]
        // 13/16 = 6 [110]
        // 14/16 = 7 [111]

        // Write 0x40 [01000000] to indicate command to display data - [Write data to display register]
        doStartCondition();
        doByteWrite(bSetData);
        doStopCondition();

        doStartCondition();
        doByteWrite((byte)(0x88+iBrightness));
        doStopCondition();
    }

	/**
	 * Publish the internal bDigits to the display
     */
    private void updateDisplay() {
        // Write 0x40 [01000000] to indicate command to display data - [Write data to display register]
        doStartCondition();
        doByteWrite(bSetData);
        doStopCondition();

        // Specify the display address 0xC0 [11000000] then write out all 4 bytes
        doStartCondition();
        doByteWrite(bSetAddr);
		for (int i = 0; i < maxDigits; i++)
			doByteWrite(bDigits[i]);
        doStopCondition();

        // Write 0x88 [10001000] - Display ON, plus brightness
        doStartCondition();
        doByteWrite((byte)(bSetOn + iBrightness));
        doStopCondition();
    }

	/**
	 * Send the start condition
     */
    private void doStartCondition() {
        clkOut.on();
        sdaOut.on();
        sdaOut.off();
        clkOut.off();
    }

	/**
	 * Write one byte
     */
    private void doByteWrite(byte bWrite) {
        Logger.info("Sending " + Integer.toHexString(bWrite));
        for (int i = 0; i < 8; i++) {
            // Clock low
            clkOut.off();
            
            // Test bit of byte, data high or low
            if ((bWrite & 0x01) > 0)
                sdaOut.on();
            else
                sdaOut.off();
            
            // Shift bits to the left
            bWrite = (byte)(bWrite >> 1);
            // Clock high
            clkOut.on();
        }

        // Ack
        clkOut.off();
        if (doAckWait) {
            // NOTE: This currently does not appear to work, even with pullup resistor. SDA never goes false.
            // Close output
            sdaOut.close();
            // Open an input to check for ACK
            try (WaitableDigitalInputDevice input = new WaitableDigitalInputDevice(sdaPin, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH)) {

                clkOut.on();

                boolean notified = false;
                while (!notified) {
                	Logger.info("Waiting for ACK");
                	notified = input.waitForValue(false, 20);
                	Logger.info("Timed out? " + !notified);
                }
            } catch (RuntimeIOException ioe) {
                Logger.error(ioe, "Error: {}", ioe);
            } catch (InterruptedException e) {
            	Logger.error(e, "Error: {}", e);
            }

            sdaOut = new DigitalOutputDevice(sdaPin);
        } else {
            clkOut.on();
            // Sleeping 1ms is enough
            SleepUtil.sleepMillis(1);
        }
    }

	/**
	 * Send the stop condition
     */
    private void doStopCondition() {
        clkOut.off();
        sdaOut.off();
        clkOut.on();
        sdaOut.on();
    }

	@Override
	public void close() {
        sdaOut.close();
        clkOut.close();
    }
}
