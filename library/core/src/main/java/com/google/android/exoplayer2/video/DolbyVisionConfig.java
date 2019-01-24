/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.google.android.exoplayer2.video;
import com.google.android.exoplayer2.Format;

import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.util.ParsableByteArray;

/**
 * Dolby Vision configuration data.
 */

public final class DolbyVisionConfig {
  public final int profile;
  public final int level;
  public boolean rpuPresent;
  public boolean blpresent;
  public boolean elpresent;

  /**
   * Parses Dolby Vision configuration data.
   *
   * @param data A {@link ParsableByteArray}, whose position is set to the start of the DoVi
   *     configuration data to parse.
   * @return A parsed representation of the DoVi configuration data.
   * @throws ParserException If an error occurred parsing the data.
   */
  public static DolbyVisionConfig parse(ParsableByteArray data) throws ParserException {
    int dv_major_version = data.readUnsignedByte();
    int dv_minor_version = data.readUnsignedByte();

    int profileData = data.readUnsignedByte();

    int profile = (profileData >> 1);
    int level = ((profileData & 0x1) << 5) | ((data.readUnsignedByte() >> 3) & 0x1F);

    if ((profile == 0) || (profile == 2)) {
      /* profile 0, 2 are backward compatible, hence neither the mime nor
         profile-level key-value pairs in MediaFormat should be modified
      */
      return new DolbyVisionConfig(Format.NO_VALUE, Format.NO_VALUE);
    }

    int dv_profile[] = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512};
    int dv_level[] = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256};

    return new DolbyVisionConfig(dv_profile[profile], dv_level[level]);
  }
  private DolbyVisionConfig(int profile, int level) {

    this.profile = profile;
    this.level = level;
    this.blpresent = false; // currently not used
    this.elpresent = false; // currently not used
    this.rpuPresent = false; // currently not used
  }

}
