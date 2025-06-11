import { NativeModules, Platform } from 'react-native';

const isAndroid = Platform.OS === 'android';
const SmsWatcherModule = isAndroid ? NativeModules.SmsWatcherModule : null;

export function setWatchedNumbers(numbers: string[]) {
  if (!isAndroid || !SmsWatcherModule) return;
  SmsWatcherModule.setTargetNumbers(numbers);
}

export function addWatchedNumber(number: string) {
  if (!isAndroid || !SmsWatcherModule) return;
  SmsWatcherModule.addTargetNumber(number);
}

export function removeWatchedNumber(number: string) {
  if (!isAndroid || !SmsWatcherModule) return;
  SmsWatcherModule.removeTargetNumber(number);
}

export function clearWatchedNumbers() {
  if (!isAndroid || !SmsWatcherModule) return;
  SmsWatcherModule.clearTargetNumbers();
}

export function getWatchedNumbers(): Promise<string[]> {
  if (!isAndroid || !SmsWatcherModule) {
    return Promise.resolve([]);
  }
  return SmsWatcherModule.getTargetNumbers();
}
