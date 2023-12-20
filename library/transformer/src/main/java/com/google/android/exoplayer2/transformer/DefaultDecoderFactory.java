/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.android.exoplayer2.transformer;

import static com.google.android.exoplayer2.util.Assertions.checkNotNull;
import static com.google.android.exoplayer2.util.MediaFormatUtil.createMediaFormatFromFormat;
import static com.google.android.exoplayer2.util.Util.SDK_INT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Pair;
import android.view.Surface;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.MediaFormatUtil;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.ColorInfo;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link Codec.DecoderFactory} that uses {@link MediaCodec} for decoding.
 *
 * @deprecated com.google.android.exoplayer2 is deprecated. Please migrate to androidx.media3 (which
 *     contains the same ExoPlayer code). See <a
 *     href="https://developer.android.com/guide/topics/media/media3/getting-started/migration-guide">the
 *     migration guide</a> for more details, including a script to help with the migration.
 */
@Deprecated
public class DefaultDecoderFactory implements Codec.DecoderFactory {

  private static final String TAG = "DefaultDecoderFactory";

  protected final Context context;

  private final boolean decoderSupportsKeyAllowFrameDrop;
  @Nullable private final CodecFallbackListener fallbackListener;

  /** A custom listener of codec initialization failures. */
  public static interface CodecFallbackListener {
    /**
     * Reports that we were able to initialize the codec, however we had to apply a fallback due to
     * {@code initializationExceptions}.
     */
    public void onCodecFallback(
        String fallbackCodecName, List<ExportException> initializationExceptions);
  }

  /** Creates a new factory. */
  public DefaultDecoderFactory(Context context) {
    this(context, /* fallbackListener= */ null);
  }

  /** Creates a new factory that supports falling back to a different codec when needed. */
  public DefaultDecoderFactory(Context context, @Nullable CodecFallbackListener fallbackListener) {
    this.context = context;
    this.fallbackListener = fallbackListener;

    decoderSupportsKeyAllowFrameDrop =
        SDK_INT >= 29
            && context.getApplicationContext().getApplicationInfo().targetSdkVersion >= 29;
  }

  @Override
  public DefaultCodec createForAudioDecoding(Format format) throws ExportException {
    MediaFormat mediaFormat = createMediaFormatFromFormat(format);
    return createCodecForMediaFormatAndReportOnFallback(
        mediaFormat, format, /* outputSurface= */ null);
  }

  @SuppressLint("InlinedApi")
  @Override
  public DefaultCodec createForVideoDecoding(
      Format format, Surface outputSurface, boolean requestSdrToneMapping) throws ExportException {
    if (ColorInfo.isTransferHdr(format.colorInfo)) {
      if (requestSdrToneMapping
          && (SDK_INT < 31
              || deviceNeedsDisableToneMappingWorkaround(
                  checkNotNull(format.colorInfo).colorTransfer))) {
        throw createExportException(
            format, /* reason= */ "Tone-mapping HDR is not supported on this device.");
      }
      if (SDK_INT < 29) {
        // TODO(b/266837571, b/267171669): Remove API version restriction after fixing linked bugs.
        throw createExportException(
            format, /* reason= */ "Decoding HDR is not supported on this device.");
      }
    }
    if (deviceNeedsDisable8kWorkaround(format)) {
      throw createExportException(
          format, /* reason= */ "Decoding 8k is not supported on this device.");
    }
    if (deviceNeedsNoFrameRateWorkaround()) {
      format = format.buildUpon().setFrameRate(Format.NO_VALUE).build();
    }

    MediaFormat mediaFormat = createMediaFormatFromFormat(format);
    if (decoderSupportsKeyAllowFrameDrop) {
      // This key ensures no frame dropping when the decoder's output surface is full. This allows
      // transformer to decode as many frames as possible in one render cycle.
      mediaFormat.setInteger(MediaFormat.KEY_ALLOW_FRAME_DROP, 0);
    }
    if (SDK_INT >= 31 && requestSdrToneMapping) {
      mediaFormat.setInteger(
          MediaFormat.KEY_COLOR_TRANSFER_REQUEST, MediaFormat.COLOR_TRANSFER_SDR_VIDEO);
    }

    @Nullable
    Pair<Integer, Integer> codecProfileAndLevel = MediaCodecUtil.getCodecProfileAndLevel(format);
    if (codecProfileAndLevel != null) {
      MediaFormatUtil.maybeSetInteger(
          mediaFormat, MediaFormat.KEY_PROFILE, codecProfileAndLevel.first);
      MediaFormatUtil.maybeSetInteger(
          mediaFormat, MediaFormat.KEY_LEVEL, codecProfileAndLevel.second);
    }

    return createCodecForMediaFormatAndReportOnFallback(mediaFormat, format, outputSurface);
  }

  private DefaultCodec createCodecForMediaFormatAndReportOnFallback(
      MediaFormat mediaFormat, Format format, @Nullable Surface outputSurface)
      throws ExportException {
    List<MediaCodecInfo> decoderInfos = ImmutableList.of();
    checkNotNull(format.sampleMimeType);
    try {
      decoderInfos =
          MediaCodecUtil.getDecoderInfosSortedByFormatSupport(
              MediaCodecUtil.getDecoderInfosSoftMatch(
                  MediaCodecSelector.DEFAULT,
                  format,
                  /* requiresSecureDecoder= */ false,
                  /* requiresTunnelingDecoder= */ false),
              format);
    } catch (MediaCodecUtil.DecoderQueryException e) {
      Log.e(TAG, "Error querying decoders", e);
      throw createExportException(format, /* reason= */ "Querying codecs failed");
    }

    if (decoderInfos.isEmpty()) {
      throw createExportException(format, /* reason= */ "No decoders for format");
    }

    List<ExportException> codecInitExceptions = new ArrayList<>();
    DefaultCodec codec =
        createCodecFromDecoderInfos(
            context,
            fallbackListener == null ? decoderInfos.subList(0, 1) : decoderInfos,
            format,
            mediaFormat,
            outputSurface,
            codecInitExceptions);

    if (fallbackListener != null && !codecInitExceptions.isEmpty()) {
      fallbackListener.onCodecFallback(codec.getName(), codecInitExceptions);
    }
    return codec;
  }

  private static DefaultCodec createCodecFromDecoderInfos(
      Context context,
      List<MediaCodecInfo> decoderInfos,
      Format format,
      MediaFormat mediaFormat,
      @Nullable Surface outputSurface,
      List<ExportException> codecInitExceptions)
      throws ExportException {
    for (MediaCodecInfo decoderInfo : decoderInfos) {
      String codecMimeType = decoderInfo.codecMimeType;
      // Does not alter format.sampleMimeType to keep the original MimeType.
      // The MIME type of the selected decoder may differ from Format.sampleMimeType, for example,
      // video/hevc is used instead of video/dolby-vision for some specific DolbyVision videos.
      mediaFormat.setString(MediaFormat.KEY_MIME, codecMimeType);
      try {
        return new DefaultCodec(
            context, format, mediaFormat, decoderInfo.name, /* isDecoder= */ true, outputSurface);
      } catch (ExportException e) {
        codecInitExceptions.add(e);
      }
    }

    // All codecs failed to be initialized, throw the first codec init error out.
    throw codecInitExceptions.get(0);
  }

  private static boolean deviceNeedsDisable8kWorkaround(Format format) {
    // Fixed on API 31+. See http://b/278234847#comment40 for more information.
    return SDK_INT < 31
        && format.width >= 7680
        && format.height >= 4320
        && format.sampleMimeType != null
        && format.sampleMimeType.equals(MimeTypes.VIDEO_H265)
        && (Util.MODEL.equals("SM-F711U1") || Util.MODEL.equals("SM-F926U1"));
  }

  private static boolean deviceNeedsDisableToneMappingWorkaround(
      @C.ColorTransfer int colorTransfer) {
    if (Util.MANUFACTURER.equals("Google") && Build.ID.startsWith("TP1A")) {
      // Some Pixel 6 builds report support for tone mapping but the feature doesn't work
      // (see b/249297370#comment8).
      return true;
    }
    if (colorTransfer == C.COLOR_TRANSFER_HLG
        && (Util.MODEL.startsWith("SM-F936")
            || Util.MODEL.startsWith("SM-F916")
            || Util.MODEL.startsWith("SM-F721")
            || Util.MODEL.equals("SM-X900"))) {
      // Some Samsung Galaxy Z Fold devices report support for HLG tone mapping but the feature only
      // works on PQ (see b/282791751#comment7).
      return true;
    }
    if (SDK_INT < 34
        && colorTransfer == C.COLOR_TRANSFER_ST2084
        && Util.MODEL.startsWith("SM-F936")) {
      // The Samsung Fold 4 HDR10 codec plugin for tonemapping sets incorrect crop values, so block
      // using it (see b/290725189).
      return true;
    }
    return false;
  }

  private static boolean deviceNeedsNoFrameRateWorkaround() {
    // Redmi Note 9 Pro fails if KEY_FRAME_RATE is set too high (see b/278076311).
    return SDK_INT < 30 && Util.DEVICE.equals("joyeuse");
  }

  private static ExportException createExportException(Format format, String reason) {
    return ExportException.createForCodec(
        new IllegalArgumentException(reason),
        ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        MimeTypes.isVideo(checkNotNull(format.sampleMimeType)),
        /* isDecoder= */ true,
        format);
  }
}
