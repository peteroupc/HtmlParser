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

package com.upokecenter.html;

import java.io.IOException;
import java.io.InputStream;

import com.upokecenter.encoding.IEncodingError;
import com.upokecenter.encoding.ITextDecoder;
import com.upokecenter.encoding.TextEncoding;

class Html5Decoder implements ITextDecoder {

  ITextDecoder decoder=null;
  boolean havebom=false;
  boolean havecr=false;
  boolean iserror=false;
  public Html5Decoder(ITextDecoder decoder){
    if(decoder==null)throw new IllegalArgumentException();
    this.decoder=decoder;
  }

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
    if(length==0)return 0;
    int count=0;
    while(length>0){
      int c=decoder.decode(stream, error);
      if(!havebom && !havecr && c>=0x20 && c<=0x7E){
        buffer[offset]=c;
        offset++;
        count++;
        length--;
        continue;
      }
      if(c<0) {
        break;
      }
      if(c==0x0D){
        // CR character
        havecr=true;
        c=0x0A;
      } else if(c==0x0A && havecr){
        havecr=false;
        continue;
      } else {
        havecr=false;
      }
      if(c==0xFEFF && !havebom){
        // leading BOM
        havebom=true;
        continue;
      } else if(c!=0xFEFF){
        havebom=false;
      }
      if(c<0x09 || (c>=0x0E && c<=0x1F) || (c>=0x7F && c<=0x9F) ||
          (c&0xFFFE)==0xFFFE || c>0x10FFFF || c==0x0B || (c>=0xFDD0 && c<=0xFDEF)){
        // control character or noncharacter
        iserror=true;
      }
      buffer[offset]=c;
      offset++;
      count++;
      length--;
    }
    return count==0 ? -1 : count;
  }

  public boolean isError(){
    return iserror;
  }
}
