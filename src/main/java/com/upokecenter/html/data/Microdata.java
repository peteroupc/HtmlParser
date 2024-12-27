package com.upokecenter.util;

import java.util.*;
using Com.Upokecenter.Html;
using Com.Upokecenter.util;
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
-1 :
            1);
      }
    }

    private static int GetElementIndex(
      INode root,
      IElement e,
      int startIndex) {
      int[] runningIndex = new int[] { startIndex };
      return GetElementIndex (root, e, runningIndex);
    }

    private static int GetElementIndex(
      INode root,
      IElement e,
      int[] runningIndex) {
      int valueIndex = runningIndex[0];
      if (root.equals (e)) {
        return valueIndex;
      }
      ++valueIndex;
      for (Object child : root.getChildNodes()) {
        int idx = GetElementIndex (child, e, runningIndex);
        if (idx >= 0) {
          return idx;
        }
      }
      runningIndex[0] = valueIndex;
      return -1;
    }

    private static String GetHref(IElement node) {
      String name = com.upokecenter.util.DataUtilities.ToLowerCaseAscii (node.getLocalName());
      String href = "";
      if ("a".equals(name) ||
        "link".equals(name) ||
        "area".equals(name)) {
        href = node.getAttribute ("href");
      } else if ("Object".equals(name)) {
        href = node.getAttribute ("Data");
      } else if ("img".equals(name) ||
        "source".equals(name) ||
        "track".equals(name) ||
        "iframe".equals(name) ||
        "audio".equals(name) ||
        "video".equals(name) ||
        "embed".equals(name)) {
        href = node.getAttribute ("src");
      } else {
        return null;
      }
      if (href == null || href.length() == 0) {
        return "";
      }
      href = HtmlCommon.resolveURL (node, href, null);
      return (href == null || href.length() == 0) ? "" : href;
    }

    /**
     * Not documented yet.
     * @param document The parameter {@code document} is
     * a.getUpokecenter().getHtml().IDocument object.
     * @return The return value is not documented yet.
     */
    public static PeterO.Cbor.CBORObject GetMicrodataJSON (IDocument document) {
      if (document == null) {
        throw new NullPointerException("document");
      }
      PeterO.Cbor.CBORObject result = PeterO.Cbor.CBORObject.NewMap();
      CBORObject items = CBORObject.NewArray();
      for (Object node : document.getElementsByTagName ("*")) {
        if (node.getAttribute ("itemscope") != null &&
          node.getAttribute ("itemprop") == null) {
          List<IElement> memory = new ArrayList<IElement>();
          items.Add (GetMicrodataObject (node, memory));
        }
      }
      result.Add ("items", items);
      return result;
    }

    private static PeterO.Cbor.CBORObject GetMicrodataObject(
      IElement item,
      List<IElement> memory) {
      String[] itemtypes = StringUtility.SplitAtSpTabCrLfFf (item.getAttribute(
            "itemtype"));
      PeterO.Cbor.CBORObject result = PeterO.Cbor.CBORObject.NewMap();
      memory.add(item);
      if (itemtypes.length > 0) {
        CBORObject array = CBORObject.NewArray();
        for (Object itemtype : itemtypes) {
          array.Add (itemtype);
        }
        result.Add ("type", array);
      }
      String globalid = item.getAttribute ("itemid");
      if (globalid != null) {
        globalid = HtmlCommon.resolveURL(
            item,
            globalid,
            item.getBaseURI());
        result.Add ("id", globalid);
      }
      PeterO.Cbor.CBORObject properties = PeterO.Cbor.CBORObject.NewMap();
      for (Object valueElement : GetMicrodataProperties (item)) {
        String[] names = StringUtility.SplitAtSpTabCrLfFf(
            valueElement.getAttribute(
              "itemprop"));
        Object obj = null;
        if (valueElement.getAttribute ("itemscope") != null) {
          obj = memory.contains(valueElement) ? (Object)"ERROR" :
            (Object)GetMicrodataObject (valueElement, Arrays.asList(memory));
          } else {
          obj = GetPropertyValue (valueElement);
        }
        for (Object name : names) {
          if (properties.ContainsKey (name)) {
            properties.get(name).Add (obj);
          } else {
            CBORObject arr = CBORObject.NewArray();
            arr.Add (obj);
            properties.Add (name, arr);
          }
        }
      }
      result.Add ("properties", properties);
      return result;
    }

    private static List<IElement> GetMicrodataProperties(IElement root) {
      List<IElement> results = new ArrayList<IElement>();
      List<IElement> memory = new ArrayList<IElement>();
      List<IElement> pending = new ArrayList<IElement>();
      memory.add(root);
      IDocument document = root.getOwnerDocument();
      for (Object child : root.getChildNodes()) {
        if (child instanceof IElement) {
          pending.add((IElement)child);
        }
      }
      String[] itemref = StringUtility.SplitAtSpTabCrLfFf (root.getAttribute(
            "itemref"));
      for (Object item : itemref) {
        IElement valueElement = document.getElementById (item);
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
        if (current.getAttribute ("itemscope") == null) {
          for (Object child : current.getChildNodes()) {
            if (child instanceof IElement) {
              pending.add((IElement)child);
            }
          }
        }
        if (!StringUtility.isNullOrSpaces (current.getAttribute ("itemprop"))) {
          results.add(current);
        }
      }
      return SortInTreeOrder (results, document);
    }

    private static String GetPropertyValue(IElement e) {
      if (IsHtmlElement (e)) {
        if (IsHtmlElement (e, "meta")) {
          String attr = e.getAttribute ("content");
          return (attr == null) ? "" : attr;
        }
        String href = GetHref (e);
        if (href != null) {
          return href;
        }
        if (IsHtmlElement (e, "Data")) {
          String attr = e.getAttribute ("value");
          return (attr == null) ? "" : attr;
        }
        if (IsHtmlElement (e, "time")) {
          String attr = e.getAttribute ("datetime");
          if (attr != null) {
            return attr;
          }
        }
      }
      return e.getTextContent();
    }

    private static boolean IsHtmlElement(IElement valueElement) {
      return "http://www.w3.org/1999/xhtml"
        .equals (valueElement.getNamespaceURI());
    }

    private static boolean IsHtmlElement(IElement e, String name) {
      return e.getLocalName().equals(name) &&
        IsHtmlElement (e);
    }

    private static List<IElement> SortInTreeOrder(
      List<IElement> elements,
      INode root) {
      if (elements == null || elements.size() < 2) {
        return elements;
      }
      ArrayList<ElementAndIndex> elems = new ArrayList<ElementAndIndex>();
      for (Object valueElement : elements) {
        ElementAndIndex el = new ElementAndIndex();
        el.setElement(valueElement);
        el.setIndex(GetElementIndex (root, valueElement, 0));
        elems.add(el);
      }
      elems.Sort (new SortInTreeOrderComparer());
      List<IElement> ret = new ArrayList<IElement>();
      for (Object el : elems) {
        ret.add(el.getElement());
      }
      return ret;
    }

    private Microdata() {
    }
  }
