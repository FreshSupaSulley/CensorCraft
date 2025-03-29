#import "MicrophoneAccess.h"
#include <iostream>
#include <AVFoundation/AVFoundation.h>
#import <Cocoa/Cocoa.h>
#import <Foundation/Foundation.h>
#import <AppKit/AppKit.h>
#import <Speech/Speech.h>

const char* getMicAuthStatus() {
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeAudio];
    switch (status) {
        case AVAuthorizationStatusNotDetermined:
            // The user hasn't decided yet
            return "NotDetermined";
        case AVAuthorizationStatusAuthorized:
            // The user has authorized access
            return "Authorized";
        case AVAuthorizationStatusDenied:
            // The user has denied access
            return "Denied";
        case AVAuthorizationStatusRestricted:
            // Access is restricted (e.g., parental controls)
            return "Restricted";
        default:
            return "Unknown";
    }
}

bool requestMicrophoneAccess() {
    // Request microphone access if it hasn't been granted yet
    [AVCaptureDevice requestAccessForMediaType:AVMediaTypeAudio completionHandler:^(BOOL granted) {
        if (granted) {
            std::cout << "Microphone access granted!" << std::endl;
        } else {
            std::cout << "Microphone access denied." << std::endl;
        }
    }];
    return false;
}

void openPrivacySettings() {
    NSURL *url = [NSURL URLWithString:@"x-apple.systempreferences:com.apple.preference.security?Privacy_Microphone"];
    if (url) {
        [[NSWorkspace sharedWorkspace] openURL:url];
    }
}

// Can't find a launcher that has both microphone access AND speech recognition ANNNND this is overkill asf. We'll stick with whisper JNI'
/*
static inline BOOL IsRightOSVersion(void) {
    return [[NSProcessInfo processInfo] isOperatingSystemAtLeastVersion:(NSOperatingSystemVersion){10, 15, 0}];
}

void die(NSString *message) {
    fprintf(stderr, "%s\n", [message UTF8String]);
    //exit(EXIT_FAILURE);
}

void startTranscribing() {
    if (!IsRightOSVersion()) {
		die(@"macOS 10.15 or later required");
		return;
	}
	return;
    // Request authorization for speech recognition
    [SFSpeechRecognizer requestAuthorization:^(SFSpeechRecognizerAuthorizationStatus status) {
        if (status != SFSpeechRecognizerAuthorizationStatusAuthorized) {
            die(@"Permission denied");
            return;
        }

        // Set up the speech recognizer
        SFSpeechRecognizer *recognizer = [[SFSpeechRecognizer alloc] init];
        if (!recognizer || !recognizer.isAvailable) {
            die(@"Speech recognizer unavailable");
            return;
        }

        // Set up audio engine
        AVAudioEngine *engine = [[AVAudioEngine alloc] init];
        AVAudioInputNode *inputNode = engine.inputNode;
        SFSpeechAudioBufferRecognitionRequest *request = [[SFSpeechAudioBufferRecognitionRequest alloc] init];
        request.shouldReportPartialResults = YES;
        request.requiresOnDeviceRecognition = YES;

        // Handle recognition task
        __block BOOL isFinal = NO;
        SFSpeechRecognitionTask *task = [recognizer recognitionTaskWithRequest:request resultHandler:^(SFSpeechRecognitionResult *result, NSError *error) {
            if (error) {
                NSLog(@"Error: %@", error.localizedDescription);
                return;
            }

            // Print the formatted transcription as it's updated
            NSLog(@"\33[2K\r%@", result.bestTranscription.formattedString);
            if (result.isFinal) {
                isFinal = YES;  // Set isFinal when final recognition result is achieved
            }
        }];

        // Set up the audio input buffer
        [inputNode installTapOnBus:0 bufferSize:3200 format:[inputNode outputFormatForBus:0] block:^(AVAudioPCMBuffer *buffer, AVAudioTime *when) {
            [request appendAudioPCMBuffer:buffer];
        }];

        // Start the audio engine
        NSError *err;
        [engine startAndReturnError:&err];
        if (err) {
            die([NSString stringWithFormat:@"Audio engine failed: %@", err.localizedDescription]);
            return;
        }

        // Keep the run loop going, continuously updating the transcription
        while (!isFinal) {
            @autoreleasepool {
                [[NSRunLoop currentRunLoop] runUntilDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
            }
        }

        // After the loop finishes, print "I'm quitting" (or when the user manually stops it)
        printf("\nI'm quitting\n");
    }];

    // Main run loop keeps the app running until the user manually stops the process
    [[NSRunLoop currentRunLoop] run];
}
*/