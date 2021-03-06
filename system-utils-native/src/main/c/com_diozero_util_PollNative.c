#include "com_diozero_util_PollNative.h"

#include <jni.h>
#include <errno.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <fcntl.h>
#include <poll.h>
#include "com_diozero_util_Util.h"

extern jclass systemClassRef;
extern jmethodID nanoTimeMethodId;

JNIEXPORT void JNICALL Java_com_diozero_util_PollNative_poll(
		JNIEnv* env, jobject pollNative, jstring filename, jint timeout, jobject ref, jobject callback) {
	jclass callback_class = (*env)->GetObjectClass(env, callback);
	if (callback_class == NULL) {
		printf("Error: poll() could not get callback class\n");
		return;
	}
	char* method_name = "notify";
	char* signature = "(Ljava/lang/String;JC)V";
	jmethodID notify_method_id = (*env)->GetMethodID(env, callback_class, method_name, signature);
	if ((*env)->ExceptionCheck(env) || notify_method_id == NULL) {
		printf("Unable to find method '%s' with signature '%s' in callback object\n", method_name, signature);
		return;
	}

	jsize len = (*env)->GetStringLength(env, filename);
	char c_filename[len];
	(*env)->GetStringUTFRegion(env, filename, 0, len, c_filename);

	int fd = open(c_filename, O_RDONLY | O_NONBLOCK);
	if (fd < 0) {
		printf("open: file %s could not be opened, %s\n", c_filename, strerror(errno));
		return;
	}

	jclass poll_native_class = (*env)->GetObjectClass(env, pollNative);
	if (poll_native_class == NULL) {
		printf("Error: poll() could not get PollNative class\n");
		return;
	}
	char* set_fd_method_name = "setFd";
	char* set_fd_signature = "(I)V";
	jmethodID set_fd_method_id = (*env)->GetMethodID(env, poll_native_class, set_fd_method_name, set_fd_signature);
	if (set_fd_method_id == NULL) {
		printf("Unable to find method '%s' with signature '%s' in PollNative object\n", set_fd_method_name, set_fd_signature);
		return;
	}
	(*env)->CallVoidMethod(env, pollNative, set_fd_method_id, fd);

	const int BUF_LEN = 2;
	uint8_t c[BUF_LEN];
	memset(c, 0, BUF_LEN);

	lseek(fd, 0, SEEK_SET); /* consume any prior interrupts */
	read(fd, &c, BUF_LEN-1);

	int retval;

	struct pollfd pfd;
	pfd.fd = fd;
	pfd.events = POLLPRI | POLLERR | POLLHUP | POLLNVAL;
	//pfd.events = POLLPRI;
	jlong nano_time;
	jlong epoch_time;

	while (1) {
		// TODO How to interrupt the blocking poll call?
		retval = poll(&pfd, 1, timeout);
		// Get the Java nano time as early as possible
		nano_time = getJavaNanoTime();
		epoch_time = getEpochTime();

		lseek(fd, 0, SEEK_SET); /* consume the interrupt */
		memset(c, 0, BUF_LEN);
		long r = read(fd, &c, BUF_LEN-1);

		if (retval < 0 || (pfd.revents & POLLNVAL) || r <= 0) {
			printf("Invalid response");
			break;
		} else if (retval > 0) {
			(*env)->CallVoidMethod(env, callback, notify_method_id, ref, epoch_time, nano_time, c[0]);
		}
	}

	close(fd);
}

JNIEXPORT void JNICALL Java_com_diozero_util_PollNative_stop
  (JNIEnv* env, jobject obj, jint fd) {
	close(fd);
}
