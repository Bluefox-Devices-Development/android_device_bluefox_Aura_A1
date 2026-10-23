#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from the most specific product definitions first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from Aura A1.
$(call inherit-product, device/bluefox/Aura_A1/device.mk)

# Keep ADB available during device bring-up.
WITH_ADB_INSECURE := true

# Inherit common LineageOS phone configuration.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

PRODUCT_NAME := lineage_Aura_A1
PRODUCT_DEVICE := Aura_A1
PRODUCT_MANUFACTURER := BLUEFOX
PRODUCT_BRAND := BLUEFOX
PRODUCT_MODEL := BF00A

# Keep the stock attestation identity while using Aura_A1 at runtime.
PRODUCT_DEVICE_FOR_ATTESTATION := Aura_A1t

PRODUCT_GMS_CLIENTID_BASE := android-bluefox

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=BLUEFOX/Aura_A1/Aura_A1t:16/BP2A.250605.031.A3/20260924:user/release-keys \
    DeviceName=Aura_A1 \
    DeviceProduct=Aura_A1
