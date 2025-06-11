import { Alert, AppRegistry } from 'react-native';
import App from './src/App';
import { name as appName } from './app.json';

AppRegistry.registerHeadlessTask('SmsBackgroundTask', () => async (data) => {
  const { message } = data;
  Alert.alert('New message!', message);
});

AppRegistry.registerComponent(appName, () => App);
