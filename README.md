# MAX7219 Led Matrix Module sample for Android Things

- Android Things Port of the Arduino LedControl library
- Sample project

![photo][]

## Schematic

![schematic][]

## Usage

### Setup

```java
try {
    ledControl = new LedControl("SPI0.0");
    ledControl.setIntensity(1);
} catch (IOException e) {
    Log.e(TAG, "Error initializing LED matrix", e);
}
// Don't forget to call ledControl.close() when you are done.
```


### Turn on one pixel at {row:2, col:3}

```java
ledControl.setLed(2, 3, true);
```


### Show a bitmap

```java
Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.smiley);
ledControl.draw(bmp);
```


### Show a space invader

```java
byte[] invader = Invaders.ALIEN_1_FRAME_1;
for (int i = 0; i < invader.length; i++) {
    ledControl.setRow(i, invader[i]);
}
```


### Show "42.ABCDEF" on a MAX7219 8-digit module

```java
ledControl.setDigit(7, (byte) 0x04, false);
ledControl.setDigit(6, (byte) 0x02, true);
ledControl.setDigit(5, (byte) 0x0A, false);
ledControl.setDigit(4, (byte) 0x0B, false);
ledControl.setDigit(3, (byte) 0x0C, false);
ledControl.setDigit(2, (byte) 0x0D, false);
ledControl.setDigit(1, (byte) 0x0E, false);
ledControl.setDigit(0, (byte) 0x0F, false);
```


### Show "123456" on a MAX7219 8-digit module

```java
int curValue = 123456;
for (int i = 0; i < 8; i++) {
    byte value = (byte) ((i != 0 && curValue == 0) ? 16 : (curValue % 10));
    ledControl.setDigit(i, value, false);
    curValue /= 10;
}
```


Credits to [https://learn.adafruit.com/trinket-slash-gemma-space-invader-pendant/animation](https://learn.adafruit.com/trinket-slash-gemma-space-invader-pendant/animation) for the Space invaders animation


## License

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[photo]: https://raw.githubusercontent.com/Nilhcem/ledcontrol-androidthings/master/preview.gif
[schematic]: https://raw.githubusercontent.com/Nilhcem/ledcontrol-androidthings/master/schematic.png
