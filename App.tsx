import React, { useEffect } from 'react';
import {
  StatusBar,
  useColorScheme,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  LogBox
} from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import initCrashHandler from './src/utils/crashHandler';
import NativeCrashHandler from './specs/NativeCrashHandler';

LogBox.ignoreAllLogs();

function App() {
  const isDarkMode = useColorScheme() === 'dark';

  useEffect(() => {
    initCrashHandler();
  }, []);

  const triggerJSCrash = () => {
    throw new Error('Manual JS Crash triggered for testing');
  };

  const triggerNativeCrash = () => {
    if (NativeCrashHandler) {
      NativeCrashHandler.triggerNativeCrash();
    }
  };

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
        <View style={styles.content}>
          <Text style={styles.title}>Visual Crash Reporting</Text>

          <TouchableOpacity
            style={[styles.button, { backgroundColor: '#FF3B30' }]}
            onPress={triggerJSCrash}
          >
            <Text style={styles.buttonText}>Trigger JS Crash</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.button, { backgroundColor: '#FF9500' }]}
            onPress={triggerNativeCrash}
          >
            <Text style={styles.buttonText}>Trigger Native Crash</Text>
          </TouchableOpacity>

          <Text style={styles.hint}>
            Crashes will capture a screenshot and save it to the app's local storage.
          </Text>
        </View>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 40,
    color: '#1C1C1E',
  },
  button: {
    width: '100%',
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 16,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '600',
  },
  hint: {
    marginTop: 20,
    color: '#8E8E93',
    textAlign: 'center',
  }
});

export default App;