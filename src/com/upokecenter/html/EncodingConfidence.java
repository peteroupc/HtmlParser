package com.upokecenter.html;

final class EncodingConfidence {
	@Override
	public String toString() {
		return "EncodingConfidence [confidence=" + confidence + ", encoding="
				+ encoding + "]";
	}
	int confidence;
	public int getConfidence() {
		return confidence;
	}
	public String getEncoding() {
		return encoding;
	}
	String encoding;
	public static final int Irrelevant=0;
	public static final int Tentative=1;
	public static final int Certain=2;

	public EncodingConfidence(String e){
		encoding=e;
		confidence=Tentative;
	}
	public EncodingConfidence(String e, int c){
		encoding=e;
		confidence=c;
	}
	public static final EncodingConfidence UTF16BE=
			new EncodingConfidence("utf-16be",Certain);
	public static final EncodingConfidence UTF16LE=
			new EncodingConfidence("utf-16le",Certain);
	public static final EncodingConfidence UTF8=
			new EncodingConfidence("utf-8",Certain);
	public static final EncodingConfidence UTF8_TENTATIVE=
			new EncodingConfidence("utf-8",Tentative);
}