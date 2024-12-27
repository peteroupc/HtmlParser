package com.upokecenter.util;

import java.util.*;

using Com.Upokecenter.Html;
using Com.Upokecenter.util;
import com.upokecenter.util.*;

  /**
   * Implements Exclusive XML Canonicalization as specified at:
   * http://www.w3.org/TR/xml-exc-c14n/.
   */
  final class ExclusiveCanonicalXML {
    private static final class AttrComparer implements Comparator<IAttr> {
      public int compare(IAttr arg0, IAttr arg1) {
        String namespace1 = ((arg0.getPrefix()) == null || (arg0.getPrefix()).length() == 0) ?
          "" : arg0.getNamespaceURI();
        String namespace2 = ((arg1.getPrefix()) == null || (arg1.getPrefix()).length() == 0) ?
          "" : arg1.getNamespaceURI();
        // compare _namespace URIs (attributes without a valuePrefix
        // are considered to have no _namespace URI)
        int cmp = com.upokecenter.util.DataUtilities.CodePointCompare (namespace1, namespace2);
        if (cmp == 0) {
          // then compare their local names
          cmp = com.upokecenter.util.DataUtilities.CodePointCompare(
              arg0.getLocalName(),
              arg1.getLocalName());
        }
        return cmp;
      }
    }

    private static final class NamespaceAttr implements IAttr {
      private String valuePrefix;
      private String valueLocalName;
      private String value;
      private String valueName;

      public NamespaceAttr(String valuePrefix, String value) {
        if (valuePrefix.length() == 0) {
          this.valuePrefix = "";
          this.valueLocalName = "xmlns";
          this.value = value;
          this.valueName = "xmlns";
        } else {
          this.valuePrefix = "xmlns";
          this.valueLocalName = valuePrefix;
          this.valueName = "xmlns:" + value;
          this.value = value;
        }
      }

      public String getLocalName() {
        return this.valueLocalName;
      }

      public String getName() {
        return this.valueName;
      }

      public String getNamespaceURI() {
        return "http://www.w3.org/2000/xmlns/";
      }

      public String getPrefix() {
        return this.valuePrefix;
      }

      public String getValue() {
        return this.value;
      }
    }

    private static final class NamespaceAttrComparer implements Comparator<IAttr> {
      public int compare(IAttr arg0, IAttr arg1) {
        return com.upokecenter.util.DataUtilities.CodePointCompare (arg0.getName(), arg1.getName());
      }
    }

    private static final Comparator<IAttr> ValueAttrComparer = new
    AttrComparer();

    private static final Comparator<IAttr> ValueAttrNamespaceComparer = new
    NamespaceAttrComparer();

    public static String Canonicalize(
      INode node,
      boolean includeRoot,
      Map<String, String> prefixList) {
      return Canonicalize (node, includeRoot, prefixList, false);
    }

    public static String Canonicalize(
      INode node,
      boolean includeRoot,
      Map<String, String> prefixList,
      boolean withComments) {
      if (node == null) {
        throw new NullPointerException("node");
      }
      StringBuilder builder = new StringBuilder();
      List<Map<String, String>> stack = new
      ArrayList<Map<String, String>>();
      prefixList = (prefixList == null) ? ((new HashMap<String, String>())) : prefixList;
      for (Object valuePrefix : prefixList.keySet()) {
        String nsvalue = prefixList.get(valuePrefix);
        CheckNamespacePrefix (valuePrefix, nsvalue);
      }
      HashMap<String, String> item = new HashMap<String, String>();
      stack.add(item);
      if (node instanceof IDocument) {
        boolean beforeElement = true;
        for (Object child : node.getChildNodes()) {
          if (child instanceof IElement) {
            beforeElement = false;
            Canonicalize (child, builder, stack, prefixList, true,
  withComments);
          } else if (withComments || child.getNodeType() !=
            NodeType.COMMENT_NODE) {
            CanonicalizeOutsideElement (child, builder, beforeElement);
          }
        }
      } else if (includeRoot) {
        Canonicalize (node, builder, stack, prefixList, true, withComments);
      } else {
        for (Object child : node.getChildNodes()) {
          Canonicalize (child, builder, stack, prefixList, true, withComments);
        }
      }
      return builder.toString();
    }

    private static void Canonicalize(
      INode node,
      StringBuilder builder,
      List<Map<String, String>> namespaceStack,
      Map<String, String> prefixList,
      boolean addPrefixes,
      boolean withComments) {
      int nodeType = node.getNodeType();
      if (nodeType == NodeType.COMMENT_NODE) {
        if (withComments) {
          builder.append ("<!--");
          builder.append (((IComment)node).getData());
          builder.append ("-->");
        }
      } else if (nodeType == NodeType.PROCESSING_INSTRUCTION_NODE) {
        builder.append ("<?");
        builder.append (((IProcessingInstruction)node).getTarget());
        String Data = ((IProcessingInstruction)node).getData();
        if (Data.length() > 0) {
          builder.append (' ');
          builder.append (Data);
        }
        builder.append ("?>");
      } else if (nodeType == NodeType.ELEMENT_NODE) {
        IElement e = (IElement)node;
        Map<String, String>
        valueNsRendered = namespaceStack.get(namespaceStack.size() - 1);
        boolean copied = false;
        builder.append ('<');
        if (e.getPrefix() != null && e.getPrefix().length > 0) {
          builder.append (e.getPrefix());
          builder.append (':');
        }
        builder.append (e.getLocalName());
        ArrayList<IAttr> attrs = new ArrayList<IAttr>();
        HashSet<String> declaredNames = null;
        if (addPrefixes && prefixList.size() > 0) {
          declaredNames = new HashSet<String>();
        }
        for (Object attr : e.getAttributes()) {
          String valueName = attr.getName();
          String nsvalue = null;
          if ("xmlns".equals(valueName)) {
            attrs.add(attr); // add default namespace
            if (declaredNames != null) {
              declaredNames.Add ("");
            }
            nsvalue = attr.getValue();
            CheckNamespacePrefix ("", nsvalue);
          } else if (valueName.startsWith("xmlns:") && valueName.length() > 6) {
            attrs.add(attr); // add valuePrefix namespace
            if (declaredNames != null) {
              declaredNames.Add (attr.getLocalName());
            }
            nsvalue = attr.getValue();
            CheckNamespacePrefix (attr.getLocalName(), nsvalue);
          }
        }
        if (declaredNames != null) {
          // add declared prefixes to list
          for (Object valuePrefix : prefixList.keySet()) {
            if (valuePrefix == null || declaredNames.Contains (valuePrefix)) {
              continue;
            }
            String value = prefixList.get(valuePrefix);
            value = (value == null) ? ("") : value;
            attrs.add(new NamespaceAttr(valuePrefix, value));
          }
        }
        attrs.Sort (ValueAttrNamespaceComparer);
        for (Object attr : attrs) {
          String valuePrefix = attr.getLocalName();
          if (attr.getPrefix().length == 0) {
            valuePrefix = "";
          }
          String value = attr.getValue();
          boolean isEmpty = ((valuePrefix) == null || (valuePrefix).length() == 0);
          boolean isEmptyDefault = isEmpty && ((value) == null || (value).length() == 0);
          boolean renderNamespace = false;
          if (isEmptyDefault) {
            // condition used for Canonical XML
            // renderNamespace=(
            // (e.getParentNode() is IElement) &&
            //
            // !((((IElement)e.getParentNode()).getAttribute("xmlns"
            //))==null || (((IElement)e.getParentNode()).getAttribute("xmlns"
            //)).length() == 0)
            //);

            // changed condition for Exclusive XML Canonicalization
            renderNamespace = (IsVisiblyUtilized (e, "") ||
                prefixList.containsKey ("")) &&
              valueNsRendered.containsKey ("");
            } else {
            String renderedValue = valueNsRendered.get(valuePrefix);
            renderNamespace = renderedValue == null || !renderedValue.equals(
              value);
            // added condition for Exclusive XML Canonicalization
            renderNamespace = renderNamespace && (
                IsVisiblyUtilized (e, valuePrefix) ||
                prefixList.containsKey (valuePrefix));
          }
          if (renderNamespace) {
            RenderAttribute(
              builder,
              isEmpty ? null : "xmlns",
              isEmpty ? "xmlns" : valuePrefix,
              value);
            if (!copied) {
              copied = true;
              valueNsRendered = new HashMap<String, String> (
                valueNsRendered);
            }
            valueNsRendered.put(valuePrefix, value);
          }
        }
        namespaceStack.add(valueNsRendered);
        attrs.clear();
        // All other attributes
        for (Object attr : e.getAttributes()) {
          String valueName = attr.getName();
          if (! ("xmlns".equals(valueName) ||
              (valueName.startsWith("xmlns:") &&
                valueName.length() > 6))) {
            // non-_namespace node
            attrs.add(attr);
          }
        }
        attrs.Sort (ValueAttrComparer);
        for (Object attr : attrs) {
          RenderAttribute(
            builder,
            attr.getPrefix(),
            attr.getLocalName(),
            attr.getValue());
        }
        builder.append ('>');
        for (Object child : node.getChildNodes()) {
          Canonicalize (child, builder, namespaceStack, prefixList, false,
            withComments);
        }
        namespaceStack.remove(namespaceStack.size() - 1);
        builder.append ("</");
        if (e.getPrefix() != null && e.getPrefix().length > 0) {
          builder.append (e.getPrefix());
          builder.append (':');
        }
        builder.append (e.getLocalName());
        builder.append ('>');
      } else if (nodeType == NodeType.TEXT_NODE) {
        String comment = ((IText)node).getData();
        for (int i = 0; i < comment.length(); ++i) {
          char c = comment.charAt(i);
          if (c == 0x0d) {
            builder.append ("&#xD;");
          } else if (c == '>') {
            builder.append ("&gt;");
          } else if (c == '<') {
            builder.append ("&lt;");
          } else if (c == '&') {
            builder.append ("&amp;");
          } else {
            builder.append (c);
          }
        }
      }
    }

    private static void CanonicalizeOutsideElement(
      INode node,
      StringBuilder builder,
      boolean beforeDocument) {
      int nodeType = node.getNodeType();
      if (nodeType == NodeType.COMMENT_NODE) {
        if (!beforeDocument) {
          builder.append ('\n');
        }
        builder.append ("<!--");
        builder.append (((IComment)node).getData());
        builder.append ("-->");
        if (beforeDocument) {
          builder.append ('\n');
        }
      } else if (nodeType == NodeType.PROCESSING_INSTRUCTION_NODE) {
        if (!beforeDocument) {
          builder.append ('\n');
        }
        builder.append ("<?");
        builder.append (((IProcessingInstruction)node).getTarget());
        String Data = ((IProcessingInstruction)node).getData();
        if (Data.length() > 0) {
          builder.append (' ');
          builder.append (Data);
        }
        builder.append ("?>");
        if (beforeDocument) {
          builder.append ('\n');
        }
      }
    }

    private static void CheckNamespacePrefix(String valuePrefix,
      String nsvalue) {
      if (valuePrefix.equals("xmlns")) {
        throw new IllegalArgumentException("'xmlns' _namespace declared");
      }
      if (valuePrefix.equals("xml") &&
        !"http://www.w3.org/XML/1998/namespace"
        .equals (nsvalue)) {
        throw new IllegalArgumentException("'xml' bound to wrong namespace" +
"\u0020valueName");
      }
      if (!"xml".equals(valuePrefix) &&
        "http://www.w3.org/XML/1998/namespace"
        .equals (nsvalue)) {
        throw new IllegalArgumentException("'xml' bound to wrong namespace" +
"\u0020valueName");
      }
      if ("http://www.w3.org/2000/xmlns/".equals(nsvalue)) {
        throw new IllegalArgumentException("'valuePrefix' bound to xmlns namespace" +
"\u0020valueName");
      }
      if (!((nsvalue) == null || (nsvalue).length() == 0)) {
        if (!URIUtility.HasSchemeForURI (nsvalue)) {
          throw new IllegalArgumentException(nsvalue + " is not a valid namespace" +
"\u0020URI.");
        }
      } else if (!"".equals(valuePrefix)) {
        throw new IllegalArgumentException("can't undeclare a valuePrefix");
      }
    }

    private static boolean IsVisiblyUtilized(IElement element, String s) {
      String valuePrefix = element.getPrefix();
      valuePrefix = (valuePrefix == null) ? ("") : valuePrefix;
      if (s.equals(valuePrefix)) {
        return true;
      }
      if (s.length() > 0) {
        for (Object attr : element.getAttributes()) {
          valuePrefix = attr.getPrefix();
          if (valuePrefix == null) {
            continue;
          }
          if (s.equals(valuePrefix)) {
            return true;
          }
        }
      }
      return false;
    }

    private static void RenderAttribute(
      StringBuilder builder,
      String valuePrefix,
      String valueName,
      String value) {
      builder.append (' ');
      if (!((valuePrefix) == null || (valuePrefix).length() == 0)) {
        builder.append (valuePrefix);
        builder.append (":");
      }
      builder.append (valueName);
      builder.append ("=\"");
      for (int i = 0; i < value.length(); ++i) {
        char c = value.charAt(i);
        if (c == 0x0d) {
          builder.append ("&#xD;");
        } else if (c == 0x09) {
          builder.append ("&#x9;");
        } else if (c == 0x0a) {
          builder.append ("&#xA;");
        } else if (c == '"') {
          builder.append ("&#x22;");
        } else if (c == '<') {
          builder.append ("&lt;");
        } else if (c == '&') {
          builder.append ("&amp;");
        } else {
          builder.append (c);
        }
      }
      builder.append ('"');
    }

    private ExclusiveCanonicalXML() {
    }
  }
