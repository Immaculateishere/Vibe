/*
 * Copyright 2021 The Android Open Source Project
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
package com.google.android.exoplayer2;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.util.Clock;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Thrown when a non locally recoverable playback failure occurs. */
public class PlaybackException extends Exception implements Bundleable {

  /**
   * Integer codes that identify causes of player errors. This list of errors may be extended in
   * future versions, and {@link Player} implementations may define custom error codes.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      open = true,
      value = {
        ERROR_CODE_UNSPECIFIED,
        ERROR_CODE_REMOTE_ERROR,
        ERROR_CODE_BEHIND_LIVE_WINDOW,
        ERROR_CODE_IO_UNSPECIFIED,
        ERROR_CODE_IO_NETWORK_UNAVAILABLE,
        ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        ERROR_CODE_IO_NETWORK_CONNECTION_CLOSED,
        ERROR_CODE_IO_BAD_HTTP_STATUS,
        ERROR_CODE_IO_DNS_FAILED,
        ERROR_CODE_IO_FILE_NOT_FOUND,
        ERROR_CODE_IO_NO_PERMISSION,
        ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        ERROR_CODE_DECODER_INIT_FAILED,
        ERROR_CODE_DECODING_FAILED,
        ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
        ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
        ERROR_CODE_DRM_PROVISIONING_FAILED,
        ERROR_CODE_DRM_CONTENT_ERROR,
        ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
        ERROR_CODE_DRM_DISALLOWED_OPERATION,
        ERROR_CODE_DRM_SYSTEM_ERROR,
        ERROR_CODE_DRM_DEVICE_REVOKED
      })
  public @interface ErrorCode {}

  // Miscellaneous errors (1xxx).

  /** Caused by an error whose cause could not be identified. */
  public static final int ERROR_CODE_UNSPECIFIED = 1000;
  /**
   * Caused by an unidentified error in a remote Player, which is a Player that runs on a different
   * host or process.
   */
  public static final int ERROR_CODE_REMOTE_ERROR = 1001;
  /** Caused by the loading position falling behind the sliding window of available live content. */
  public static final int ERROR_CODE_BEHIND_LIVE_WINDOW = 1002;

  // Input/Output errors (2xxx).

  /** Caused by an Input/Output error which could not be identified. */
  public static final int ERROR_CODE_IO_UNSPECIFIED = 2000;
  /** Caused by the lack of network connectivity while trying to access a network resource. */
  public static final int ERROR_CODE_IO_NETWORK_UNAVAILABLE = 2001;
  /** Caused by a failure while establishing a network connection. */
  public static final int ERROR_CODE_IO_NETWORK_CONNECTION_FAILED = 2002;
  /** Caused by a network timeout, meaning the server is taking too long to fulfill a request. */
  public static final int ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT = 2003;
  /** Caused by an existing connection being unexpectedly closed. */
  public static final int ERROR_CODE_IO_NETWORK_CONNECTION_CLOSED = 2004;
  /** Caused by an HTTP server returning an unexpected HTTP response status code. */
  public static final int ERROR_CODE_IO_BAD_HTTP_STATUS = 2005;
  /** Caused by the player failing to resolve a hostname. */
  public static final int ERROR_CODE_IO_DNS_FAILED = 2006;
  /** Caused by a non-existent file. */
  public static final int ERROR_CODE_IO_FILE_NOT_FOUND = 2007;
  /**
   * Caused by lack of permission to perform an IO operation. For example, lack of permission to
   * access internet or external storage.
   */
  public static final int ERROR_CODE_IO_NO_PERMISSION = 2008;

  // Content parsing errors (3xxx).

  /**
   * Caused by a parsing error associated to a media manifest. Examples of a media manifest are a
   * DASH or a SmoothStreaming manifest, or an HLS playlist.
   */
  public static final int ERROR_CODE_PARSING_MANIFEST_MALFORMED = 3000;
  /** Caused by a parsing error associated to a media container format bitstream. */
  public static final int ERROR_CODE_PARSING_CONTAINER_MALFORMED = 3001;
  /**
   * Caused by attempting to extract a file with an unsupported media container format, or an
   * unsupported media container feature.
   */
  public static final int ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED = 3002;
  /**
   * Caused by an unsupported feature in a media manifest. Examples of a media manifest are a DASH
   * or a SmoothStreaming manifest, or an HLS playlist.
   */
  public static final int ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED = 3003;

  // Decoding errors (4xxx).

  /** Caused by a decoder initialization failure. */
  public static final int ERROR_CODE_DECODER_INIT_FAILED = 4000;
  /** Caused by a failure while trying to decode media samples. */
  public static final int ERROR_CODE_DECODING_FAILED = 4001;
  /** Caused by trying to decode content whose format exceeds the capabilities of the device. */
  public static final int ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES = 4002;
  /** Caused by trying to decode content whose format is not supported. */
  public static final int ERROR_CODE_DECODING_FORMAT_UNSUPPORTED = 4003;

  // AudioTrack errors (5xxx).

  /** Caused by an AudioTrack initialization failure. */
  public static final int ERROR_CODE_AUDIO_TRACK_INIT_FAILED = 5000;
  /** Caused by an AudioTrack write operation failure. */
  public static final int ERROR_CODE_AUDIO_TRACK_WRITE_FAILED = 5001;

  // DRM errors (6xxx).

  /**
   * Caused by a chosen DRM protection scheme not being supported by the device. Examples of DRM
   * protection schemes are ClearKey and Widevine.
   */
  public static final int ERROR_CODE_DRM_SCHEME_UNSUPPORTED = 6000;
  /** Caused by a failure while provisioning the device. */
  public static final int ERROR_CODE_DRM_PROVISIONING_FAILED = 6001;
  /** Caused by attempting to play incompatible DRM-protected content. */
  public static final int ERROR_CODE_DRM_CONTENT_ERROR = 6002;
  /** Caused by a failure while trying to obtain a license. */
  public static final int ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED = 6003;
  /** Caused by an operation being disallowed by a license policy. */
  public static final int ERROR_CODE_DRM_DISALLOWED_OPERATION = 6004;
  /** Caused by an error in the DRM system. */
  public static final int ERROR_CODE_DRM_SYSTEM_ERROR = 6005;
  /** Caused by the device having revoked DRM privileges. */
  public static final int ERROR_CODE_DRM_DEVICE_REVOKED = 6006;

  /**
   * Player implementations that want to surface custom errors can use error codes greater than this
   * value, so as to avoid collision with other error codes defined in this class.
   */
  public static final int CUSTOM_ERROR_CODE_BASE = 1000000;

  /** An error code which identifies the source of the playback failure. */
  public final int errorCode;

  /** The value of {@link SystemClock#elapsedRealtime()} when this exception was created. */
  public final long timestampMs;

  /**
   * Creates an instance.
   *
   * @param errorCode A number which identifies the cause of the error. May be one of the {@link
   *     ErrorCode ErrorCodes}.
   * @param cause See {@link #getCause()}.
   * @param message See {@link #getMessage()}.
   */
  public PlaybackException(@Nullable String message, @Nullable Throwable cause, int errorCode) {
    this(message, cause, errorCode, Clock.DEFAULT.elapsedRealtime());
  }

  private PlaybackException(
      @Nullable String message,
      @Nullable Throwable cause,
      @ErrorCode int errorCode,
      long timestampMs) {
    super(message, cause);
    this.errorCode = errorCode;
    this.timestampMs = timestampMs;
  }

  // Bundleable implementation.

  private static final int FIELD_INT_ERROR_CODE = 0;
  private static final int FIELD_LONG_TIME_STAMP_MS = 1;
  private static final int FIELD_STRING_MESSAGE = 2;
  private static final int FIELD_STRING_CAUSE_CLASS_NAME = 3;
  private static final int FIELD_STRING_CAUSE_MESSAGE = 4;

  /** Object that can restore {@link PlaybackException} from a {@link Bundle}. */
  public static final Creator<PlaybackException> CREATOR = PlaybackException::fromBundle;

  /**
   * Defines a minimum field id value for subclasses to use when implementing {@link #toBundle()}
   * and {@link Bundleable.Creator}.
   *
   * <p>Subclasses should obtain their {@link Bundle Bundle's} field keys by applying a non-negative
   * offset on this constant and passing the result to {@link #keyForField(int)}.
   */
  protected static final int FIELD_CUSTOM_ID_BASE = 1000;

  @CallSuper
  @Override
  public Bundle toBundle() {
    Bundle bundle = new Bundle();
    bundle.putInt(keyForField(FIELD_INT_ERROR_CODE), errorCode);
    bundle.putLong(keyForField(FIELD_LONG_TIME_STAMP_MS), timestampMs);
    bundle.putString(keyForField(FIELD_STRING_MESSAGE), getMessage());
    @Nullable Throwable cause = getCause();
    if (cause != null) {
      bundle.putString(keyForField(FIELD_STRING_CAUSE_CLASS_NAME), cause.getClass().getName());
      bundle.putString(keyForField(FIELD_STRING_CAUSE_MESSAGE), cause.getMessage());
    }
    return bundle;
  }

  /**
   * Creates and returns a new instance using the field data in the given {@link Bundle}.
   *
   * @param bundle The {@link Bundle} from which to obtain the returned instance's contents.
   * @return The created instance.
   */
  protected static PlaybackException fromBundle(Bundle bundle) {
    @ErrorCode
    int errorCode =
        bundle.getInt(
            keyForField(FIELD_INT_ERROR_CODE), /* defaultValue= */ ERROR_CODE_UNSPECIFIED);
    long timestampMs =
        bundle.getLong(
            keyForField(FIELD_LONG_TIME_STAMP_MS),
            /* defaultValue= */ SystemClock.elapsedRealtime());
    @Nullable String message = bundle.getString(keyForField(FIELD_STRING_MESSAGE));
    @Nullable String causeClassName = bundle.getString(keyForField(FIELD_STRING_CAUSE_CLASS_NAME));
    @Nullable String causeMessage = bundle.getString(keyForField(FIELD_STRING_CAUSE_MESSAGE));
    @Nullable Throwable cause = null;
    if (!TextUtils.isEmpty(causeClassName)) {
      try {
        Class<?> clazz =
            Class.forName(
                causeClassName, /* initialize= */ true, PlaybackException.class.getClassLoader());
        if (Throwable.class.isAssignableFrom(clazz)) {
          cause = createThrowable(clazz, causeMessage);
        }
      } catch (Throwable e) {
        // There was an error while creating the cause using reflection, do nothing here and let the
        // finally block handle the issue.
      } finally {
        if (cause == null) {
          // The bundle has fields to represent the cause, but we were unable to re-create the
          // exception using reflection. We instantiate a RemoteException to reflect this problem.
          cause = createRemoteException(causeMessage);
        }
      }
    }
    return new PlaybackException(message, cause, errorCode, timestampMs);
  }

  /**
   * Converts the given {@code field} to a string which can be used as a field key when implementing
   * {@link #toBundle()} and {@link Bundleable.Creator}.
   */
  protected static String keyForField(int field) {
    return Integer.toString(field, Character.MAX_RADIX);
  }

  // Creates a new {@link Throwable} with possibly {@code null} message.
  @SuppressWarnings("nullness:argument.type.incompatible")
  private static Throwable createThrowable(Class<?> clazz, @Nullable String message)
      throws Exception {
    return (Throwable) clazz.getConstructor(String.class).newInstance(message);
  }

  // Creates a new {@link RemoteException} with possibly {@code null} message.
  @SuppressWarnings("nullness:argument.type.incompatible")
  private static RemoteException createRemoteException(@Nullable String message) {
    return new RemoteException(message);
  }
}
