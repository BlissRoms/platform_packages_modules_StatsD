/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.cts.statsd.atom;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import android.os.WakeLockLevelEnum;

import com.android.internal.os.StatsdConfigProto.FieldMatcher;
import com.android.internal.os.StatsdConfigProto.StatsdConfig;
import com.android.os.AtomsProto.AppStartChanged;
import com.android.os.AtomsProto.Atom;
import com.android.os.AtomsProto.AudioStateChanged;
import com.android.os.AtomsProto.BleScanResultReceived;
import com.android.os.AtomsProto.BleScanStateChanged;
import com.android.os.AtomsProto.BleUnoptimizedScanStateChanged;
import com.android.os.AtomsProto.CameraStateChanged;
import com.android.os.AtomsProto.CpuTimePerUid;
import com.android.os.AtomsProto.CpuTimePerUidFreq;
import com.android.os.AtomsProto.DropboxErrorChanged;
import com.android.os.AtomsProto.FlashlightStateChanged;
import com.android.os.AtomsProto.ForegroundServiceStateChanged;
import com.android.os.AtomsProto.GpsScanStateChanged;
import com.android.os.AtomsProto.MediaCodecActivityChanged;
import com.android.os.AtomsProto.OverlayStateChanged;
import com.android.os.AtomsProto.PictureInPictureStateChanged;
import com.android.os.AtomsProto.ScheduledJobStateChanged;
import com.android.os.AtomsProto.SyncStateChanged;
import com.android.os.AtomsProto.WakelockStateChanged;
import com.android.os.AtomsProto.WifiLockStateChanged;
import com.android.os.AtomsProto.WifiMulticastLockStateChanged;
import com.android.os.AtomsProto.WifiScanStateChanged;
import com.android.os.StatsLog.EventMetricData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Statsd atom tests that are done via app, for atoms that report a uid.
 */
public class UidAtomTests extends DeviceAtomTestCase {

    private static final String TAG = "Statsd.UidAtomTests";

    // These constants are those in PackageManager.
    private static final String FEATURE_BLUETOOTH_LE = "android.hardware.bluetooth_le";
    private static final String FEATURE_LOCATION_GPS = "android.hardware.location.gps";
    private static final String FEATURE_WIFI = "android.hardware.wifi";
    private static final String FEATURE_CAMERA_FLASH = "android.hardware.camera.flash";
    private static final String FEATURE_CAMERA = "android.hardware.camera";
    private static final String FEATURE_CAMERA_FRONT = "android.hardware.camera.front";
    private static final String FEATURE_AUDIO_OUTPUT = "android.hardware.audio.output";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testAppStartChanged() throws Exception {
        final int atomTag = Atom.APP_START_CHANGED_FIELD_NUMBER;

        createAndUploadConfig(atomTag, false);
        Thread.sleep(WAIT_TIME_SHORT);

        runActivity("StatsdCtsForegroundActivity", "action", "action.sleep_top");

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        AppStartChanged atom = data.get(0).getAtom().getAppStartChanged();
        assertEquals("com.android.server.cts.device.statsd", atom.getPkgName());
        assertEquals("com.android.server.cts.device.statsd.StatsdCtsForegroundActivity",
                atom.getActivityName());
        assertFalse(atom.getIsInstantApp());
        assertTrue(atom.getActivityStartMillis() > 0);
        assertTrue(atom.getTransitionDelayMillis() > 0);
    }

    public void testBleScan() throws Exception {
        if (!hasFeature(FEATURE_BLUETOOTH_LE, true)) return;

        final int atom = Atom.BLE_SCAN_STATE_CHANGED_FIELD_NUMBER;
        final int field = BleScanStateChanged.STATE_FIELD_NUMBER;
        final int stateOn = BleScanStateChanged.State.ON_VALUE;
        final int stateOff = BleScanStateChanged.State.OFF_VALUE;
        final int minTimeDiffMillis = 1_500;
        final int maxTimeDiffMillis = 3_000;

        List<EventMetricData> data = doDeviceMethodOnOff("testBleScanUnoptimized", atom, field,
                stateOn, stateOff, minTimeDiffMillis, maxTimeDiffMillis, true);

        BleScanStateChanged a0 = data.get(0).getAtom().getBleScanStateChanged();
        BleScanStateChanged a1 = data.get(1).getAtom().getBleScanStateChanged();
        assertTrue(a0.getState().getNumber() == stateOn);
        assertTrue(a1.getState().getNumber() == stateOff);
    }

    public void testBleUnoptimizedScan() throws Exception {
        if (!hasFeature(FEATURE_BLUETOOTH_LE, true)) return;

        final int atom = Atom.BLE_UNOPTIMIZED_SCAN_STATE_CHANGED_FIELD_NUMBER;
        final int field = BleUnoptimizedScanStateChanged.STATE_FIELD_NUMBER;
        final int stateOn = BleUnoptimizedScanStateChanged.State.ON_VALUE;
        final int stateOff = BleUnoptimizedScanStateChanged.State.OFF_VALUE;
        final int minTimeDiffMillis = 1_500;
        final int maxTimeDiffMillis = 3_000;

        List<EventMetricData> data = doDeviceMethodOnOff("testBleScanUnoptimized", atom, field,
                stateOn, stateOff, minTimeDiffMillis, maxTimeDiffMillis, true);

        BleUnoptimizedScanStateChanged a0 = data.get(0).getAtom().getBleUnoptimizedScanStateChanged();
        BleUnoptimizedScanStateChanged a1 = data.get(1).getAtom().getBleUnoptimizedScanStateChanged();
        assertTrue(a0.getState().getNumber() == stateOn);
        assertTrue(a1.getState().getNumber() == stateOff);

        // Now repeat the test for optimized scanning and make sure no atoms are reported.
        StatsdConfig.Builder conf = createConfigBuilder();
        addAtomEvent(conf, atom, createFvm(field).setEqInt(stateOn));
        addAtomEvent(conf, atom, createFvm(field).setEqInt(stateOff));
        data = doDeviceMethod("testBleScanOptimized", conf);
        assertTrue(data.isEmpty());
    }

    public void testBleScanResult() throws Exception {
        if (!hasFeature(FEATURE_BLUETOOTH_LE, true)) return;
        turnScreenOn(); // BLE results are not given unless screen is on. TODO: make more robust.

        final int atom = Atom.BLE_SCAN_RESULT_RECEIVED_FIELD_NUMBER;
        final int field = BleScanResultReceived.NUM_OF_RESULTS_FIELD_NUMBER;

        StatsdConfig.Builder conf = createConfigBuilder();
        addAtomEvent(conf, atom, createFvm(field).setGteInt(0));
        List<EventMetricData> data = doDeviceMethod("testBleScanResult", conf);

        assertTrue(data.size() >= 1);
        BleScanResultReceived a0 = data.get(0).getAtom().getBleScanResultReceived();
        assertTrue(a0.getNumOfResults() >= 1);

        turnScreenOff();
    }

    public void testCameraState() throws Exception {
        if (!hasFeature(FEATURE_CAMERA, true) && !hasFeature(FEATURE_CAMERA_FRONT, true)) return;

        final int atomTag = Atom.CAMERA_STATE_CHANGED_FIELD_NUMBER;
        Set<Integer> cameraOn = new HashSet<>(Arrays.asList(CameraStateChanged.State.ON_VALUE));
        Set<Integer> cameraOff = new HashSet<>(Arrays.asList(CameraStateChanged.State.OFF_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(cameraOn, cameraOff);

        createAndUploadConfig(atomTag, true);  // True: uses attribution.
        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", "testCameraState");

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        // Assert that the events happened in the expected order.
        assertStatesOccurred(stateSet, data, WAIT_TIME_LONG,
                atom -> atom.getCameraStateChanged().getState().getNumber());
    }

    public void testCpuTimePerUid() throws Exception {
        StatsdConfig.Builder config = getPulledAndAnomalyConfig();
        FieldMatcher.Builder dimension = FieldMatcher.newBuilder()
                .setField(Atom.CPU_TIME_PER_UID_FIELD_NUMBER)
                .addChild(FieldMatcher.newBuilder()
                        .setField(CpuTimePerUid.UID_FIELD_NUMBER));
        addGaugeAtom(config, Atom.CPU_TIME_PER_UID_FIELD_NUMBER, dimension);

        uploadConfig(config);

        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", "testSimpleCpu");

        turnScreenOff();
        Thread.sleep(WAIT_TIME_SHORT);
        turnScreenOn();
        Thread.sleep(WAIT_TIME_SHORT);

        List<Atom> atomList = getGaugeMetricDataList();

        // TODO: We don't have atom matching on gauge yet. Let's refactor this after that feature is
        // implemented.
        boolean found = false;
        int uid = getUid();
        for (Atom atom : atomList) {
            if (atom.getCpuTimePerUid().getUid() == uid) {
                found = true;
                assertTrue(atom.getCpuTimePerUid().getUserTimeMillis() > 0);
                assertTrue(atom.getCpuTimePerUid().getSysTimeMillis() > 0);
            }
        }
        assertTrue("found uid " + uid, found);
    }

    public void testCpuTimePerUidFreq() throws Exception {
        StatsdConfig.Builder config = getPulledAndAnomalyConfig();
        FieldMatcher.Builder dimension = FieldMatcher.newBuilder()
                .setField(Atom.CPU_TIME_PER_UID_FREQ_FIELD_NUMBER)
                .addChild(FieldMatcher.newBuilder()
                        .setField(CpuTimePerUidFreq.UID_FIELD_NUMBER));
        addGaugeAtom(config, Atom.CPU_TIME_PER_UID_FREQ_FIELD_NUMBER, dimension);

        uploadConfig(config);

        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", "testSimpleCpu");

        turnScreenOff();
        Thread.sleep(WAIT_TIME_SHORT);
        turnScreenOn();
        Thread.sleep(WAIT_TIME_SHORT);

        List<Atom> atomList = getGaugeMetricDataList();

        // TODO: We don't have atom matching on gauge yet. Let's refactor this after that feature is
        // implemented.
        boolean found = false;
        int uid = getUid();
        long timeSpent = 0;
        for (Atom atom : atomList) {
            if (atom.getCpuTimePerUidFreq().getUid() == uid) {
                found = true;
                timeSpent += atom.getCpuTimePerUidFreq().getTimeMillis();
            }
        }
        assertTrue(timeSpent > 0);
        assertTrue("found uid " + uid, found);
    }

    public void testFlashlightState() throws Exception {
        if (!hasFeature(FEATURE_CAMERA_FLASH, true)) return;

        final int atomTag = Atom.FLASHLIGHT_STATE_CHANGED_FIELD_NUMBER;
        final String name = "testFlashlight";

        Set<Integer> flashlightOn = new HashSet<>(
            Arrays.asList(FlashlightStateChanged.State.ON_VALUE));
        Set<Integer> flashlightOff = new HashSet<>(
            Arrays.asList(FlashlightStateChanged.State.OFF_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(flashlightOn, flashlightOff);

        createAndUploadConfig(atomTag, true);  // True: uses attribution.
        Thread.sleep(WAIT_TIME_SHORT);

        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", name);

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        // Assert that the events happened in the expected order.
        assertStatesOccurred(stateSet, data, WAIT_TIME_SHORT,
                atom -> atom.getFlashlightStateChanged().getState().getNumber());
    }

    public void testForegroundServiceState() throws Exception {
        final int atomTag = Atom.FOREGROUND_SERVICE_STATE_CHANGED_FIELD_NUMBER;
        final String name = "testForegroundService";

        Set<Integer> enterForeground = new HashSet<>(
                Arrays.asList(ForegroundServiceStateChanged.State.ENTER_VALUE));
        Set<Integer> exitForeground = new HashSet<>(
                Arrays.asList(ForegroundServiceStateChanged.State.EXIT_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(enterForeground, exitForeground);

        createAndUploadConfig(atomTag, false);
        Thread.sleep(WAIT_TIME_SHORT);

        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", name);

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        // Assert that the events happened in the expected order.
        assertStatesOccurred(stateSet, data, WAIT_TIME_SHORT,
                atom -> atom.getForegroundServiceStateChanged().getState().getNumber());
    }

    public void testGpsScan() throws Exception {
        if (!hasFeature(FEATURE_LOCATION_GPS, true)) return;
        // Whitelist this app against background location request throttling
        getDevice().executeShellCommand(String.format(
                "settings put global location_background_throttle_package_whitelist %s",
                DEVICE_SIDE_TEST_PACKAGE));

        final int atom = Atom.GPS_SCAN_STATE_CHANGED_FIELD_NUMBER;
        final int key = GpsScanStateChanged.STATE_FIELD_NUMBER;
        final int stateOn = GpsScanStateChanged.State.ON_VALUE;
        final int stateOff = GpsScanStateChanged.State.OFF_VALUE;
        final int minTimeDiffMillis = 500;
        final int maxTimeDiffMillis = 60_000;

        List<EventMetricData> data = doDeviceMethodOnOff("testGpsScan", atom, key,
                stateOn, stateOff, minTimeDiffMillis, maxTimeDiffMillis, true);

        GpsScanStateChanged a0 = data.get(0).getAtom().getGpsScanStateChanged();
        GpsScanStateChanged a1 = data.get(1).getAtom().getGpsScanStateChanged();
        assertTrue(a0.getState().getNumber() == stateOn);
        assertTrue(a1.getState().getNumber() == stateOff);
    }

    public void testDavey() throws Exception {
        long MAX_DURATION = 2000;
        long MIN_DURATION = 750;
        final int atomTag = Atom.DAVEY_OCCURRED_FIELD_NUMBER;
        createAndUploadConfig(atomTag, false); // UID is logged without attribution node

        runActivity("DaveyActivity", null, null);

        List<EventMetricData> data = getEventMetricDataList();
        assertTrue(data.size() == 1);
        long duration = data.get(0).getAtom().getDaveyOccurred().getJankDurationMillis();
        assertTrue("Jank duration of " + duration + "ms was less than " + MIN_DURATION + "ms",
                duration >= MIN_DURATION);
        assertTrue("Jank duration of " + duration + "ms was longer than " + MAX_DURATION + "ms",
                duration <= MAX_DURATION);
    }

    public void testScheduledJobState() throws Exception {
        String expectedName = "com.android.server.cts.device.statsd/.StatsdJobService";
        final int atomTag = Atom.SCHEDULED_JOB_STATE_CHANGED_FIELD_NUMBER;
        Set<Integer> jobSchedule = new HashSet<>(
                Arrays.asList(ScheduledJobStateChanged.State.SCHEDULED_VALUE));
        Set<Integer> jobOn = new HashSet<>(
                Arrays.asList(ScheduledJobStateChanged.State.STARTED_VALUE));
        Set<Integer> jobOff = new HashSet<>(
                Arrays.asList(ScheduledJobStateChanged.State.FINISHED_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(jobSchedule, jobOn, jobOff);

        createAndUploadConfig(atomTag, true);  // True: uses attribution.
        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", "testScheduledJob");

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        assertStatesOccurred(stateSet, data, 0,
                atom -> atom.getScheduledJobStateChanged().getState().getNumber());

        for (EventMetricData e : data) {
            assertTrue(e.getAtom().getScheduledJobStateChanged().getName().equals(expectedName));
        }
    }

    public void testSyncState() throws Exception {
        final int atomTag = Atom.SYNC_STATE_CHANGED_FIELD_NUMBER;
        Set<Integer> syncOn = new HashSet<>(Arrays.asList(SyncStateChanged.State.ON_VALUE));
        Set<Integer> syncOff = new HashSet<>(Arrays.asList(SyncStateChanged.State.OFF_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(syncOn, syncOff, syncOn, syncOff);

        createAndUploadConfig(atomTag, true);
        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", "testSyncState");

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        // Assert that the events happened in the expected order.
        assertStatesOccurred(stateSet, data, WAIT_TIME_SHORT,
                atom -> atom.getSyncStateChanged().getState().getNumber());
    }

    public void testWakelockState() throws Exception {
        final int atomTag = Atom.WAKELOCK_STATE_CHANGED_FIELD_NUMBER;
        Set<Integer> wakelockOn = new HashSet<>(Arrays.asList(
                WakelockStateChanged.State.ACQUIRE_VALUE,
                WakelockStateChanged.State.CHANGE_ACQUIRE_VALUE));
        Set<Integer> wakelockOff = new HashSet<>(Arrays.asList(
                WakelockStateChanged.State.RELEASE_VALUE,
                WakelockStateChanged.State.CHANGE_RELEASE_VALUE));

        final String EXPECTED_TAG = "StatsdPartialWakelock";
        final WakeLockLevelEnum EXPECTED_LEVEL = WakeLockLevelEnum.PARTIAL_WAKE_LOCK;

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(wakelockOn, wakelockOff);

        createAndUploadConfig(atomTag, true);  // True: uses attribution.
        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", "testWakelockState");

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        // Assert that the events happened in the expected order.
        assertStatesOccurred(stateSet, data, WAIT_TIME_SHORT,
            atom -> atom.getWakelockStateChanged().getState().getNumber());

        for (EventMetricData event: data) {
            String tag = event.getAtom().getWakelockStateChanged().getTag();
            WakeLockLevelEnum type = event.getAtom().getWakelockStateChanged().getLevel();
            assertTrue("Expected tag: " + EXPECTED_TAG + ", but got tag: " + tag,
                    tag.equals(EXPECTED_TAG));
            assertTrue("Expected wakelock level: " + EXPECTED_LEVEL  + ", but got level: " + type,
                    type == EXPECTED_LEVEL);
        }
    }

    public void testWakeupAlarm() throws Exception {
        final int atomTag = Atom.WAKEUP_ALARM_OCCURRED_FIELD_NUMBER;

        StatsdConfig.Builder config = createConfigBuilder();
        addAtomEvent(config, atomTag, true);  // True: uses attribution.

        List<EventMetricData> data = doDeviceMethod("testWakeupAlarm", config);
        assertTrue(data.size() >= 1);
        for (int i = 0; i < data.size(); i++) {
            String tag = data.get(i).getAtom().getWakeupAlarmOccurred().getTag();
            assertTrue(tag.equals("*walarm*:android.cts.statsd.testWakeupAlarm"));
        }
    }

    public void testWifiLock() throws Exception {
        if (!hasFeature(FEATURE_WIFI, true)) return;

        final int atomTag = Atom.WIFI_LOCK_STATE_CHANGED_FIELD_NUMBER;
        Set<Integer> lockOn = new HashSet<>(Arrays.asList(WifiLockStateChanged.State.ON_VALUE));
        Set<Integer> lockOff = new HashSet<>(Arrays.asList(WifiLockStateChanged.State.OFF_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(lockOn, lockOff);

        createAndUploadConfig(atomTag, true);  // True: uses attribution.
        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", "testWifiLock");

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        // Assert that the events happened in the expected order.
        assertStatesOccurred(stateSet, data, WAIT_TIME_SHORT,
                atom -> atom.getWifiLockStateChanged().getState().getNumber());
    }

    public void testWifiMulticastLock() throws Exception {
        if (!hasFeature(FEATURE_WIFI, true)) return;

        final int atomTag = Atom.WIFI_MULTICAST_LOCK_STATE_CHANGED_FIELD_NUMBER;
        Set<Integer> lockOn = new HashSet<>(
                Arrays.asList(WifiMulticastLockStateChanged.State.ON_VALUE));
        Set<Integer> lockOff = new HashSet<>(
                Arrays.asList(WifiMulticastLockStateChanged.State.OFF_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(lockOn, lockOff);

        createAndUploadConfig(atomTag, true);
        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", "testWifiMulticastLock");

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        // Assert that the events happened in the expected order.
        assertStatesOccurred(stateSet, data, WAIT_TIME_SHORT,
                atom -> atom.getWifiMulticastLockStateChanged().getState().getNumber());
    }

    public void testWifiScan() throws Exception {
        if (!hasFeature(FEATURE_WIFI, true)) return;

        final int atom = Atom.WIFI_SCAN_STATE_CHANGED_FIELD_NUMBER;
        final int key = WifiScanStateChanged.STATE_FIELD_NUMBER;
        final int stateOn = WifiScanStateChanged.State.ON_VALUE;
        final int stateOff = WifiScanStateChanged.State.OFF_VALUE;
        final int minTimeDiffMillis = 500;
        final int maxTimeDiffMillis = 60_000;
        final boolean demandExactlyTwo = false; // Two scans are performed, so up to 4 atoms logged.

        List<EventMetricData> data = doDeviceMethodOnOff("testWifiScan", atom, key,
                stateOn, stateOff, minTimeDiffMillis, maxTimeDiffMillis, demandExactlyTwo);

        assertTrue(data.size() >= 2);
        assertTrue(data.size() <= 4);
        WifiScanStateChanged a0 = data.get(0).getAtom().getWifiScanStateChanged();
        WifiScanStateChanged a1 = data.get(1).getAtom().getWifiScanStateChanged();
        assertTrue(a0.getState().getNumber() == stateOn);
        assertTrue(a1.getState().getNumber() == stateOff);
    }

    public void testAudioState() throws Exception {
        if (!hasFeature(FEATURE_AUDIO_OUTPUT, true)) return;

        final int atomTag = Atom.AUDIO_STATE_CHANGED_FIELD_NUMBER;
        final String name = "testAudioState";

        Set<Integer> onState = new HashSet<>(
                Arrays.asList(AudioStateChanged.State.ON_VALUE));
        Set<Integer> offState = new HashSet<>(
                Arrays.asList(AudioStateChanged.State.OFF_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(onState, offState);

        createAndUploadConfig(atomTag, true);  // True: uses attribution.
        Thread.sleep(WAIT_TIME_SHORT);

        runDeviceTests(DEVICE_SIDE_TEST_PACKAGE, ".AtomTests", name);

        Thread.sleep(WAIT_TIME_SHORT);
        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        // AudioStateChanged timestamp is fuzzed to 5min buckets
        assertStatesOccurred(stateSet, data, 0,
                atom -> atom.getAudioStateChanged().getState().getNumber());
    }

    public void testMediaCodecActivity() throws Exception {
        final int atomTag = Atom.MEDIA_CODEC_ACTIVITY_CHANGED_FIELD_NUMBER;

        Set<Integer> onState = new HashSet<>(
                Arrays.asList(MediaCodecActivityChanged.State.ON_VALUE));
        Set<Integer> offState = new HashSet<>(
                Arrays.asList(MediaCodecActivityChanged.State.OFF_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(onState, offState);

        turnScreenOn();
        createAndUploadConfig(atomTag, true);  // True: uses attribution.
        Thread.sleep(WAIT_TIME_SHORT);

        runActivity("VideoPlayerActivity", "action", "action.play_video");

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        turnScreenOff();
        // Assert that the events happened in the expected order.
        assertStatesOccurred(stateSet, data, WAIT_TIME_LONG,
                atom -> atom.getMediaCodecActivityChanged().getState().getNumber());
    }

    public void testPictureInPictureState() throws Exception {
        final int atomTag = Atom.PICTURE_IN_PICTURE_STATE_CHANGED_FIELD_NUMBER;

        Set<Integer> entered = new HashSet<>(
                Arrays.asList(PictureInPictureStateChanged.State.ENTERED_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(entered);

        turnScreenOn();
        createAndUploadConfig(atomTag, false);

        runActivity("VideoPlayerActivity", "action", "action.play_video_picture_in_picture_mode");

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        turnScreenOff();
        // Assert that the events happened in the expected order.
        assertStatesOccurred(stateSet, data, WAIT_TIME_LONG,
                atom -> atom.getPictureInPictureStateChanged().getState().getNumber());
    }

    public void testOverlayState() throws Exception {
        final int atomTag = Atom.OVERLAY_STATE_CHANGED_FIELD_NUMBER;

        Set<Integer> entered = new HashSet<>(
                Arrays.asList(OverlayStateChanged.State.ENTERED_VALUE));
        Set<Integer> exited = new HashSet<>(
                Arrays.asList(OverlayStateChanged.State.EXITED_VALUE));

        // Add state sets to the list in order.
        List<Set<Integer>> stateSet = Arrays.asList(entered, exited);

        createAndUploadConfig(atomTag, false);

        runActivity("StatsdCtsForegroundActivity", "action", "action.show_application_overlay",
                3_000);

        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        // Assert that the events happened in the expected order.
        // The overlay box should appear about 2sec after the app start, we wait 3sec in total
        // before force-stop it. So the duration is about 1 sec
        assertStatesOccurred(stateSet, data, 1_000,
                atom -> atom.getOverlayStateChanged().getState().getNumber());
    }

    public void testDropboxErrorChanged() throws Exception {
        final int atomTag = Atom.DROPBOX_ERROR_CHANGED_FIELD_NUMBER;
        createAndUploadConfig(atomTag, false);
        Thread.sleep(WAIT_TIME_SHORT);

        runActivity("StatsdCtsForegroundActivity", "action", "action.crash");

        Thread.sleep(WAIT_TIME_SHORT);
        // Sorted list of events in order in which they occurred.
        List<EventMetricData> data = getEventMetricDataList();

        DropboxErrorChanged atom = data.get(0).getAtom().getDropboxErrorChanged();
        assertTrue(atom.getIsInstantApp() == 0);
        assertEquals("data_app_crash", atom.getTag());
    }
}
