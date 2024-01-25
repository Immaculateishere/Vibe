/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.google.android.exoplayer2.text;

import androidx.annotation.Nullable;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.text.cea.Cea608Decoder;
import com.google.android.exoplayer2.text.cea.Cea708Decoder;
import com.google.android.exoplayer2.util.MimeTypes;
import java.util.Objects;

/**
 * A factory for {@link SubtitleDecoder} instances.
 *
 * @deprecated com.google.android.exoplayer2 is deprecated. Please migrate to androidx.media3 (which
 *     contains the same ExoPlayer code). See <a
 *     href="https://developer.android.com/guide/topics/media/media3/getting-started/migration-guide">the
 *     migration guide</a> for more details, including a script to help with the migration.
 */
@Deprecated
public interface SubtitleDecoderFactory {

  /**
   * Returns whether the factory is able to instantiate a {@link SubtitleDecoder} for the given
   * {@link Format}.
   *
   * @param format The {@link Format}.
   * @return Whether the factory can instantiate a suitable {@link SubtitleDecoder}.
   */
  boolean supportsFormat(Format format);

  /**
   * Creates a {@link SubtitleDecoder} for the given {@link Format}.
   *
   * @param format The {@link Format}.
   * @return A new {@link SubtitleDecoder}.
   * @throws IllegalArgumentException If the {@link Format} is not supported.
   */
  SubtitleDecoder createDecoder(Format format);

  /**
   * Default {@link SubtitleDecoderFactory} implementation.
   *
   * <p>Supports formats supported by {@link DefaultSubtitleParserFactory} as well as the following:
   *
   * <ul>
   *   <li>Cea608 ({@link Cea608Decoder})
   *   <li>Cea708 ({@link Cea708Decoder})
   * </ul>
   */
  SubtitleDecoderFactory DEFAULT =
      new SubtitleDecoderFactory() {

        private final DefaultSubtitleParserFactory delegate = new DefaultSubtitleParserFactory();

        @Override
        public boolean supportsFormat(Format format) {
          @Nullable String mimeType = format.sampleMimeType;
          return delegate.supportsFormat(format)
              || Objects.equals(mimeType, MimeTypes.APPLICATION_CEA608)
              || Objects.equals(mimeType, MimeTypes.APPLICATION_MP4CEA608)
              || Objects.equals(mimeType, MimeTypes.APPLICATION_CEA708);
        }

        @Override
        public SubtitleDecoder createDecoder(Format format) {
          @Nullable String mimeType = format.sampleMimeType;
          if (mimeType != null) {
            switch (mimeType) {
              case MimeTypes.APPLICATION_CEA608:
              case MimeTypes.APPLICATION_MP4CEA608:
                return new Cea608Decoder(
                    mimeType,
                    format.accessibilityChannel,
                    Cea608Decoder.MIN_DATA_CHANNEL_TIMEOUT_MS);
              case MimeTypes.APPLICATION_CEA708:
                return new Cea708Decoder(format.accessibilityChannel, format.initializationData);
              default:
                break;
            }
          }
          if (delegate.supportsFormat(format)) {
            SubtitleParser subtitleParser = delegate.create(format);
            return new DelegatingSubtitleDecoder(
                subtitleParser.getClass().getSimpleName() + "Decoder", subtitleParser);
          }
          throw new IllegalArgumentException(
              "Attempted to create decoder for unsupported MIME type: " + mimeType);
        }
      };
}
