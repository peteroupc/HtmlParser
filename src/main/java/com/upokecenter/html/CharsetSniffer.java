package com.upokecenter.util;

import java.util.*;

import java.io.*;

using Com.Upokecenter.net;
using Com.Upokecenter.util;
import com.upokecenter.util.*;
import com.upokecenter.text.*;

/*
If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/

Licensed under the Expat License.

Copyright (C) 2013 Peter Occil

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

  final class CharsetSniffer {
    private static final int NoFeed = 0;

    private static final int RSSFeed = 1; // application/rss + xml
    private static final int AtomFeed = 2; // application/atom + xml
    private static final byte[] ValueRdfNamespace = new byte[] {
      0x68, 0x74,
      0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x77, 0x77, 0x77, 0x2e,
      0x77, 0x33, 0x2e, 0x6f, 0x72, 0x67, 0x2f, 0x31, 0x39, 0x39, 0x39,
      0x2f, 0x30, 0x32, 0x2f, 0x32, 0x32, 0x2d, 0x72, 0x64, 0x66, 0x2d,
      0x73, 0x79, 0x6e, 0x74, 0x61, 0x78, 0x2d, 0x6e, 0x73, 0x23,
     };

    private static final byte[] ValueRssNamespace = new byte[] {
      0x68, 0x74,
      0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x70, 0x75, 0x72, 0x6c,
      0x2e, 0x6f, 0x72, 0x67, 0x2f, 0x72, 0x73, 0x73, 0x2f, 0x31, 0x2e,
      0x30, 0x2f,
     };

    private static byte[][] valuePatternsHtml = new byte[][] {
      new byte[] {
        0x3c, 0x21, 0x44, 0x4f, 0x43, 0x54, 0x59, 0x50, 0x45, 0x20,
        0x48, 0x54, 0x4d, 0x4c,
       },
      new byte[] {
        (byte)255, (byte)255, (byte)0xdf, (byte)0xdf, (byte)0xdf,
        (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)255, (byte)0xdf,
        (byte)0xdf, (byte)0xdf, (byte)0xdf,
       },
      new byte[] { 0x3c, 0x48, 0x54, 0x4d, 0x4c }, new byte[] {
  (byte)255,
  (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf,
 },
      new byte[] { 0x3c, 0x48, 0x45, 0x41, 0x44 }, new byte[] {
  (byte)255,
  (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf,
 },
      new byte[] { 0x3c, 0x53, 0x43, 0x52, 0x49, 0x50, 0x54 }, new byte[] {
  (byte)255, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf,
  (byte)0xdf,
 },
      new byte[] { 0x3c, 0x49, 0x46, 0x52, 0x41, 0x4d, 0x45 }, new byte[] {
  (byte)255, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf,
  (byte)0xdf,
 },
      new byte[] { 0x3c, 0x48, 0x31 }, new byte[] {
  (byte)255, (byte)0xdf,
  (byte)255,
 },
      new byte[] { 0x3c, 0x44, 0x49, 0x56 }, new byte[] {
  (byte)255,
  (byte)0xdf, (byte)0xdf, (byte)0xdf,
 },
      new byte[] { 0x3c, 0x46, 0x4f, 0x4e, 0x54 }, new byte[] {
  (byte)255,
  (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf,
 },
      new byte[] { 0x3c, 0x54, 0x41, 0x42, 0x4c, 0x45 }, new byte[] {
  (byte)255, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf,
 },
      new byte[] { 0x3c, 0x41 }, new byte[] { (byte)255, (byte)0xdf },
      new byte[] { 0x3c, 0x53, 0x54, 0x59, 0x4c, 0x45 }, new byte[] {
  (byte)255, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf,
 },
      new byte[] { 0x3c, 0x54, 0x49, 0x54, 0x4c, 0x45 }, new byte[] {
  (byte)255, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf,
 },
      new byte[] { 0x3c, 0x42 }, new byte[] { (byte)255, (byte)0xdf },
      new byte[] { 0x3c, 0x42, 0x4f, 0x44, 0x59 }, new byte[] {
  (byte)255,
  (byte)0xdf, (byte)0xdf, (byte)0xdf, (byte)0xdf,
 },
      new byte[] { 0x3c, 0x42, 0x52 }, new byte[] {
  (byte)255, (byte)0xdf,
  (byte)0xdf,
 },
      new byte[] { 0x3c, 0x50 }, new byte[] { (byte)255, (byte)0xdf },
      new byte[] { 0x3c, 0x21, 0x2d, 0x2d }, new byte[] {
  (byte)255,
  (byte)255, (byte)255, (byte)255,
 },
    };

    private static byte[][] valuePatternsXml = new byte[][] {
      new byte[] { 0x3c, 0x3f, 0x78, 0x6d, 0x6c }, new byte[] {
  (byte)255,
  (byte)255, (byte)255, (byte)255, (byte)255,
 },
    };

    private static byte[][] valuePatternsPdf = new byte[][] {
      new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2d, }, new byte[] {
  (byte)255,
  (byte)255, (byte)255, (byte)255, (byte)255,
 }
    };
    byte[] vpps = new byte[] {
      0x25, 0x21, 0x50, 0x53, 0x2d, 0x41,
      0x64, 0x6f, 0x62, 0x65, 0x2d,
     };
    private static byte[][] valuePatternsPs = new byte[][] {
      vpps, null
    };

    static String ExtractCharsetFromMeta(String value) {
      if (value == null) {
        return value;
      }
      // We assume value is lower-case here
      int index = 0;
      int length = value.length();
      char c = (char)0;
      while (true) {
        index = value.indexOf("charset",0);
        if (index < 0) {
          return null;
        }
        index += 7;
        // skip whitespace
        while (index < length) {
          c = value.charAt(index);
          if (c != 0x09 && c != 0x0c && c != 0x0d && c != 0x0a && c != 0x20) {
            break;
          }
          ++index;
        }
        if (index >= length) {
          return null;
        }
        if (value.charAt(index) == '=') {
          ++index;
          break;
        }
      }
      // skip whitespace
      while (index < length) {
        c = value.charAt(index);
        if (c != 0x09 && c != 0x0c && c != 0x0d && c != 0x0a && c != 0x20) {
          break;
        }
        ++index;
      }
      if (index >= length) {
        return null;
      }
      c = value.charAt(index);
      if (c == '"' || c == '\'') {
        ++index;
        int nextIndex = index;
        while (nextIndex < length) {
          char c2 = value.charAt(nextIndex);
          if (c == c2) {
            return Encodings.ResolveAlias(
                value.substring(
                  index, (
                  index)+(nextIndex - index)));
          }
          ++nextIndex;
        }
        return null;
      } else {
        int nextIndex = index;
        while (nextIndex < length) {
          char c2 = value.charAt(nextIndex);
          if (
            c2 == 0x09 || c2 == 0x0c || c2 == 0x0d || c2 == 0x0a || c2 ==
            0x20 || c2 == 0x3b) {
            break;
          }
          ++nextIndex;
        }
        return
          Encodings.ResolveAlias (value.substring(index, (index)+(nextIndex - index)));
      }
    }

    private static int IndexOfBytes(
      byte[] array,
      int offset,
      int count,
      byte[] pattern) {
      int endIndex = Math.min (offset + count, array.length);
      endIndex -= pattern.length - 1;
      if (endIndex < 0 || endIndex < offset) {
        return -1;
      }
      boolean found = false;
      for (int i = offset; i < endIndex; ++i) {
        found = true;
        for (int j = 0; j < pattern.length; ++j) {
          if (pattern[j] != array[i + j]) {
            found = false;
            break;
          }
        }
        if (found) {
          return i;
        }
      }
      return -1;
    }

    private static boolean MatchesPattern(
      byte[] pattern,
      byte[] sequence,
      int seqIndex,
      int count) {
      count = Math.min (count, sequence.length - seqIndex);
      int len = pattern.length;
      if (len <= count) {
        for (int i = 0; i < len; i++, seqIndex++) {
          if (sequence[seqIndex] != pattern[i]) {
            return false;
          }
        }
        return true;
      }
      return false;
    }

    private static boolean MatchesPattern(
      byte[][] patterns,
      int index,
      byte[] sequence,
      int seqIndex,
      int count) {
      byte[] pattern = patterns.get(index);
      count = Math.min (count, sequence.length - seqIndex);
      byte[] mask = patterns.get(index + 1);
      int len = pattern.length;
      if (len <= count) {
        if (mask == null) {
          for (int i = 0; i < len; i++, seqIndex++) {
            if (sequence[seqIndex] != pattern[i]) {
              return false;
            }
          }
        } else {
          for (int i = 0; i < len; i++, seqIndex++) {
            if ((sequence[seqIndex] & mask[i]) != pattern[i]) {
              return false;
            }
          }
        }
        return true;
      }
      return false;
    }

    private static boolean MatchesPatternAndTagTerminator(
      byte[][] patterns,
      int index,
      byte[] sequence,
      int seqIndex,
      int count) {
      byte[] pattern = patterns.get(index);
      count = Math.min (count, sequence.length - seqIndex);
      byte[] mask = patterns.get(index + 1);
      int len = pattern.length;
      if (len + 1 <= count) {
        for (int i = 0; i < len; i++, seqIndex++) {
          if ((sequence[seqIndex] & mask[i]) != pattern[i]) {
            return false;
          }
        }
        return sequence[seqIndex] != 0x20 && sequence[seqIndex] != 0x3e;
      }
      return false;
    }

    /*
    public static String sniffContentType(
      PeterO.Support.InputStream input,
      IHttpHeaders headers) {
      String contentType = headers.getHeaderField("content-type");
      if (contentType != null && (contentType.equals("text/plain") ||
          contentType.equals("text/plain; charset=ISO-8859-1") ||
          contentType.equals("text/plain; charset=iso-8859-1") ||
          contentType.equals("text/plain; charset=UTF-8"))) {
        String url = headers.getUrl();
        if (url != null && url.length() >= 5 &&
          (url.charAt(0) == 'h' || url.charAt(0) == 'H') && (url.charAt(1) == 't' || url.charAt(0) == 'T'
) && (url.charAt(2) == 't' || url.charAt(0) == 'T') && (url.charAt(3) == 'p' || url.charAt(0) == 'P'
) && (url.charAt(4) == ':')) {
          return SniffTextOrBinary(input);
        }
      }
      return sniffContentType(input, contentType);
    }
    public static String sniffContentType(
      PeterO.Support.InputStream input,
      String mediaType) {
      // TODO: Use MediaType.Parse here
      if (mediaType != null) {
        String type = mediaType;
        if (type.equals("text/xml") ||
    type.equals("application/xml") ||
    type.endsWith("+xml")) {
          return mediaType;
        }
        if (type.equals("*" + "/*") ||
    type.equals("unknown/unknown") ||
    type.equals("application/unknown")) {
          return SniffUnknownContentType(input, true);
        }
        if (type.equals("text/Html")) {
          byte[] header = new byte[512];
          input.mark(514);
          int count = 0;
          try {
            count = input.Read(header, 0, 512);
          } finally {
            input.reset();
          }
          int feed = SniffFeed(header, 0, count);
          if (feed == 0) {
            return "text/Html";
          } else if (feed == 1) {
            return "application/rss+xml";
          } else if (feed == 2) {
            return "application/atom+xml";
          }
        }
        return mediaType;
      } else {
        return SniffUnknownContentType(input, true);
      }
    }
    */

    private static int SniffFeed(byte[] header, int offset, int count) {
      if (header == null || offset < 0 || count < 0 || offset + count >
        header.length) {
        throw new IllegalArgumentException();
      }
      int endPos = offset + count;
      int index = offset;
      if (index + 3 <= endPos && (header[index] & 0xff) == 0xef &&
        (header[index + 1] & 0xff) == 0xbb && (header[index + 2] & 0xff) ==
        0xbf) {
        index += 3;
      }
      while (index < endPos) {
        while (index < endPos) {
          if (header[index] == '<') {
            ++index;
            break;
          } else if (header[index] == 0x09 || header[index] == 0x0a ||
            header[index] == 0x0c || header[index] == 0x0d ||
            header[index] == 0x20) {
            ++index;
          } else {
            return NoFeed;
          }
        }
        while (index < endPos) {
          if (index + 3 <= endPos && (header[index] & 0xff) == 0x21 &&
            (header[index + 1] & 0xff) == 0x2d && (header[index + 2] & 0xff) ==
            0x2d) {
            // Skip comment
            int hyphenCount = 0;
            index += 3;
            while (index < endPos) {
              int c = header[index] & 0xff;
              if (c == '-') {
                hyphenCount = Math.min (2, hyphenCount + 1);
              } else if (c == '>' && hyphenCount >= 2) {
                ++index;
                break;
              } else {
                hyphenCount = 0;
              }
              ++index;
            }
            break;
          } else if (index + 1 <= endPos && (header[index] & 0xFF) == '!') {
            ++index;
            while (index < endPos) {
              if (header[index] == '>') {
                ++index;
                break;
              }
              ++index;
            }
            break;
          } else if (index + 1 <= endPos && (header[index] & 0xFF) == '?') {
            int charCount = 0;
            ++index;
            while (index < endPos) {
              int c = header[index] & 0xff;
              if (c == '?') {
                charCount = 1;
              } else if (c == '>' && charCount == 1) {
                ++index;
                break;
              } else {
                charCount = 0;
              }
              ++index;
            }
            break;
          } else if (index + 3 <= endPos && (header[index] & 0xFF) == 'r' &&
            (header[index + 1] & 0xFF) == 's' && (header[index + 2] & 0xFF)
== 's'
) {
            return RSSFeed;
          } else if (index + 4 <= endPos && (header[index] & 0xFF) == 'f' &&
            (header[index + 1] & 0xFF) == 'e' &&
            (header[index + 2] & 0xFF) == 'e' &&
            (header[index + 3] & 0xFF) == 'd') {
            return AtomFeed;
          } else if (index + 7 <= endPos && (header[index] & 0xFF) == 'r' &&
            (header[index + 1] & 0xFF) == 'd' && (header[index + 2] & 0xFF)
== 'f'
            &&
            (header[index + 3] & 0xFF) == ':' && (header[index + 4] & 0xFF)
== 'R'
            &&
            (header[index + 5] & 0xFF) == 'D' && (header[index + 6] & 0xFF)
== 'F'
) {
            index += 7;
            if (IndexOfBytes (header, index, endPos - index, ValueRdfNamespace)
              >= 0 &&
              IndexOfBytes (header, index, endPos - index, ValueRssNamespace)
              >= 0) {
              return RSSFeed;
            } else {
              return NoFeed;
            }
          } else {
            return NoFeed;
          }
        }
      }
      return NoFeed;
    }

    private static String SniffTextOrBinary(byte[] header, int count) {
      if (count >= 4 && header[0] == (byte)0xfe && header[1] == (byte)0xff) {
        return "text/plain";
      }
      if (count >= 4 && header[0] == (byte)0xff && header[1] == (byte)0xfe) {
        return "text/plain";
      }
      if (count >= 4 && header[0] == (byte)0xef && header[1] == (byte)0xbb &&
        header[2] == (byte)0xbf) {
        return "text/plain";
      }
      boolean binary = false;
      for (int i = 0; i < count; ++i) {
        int b = header[i] & 0xff;
        if (!(b >= 0x20 || b == 0x09 || b == 0x0a || b == 0x0c || b == 0x0d ||
            b == 0x1b)) {
          binary = true;
          break;
        }
      }
      return (!binary) ? "text/plain" : SniffUnknownContentType(
        header,
        count,
        false);
    }

    private static String SniffUnknownContentType(
      byte[] header,
      int count,
      boolean sniffScriptable) {
      if (sniffScriptable) {
        int index = 0;
        while (index < count) {
          if (header[index] != 0x09 && header[index] != 0x0a &&
            header[index] != 0x0c && header[index] != 0x0d &&
            header[index] != 0x20) {
            break;
          }
          ++index;
        }
        if (index < count && header[index] == 0x3c) {
          for (int i = 0; i < valuePatternsHtml.length; i += 2) {
            if (
              MatchesPatternAndTagTerminator(
                valuePatternsHtml,
                i,
                header,
                index,
                count)) {
              return "text/Html";
            }
          }
          for (int i = 0; i < valuePatternsXml.length; i += 2) {
            if (
              MatchesPattern(
                valuePatternsXml,
                i,
                header,
                index,
                count)) {
              return "text/xml";
            }
          }
        }
        for (int i = 0; i < valuePatternsPdf.length; i += 2) {
          if (
            MatchesPattern(
              valuePatternsPdf,
              i,
              header,
              0,
              count)) {
            return "text/xml";
          }
        }
      }
      if (MatchesPattern (valuePatternsPs, 0, header, 0, count)) {
        return "application/postscript";
      }
      if (count >= 4 && header[0] == (byte)0xfe && header[1] == (byte)0xff) {
        return "text/plain";
      }
      if (count >= 4 && header[0] == (byte)0xff && header[1] == (byte)0xfe) {
        return "text/plain";
      }
      if (count >= 4 && header[0] == (byte)0xef && header[1] == (byte)0xbb &&
        header[2] == (byte)0xbf) {
        return "text/plain";
      }
      // Image types
      if (MatchesPattern (new byte[] { 0, 0, 1, 0 }, header, 0, count)) {
        return "image/x-icon"; // icon
      }
      if (MatchesPattern (new byte[] { 0, 0, 2, 0 }, header, 0, count)) {
        return "image/x-icon"; // cursor
      }
      if (MatchesPattern (new byte[] { 0x42, 0x4d }, header, 0, count)) {
        return "image/bmp";
      }
      if (
        MatchesPattern(
          new byte[] { 0x47, 0x49, 0x46, 0x38, 0x37, 0x61 },
          header,
          0,
          count)) {
        return "image/gif";
      }
      if (
        MatchesPattern(
          new byte[] { 0x47, 0x49, 0x46, 0x38, 0x39, 0x61 },
          header,
          0,
          count)) {
        return "image/gif";
      }
      if (
        MatchesPattern(
          new byte[] { 0x52, 0x49, 0x46, 0x46 },
          header,
          0,
          count) && MatchesPattern(
          new byte[] { 0x57, 0x45, 0x42, 0x50, 0x56, 0x50 },
          header,
          8,
          count - 8)) {
        return "image/webp";
      }
      if (
        MatchesPattern(
      new byte[] {
        (byte)0x89, 0x50, 0x4e, 0x47, 0x0d,
        0x0a, 0x1a, 0x0a,
       },
      header,
      0,
      count)) {
        return "image/png";
      }
      if (
        MatchesPattern(
          new byte[] { (byte)0xff, (byte)0xd8, (byte)0xff },
          header,
          0,
          count)) {
        return "image/jpeg";
      }
      // Audio and video types
      if (
        MatchesPattern(
          new byte[] { 0x1a, 0x45, (byte)0xdf, (byte)0xa3 },
          header,
          0,
          count)) {
        return "video/webm";
      }
      if (
        MatchesPattern(
          new byte[] { 0x2e, 0x7e, (byte)0x6e, (byte)0x64 },
          header,
          0,
          count)) {
        return "audio/basic";
      }
      if (
        MatchesPattern(
      new byte[] {
        (byte)0x46, (byte)0x4f, (byte)0x52,
        (byte)0x4d,
       },
      header,
      0,
      count) && MatchesPattern(
      new byte[] {
        (byte)0x41, (byte)0x49, (byte)0x46,
        (byte)0x46,
       },
      header,
      8,
      count - 8)) {
        return "audio/aiff";
      }
      if (
        MatchesPattern(
          new byte[] { (byte)0x49, (byte)0x44, (byte)0x33 },
          header,
          0,
          count)) {
        return "audio/mpeg";
      }
      if (
        MatchesPattern(
      new byte[] {
        (byte)0x4f, (byte)0x67, (byte)0x67,
        (byte)0x53, 0,
       },
      header,
      0,
      count)) {
        return "application/ogg";
      }
      if (
        MatchesPattern(
      new byte[] {
        (byte)0x4d, (byte)0x54, (byte)0x68,
        (byte)0x64, 0, 0, 0, 6,
       },
      header,
      0,
      count)) {
        return "audio/midi";
      }
      if (
        MatchesPattern(
      new byte[] {
        (byte)0x52, (byte)0x49, (byte)0x46,
        (byte)0x46,
       },
      header,
      0,
      count)) {
        if (
          MatchesPattern(
        new byte[] {
          (byte)0x41, (byte)0x56, (byte)0x49,
          (byte)' ',
         }, header, 8, count - 8)) {
          return "video/avi";
        }
        if (
          MatchesPattern(
        new byte[] { (byte)0x57, (byte)0x41, (byte)0x56, (byte)0x45, },
        header,
        8,
        count - 8)) {
          return "audio/wave";
        }
      }
      if (count >= 12) {
        int boxSize = (header[0] & 0xff) << 24;
        boxSize |= (header[1] & 0xff) << 16;
        boxSize |= (header[2] & 0xff) << 8;
        boxSize |= header[3] & 0xff;
        if ((boxSize & 3) == 0 && boxSize >= 0 && count >= boxSize &&
          header[4] == (byte)0x66 && header[5] == (byte)0x74 &&
          header[6] == (byte)0x79 && header[7] == (byte)0x70) {
          if (header[8] == (byte)0x6d && header[9] == (byte)0x70 &&
            header[10] == (byte)0x34) {
            return "video/mp4";
          }
          int index = 16;
          while (index < boxSize) {
            if ((header[index] & 0xFF) == 'm' &&
              (header[index + 1] & 0xFF) == 'p' &&
              (header[index + 2] & 0xFF) == '4') {
              return "video/mp4";
            }
            index += 4;
          }
        }
      }
      // Archive types
      if (
        MatchesPattern(
          new byte[] { 0x1f, (byte)0x8b, 8 },
          header,
          0,
          count)) {
        return "application/x-gzip";
      }
      if (
        MatchesPattern(
          new byte[] { (byte)0x50, (byte)0x4b, 3, 4 },
          header,
          0,
          count)) {
        return "application/zip";
      }
      if (
        MatchesPattern(
      new byte[] {
        (byte)0x52, (byte)0x61, (byte)0x72,
        (byte)' ', 0x1a, 7, 0,
       }, header, 0, count)) {
        return "application/x-rar-compressed";
      }
      boolean binary = false;
      for (int i = 0; i < count; ++i) {
        int b = header[i] & 0xff;
        if (!(b >= 0x20 || b == 0x09 || b == 0x0a || b == 0x0c || b == 0x0d ||
            b == 0x1b)) {
          binary = true;
          break;
        }
      }
      return (!binary) ? "text/plain" : "application/octet-stream";
    }

    private CharsetSniffer() {
    }
  }
