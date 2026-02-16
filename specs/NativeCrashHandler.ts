import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
    captureJSException(stack: string): void;
    triggerNativeCrash(): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NativeCrashHandler');