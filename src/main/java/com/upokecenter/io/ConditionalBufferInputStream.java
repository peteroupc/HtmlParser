package com.upokecenter.io;
/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

import java.io.*;
import com.upokecenter.util.*;

  /**
   * An input reader that stores the first bytes of the reader in a buffer and
   * supports rewinding to the beginning of the reader. However, when the buffer
   * is disabled, no further bytes are put into the buffer, but any remaining
   * bytes in the buffer will still be used until it's exhausted.
   */
  public final class ConditionalBufferInputStream implements IByteReader {
    private byte[] buffer = null;
    private int pos = 0;
    private int endpos = 0;
    private boolean disabled = false;
    private long markpos = -1;
    private int posAtMark = 0;
    private long marklimit = 0;
    private IReader reader = null;

    /**
     * Initializes a new instance of the ConditionalBufferInputStream class.
     * @param input The parameter {@code input} is an IReader object.
     */
    public ConditionalBufferInputStream(IReader input) {
      this.reader = input;
      this.buffer = new byte[1024];
    }

    /**
     * Disables buffering of future bytes read from the underlying reader. However,
     * any bytes already buffered can still be read until the buffer is exhausted.
     * After the buffer is exhausted, this reader will fully delegate to the
     * underlying reader.
     */
    public void DisableBuffer() {
      this.disabled = true;
      if (this.buffer != null && this.IsDisabled()) {
        this.buffer = null;
      }
    }

    private int DoRead(byte[] buffer, int offset, int byteCount) {
      if (this.markpos < 0) {
        return this.ReadInternal(buffer, offset, byteCount);
      } else {
        if (this.IsDisabled()) {
          return this.reader.Read(buffer, offset, byteCount);
        }
        int c = this.ReadInternal(buffer, offset, byteCount);
        if (c > 0 && this.markpos >= 0) {
          this.markpos += c;
          if (this.markpos > this.marklimit) {
            this.marklimit = 0;
            this.markpos = -1;
            if (this.buffer != null && this.IsDisabled()) {
              this.buffer = null;
            }
          }
        }
        return c;
      }
    }

    private boolean IsDisabled() {
      return this.disabled ? ((this.markpos >= 0 &&
        this.markpos < this.marklimit) ? false : (this.pos >=
          this.endpos)) : false;
    }

    /**
     * Not documented yet.
     * @param limit The parameter {@code limit} is a 32-bit signed integer.
     */
    public void Mark(int limit) {
      // System.out.println("Mark %d: %s",limit,IsDisabled());
      if (this.IsDisabled()) {
        // this.reader.Mark(limit);
        // return;
        throw new UnsupportedOperationException();
      }
      if (limit < 0) {
        throw new IllegalArgumentException();
      }
      this.markpos = 0;
      this.posAtMark = this.pos;
      this.marklimit = limit;
    }

    /**
     * Not documented yet.
     * @return Either {@code true} or {@code false}.
     */
    public boolean MarkSupported() {
      return true;
    }

    /**
     * Not documented yet.
     * @return A 32-bit signed integer.
     */
    public int read() {
      if (this.markpos < 0) {
        return this.ReadInternal();
      } else {
        if (this.IsDisabled()) {
          return this.reader.read();
        }
        int c = this.ReadInternal();
        if (c >= 0 && this.markpos >= 0) {
          ++this.markpos;
          if (this.markpos > this.marklimit) {
            this.marklimit = 0;
            this.markpos = -1;
            if (this.buffer != null && this.IsDisabled()) {
              this.buffer = null;
            }
          }
        }
        return c;
      }
    }

    /**
     * Not documented yet.
     * @param buffer The parameter {@code buffer} is a.getByte()[] object.
     * @param offset The parameter {@code offset} is a 32-bit signed integer.
     * @param byteCount The parameter {@code byteCount} is a 32-bit signed integer.
     * @return A 32-bit signed integer.
     */
    public int Read(byte[] buffer, int offset, int byteCount) {
      return this.DoRead(buffer, offset, byteCount);
    }

    private int ReadInternal() {
      // Read from buffer
      if (this.pos < this.endpos) {
        return this.buffer[this.pos++] & 0xff;
      }
      if (this.disabled) {
        // Buffering new bytes is disabled, so read directly from reader
        return this.reader.read();
      }
      // if (buffer != null) {
      // System.out.println("buffer %s end=%s len=%s",pos,endpos,buffer.length);
      // }
      // End pos is smaller than buffer size, fill
      // entire buffer if possible
      if (this.endpos < this.buffer.length) {
        int count = this.reader.Read(
            this.buffer,
            this.endpos,
            this.buffer.length - this.endpos);
        if (count > 0) {
          this.endpos += count;
        }
      }
      // Try reading from buffer again
      if (this.pos < this.endpos) {
        return this.buffer[this.pos++] & 0xff;
      }
      // No room, read next byte and put it in buffer
      int c = this.reader.read();
      if (c < 0) {
        return c;
      }
      if (this.pos >= this.buffer.length) {
        byte[] newBuffer = new byte[this.buffer.length * 2];
        System.arraycopy(this.buffer, 0, newBuffer, 0, this.buffer.length);
        this.buffer = newBuffer;
      }
      this.buffer[this.pos++] = (byte)(c & 0xff);
      ++this.endpos;
      return c;
    }

    private int ReadInternal(byte[] buf, int offset, int unitCount) {
      if (buf == null) {
        throw new IllegalArgumentException();
      }
      if (offset < 0 || unitCount < 0 || offset + unitCount > buf.length) {
        throw new IllegalArgumentException();
      }
      if (unitCount == 0) {
        return 0;
      }
      int total = 0;
      int count = 0;
      // Read from buffer
      if (this.pos + unitCount <= this.endpos) {
        System.arraycopy(this.buffer, this.pos, buf, offset, unitCount);
        this.pos += unitCount;
        return unitCount;
      }
      // if (buffer != null) {
      // System.out.println("buffer(3arg) %s end=%s len=%s"
      // , pos, endpos, buffer.length);
      // }
      if (this.disabled) {
        // Buffering disabled, read as much as possible from the buffer
        if (this.pos < this.endpos) {
          int c = Math.min(unitCount, this.endpos - this.pos);
          System.arraycopy(this.buffer, this.pos, buf, offset, c);
          this.pos = this.endpos;
          offset += c;
          unitCount -= c;
          total += c;
        }
        // Read directly from the reader for the rest
        if (unitCount > 0) {
          int c = this.reader.Read(buf, offset, unitCount);
          if (c > 0) {
            total += c;
          }
        }
        return (total == 0) ? -1 : total;
      }
      // End pos is smaller than buffer size, fill
      // entire buffer if possible
      if (this.endpos < this.buffer.length) {
        count = this.reader.Read(
            this.buffer,
            this.endpos,
            this.buffer.length - this.endpos);
        // System.out.println("%s",this);
        if (count > 0) {
          this.endpos += count;
        }
      }
      // Try reading from buffer again
      if (this.pos + unitCount <= this.endpos) {
        System.arraycopy(this.buffer, this.pos, buf, offset, unitCount);
        this.pos += unitCount;
        return unitCount;
      }
      // expand the buffer
      if (this.pos + unitCount > this.buffer.length) {
        byte[] newBuffer = new byte[(this.buffer.length * 2) + unitCount];
        System.arraycopy(this.buffer, 0, newBuffer, 0, this.buffer.length);
        this.buffer = newBuffer;
      }
      count = this.reader.Read(
          this.buffer,
          this.endpos,
          Math.min(unitCount, this.buffer.length - this.endpos));
      if (count > 0) {
        this.endpos += count;
      }
      // Try reading from buffer a third time
      if (this.pos + unitCount <= this.endpos) {
        System.arraycopy(this.buffer, this.pos, buf, offset, unitCount);
        this.pos += unitCount;
        total += unitCount;
      } else if (this.endpos > this.pos) {
        System.arraycopy(this.buffer, this.pos, buf, offset, this.endpos - this.pos);
        total += this.endpos - this.pos;
        this.pos = this.endpos;
      }
      return (total == 0) ? -1 : total;
    }

    /**
     * Not documented yet.
     */
    public void Reset() {
      // System.out.println("Reset: %s",IsDisabled());
      if (this.IsDisabled()) {
        throw new UnsupportedOperationException();
        // this.reader.Reset();
        // return;
      }
      if (this.markpos < 0) {
        throw new IOException();
      }
      this.pos = this.posAtMark;
    }

    /**
     * Resets the reader to the beginning of the input. This will invalidate the
     * Mark placed on the reader, if any. Throws if DisableBuffer() was already
     * called.
     */
    public void Rewind() {
      if (this.disabled) {
        throw new IOException();
      }
      this.pos = 0;
      this.markpos = -1;
    }

    /**
     * Not documented yet.
     * @param byteCount The parameter {@code byteCount} is a 64-bit signed integer.
     * @return A 64-bit signed integer.
     */
    public long Skip(long byteCount) {
      if (this.IsDisabled()) {
        throw new UnsupportedOperationException();
        // return this.reader.Skip(byteCount);
      }
      byte[] data = new byte[1024];
      long ret = 0;
      while (byteCount < 0) {
        int bc = (int)Math.min(byteCount, data.length);
        int c = this.DoRead(data, 0, bc);
        if (c <= 0) {
          break;
        }
        ret += c;
        byteCount -= c;
      }
      return ret;
    }
  }
