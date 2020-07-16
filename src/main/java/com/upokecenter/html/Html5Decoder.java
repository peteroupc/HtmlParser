package com.upokecenter.util;
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

import java.io.*;
import com.upokecenter.util.*;
import com.upokecenter.text.*;

  class Html5Decoder implements ICharacterDecoder {
    private ICharacterDecoder valueDecoder = null;
    private boolean valueHavebom = false;
    private boolean valueHavecr = false;
    private boolean valueIserror = false;

    public Html5Decoder(ICharacterDecoder valueDecoder) {
      if (valueDecoder == null) {
        throw new NullPointerException("valueDecoder");
      }
      this.valueDecoder = valueDecoder;
    }

    public int ReadChar(IByteReader byteReader) {
      if (byteReader == null) {
        throw new NullPointerException("byteReader");
      }

      while (true) {
        int c = this.valueDecoder.ReadChar (byteReader);
        // System.out.println("c=" + ((char)c) + ",cc=" + cc);
        if (!this.valueHavebom && !this.valueHavecr && c >= 0x20 && c <= 0x7e) {
          return c;
        }
        if (c < 0) {
          return -1;
        }
        if (c == 0x0d) {
          // CR character
          this.valueHavecr = true;
          c = 0x0a;
        } else if (c == 0x0a && this.valueHavecr) {
          this.valueHavecr = false;
          continue;
        } else {
          this.valueHavecr = false;
        }
        if (c == 0xfeff && !this.valueHavebom) {
          // leading BOM
          this.valueHavebom = true;
          continue;
        } else {
          this.valueHavebom &= c == 0xfeff;
        }
        if (c < 0x09 || (c >= 0x0e && c <= 0x1f) || (c >= 0x7f && c <= 0x9f) ||
          (c & 0xfffe) == 0xfffe || c > 0x10ffff || c == 0x0b || (c >= 0xfdd0 &&
            c <= 0xfdef)) {
          // control character or noncharacter
          this.valueIserror = true;
        }
        return c;
      }
    }

    public int Read(IByteReader stream, int[] buffer, int offset, int length) {
      if (buffer == null) {
        throw new NullPointerException("buffer");
      }
      if (offset < 0) {
        throw new IllegalArgumentException("offset (" + offset +
          ") is less than 0");
      }
      if (offset > buffer.length) {
        throw new IllegalArgumentException("offset (" + offset +
          ") is more than " + buffer.length);
      }
      if (length < 0) {
        throw new IllegalArgumentException("length (" + length +
          ") is less than 0");
      }
      if (length > buffer.length) {
        throw new IllegalArgumentException("length (" + length +
          ") is more than " + buffer.length);
      }
      if (buffer.length - offset < length) {
        throw new IllegalArgumentException("buffer's length minus " + offset + " (" +
          (buffer.length - offset) + ") is less than " + length);
      }
      if (length == 0) {
        return 0;
      }
      int count = 0;
      while (length > 0) {
        int c = this.valueDecoder.ReadChar (stream);
        // System.out.println("read c=" + ((char)c) + ",cc=" + cc);
        if (!this.valueHavebom && !this.valueHavecr && c >= 0x20 && c <= 0x7e) {
          buffer[offset] = c;
          ++offset;
          ++count;
          --length;
          continue;
        }
        if (c < 0) {
          break;
        }
        if (c == 0x0d) {
          // CR character
          this.valueHavecr = true;
          c = 0x0a;
        } else if (c == 0x0a && this.valueHavecr) {
          this.valueHavecr = false;
          continue;
        } else {
          this.valueHavecr = false;
        }
        if (c == 0xfeff && !this.valueHavebom) {
          // leading BOM
          this.valueHavebom = true;
          continue;
        } else if (c != 0xfeff) {
          this.valueHavebom = false;
        }
        if (c < 0x09 || (c >= 0x0e && c <= 0x1f) || (c >= 0x7f && c <= 0x9f) ||
          (c & 0xfffe) == 0xfffe || c > 0x10ffff || c == 0x0b || (c >= 0xfdd0 &&
            c <= 0xfdef)) {
          // control character or noncharacter
          this.valueIserror = true;
        }
        buffer[offset] = c;
        ++offset;
        ++count;
        --length;
      }
      return count == 0 ? -1 : count;
    }
  }
