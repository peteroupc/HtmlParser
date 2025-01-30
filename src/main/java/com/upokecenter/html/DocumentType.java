package com.upokecenter.html;

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

  final class DocumentType extends Node implements IDocumentType {
    public DocumentType(String name, String pub, String sys) {
 super(NodeType.DOCUMENT_TYPE_NODE);
      this.propVarname = name;
      this.propVarpublicid = pub;
      this.propVarsystemid = sys;
    }

    public final String getSystemId() { return propVarsystemid; }
private final String propVarsystemid;

    public final String getPublicId() { return propVarpublicid; }
private final String propVarpublicid;

    public final String getName() { return propVarname; }
private final String propVarname;

    @Override public final String GetNodeName() {
      return this.GetName();
    }

    public String GetPublicId() {
      return this.getPublicId();
    }

    @Override public final String GetTextContent() {
      return null;
    }

    public String GetSystemId() {
      return this.getSystemId();
    }

    public String GetName() {
      return this.getName();
    }

    @Override final String ToDebugString() {
      StringBuilder builder = new StringBuilder();
      builder.append("<!DOCTYPE " + this.getName());
      if ((this.getPublicId() != null && this.getPublicId().length() > 0) ||
        (this.getSystemId() != null && this.getSystemId().length() > 0)) {
        builder.append(this.getPublicId() != null && this.getPublicId().length() >
          0 ? " \"" + this.getPublicId().replace("\n", "~~~~") + "\"" : " \"\"");
        builder.append(this.getSystemId() != null && this.getSystemId().length() >
          0 ? " \"" + this.getSystemId().replace("\n", "~~~~") + "\"" : " \"\"");
      }
      builder.append(">\n");
      return builder.toString();
    }
  }
