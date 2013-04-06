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



final class DocumentType extends Node implements IDocumentType {

	 String publicId;
	 String systemId;
	 String name;

	public DocumentType() {
		super(NodeType.DOCUMENT_TYPE_NODE);
	}
	@Override
	 String toDebugString(){
		StringBuilder builder=new StringBuilder();
		builder.append("<!DOCTYPE "+name);
		if((publicId!=null && publicId.length()>0) ||
				(systemId!=null && systemId.length()>0)){
			builder.append(publicId!=null && publicId.length()>0 ? " \""+publicId.replace("\n","~~~~")+"\"" : " \"\"");
			builder.append(systemId!=null && systemId.length()>0 ? " \""+systemId.replace("\n","~~~~")+"\"" : " \"\"");
		}
		builder.append(">\n");
		return builder.toString();
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public String getPublicId() {
		return publicId;
	}
	@Override
	public String getSystemId() {
		return systemId;
	}


	@Override
	public  String getTextContent(){
		return null;
	}
}