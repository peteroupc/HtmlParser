/*

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

final class EncodingConfidence {
	int confidence;
	String encoding;
	public static final int Irrelevant=0;
	public static final int Tentative=1;
	public static final int Certain=2;
	public static final EncodingConfidence UTF16BE=
			new EncodingConfidence("utf-16be",Certain);
	public static final EncodingConfidence UTF16LE=
			new EncodingConfidence("utf-16le",Certain);
	public static final EncodingConfidence UTF8=
			new EncodingConfidence("utf-8",Certain);

	public static final EncodingConfidence UTF8_TENTATIVE=
			new EncodingConfidence("utf-8",Tentative);
	public EncodingConfidence(String e){
		encoding=e;
		confidence=Tentative;
	}
	public EncodingConfidence(String e, int c){
		encoding=e;
		confidence=c;
	}
	public int getConfidence() {
		return confidence;
	}
	public String getEncoding() {
		return encoding;
	}
	@Override
	public String toString() {
		return "EncodingConfidence [confidence=" + confidence + ", encoding="
				+ encoding + "]";
	}
}