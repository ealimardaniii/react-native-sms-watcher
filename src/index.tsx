import { NativeModules, Platform } from 'react-native';
const { SmsWatcherModule } = NativeModules;

export function setWatchedNumbers(numbers: string[]) {
  SmsWatcherModule.setTargetNumbers(numbers);
}

export function addWatchedNumber(number: string) {
  SmsWatcherModule.addTargetNumber(number);
}

export function removeWatchedNumber(number: string) {
  SmsWatcherModule.removeTargetNumber(number);
}

export function clearWatchedNumbers() {
  SmsWatcherModule.clearTargetNumbers();
}

export function getWatchedNumbers(): Promise<string[]> {
  if (Platform.OS !== 'android') {
    return Promise.resolve([]);
  }
  return SmsWatcherModule.getTargetNumbers();
}
