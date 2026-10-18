#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

DEVICE_PATH := device/bluefox/Aura_A1

PRODUCT_SOONG_NAMESPACES += \
    $(DEVICE_PATH) \
    hardware/mediatek

# Shipping API levels
BOARD_SHIPPING_API_LEVEL := 31
PRODUCT_SHIPPING_API_LEVEL := 36

# The stock GKI is 4 KiB. Keep userspace page-size agnostic and request
# 16 KiB-compatible ELF segment alignment, but do not expose the 16 KiB
# developer toggle without an alternate kernel and boot OTA payload.
PRODUCT_MAX_PAGE_SIZE_SUPPORTED := 16384
PRODUCT_NO_BIONIC_PAGE_SIZE_MACRO := true

# Security patch level from the stock vendor build.
BOOT_SECURITY_PATCH := 2026-05-05
INIT_BOOT_SECURITY_PATCH := $(BOOT_SECURITY_PATCH)
VENDOR_SECURITY_PATCH := $(BOOT_SECURITY_PATCH)

# Display
TARGET_SCREEN_WIDTH := 720
TARGET_SCREEN_HEIGHT := 1600

PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/configs/displayconfig/display_port_0.xml:$(TARGET_COPY_OUT_VENDOR)/etc/displayconfig/display_port_0.xml

# Device feature declarations
PRODUCT_PACKAGES += \
    android.hardware.audio.low_latency.prebuilt.xml \
    android.hardware.bluetooth_le.prebuilt.xml \
    android.hardware.camera.flash-autofocus.prebuilt.xml \
    android.hardware.camera.front.prebuilt.xml \
    android.hardware.camera.full.prebuilt.xml \
    android.hardware.camera.raw.prebuilt.xml \
    android.hardware.consumerir.prebuilt.xml \
    android.hardware.fingerprint.prebuilt.xml \
    android.hardware.location.gps.prebuilt.xml \
    android.hardware.nfc.hce.prebuilt.xml \
    android.hardware.nfc.prebuilt.xml \
    android.hardware.se.omapi.uicc.prebuilt.xml \
    android.hardware.sensor.gyroscope.prebuilt.xml \
    android.hardware.sensor.hifi_sensors.prebuilt.xml \
    android.hardware.sensor.light.prebuilt.xml \
    android.hardware.sensor.proximity.prebuilt.xml \
    android.hardware.sensor.stepcounter.prebuilt.xml \
    android.hardware.sensor.stepdetector.prebuilt.xml \
    android.hardware.telephony.gsm.prebuilt.xml \
    android.hardware.telephony.ims.prebuilt.xml \
    android.hardware.usb.accessory.prebuilt.xml \
    android.hardware.usb.host.prebuilt.xml \
    android.hardware.vulkan.compute-0.prebuilt.xml \
    android.hardware.vulkan.level-1.prebuilt.xml \
    android.hardware.vulkan.version-1_3.prebuilt.xml \
    android.hardware.wifi.direct.prebuilt.xml \
    android.hardware.wifi.passpoint.prebuilt.xml \
    android.hardware.wifi.prebuilt.xml \
    android.hardware.hardware_keystore_V4.xml \
    android.hardware.identity_credential.xml \
    android.software.device_id_attestation.prebuilt.xml \
    android.software.ipsec_tunnels.prebuilt.xml \
    android.software.opengles.deqp.level-latest.prebuilt.xml \
    android.software.verified_boot.prebuilt.xml \
    android.software.vulkan.deqp.level-latest.prebuilt.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.keystore.app_attest_key.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.keystore.app_attest_key.xml \
    frameworks/native/data/etc/android.hardware.nfc.uicc.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.nfc.uicc.xml \
    frameworks/native/data/etc/android.hardware.opengles.aep.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.opengles.aep.xml \
    frameworks/native/data/etc/android.hardware.telephony.cdma.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.telephony.cdma.xml \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.jazzhand.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.touchscreen.multitouch.jazzhand.xml \
    frameworks/native/data/etc/android.software.midi.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.midi.xml

# Generic ramdisk and storage
$(call inherit-product, $(SRC_TARGET_DIR)/product/generic_ramdisk.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/emulated_storage.mk)
$(call inherit-product, frameworks/native/build/phone-xhdpi-6144-dalvik-heap.mk)

# Virtual A/B with compressed userspace snapshots
$(call inherit-product, $(SRC_TARGET_DIR)/product/virtual_ab_ota/compression.mk)

# Dynamic partitions
PRODUCT_USE_DYNAMIC_PARTITIONS := true
PRODUCT_BUILD_SUPER_PARTITION := false

# MediaTek framework compatibility
ENABLE_VENDOR_RIL_SERVICE := true
TARGET_BOARD_PLATFORM := mt6789

# The product launches on API 36, but its vendor contract starts at API 31 and
# still exposes legacy HIDL services. Select the compatibility runtime
# explicitly because the platform's shipping-API gate would otherwise omit it.
PRODUCT_PACKAGES += \
    android.hidl.allocator@1.0-service \
    android.hidl.memory@1.0-impl \
    android.hidl.memory@1.0-impl.vendor \
    hwservicemanager \
    vndservicemanager

# Keep the stock board-specific USB init policy instead of the generic
# MediaTek gadget fallback.
$(call soong_config_set_bool,mediatek_gadget,use_custom_usb_gadget_rc,true)

PRODUCT_PACKAGES += \
    android.hardware.usb-service.mediatek \
    android.hardware.usb.gadget-service.mediatek \
    TetheringOverlayAura_A1

PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/rootdir/etc/init/init.aura.usb.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.aura.usb.rc

$(call inherit-product, hardware/mediatek/overlay/mssi.mk)

# Device settings
PRODUCT_PACKAGES += \
    AuraParts

# Device overlays
PRODUCT_PACKAGES += \
    CellBroadcastOverlayAura_A1 \
    DeviceAsWebcamOverlayAura_A1 \
    FrameworkOverlayAura_A1 \
    LineagePartsOverlayAura_A1 \
    LineageSDKOverlayAura_A1 \
    LineageSettingsOverlayAura_A1 \
    SystemUIOverlayAura_A1 \
    WifiOverlayAura_A1

$(call inherit-product, hardware/lineage/compat/frameworks/compat.mk)
$(call inherit-product, hardware/mediatek/frameworks/mediatek-frameworks.mk)

include hardware/mediatek/configs/properties/vendor_logtag.mk

# IMS
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/configs/permissions/privapp-permissions-com.mediatek.ims.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-com.mediatek.ims.xml \
    $(DEVICE_PATH)/configs/permissions/privapp-permissions-com.mediatek.telephony.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-com.mediatek.telephony.xml

# The minimal proprietary closure does not ship MediaTek's legacy IWLAN
# framework classes. Keep the platform defaults instead of installing an
# overlay that points to unavailable implementations.
PRODUCT_PACKAGES_EXCLUDE += MssiFrameworkTelephonyOverlay

# Enforce the stock 202504 device contract during the build. HAL-specific
# fragments remain owned by their implementation packages and are not copied
# here, which prevents duplicate declarations when proprietary HALs are added.
PRODUCT_ENFORCE_VINTF_MANIFEST := true

# Audio effects
PRODUCT_PACKAGES += \
    aidl_audio_set_configurations_bfbs \
    aidl_audio_set_scenarios_bfbs \
    android.hardware.audio.parameter_parser.example_service \
    android.hardware.audio.common-V3-ndk \
    android.hardware.audio.effect-V2-ndk \
    android.hardware.bluetooth.audio-V4-ndk \
    android.hardware.camera.common-V2-ndk \
    android.hardware.camera.device-V3-ndk \
    android.hardware.camera.provider-V3-ndk \
    android.hardware.graphics.common-V6-ndk \
    android.media.audio.common.types-V3-ndk \
    hfp_codec_capabilities_xml \
    libhapticgeneratoraidl

# Frozen AIDL NDK libraries needed by proprietary vendor processes. Install
# their source-built vendor variants in the vendor linker namespace.
PRODUCT_PACKAGES += \
    android.hardware.audio.common-V3-ndk.vendor \
    android.hardware.audio.effect-V2-ndk.vendor \
    android.hardware.bluetooth.audio-V4-ndk.vendor \
    android.hardware.camera.common-V2-ndk.vendor \
    android.hardware.camera.device-V3-ndk.vendor \
    android.hardware.camera.provider-V3-ndk.vendor \
    android.hardware.graphics.common-V6-ndk.vendor \
    android.media.audio.common.types-V3-ndk.vendor

# A/B and recovery
PRODUCT_PACKAGES += \
    android.hardware.bluetooth-service.mediatek \
    android.hardware.boot-service.mediatek \
    android.hardware.boot-service.mediatek_recovery \
    android.hardware.health-service.mediatek \
    android.hardware.health-service.mediatek-recovery \
    android.hardware.ir-service.example \
    android.hardware.memtrack-service.mediatek \
    android.hardware.sensors-service.multihal \
    chipinfo \
    create_pl_dev \
    create_pl_dev.recovery \
    fastbootd \
    recovery \
    update_engine \
    update_engine_sideload \
    update_verifier

PRODUCT_PACKAGES_DEBUG += \
    bootctl

# Keep the normal-boot fstab in both locations used by first- and second-stage
# init. Recovery uses a separate restricted fstab that omits sensitive device
# data partitions.
PRODUCT_COPY_FILES += \
    $(DEVICE_PATH)/rootdir/etc/init/init.aura.early-adb.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.aura.early-adb.rc \
    $(DEVICE_PATH)/rootdir/etc/init/init.nfc.tms.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.nfc.tms.rc \
    $(DEVICE_PATH)/rootdir/etc/init/init.sensors.aura.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.sensors.aura.rc \
    $(DEVICE_PATH)/rootdir/etc/fstab.enableswap:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.enableswap \
    $(DEVICE_PATH)/rootdir/etc/fstab.mt6789:$(TARGET_COPY_OUT_VENDOR)/etc/fstab.mt6789 \
    $(DEVICE_PATH)/rootdir/etc/fstab.mt6789:$(TARGET_COPY_OUT_VENDOR_RAMDISK)/first_stage_ramdisk/fstab.mt6789

# Wi-Fi
PRODUCT_PACKAGES += wlan_assistant

# Inherit proprietary files
$(call inherit-product, vendor/bluefox/Aura_A1/Aura_A1-vendor.mk)
