package com.upokecenter.io;

import java.util.*;

import com.upokecenter.util.*;
import com.upokecenter.text.*;

/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

  /**
   * A character input stream where additional inputs can be stacked on.
   */
  public final class StackableCharacterInput implements IMarkableCharacterInput {
    private static class InputAndBuffer implements ICharacterInput {
      private int[] buffer;
      private ICharacterInput charInput;
      private int pos = 0;

      public InputAndBuffer(
        ICharacterInput charInput,
        int[] buffer,
        int offset,
        int length) {
        this.charInput = charInput;
        if (length > 0) {
          this.buffer = new int[length];
          System.arraycopy(buffer, offset, this.buffer, 0, length);
        } else {
          this.buffer = null;
        }
      }

      public int ReadChar() {
        if (this.charInput != null) {
          int c = this.charInput.ReadChar();
          if (c >= 0) {
            return c;
          }
          this.charInput = null;
        }
        if (this.buffer != null) {
          if (this.pos < this.buffer.length) {
            return this.buffer[this.pos++];
          }
          this.buffer = null;
        }
        return -1;
      }

      public int Read(int[] buf, int offset, int unitCount) {
        if (buf == null) {
          throw new NullPointerException("buf");
        }
        if (offset < 0) {
          throw new IllegalArgumentException("offset less than 0(" + offset + ")");
        }
        if (unitCount < 0) {
          throw new IllegalArgumentException("unitCount less than 0(" + unitCount +
            ")");
        }
        if (offset + unitCount > buf.length) {
          throw new
          IllegalArgumentException("offset+unitCount more than " +
            buf.length + " (" + (offset + unitCount) + ")");
        }
        if (unitCount == 0) {
          return 0;
        }
        int count = 0;
        if (this.charInput != null) {
          int c = this.charInput.Read(buf, offset, unitCount);
          if (c <= 0) {
            this.charInput = null;
          } else {
            offset += c;
            unitCount -= c;
            count += c;
          }
        }
        if (this.buffer != null) {
          int c = Math.min(unitCount, this.buffer.length - this.pos);
          if (c > 0) {
            System.arraycopy(this.buffer, this.pos, buf, offset, c);
          }
          this.pos += c;
          count += c;
          if (c == 0) {
            this.buffer = null;
          }
        }
        return count;
      }
    }

    private int pos;
    private int endpos;
    private boolean haveMark;
    private int[] buffer;
    private List<ICharacterInput> stack = new ArrayList<ICharacterInput>();

    /**
     * Initializes a new instance of the {@link
     * com.upokecenter.io.StackableCharacterInput} class.
     * @param source The parameter {@code source} is an ICharacterInput object.
     */
    public StackableCharacterInput(ICharacterInput source) {
      this.stack.add(source);
    }

    /**
     * Not documented yet.
     * @return A 32-bit signed integer.
     */
    public int GetMarkPosition() {
      return this.pos;
    }

    /**
     * Not documented yet.
     * @param count The parameter {@code count} is a 32-bit signed integer.
     */
    public void MoveBack(int count) {
      if (count < 0) {
        throw new IllegalArgumentException("count(" + count +
          ") is not greater or equal to 0");
      }
      if (this.haveMark && this.pos >= count) {
        this.pos -= count;
        return;
      }
      throw new IllegalStateException();
    }

    /**
     * Not documented yet.
     * @param input The parameter {@code input} is a.getText().ICharacterInput object.
     * @throws NullPointerException The parameter {@code input} is null.
     */
    public void PushInput(ICharacterInput input) {
      if (input == null) {
        throw new NullPointerException("input");
      }
      // Move unread characters in buffer, since this new
      // input sits on top of the existing input
      this.stack.add(
        new InputAndBuffer(
          input,
          this.buffer,
          this.pos,
          this.endpos - this.pos));
      this.endpos = this.pos;
    }

    /**
     * Not documented yet.
     * @return A 32-bit signed integer.
     */
    public int ReadChar() {
      if (this.haveMark) {
        // Read from buffer
        if (this.pos < this.endpos) {
          int ch = this.buffer[this.pos++];
          // System.out.println ("buffer: [" + ch + "],["+(char)ch+"]");
          return ch;
        }
        // System.out.println(this);
        // End pos is smaller than buffer size, fill
        // entire buffer if possible
        if (this.endpos < this.buffer.length) {
          int count = this.ReadInternal(
              this.buffer,
              this.endpos,
              this.buffer.length - this.endpos);
          if (count > 0) {
            this.endpos += count;
          }
        }
        // Try reading from buffer again
        if (this.pos < this.endpos) {
          int ch = this.buffer[this.pos++];
          // System.out.println ("buffer2: [" + ch + "],[" + charch + "]");
          return ch;
        }
        // System.out.println(this);
        // No room, read next character and put it in buffer
        int c = this.ReadInternal();
        if (c < 0) {
          return c;
        }
        if (this.pos >= this.buffer.length) {
          int[] newBuffer = new int[this.buffer.length * 2];
          System.arraycopy(this.buffer, 0, newBuffer, 0, this.buffer.length);
          this.buffer = newBuffer;
        }
        // System.out.println(this);
        this.buffer[this.pos++] = c;
        ++this.endpos;
        // System.out.println ("readInt3: [" + c + "],[" + charc + "]");
        return c;
      } else {
        int c = this.ReadInternal();
        // System.out.println ("readInt3: [" + c + "],[" + charc + "]");
        return c;
      }
    }

    /**
     * Not documented yet.
     * @param buf The parameter {@code buf} is a.getInt32()[] object.
     * @param offset The parameter {@code offset} is a 32-bit signed integer.
     * @param unitCount The parameter {@code unitCount} is a 32-bit signed integer.
     * @return A 32-bit signed integer.
     * @throws NullPointerException The parameter {@code buf} is null.
     */
    public int Read(int[] buf, int offset, int unitCount) {
      if (buf == null) {
        throw new NullPointerException("buf");
      }
      if (offset < 0) {
        throw new IllegalArgumentException("offset(" + offset +
          ") is less than 0");
      }
      if (offset > buf.length) {
        throw new IllegalArgumentException("offset(" + offset +
          ") is more than " + buf.length);
      }
      if (unitCount < 0) {
        throw new IllegalArgumentException("unitCount(" + unitCount +
          ") is less than 0");
      }
      if (unitCount > buf.length) {
        throw new IllegalArgumentException("unitCount(" + unitCount +
          ") is more than " + buf.length);
      }
      if (buf.length - offset < unitCount) {
        throw new IllegalArgumentException("buf's length minus " + offset + "(" +
          (buf.length - offset) + ") is less than " + unitCount);
      }
      if (this.haveMark) {
        if (unitCount == 0) {
          return 0;
        }
        // Read from buffer
        if (this.pos + unitCount <= this.endpos) {
          System.arraycopy(this.buffer, this.pos, buf, offset, unitCount);
          this.pos += unitCount;
          return unitCount;
        }
        // End pos is smaller than buffer size, fill
        // entire buffer if possible
        int count = 0;
        if (this.endpos < this.buffer.length) {
          count = this.ReadInternal(
              this.buffer,
              this.endpos,
              this.buffer.length - this.endpos);
          // System.out.println("%s",this);
          if (count > 0) {
            this.endpos += count;
          }
        }
        int total = 0;
        // Try reading from buffer again
        if (this.pos + unitCount <= this.endpos) {
          System.arraycopy(this.buffer, this.pos, buf, offset, unitCount);
          this.pos += unitCount;
          return unitCount;
        }
        // expand the buffer
        if (this.pos + unitCount > this.buffer.length) {
          int[] newBuffer = new int[(this.buffer.length * 2) + unitCount];
          System.arraycopy(this.buffer, 0, newBuffer, 0, this.buffer.length);
          this.buffer = newBuffer;
        }
        count = this.ReadInternal(
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
          System.arraycopy(
            this.buffer,
            this.pos,
            buf,
            offset,
            this.endpos - this.pos);
          total += this.endpos - this.pos;
          this.pos = this.endpos;
        }
        return total;
      } else {
        return this.ReadInternal(buf, offset, unitCount);
      }
    }

    private int ReadInternal() {
      if (this.stack.size() == 0) {
        return -1;
      }
      while (this.stack.size() > 0) {
        int index = this.stack.size() - 1;
        int c = this.stack.get(index).ReadChar();
        if (c == -1) {
          this.stack.remove(index);
          continue;
        }
        return c;
      }
      return -1;
    }

    private int ReadInternal(int[] buf, int offset, int unitCount) {
      if (this.stack.size() == 0) {
        return -1;
      }
      if (unitCount == 0) {
        return 0;
      }
      int count = 0;
      while (this.stack.size() > 0 && unitCount > 0) {
        int index = this.stack.size() - 1;
        int c = this.stack.get(index).Read(buf, offset, unitCount);
        if (c <= 0) {
          this.stack.remove(index);
          continue;
        }
        count += c;
        unitCount -= c;
        if (unitCount == 0) {
          break;
        }
        this.stack.remove(index);
      }
      return count;
    }

    /**
     * Not documented yet.
     * @return A 32-bit signed integer.
     */
    public int SetHardMark() {
      if (this.buffer == null) {
        this.buffer = new int[16];
        this.pos = 0;
        this.endpos = 0;
        this.haveMark = true;
      } else if (this.haveMark) {
        // Already have a mark; shift buffer to the new mark
        if (this.pos > 0 && this.pos < this.endpos) {
          System.arraycopy(
            this.buffer,
            this.pos,
            this.buffer,
            0,
            this.endpos - this.pos);
        }
        this.endpos -= this.pos;
        this.pos = 0;
      } else {
        this.pos = 0;
        this.endpos = 0;
        this.haveMark = true;
      }
      return 0;
    }

    /**
     * Not documented yet.
     * @param pos The parameter {@code pos} is a 32-bit signed integer.
     */
    public void SetMarkPosition(int pos) {
      if (!this.haveMark || pos < 0 || pos > this.endpos) {
        throw new IllegalStateException();
      }
      this.pos = pos;
    }

    /**
     * Not documented yet.
     * @return A 32-bit signed integer.
     */
    public int SetSoftMark() {
      if (!this.haveMark) {
        this.SetHardMark();
      }
      return this.GetMarkPosition();
    }
  }
