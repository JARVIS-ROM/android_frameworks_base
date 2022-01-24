/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.biometrics.sensors.face.aidl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.biometrics.common.OperationContext;
import android.hardware.biometrics.face.ISession;
import android.os.IBinder;
import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;
import android.testing.TestableContext;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Presubmit
@SmallTest
public class FaceDetectClientTest {

    private static final int USER_ID = 12;
    private static final boolean HAS_AOD = true;

    @Rule
    public final TestableContext mContext = new TestableContext(
            InstrumentationRegistry.getInstrumentation().getTargetContext(), null);

    @Mock
    private ISession mHal;
    @Mock
    private IBinder mToken;
    @Mock
    private ClientMonitorCallbackConverter mClientMonitorCallbackConverter;
    @Mock
    private BiometricLogger mBiometricLogger;
    @Mock
    private BiometricContext mBiometricContext;
    @Mock
    private ClientMonitorCallback mCallback;
    @Mock
    private Sensor.HalSessionCallback mHalSessionCallback;
    @Captor
    private ArgumentCaptor<OperationContext> mOperationContextCaptor;

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Before
    public void setup() {
        when(mBiometricContext.isAoD()).thenReturn(HAS_AOD);
    }

    @Test
    public void detectNoContext_v1() throws RemoteException {
        final FaceDetectClient client = createClient(1);
        client.start(mCallback);

        verify(mHal).detectInteraction();
        verify(mHal, never()).detectInteractionWithContext(any());
    }

    @Test
    public void detectWithContext_v2() throws RemoteException {
        final FaceDetectClient client = createClient(2);
        client.start(mCallback);

        verify(mHal).detectInteractionWithContext(mOperationContextCaptor.capture());
        verify(mHal, never()).detectInteraction();

        final OperationContext opContext = mOperationContextCaptor.getValue();
        assertThat(opContext.isAoD).isEqualTo(HAS_AOD);
    }

    private FaceDetectClient createClient(int version) throws RemoteException {
        when(mHal.getInterfaceVersion()).thenReturn(version);

        final AidlSession aidl = new AidlSession(version, mHal, USER_ID, mHalSessionCallback);
        return new FaceDetectClient(mContext, () -> aidl, mToken,
                99 /* requestId */, mClientMonitorCallbackConverter, USER_ID,
                "own-it", 5 /* sensorId */, mBiometricLogger, mBiometricContext,
                false /* isStrongBiometric */, null /* sensorPrivacyManager */);
    }
}
