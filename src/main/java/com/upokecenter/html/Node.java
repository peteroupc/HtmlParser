package com.upokecenter.util;

import java.util.*;

using Com.Upokecenter.util;
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

    public void appendChild(INode node) {
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
      if (current.getNodeType() == NodeType.ELEMENT_NODE) {
        IElement e = (IElement)current;
        String tagname = e.getTagName();
        String namespaceURI = e.getNamespaceURI();
        if (HtmlCommon.HTML_NAMESPACE.equals(namespaceURI) ||
          HtmlCommon.SVG_NAMESPACE.equals(namespaceURI) ||
          HtmlCommon.MATHML_NAMESPACE.equals(namespaceURI)) {
          tagname = e.getLocalName();
        }
        builder.append ('<');
        builder.append (tagname);
        for (Object attr : e.getAttributes()) {
          namespaceURI = attr.getNamespaceURI();
          builder.append (' ');
          if (namespaceURI == null || namespaceURI.length() == 0) {
            builder.append (attr.getLocalName());
          } else if (namespaceURI.equals(HtmlCommon.XML_NAMESPACE)) {
            builder.append ("xml:");
            builder.append (attr.getLocalName());
          } else if (namespaceURI.equals(
            "http://www.w3.org/2000/xmlns/")) {
            if (!"xmlns".equals(attr.getLocalName())) {
              builder.append ("xmlns:");
            }
            builder.append (attr.getLocalName());
          } else if (namespaceURI.equals(HtmlCommon.XLINK_NAMESPACE)) {
            builder.append ("xlink:");
            builder.append (attr.getLocalName());
          } else {
            builder.append (attr.getName());
          }
          builder.append ("=\"");
          String value = attr.getValue();
          for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            if (c == '&') {
              builder.append ("&amp;");
            } else if (c == 0xa0) {
              builder.append ("&nbsp;");
            } else if (c == '"') {
              builder.append ("&#x22;");
            } else {
              builder.append (c);
            }
          }
          builder.append ('"');
        }
        builder.append ('>');
        if (HtmlCommon.HTML_NAMESPACE.equals(namespaceURI)) {
          String localName = e.getLocalName();
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
            for (Object node : e.getChildNodes()) {
              if (node.getNodeType() == NodeType.TEXT_NODE &&
                ((IText)node).getData().length > 0 &&
                ((IText)node).getData()[0] == '\n') {
                builder.append ('\n');
              }
            }
          }
        }
        // Recurse
        for (Object child : e.getChildNodes()) {
          this.FragmentSerializeInner (child, builder);
        }
        builder.append ("</");
        builder.append (tagname);
        builder.append (">");
      } else if (current.getNodeType() == NodeType.TEXT_NODE) {
        INode parent = current.getParentNode();
        if (parent instanceof IElement &&
          HtmlCommon.HTML_NAMESPACE.equals(((IElement)parent).getNamespaceURI())) {
          String localName = ((IElement)parent).getLocalName();
          if ("script".equals(localName) ||
            "style".equals(localName) ||
            "script".equals(localName) ||
            "xmp".equals(localName) ||
            "iframe".equals(localName) ||
            "noembed".equals(localName) ||
            "noframes".equals(localName) ||
            "plaintext".equals(localName)) {
            builder.append (((IText)current).getData());
          } else {
            String value = ((IText)current).getData();
            for (int i = 0; i < value.length(); ++i) {
              char c = value.charAt(i);
              if (c == '&') {
                builder.append ("&amp;");
              } else if (c == 0xa0) {
                builder.append ("&nbsp;");
              } else if (c == '<') {
                builder.append ("&lt;");
              } else if (c == '>') {
                builder.append ("&gt;");
              } else {
                builder.append (c);
              }
            }
          }
        }
      } else if (current.getNodeType() == NodeType.COMMENT_NODE) {
        builder.append ("<!--");
        builder.append (((IComment)current).getData());
        builder.append ("-->");
      } else if (current.getNodeType() == NodeType.DOCUMENT_TYPE_NODE) {
        builder.append ("<!DOCTYPE ");
        builder.append (((IDocumentType)current).getName());
        builder.append (">");
      } else if (current.getNodeType() ==
NodeType.PROCESSING_INSTRUCTION_NODE) {
        builder.append ("<?");
        builder.append (((IProcessingInstruction)current).getTarget());
        builder.append (' ');
        builder.append (((IProcessingInstruction)current).getData());
        builder.append (">"); // NOTE: may be erroneous
      }
    }

    public String getBaseURI() {
      INode parent = this.getParentNode();
      if (this.baseURI == null) {
        if (parent == null) {
          return "about:blank";
        } else {
          return parent.getBaseURI();
        }
      } else {
        if (parent == null) {
          return this.baseURI;
        } else {
          URL ret = URL.parse (this.baseURI, URL.parse (parent.getBaseURI()));
          return (ret == null) ? parent.getBaseURI() : ret.toString();
        }
      }
    }

    public List<INode> getChildNodes() {
      return Arrays.asList(this.childNodes);
    }

    List<INode> GetChildNodesInternal() {
      return this.childNodes;
    }

    protected String GetInnerHtmlInternal() {
      StringBuilder builder = new StringBuilder();
      for (Object child : this.getChildNodes()) {
        this.FragmentSerializeInner (child, builder);
      }
      return builder.toString();
    }

    public String getLanguage() {
      INode parent = this.getParentNode();
      if (parent == null) {
        parent = this.getOwnerDocument();
        return (parent == null) ? "" : parent.getLanguage();
      } else {
        return parent.getLanguage();
      }
    }

    public String getNodeName() {
      return "";
    }

    public int getNodeType() {
      return this.valueNodeType;
    }

    public IDocument getOwnerDocument() {
      return this.ownerDocument;
    }

    public INode getParentNode() {
      return this.parentNode;
    }

    public String getTextContent() {
      return null;
    }

    public void InsertBefore(Node child, Node sibling) {
      if (sibling == null) {
        this.appendChild (child);
        return;
      }
      if (this.childNodes.size() == 0) {
        throw new IllegalStateException();
      }
      int childNodesSize = this.childNodes.size();
      for (int j = 0; j < childNodesSize; ++j) {
        if (this.childNodes.get(j).equals (sibling)) {
          child.parentNode = this;
          child.ownerDocument = (child instanceof IDocument) ? (IDocument)this :
            this.ownerDocument;
          this.childNodes.add(j, child);
          return;
        }
      }
      throw new IllegalArgumentException();
    }

    public void removeChild(INode node) {
      ((Node)node).parentNode = null;
      this.childNodes.Remove (node);
    }

    void SetBaseURI(String value) {
      INode parent = this.getParentNode();
      if (parent == null) {
        this.baseURI = value;
      } else {
        String val = URL.parse (value, URL.parse
(parent.getBaseURI())).toString();
        this.baseURI = (val == null) ? parent.getBaseURI() : val.toString();
      }
    }

    void SetOwnerDocument(IDocument document) {
      this.ownerDocument = document;
    }

    internal virtual String ToDebugString() {
      return null;
    }

    @Override public String toString() {
      return this.getNodeName();
    }
  }
