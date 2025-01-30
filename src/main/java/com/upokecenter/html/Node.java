package com.upokecenter.html;

import java.util.*;

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

  class Node implements INode {
    private List<INode> childNodes;
    private INode parentNode = null;
    private IDocument ownerDocument = null;

    private int valueNodeType;

    private String baseURI = null;

    public Node(int valueNodeType) {
      this.valueNodeType = valueNodeType;
      this.childNodes = new ArrayList<INode>();
    }

    public void AppendChild(INode node) {
      if (node == this) {
        throw new IllegalArgumentException();
      }
      ((Node)node).parentNode = this;
      ((Node)node).ownerDocument = (this instanceof IDocument) ? (IDocument)this :
        this.ownerDocument;
      this.childNodes.add(node);
    }

    private void FragmentSerializeInner(
      INode current,
      StringBuilder builder) {
      if (current.GetNodeType() == NodeType.ELEMENT_NODE) {
        IElement e = (IElement)current;
        String tagname = e.GetTagName();
        String namespaceURI = e.GetNamespaceURI();
        if (HtmlCommon.HTML_NAMESPACE.equals(namespaceURI) ||
          HtmlCommon.SVG_NAMESPACE.equals(namespaceURI) ||
          HtmlCommon.MATHML_NAMESPACE.equals(namespaceURI)) {
          tagname = e.GetLocalName();
        }
        builder.append('<');
        builder.append(tagname);
        for (IAttr attr : e.GetAttributes()) {
          namespaceURI = attr.GetNamespaceURI();
          builder.append(' ');
          if (namespaceURI == null || namespaceURI.length() == 0) {
            builder.append(attr.GetLocalName());
          } else if (namespaceURI.equals(HtmlCommon.XML_NAMESPACE)) {
            builder.append("xml:");
            builder.append(attr.GetLocalName());
          } else if (namespaceURI.equals(
            "http://www.w3.org/2000/xmlns/")) {
            if (!"xmlns".equals(attr.GetLocalName())) {
              builder.append("xmlns:");
            }
            builder.append(attr.GetLocalName());
          } else if (namespaceURI.equals(HtmlCommon.XLINK_NAMESPACE)) {
            builder.append("xlink:");
            builder.append(attr.GetLocalName());
          } else {
            builder.append(attr.GetName());
          }
          builder.append("=\"");
          String value = attr.GetValue();
          for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            if (c == '&') {
              builder.append("&amp;");
            } else if (c == 0xa0) {
              builder.append("&nbsp;");
            } else if (c == '"') {
              builder.append("&#x22;");
            } else {
              builder.append(c);
            }
          }
          builder.append('"');
        }
        builder.append('>');
        if (HtmlCommon.HTML_NAMESPACE.equals(namespaceURI)) {
          String localName = e.GetLocalName();
          if ("area".equals(localName) ||
            "base".equals(localName) ||
            "basefont".equals(localName) ||
            "bgsound".equals(localName) ||
            "br".equals(localName) ||
            "col".equals(localName) ||
            "embed".equals(localName) ||
            "frame".equals(localName) ||
            "hr".equals(localName) ||
            "img".equals(localName) ||
            "input".equals(localName) ||
            "keygen".equals(localName) ||
            "link".equals(localName) ||
            "menuitem".equals(localName) ||
            "meta".equals(localName) ||
            "param".equals(localName) ||
            "source".equals(localName) ||
            "track".equals(localName) ||
            "wbr".equals(localName)) {
            return;
          }
          if ("pre".equals(localName) ||
            "textarea".equals(localName) ||
            "listing".equals(localName)) {
            for (INode node : e.GetChildNodes()) {
              if (node.GetNodeType() == NodeType.TEXT_NODE) {
                String nodeData = ((IText)node).GetData();
                if (nodeData.length() > 0 && nodeData.charAt(0) == '\n') {
                  builder.append('\n');
                }
              }
            }
          }
        }
        // Recurse
        for (INode child : e.GetChildNodes()) {
          this.FragmentSerializeInner(child, builder);
        }
        builder.append("</");
        builder.append(tagname);
        builder.append(">");
      } else if (current.GetNodeType() == NodeType.TEXT_NODE) {
        INode parent = current.GetParentNode();
        if (parent instanceof IElement &&
          HtmlCommon.HTML_NAMESPACE.equals((
          (IElement)parent).GetNamespaceURI())) {
          String localName = ((IElement)parent).GetLocalName();
          if ("script".equals(localName) ||
            "style".equals(localName) ||
            "script".equals(localName) ||
            "xmp".equals(localName) ||
            "iframe".equals(localName) ||
            "noembed".equals(localName) ||
            "noframes".equals(localName) ||
            "plaintext".equals(localName)) {
            builder.append(((IText)current).GetData());
          } else {
            String value = ((IText)current).GetData();
            for (int i = 0; i < value.length(); ++i) {
              char c = value.charAt(i);
              if (c == '&') {
                builder.append("&amp;");
              } else if (c == 0xa0) {
                builder.append("&nbsp;");
              } else if (c == '<') {
                builder.append("&lt;");
              } else if (c == '>') {
                builder.append("&gt;");
              } else {
                builder.append(c);
              }
            }
          }
        }
      } else if (current.GetNodeType() == NodeType.COMMENT_NODE) {
        builder.append("<!--");
        builder.append(((IComment)current).GetData());
        builder.append("-->");
      } else if (current.GetNodeType() == NodeType.DOCUMENT_TYPE_NODE) {
        builder.append("<!DOCTYPE ");
        builder.append(((IDocumentType)current).GetName());
        builder.append(">");
      } else if (current.GetNodeType() ==
        NodeType.PROCESSING_INSTRUCTION_NODE) {
        builder.append("<?");
        builder.append(((IProcessingInstruction)current).GetTarget());
        builder.append(' ');
        builder.append(((IProcessingInstruction)current).GetData());
        builder.append(">"); // NOTE: may be erroneous
      }
    }

    public String GetBaseURI() {
      INode parent = this.GetParentNode();
      if (this.baseURI == null) {
        if (parent == null) {
          return "about:blank";
        } else {
          return parent.GetBaseURI();
        }
      } else {
        if (parent == null) {
          return this.baseURI;
        } else {
          URL ret = URL.Parse(this.baseURI, URL.Parse(parent.GetBaseURI()));
          return (ret == null) ? parent.GetBaseURI() : ret.toString();
        }
      }
    }

    public List<INode> GetChildNodes() {
      ArrayList<INode> cn = new ArrayList<INode>();
      for (int i = 0; i < this.childNodes.size(); ++i) {
        cn.add(this.childNodes.get(i));
      }
      return cn;
    }

    List<INode> GetChildNodesInternal() {
      return this.childNodes;
    }

    protected String GetInnerHtmlInternal() {
      StringBuilder builder = new StringBuilder();
      for (INode child : this.GetChildNodes()) {
        this.FragmentSerializeInner(child, builder);
      }
      return builder.toString();
    }

    public String GetLanguage() {
      INode parent = this.GetParentNode();
      if (parent == null) {
        parent = this.GetOwnerDocument();
        return (parent == null) ? "" : parent.GetLanguage();
      } else {
        return parent.GetLanguage();
      }
    }

    public String GetNodeName() {
      return "";
    }

    public int GetNodeType() {
      return this.valueNodeType;
    }

    public IDocument GetOwnerDocument() {
      return this.ownerDocument;
    }

    public INode GetParentNode() {
      return this.parentNode;
    }

    public String GetTextContent() {
      return null;
    }

    public void InsertBefore(Node child, Node sibling) {
      if (sibling == null) {
        this.AppendChild(child);
        return;
      }
      if (this.childNodes.size() == 0) {
        throw new IllegalStateException();
      }
      int childNodesSize = this.childNodes.size();
      for (int j = 0; j < childNodesSize; ++j) {
        if (this.childNodes.get(j).equals(sibling)) {
          child.parentNode = this;
          child.ownerDocument = (child instanceof IDocument) ? (IDocument)this :
            this.ownerDocument;
          this.childNodes.add(j, child);
          return;
        }
      }
      throw new IllegalArgumentException();
    }

    public void RemoveChild(INode node) {
      ((Node)node).parentNode = null;
      List<INode> cn = this.childNodes;
      cn.remove(node);
    }

    void SetBaseURI(String value) {
      INode parent = this.GetParentNode();
      if (parent == null) {
        this.baseURI = value;
      } else {
        String val = URL.Parse(value, URL.Parse(
          parent.GetBaseURI())).toString();
        this.baseURI = (val == null) ? parent.GetBaseURI() : val.toString();
      }
    }

    void SetOwnerDocument(IDocument document) {
      this.ownerDocument = document;
    }

     String ToDebugString() {
      return null;
    }

    @Override public String toString() {
      return this.GetNodeName();
    }
  }
