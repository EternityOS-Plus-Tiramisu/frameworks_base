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

package com.android.settingslib.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.text.SpannedString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UsageProgressBarPreferenceTest {

    private UsageProgressBarPreference mUsageProgressBarPreference;
    private PreferenceViewHolder mViewHolder;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        mUsageProgressBarPreference = new UsageProgressBarPreference(context);
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View rootView = inflater.inflate(mUsageProgressBarPreference.getLayoutResource(),
                new LinearLayout(context), false /* attachToRoot */);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(rootView);
    }

    @Test
    public void setUsageSummary_noNumber_noRelativeSizeSpan() {
        mUsageProgressBarPreference.setUsageSummary("test");

        mUsageProgressBarPreference.onBindViewHolder(mViewHolder);

        final TextView usageSummary = (TextView) mViewHolder.findViewById(R.id.usage_summary);
        final SpannedString summary = new SpannedString(usageSummary.getText());
        assertThat(summary.getSpans(0, summary.length(), RelativeSizeSpan.class).length)
                .isEqualTo(0);
    }

    @Test
    public void setUsageSummary_integerNumber_findRelativeSizeSpan() {
        mUsageProgressBarPreference.setUsageSummary("10Test");

        mUsageProgressBarPreference.onBindViewHolder(mViewHolder);

        final TextView usageSummary = (TextView) mViewHolder.findViewById(R.id.usage_summary);
        final SpannedString summary = new SpannedString(usageSummary.getText());
        assertThat(summary.getSpans(0, summary.length(), RelativeSizeSpan.class).length)
                .isEqualTo(1);
    }

    @Test
    public void setUsageSummary_floatNumber_findRelativeSizeSpan() {
        mUsageProgressBarPreference.setUsageSummary("3.14Test");

        mUsageProgressBarPreference.onBindViewHolder(mViewHolder);

        final TextView usageSummary = (TextView) mViewHolder.findViewById(R.id.usage_summary);
        final SpannedString summary = new SpannedString(usageSummary.getText());
        assertThat(summary.getSpans(0, summary.length(), RelativeSizeSpan.class).length)
                .isEqualTo(1);
    }

    @Test
    public void setPercent_getCorrectProgress() {
        mUsageProgressBarPreference.setPercent(31, 80);

        mUsageProgressBarPreference.onBindViewHolder(mViewHolder);

        final ProgressBar progressBar = (ProgressBar) mViewHolder
                .findViewById(android.R.id.progress);
        assertThat(progressBar.getProgress()).isEqualTo((int) (31.0f / 80 * 100));
    }

    @Test
    public void setCustomContent_setNullImageView_noChild() {
        mUsageProgressBarPreference.setCustomContent(null /* imageView */);

        mUsageProgressBarPreference.onBindViewHolder(mViewHolder);

        final FrameLayout customContent =
                (FrameLayout) mViewHolder.findViewById(R.id.custom_content);
        assertThat(customContent.getChildCount()).isEqualTo(0);
        assertThat(customContent.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void setCustomContent_setImageView_oneChild() {
        final ImageView imageView = mock(ImageView.class);
        mUsageProgressBarPreference.setCustomContent(imageView);

        mUsageProgressBarPreference.onBindViewHolder(mViewHolder);

        final FrameLayout customContent =
                (FrameLayout) mViewHolder.findViewById(R.id.custom_content);
        assertThat(customContent.getChildCount()).isEqualTo(1);
        assertThat(customContent.getChildAt(0)).isEqualTo(imageView);
        assertThat(customContent.getVisibility()).isEqualTo(View.VISIBLE);
    }
}
