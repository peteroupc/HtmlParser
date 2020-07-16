package com.upokecenter.util;

import java.util.*;

using Com.Upokecenter.util;
import com.upokecenter.util.*;
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

  class Element extends Node implements IElement {
    private static final class AttributeNameComparator implements Comparator<IAttr> {
      public int compare(IAttr arg0, IAttr arg1) {
        String a = arg0.getName();
        String b = arg1.getName();
        return String.Compare (a, b, StringComparison.Ordinal);
      }
    }

    static Element FromToken(INameAndAttributes token) {
      return FromToken (token, HtmlCommon.HTML_NAMESPACE);
    }

    static Element FromToken(
      INameAndAttributes token,
      String _namespace) {
      Element ret = new Element();
      ret.name = token.getName();
      ret.attributes = new ArrayList<Attr>();
      for (Object attribute : token.getAttributes()) {
        ret.attributes.add(new Attr(attribute));
      }
      ret._namespace = _namespace;
      return ret;
    }

    private String name;

    private String _namespace;

    private String prefix = null;

    private List<Attr> attributes;

    Element() {
 super(NodeType.ELEMENT_NODE);
      this.attributes = new ArrayList<Attr>();
    }

    public Element(String name) {
 super(NodeType.ELEMENT_NODE);
      this.attributes = new ArrayList<Attr>();
      this.name = name;
    }

    void AddAttribute(Attr value) {
      this.attributes.add(value);
    }

    private void CollectElements(INode c, String s, List<IElement> nodes) {
      if (c.getNodeType() == NodeType.ELEMENT_NODE) {
        Element e = (Element)c;
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
        Element e = (Element)c;
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

    public String getAttribute(String name) {
      for (Object attr : this.getAttributes()) {
        if (attr.getName().equals(name)) {
          return attr.getValue();
        }
      }
      return null;
    }

    public String getAttributeNS(String _namespace, String localName) {
      for (Object attr : this.getAttributes()) {
        if ((localName == null ? attr.getLocalName() == null :
            localName.equals(attr.getLocalName())) &&
          (_namespace == null ? attr.getNamespaceURI() == null :
            _namespace.equals(attr.getNamespaceURI()))) {
          return attr.getValue();
        }
      }
      return null;
    }

    public List<IAttr> getAttributes() {
      return Arrays.asList(this.attributes);
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
      if (((Document)this.getOwnerDocument()).isHtmlDocument()) {
        String lowerTagName = DataUtilities.ToLowerCaseAscii (tagName);
        for (Object node : this.getChildNodes()) {
          this.CollectElementsHtml (node, tagName, lowerTagName, ret);
        }
      } else {
        for (Object node : this.getChildNodes()) {
          this.CollectElements (node, tagName, ret);
        }
      }
      return ret;
    }

    public String getId() {
      return this.getAttribute ("id");
    }

    public String getInnerHTML() {
      return this.getInnerHtmlInternal();
    }

    @Override public final String getLanguage() {
      INode parent = this.getParentNode();
      String a = this.getAttributeNS (HtmlCommon.XML_NAMESPACE, "lang");
      a = (a == null) ? (this.getAttribute ("lang")) : a;
      if (a != null) {
        return a;
      }
      if (parent == null) {
        parent = this.getOwnerDocument();
        return (parent == null) ? "" : parent.getLanguage();
      } else {
        return parent.getLanguage();
      }
    }

    public String getLocalName() {
      return this.name;
    }

    public String getNamespaceURI() {
      return this._namespace;
    }

    @Override public final String getNodeName() {
      return this.getTagName();
    }

    public String getPrefix() {
      return this.prefix;
    }

    public String getTagName() {
      String tagName = this.name;
      if (this.prefix != null) {
        tagName = this.prefix + ":" + this.name;
      }
      return ((this.getOwnerDocument() is Document) &&
          HtmlCommon.HTML_NAMESPACE.equals(this._namespace)) ?
        DataUtilities.ToUpperCaseAscii (tagName) : tagName;
    }

    @Override public final String getTextContent() {
      StringBuilder builder = new StringBuilder();
      for (Object node : this.getChildNodes()) {
        if (node.getNodeType() != NodeType.COMMENT_NODE) {
          builder.append (node.getTextContent());
        }
      }
      return builder.toString();
    }

    void MergeAttributes(INameAndAttributes token) {
      for (Object attr : token.getAttributes()) {
        String s = this.getAttribute (attr.getName());
        if (s == null) {
          this.SetAttribute (attr.getName(), attr.getValue());
        }
      }
    }

    void SetAttribute(String _string, String value) {
      for (Object attr : this.getAttributes()) {
        if (attr.getName().equals(_string)) {
          ((Attr)attr).setValue (value);
        }
      }
      this.attributes.add(new Attr(_string, value));
    }

    void SetLocalName(String name) {
      this.name = name;
    }

    void SetNamespace(String _namespace) {
      this._namespace = _namespace;
    }

    public void SetPrefix(String prefix) {
      this.prefix = prefix;
    }

    internal override final String toDebugString() {
      StringBuilder builder = new StringBuilder();
      String extra = "";
      if (HtmlCommon.MATHML_NAMESPACE.equals(this._namespace)) {
        extra = "math ";
      }
      if (HtmlCommon.SVG_NAMESPACE.equals(this._namespace)) {
        extra = "svg ";
      }
      builder.append ("<" + extra + this.name.toString() + ">\n");
      ArrayList<IAttr> attribs = Arrays.asList(this.getAttributes());
      attribs.Sort (new AttributeNameComparator());
      for (Object attribute : attribs) {
        // System.out.println("%s %s"
        // , attribute.getNamespace(), attribute.getLocalName());
        if (attribute.getNamespaceURI() != null) {
          String attributeName = "";
          if (HtmlCommon.XLINK_NAMESPACE.equals(attribute.getNamespaceURI())) {
            attributeName = "xlink ";
          }
          if (HtmlCommon.XML_NAMESPACE.equals(attribute.getNamespaceURI())) {
            attributeName = "xml ";
          }
          attributeName += attribute.getLocalName();
          builder.append ("\u0020\u0020" + attributeName + "=\"" +
            attribute.getValue().toString().replace("\n", "~~~~") + "\"\n");
        } else {
          builder.append ("\u0020\u0020" + attribute.getName().toString() +
            "=\"" +
            attribute.getValue().toString().replace("\n", "~~~~") + "\"\n");
        }
      }
      boolean isTemplate = HtmlCommon.isHtmlElement (this, "template");
      if (isTemplate) {
        builder.append ("\u0020\u0020content\n");
      }
      for (Object node : this.getChildNodesInternal()) {
        String str = ((Node)node).toDebugString();
        if (str == null) {
          continue;
        }
        String[] strarray = StringUtility.splitAt (str, "\n");
        int len = strarray.length;
        if (len > 0 && strarray[len - 1].length == 0) {
          --len; // ignore trailing empty String
        }
        for (int i = 0; i < len; ++i) {
          String el = strarray[i];
          // TODO: Separate template content from child nodes;
          // content is child nodes for convenience currently
          if (isTemplate) {
            {
              builder.append ("\u0020\u0020");
            }
          }
          builder.append ("\u0020\u0020");
          builder.append (el);
          builder.append ("\n");
        }
      }
      return builder.toString();
    }
  }
