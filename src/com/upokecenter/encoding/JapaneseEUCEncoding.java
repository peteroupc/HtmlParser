/*
If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/



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
package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


final class JapaneseEUCEncoding implements ITextEncoder, ITextDecoder {

  int eucjp1=0;
  int eucjp2=0;

  @Override
  public int decode(InputStream stream) throws IOException {
    return decode(stream, TextEncoding.ENCODING_ERROR_THROW);
  }

  @Override
  public int decode(InputStream stream, IEncodingError error) throws IOException {
    int[] value=new int[1];
    int c=decode(stream,value,0,1, error);
    if(c<=0)return -1;
    return value[0];
  }

  @Override
  public int decode(InputStream stream, int[] buffer, int offset, int length)
      throws IOException {
    return decode(stream, buffer, offset, length, TextEncoding.ENCODING_ERROR_THROW);
  }

  @Override
  public int decode(InputStream stream, int[] buffer, int offset, int length, IEncodingError error)
      throws IOException {
    if(stream==null || buffer==null || offset<0 || length<0 ||
        offset+length>buffer.length)
      throw new IllegalArgumentException();
    int count=0;
    while(length>0){
      int b=stream.read();
      if(b<0 && (eucjp1|eucjp2)==0)
        return (count==0) ? -1 : count;
      else if(b<0){
        int o=error.emitDecoderError(buffer, offset, length);
        offset+=o;
        count+=o;
        length-=o;
        break;
      }
      if(eucjp2!=0){
        int lead=0;
        eucjp2=0;
        int cp=0;
        if((lead>=0xA1 && lead<=0xFE) && (b>=0xA1 && b<=0xFE)){
          int index=(lead-0xA1)*94+b-0xA1;
          cp=JIS0212.indexToCodePoint(index);
        }
        if(b<0xA1 || b==0xFF){
          stream.reset();
        }
        if(cp<=0){
          int o=error.emitDecoderError(buffer, offset, length);
          offset+=o;
          count+=o;
          length-=o;
          continue;
        } else {
          buffer[offset++]=(cp);
          count++;
          length--;
          continue;
        }
      }
      if(eucjp1==0x8E && b>=0xA1 && b<=0xDF){
        eucjp1=0;
        buffer[offset++]=(0xFF61+b-0xA1);
        count++;
        length--;
        //DebugUtility.log("return 0xFF61 cp: %04X",0xFF61+b-0xA1);
        continue;
      }
      if(eucjp1==0x8F && b>=0xA1 && b<=0xFE){
        eucjp1=0;
        eucjp2=b;
        stream.mark(4);
        continue;
      }
      if(eucjp1!=0){
        int lead=eucjp1;
        eucjp1=0;
        int cp=0;
        if((lead>=0xA1 && lead<=0xFE) && (b>=0xA1 && b<=0xFE)){
          int index=(lead-0xA1)*94+b-0xA1;
          cp=JIS0208.indexToCodePoint(index);
          //DebugUtility.log("return 0208 cp: %04X lead=%02X b=%02X index=%04X",cp,lead,b,index);
        }
        if(b<0xA1 || b==0xFF){
          stream.reset();
        }
        if(cp==0){
          int o=error.emitDecoderError(buffer, offset, length);
          offset+=o;
          count+=o;
          length-=o;
          continue;
        } else {
          buffer[offset++]=(cp);
          count++;
          length--;
          continue;
        }
      }
      if(b<0x80){
        buffer[offset++]=(b);
        count++;
        length--;
        continue;
      } else if(b==0x8E || b==0x8F || (b>=0xA1 && b<=0xFE)){
        eucjp1=b;
        stream.mark(4);
        continue;
      } else {
        int o=error.emitDecoderError(buffer, offset, length);
        offset+=o;
        count+=o;
        length-=o;
        continue;
      }
    }
    return (count==0) ? -1 : count;
  }

  @Override
  public void encode(OutputStream stream, int[] array, int offset, int length)
      throws IOException {
    encode(stream, array, offset, length, TextEncoding.ENCODING_ERROR_THROW);
  }

  @Override
  public void encode(OutputStream stream, int[] array, int offset, int length, IEncodingError error)
      throws IOException {
    if(stream==null || array==null)throw new IllegalArgumentException();
    if(offset<0 || length<0 || offset+length>array.length)
      throw new IndexOutOfBoundsException();
    for(int i=0;i<array.length;i++){
      int cp=array[offset+i];
      if(cp<0 || cp>=0x10000){
        error.emitEncoderError(stream, cp);
        continue;
      }
      if(cp<=0x7F){
        stream.write(cp);
      } else if(cp==0xA5){
        stream.write(0x5c);
      } else if(cp==0x203E){
        stream.write(0x7E);
      } else {
        int index=JIS0208.codePointToIndex(cp);
        if(index<0){
          error.emitEncoderError(stream, cp);
          continue;
        }
        stream.write(index/94+0xa1);
        stream.write(index%94+0xa1);
      }
    }
  }
}
