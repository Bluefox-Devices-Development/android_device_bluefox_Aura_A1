#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.bp_builder import BpBuilder
from extract_utils.bp_encoder import BpJSONEncoder
from extract_utils.fixups_blob import blob_fixup, blob_fixups_user_type
from extract_utils.fixups_lib import (
    lib_fixup_vendorcompat,
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import ExtractUtils, ExtractUtilsModule

namespace_imports = [
    'device/bluefox/Aura_A1',
    'hardware/mediatek',
    'hardware/mediatek/libmtkperf_client',
]

noninstallable_compat_libraries = (
    (
        'android.hardware.audio.common-V3-ndk',
        (
            'libbinder_ndk',
            'android.media.audio.common.types-V3-ndk-vendorcompat',
            'libc++',
            'libc',
            'libm',
            'libdl',
        ),
    ),
    (
        'android.hardware.audio.effect-V2-ndk',
        (
            'libbinder_ndk',
            'android.media.audio.common.types-V3-ndk-vendorcompat',
            'android.hardware.audio.common-V3-ndk-vendorcompat',
            'android.hardware.common-V2-ndk',
            'android.hardware.common.fmq-V1-ndk',
            'libc++',
            'libc',
            'libm',
            'libdl',
        ),
    ),
    (
        'android.hardware.bluetooth.audio-V4-ndk',
        (
            'libbinder_ndk',
            'android.hardware.audio.common-V3-ndk-vendorcompat',
            'android.hardware.common-V2-ndk',
            'android.hardware.common.fmq-V1-ndk',
            'libc++',
            'libc',
            'libm',
            'libdl',
        ),
    ),
    (
        'android.hardware.camera.common-V2-ndk',
        (
            'libbinder_ndk',
            'libc++',
            'libc',
            'libm',
            'libdl',
        ),
    ),
    (
        'android.hardware.camera.device-V3-ndk',
        (
            'libbinder_ndk',
            'android.hardware.common-V2-ndk',
            'android.hardware.common.fmq-V1-ndk',
            'android.hardware.camera.common-V1-ndk',
            'android.hardware.camera.metadata-V3-ndk',
            'android.hardware.graphics.common-V6-ndk-vendorcompat',
            'libc++',
            'libc',
            'libm',
            'libdl',
        ),
    ),
    (
        'android.hardware.camera.provider-V3-ndk',
        (
            'libbinder_ndk',
            'android.hardware.camera.device-V3-ndk-vendorcompat',
            'android.hardware.camera.common-V1-ndk',
            'libc++',
            'libc',
            'libm',
            'libdl',
        ),
    ),
    (
        'android.hardware.graphics.common-V6-ndk',
        (
            'libbinder_ndk',
            'android.hardware.common-V2-ndk',
            'libc++',
            'libc',
            'libm',
            'libdl',
        ),
    ),
    (
        'android.media.audio.common.types-V3-ndk',
        (
            'libbinder_ndk',
            'libc++',
            'libc',
            'libm',
            'libdl',
        ),
    ),
)


def write_noninstallable_compat_libraries(ctx, _):
    encoder = BpJSONEncoder(legacy=ctx.legacy)

    for library, shared_libs in noninstallable_compat_libraries:
        (
            BpBuilder(encoder)
            .set_rule_name('cc_prebuilt_library_shared')
            .set_owner('bluefox')
            .set_partition('vendor')
            .name(f'{library}-vendorcompat')
            .stem(library)
            .owner()
            .no_strip()
            .set(
                'target',
                {
                    'android_arm64': {
                        'srcs': [
                            f'proprietary/vendor/lib64/{library}.so',
                        ],
                        'shared_libs': list(shared_libs),
                    },
                },
            )
            .multilib(64)
            .set('installable', False)
            .prefer()
            .specific()
            .write(ctx.bp_out)
        )


def fix_ov64b40_pdaf_geometry(ctx, file, file_path, **_):
    # WW 260924 added input bounds checks, but its cold path still selects
    # 568x1720 while the reuse path selects the actual 1136x860 RAW10 input.
    # The cold path therefore returns null at row 860. Its register allocation
    # also changed: patch w28/w26, not the old build's w25/w24.
    cold_geometry = b'\x1c\x47\x80\x52\x1a\xd7\x80\x52'
    warm_geometry = b'\x1c\x8e\x80\x52\x9a\x6b\x80\x52'

    with open(file_path, 'r+b') as blob:
        data = blob.read()
        if data.count(cold_geometry) != 1 or data.count(warm_geometry) != 1:
            raise ValueError(
                f'{file.dst}: unexpected OV64B40 PDAF code; re-audit geometry'
            )
        blob.seek(0)
        blob.write(data.replace(cold_geometry, warm_geometry, 1))


blob_fixups: blob_fixups_user_type = {
    # Stock V2 adds perfCusLockHintWithData, absent from upstream V2.
    (
        'vendor/lib64/android.hardware.power-service-mediatek.so',
        'vendor/lib64/libmtkperf_client_vendor.so',
    ): blob_fixup()
        .replace_needed(
            'vendor.mediatek.hardware.mtkpower-V2-ndk.so',
            'vendor.mediatek.hardware.mtkpower-V2-ndk_prebuilt.so',
        ),
    'system_ext/priv-app/MtkTelephonyAssist/MtkTelephonyAssist.apk': blob_fixup()
        .apktool_patch('blob-patches/MtkTelephonyAssist'),
    'system_ext/priv-app/ImsService/ImsService.apk': blob_fixup()
        .apktool_patch('blob-patches/ImsService'),
    'system_ext/lib64/libimsma.so': blob_fixup()
        .replace_needed('libsink.so', 'libsink-mtk.so'),
    'system_ext/lib64/libsink-mtk.so': blob_fixup()
        .add_needed('libaudioclient_shim.so'),
    'vendor/etc/audio_effects_config.xml': blob_fixup()
        .regex_replace(r'libbundleaidl\.so', 'libbundleaidl_prebuilt.so')
        .regex_replace(r'libdownmixaidl\.so', 'libdownmixaidl_prebuilt.so')
        .regex_replace(r'libdynamicsprocessingaidl\.so', 'libdynamicsprocessingaidl_prebuilt.so')
        .regex_replace(r'libhapticgeneratoraidl\.so', 'libhapticgeneratoraidl_prebuilt.so')
        .regex_replace(r'libloudnessenhanceraidl\.so', 'libloudnessenhanceraidl_prebuilt.so')
        .regex_replace(r'libreverbaidl\.so', 'libreverbaidl_prebuilt.so')
        .regex_replace(r'libvisualizeraidl\.so', 'libvisualizeraidl_prebuilt.so')
        .regex_replace('libaecsw\\.so', 'libaecsw_prebuilt.so')
        .regex_replace('libagc1sw\\.so', 'libagc1sw_prebuilt.so')
        .regex_replace('libagc2sw\\.so', 'libagc2sw_prebuilt.so')
        .regex_replace('libbassboostsw\\.so', 'libbassboostsw_prebuilt.so')
        .regex_replace('libenvreverbsw\\.so', 'libenvreverbsw_prebuilt.so')
        .regex_replace('libequalizersw\\.so', 'libequalizersw_prebuilt.so')
        .regex_replace('libextensioneffect\\.so', 'libextensioneffect_prebuilt.so')
        .regex_replace('libnssw\\.so', 'libnssw_prebuilt.so')
        .regex_replace('libpreprocessingaidl\\.so', 'libpreprocessingaidl_prebuilt.so')
        .regex_replace('libpresetreverbsw\\.so', 'libpresetreverbsw_prebuilt.so')
        .regex_replace('libvirtualizersw\\.so', 'libvirtualizersw_prebuilt.so')
        .regex_replace('libvolumesw\\.so', 'libvolumesw_prebuilt.so')
        .regex_replace(
            r'(?m)^[ \t]*<library name="erasersw" path="liberasersw\.so"/>'
            r'[ \t]*\r?\n',
            '',
        )
        .regex_replace(
            r'(?m)^[ \t]*<effect name="eraser" library="erasersw" '
            r'uuid="fa81ab46-588b-11ed-9b6a-0242ac120002"/>'
            r'[ \t]*\r?\n',
            '',
        )
        # AudioFX uses a nonzero session; auxiliary preset reverb is invalid
        # there and is otherwise selected ahead of the insert implementation.
        .regex_replace(
            r'(?m)^[ \t]*<effect name="reverb_pre_aux" library="reverb" '
            r'uuid="f29a1400-a3bb-11df-8ddc-0002a5d5c51b"/>'
            r'[ \t]*\r?\n',
            '',
        ),
    (
        'vendor/lib64/libcodec2_mtk_vdec.so',
        'vendor/lib64/libcodec2_mtk_venc.so',
    ): blob_fixup()
        .replace_needed('libformatter.so', 'libformatter_mtk.so'),
    'vendor/bin/mnld': blob_fixup()
        .remove_needed('android.hardware.sensors-V2-ndk.so')
        .replace_needed('libmnl.so', 'libmnl_mtk.so'),
    # Keep the stock V4 audio conversion ABI separate from the platform V5.
    (
        'vendor/lib64/soundfx/libbundleaidl_prebuilt.so',
        'vendor/lib64/soundfx/libdownmixaidl_prebuilt.so',
        'vendor/lib64/soundfx/libdynamicsprocessingaidl_prebuilt.so',
        'vendor/lib64/soundfx/libhapticgeneratoraidl_prebuilt.so',
        'vendor/lib64/soundfx/libloudnessenhanceraidl_prebuilt.so',
        'vendor/lib64/soundfx/libreverbaidl_prebuilt.so',
        'vendor/lib64/soundfx/libvisualizeraidl_prebuilt.so',
        'vendor/lib64/android.hardware.audio.core-impl-mediatek.so',
        'vendor/lib64/hw/android.hardware.audio.effect.aidl-impl-mediatek.so',
        'vendor/lib64/hw/android.hardware.soundtrigger3-impl.so',
        'vendor/lib64/soundfx/libaecsw_prebuilt.so',
        'vendor/lib64/soundfx/libaecsw_mtk.so',
        'vendor/lib64/soundfx/libagc1sw_prebuilt.so',
        'vendor/lib64/soundfx/libagc1sw_mtk.so',
        'vendor/lib64/soundfx/libagc2sw_prebuilt.so',
        'vendor/lib64/soundfx/libagc2sw_mtk.so',
        'vendor/lib64/soundfx/libbassboostsw_prebuilt.so',
        'vendor/lib64/soundfx/libenvreverbsw_prebuilt.so',
        'vendor/lib64/soundfx/libequalizersw_prebuilt.so',
        'vendor/lib64/soundfx/libextensioneffect_prebuilt.so',
        'vendor/lib64/soundfx/libnssw_prebuilt.so',
        'vendor/lib64/soundfx/libnssw_mtk.so',
        'vendor/lib64/soundfx/libpreprocessingaidl_prebuilt.so',
        'vendor/lib64/soundfx/libpreprocessingaidl_mtk.so',
        'vendor/lib64/soundfx/libpresetreverbsw_prebuilt.so',
        'vendor/lib64/soundfx/libspatializeraidl.so',
        'vendor/lib64/soundfx/libvirtualizersw_prebuilt.so',
        'vendor/lib64/soundfx/libvolumesw_prebuilt.so',
        'vendor/bin/hw/android.hardware.audio.service-aidl.mediatek',
    ): blob_fixup()
        .replace_needed(
            'libaudio_aidl_conversion_common_ndk.so',
            'libaudio_aidl_conversion_common_ndk_prebuilt.so',
        ),
    'vendor/lib64/android.hardware.audio.core-impl-mediatek.so': blob_fixup()
        .add_needed('libaudioutils_shim.so'),
    # MTK's V1 blobs use audio.common V4, matching upstream's frozen V4.
    (
        'vendor/lib64/android.hardware.bluetooth.audio-impl-mediatek.so',
        'vendor/lib64/hw/audio.primary.mediatek.so',
        'vendor/lib64/hw/vendor.mediatek.hardware.bluetooth.audio@2.1-impl.so',
        'vendor/lib64/hw/vendor.mediatek.hardware.bluetooth.audio@2.2-impl.so',
        'vendor/lib64/libbluetooth_audio_session_aidl_displayone.so',
        'vendor/lib64/libbluetooth_audio_session_aidl_displaytwo.so',
        'vendor/lib64/libbluetooth_audio_session_aidl_mtk.so',
    ): blob_fixup()
        .replace_needed(
            'vendor.mediatek.hardware.bluetooth.audio-V1-ndk.so',
            'vendor.mediatek.hardware.bluetooth.audio-V4-ndk.so',
        ),
    'vendor/lib64/libbluetooth_audio_session_aidl_mtk.so': blob_fixup()
        .binary_regex_replace(
            br'/vendor/etc/aidl/le_audio/aidl_default_audio_set_configurations\.json(?!_)',
            b'/vendor/etc/aidl/le_audio/aidl_default_audio_set_configurations.aura',
        )
        .binary_regex_replace(
            br'/vendor/etc/aidl/le_audio/aidl_default_audio_set_scenarios\.json(?!_)',
            b'/vendor/etc/aidl/le_audio/aidl_default_audio_set_scenarios.aura',
        ),
    (
        'vendor/lib64/hw/android.hardware.audio.effect.aidl-impl-mediatek.so',
        'vendor/lib64/hw/audio.primary.mediatek.so',
        'vendor/lib64/hw/hwcomposer.mtk_common.so',
        'vendor/lib64/lib_power_applist.so',
        'vendor/lib64/libmtkgpuserv.so',
        'vendor/lib64/libpowerhal.so',
        'vendor/lib64/libpqxmlflagparser.so',
        'vendor/lib64/libpqxmlparser.so',
        'vendor/lib64/librt_extamp_intf.so',
        'vendor/lib64/libsilkybrightnesscore.so',
    ): blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2_mtk.so'),
    (
        'vendor/lib64/libmcve.so',
        'vendor/lib64/libtflite_mtk.so',
    ): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock'),
    'vendor/lib64/mt6789/libneuron_adapter_mgvi.so': blob_fixup()
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_unlock'),
    'vendor/lib64/libneuralnetworks_sl_driver_mtk_prebuilt.so': blob_fixup()
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_createFromHandle')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_getNativeHandle')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock'),
    'vendor/lib64/libteei_daemon_vfs.so': blob_fixup()
        .add_needed('liblog.so'),
    'vendor/etc/init/android.hardware.wifi.supplicant-service-stock.rc': blob_fixup()
        .regex_replace(
            r'/vendor/bin/hw/wpa_supplicant(?=\s)',
            '/vendor/bin/hw/wpa_supplicant-stock',
        )
        .regex_replace(
            r'(?m)^[ \t]*#   we will start as root.*\n'
            r'[ \t]*#   after setting up.*\n'
            r'[ \t]*#   user wifi\n'
            r'[ \t]*#   group wifi inet keystore\n',
            '',
        )
        .regex_replace(
            r'(?m)^([ \t]+)user root$',
            r'\1user wifi\n\1group wifi net_raw net_admin\n'
            r'\1capabilities NET_RAW NET_ADMIN',
        ),
    'vendor/etc/init/hw/init.mt6789.rc': blob_fixup()
        .regex_replace(
            r'(?s)\n# add meta_tst for ATM\n.*?(?=\n# MTK fast charging support\n)',
            '',
        ),
    'vendor/etc/init/hw/init.connectivity.common.rc': blob_fixup()
        .regex_replace(
            r'\nservice gnss_daemon /vendor/bin/gnss_daemon\n'
            r'    class main\n'
            r'    user gps\n'
            r'    group gps inet misc sdcard_rw sdcard_r media_rw system radio wakelock\n'
            r'    socket gnss_daemon stream 660 gps system\n',
            '',
        ),
    'vendor/etc/gnss/agps_profiles_conf2.xml': blob_fixup()
        .regex_replace(
            r'(?s)  <carrier_mapping name="Test_SIM1">.*?'
            r'  </carrier_mapping>\n'
            r'  <carrier_mapping name="Test_SIM2">.*?'
            r'  </carrier_mapping>\n',
            '',
        ),
    'vendor/lib64/hw/mt6789/vendor.mediatek.hardware.pq_aidl-impl.so': blob_fixup()
        .remove_needed('android.hardware.sensors-V2-ndk.so')
        .replace_needed('libtinyxml2.so', 'libtinyxml2_mtk.so'),
    'vendor/lib64/mt6789/libcam.halsensor.so': blob_fixup()
        .remove_needed('libcam.halsensor.hwintegration_mt6855.so'),
    'vendor/lib64/mt6789/libcam.utils.sensorprovider.so': blob_fixup()
        .remove_needed('android.hardware.sensors-V2-ndk.so'),
    'vendor/lib64/mt6789/libcamera.custom.pd_buf_mgr.so': blob_fixup()
        .call(fix_ov64b40_pdaf_geometry, need_tmp_dir=False),
    'vendor/lib64/mt6789/libmtkcam_grallocutils.so': blob_fixup()
        .remove_needed('android.hardware.graphics.common-V5-ndk.so'),
}

lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    (
        'android.hardware.audio.common-V3-ndk',
        'android.hardware.audio.effect-V2-ndk',
        'android.hardware.bluetooth.audio-V4-ndk',
        'android.hardware.camera.common-V2-ndk',
        'android.hardware.camera.device-V3-ndk',
        'android.hardware.camera.provider-V3-ndk',
        'android.hardware.graphics.common-V6-ndk',
        'android.media.audio.common.types-V3-ndk',
    ): lib_fixup_vendorcompat,
}

module = ExtractUtilsModule(
    'Aura_A1',
    'bluefox',
    namespace_imports=namespace_imports,
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    add_firmware_proprietary_file=True,
)

main_proprietary_file = next(
    proprietary_file
    for proprietary_file in module.proprietary_files
    if proprietary_file.file_list_path
    == module.proprietary_file_path('proprietary-files.txt')
)
main_proprietary_file.add_post_makefile_generation_fn(
    write_noninstallable_compat_libraries
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
