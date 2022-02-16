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

package android.os.vibrator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import static org.testng.Assert.assertThrows;

import android.os.Parcel;
import android.os.VibrationEffect;
import android.platform.test.annotations.Presubmit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@Presubmit
@RunWith(MockitoJUnitRunner.class)
public class PrimitiveSegmentTest {
    private static final float TOLERANCE = 1e-2f;

    @Test
    public void testCreation() {
        PrimitiveSegment primitive = new PrimitiveSegment(
                VibrationEffect.Composition.PRIMITIVE_CLICK, 1, 10);

        assertEquals(-1, primitive.getDuration());
        assertTrue(primitive.hasNonZeroAmplitude());
        assertEquals(VibrationEffect.Composition.PRIMITIVE_CLICK, primitive.getPrimitiveId());
        assertEquals(10, primitive.getDelay());
        assertEquals(1f, primitive.getScale(), TOLERANCE);
    }

    @Test
    public void testSerialization() {
        PrimitiveSegment original = new PrimitiveSegment(
                VibrationEffect.Composition.PRIMITIVE_CLICK, 1, 10);
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        assertEquals(original, PrimitiveSegment.CREATOR.createFromParcel(parcel));
    }

    @Test
    public void testValidate() {
        new PrimitiveSegment(VibrationEffect.Composition.PRIMITIVE_CLICK, 1, 0).validate();

        assertThrows(IllegalArgumentException.class,
                () -> new PrimitiveSegment(1000, 0, 10).validate());
        assertThrows(IllegalArgumentException.class,
                () -> new PrimitiveSegment(VibrationEffect.Composition.PRIMITIVE_NOOP, -1, 0)
                        .validate());
        assertThrows(IllegalArgumentException.class,
                () -> new PrimitiveSegment(VibrationEffect.Composition.PRIMITIVE_NOOP, 1, -1)
                        .validate());
    }

    @Test
    public void testResolve_ignoresAndReturnsSameEffect() {
        PrimitiveSegment primitive = new PrimitiveSegment(
                VibrationEffect.Composition.PRIMITIVE_CLICK, 1, 0);
        assertSame(primitive, primitive.resolve(1000));
    }

    @Test
    public void testApplyEffectStrength_ignoresAndReturnsSameEffect() {
        PrimitiveSegment primitive = new PrimitiveSegment(
                VibrationEffect.Composition.PRIMITIVE_CLICK, 1, 0);
        assertSame(primitive,
                primitive.applyEffectStrength(VibrationEffect.EFFECT_STRENGTH_STRONG));
    }

    @Test
    public void testScale_fullPrimitiveScaleValue() {
        PrimitiveSegment initial = new PrimitiveSegment(
                VibrationEffect.Composition.PRIMITIVE_CLICK, 1, 0);

        assertEquals(1f, initial.scale(1).getScale(), TOLERANCE);
        assertEquals(0.34f, initial.scale(0.5f).getScale(), TOLERANCE);
        // The original value was not scaled up, so this only scales it down.
        assertEquals(1f, initial.scale(1.5f).getScale(), TOLERANCE);
        assertEquals(0.53f, initial.scale(1.5f).scale(2 / 3f).getScale(), TOLERANCE);
        // Does not restore to the exact original value because scale up is a bit offset.
        assertEquals(0.71f, initial.scale(0.8f).getScale(), TOLERANCE);
        assertEquals(0.84f, initial.scale(0.8f).scale(1.25f).getScale(), TOLERANCE);
    }

    @Test
    public void testScale_halfPrimitiveScaleValue() {
        PrimitiveSegment initial = new PrimitiveSegment(
                VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f, 0);

        assertEquals(0.5f, initial.scale(1).getScale(), TOLERANCE);
        assertEquals(0.17f, initial.scale(0.5f).getScale(), TOLERANCE);
        // The original value was not scaled up, so this only scales it down.
        assertEquals(0.86f, initial.scale(1.5f).getScale(), TOLERANCE);
        assertEquals(0.47f, initial.scale(1.5f).scale(2 / 3f).getScale(), TOLERANCE);
        // Does not restore to the exact original value because scale up is a bit offset.
        assertEquals(0.35f, initial.scale(0.8f).getScale(), TOLERANCE);
        assertEquals(0.5f, initial.scale(0.8f).scale(1.25f).getScale(), TOLERANCE);
    }

    @Test
    public void testScale_zeroPrimitiveScaleValue() {
        PrimitiveSegment initial = new PrimitiveSegment(
                VibrationEffect.Composition.PRIMITIVE_CLICK, 0, 0);

        assertEquals(0f, initial.scale(1).getScale(), TOLERANCE);
        assertEquals(0f, initial.scale(0.5f).getScale(), TOLERANCE);
        assertEquals(0f, initial.scale(1.5f).getScale(), TOLERANCE);
        assertEquals(0f, initial.scale(1.5f).scale(2 / 3f).getScale(), TOLERANCE);
        assertEquals(0f, initial.scale(0.8f).scale(1.25f).getScale(), TOLERANCE);
    }
}
