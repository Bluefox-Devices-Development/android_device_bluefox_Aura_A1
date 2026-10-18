#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

DEVICE_PATH := device/bluefox/Aura_A1
KERNEL_PATH := $(DEVICE_PATH)-kernels

# A/B
AB_OTA_UPDATER := true
AB_OTA_PARTITIONS := \
    boot \
    dtbo \
    init_boot \
    odm_dlkm \
    product \
    system \
    system_dlkm \
    system_ext \
    vbmeta \
    vbmeta_system \
    vbmeta_vendor \
    vendor \
    vendor_boot \
    vendor_dlkm

# Architecture
TARGET_ARCH := arm64
TARGET_ARCH_VARIANT := armv8-2a-dotprod
TARGET_CPU_ABI := arm64-v8a
TARGET_CPU_ABI2 :=
TARGET_CPU_VARIANT := generic
TARGET_CPU_VARIANT_RUNTIME := cortex-a55
TARGET_SUPPORTS_32_BIT_APPS := false
TARGET_SUPPORTS_64_BIT_APPS := true

# Platform
TARGET_BOARD_PLATFORM := mt6789
TARGET_USES_VULKAN := true

# VINTF
DEVICE_MANIFEST_FILE := $(DEVICE_PATH)/vintf/manifest.xml
DEVICE_MATRIX_FILE := $(DEVICE_PATH)/vintf/compatibility_matrix.xml
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE := \
    $(DEVICE_PATH)/vintf/framework_compatibility_matrix.xml

# The stock vendor image carries radio manifests for every supported modem
# SKU under vendor/odm. Keep these as build-time inputs so assemble_vintf can
# validate and install every variant; libvintf selects the bootloader-selected
# SKU at runtime.
ODM_MANIFEST_SKUS := \
    dsds \
    qsqs \
    ss \
    tsts

ODM_MANIFEST_DSDS_FILES := $(DEVICE_PATH)/odm/manifest_dsds.xml
ODM_MANIFEST_QSQS_FILES := $(DEVICE_PATH)/odm/manifest_qsqs.xml
ODM_MANIFEST_SS_FILES := $(DEVICE_PATH)/odm/manifest_ss.xml
ODM_MANIFEST_TSTS_FILES := $(DEVICE_PATH)/odm/manifest_tsts.xml

# Bootloader
BOARD_VENDOR := bluefox
TARGET_BOOTLOADER_BOARD_NAME := k6789v1_64
TARGET_NO_BOOTLOADER := true
TARGET_OTA_ASSERT_DEVICE := Aura_A1,k6789v1_64

# Boot image layout
BOARD_KERNEL_BASE := 0x3fff8000
BOARD_KERNEL_OFFSET := 0x00008000
BOARD_RAMDISK_OFFSET := 0x26f08000
BOARD_KERNEL_TAGS_OFFSET := 0x07c88000
BOARD_DTB_OFFSET := 0x07c88000
BOARD_KERNEL_PAGESIZE := 4096
BOARD_BOOT_HEADER_VERSION := 4
BOARD_INIT_BOOT_HEADER_VERSION := 4

BOARD_MKBOOTIMG_ARGS += --header_version $(BOARD_BOOT_HEADER_VERSION)
BOARD_MKBOOTIMG_ARGS += --kernel_offset $(BOARD_KERNEL_OFFSET)
BOARD_MKBOOTIMG_ARGS += --ramdisk_offset $(BOARD_RAMDISK_OFFSET)
BOARD_MKBOOTIMG_ARGS += --tags_offset $(BOARD_KERNEL_TAGS_OFFSET)
BOARD_MKBOOTIMG_ARGS += --dtb_offset $(BOARD_DTB_OFFSET)
BOARD_MKBOOTIMG_INIT_ARGS += --header_version $(BOARD_INIT_BOOT_HEADER_VERSION)

BOARD_KERNEL_CMDLINE := bootopt=64S3,32N2,64N2
BOARD_BOOTCONFIG := \
    androidboot.serialconsole=0 \
    kernel.rcu_nocbs=all \
    kernel.rcutree.enable_rcu_lazy=1 \
    kernel.rcupdate.rcu_cpu_stall_cputime=1

BOARD_RAMDISK_USE_LZ4 := true
BOARD_USES_GENERIC_KERNEL_IMAGE := true
BOARD_MOVE_GSI_AVB_KEYS_TO_VENDOR_BOOT := true

# Package Lineage Recovery as the recovery ramdisk fragment in vendor_boot v4.
BOARD_MOVE_RECOVERY_RESOURCES_TO_VENDOR_BOOT := true
BOARD_INCLUDE_RECOVERY_RAMDISK_IN_VENDOR_BOOT := true

# Partitions
BOARD_FLASH_BLOCK_SIZE := 262144 # BOARD_KERNEL_PAGESIZE * 64
BOARD_BOOTIMAGE_PARTITION_SIZE := 67108864
BOARD_DTBOIMG_PARTITION_SIZE := 8388608
BOARD_INIT_BOOT_IMAGE_PARTITION_SIZE := 8388608
BOARD_VENDOR_BOOTIMAGE_PARTITION_SIZE := 67108864
BOARD_SUPER_PARTITION_SIZE := 9663676416
BOARD_USERDATAIMAGE_PARTITION_SIZE := 116651950080

BOARD_SUPER_PARTITION_GROUPS := main
BOARD_MAIN_SIZE := 9661579264
BOARD_MAIN_PARTITION_LIST := \
    odm_dlkm \
    product \
    system \
    system_dlkm \
    system_ext \
    vendor \
    vendor_dlkm

BOARD_ODM_DLKMIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_PRODUCTIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_SYSTEMIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_SYSTEM_DLKMIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_SYSTEM_EXTIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_VENDORIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_VENDOR_DLKMIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_USERDATAIMAGE_FILE_SYSTEM_TYPE := f2fs

TARGET_COPY_OUT_ODM_DLKM := odm_dlkm
TARGET_COPY_OUT_PRODUCT := product
TARGET_COPY_OUT_SYSTEM_DLKM := system_dlkm
TARGET_COPY_OUT_SYSTEM_EXT := system_ext
TARGET_COPY_OUT_VENDOR := vendor
TARGET_COPY_OUT_VENDOR_DLKM := vendor_dlkm

BOARD_USES_METADATA_PARTITION := true
TARGET_USERIMAGES_USE_F2FS := true

-include vendor/lineage/config/BoardConfigReservedSize.mk

# Kernel
TARGET_KERNEL_DIR := $(KERNEL_PATH)/6.12
BOARD_KERNEL_IMAGE_NAME := Image.lz4
TARGET_PREBUILT_KERNEL := $(TARGET_KERNEL_DIR)/$(BOARD_KERNEL_IMAGE_NAME)

BOARD_INCLUDE_DTB_IN_BOOTIMG := true
BOARD_PREBUILT_DTBIMAGE_DIR := $(TARGET_KERNEL_DIR)/dtb
BOARD_KERNEL_SEPARATED_DTBO := true
BOARD_PREBUILT_DTBOIMAGE := $(TARGET_KERNEL_DIR)/dtbo.img

TARGET_PROVIDES_STATIC_MODULE_LISTS := true

SYSTEM_DLKM_MODULE_DIR := $(TARGET_KERNEL_DIR)/system_dlkm/lib/modules
VENDOR_DLKM_MODULE_DIR := $(TARGET_KERNEL_DIR)/vendor_dlkm/lib/modules
VENDOR_RAMDISK_MODULE_DIR := $(TARGET_KERNEL_DIR)/vendor_ramdisk/lib/modules

BOARD_SYSTEM_KERNEL_MODULES_LOAD := $(strip $(shell cat $(SYSTEM_DLKM_MODULE_DIR)/modules.load))
BOARD_VENDOR_KERNEL_MODULES_LOAD := $(strip $(shell cat $(VENDOR_DLKM_MODULE_DIR)/modules.load))
BOARD_VENDOR_RAMDISK_KERNEL_MODULES_LOAD := $(strip $(shell cat $(VENDOR_RAMDISK_MODULE_DIR)/modules.load))
BOARD_VENDOR_RAMDISK_RECOVERY_KERNEL_MODULES_LOAD := $(strip $(shell cat $(VENDOR_RAMDISK_MODULE_DIR)/modules.load.recovery))

# Package every stock module. The load lists remain separate because MediaTek
# also loads some vendor_dlkm modules dynamically from init.
BOARD_SYSTEM_KERNEL_MODULES := $(wildcard $(SYSTEM_DLKM_MODULE_DIR)/*.ko)
BOARD_VENDOR_KERNEL_MODULES := $(wildcard $(VENDOR_DLKM_MODULE_DIR)/*.ko)
BOARD_VENDOR_RAMDISK_KERNEL_MODULES := $(wildcard $(VENDOR_RAMDISK_MODULE_DIR)/*.ko)

# Preserve the stock module bytes and relocations.
BOARD_DO_NOT_STRIP_VENDOR_MODULES := true
BOARD_DO_NOT_STRIP_VENDOR_RAMDISK_MODULES := true

# Properties
TARGET_PRODUCT_PROP += $(DEVICE_PATH)/product.prop
TARGET_SYSTEM_EXT_PROP += $(DEVICE_PATH)/system_ext.prop
TARGET_VENDOR_PROP += \
    $(DEVICE_PATH)/vendor-platform.prop \
    $(DEVICE_PATH)/vendor.prop

# Recovery
TARGET_RECOVERY_DEVICE_DIRS := $(DEVICE_PATH)
TARGET_RECOVERY_FSTAB := $(DEVICE_PATH)/recovery/root/system/etc/recovery.fstab
TARGET_RECOVERY_DENSITY := xhdpi
TARGET_RECOVERY_PIXEL_FORMAT := RGBX_8888
TARGET_RECOVERY_UI_SCREEN_WIDTH := 720
TARGET_RECOVERY_UI_MARGIN_HEIGHT := 96
TARGET_RECOVERY_UI_BRIGHTNESS_FILE := /sys/class/leds/lcd-backlight/brightness
TARGET_RECOVERY_UI_MAX_BRIGHTNESS_FILE := /sys/class/leds/lcd-backlight/max_brightness
TARGET_RECOVERY_UI_BRIGHTNESS_NORMAL := 50
TARGET_RECOVERY_UI_BRIGHTNESS_DIMMED := 25
TARGET_SCREEN_DENSITY := 320

# SELinux
include device/mediatek/sepolicy_vndr/SEPolicy.mk

BOARD_VENDOR_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/vendor
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(DEVICE_PATH)/sepolicy/private

# Verified Boot
BOARD_AVB_ENABLE := true
BOARD_AVB_ALGORITHM := SHA256_RSA2048
# This AOSP public test key is suitable only for unlocked-device bring-up.
BOARD_AVB_KEY_PATH := external/avb/test/data/testkey_rsa2048.pem

# Aura uses physical vbmeta_system and vbmeta_vendor A/B partitions. Keep the
# chained images split like the stock mount contract so target-files and OTA
# payloads always contain both images. The rollback-index locations are
# bring-up values and must be compared with the stock descriptors before any
# AVB-locked installation is supported.
BOARD_AVB_VBMETA_SYSTEM := product system system_ext
BOARD_AVB_VBMETA_SYSTEM_KEY_PATH := external/avb/test/data/testkey_rsa2048.pem
BOARD_AVB_VBMETA_SYSTEM_ALGORITHM := SHA256_RSA2048
BOARD_AVB_VBMETA_SYSTEM_ROLLBACK_INDEX := $(PLATFORM_SECURITY_PATCH_TIMESTAMP)
BOARD_AVB_VBMETA_SYSTEM_ROLLBACK_INDEX_LOCATION := 1

BOARD_AVB_VBMETA_VENDOR := vendor
BOARD_AVB_VBMETA_VENDOR_KEY_PATH := external/avb/test/data/testkey_rsa2048.pem
BOARD_AVB_VBMETA_VENDOR_ALGORITHM := SHA256_RSA2048
BOARD_AVB_VBMETA_VENDOR_ROLLBACK_INDEX := $(PLATFORM_SECURITY_PATCH_TIMESTAMP)
BOARD_AVB_VBMETA_VENDOR_ROLLBACK_INDEX_LOCATION := 2

BOARD_AVB_SYSTEM_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_SYSTEM_OTHER_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_SYSTEM_EXT_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_PRODUCT_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_VENDOR_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_VENDOR_DLKM_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_ODM_DLKM_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256
BOARD_AVB_SYSTEM_DLKM_ADD_HASHTREE_FOOTER_ARGS += --hash_algorithm sha256

# Wi-Fi
WIFI_DRIVER_FW_PATH_STA := "STA"
WIFI_DRIVER_FW_PATH_AP := "AP"
WIFI_DRIVER_FW_PATH_P2P := "P2P"
WIFI_DRIVER_FW_PATH_PARAM := "/dev/wmtWifi"
WIFI_DRIVER_STATE_CTRL_PARAM := "/dev/wmtWifi"
WIFI_DRIVER_STATE_ON := "1"
WIFI_DRIVER_STATE_OFF := "0"

# Match the production HAL: AP, STA + AP, or STA + P2P, all in mode 3.
WIFI_HAL_INTERFACE_COMBINATIONS := \
    {{{AP}, 1}}, \
    {{{STA}, 1}, {{AP}, 1}}, \
    {{{STA}, 1}, {{P2P}, 1}}

# Inherit proprietary files
include vendor/bluefox/Aura_A1/BoardConfigVendor.mk
