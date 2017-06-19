package com.nilhcem.ledcontrol;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

/**
 * Port of Arduino's LedControl library
 */
public class LedControl implements AutoCloseable {

    //the opcodes for the MAX7221 and MAX7219
    private static final byte OP_NOOP = 0;
    private static final byte OP_DIGIT0 = 1;
    private static final byte OP_DIGIT1 = 2;
    private static final byte OP_DIGIT2 = 3;
    private static final byte OP_DIGIT3 = 4;
    private static final byte OP_DIGIT4 = 5;
    private static final byte OP_DIGIT5 = 6;
    private static final byte OP_DIGIT6 = 7;
    private static final byte OP_DIGIT7 = 8;
    private static final byte OP_DECODEMODE = 9;
    private static final byte OP_INTENSITY = 10;
    private static final byte OP_SCANLIMIT = 11;
    private static final byte OP_SHUTDOWN = 12;
    private static final byte OP_DISPLAYTEST = 15;

    /**
     * Segments to be switched on for characters and digits on 7-Segment Displays
     */
    private static final byte[] CHAR_TABLE = new byte[]{
            (byte) 0b01111110, (byte) 0b00110000, (byte) 0b01101101, (byte) 0b01111001, (byte) 0b00110011, (byte) 0b01011011, (byte) 0b01011111, (byte) 0b01110000,
            (byte) 0b01111111, (byte) 0b01111011, (byte) 0b01110111, (byte) 0b00011111, (byte) 0b00001101, (byte) 0b00111101, (byte) 0b01001111, (byte) 0b01000111,
            (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b10000000, (byte) 0b00000001, (byte) 0b10000000, (byte) 0b00000000,
            (byte) 0b01111110, (byte) 0b00110000, (byte) 0b01101101, (byte) 0b01111001, (byte) 0b00110011, (byte) 0b01011011, (byte) 0b01011111, (byte) 0b01110000,
            (byte) 0b01111111, (byte) 0b01111011, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b00000000, (byte) 0b01110111, (byte) 0b00011111, (byte) 0b00001101, (byte) 0b00111101, (byte) 0b01001111, (byte) 0b01000111, (byte) 0b00000000,
            (byte) 0b00110111, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00001110, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b01100111, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00001000,
            (byte) 0b00000000, (byte) 0b01110111, (byte) 0b00011111, (byte) 0b00001101, (byte) 0b00111101, (byte) 0b01001111, (byte) 0b01000111, (byte) 0b00000000,
            (byte) 0b00110111, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00001110, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b01100111, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
            (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000
    };

    private SpiDevice spiDevice;

    /* The array for shifting the data to the devices */
    private byte[] spidata = new byte[16];

    /* We keep track of the led-status for all 8 devices in this array */
    private byte[] status = new byte[64];

    /* The maximum number of devices we use */
    private int maxDevices;

    public LedControl(String spiGpio, int numDevices) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        spiDevice = pioService.openSpiDevice(spiGpio);
        spiDevice.setMode(SpiDevice.MODE0);
        spiDevice.setFrequency(1000000); // 1MHz
        spiDevice.setBitsPerWord(8); // 8 BPW
        spiDevice.setBitJustification(false); // MSB first

        maxDevices = numDevices;
        if (numDevices < 1 || numDevices > 8) {
            maxDevices = 8;
        }

        for (int i = 0; i < maxDevices; i++) {
            spiTransfer(i, OP_DISPLAYTEST, 0);
            setScanLimit(i, 7); // scanlimit: 8 LEDs
            spiTransfer(i, OP_DECODEMODE, 0); // decoding： BCD
            clearDisplay(i);
            // we go into shutdown-mode on startup
            shutdown(i, true);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            spiDevice.close();
        } finally {
            spiDevice = null;
        }
    }

    /**
     * Get the number of devices attached to this LedControl.
     *
     * @return the number of devices on this LedControl
     */
    public int getDeviceCount() {
        return maxDevices;
    }

    /**
     * Set the shutdown (power saving) mode for the device
     *
     * @param addr   the address of the display to control
     * @param status if true the device goes into power-down mode. Set to false for normal operation.
     */
    public void shutdown(int addr, boolean status) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }

        spiTransfer(addr, OP_SHUTDOWN, status ? 0 : 1);
    }

    /**
     * Set the number of digits (or rows) to be displayed.
     * <p>
     * See datasheet for sideeffects of the scanlimit on the brightness of the display
     * </p>
     *
     * @param addr  the address of the display to control
     * @param limit number of digits to be displayed (1..8)
     */
    public void setScanLimit(int addr, int limit) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }

        if (limit >= 0 || limit < 8) {
            spiTransfer(addr, OP_SCANLIMIT, limit);
        }
    }

    /**
     * Set the brightness of the display
     *
     * @param addr      the address of the display to control
     * @param intensity the brightness of the display. (0..15)
     */
    public void setIntensity(int addr, int intensity) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }

        if (intensity >= 0 || intensity < 16) {
            spiTransfer(addr, OP_INTENSITY, intensity);
        }
    }

    /**
     * Switch all Leds on the display off
     *
     * @param addr the address of the display to control
     */
    public void clearDisplay(int addr) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }

        int offset = addr * 8;
        for (int i = 0; i < 8; i++) {
            status[offset + i] = 0;
            spiTransfer(addr, (byte) (OP_DIGIT0 + i), status[offset + i]);
        }
    }

    /**
     * Set the status of a single Led
     *
     * @param addr  the address of the display to control
     * @param row   the row of the Led (0..7)
     * @param col   the column of the Led (0..7)
     * @param state if true the led is switched on, if false it is switched off
     * @throws IOException
     */
    public void setLed(int addr, int row, int col, boolean state) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }
        if (row < 0 || row > 7 || col < 0 || col > 7) {
            return;
        }

        int offset = addr * 8;
        byte val = (byte) (0b10000000 >> col);
        if (state) {
            status[offset + row] = (byte) (status[offset + row] | val);
        } else {
            val = (byte) ~val;
            status[offset + row] = (byte) (status[offset + row] & val);
        }
        spiTransfer(addr, (byte) (OP_DIGIT0 + row), status[offset + row]);
    }

    /**
     * Set all 8 Led's in a row to a new state
     *
     * @param addr  the address of the display to control
     * @param row   row which is to be set (0..7)
     * @param value each bit set to 1 will light up the corresponding Led.
     */
    public void setRow(int addr, int row, byte value) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }
        if (row < 0 || row > 7) {
            return;
        }

        int offset = addr * 8;
        status[offset + row] = value;
        spiTransfer(addr, (byte) (OP_DIGIT0 + row), status[offset + row]);
    }

    /**
     * Set all 8 Led's in a column to a new state
     *
     * @param addr  the address of the display to control
     * @param col   column which is to be set (0..7)
     * @param value each bit set to 1 will light up the corresponding Led.
     */
    public void setColumn(int addr, int col, byte value) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }
        if (col < 0 || col > 7) {
            return;
        }

        byte val;
        for (int row = 0; row < 8; row++) {
            val = (byte) (value >> (7 - row));
            setLed(addr, row, col, (val & 0x01) == 0x01);
        }
    }

    /**
     * Display a hexadecimal digit on a 7-Segment Display
     *
     * @param addr  the address of the display to control
     * @param digit the position of the digit on the display (0..7)
     * @param value the value to be displayed. (0x00..0x0F. 0x10 to clear digit)
     * @param dp    sets the decimal point.
     */
    public void setDigit(int addr, int digit, byte value, boolean dp) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }
        if (digit < 0 || digit > 7 || value > 16) {
            return;
        }

        int offset = addr * 8;
        byte v = CHAR_TABLE[value];
        if (dp) {
            v |= 0b10000000;
        }
        status[offset + digit] = v;
        spiTransfer(addr, (byte) (OP_DIGIT0 + digit), v);
    }

    /**
     * Display a character on a 7-Segment display.
     * <pre>
     * There are only a few characters that make sense here :
     * '0','1','2','3','4','5','6','7','8','9','0',
     * 'A','b','c','d','E','F','H','L','P',
     * '.','-','_',' '
     * </pre>
     *
     * @param addr  the address of the display to control
     * @param digit the position of the character on the display (0..7)
     * @param value the character to be displayed.
     * @param dp    sets the decimal point.
     */
    public void setChar(int addr, int digit, char value, boolean dp) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }
        if (digit < 0 || digit > 7) {
            return;
        }

        int offset = addr * 8;
        int index = value;
        if (index >= CHAR_TABLE.length) {
            // no defined beyond index 127, so we use the space char
            index = 32;
        }
        byte v = CHAR_TABLE[index];
        if (dp) {
            v |= 0b10000000;
        }
        status[offset + digit] = v;
        spiTransfer(addr, (byte) (OP_DIGIT0 + digit), v);
    }

    /**
     * Draw the given bitmap to the LED matrix.
     *
     * @param addr   the address of the display to control
     * @param bitmap Bitmap to draw
     * @throws IOException
     */
    public void draw(int addr, Bitmap bitmap) throws IOException {
        if (addr < 0 || addr >= maxDevices) {
            return;
        }

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 8, 8, true);
        for (int row = 0; row < 8; row++) {
            int value = 0;
            for (int col = 0; col < 8; col++) {
                value |= scaled.getPixel(col, row) == Color.WHITE ? (0x80 >> col) : 0;
            }
            setRow(addr, row, (byte) value);
        }
    }

    /**
     * Send out a single command to the device
     */
    private void spiTransfer(int addr, byte opcode, int data) throws IOException {
        int offset = addr * 2;
        int maxbytes = maxDevices * 2;

        for (int i = 0; i < maxbytes; i++) {
            spidata[i] = (byte) 0;
        }

        // put our device data into the array
        spidata[maxbytes - offset - 2] = opcode;
        spidata[maxbytes - offset - 1] = (byte) data;
        spiDevice.write(spidata, maxbytes);
    }
}
