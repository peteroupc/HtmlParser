package com.upokecenter.html.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.upokecenter.html.HtmlDocument;
import com.upokecenter.html.IDocument;
import com.upokecenter.html.IElement;
import com.upokecenter.html.INode;
import com.upokecenter.json.JSONArray;
import com.upokecenter.json.JSONObject;
import com.upokecenter.util.StringUtility;

public final class Microdata {
  private static class ElementAndIndex {
    public int index;
    public IElement element;
  }

  private static final class SortInTreeOrderComparer implements
  Comparator<ElementAndIndex> {
    @Override
    public int compare(ElementAndIndex arg0, ElementAndIndex arg1) {
      if(arg0.index==arg1.index)return 0;
      return (arg0.index<arg1.index) ? -1 : 1;
    }
  }

  private static int getElementIndex(
      INode root, IElement e, int startIndex){
    int[] runningIndex=new int[]{startIndex};
    return getElementIndex(root,e,runningIndex);
  }
  private static int getElementIndex(
      INode root, IElement e, int[] runningIndex){
    int index=runningIndex[0];
    if(root.equals(e))
      return index;
    index++;
    for(INode child : root.getChildNodes()){
      int idx=getElementIndex(child,e,runningIndex);
      if(idx>=0)
        return idx;
    }
    runningIndex[0]=index;
    return -1;
  }

  private static String getHref(IElement node){
    String name=StringUtility.toLowerCaseAscii(node.getLocalName());
    String href="";
    if("a".equals(name) || "link".equals(name) || "area".equals(name)){
      href=node.getAttribute("href");
    } else if("object".equals(name)){
      href=node.getAttribute("data");
    } else if("img".equals(name) || "source".equals(name) ||
        "track".equals(name) ||
        "iframe".equals(name) ||
        "audio".equals(name) ||
        "video".equals(name) ||
        "embed".equals(name)){
      href=node.getAttribute("src");
    } else
      return null;
    if(href==null || href.length()==0)
      return "";
    href=HtmlDocument.resolveURL(node,href,null);
    if(href==null || href.length()==0)
      return "";
    return href;
  }

  public static JSONObject getMicrodataJSON(IDocument document){
    if((document)==null)throw new NullPointerException("document");
    JSONObject result=new JSONObject();
    JSONArray items=new JSONArray();
    for(IElement node : document.getElementsByTagName("*")){
      if(node.getAttribute("itemscope")!=null &&
          node.getAttribute("itemprop")==null){
        List<IElement> memory=new ArrayList<IElement>();
        items.put(getMicrodataObject(node,memory));
      }
    }
    result.put("items", items);
    return result;
  }

  private static JSONObject getMicrodataObject(IElement item, List<IElement> memory){
    String[] itemtypes=StringUtility.splitAtSpaces(item.getAttribute("itemtype"));
    JSONObject result=new JSONObject();
    memory.add(item);
    if(itemtypes.length>0){
      JSONArray array=new JSONArray();
      for(String itemtype : itemtypes){
        array.put(itemtype);
      }
      result.put("type",array);
    }
    String globalid=item.getAttribute("itemid");
    if(globalid!=null){
      globalid=HtmlDocument.resolveURL(item, globalid,
          item.getBaseURI());
      result.put("id", globalid);
    }
    JSONObject properties=new JSONObject();
    for(IElement element : getMicrodataProperties(item)){
      String[] names=StringUtility.splitAtSpaces(element.getAttribute("itemprop"));
      Object obj=null;
      if(element.getAttribute("itemscope")!=null){
        if(memory.contains(element)){
          obj="ERROR";
        } else {
          obj=getMicrodataObject(element,new ArrayList<IElement>(memory));
        }
      } else {
        obj=getPropertyValue(element);
      }
      for(String name : names){
        if(properties.has(name)){
          properties.getJSONArray(name).put(obj);
        } else {
          JSONArray arr=new JSONArray();
          arr.put(obj);
          properties.put(name, arr);
        }
      }
    }
    result.put("properties",properties);
    return result;
  }

  private static List<IElement> getMicrodataProperties(IElement root){
    List<IElement> results=new ArrayList<IElement>();
    List<IElement> memory=new ArrayList<IElement>();
    List<IElement> pending=new ArrayList<IElement>();
    memory.add(root);
    IDocument document=root.getOwnerDocument();
    for(INode child : root.getChildNodes()){
      if(child instanceof IElement){
        pending.add((IElement)child);
      }
    }
    String[] itemref=StringUtility.splitAtSpaces(root.getAttribute("itemref"));
    for(String item : itemref){
      IElement element=document.getElementById(item);
      if(element!=null){
        pending.add(element);
      }
    }
    while(pending.size()>0){
      IElement current=pending.get(0);
      pending.remove(0);
      if(memory.contains(current)){
        continue;
      }
      memory.add(current);
      if(current.getAttribute("itemscope")==null){
        for(INode child : current.getChildNodes()){
          if(child instanceof IElement){
            pending.add((IElement)child);
          }
        }
      }
      if(!StringUtility.isNullOrSpaces(current.getAttribute("itemprop"))){
        results.add(current);
      }
    }
    return sortInTreeOrder(results,document);
  }

  private static String getPropertyValue(IElement e){
    if(isHtmlElement(e)){
      if(isHtmlElement(e,"meta")){
        String attr=e.getAttribute("content");
        return (attr==null) ? "" : attr;
      }
      String href=getHref(e);
      if(href!=null)return href;
      if(isHtmlElement(e,"data")){
        String attr=e.getAttribute("value");
        return (attr==null) ? "" : attr;
      }
      if(isHtmlElement(e,"time")){
        String attr=e.getAttribute("datetime");
        if(attr!=null)
          return attr;
      }
    }
    return e.getTextContent();
  }

  private static boolean isHtmlElement(IElement element){
    return "http://www.w3.org/1999/xhtml".equals(element.getNamespaceURI());
  }

  private static boolean isHtmlElement(IElement e, String name){
    return e.getLocalName().equals(name) && isHtmlElement(e);
  }

  private static List<IElement> sortInTreeOrder(
      List<IElement> elements, INode root){
    if(elements==null || elements.size()<2)
      return elements;
    ArrayList<ElementAndIndex> elems=new ArrayList<ElementAndIndex>();
    for(IElement element : elements){
      ElementAndIndex el=new ElementAndIndex();
      el.element=element;
      el.index=getElementIndex(root,element,0);
      elems.add(el);
    }
    Collections.sort(elems,new SortInTreeOrderComparer());
    List<IElement> ret=new ArrayList<IElement>();
    for(ElementAndIndex el : elems){
      ret.add(el.element);
    }
    return ret;
  }

  private Microdata(){}

}
