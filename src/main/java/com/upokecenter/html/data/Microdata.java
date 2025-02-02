package com.upokecenter.html.data;

import java.util.*;
import com.upokecenter.html.*;
import com.upokecenter.util.*;
import com.upokecenter.util.*;
import com.upokecenter.cbor.*;

  /**
   * Not documented yet.
   */
  public final class Microdata {
    private static class ElementAndIndex {
      final int getIndex() { return propVarindex; }
final void setIndex(int value) { propVarindex = value; }
private int propVarindex;

      final IElement getElement() { return propVarelement; }
final void setElement(IElement value) { propVarelement = value; }
private IElement propVarelement;
    }

    private static final class SortInTreeOrderComparer implements Comparator<ElementAndIndex> {
      public int compare(ElementAndIndex arg0, ElementAndIndex arg1) {
        return (arg0.getIndex() == arg1.getIndex()) ? 0 : ((arg0.getIndex() < arg1.getIndex()) ?
          -1 : 1);
      }
    }

    private static int GetElementIndex(
      INode root,
      IElement e,
      int startIndex) {
      int[] runningIndex = new int[] { startIndex };
      return GetElementIndex(root, e, runningIndex);
    }

    private static int GetElementIndex(
      INode root,
      IElement e,
      int[] runningIndex) {
      int valueIndex = runningIndex[0];
      if (root.equals(e)) {
        return valueIndex;
      }
      ++valueIndex;
      for (INode child : root.GetChildNodes()) {
        int idx = GetElementIndex(child, e, runningIndex);
        if (idx >= 0) {
          return idx;
        }
      }
      runningIndex[0] = valueIndex;
      return -1;
    }

    private static String GetHref(IElement node) {
      String name = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(node.GetLocalName());
      String href = "";
      if ("a".equals(name) ||
        "link".equals(name) ||
        "area".equals(name)) {
        href = node.GetAttribute("href");
      } else if ("Object".equals(name)) {
        href = node.GetAttribute("Data");
      } else if ("img".equals(name) ||
        "source".equals(name) ||
        "track".equals(name) ||
        "iframe".equals(name) ||
        "audio".equals(name) ||
        "video".equals(name) ||
        "embed".equals(name)) {
        href = node.GetAttribute("src");
      } else {
        return null;
      }
      if (href == null || href.length() == 0) {
        return "";
      }
      href = HtmlCommon.ResolveURL(node, href, null);
      return (href == null || href.length() == 0) ? "" : href;
    }

    /**
     * Not documented yet.
     * @param document A document object.
     * @return The return value is not documented yet.
     * @throws NullPointerException The parameter {@code document} is null.
     */
    public static CBORObject GetMicrodataJSON(IDocument document) {
      if (document == null) {
        throw new NullPointerException("document");
      }
      CBORObject result = CBORObject.NewMap();
      CBORObject items = CBORObject.NewArray();
      List<IElement> tagNameEl = document.GetElementsByTagName("*");
      for (IElement node : tagNameEl) {
        if (node.GetAttribute("itemscope") != null &&
          node.GetAttribute("itemprop") == null) {
          List<IElement> memory = new ArrayList<IElement>();
          CBORObject mdobject = GetMicrodataObject(node, memory);
          if (mdobject != null) {
            items.Add(mdobject);
          }
        }
      }
      result.Add("items", items);
      return result;
    }

    private static CBORObject GetMicrodataObject(
      IElement item,
      List<IElement> memory) {
      String[] itemtypes = StringUtility.SplitAtSpTabCrLfFf(item.GetAttribute(
        "itemtype"));
      CBORObject result = CBORObject.NewMap();
      memory.add(item);
      if (itemtypes.length == 0) {
        return null;
      }
      if (itemtypes.length > 0) {
        CBORObject array = CBORObject.NewArray();
        for (String itemtype : itemtypes) {
          array.Add(itemtype);
        }
        result.Add("type", array);
      }
      String globalid = item.GetAttribute("itemid");
      if (globalid != null) {
        globalid = HtmlCommon.ResolveURL(
            item,
            globalid,
            item.GetBaseURI());
        result.Add("id", globalid);
      }
      CBORObject properties = CBORObject.NewMap();
      List<IElement> mdprop = GetMicrodataProperties(item);
      for (IElement valueElement : mdprop) {
        String[] names = StringUtility.SplitAtSpTabCrLfFf(
            valueElement.GetAttribute(
              "itemprop"));
        Object obj = null;
        if (valueElement.GetAttribute("itemscope") != null) {
          obj = memory.contains(valueElement) ? (Object)"ERROR" :
            (Object)GetMicrodataObject(valueElement, new ArrayList<IElement>(memory));
          obj = (obj == null) ? (((Object)"ERROR")) : obj;
        } else {
          obj = GetPropertyValue(valueElement);
        }
        for (String name : names) {
          if (properties.ContainsKey(name)) {
            properties.get(name).Add(obj);
          } else {
            CBORObject arr = CBORObject.NewArray();
            arr.Add(obj);
            properties.Add(name, arr);
          }
        }
      }
      result.Add("properties", properties);
      return result;
    }

    private static List<IElement> GetMicrodataProperties(IElement root) {
      List<IElement> results = new ArrayList<IElement>();
      List<IElement> memory = new ArrayList<IElement>();
      List<IElement> pending = new ArrayList<IElement>();
      memory.add(root);
      IDocument document = root.GetOwnerDocument();
      for (INode child : root.GetChildNodes()) {
        if (child instanceof IElement) {
          pending.add((IElement)child);
        }
      }
      String[] itemref = StringUtility.SplitAtSpTabCrLfFf(root.GetAttribute(
        "itemref"));
      for (String item : itemref) {
        IElement valueElement = document.GetElementById(item);
        if (valueElement != null) {
          pending.add(valueElement);
        }
      }
      while (pending.size() > 0) {
        IElement current = pending.get(0);
        pending.remove(0);
        if (memory.contains(current)) {
          continue;
        }
        memory.add(current);
        if (current.GetAttribute("itemscope") == null) {
          for (INode child : current.GetChildNodes()) {
            if (child instanceof IElement) {
              pending.add((IElement)child);
            }
          }
        }
        if (!StringUtility.IsNullOrSpaces(current.GetAttribute("itemprop"))) {
          results.add(current);
        }
      }
      return SortInTreeOrder(results, document);
    }

    private static String GetPropertyValue(IElement e) {
      if (IsHtmlElement(e)) {
        if (IsHtmlElement(e, "meta")) {
          String attr = e.GetAttribute("content");
          return (attr == null) ? "" : attr;
        }
        String href = GetHref(e);
        if (href != null) {
          return href;
        }
        if (IsHtmlElement(e, "Data")) {
          String attr = e.GetAttribute("value");
          return (attr == null) ? "" : attr;
        }
        if (IsHtmlElement(e, "time")) {
          String attr = e.GetAttribute("datetime");
          if (attr != null) {
            return attr;
          }
        }
      }
      return e.GetTextContent();
    }

    private static boolean IsHtmlElement(IElement valueElement) {
      return "http://www.w3.org/1999/xhtml"
        .equals(valueElement.GetNamespaceURI());
    }

    private static boolean IsHtmlElement(IElement e, String name) {
      return e.GetLocalName().equals(name) &&
        IsHtmlElement(e);
    }

    private static List<IElement> SortInTreeOrder(
      List<IElement> elements,
      INode root) {
      if (elements == null || elements.size() < 2) {
        return elements;
      }
      ArrayList<ElementAndIndex> elems = new ArrayList<ElementAndIndex>();
      for (IElement valueElement : elements) {
        ElementAndIndex el = new ElementAndIndex();
        el.setElement(valueElement);
        el.setIndex(GetElementIndex(root, valueElement, 0));
        elems.add(el);
      }
      java.util.Collections.sort(elems, new SortInTreeOrderComparer());
      List<IElement> ret = new ArrayList<IElement>();
      for (ElementAndIndex el : elems) {
        ret.add(el.getElement());
      }
      return ret;
    }

    private Microdata() {
    }
  }
