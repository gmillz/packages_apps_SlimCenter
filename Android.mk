LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

appcompat_dir := prebuilts/sdk/current/support/v7/appcompat/res

res_dir := $(LOCAL_PATH)/res $(appcompat_dir)

LOCAL_RESOURCE_DIR := $(res_dir)

LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages android.support.v7.appcompat

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SlimCenter

LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4 \
    android-support-v7-appcompat \
    android-support-v13

include $(BUILD_PACKAGE)
