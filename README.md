# react-native-sms-watcher

**A React Native library to watch incoming SMS by specific sender numbers, supporting both foreground and background (Headless JS) cases.**

---

## Features

- **Dynamic Listening**: Subscribe to SMS events while the app is in the foreground.
- **Headless JS**: Handle SMS even when the app is in the background or closed.
- **Filter by Sender**: Only process messages from watched phone numbers.
- **JavaScript API**: Manage watched numbers list at runtime.
- **Autolinking**: Zero manual linking for React Native ≥0.60.

---

## Installation

```bash
# yarn
yarn add react-native-sms-watcher

# npm
npm install react-native-sms-watcher --save
```

> React Native ≥0.60 uses autolinking; no need to run `react-native link`.

---

## Android Setup

### Permissions

In `android/app/src/main/AndroidManifest.xml`, ensure:

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
```

---

## JavaScript API

Import the module:

```js
import {
  setWatchedNumbers,
  addWatchedNumber,
  removeWatchedNumber,
  clearWatchedNumbers,
  getWatchedNumbers,
} from 'react-native-sms-watcher';
```

### Managing Watched Numbers

```js
// Replace entire list
setWatchedNumbers(['+1234567890', '+1987654321']);

// Add one
addWatchedNumber('+2233445566');

// Remove one
removeWatchedNumber('+1234567890');

// Clear all
clearWatchedNumbers();

// Fetch current list (async)
const list = await getWatchedNumbers();
console.log(list);
```

### Subscribing to SS in Foreground

## Headless JS Support

To handle SMS when the app is closed or in background, register a Headless Task. Add this import in your app’s entrypoint (`index.js`):

```js
import { AppRegistry } from 'react-native';

AppRegistry.registerHeadlessTask('SmsBackgroundTask', () => async (data) => {
  const { message } = data;
  Alert.alert('New message!', message);
});
```

---

## Example App Usage

In your `App.js`:

```js
import React, { useEffect } from 'react';
import { View, Button, Alert } from 'react-native';
import {
  setWatchedNumbers,
  subscribeSms,
  getWatchedNumbers,
  clearWatchedNumbers,
} from 'react-native-sms-watcher';

export default function App() {
  useEffect(() => {
    setWatchedNumbers(['6505551213']);
  }, []);

  const showList = async () => {
    const list = await getWatchedNumbers();
    Alert.alert('Watched', list.join(', '));
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Button title="Show Watched" onPress={showList} />
      <Button title="Clear All" onPress={clearWatchedNumbers} />
    </View>
  );
}
```

---

## License

MIT © Erfan Alimardani
