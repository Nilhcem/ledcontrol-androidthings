# MAX7219 / MAX7221 driver for Android Things

A port of the LedControl Arduino library for Android Things.

![photo][]

## Download

```groovy
dependencies {
    compile 'com.nilhcem.androidthings:driver-max72xx:0.0.2'
}
```

## Usage

### Setup

```java
try {
    ledControl = new LedControl("SPI0.0", 1); // second parameter is the number of chained matrices. Here, we only use 1 LED matrix module (8x8).
    for (int i = 0; i < ledControl.getDeviceCount(); i++) {
        ledControl.setIntensity(i, 3);
        ledControl.shutdown(i, false);
        ledControl.clearDisplay(i);
    }
} catch (IOException e) {
    Log.e(TAG, "Error initializing LED matrix", e);
}
// Don't forget to call ledControl.close() when you are done.
```


### Turn on one pixel on matrix #0 at {row:2, col:3}

```java
ledControl.setLed(0, 2, 3, true);
```


### Show a bitmap on matrix #0

```java
Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.smiley);
ledControl.draw(0, bmp);
```


### Draw a single bitmap on multiple devices
```java
// Here, we're drawing a [width=32, height=8] bitmap on a "4 in 1" display module
Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.hearts32x8);
ledControl.draw(bmp);
```


### Show "42.ABCDEF" on a MAX7219 8-digit module

```java
ledControl.setDigit(0, 7, (byte) 0x04, false);
ledControl.setDigit(0, 6, (byte) 0x02, true);
ledControl.setDigit(0, 5, (byte) 0x0A, false);
ledControl.setDigit(0, 4, (byte) 0x0B, false);
ledControl.setDigit(0, 3, (byte) 0x0C, false);
ledControl.setDigit(0, 2, (byte) 0x0D, false);
ledControl.setDigit(0, 1, (byte) 0x0E, false);
ledControl.setDigit(0, 0, (byte) 0x0F, false);
```


### Show "123456" on a MAX7219 8-digit module

```java
int curValue = 123456;
for (int i = 0; i < 8; i++) {
    byte value = (byte) ((i != 0 && curValue == 0) ? 16 : (curValue % 10));
    ledControl.setDigit(0, i, value, false);
    curValue /= 10;
}
```


## Schematic

![schematic][]


Credits to [https://learn.adafruit.com/trinket-slash-gemma-space-invader-pendant/animation][invaders] for the Space invaders animation, [http://wdi.supelec.fr/boulanger/MicroPython/][fritzing] for the fritzing part.

[photo]: https://raw.githubusercontent.com/Nilhcem/ledcontrol-androidthings/master/assets/preview.gif
[schematic]: https://raw.githubusercontent.com/Nilhcem/ledcontrol-androidthings/master/assets/schematic.png
[invaders]: https://learn.adafruit.com/trinket-slash-gemma-space-invader-pendant/animation
[fritzing]: http://wdi.supelec.fr/boulanger/MicroPython/
