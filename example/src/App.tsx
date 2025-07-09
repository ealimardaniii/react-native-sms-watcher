import { useEffect, useState } from 'react';
import {
  View,
  PermissionsAndroid,
  StyleSheet,
  Button,
  Alert,
  TextInput,
  SafeAreaView,
  StatusBar,
  NativeModules,
  Platform,
} from 'react-native';
import {
  addWatchedNumber,
  clearWatchedNumbers,
  getWatchedNumbers,
} from 'react-native-sms-watcher';

export default function App() {
  const [number, setNumber] = useState('');

  useEffect(() => {
    PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.RECEIVE_SMS);
  }, []);

  const handlePressWatchedNumbers = () => {
    getWatchedNumbers()
      .then((list) => {
        Alert.alert('Watched numbers', JSON.stringify(list));
      })
      .catch(() => {
        Alert.alert('Error occured');
      });
  };

  const handlePressAddWatchedNumber = () => {
    addWatchedNumber(number);
    warmUpHeadlessTask();
    setNumber('');
  };

  const warmUpHeadlessTask = () => {
    if (Platform.OS === 'android' && NativeModules.HeadlessTaskStarter) {
      NativeModules.HeadlessTaskStarter.start();
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <Button title="Get watched numbers" onPress={handlePressWatchedNumbers} />
      <View style={styles.separator} />
      <TextInput
        value={number}
        onChangeText={setNumber}
        placeholder="Enter number"
        style={styles.input}
        keyboardType="phone-pad"
      />
      <Button
        disabled={!number}
        title="Add watched number"
        onPress={handlePressAddWatchedNumber}
      />
      <View style={styles.separator} />
      <Button title="Clear watched numbers" onPress={clearWatchedNumbers} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    flex: 1,
  },
  separator: {
    marginBottom: 12,
  },
  input: {
    borderRadius: 4,
    borderWidth: 1,
    borderColor: 'grey',
    width: '50%',
  },
});
