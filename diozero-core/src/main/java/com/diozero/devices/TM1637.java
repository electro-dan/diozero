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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.diozero.api.DigitalOutputDevice;

/*
https://github.com/Bogdanel/Raspberry-Pi-Python-3-TM1637-Clock/blob/master/tm1637.py
https://github.com/depklyon/raspberrypi-python-tm1637/blob/master/tm1637.py
https://github.com/rwbl/jTM1637
*/

public class TM1637 implements AutoCloseable {
	
	// Used to output the segments from numbers
    final byte displayNumtoSeg[] = {0x3f, 0x06, 0x5b, 0x4f, 0x66, 0x6d, 0x7d, 0x07, 0x7f, 0x6f};
	// Used to output the segments from ascii
	// This table, taken from http://www.ccsinfo.com/forum/viewtopic.php?p=57034 is ideal for writing the converted character out
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
	
	private DigitalOutputDevice clkOut;
    private DigitalOutputDevice sdaOut;

    public TM1637(int sdaPin, int clkPin, int maxDigits) {
        this.sdaPin = sdaPin;
		this.clkPin = clkPin;
		// Some displays are 4 digits, some are 6 digits
		if ((maxDigits > 6) || (maxDigits < 0))
			throw new RuntimeIOException("Maximum number of digits can only be 0 to 6");
		this.maxDigits = maxDigits;

        clkOut = new DigitalOutputDevice(clkPin);
        sdaOut = new DigitalOutputDevice(sdaPin);
    }

    public void init() {
        clkOut.on();
        sdaOut.on();

        clearDisplay();
    }

    public void clearDisplay() {
		for (int i = 0; i < 6; i++)
			bDigits[i] = (byte)0;
        updateDisplay();
    }
    
    // Displays the time
    public void displayTime(boolean is24, boolean showColon) {
		String pattern = "Hmm";
		if (!is24)
			pattern = "hmm";
		LocalDateTime now = LocalDateTime.now();

		String sData = now.format(DateTimeFormatter.ofPattern(pattern));
		if (now.getHour() < 10)
			sData = " " + sData;
		displayString(sData, true);
	}

	public void displayDate(boolean isMonthFirst) {
		displayDate(isMonthFirst, false);
	}

	// Displays the date
    public void displayDate(boolean isMonthFirst, boolean showColon) {
		String pattern = "dM";
		if (isMonthFirst)
			pattern = "Md";
		LocalDateTime now = LocalDateTime.now();

		String sData = now.format(DateTimeFormatter.ofPattern(pattern));
		if (now.getDayOfMonth() < 10)
			sData = " " + sData;
		displayString(sData, true);
	}

	// Display a string
    public void displayString(String sData) {
		displayString(sData, false);
	}
	
	// Display a string, with or without colon
    public void displayString(String sData, boolean showColon) {
        // If the colon is to be displayed, add 0x80 to every digit data
        int iPointAdd = 0;
        if (showColon)
            iPointAdd = COLON;

		if (sData.length() > maxDigits)
			throw new RuntimeIOException("String is too long, cannot exceed " + maxDigits);
		byte[] bData = sData.getBytes(StandardCharsets.US_ASCII);
		for (int i = 0; i < maxDigits; i++) {
			// Fill bDigits until the maxDigits, so if string is shorter, blank is written
			if (i < bData.length)
				bDigits[i] = (byte)(displayASCIItoSeg[bData[i]] + iPointAdd);
			else
				bDigits[i] = (byte)(0 + iPointAdd);
		}
        // Do the display
        updateDisplay();
	}
	
	// Display 4 bytes of data, with or without colon
    public void displayInteger(int iNumber) {
		int iMaxNumber = (10 ^ maxDigits) - 1;
		int iMinNumber = (-10 ^ (maxDigits-1)) + 1;
		if ((iNumber > iMaxNumber || (iNumber < iMinNumber))
			throw new RuntimeIOException("Cannot display range beyond " + iMaxNumber + " to " + iMinNumber);

		String format = "%" + ((10 ^ maxDigits)/10);
		String sData = String.format(format, iNumber);
			
        // Do the display
        displayString(sData, false);
    }

	// Display 4 bytes of data, with or without colon
    public void displayNumberDataSet(int iData[], boolean showColon) {
        // If the colon is to be displayed, add 0x80 to every digit data
        int iPointAdd = 0;
        if (showColon)
            iPointAdd = COLON;
        
        // Loop through the max digits - validate first
        for (int i = 0; i < maxDigits; i++) {
			if ((iData[i] > 9) || (iData[i] < 0))
				throw new RuntimeIOException("Number can only be 0 to 9");
        }
        // Loop through the max digits
        for (int i = 0; i < maxDigits; i++) {
			// Set the internal byte array to the translated data
            bDigits[i] = (byte)(displayNumtoSeg[iData[i]] + iPointAdd);
        }
        // Do the display
        updateDisplay();
    }

    public void displayNumberDataDigit(int iDigit, int iData, boolean showColon) {
		if ((iData > 9) || (iData < 0))
			throw new RuntimeIOException("Number can only be 0 to 9");
		if ((iDigit > maxDigits) || (iDigit < 0))
			throw new RuntimeIOException("Digit can only be 0 to " + maxDigits);

		// If the colon is to be displayed, add 0x80 to every digit data
        int iPointAdd = 0;
        if (showColon)
            iPointAdd = COLON;
        
        // Set the internal byte array to the translated data
        bDigits[iDigit] = (byte)(displayNumtoSeg[iData] + iPointAdd);
        // Do the display
        updateDisplay();
    }

    public void test() {
        // displays 012345
		for (int i = 0; i < maxDigits; i++)
			bDigits[i] = displayNumtoSeg[i];
        updateDisplay();
    }

    public void changeBrightness(int iBrightness) {
        // Brightness - 0 is lowest, 7 is highest
        // 1/16  = 0 [000]
        // 2/16  = 1 [001]
        // 4/16  = 2 [010]
        // 10/16 = 3 [011]
        // 11/16 = 4 [100]
        // 12/16 = 5 [101]
        // 13/16 = 6 [110]
        // 14/16 = 7 [111]
        doStartCondition();
        doByteWrite((byte)(0x88+iBrightness));
        doStopCondition();
    }

    // Publish the internal bDigits to the display
    public void updateDisplay() {
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

    public void doStartCondition() {
        clkOut.on();
        sdaOut.on();
        sdaOut.off();
        clkOut.off();
    }

    public void doStopCondition() {
        clkOut.off();
        sdaOut.off();
        clkOut.on();
        sdaOut.on();
    }

    public void doByteWrite(byte bWrite) {
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
        sdaOut.on();
        // Close output
        //sdaOut.close();
        clkOut.on();

        // Open an input to check for ACK
        /*try (WaitableDigitalInputDevice input = new WaitableDigitalInputDevice(sdaPin, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH)) {
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
        sdaOut = new DigitalOutputDevice(sdaPin);*/

        // Sleeping 1ms is enough
        SleepUtil.sleepMillis(1);
        sdaOut.off();
    }

	@Override
	public void close() {
        sdaOut.close();
        clkOut.close();
    }
}
