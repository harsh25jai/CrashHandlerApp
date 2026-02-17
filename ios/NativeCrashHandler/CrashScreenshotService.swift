//
//  CrashScreenshotService.swift
//  CrashHandlerApp
//
//  Created by Harsh Jaiswal on 17/02/26.
//

import Foundation
import UIKit

@objc class CrashScreenshotService: NSObject {
    @objc static let shared = CrashScreenshotService()
    
    private let SCREENSHOT_DIR = "crash_screenshots"
    
    @objc func initialize() {
        NSLog("CrashHandler: Initializing service")
        NSSetUncaughtExceptionHandler { exception in
            NSLog("CrashHandler: Uncaught exception caught: %@", exception.name.rawValue)
            CrashScreenshotService.shared.handleCrash(exception: exception)
        }
        
        // Handle signals
        let signals = [SIGABRT, SIGILL, SIGSEGV, SIGFPE, SIGBUS, SIGPIPE]
        for s in signals {
            signal(s) { sig in
                NSLog("CrashHandler: Signal %d received", sig)
                CrashScreenshotService.shared.handleSignal(sig)
            }
        }
    }
    
    func handleSignal(_ signal: Int32) {
        handleCrash(exception: nil, signal: signal)
    }
    
    @objc func handleCrash(exception: NSException?, signal: Int32 = 0) {
        let crashDesc = (exception != nil) ? "Exception: \(exception!.name.rawValue)" : "Signal \(signal)"
        NSLog("CrashHandler: Handling %@", crashDesc)
        
        let captureBlock = {
            NSLog("CrashHandler: Attempting screenshot capture")
            let screenshot = self.captureScreenshot()
            if let image = screenshot {
                NSLog("CrashHandler: Screenshot captured, saving...")
                self.saveScreenshot(image: image, exception: exception, signal: signal)
            } else {
                NSLog("CrashHandler: Could not capture screenshot")
            }
        }
        
        if Thread.isMainThread {
            captureBlock()
        } else {
            let semaphore = DispatchSemaphore(value: 0)
            DispatchQueue.main.async {
                captureBlock()
                semaphore.signal()
            }
            _ = semaphore.wait(timeout: .now() + 2.0)
        }
        
        if signal != 0 {
            NSLog("CrashHandler: Terminating process due to signal %d", signal)
            exit(signal)
        }
    }
    
    private func captureScreenshot() -> UIImage? {
        let windows = UIApplication.shared.windows
        let window = windows.first(where: { $0.isKeyWindow }) 
                     ?? UIApplication.shared.keyWindow
                     ?? windows.first
        
        guard let validWindow = window else { 
            NSLog("CrashHandler: No window found for capture")
            return nil 
        }
        
        NSLog("CrashHandler: Capturing window: %@", validWindow.description)
        
        let renderer = UIGraphicsImageRenderer(size: validWindow.bounds.size)
        return renderer.image { context in
            validWindow.drawHierarchy(in: validWindow.bounds, afterScreenUpdates: false)
        }
    }
    
    private func saveScreenshot(image: UIImage, exception: NSException?, signal: Int32) {
        guard let data = image.jpegData(compressionQuality: 0.7) else { 
            NSLog("CrashHandler: Failed to convert image to JPEG")
            return 
        }
        
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else { 
            NSLog("CrashHandler: Could not find documents directory")
            return 
        }
        
        let directoryURL = documentsURL.appendingPathComponent(SCREENSHOT_DIR)
        
        do {
            try fileManager.createDirectory(at: directoryURL, withIntermediateDirectories: true)
        } catch {
            NSLog("CrashHandler: Failed to create directory: %@", error.localizedDescription)
        }
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd_HH-mm-ss"
        let timestamp = formatter.string(from: Date())
        
        let crashName = exception?.name.rawValue ?? "Signal_\(signal)"
        let filename = "\(timestamp)_\(crashName).jpg"
        let fileURL = directoryURL.appendingPathComponent(filename)
        
        do {
            try data.write(to: fileURL)
            NSLog("CrashHandler: Screenshot saved to %@", fileURL.path)
        } catch {
            NSLog("CrashHandler: Failed to save screenshot: %@", error.localizedDescription)
        }
    }
}
