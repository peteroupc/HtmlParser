package com.upokecenter.util;

import java.util.*;

using Com.Upokecenter.util;
import com.upokecenter.util.*;
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

  class Document extends Node implements IDocument {
    final DocumentType getDoctype() { return propVardoctype; }
final void setDoctype(DocumentType value) { propVardoctype = value; }
private DocumentType propVardoctype;

    final String getEncoding() { return propVarencoding; }
final void setEncoding(String value) { propVarencoding = value; }
private String propVarencoding;

    private DocumentMode docmode = DocumentMode.NoQuirksMode;

    final String getDefaultLanguage() { return propVardefaultlanguage; }
final void setDefaultLanguage(String value) { propVardefaultlanguage = value; }
private String propVardefaultlanguage;

    final String getAddress() { return propVaraddress; }
final void setAddress(String value) { propVaraddress = value; }
private String propVaraddress;

    Document() {
 super(NodeType.DOCUMENT_NODE);
    }

    private void CollectElements(INode c, String s, List<IElement> nodes) {
      if (c.getNodeType() == NodeType.ELEMENT_NODE) {
        IElement e = (IElement)c;
        if (s == null || e.getLocalName().equals(s)) {
          nodes.add(e);
        }
      }
      for (Object node : c.getChildNodes()) {
        this.CollectElements (node, s, nodes);
      }
    }

    private void CollectElementsHtml(
      INode c,
      String s,
      String valueSLowercase,
      List<IElement> nodes) {
      if (c.getNodeType() == NodeType.ELEMENT_NODE) {
        IElement e = (IElement)c;
        if (s == null) {
          nodes.add(e);
        } else if (HtmlCommon.HTML_NAMESPACE.equals(e.getNamespaceURI()) && e.getLocalName().equals(valueSLowercase)) {
          nodes.add(e);
        } else if (e.getLocalName().equals(s)) {
          nodes.add(e);
        }
      }
      for (Object node : c.getChildNodes()) {
        this.CollectElements (node, s, nodes);
      }
    }

    public String getCharset() {
      return (this.getEncoding() == null) ? "utf-8" : this.getEncoding();
    }

    public IDocumentType getDoctype() {
      return this.getDoctype();
    }

    public IElement getDocumentElement() {
      for (Object node : this.getChildNodes()) {
        if (node instanceof IElement) {
          return (IElement)node;
        }
      }
      return null;
    }

    public IElement getElementById(String id) {
      if (id == null) {
        throw new IllegalArgumentException();
      }
      for (Object node : this.getChildNodes()) {
        if (node instanceof IElement) {
          if (id.equals(((IElement)node).getId())) {
            return (IElement)node;
          }
          IElement element = ((IElement)node).getElementById (id);
          if (element != null) {
            return element;
          }
        }
      }
      return null;
    }

    public List<IElement> getElementsByTagName(String tagName) {
      if (tagName == null) {
        throw new IllegalArgumentException();
      }
      if (tagName.equals("*")) {
        tagName = null;
      }
      List<IElement> ret = new ArrayList<IElement>();
      if (this.IsHtmlDocument()) {
        this.CollectElementsHtml(
          this,
          tagName,
          com.upokecenter.util.DataUtilities.ToLowerCaseAscii (tagName),
          ret);
      } else {
        this.CollectElements (this, tagName, ret);
      }
      return ret;
    }

    @Override public String getLanguage() {
      return (this.getDefaultLanguage() == null) ? "" :
        this.getDefaultLanguage();
    }

    DocumentMode GetMode() {
      return this.docmode;
    }

    @Override public String getNodeName() {
      return "#document";
    }

    @Override public IDocument getOwnerDocument() {
      return null;
    }

    public String getURL() {
      return this.getAddress();
    }

    boolean IsHtmlDocument() {
      return true;
    }

    void SetMode(DocumentMode mode) {
      this.docmode = mode;
    }

    @Override public String toString() {
      return this.ToDebugString();
    }

    internal override String ToDebugString() {
      return ToDebugString (this.getChildNodes());
    }

    static String ToDebugString(List<INode> nodes) {
      StringBuilder builder = new StringBuilder();
      for (Object node : nodes) {
        String str = ((Node)node).ToDebugString();
        if (str == null) {
          continue;
        }
        String[] strarray = StringUtility.splitAt (str, "\n");
        int len = strarray.length;
        if (len > 0 && strarray[len - 1].length == 0) {
          --len; // ignore trailing empty _string
        }
        for (int i = 0; i < len; ++i) {
          String el = strarray[i];
          builder.append ("| ");
          builder.append (el.replace("~~~~", "\n"));
          builder.append ("\n");
        }
      }
      return builder.toString();
    }
  }
