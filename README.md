<!--
SPDX-FileCopyrightText: The LineageOS Project
SPDX-License-Identifier: CC-BY-SA-4.0
-->

Device configuration for Bluefox Aura A1
=========================================

The Bluefox Aura A1 (BF00A) is an Android 16 device based on the
MediaTek MT6789 platform.

Lineage Recovery is packaged in `vendor_boot`; ADB, touch input and sideload
have been verified. A complete LineageOS system boot has not yet been verified
on hardware.

> [!WARNING]
> Current builds use public AOSP AVB test keys and provisional rollback-index
> locations. They are intended only for unlocked-device development. Do not
> relock the bootloader with these images.

## Device specifications

| Basic                   | Bluefox Aura A1                         |
| ----------------------: | :-------------------------------------- |
| SoC                     | MediaTek Helio G100 (MT6789)            |
| CPU                     | 2x Cortex-A76 and 6x Cortex-A55         |
| GPU                     | Mali-G57 MC2                            |
| Memory                  | 8 GB                                    |
| Storage                 | 128 GB UFS                              |
| Shipped Android Version | 16                                      |
| Display                 | 720 x 1600, 320 dpi, 60/90 Hz           |
| Kernel                  | 6.12.58-android16-6, 4 KiB pages        |
| Codename                | Aura_A1                                 |
| Stock product           | BF00A                                   |
| Bootloader board        | k6789v1_64                              |
