package com.upokecenter.html.data;

import java.util.*;

import com.upokecenter.html.*;
import com.upokecenter.util.*;
import com.upokecenter.util.*;
import com.upokecenter.cbor.*;

  /**
   * Not documented yet.
   */
  public final class Microformats {
    private static Map<String, String[]> complexLegacyMap = new
    HashMap<String, String[]>();

    static {
      String[] strarr;
      complexLegacyMap.put("adr", new String[] { "p-adr", "h-adr" });
      strarr = new String[] {
        "p-affiliation",
        "h-card",
      };
      complexLegacyMap.put(
        "affiliation",
        strarr);
      complexLegacyMap.put("author", new String[] { "p-author", "h-card" });
      complexLegacyMap.put("contact", new String[] { "p-contact", "h-card" });
      strarr = new String[] {
        "p-education",
        "h-event",
      };
      complexLegacyMap.put(
        "education",
        strarr);
      strarr = new String[] {
        "p-experience",
        "h-event",
      };
      complexLegacyMap.put(
        "experience",
        strarr);
      complexLegacyMap.put("fn", new String[] {
        "p-item", "h-item",
        "p-name",
      });
      complexLegacyMap.put("geo", new String[] { "p-geo", "h-geo" });
      strarr = new String[] {
        "p-location",
        "h-card",
        "h-adr",
      };

      complexLegacyMap.put(
        "location",
        strarr);
      strarr = new String[] {
        "p-item",
        "h-item",
        "u-photo",
      };
      complexLegacyMap.put(
        "photo",
        strarr);
      complexLegacyMap.put("review", new String[] { "p-review", "h-review" });
      complexLegacyMap.put("reviewer", new String[] {
        "p-reviewer",
        "h-card",
      });
      complexLegacyMap.put("url", new String[] {
        "p-item", "h-item",
        "u-url",
      });
    }

    private static final String[] ValueLegacyLabels = new String[] {
      "additional-name", "p-additional-name", "adr", "h-adr", "bday",
      "dt-bday", "best", "p-best", "brand", "p-brand", "category",
      "p-category", "count", "p-count", "country-name", "p-country-name",
      "description", "e-description", "dtend", "dt-end", "dtreviewed",
      "dt-dtreviewed", "dtstart", "dt-start", "duration",
      "dt-duration", "e-entry-summary", "e-summary", "email", "u-email",
      "entry-content", "e-content", "entry-summary", "p-summary",
      "entry-title",
      "p-name", "extended-address", "p-extended-address", "family-name",
      "p-family-name", "fn", "p-name", "geo", "h-geo", "given-name",
      "p-given-name", "hentry", "h-entry", "honorific-prefix",
      "p-honorific-prefix", "honorific-suffix", "p-honorific-suffix",
      "hproduct", "h-product", "hrecipe", "h-recipe", "hresume",
      "h-resume", "hreview", "h-review", "hreview-aggregate",
      "h-review-aggregate", "identifier", "u-identifier", "ingredient",
      "p-ingredient",
      "instructions", "e-instructions", "key", "u-key", "label",
      "p-label", "latitude", "p-latitude", "locality", "p-locality",
      "logo", "u-logo", "longitude", "p-longitude", "nickname",
      "p-nickname", "note", "p-note", "nutrition", "p-nutrition", "org",
      "p-org", "organization-name", "p-organization-name",
      "organization-unit", "p-organization-unit", "p-entry-summary",
      "p-summary", "p-entry-title", "p-name", "photo", "u-photo",
      "post-office-box", "p-post-office-box",
      "postal-code", "p-postal-code", "price", "p-price", "published",
      "dt-published", "rating", "p-rating", "region", "p-region", "rev",
      "dt-rev", "skill", "p-skill", "street-address",
      "p-street-address", "summary", "p-name", "tel", "p-tel", "tz",
      "p-tz", "uid", "u-uid", "updated", "dt-updated", "url", "p-url",
      "vcard", "h-card", "vevent", "h-event", "votes", "p-votes",
      "worst", "p-worst", "yield", "p-yield",
    };

    private static final Map<String, String>
    ValueLegacyLabelsMap = CreateLegacyLabelsMap();

    private static final int[] ValueNormalDays = {
      0, 31, 28, 31, 30, 31, 30, 31, 31, 30,
      31, 30, 31,
    };

    private static final int[] ValueLeapDays = {
      0, 31, 29, 31, 30, 31, 30, 31, 31, 30,
      31, 30, 31,
    };

    private static void AccumulateValue(
      CBORObject obj,
      String key,
      Object value) {
      CBORObject arr = null;
      if (obj.ContainsKey(key)) {
        arr = obj.get(key);
      } else {
        arr = CBORObject.NewArray();
        obj.Add(key, arr);
      }
      arr.Add(value);
    }

    private static void Append2d(StringBuilder builder, int value) {
      value = Math.abs(value);
      builder.append((char)('0' + ((value / 10) % 10)));
      builder.append((char)('0' + (value % 10)));
    }

    private static void Append3d(StringBuilder builder, int value) {
      value = Math.abs(value);
      builder.append((char)('0' + ((value / 100) % 10)));
      builder.append((char)('0' + ((value / 10) % 10)));
      builder.append((char)('0' + (value % 10)));
    }

    private static void Append4d(StringBuilder builder, int value) {
      value = Math.abs(value);
      builder.append((char)('0' + ((value / 1000) % 10)));
      builder.append((char)('0' + ((value / 100) % 10)));
      builder.append((char)('0' + ((value / 10) % 10)));
      builder.append((char)('0' + (value % 10)));
    }

    private static void CopyComponents(
      int[] src,
      int[] components,
      boolean useDate,
      boolean useTime,
      boolean useTimezone) {
      if (useDate) {
        if (src[0] != Integer.MIN_VALUE) {
          components[0] = src[0];
        }
        if (src[1] != Integer.MIN_VALUE) {
          components[1] = src[1];
        }
        if (src[2] != Integer.MIN_VALUE) {
          components[2] = src[2];
        }
      }
      if (useTime) {
        if (src[3] != Integer.MIN_VALUE) {
          components[3] = src[3];
        }
        if (src[4] != Integer.MIN_VALUE) {
          components[4] = src[4];
        }
        if (src[5] != Integer.MIN_VALUE) {
          components[5] = src[5];
        }
      }
      if (useTimezone) {
        if (src[6] != Integer.MIN_VALUE) {
          components[6] = src[6];
        }
        if (src[7] != Integer.MIN_VALUE) {
          components[7] = src[7];
        }
      }
    }

    private static CBORObject CopyJson(CBORObject obj) {
      return CBORObject.FromJSONString(obj.ToJSONString());
    }

    private static Map<String, String> CreateLegacyLabelsMap() {
      Map<String, String> map = new HashMap<String, String>();
      for (int i = 0; i < ValueLegacyLabels.length; i += 2) {
        map.put(ValueLegacyLabels[i], ValueLegacyLabels[i + 1]);
      }
      return map;
    }

    private static String ElementName(IElement element) {
      return com.upokecenter.util.DataUtilities.ToLowerCaseAscii(element.GetLocalName());
    }

    private static List<IElement> GetChildElements(INode e) {
      List<IElement> elements = new ArrayList<IElement>();
      for (INode child : e.GetChildNodes()) {
        if (child instanceof IElement) {
          elements.add((IElement)child);
        }
      }
      return elements;
    }

    private static String[] GetClassNames(IElement element) {
      String[] ret = StringUtility.SplitAtSpTabCrLfFf(element.GetAttribute(
        "class"));
      String[] rel = ParseLegacyRel(element.GetAttribute("rel"));
      if (ret.length == 0 && rel.length == 0) {
        return ret;
      }
      // Replace old microformats class names with
      // their modern versions
      List<String> retList = new ArrayList<String>();
      for (String element2 : rel) {
        retList.add(element2);
      }
      for (String element2 : ret) {
        String legacyLabel = ValueLegacyLabelsMap.get(element2);
        if (complexLegacyMap.containsKey(element2)) {
          for (Object item : complexLegacyMap.get(element2)) {
            retList.add(item);
          }
        } else if (legacyLabel != null) {
          retList.add(legacyLabel);
        } else {
          retList.add(element2);
        }
      }
      if (retList.size() >= 2) {
        HashSet<String> stringSet = new HashSet<String>(retList);
        retList = Arrays.asList((Collection<String>)stringSet);
      }
      return retList.ToArray();
    }

    private static final String[] DatePatterns = new String[] {
      "%Y-%M-%d",
      "%Y-%D",
    };

    private static final String[] TimePatterns = new String[] {
      "%H:%m:%s",
      "%H:%m",
      "%H:%m:%s%Z:%z",
      "%H:%m:%s%Z%z", "%H:%m:%s%G",
      "%H:%m%Z:%z", "%H:%m%Z%z",
      "%H:%m%G",
    };

    private static String GetDTValue(IElement root, int[] source) {
      List<IElement> valueElements = GetValueClasses(root);
      boolean haveDate = false, haveTime = false, haveTimeZone = false;
      int[] components = new int[] {
        Integer.MIN_VALUE,
        Integer.MIN_VALUE,
        Integer.MIN_VALUE,
        Integer.MIN_VALUE,
        Integer.MIN_VALUE,
        Integer.MIN_VALUE,
        Integer.MIN_VALUE,
        Integer.MIN_VALUE,
      };
      if (source != null) {
        CopyComponents(source, components, true, true, true);
      }
      if (valueElements.size() == 0) {
        // No value elements, get the text content
        return GetDTValueContent(root);
      }
      for (IElement valueElement : valueElements) {
        String text = GetDTValueContent(valueElement);
        if (
          MatchDateTimePattern(
            text, // check date or date + time
            DatePatterns,
            TimePatterns,
            components,
            !haveDate,
            !haveTime,
            !haveTimeZone)) {
          // check if components are defined
          if (components[0] != Integer.MIN_VALUE) {
            haveDate = true;
          }
          if (components[3] != Integer.MIN_VALUE) {
            haveTime = true;
          }
          if (components[6] != Integer.MIN_VALUE) {
            haveTimeZone = true;
          }
        } else if (
          MatchDateTimePattern(
            text, // check time-only formats
            null,
            new String[] {
              "%H:%m:%s", "%H:%m",
              "%H:%m:%s%Z:%z",
              "%H:%m:%s%Z%z", "%H:%m:%s%G",
              "%H:%m%Z:%z", "%H:%m%Z%z",
              "%H:%m%G",
            },
            components,
            false,
            !haveTime,
            !haveTimeZone)) {
          // check if components are defined
          if (components[3] != Integer.MIN_VALUE) {
            haveTime = true;
          }
          if (components[6] != Integer.MIN_VALUE) {
            haveTimeZone = true;
          }
        } else if (
          MatchDateTimePattern(
            text,
            null,
            new String[] {
              "%Z:%z",
              "%Z%z",
              "%Z",
              "%G",
            },
            components,
            false,
            false,
            !haveTimeZone)) { // check timezone
          // formats
          if (components[6] != Integer.MIN_VALUE) {
            haveTimeZone = true;
          }
        } else if (
          MatchDateTimePattern(
            com.upokecenter.util.DataUtilities.ToLowerCaseAscii(text),
            null,
            new String[] {
              "%h:%m:%sa.m.",
              // AM clock values
              "%h:%m:%sam", "%h:%ma.m.", "%h:%mam",
              "%ha.m.", "%ham",
            },
            components,
            false,
            !haveTime,
            false)) { // check AM time formats
          if (components[3] != Integer.MIN_VALUE) {
            haveTime = true;
            // convert AM hour to 24-hour clock
            if (components[3] == 12) {
              components[3] = 0;
            }
          }
        } else if (
          MatchDateTimePattern(
            com.upokecenter.util.DataUtilities.ToLowerCaseAscii(text),
            null,
            new String[] {
              "%h:%m:%sp.m.",
              // PM clock values
              "%h:%m:%spm", "%h:%mp.m.", "%h:%mpm", "%hp.m.", "%hpm",
            },
            components,
            false,
            !haveTime,
            false)) { // check PM time formats
          if (components[3] != Integer.MIN_VALUE) {
            haveTime = true;
            // convert PM hour to 24-hour clock
            if (components[3] < 12) {
              components[3] += 12;
            }
          }
        }
      }
      return (components[0] != Integer.MIN_VALUE) ?
        ToDateTimeString(components) : GetDTValueContent(root);
    }

    private static String GetDTValueContent(IElement valueElement) {
      String elname = ElementName(valueElement);
      String text = "";
      if (HasClassName(valueElement, "value-title")) {
        return OrEmpty(valueElement.GetAttribute("title"));
      } else if (elname.equals("img") ||
        elname.equals("area")) {
        String s = valueElement.GetAttribute("alt");
        text = (s == null) ? "" : s;
      } else if (elname.equals("Data")) {
        String s = valueElement.GetAttribute("value");
        text = (s == null) ? GetTrimmedTextContent(valueElement) : s;
      } else if (elname.equals("abbr")) {
        String s = valueElement.GetAttribute("title");
        text = (s == null) ? GetTrimmedTextContent(valueElement) : s;
      } else if (elname.equals("del") ||
        elname.equals("ins") ||
        elname.equals("time")) {
        String s = valueElement.GetAttribute("datetime");
        if (StringUtility.IsNullOrSpaces(s)) {
          s = valueElement.GetAttribute("title");
        }
        text = (s == null) ? GetTrimmedTextContent(valueElement) : s;
      } else {
        text = GetTrimmedTextContent(valueElement);
      }
      return text;
    }

    private static String GetEValue(IElement root) {
      return root.GetInnerHTML();
    }

    private static IElement GetFirstChildElement(INode e) {
      for (INode child : e.GetChildNodes()) {
        if (child instanceof IElement) {
          return (IElement)child;
        }
      }
      return null;
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
      if (((href) == null || (href).length() == 0)) {
        return "";
      }
      href = HtmlCommon.ResolveURL(node, href, null);
      return ((href)==null || (href).length()==0) ? "" : href;
    }

    private static int[] GetLastKnownTime(CBORObject obj) {
      if (obj.ContainsKey("start")) {
        CBORObject arr = obj.get("start");
        // System.out.println("start %s",arr);
        Object result = arr.get(arr.size() - 1);
        if (result instanceof String) {
          int[] components = new int[] {
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
          };
          if (
            MatchDateTimePattern(
              (String)result,
              new String[] { "%Y-%M-%d", "%Y-%D" },
          new String[] {
          "%H:%m:%s", "%H:%m",
          "%H:%m:%s%Z:%z",
          "%H:%m:%s%Z%z", "%H:%m:%s%G",
          "%H:%m%Z:%z", "%H:%m%Z%z", "%H:%m%G",
        },
        components,
        true,
        true,
        true)) {
            // reset the time components
            components[3] = Integer.MIN_VALUE;
            components[4] = Integer.MIN_VALUE;
            components[5] = Integer.MIN_VALUE;
            components[6] = Integer.MIN_VALUE;
            components[7] = Integer.MIN_VALUE;
            // System.out.println("match %s",Arrays.toString(components));
            return components;
          } else {
            // System.out.println("no match");
          }
        }
      }
      return null;
    }

    /**
     * Scans an HTML document for Microformats.org metadata. The resulting object
     * will contain an "items" property, an array of all Microformats items. Each
     * item will have a "type" and "properties" properties.
     * @param root The document to scan.
     * @return A JSON object containing Microformats metadata.
     * @throws NullPointerException The parameter {@code root} is null.
     */
    public static CBORObject GetMicroformatsJSON(IDocument root) {
      if (root == null) {
        throw new NullPointerException("root");
      }
      return GetMicroformatsJSON(root.GetDocumentElement());
    }

    /**
     * Scans an HTML element for Microformats.org metadata. The resulting object
     * will contain an "items" property, an array of all Microformats items. Each
     * item will have a "type" and "properties" properties.
     * @param root The element to scan.
     * @return A JSON object containing Microformats metadata.
     * @throws NullPointerException The parameter {@code root} is null.
     */
    public static CBORObject GetMicroformatsJSON(IElement root) {
      if (root == null) {
        throw new NullPointerException("root");
      }
      CBORObject obj = CBORObject.NewMap();
      CBORObject items = CBORObject.NewArray();
      PropertyWalk(root, null, items);
      obj.Add("items", items);
      return obj;
    }

    private static int[] GetMonthAndDay(int year, int day) {
      int[] dayArray = ((year & 3) != 0 || (year % 100 == 0 && year % 400 !=
        0)) ? ValueNormalDays : ValueLeapDays;
      int month = 1;
      while (day <= 0 || day > dayArray[month]) {
        if (day > dayArray[month]) {
          day -= dayArray[month];
          ++month;
          if (month > 12) {
            return null;
          }
        }
        if (day <= 0) {
          --month;
          if (month < 1) {
            return null;
          }
          day += dayArray[month];
        }
      }

      return new int[] { month, day };
    }

    private static String GetPValue(IElement root) {
      if (root.GetAttribute("title") != null) {
        return root.GetAttribute("title");
      }
      return (com.upokecenter.util.DataUtilities.ToLowerCaseAscii(root.GetLocalName()).equals(
        "img") &&
        !StringUtility.IsNullOrSpaces(root.GetAttribute("alt"))) ?
        root.GetAttribute("alt") : GetValueContent(root, false);
    }

    /**
     * Not documented yet.
     * @param root The parameter {@code root} is a.getUpokecenter().getHtml().getIDocument()
     * object.
     * @return A CBORObject object.
     * @throws NullPointerException The parameter {@code root} is null.
     */
    public static CBORObject GetRelJSON(IDocument root) {
      if (root == null) {
        throw new NullPointerException("root");
      }
      return GetRelJSON(root.GetDocumentElement());
    }

    /**
     * Not documented yet.
     * @param root The parameter {@code root} is a.getUpokecenter().getHtml().getIElement()
     * object.
     * @return A CBORObject object.
     * @throws NullPointerException The parameter {@code root} is null.
     */
    public static CBORObject GetRelJSON(IElement root) {
      if (root == null) {
        throw new NullPointerException("root");
      }
      CBORObject obj = CBORObject.NewMap();
      CBORObject items = CBORObject.NewArray();
      CBORObject item = CBORObject.NewMap();
      AccumulateValue(item, "type", "rel");
      CBORObject props = CBORObject.NewMap();
      RelWalk(root, props);
      item.Add("properties", props);
      items.Add(item);
      obj.Add("items", items);
      return obj;
    }

    private static String[] GetRelNames(IElement element) {
      String[] ret = StringUtility.SplitAtSpTabCrLfFf(
          com.upokecenter.util.DataUtilities.ToLowerCaseAscii(element.GetAttribute("rel")));
      if (ret.length == 0) {
        return ret;
      }
      List<String> retList = new ArrayList<String>();
      for (String element2 : ret) {
        retList.add(element2);
      }
      if (retList.size() >= 2) {
        HashSet<String> stringSet = new HashSet<String>(retList);
        retList = Arrays.asList((Collection<String>)stringSet);
      }
      return retList.ToArray();
    }

    private static String TrimAndCollapseSpaces(String str) {
      if (((str) == null || (str).length() == 0)) {
        return str;
      }
      int index = 0;
      int valueSLength = str.length();
      int state = 0;
      StringBuilder sb = null;
      if (str.length() < 512) {
        while (index < valueSLength) {
          char c = str.charAt(index);
          if (c > 0x20 || (c != 0x09 && c != 0x20 && c != 0x0d && c != 0x0a &&
            c != 0x0c)) {
            ++index;
          } else {
            break;
          }
        }
        if (index == valueSLength) {
          return str;
        }
        sb = new StringBuilder();
        sb.append(str, 0, index);
      } else {
        sb = new StringBuilder();
      }
      while (index < valueSLength) {
        char c = str.charAt(index);
        if (c > 0x20 || (c != 0x09 && c != 0x20 && c != 0x0d && c != 0x0a &&
          c != 0x0c)) {
          if (state > 0) {
            sb.append(' ');
            sb.append(c);
          } else {
            sb.append(c);
          }
          state = 1;
        }
        ++index;
      }
      return sb.toString();
    }

    private static String GetTrimmedTextContent(IElement element) {
      return TrimAndCollapseSpaces(element.GetTextContent());
    }

    /**
     * Gets a Microformats "u-*" value from an HTML element. It tries to find the
     * URL from the element's attributes, if possible; otherwise from the element's
     * text.
     * @param e An HTML element.
     * @return A URL, or the empty stringValue if none was found.
     */
    private static String GetUValue(IElement e) {
      String url = GetHref(e);
      if (((url) == null || (url).length() == 0)) {
        url = GetTrimmedTextContent(e);
        if (com.upokecenter.util.URIUtility.IsValidIRI(url)) {
          return url;
        } else {
          return "";
        }
      }
      return url;
    }

    private static List<IElement> GetValueClasses(IElement root) {
      List<IElement> elements = new ArrayList<IElement>();
      for (Object element : GetChildElements(root)) {
        GetValueClassInner(element, elements);
      }
      return elements;
    }

    private static void GetValueClassInner(
      IElement root,
      List<IElement> elements) {
      String[] cls = GetClassNames(root);
      // Check if this is a value
      for (String c : cls) {
        if (c.equals("value")) {
          elements.add(root);
          return;
        } else if (c.equals("value-title")) {
          elements.add(root);
          return;
        }
      }
      // Not a value; check if this is a property
      for (String c : cls) {
        if (c.startsWith("p-") ||
          c.startsWith("e-") ||
          c.startsWith("dt-") ||
          c.startsWith("u-")) {
          // don't traverse
          return;
        }
      }
      for (Object element : GetChildElements(root)) {
        GetValueClassInner(element, elements);
      }
    }

    private static String GetValueContent(IElement root, boolean dt) {
      List<IElement> elements = GetValueClasses(root);
      if (elements.size() == 0) {
        // No value elements, get the text content
        return GetValueElementContent(root);
      } else if (elements.size() == 1) {
        // One value element
        IElement valueElement = elements.get(0);
        return GetValueElementContent(valueElement);
      } else {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (IElement element : elements) {
          if (!first) {
            builder.append(' ');
          }
          first = false;
          builder.append(GetValueElementContent(element));
        }
        return builder.toString();
      }
    }

    private static String GetValueElementContent(IElement valueElement) {
      if (HasClassName(valueElement, "value-title")) {
        // If element has the value-title class, use
        // the title instead
        return OrEmpty(valueElement.GetAttribute("title"));
      } else if (ElementName(valueElement).equals("img") || ElementName(valueElement).equals(
          "area")) {
        String s = valueElement.GetAttribute("alt");
        return (s == null) ? "" : s;
      } else if (ElementName(valueElement).equals("Data")) {
        String s = valueElement.GetAttribute("value");
        return (s == null) ? GetTrimmedTextContent(valueElement) : s;
      } else if (ElementName(valueElement).equals("abbr")) {
        String s = valueElement.GetAttribute("title");
        return (s == null) ? GetTrimmedTextContent(valueElement) : s;
      } else {
        return GetTrimmedTextContent(valueElement);
      }
    }

    private static boolean HasClassName(IElement e, String className) {
      String attr = e.GetAttribute("class");
      if (attr == null || attr.length() < className.length()) {
        return false;
      }
      String[] cls = StringUtility.SplitAtSpTabCrLfFf(attr);
      for (String c : cls) {
        if (c.equals(className)) {
          return true;
        }
      }
      return false;
    }

    private static boolean HasSingleChildElementNamed(INode e, String name) {
      boolean seen = false;
      for (INode child : e.GetChildNodes()) {
        if (child instanceof IElement) {
          if (seen) {
            return false;
          }
          if (!DataUtilities.ToLowerCaseAscii(((IElement)child).GetLocalName())
            .equals(name)) {
            return false;
          }
          seen = true;
        }
      }
      return seen;
    }

    private static boolean ImplyForLink(
      IElement root,
      CBORObject subProperties) {
      if (com.upokecenter.util.DataUtilities.ToLowerCaseAscii(root.GetLocalName()).equals("a") && root.GetAttribute("href") != null) {
        // get the link's URL
        SetValueIfAbsent(subProperties, "url", GetUValue(root));
        List<IElement> elements = GetChildElements(root);
        if (elements.size() == 1 &&
          com.upokecenter.util.DataUtilities.ToLowerCaseAscii(elements.get(0).GetLocalName()).equals(
          "img")) {
          // try to get the ALT/TITLE
          // from the image
          String valuePValue = GetPValue(elements.get(0));
          if (StringUtility.IsNullOrSpaces(valuePValue)) {
            valuePValue = GetPValue(root); // if empty, get text from link
            // instead
          }
          SetValueIfAbsent(subProperties, "name", valuePValue);
          // get the SRC of the image
          SetValueIfAbsent(subProperties, "photo", GetUValue(elements.get(0)));
        } else {
          // get the text content
          String pvalue = GetPValue(root);
          if (!StringUtility.IsNullOrSpaces(pvalue)) {
            SetValueIfAbsent(subProperties, "name", pvalue);
          }
        }
        return true;
      }
      return false;
    }

    private static int IsDatePattern(
      String value,
      int index,
      String pattern,
      int[] components) {
      int[] c = components;
      c[0] = c[1] = c[2] = c[3] = c[4] = c[5] = c[6] = c[7] = Integer.MIN_VALUE;
      if (pattern == null) {
        throw new NullPointerException("pattern");
      }
      if (value == null) {
        return -1;
      }
      int patternValue = 0;
      int valueIndex = index;
      for (int patternIndex = 0; patternIndex < pattern.length();
        patternIndex++) {
        if (valueIndex >= value.length()) {
          return -1;
        }
        char vc;
        char pc = pattern.charAt(patternIndex);
        if (pc == '%') {
          ++patternIndex;
          if (patternIndex >= pattern.length()) {
            return -1;
          }
          pc = pattern.charAt(patternIndex);
          if (pc == 'D') {
            // day of year -- expect three digits
            if (valueIndex + 3 > value.length()) {
              return -1;
            }
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = vc - '0';
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = (patternValue * 10) + (vc - '0');
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = (patternValue * 10) + (vc - '0');
            if (patternValue > 366) {
              return -1;
            }
            components[2] = patternValue;
          } else if (pc == 'Y') {
            // year -- expect four digits
            if (valueIndex + 4 > value.length()) {
              return -1;
            }
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = vc - '0';
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = (patternValue * 10) + (vc - '0');
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = (patternValue * 10) + (vc - '0');
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = (patternValue * 10) + (vc - '0');
            components[0] = patternValue;
          } else if (pc == 'G') { // expect 'Z'
            if (valueIndex + 1 > value.length()) {
              return -1;
            }
            vc = value.charAt(valueIndex++);
            if (vc != 'Z') {
              return -1;
            }
            components[6] = 0; // time zone offset is 0
            components[7] = 0;
          } else if (pc == '%') { // expect 'Z'
            if (valueIndex + 1 > value.length()) {
              return -1;
            }
            vc = value.charAt(valueIndex++);
            if (vc != '%') {
              return -1;
            }
          } else if (pc == 'Z') { // expect plus or minus, then two digits
            if (valueIndex + 3 > value.length()) {
              return -1;
            }
            boolean negative = false;
            vc = value.charAt(valueIndex++);
            if (vc != '+' && vc != '-') {
              return -1;
            }
            negative = vc == '-';
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = vc - '0';
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = (patternValue * 10) + (vc - '0');
            if (pc == 'Z' && patternValue > 12) {
              return -1; // time zone offset hour
            }
            if (negative) {
              patternValue = -patternValue;
            }
            components[6] = patternValue;
          } else if (pc == 'M' || pc == 'd' || pc == 'H' || pc == 'h' ||
            pc == 'm' || pc == 's' || pc == 'z') { // expect two digits
            if (valueIndex + 2 > value.length()) {
              return -1;
            }
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = vc - '0';
            vc = value.charAt(valueIndex++);
            if (vc < '0' || vc > '9') {
              return -1;
            }
            patternValue = (patternValue * 10) + (vc - '0');
            if (pc == 'M' && patternValue > 12) {
              return -1;
            } else if (pc == 'M') {
              components[1] = patternValue; // month
            } else if (pc == 'd' && patternValue > 31) {
              return -1;
            } else if (pc == 'd') {
              components[2] = patternValue; // day
            } else if (pc == 'H' && patternValue >= 24) {
              return -1;
            } else if (pc == 'H') {
              components[3] = patternValue; // hour
            } else if (pc == 'h' && patternValue >= 12 && patternValue != 0) {
              return -1;
            } else if (pc == 'h') {
              components[3] = patternValue; // hour (12-hour clock)
            } else if (pc == 'm' && patternValue >= 60) {
              return -1;
            } else if (pc == 'm') {
              components[4] = patternValue; // minute
            } else if (pc == 's' && patternValue > 60) {
              return -1;
            } else if (pc == 's') {
              components[5] = patternValue; // second
            } else if (pc == 'z' && patternValue >= 60) {
              return -1;
            } else if (pc == 'z') {
              components[7] = patternValue; // timezone offset minute
            }
          } else {
            return -1;
          }
        } else {
          vc = value.charAt(valueIndex++);
          if (vc != pc) {
            return -1;
          }
        }
      }
      // Special case: day of year
      if (components[2] != Integer.MIN_VALUE && components[0] != Integer.MIN_VALUE &&
        components[1] == Integer.MIN_VALUE) {
        int[] monthDay = GetMonthAndDay(components[0], components[2]);
        // System.out.println("monthday %d->%d %d"
        // , components[2], monthDay[0], monthDay[1]);
        if (monthDay == null) {
          return -1;
        }
        components[1] = monthDay[0];
        components[2] = monthDay[1];
      }
      if (components[3] != Integer.MIN_VALUE && components[4] == Integer.MIN_VALUE) {
        components[4] = 0;
      }
      if (components[4] != Integer.MIN_VALUE && components[5] == Integer.MIN_VALUE) {
        components[5] = 0;
      }
      // Special case: time zone offset
      if (components[6] != Integer.MIN_VALUE && components[7] == Integer.MIN_VALUE) {
        // System.out.println("spcase");
        components[7] = 0;
      }
      return valueIndex;
    }

    private static boolean MatchDateTimePattern(
      String value,
      String[] datePatterns,
      String[] timePatterns,
      int[] components,
      boolean useDate,
      boolean useTime,
      boolean useTimezone) {
      // year, month, day, hour, minute, second, zone offset,
      // zone offset minutes
      if (!useDate && !useTime && !useTimezone) {
        return false;
      }
      int[] c = new int[8];
      int[] c2 = new int[8];
      int index = 0;
      int oldIndex = index;
      if (datePatterns != null) {
        // match the date patterns, if any
        for (String pattern : datePatterns) {
          // reset components
          int endIndex = IsDatePattern(value, index, pattern, c);
          if (endIndex >= 0) {
            // copy any matching components
            if (endIndex >= value.length()) {
              CopyComponents(
                c,
                components,
                useDate,
                useTime,
                useTimezone);
              // we have just a date
              return true;
            }
            // match the T
            if (value.charAt(endIndex) != 'T') {
              return false;
            }
            index = endIndex + 1;
            break;
          }
        }
        if (index == oldIndex) {
          return false;
        }
      } else {
        // Won't match date patterns, so reset all components
        // instead
        c[0] = c[1] = c[2] = c[3] = c[4] = c[5] = c[6] = c[7] = Integer.MIN_VALUE;
      }
      // match the time pattern
      for (String pattern : timePatterns) {
        // reset components
        int endIndex = IsDatePattern(value, index, pattern, c2);
        if (endIndex == value.length()) {
          // copy any matching components
          CopyComponents(
            c,
            components,
            useDate,
            useTime,
            useTimezone);
          CopyComponents(
            c2,
            components,
            useDate,
            useTime,
            useTimezone);
          return true;
        }
      }
      return false;
    }

    private static String[] ParseLegacyRel(String str) {
      String[] ret = StringUtility.SplitAtSpTabCrLfFf(
          com.upokecenter.util.DataUtilities.ToLowerCaseAscii(str));
      if (ret.length == 0) {
        return ret;
      }
      List<String> relList = new ArrayList<String>();
      boolean hasTag = false;
      boolean hasSelf = false;
      boolean hasBookmark = false;
      for (String element : ret) {
        if (!hasTag && "tag".equals(element)) {
          relList.add("p-category");
          hasTag = true;
        } else if (!hasSelf && "self".equals(element)) {
          if (hasBookmark) {
            relList.add("u-url");
          }
          hasSelf = true;
        } else if (!hasBookmark && "bookmark".equals(element)) {
          if (hasSelf) {
            relList.add("u-url");
          }
          hasBookmark = true;
        }
      }
      return relList.ToArray();
    }

    private static void PropertyWalk(
      IElement root,
      CBORObject properties,
      CBORObject children) {
      String[] className = GetClassNames(root);
      if (className.length() > 0) {
        List<String> types = new ArrayList<String>();
        boolean hasProperties = false;
        for (String cls : className) {
          if (cls.startsWith("p-") && properties !=
            null) {
            hasProperties = true;
          } else if (cls.startsWith("u-") &&
            properties != null) {
            hasProperties = true;
          } else if (cls.startsWith("dt-") &&
            properties != null) {
            hasProperties = true;
          } else if (cls.startsWith("e-") &&
            properties != null) {
            hasProperties = true;
          } else if (cls.startsWith("h-")) {
            types.add(cls);
          }
        }
        if (types.size() == 0 && hasProperties) {
          // has properties and isn't a microformat
          // root
          for (String cls : className) {
            if (cls.startsWith("p-")) {
              String value = GetPValue(root);
              if (!StringUtility.IsNullOrSpaces(value)) {
                AccumulateValue(properties, cls.substring(2), value);
              }
            } else if (cls.startsWith("u-")) {
              AccumulateValue(
                properties,
                cls.substring(2),
                GetUValue(root));
            } else if (cls.startsWith("dt-")) {
              AccumulateValue(
                properties,
                cls.substring(3),
                GetDTValue(root, GetLastKnownTime(properties)));
            } else if (cls.startsWith("e-")) {
              AccumulateValue(
                properties,
                cls.substring(2),
                GetEValue(root));
            }
          }
        } else if (types.size() > 0) {
          // this is a child microformat
          // with no properties
          CBORObject obj = CBORObject.NewMap();
          obj.set("type",CBORObject.FromObject(types));
          // for holding child elements with
          // properties
          CBORObject subProperties = CBORObject.NewMap();
          // for holding child microformats with no
          // property class
          CBORObject subChildren = CBORObject.NewArray();
          for (INode child : root.GetChildNodes()) {
            if (child instanceof IElement) {
              PropertyWalk(
                (IElement)child,
                subProperties,
                subChildren);
            }
          }
          if (subChildren.size() > 0) {
            obj.Add("children", subChildren);
          }
          if (types.size() > 0) {
            // we imply missing properties here
            // Imply p-name and p-url
            if (!ImplyForLink(root, subProperties)) {
              if (HasSingleChildElementNamed(root, "a")) {
                ImplyForLink(GetFirstChildElement(root), subProperties);
              } else {
                String pvalue = GetPValue(root);
                if (!StringUtility.IsNullOrSpaces(pvalue)) {
                  SetValueIfAbsent(subProperties, "name", pvalue);
                }
              }
            }
            // Also imply u-photo
            if (com.upokecenter.util.DataUtilities.ToLowerCaseAscii(root.GetLocalName()).equals(
              "img") && root.GetAttribute("src") !=
              null) {
              SetValueIfAbsent(subProperties, "photo", GetUValue(root));
            }
            if (!subProperties.ContainsKey("photo")) {
              List<IElement> images = root.GetElementsByTagName("img");
              // If there is only one descendant image, imply
              // u-photo
              if (images.size() == 1) {
                SetValueIfAbsent(
                  subProperties,
                  "photo",
                  GetUValue(images.get(0)));
              }
            }
          }
          obj.Add("properties", subProperties);
          if (hasProperties) {
            for (String cls : className) {
              if (cls.startsWith("p-")) { // property
                CBORObject clone = CopyJson(obj);
                clone.Add("value", GetPValue(root));
                AccumulateValue(properties, cls.substring(2), clone);
              } else if (cls.startsWith("u-")) {
                // URL
                CBORObject clone = CopyJson(obj);
                clone.Add("value", GetUValue(root));
                AccumulateValue(properties, cls.substring(2), clone);
              } else if (cls.startsWith("dt-")) {
                // date/time
                CBORObject clone = CopyJson(obj);
                {
                  Object objectTemp = "value";
                  Object objectTemp2 = GetDTValue(
                      root,
                      GetLastKnownTime(properties));
                  clone.Add(objectTemp, objectTemp2);
                }
                AccumulateValue(properties, cls.substring(3), clone);
              } else if (cls.startsWith("e-")) {
                // date/time
                CBORObject clone = CopyJson(obj);
                clone.Add("value", GetEValue(root));
                AccumulateValue(properties, cls.substring(2), clone);
              }
            }
          } else {
            children.Add(obj);
          }
          return;
        }
      }
      for (INode child : root.GetChildNodes()) {
        if (child instanceof IElement) {
          PropertyWalk((IElement)child, properties, children);
        }
      }
    }

    private static void RelWalk(
      IElement root,
      CBORObject properties) {
      String[] className = GetRelNames(root);
      if (className.length() > 0) {
        String href = GetHref(root);
        if (!StringUtility.IsNullOrSpaces(href)) {
          for (String cls : className) {
            AccumulateValue(properties, cls, href);
          }
        }
      }
      for (INode child : root.GetChildNodes()) {
        if (child instanceof IElement) {
          RelWalk((IElement)child, properties);
        }
      }
    }

    private static void SetValueIfAbsent(
      CBORObject obj,
      String key,
      Object value) {
      if (!obj.ContainsKey(key)) {
        CBORObject arr = null;
        arr = CBORObject.NewArray();
        obj.Add(key, arr);
        arr.Add(value);
      }
    }

    private static String ToDateTimeString(int[] components) {
      StringBuilder builder = new StringBuilder();
      if (components[0] != Integer.MIN_VALUE) { // has a date
        // add year
        Append4d(builder, components[0]);
        builder.append('-');
        if (components[1] == Integer.MIN_VALUE) {
          Append3d(builder, components[2]); // year and day of year
        } else { // has month
          // add month and day
          Append2d(builder, components[1]);
          builder.append('-');
          Append2d(builder, components[2]);
        }
        // add T if there is a time
        if (components[3] != Integer.MIN_VALUE) {
          builder.append('T');
        }
      }
      if (components[3] != Integer.MIN_VALUE) {
        Append2d(builder, components[3]);
        builder.append(':');
        Append2d(builder, components[4]);
        builder.append(':');
        Append2d(builder, components[5]);
      }
      if (components[6] != Integer.MIN_VALUE) {
        if (components[6] == 0 && components[7] == 0) {
          builder.append('Z');
        } else if (components[6] < 0) { // negative time zone offset
          builder.append('-');
          Append2d(builder, components[6]);
          Append2d(builder, components[7]);
        } else { // positive time zone offset
          builder.append('+');
          Append2d(builder, components[6]);
          Append2d(builder, components[7]);
        }
      }
      return builder.toString();
    }

    private static String OrEmpty(String s) {
      return s == null ? "" : s;
    }

    private Microformats() {
    }
  }
