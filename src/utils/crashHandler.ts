import NativeCrashHandler from '../../specs/NativeCrashHandler';

const initCrashHandler = () => {
    const defaultHandler =
        (ErrorUtils as any).getGlobalHandler && (ErrorUtils as any).getGlobalHandler();

    (ErrorUtils as any).setGlobalHandler(
        (error: any, isFatal?: boolean) => {
            // Send error details to native module
            if (NativeCrashHandler) {
                NativeCrashHandler.captureJSException(
                    error.stack || error.message || 'Unknown JS Error'
                );
            }

            // Call default handler to maintain standard RN behavior (like redbox in dev)
            if (defaultHandler) {
                defaultHandler(error, isFatal);
            }
        }
    );
};

export default initCrashHandler;