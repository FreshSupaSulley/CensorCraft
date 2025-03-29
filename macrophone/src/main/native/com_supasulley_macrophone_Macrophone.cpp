#include "com_supasulley_macrophone_Macrophone.h"
#include "MicrophoneAccess.h"

// Should I keep this?
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jobject JNICALL Java_com_supasulley_macrophone_Macrophone_getMicrophonePermission(
		JNIEnv *env, jclass thisClass) {

	const jclass clazz = env->FindClass(
			"com/supasulley/macrophone/AVAuthorizationStatus");
	if (clazz == NULL) {
		return NULL;
	}

	// Convert to enum
	return env->GetStaticObjectField(clazz,
			env->GetStaticFieldID(clazz, getMicAuthStatus(),
					"Lcom/supasulley/macrophone/AVAuthorizationStatus;"));
}

JNIEXPORT void JNICALL Java_com_supasulley_macrophone_Macrophone_openMicrophoneSettings(
		JNIEnv *ev, jclass thisClass) {
	openPrivacySettings();
}

JNIEXPORT void JNICALL Java_com_supasulley_macrophone_Macrophone_requestMicrophoneAccess(
		JNIEnv *env, jclass thisClass) {
	requestMicrophoneAccess();
}

// Should I keep this pt2?
#ifdef __cplusplus
}
#endif
