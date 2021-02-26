/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.os;


import static com.google.common.truth.Truth.assertThat;

import android.net.NetworkCapabilities;
import android.net.NetworkStats;
import android.os.BatteryConsumer;
import android.os.Process;
import android.os.SystemBatteryConsumer;
import android.os.UidBatteryConsumer;
import android.os.WorkSource;
import android.os.connectivity.WifiActivityEnergyInfo;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WifiPowerCalculatorTest {
    private static final double PRECISION = 0.00001;

    private static final int APP_UID = Process.FIRST_APPLICATION_UID + 42;

    @Rule
    public final BatteryUsageStatsRule mStatsRule = new BatteryUsageStatsRule()
            .setAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_IDLE, 360.0)
            .setAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_RX, 480.0)
            .setAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_TX, 720.0)
            .setAveragePower(PowerProfile.POWER_WIFI_ON, 360.0)
            .setAveragePower(PowerProfile.POWER_WIFI_SCAN, 480.0)
            .setAveragePower(PowerProfile.POWER_WIFI_BATCHED_SCAN, 720.0)
            .setAveragePower(PowerProfile.POWER_WIFI_ACTIVE, 1080.0);

    @Test
    public void testPowerControllerBasedModel() {
        BatteryStatsImpl batteryStats = mStatsRule.getBatteryStats();

        batteryStats.noteNetworkInterfaceForTransports("wifi",
                new int[]{NetworkCapabilities.TRANSPORT_WIFI});

        NetworkStats networkStats = new NetworkStats(10000, 1)
                .insertEntry("wifi", APP_UID, 0, 0, 1000, 100, 2000, 20, 100)
                .insertEntry("wifi", Process.WIFI_UID, 0, 0, 1111, 111, 2222, 22, 111);
        mStatsRule.setNetworkStats(networkStats);

        WifiActivityEnergyInfo energyInfo = new WifiActivityEnergyInfo(10000,
                WifiActivityEnergyInfo.STACK_STATE_STATE_ACTIVE, 1000, 2000, 3000, 4000);

        batteryStats.updateWifiState(energyInfo, 1000, 1000);

        WifiPowerCalculator calculator = new WifiPowerCalculator(mStatsRule.getPowerProfile());
        mStatsRule.apply(calculator);

        UidBatteryConsumer uidConsumer = mStatsRule.getUidBatteryConsumer(APP_UID);
        assertThat(uidConsumer.getUsageDurationMillis(BatteryConsumer.TIME_COMPONENT_WIFI))
                .isEqualTo(1423);
        assertThat(uidConsumer.getConsumedPower(BatteryConsumer.POWER_COMPONENT_WIFI))
                .isWithin(PRECISION).of(0.2214666);

        SystemBatteryConsumer systemConsumer =
                mStatsRule.getSystemBatteryConsumer(SystemBatteryConsumer.DRAIN_TYPE_WIFI);
        assertThat(systemConsumer.getUsageDurationMillis(BatteryConsumer.TIME_COMPONENT_WIFI))
                .isEqualTo(5577);
        assertThat(systemConsumer.getConsumedPower(BatteryConsumer.POWER_COMPONENT_WIFI))
                .isWithin(PRECISION).of(0.645200);
    }

    @Test
    public void testTimerBasedModel() {
        BatteryStatsImpl batteryStats = mStatsRule.getBatteryStats();

        batteryStats.noteNetworkInterfaceForTransports("wifi",
                new int[]{NetworkCapabilities.TRANSPORT_WIFI});

        NetworkStats networkStats = new NetworkStats(10000, 1)
                .insertEntry("wifi", APP_UID, 0, 0, 1000, 100, 2000, 20, 100)
                .insertEntry("wifi", Process.WIFI_UID, 0, 0, 1111, 111, 2222, 22, 111);
        mStatsRule.setNetworkStats(networkStats);

        batteryStats.noteWifiScanStartedLocked(APP_UID, 1000, 1000);
        batteryStats.noteWifiScanStoppedLocked(APP_UID, 2000, 2000);
        batteryStats.noteWifiRunningLocked(new WorkSource(APP_UID), 3000, 3000);
        batteryStats.noteWifiStoppedLocked(new WorkSource(APP_UID), 4000, 4000);
        batteryStats.noteWifiRunningLocked(new WorkSource(Process.WIFI_UID), 1111, 2222);
        batteryStats.noteWifiStoppedLocked(new WorkSource(Process.WIFI_UID), 3333, 4444);

        // Don't pass WifiActivityEnergyInfo, making WifiPowerCalculator rely exclusively
        // on the packet counts.
        batteryStats.updateWifiState(/* energyInfo */ null, 1000, 1000);

        WifiPowerCalculator calculator = new WifiPowerCalculator(mStatsRule.getPowerProfile());
        mStatsRule.apply(calculator);

        UidBatteryConsumer uidConsumer = mStatsRule.getUidBatteryConsumer(APP_UID);
        assertThat(uidConsumer.getUsageDurationMillis(BatteryConsumer.TIME_COMPONENT_WIFI))
                .isEqualTo(1000);
        assertThat(uidConsumer.getConsumedPower(BatteryConsumer.POWER_COMPONENT_WIFI))
                .isWithin(PRECISION).of(0.8231573);

        SystemBatteryConsumer systemConsumer =
                mStatsRule.getSystemBatteryConsumer(SystemBatteryConsumer.DRAIN_TYPE_WIFI);
        assertThat(systemConsumer.getUsageDurationMillis(BatteryConsumer.TIME_COMPONENT_WIFI))
                .isEqualTo(2222);
        assertThat(systemConsumer.getConsumedPower(BatteryConsumer.POWER_COMPONENT_WIFI))
                .isWithin(PRECISION).of(0.8759216);
    }
}
