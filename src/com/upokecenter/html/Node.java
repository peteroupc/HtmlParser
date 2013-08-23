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

import java.util.ArrayList;
import java.util.List;

import com.upokecenter.util.URL;

class Node implements INode {
  private final List<Node> childNodes;
  private Node parentNode=null;
  private IDocument ownerDocument=null;

  int nodeType;


  private String baseURI=null;
  public Node(int nodeType){
    this.nodeType=nodeType;
    childNodes=new ArrayList<Node>();
  }


  public void appendChild(Node node){
    if(node==this)
      throw new IllegalArgumentException();
    node.parentNode=this;
    node.ownerDocument=(this instanceof IDocument) ? (IDocument)this : ownerDocument;
    childNodes.add(node);
  }

  private void fragmentSerializeInner(
      INode current, StringBuilder builder){
    if(current.getNodeType()==NodeType.ELEMENT_NODE){
      IElement e=((IElement)current);
      String tagname=e.getTagName();
      String namespaceURI=e.getNamespaceURI();
      if(HtmlParser.HTML_NAMESPACE.equals(namespaceURI) ||
          HtmlParser.SVG_NAMESPACE.equals(namespaceURI) ||
          HtmlParser.MATHML_NAMESPACE.equals(namespaceURI)){
        tagname=e.getLocalName();
      }
      builder.append('<');
      builder.append(tagname);
      for(IAttr attr : e.getAttributes()){
        namespaceURI=attr.getNamespaceURI();
        builder.append(' ');
        if(namespaceURI==null || namespaceURI.length()==0){
          builder.append(attr.getLocalName());
        } else if(namespaceURI.equals(HtmlParser.XML_NAMESPACE)){
          builder.append("xml:");
          builder.append(attr.getLocalName());
        } else if(namespaceURI.equals(
            "http://www.w3.org/2000/xmlns/")){
          if(!"xmlns".equals(attr.getLocalName())) {
            builder.append("xmlns:");
          }
          builder.append(attr.getLocalName());
        } else if(namespaceURI.equals(HtmlParser.XLINK_NAMESPACE)){
          builder.append("xlink:");
          builder.append(attr.getLocalName());
        } else {
          builder.append(attr.getName());
        }
        builder.append("=\"");
        String value=attr.getValue();
        for(int i=0;i<value.length();i++){
          char c=value.charAt(i);
          if(c=='&') {
            builder.append("&amp;");
          } else if(c==0xa0) {
            builder.append("&nbsp;");
          } else if(c=='"') {
            builder.append("&quot;");
          } else {
            builder.append(c);
          }
        }
        builder.append('"');
      }
      builder.append('>');
      if(HtmlParser.HTML_NAMESPACE.equals(namespaceURI)){
        String localName=e.getLocalName();
        if("area".equals(localName) ||
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
            "wbr".equals(localName))
          return;
        if("pre".equals(localName) ||
            "textarea".equals(localName) ||
            "listing".equals(localName)){
          for(INode node : e.getChildNodes()){
            if(node.getNodeType()==NodeType.TEXT_NODE &&
                ((IText)node).getData().length()>0 &&
                ((IText)node).getData().charAt(0)=='\n'){
              builder.append('\n');
            }
          }
        }
      }
      // Recurse
      for(INode child : e.getChildNodes()){
        fragmentSerializeInner(child,builder);
      }
      builder.append("</");
      builder.append(tagname);
      builder.append(">");
    } else if(current.getNodeType()==NodeType.TEXT_NODE){
      INode parent=current.getParentNode();
      if(parent instanceof IElement &&
          HtmlParser.HTML_NAMESPACE.equals(((IElement)parent).getNamespaceURI())){
        String localName=((IElement)parent).getLocalName();
        if("script".equals(localName) ||
            "style".equals(localName) ||
            "script".equals(localName) ||
            "xmp".equals(localName) ||
            "iframe".equals(localName) ||
            "noembed".equals(localName) ||
            "noframes".equals(localName) ||
            "plaintext".equals(localName)){
          builder.append(((IText)current).getData());
        } else {
          String value=((IText)current).getData();
          for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(c=='&') {
              builder.append("&amp;");
            } else if(c==0xa0) {
              builder.append("&nbsp;");
            } else if(c=='<') {
              builder.append("&lt;");
            } else if(c=='>') {
              builder.append("&gt;");
            } else {
              builder.append(c);
            }
          }
        }
      }
    } else if(current.getNodeType()==NodeType.COMMENT_NODE){
      builder.append("<!--");
      builder.append(((IComment)current).getData());
      builder.append("-->");
    } else if(current.getNodeType()==NodeType.DOCUMENT_TYPE_NODE){
      builder.append("<!DOCTYPE ");
      builder.append(((IDocumentType)current).getName());
      builder.append(">");
    } else if(current.getNodeType()==NodeType.PROCESSING_INSTRUCTION_NODE){
      builder.append("<?");
      builder.append(((IProcessingInstruction)current).getTarget());
      builder.append(' ');
      builder.append(((IProcessingInstruction)current).getData());
      builder.append(">");      // NOTE: may be erroneous
    }
  }

  @Override
  public  String getBaseURI() {
    INode parent=getParentNode();
    if(baseURI==null){
      if(parent==null)
        return "about:blank";
      else
        return parent.getBaseURI();
    } else {
      if(parent==null)
        return baseURI;
      else {
        URL ret=URL.parse(baseURI,URL.parse(parent.getBaseURI()));
        return (ret==null) ? parent.getBaseURI() : ret.toString();
      }
    }
  }

  @Override
  public List<INode> getChildNodes() {
    return new ArrayList<INode>(childNodes);
  }

   List<Node> getChildNodesInternal(){
    return childNodes;
  }

  protected  String getInnerHtmlInternal(){
    StringBuilder builder=new StringBuilder();
    for(INode child : getChildNodes()){
      fragmentSerializeInner(child,builder);
    }
    return builder.toString();
  }

  @Override public  String getLanguage(){
    INode parent=getParentNode();
    if(parent==null){
      parent=getOwnerDocument();
      if(parent==null)return "";
      return parent.getLanguage();
    } else
      return parent.getLanguage();
  }

  @Override
  public  String getNodeName() {
    return "";
  }

  @Override
  public int getNodeType(){
    return nodeType;
  }

  @Override
  public  IDocument getOwnerDocument(){
    return ownerDocument;
  }
  @Override
  public INode getParentNode() {
    return parentNode;
  }
  @Override
  public  String getTextContent(){
    return null;
  }
  public void insertBefore(Node child, Node sibling){
    if(sibling==null){
      appendChild(child);
      return;
    }
    if(childNodes.size()==0)
      throw new IllegalStateException();
    int childNodesSize=childNodes.size();
    for(int j=0;j<childNodesSize;j++){
      if(childNodes.get(j).equals(sibling)){
        child.parentNode=this;
        child.ownerDocument=(child instanceof IDocument) ? (IDocument)this : ownerDocument;
        childNodes.add(j,child);
        return;
      }
    }
    throw new IllegalArgumentException();
  }

  public void removeChild(Node node){
    node.parentNode=null;
    childNodes.remove(node);
  }

   void setBaseURI(String value){
    INode parent=getParentNode();
    if(parent==null){
      baseURI=value;
    } else {
      String val=URL.parse(value,URL.parse(parent.getBaseURI())).toString();
      baseURI=(val==null) ? parent.getBaseURI() : val.toString();
    }
  }

  void setOwnerDocument(IDocument document){
    ownerDocument=document;
  }

   String toDebugString() {
    return null;
  }


  @Override
  public String toString() {
    return getNodeName();
  }

}