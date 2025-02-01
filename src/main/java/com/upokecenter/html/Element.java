package com.upokecenter.html;

import java.util.*;

import com.upokecenter.util.*;
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

  class Element extends Node implements IElement {
    private static final class AttributeNameComparator implements Comparator<IAttr> {
      public int compare(IAttr arg0, IAttr arg1) {
        String a = arg0.GetName();
        String b = arg1.GetName();
        return a.compareTo(b);
      }
    }

    static Element FromToken(INameAndAttributes token) {
      return FromToken(token, HtmlCommon.HTML_NAMESPACE);
    }

    static Element FromToken(
      INameAndAttributes token,
      String namespaceValue) {
      Element ret = new Element();
      ret.name = token.GetName();
      ret.attributes = new ArrayList<Attr>();
      for (IAttr attribute : token.GetAttributes()) {
        ret.attributes.add(new Attr(attribute));
      }
      ret.namespaceValue = namespaceValue;
      return ret;
    }

    private String name;

    private String namespaceValue;

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
      if (c.GetNodeType() == NodeType.ELEMENT_NODE) {
        Element e = (Element)c;
        if (s == null || e.GetLocalName().equals(s)) {
          nodes.add(e);
        }
      }
      for (INode node : c.GetChildNodes()) {
        this.CollectElements(node, s, nodes);
      }
    }

    private void CollectElementsHtml(
      INode c,
      String s,
      String valueSLowercase,
      List<IElement> nodes) {
      if (c.GetNodeType() == NodeType.ELEMENT_NODE) {
        Element e = (Element)c;
        if (s == null) {
          nodes.add(e);
        } else if (HtmlCommon.HTML_NAMESPACE.equals(e.GetNamespaceURI()) && e.GetLocalName().equals(
            valueSLowercase)) {
          nodes.add(e);
        } else if (e.GetLocalName().equals(s)) {
          nodes.add(e);
        }
      }
      for (INode node : c.GetChildNodes()) {
        this.CollectElements(node, s, nodes);
      }
    }

    public String GetAttribute(String name) {
      for (IAttr attr : this.GetAttributes()) {
        if (attr.GetName().equals(name)) {
          return attr.GetValue();
        }
      }
      return null;
    }

    public String GetAttributeNS(String namespaceValue, String localName) {
      for (IAttr attr : this.GetAttributes()) {
        if ((localName == null ? attr.GetLocalName() == null :
          localName.equals(attr.GetLocalName())) &&
          (namespaceValue == null ? attr.GetNamespaceURI() == null :
          namespaceValue.equals(attr.GetNamespaceURI()))) {
          return attr.GetValue();
        }
      }
      return null;
    }

    private ArrayList<IAttr> GetAttributesList() {
      ArrayList<IAttr> attrs = new ArrayList<IAttr>();
      List<Attr> thisattrs = this.attributes;
      for (Attr attr : thisattrs) {
        attrs.add(attr);
      }
      return attrs;
    }

    public List<IAttr> GetAttributes() {
      return this.GetAttributesList();
    }

    public IElement GetElementById(String id) {
      if (id == null) {
        throw new IllegalArgumentException();
      }
      for (INode node : this.GetChildNodes()) {
        if (node instanceof IElement) {
          if (id.equals(((IElement)node).GetId())) {
            return (IElement)node;
          }
          IElement element = ((IElement)node).GetElementById(id);
          if (element != null) {
            return element;
          }
        }
      }
      return null;
    }

    public List<IElement> GetElementsByTagName(String tagName) {
      if (tagName == null) {
        throw new IllegalArgumentException();
      }
      if (tagName.equals("*")) {
        tagName = null;
      }
      List<IElement> ret = new ArrayList<IElement>();
      if (((Document)this.GetOwnerDocument()).IsHtmlDocument()) {
        String lowerTagName = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(tagName);
        for (INode node : this.GetChildNodes()) {
          this.CollectElementsHtml(node, tagName, lowerTagName, ret);
        }
      } else {
        for (INode node : this.GetChildNodes()) {
          this.CollectElements(node, tagName, ret);
        }
      }
      return ret;
    }

    public String GetId() {
      return this.GetAttribute("id");
    }

    public String GetInnerHTML() {
      return this.GetInnerHtmlInternal();
    }

    @Override public final String GetLanguage() {
      INode parent = this.GetParentNode();
      String a = this.GetAttributeNS(HtmlCommon.XML_NAMESPACE, "lang");
      a = (a == null) ? (this.GetAttribute("lang")) : a;
      if (a != null) {
        return a;
      }
      if (parent == null) {
        parent = this.GetOwnerDocument();
        return (parent == null) ? "" : parent.GetLanguage();
      } else {
        return parent.GetLanguage();
      }
    }

    public String GetLocalName() {
      return this.name;
    }

    public String GetNamespaceURI() {
      return this.namespaceValue;
    }

    @Override public final String GetNodeName() {
      return this.GetTagName();
    }

    public String GetPrefix() {
      return this.prefix;
    }

    public String GetTagName() {
      String tagName = this.name;
      if (this.prefix != null) {
        tagName = this.prefix + ":" + this.name;
      }
      return ((this.GetOwnerDocument() instanceof Document) &&
        HtmlCommon.HTML_NAMESPACE.equals(this.namespaceValue)) ?
        com.upokecenter.util.DataUtilities.ToUpperCaseAscii(tagName) : tagName;
    }

    @Override public final String GetTextContent() {
      StringBuilder builder = new StringBuilder();
      for (INode node : this.GetChildNodes()) {
        if (node.GetNodeType() != NodeType.COMMENT_NODE) {
          builder.append(node.GetTextContent());
        }
      }
      return builder.toString();
    }

    void MergeAttributes(INameAndAttributes token) {
      for (IAttr attr : token.GetAttributes()) {
        String s = this.GetAttribute(attr.GetName());
        if (s == null) {
          this.SetAttribute(attr.GetName(), attr.GetValue());
        }
      }
    }

    void SetAttribute(String stringValue, String value) {
      for (IAttr attr : this.GetAttributes()) {
        if (attr.GetName().equals(stringValue)) {
          ((Attr)attr).SetValue(value);
        }
      }
      this.attributes.add(new Attr(stringValue, value));
    }

    void SetLocalName(String name) {
      this.name = name;
    }

    void SetNamespace(String namespaceValue) {
      this.namespaceValue = namespaceValue;
    }

    public void SetPrefix(String prefix) {
      this.prefix = prefix;
    }

    @Override final String ToDebugString() {
      StringBuilder builder = new StringBuilder();
      String extra = "";
      String ns = this.namespaceValue;
      if (!((ns) == null || (ns).length() == 0)) {
        if (ns.equals(HtmlCommon.MATHML_NAMESPACE)) {
          extra = "math ";
        }
        if (ns.equals(HtmlCommon.SVG_NAMESPACE)) {
          extra = "svg ";
        }
      }
      builder.append("<" + extra + this.name.toString() + ">\n");
      ArrayList<IAttr> attribs = this.GetAttributesList();
      java.util.Collections.sort(attribs, new AttributeNameComparator());
      for (IAttr attribute : attribs) {
        // System.out.println("%s %s"
        // , attribute.Getspace(), attribute.GetLocalName());
        String nsuri = attribute.GetNamespaceURI();
        if (nsuri != null) {
          String attributeName = "";
          if (HtmlCommon.XLINK_NAMESPACE.equals(nsuri)) {
            attributeName = "xlink ";
          }
          if (HtmlCommon.XML_NAMESPACE.equals(nsuri)) {
            attributeName = "xml ";
          }
          attributeName += attribute.GetLocalName();
          builder.append("\u0020\u0020" + attributeName + "=\"" +
            attribute.GetValue().toString().replace("\n", "~~~~") + "\"\n");
        } else {
          builder.append("\u0020\u0020" + attribute.GetName().toString() +
            "=\"" +
            attribute.GetValue().toString().replace("\n", "~~~~") + "\"\n");
        }
      }
      boolean isTemplate = HtmlCommon.IsHtmlElement(this, "template");
      if (isTemplate) {
        builder.append("\u0020\u0020content\n");
      }
      for (var node : this.GetChildNodesInternal()) {
        String str = ((Node)node).ToDebugString();
        if (str == null) {
          continue;
        }
        String[] strarray = StringUtility.SplitAt(str, "\n");
        int len = strarray.length;
        if (len > 0 && ((strarray[len - 1]) == null || (strarray[len - 1]).length() == 0)) {
          --len; // ignore trailing empty String
        }
        for (int i = 0; i < len; ++i) {
          String el = strarray[i];
          // TODO: Separate template content from child nodes;
          // content is child nodes for convenience currently
          if (isTemplate) {
            {
              builder.append("\u0020\u0020");
            }
          }
          builder.append("\u0020\u0020");
          builder.append(el);
          builder.append("\n");
        }
      }
      return builder.toString();
    }
  }
