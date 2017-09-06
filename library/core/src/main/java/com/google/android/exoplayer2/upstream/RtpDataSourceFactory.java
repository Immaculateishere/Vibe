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
package com.google.android.exoplayer2.upstream;

import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.util.rtp.RtpDistributionFeedback;

/**
 * A {@link Factory} that produces {@link RtpDataSourceFactory} for RTP data sources.
 */
public final class RtpDataSourceFactory implements Factory {

    private final TransferListener<? super DataSource> listener;
    private final RtpDistributionFeedback.RtpFeedbackProperties feedbackProperties;
    public RtpDataSourceFactory() {
        this(null);
    }

    /**
     * @param listener An optional listener.
     */
    public RtpDataSourceFactory(TransferListener<? super DataSource> listener) {
        this.listener = listener;
        this.feedbackProperties = new RtpDistributionFeedback.RtpFeedbackProperties();
    }

    public final RtpDistributionFeedback.RtpFeedbackProperties getFeedbackProperties() {
        return feedbackProperties;
    }

    public final void setFeedbackProperty(Integer id, Object value) {
        feedbackProperties.set(id, value);
    }

    public final void clearFeedbackProperty(Integer id) {
        feedbackProperties.remove(id);
    }

    @Override
    public DataSource createDataSource() {
        return new RtpDataSource(listener, feedbackProperties);
    }

}
