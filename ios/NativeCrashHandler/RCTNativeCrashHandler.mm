//
//  RCTNativeCrashHandler.m
//  CrashHandlerApp
//
//  Created by Harsh Jaiswal on 17/02/26.
//

#import "RCTNativeCrashHandler.h"
#import "RCTAppDelegate.h" // RCTAppDelegate must be imported before YourAppName-Swift.h
#import "CrashHandlerApp-Swift.h"

@implementation RCTNativeCrashHandler

RCT_EXPORT_MODULE(NativeCrashHandler)

- (void)captureJSException:(NSString *)stack {
  dispatch_async(dispatch_get_main_queue(), ^{
    NSException *exception =
        [NSException exceptionWithName:@"JavaScriptException"
                                reason:stack
                              userInfo:nil];
    [[CrashScreenshotService shared] handleCrashWithException:exception
                                                       signal:0];
  });
}

- (void)triggerNativeCrash {
  // We use a delayed dispatch to ensure this crash happens OUTSIDE of the
  // React Native TurboModule call stack, which usually has try-catch blocks
  // that swallow native exceptions during development.
  dispatch_after(
      dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.1 * NSEC_PER_SEC)),
      dispatch_get_main_queue(), ^{
        // Trigger an out-of-bounds exception
        NSArray *array = @[];
        id item = array[1];
        NSLog(@"%@", item);
      });
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params {
  return std::make_shared<facebook::react::NativeCrashHandlerSpecJSI>(params);
}

@end
