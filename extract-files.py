#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.bp_builder import BpBuilder, FileBpBuilder
from extract_utils.bp_encoder import BpJSONEncoder
from extract_utils.fixups_blob import blob_fixup, blob_fixups_user_type
from extract_utils.fixups_lib import (
    lib_fixup_vendorcompat,
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import ExtractUtils, ExtractUtilsModule
from extract_utils.makefiles import write_elfs_package, write_packages_inclusion

namespace_imports = [
    'device/bluefox/Aura_A1',
    'hardware/mediatek',
    'hardware/mediatek/libmtkperf_client',
]

# These stock-only binaries are the complete 4 KiB PT_LOAD exception set.
# Keep normal ELF dependency checks enabled and waive only the 16 KiB alignment
# gate that cannot be repaired without relinking the proprietary objects.
page_size_4k_exceptions = (
    ('vendor/lib64/libcamplugin.pi.dyn_prvportrait.so', False),
    ('vendor/lib64/libcamplugin.pi.dyn_stillportrait.so', False),
    ('vendor/lib64/libpi_feature_check.so', False),
    ('vendor/lib64/libpi_feature_util.so', False),
)

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


def write_page_size_4k_exceptions(ctx, packages_ctx):
    encoder = BpJSONEncoder(legacy=ctx.legacy)
    package_names = []

    for file_path, is_binary in page_size_4k_exceptions:
        file = main_proprietary_file.file_list.all_files.get_file(file_path)
        base_dir = 'vendor/bin' if is_binary else 'vendor/lib64'
        prefix_len = len(f'{base_dir}/')
        builder = FileBpBuilder(
            file,
            prefix_len,
            packages_ctx.vendor_prop_rel_sub_path,
            encoder,
        ).set_owner(packages_ctx.vendor)
        package_name = write_elfs_package(
            [file],
            builder,
            packages_ctx,
            is_bin=is_binary,
        )
        builder.set('ignore_max_page_size', True).write(ctx.bp_out)
        package_names.append(package_name)

    write_packages_inclusion(package_names, ctx.product_mk_out)


blob_fixups: blob_fixups_user_type = {
    'system_ext/priv-app/MtkTelephonyAssist/MtkTelephonyAssist.apk': blob_fixup()
        .apktool_patch('blob-patches/MtkTelephonyAssist'),
    'system_ext/priv-app/ImsService/ImsService.apk': blob_fixup()
        .apktool_patch('blob-patches/ImsService'),
    'system_ext/lib64/libimsma.so': blob_fixup()
        .replace_needed('libsink.so', 'libsink-mtk.so'),
    'vendor/etc/audio_effects_config.xml': blob_fixup()
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
        ),
    'vendor/bin/mnld': blob_fixup()
        .remove_needed('android.hardware.sensors-V2-ndk.so'),
    'vendor/lib64/android.hardware.audio.core-impl-mediatek.so': blob_fixup()
        .add_needed('libaudioutils_shim.so'),
    (
        'vendor/lib64/libbluetooth_audio_session_aidl.so',
        'vendor/lib64/libbluetooth_audio_session_aidl_mtk.so',
    ): blob_fixup()
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
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
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
    'vendor/etc/init/android.hardware.drm-service.clearkey-stock.rc': blob_fixup()
        .regex_replace(
            r'/vendor/bin/hw/android\.hardware\.drm-service\.clearkey(?=\s)',
            '/vendor/bin/hw/android.hardware.drm-service.clearkey-stock',
        ),
    'vendor/etc/init/android.hardware.wifi-service-lazy-stock.rc': blob_fixup()
        .regex_replace(
            r'/vendor/bin/hw/android\.hardware\.wifi-service-lazy(?=\s)',
            '/vendor/bin/hw/android.hardware.wifi-service-lazy-stock',
        ),
    'vendor/etc/init/android.hardware.wifi.supplicant-service-stock.rc': blob_fixup()
        .regex_replace(
            r'/vendor/bin/hw/wpa_supplicant(?=\s)',
            '/vendor/bin/hw/wpa_supplicant-stock',
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
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v34.so'),
    'vendor/lib64/mt6789/libcam.halsensor.so': blob_fixup()
        .remove_needed('libcam.halsensor.hwintegration_mt6855.so'),
    'vendor/lib64/mt6789/libcam.utils.sensorprovider.so': blob_fixup()
        .remove_needed('android.hardware.sensors-V2-ndk.so'),
    'vendor/lib64/mt6789/libcamera.custom.pd_buf_mgr.so': blob_fixup()
        .binary_regex_replace(
            b'\x19\x47\x80\x52\x18\xd7\x80\x52',
            b'\x19\x8e\x80\x52\x98\x6b\x80\x52',
        ),
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
main_proprietary_file.add_post_makefile_generation_fn(
    write_page_size_4k_exceptions
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
