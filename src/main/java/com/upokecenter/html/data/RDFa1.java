package com.upokecenter.html.data;

import java.util.*;

import com.upokecenter.html.*;
import com.upokecenter.util.*;
import com.upokecenter.util.*;
import com.upokecenter.rdf.*;

  class RDFa1 implements IRDFParser {
    public static String IntToString(int value) {
      String digits = "0123456789";
      if (value == Integer.MIN_VALUE) {
        return "-2147483648";
      }
      if (value == 0) {
        return "0";
      }
      boolean neg = value < 0;
      char[] chars = new char[12];
      int count = 11;
      if (neg) {
        value = -value;
      }
      while (value > 43698) {
        int intdivvalue = value / 10;
        char digit = digits.charAt((int)(value - (intdivvalue * 10)));
        chars[count--] = digit;
        value = intdivvalue;
      }
      while (value > 9) {
        int intdivvalue = (value * 26215) >> 18;
        char digit = digits.charAt((int)(value - (intdivvalue * 10)));
        chars[count--] = digit;
        value = intdivvalue;
      }
      if (value != 0) {
        chars[count--] = digits.charAt((int)value);
      }
      if (neg) {
        chars[count] = '-';
      } else {
        ++count;
      }
      return new String(chars, count, 12 - count);
    }

    private static String GetTextNodeText(INode node) {
      StringBuilder builder = new StringBuilder();
      for (INode child : node.GetChildNodes()) {
        if (child.GetNodeType() == NodeType.TEXT_NODE) {
          builder.append(((IText)child).GetData());
        } else {
          builder.append(GetTextNodeText(child));
        }
      }
      return builder.toString();
    }

    private static boolean IsHtmlElement(IElement element, String name) {
      return element != null &&
        "http://www.w3.org/1999/xhtml".equals(element.GetNamespaceURI()) &&
        name.equals(element.GetLocalName());
    }

    private RDFa.EvalContext context;
    private Set<RDFTriple> outputGraph;

    private IDocument document;

    private boolean xhtml = false;

    private static final String
    RDF_XMLLITERAL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral";

    private static final String
    RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    private static List<String> relterms = Arrays.asList(
      "alternate",
      "appendix", "cite", "bookmark", "chapter", "contents", "copyright",
      "first", "glossary", "help", "icon", "index", "last",
      "license", "meta", "next", "prev",
      "role", "section", "start",
      "stylesheet", "subsection", "top",
      "up", "p3pv1");

    private static int GetCuriePrefixLength(String s, int offset, int length) {
      if (s == null || length == 0) {
        return -1;
      }
      if (s.charAt(offset) == ':') {
        return 0;
      }
      if (!IsNCNameStartChar(s.charAt(offset))) {
        return -1;
      }
      int index = offset + 1;
      int valueSLength = offset + length;
      while (index < valueSLength) {
        // Get the next Unicode character
        int c = s.charAt(index);
        if ((c & 0xfc00) == 0xd800 && index + 1 < valueSLength &&
          (s.charAt(index + 1) & 0xfc00) == 0xdc00) {
          // Get the Unicode code point for the surrogate pair
          c = 0x10000 + ((c & 0x3ff) << 10) + (s.charAt(index + 1) & 0x3ff);
          ++index;
        } else if ((c & 0xf800) == 0xd800) {
          // error
          return -1;
        }
        if (c == ':') {
          return index - offset;
        } else if (!IsNCNameChar(c)) {
          return -1;
        }
        ++index;
      }
      return -1;
    }

    private static boolean HasNonTextChildNodes(INode node) {
      for (INode child : node.GetChildNodes()) {
        if (child.GetNodeType() != NodeType.TEXT_NODE) {
          return true;
        }
      }
      return false;
    }

    private static boolean IsNCNameChar(int c) {
      return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
        c == '_' || c == '.' || c == '-' || (c >= '0' && c <= '9') ||
        c == 0xb7 || (c >= 0xc0 && c <= 0xd6) ||
        (c >= 0xd8 && c <= 0xf6) || (c >= 0xf8 && c <= 0x2ff) ||
        (c >= 0x300 && c <= 0x37d) || (c >= 0x37f && c <= 0x1fff) ||
        (c >= 0x200c && c <= 0x200d) || (c >= 0x203f && c <= 0x2040) ||
        (c >= 0x2070 && c <= 0x218f) || (c >= 0x2c00 && c <= 0x2fef) ||
        (c >= 0x3001 && c <= 0xd7ff) || (c >= 0xf900 && c <= 0xfdcf) ||
        (c >= 0xfdf0 && c <= 0xfffd) || (c >= 0x10000 && c <= 0xeffff);
    }

    private static boolean IsNCNameStartChar(int c) {
      return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
        c == '_' || (c >= 0xc0 && c <= 0xd6) ||
        (c >= 0xd8 && c <= 0xf6) || (c >= 0xf8 && c <= 0x2ff) ||
        (c >= 0x370 && c <= 0x37d) || (c >= 0x37f && c <= 0x1fff) ||
        (c >= 0x200c && c <= 0x200d) || (c >= 0x2070 && c <= 0x218f) ||
        (c >= 0x2c00 && c <= 0x2fef) || (c >= 0x3001 && c <= 0xd7ff) ||
        (c >= 0xf900 && c <= 0xfdcf) || (c >= 0xfdf0 && c <= 0xfffd) ||
        (c >= 0x10000 && c <= 0xeffff);
    }

    private static boolean IsValidCurieReference(
      String s,
      int offset,
      int length) {
      if (s == null) {
        return false;
      }
      if (length == 0) {
        return true;
      }
      int[]
      indexes = com.upokecenter.util.URIUtility.SplitIRI(
          s,
          offset,
          length,
          com.upokecenter.util.URIUtility.ParseMode.IRIStrict);
      if (indexes == null) {
        return false;
      }
      if (indexes[0] != -1) {
        // check if scheme component is present
        return false;
      }
      return true;
    }

    private int blankNode;

    private Map<String, RDFTerm> bnodeLabels = new
    HashMap<String, RDFTerm>();

    private static final String RDFA_DEFAULT_PREFIX =
      "http://www.w3.org/1999/xhtml/vocab#";

    public RDFa1(IDocument document) {
      this.document = document;
      this.context = new RDFa.EvalContext();
      this.context.setValueBaseURI(document.GetBaseURI());
      this.context.setValueNamespaces(new HashMap<String, String>());
      if (!URIUtility.HasScheme(this.context.getValueBaseURI())) {
        throw new IllegalArgumentException("baseURI: " + this.context.getValueBaseURI());
      }
      this.context.setValueParentSubject(RDFTerm.FromIRI(
          this.context.getValueBaseURI()));
      this.context.setValueParentObject(null);
      this.context.setValueIriMap(new HashMap<String, String>());
      this.context.setValueListMap(new HashMap<String, List<RDFTerm>>());
      this.context.setValueIncompleteTriples(new ArrayList<RDFa.IncompleteTriple>());
      this.context.setValueLanguage(null);
      this.outputGraph = new HashSet<RDFTriple>();
      if (IsHtmlElement(document.GetDocumentElement(), "Html")) {
        this.xhtml = true;
      }
    }

    private RDFTerm GenerateBlankNode() {
      // Use "b:" as the prefix; according to the CURIE syntax,
      // "b:" can never begin a valid CURIE reference (in RDFa 1.0,
      // the reference has the broader production irelative-refValue),
      // so it can
      // be used to guarantee that generated blank nodes will never
      // conflict with those stated explicitly
      String blankNodeString = "b:" + IntToString(this.blankNode);
      ++this.blankNode;
      RDFTerm term = RDFTerm.FromBlankNode(blankNodeString);
      this.bnodeLabels.put(blankNodeString, term);
      return term;
    }

    private String GetCurie(
      String attribute,
      int offset,
      int length,
      Map<String, String> prefixMapping) {
      if (attribute == null) {
        return null;
      }
      int refIndex = offset;
      int refLength = length;
      int prefix = GetCuriePrefixLength(attribute, refIndex, refLength);
      String prefixIri = null;
      if (prefix >= 0) {
        String prefixName = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(
            attribute.substring(
              refIndex, (
              refIndex)+((refIndex + prefix) - (refIndex)))); refIndex += prefix + 1;
 refLength -= prefix + 1;
 prefixIri = prefixMapping.get(prefixName);
 prefixIri =
(prefix == 0) ? RDFA_DEFAULT_PREFIX : prefixMapping.get(prefixName);
        if (prefixIri == null || "_".equals(prefixName)) {
          return null;
        }
      } else
        // RDFa doesn't define a mapping for an absent prefix
      {
        return null;
      }
      if (!IsValidCurieReference(attribute, refIndex, refLength)) {
        return null;
      }
      if (prefix >= 0) {
        return
          this.RelativeResolve(
            prefixIri + attribute.substring(
              refIndex, (
              refIndex)+((refIndex + refLength) - refIndex)))
          .GetValue();
      } else {
        return null;
      }
    }

    private String GetCurie(
      String attribute,
      Map<String, String> prefixMapping) {
      return (attribute == null) ? null :
        this.GetCurie(attribute, 0, attribute.length(), prefixMapping);
    }

    private RDFTerm GetCurieOrBnode(
      String attribute,
      int offset,
      int length,
      Map<String, String> prefixMapping) {
      int refIndex = offset;
      int refLength = length;
      int prefix = GetCuriePrefixLength(attribute, refIndex, refLength);
      String prefixIri = null;
      String prefixName = null;
      if (prefix >= 0) {
        String blank = "_";
        prefixName = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(
            attribute.substring(
              refIndex, (
              refIndex)+((refIndex + prefix) - (refIndex)))); refIndex += prefix + 1;
 refLength -= prefix + 1;
 prefixIri = (prefix == 0) ? RDFA_DEFAULT_PREFIX :
prefixMapping.get(prefixName);
        if (prefixIri == null && !blank.equals(prefixName)) {
          return null;
        }
      } else
        // RDFa doesn't define a mapping for an absent prefix
      {
        return null;
      }
      if (!IsValidCurieReference(attribute, refIndex, refLength)) {
        return null;
      }
      if (prefix >= 0) {
        if ("_".equals(prefixName)) {
          if (refLength == 0) {
            // use an empty blank node: the CURIE syntax
            // allows an empty reference;
            // see the comment
            // in GenerateBlankNode for why "b:" appears
            // at the beginning
            return this.GetdBlankNode("b:empty");
          }
          return this.GetdBlankNode(
              attribute.substring(
                refIndex, (
                refIndex)+((refIndex + refLength) - refIndex)));
        }
        if (!(refIndex >= 0)) {
          throw new IllegalStateException(attribute);
        }
        if (!(refIndex + refLength <= attribute.length())) {
          throw new IllegalStateException(attribute);
        }
        return
          this.RelativeResolve(
            prefixIri + attribute.substring(
              refIndex, (
              refIndex)+((refIndex + refLength) - refIndex)));
      } else {
        return null;
      }
    }

    private RDFTerm GetdBlankNode(String str) {
      RDFTerm term = RDFTerm.FromBlankNode(str);
      this.bnodeLabels.put(str, term);
      return term;
    }

    private String GetRelTermOrCurie(
      String attribute,
      Map<String, String> prefixMapping) {
      return relterms.contains(com.upokecenter.util.DataUtilities.ToLowerCaseAscii(attribute)) ?
        ("http://www.w3.org/1999/xhtml/vocab#" +
          com.upokecenter.util.DataUtilities.ToLowerCaseAscii(attribute)) :
        this.GetCurie(attribute, prefixMapping);
    }

    private RDFTerm GetSafeCurieOrCurieOrIri(
      String attribute,
      Map<String, String> prefixMapping) {
      if (attribute == null) {
        return null;
      }
      int lastIndex = attribute.length() - 1;
      if (attribute.length() >= 2 && attribute.charAt(0) == '[' && attribute.charAt(lastIndex)
        == ']') {
        RDFTerm curie = this.GetCurieOrBnode(
            attribute,
            1,
            attribute.length() - 2,
            prefixMapping);
        return curie;
      } else {
        RDFTerm curie = this.GetCurieOrBnode(
            attribute,
            0,
            attribute.length(),
            prefixMapping);
        if (curie == null) {
          // evaluate as IRI
          return this.RelativeResolve(attribute);
        }
        return curie;
      }
    }

    private void MiniRdfXml(IElement node, RDFa.EvalContext evalContext) {
      this.MiniRdfXml(node, evalContext, null);
    }

    // Processes a subset of RDF/XML metadata
    // Doesn't implement RDF/XML completely
    private void MiniRdfXml(
      IElement node,
      RDFa.EvalContext evalContext,
      RDFTerm subject) {
      String language = evalContext.getValueLanguage();
      for (INode child : node.GetChildNodes()) {
        IElement childElement = (child instanceof IElement) ?
          ((IElement)child) : null;
        if (childElement == null) {
          continue;
        }
        language = (node.GetAttribute("xml:lang") != null) ?
          node.GetAttribute("xml:lang") : evalContext.getValueLanguage();
        if (childElement.GetLocalName().equals("Description") &&
          RDF_NAMESPACE.equals(childElement.GetNamespaceURI())) {
          RDFTerm about = this.RelativeResolve(childElement.GetAttributeNS(
            RDF_NAMESPACE,
            "about"));
          // System.out.println("about=%s.charAt(%s)"
          // ,about,childElement.GetAttribute("about"));
          if (about == null) {
            about = subject;
            if (about == null) {
              continue;
            }
          }
          for (INode child2 : child.GetChildNodes()) {
            IElement childElement2 = (child2 instanceof IElement) ? ((IElement)child2) :
              null;
            if (childElement2 == null) {
              continue;
            }
            this.MiniRdfXmlChild(childElement2, about, language);
          }
        } else if (RDF_NAMESPACE.equals(childElement.GetNamespaceURI())) {
          throw new UnsupportedOperationException();
        }
      }
    }

    private void MiniRdfXmlChild(
      IElement node,
      RDFTerm subject,
      String language) {
      String nsname = node.GetNamespaceURI();
      if (node.GetAttribute("xml:lang") != null) {
        language = node.GetAttribute("xml:lang");
      }
      String localname = node.GetLocalName();
      RDFTerm predicate = this.RelativeResolve(nsname + localname);
      if (!HasNonTextChildNodes(node)) {
        String content = GetTextNodeText(node);
        RDFTerm literal;
        literal = (!((language) == null || (language).length() == 0)) ?
          RDFTerm.FromLangString(content, language) :
          RDFTerm.FromTypedString(content);
        this.outputGraph.add(new RDFTriple(subject, predicate, literal));
      } else {
        String parseType = node.GetAttributeNS(RDF_NAMESPACE, "parseType");
        if ("Literal".equals(parseType)) {
          throw new UnsupportedOperationException();
        }
        RDFTerm blank = this.GenerateBlankNode();
        this.context.setValueLanguage(language);
        this.MiniRdfXml(node, this.context, blank);
        this.outputGraph.add(new RDFTriple(subject, predicate, blank));
      }
    }

    public Set<RDFTriple> Parse() {
      this.Process(this.document.GetDocumentElement(), true);
      RDFInternal.ReplaceBlankNodes(this.outputGraph, this.bnodeLabels);
      return this.outputGraph;
    }

    private void Process(IElement node, boolean root) {
      List<RDFa.IncompleteTriple> incompleteTriplesLocal = new
      ArrayList<RDFa.IncompleteTriple>();
      String localLanguage = this.context.getValueLanguage();
      RDFTerm newSubject = null;
      boolean recurse = true;
      boolean skipElement = false;
      RDFTerm currentObject = null;
      Map<String, String> namespacesLocal =
        new HashMap<String, String>(this.context.getValueNamespaces());
      Map<String, String> iriMapLocal =
        new HashMap<String, String>(this.context.getValueIriMap());
      String attr = null;
      if (!this.xhtml) {
        attr = node.GetAttribute("xml:base");
        if (attr != null) {
          this.context.setValueBaseURI(com.upokecenter.util.URIUtility.RelativeResolve(
              attr,
              this.context.getValueBaseURI()));
        }
      }
      // Support XML namespaces
      for (IAttr attrib : node.GetAttributes()) {
        String name = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(attrib.GetName());
        // System.out.println(attrib);
        if (name.equals("xmlns")) {
          // System.out.println("xmlns %s",attrib.GetValue());
          iriMapLocal.put("", attrib.GetValue());
          namespacesLocal.put("", attrib.GetValue());
        } else if (name.startsWith("xmlns:") &&
          name.length() > 6) {
          String prefix = name.substring(6);
          // System.out.println("xmlns %s %s",prefix,attrib.GetValue());
          if (!"_".equals(prefix)) {
            iriMapLocal.put(prefix, attrib.GetValue());
          }
          namespacesLocal.put(prefix, attrib.GetValue());
        }
      }
      attr = node.GetAttribute("xml:lang");
      if (attr != null) {
        localLanguage = attr;
      }
      // Support RDF/XML metadata
      if (node.GetLocalName().equals("RDF") &&
        RDF_NAMESPACE.equals(node.GetNamespaceURI())) {
        this.MiniRdfXml(node, this.context);
        return;
      }
      String rel = node.GetAttribute("rel");
      String rev = node.GetAttribute("rev");
      String property = node.GetAttribute("property");
      String content = node.GetAttribute("content");
      String datatype = node.GetAttribute("datatype");
      if (rel == null && rev == null) {
        // Step 4
        RDFTerm resource = this.GetSafeCurieOrCurieOrIri(
            node.GetAttribute("about"),
            iriMapLocal);
        if (resource == null) {
          resource = this.GetSafeCurieOrCurieOrIri(
              node.GetAttribute("resource"),
              iriMapLocal);
        }
        resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
          "href"))) : resource;
        resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute("src"))) : resource;
        if (resource == null || resource.GetKind() != RDFTerm.IRI) {
          String rdfTypeof = this.GetCurie(
              node.GetAttribute("typeof"),
              iriMapLocal);
          if (IsHtmlElement(node, "head") || IsHtmlElement(node, "body")) {
            resource = this.GetSafeCurieOrCurieOrIri("",
                iriMapLocal);
          }
          if (resource == null && !this.xhtml && root) {
            resource = this.GetSafeCurieOrCurieOrIri("",
                iriMapLocal);
          }
          if (resource == null && rdfTypeof != null) {
            resource = this.GenerateBlankNode();
          }
          if (resource == null) {
            if (this.context.getValueParentObject() != null) {
              resource = this.context.getValueParentObject();
            }
            if (node.GetAttribute("property") == null) {
              skipElement = true;
            }
          }
          newSubject = resource;
        } else {
          newSubject = resource;
        }
      } else {
        // Step 5
        RDFTerm resource = this.GetSafeCurieOrCurieOrIri(
            node.GetAttribute("about"),
            iriMapLocal);
        resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute("src"))) : resource;
        if (resource == null || resource.GetKind() != RDFTerm.IRI) {
          String rdfTypeof = this.GetCurie(
              node.GetAttribute("typeof"),
              iriMapLocal);
          if (IsHtmlElement(node, "head") || IsHtmlElement(node, "body")) {
            resource = this.GetSafeCurieOrCurieOrIri("",
                iriMapLocal);
          }
          if (resource == null && !this.xhtml && root) {
            resource = this.GetSafeCurieOrCurieOrIri("",
                iriMapLocal);
          }
          if (resource == null && rdfTypeof != null) {
            resource = this.GenerateBlankNode();
          }
          if (resource == null) {
            if (this.context.getValueParentObject() != null) {
              resource = this.context.getValueParentObject();
            }
          }
          newSubject = resource;
        } else {
          newSubject = resource;
        }
        resource = this.GetSafeCurieOrCurieOrIri(
            node.GetAttribute("resource"),
            iriMapLocal);
        resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
          "href"))) : resource;
        currentObject = resource;
      }
      // Step 6
      if (newSubject != null) {
        String[] types = StringUtility.SplitAtSpTabCrLf(node.GetAttribute(
          "typeof"));
        for (String type : types) {
          String iri = this.GetCurie(type, iriMapLocal);
          if (iri != null) {
            this.outputGraph.add(new RDFTriple(
              newSubject,
              RDFTerm.A,
              RDFTerm.FromIRI(iri)));
          }
        }
      }
      // Step 7
      if (currentObject != null) {
        String[] types = StringUtility.SplitAtSpTabCrLf(rel);
        for (String type : types) {
          String iri = this.GetRelTermOrCurie(
              type,
              iriMapLocal);

          if (iri != null) {
            this.outputGraph.add(new RDFTriple(
              newSubject,
              RDFTerm.FromIRI(iri),
              currentObject));
          }
        }
        types = StringUtility.SplitAtSpTabCrLf(rev);
        for (String type : types) {
          String iri = this.GetRelTermOrCurie(
              type,
              iriMapLocal);
          if (iri != null) {
            this.outputGraph.add(new RDFTriple(
              currentObject,
              RDFTerm.FromIRI(iri),
              newSubject));
          }
        }
      } else {
        // Step 8
        String[] types = StringUtility.SplitAtSpTabCrLf(rel);
        boolean hasPredicates = false;
        // Defines predicates
        for (String type : types) {
          String iri = this.GetRelTermOrCurie(
              type,
              iriMapLocal);
          if (iri != null) {
            if (!hasPredicates) {
              hasPredicates = true;
              currentObject = this.GenerateBlankNode();
            }
            RDFa.IncompleteTriple inc = new RDFa.IncompleteTriple();
            inc.setValuePredicate(RDFTerm.FromIRI(iri));
            inc.setValueDirection(RDFa.ChainingDirection.Forward);
            incompleteTriplesLocal.add(inc);
          }
        }
        types = StringUtility.SplitAtSpTabCrLf(rev);
        for (String type : types) {
          String iri = this.GetRelTermOrCurie(
              type,
              iriMapLocal);
          if (iri != null) {
            if (!hasPredicates) {
              hasPredicates = true;
              currentObject = this.GenerateBlankNode();
            }
            RDFa.IncompleteTriple inc = new RDFa.IncompleteTriple();
            inc.setValuePredicate(RDFTerm.FromIRI(iri));
            inc.setValueDirection(RDFa.ChainingDirection.Reverse);
            incompleteTriplesLocal.add(inc);
          }
        }
      }
      // Step 9
      String[] preds = StringUtility.SplitAtSpTabCrLf(property);
      String datatypeValue = this.GetCurie(
          datatype,
          iriMapLocal);
      if (datatype != null && datatypeValue == null) {
        datatypeValue = "";
      }
      // System.out.println("datatype=[%s] prop=%s vocab=%s",
      // datatype, property, localDefaultVocab);
      // System.out.println("datatypeValue=[%s]",datatypeValue);
      RDFTerm currentProperty = null;
      for (String pred : preds) {
        String iri = this.GetCurie(
            pred,
            iriMapLocal);
        if (iri != null) {
          // System.out.println("iri=[%s]",iri);
          currentProperty = null;
          if (datatypeValue != null && datatypeValue.length() > 0 &&
            !datatypeValue.equals(RDF_XMLLITERAL)) {
            String literal = content;
            literal = (literal == null) ? (GetTextNodeText(node)) : literal;
            currentProperty = RDFTerm.FromTypedString(literal, datatypeValue);
          } else if (node.GetAttribute("content") != null ||
            !HasNonTextChildNodes(node) ||
            (datatypeValue != null && datatypeValue.length() == 0)) {
            String literal = node.GetAttribute("content");
            literal = (literal == null) ? (GetTextNodeText(node)) : literal;
            currentProperty = (!((localLanguage) == null || (localLanguage).length() == 0)) ?
              RDFTerm.FromLangString(literal, localLanguage) :
              RDFTerm.FromTypedString(literal);
          } else if (HasNonTextChildNodes(node) && (datatypeValue == null ||
            datatypeValue.equals(RDF_XMLLITERAL))) {
            // XML literal
            recurse = false;
            datatypeValue = (datatypeValue == null) ? (RDF_XMLLITERAL) : datatypeValue;
            try {
              String literal = ExclusiveCanonicalXML.Canonicalize(
                  node,
                  false,
                  namespacesLocal);
              currentProperty = RDFTerm.FromTypedString(literal,
                  datatypeValue);
            } catch (IllegalArgumentException ex) {
              // failure to canonicalize
            }
          }

          this.outputGraph.add(new RDFTriple(
            newSubject,
            RDFTerm.FromIRI(iri),
            currentProperty));
        }
      }
      // Step 10
      if (!skipElement && newSubject != null) {
        List<RDFa.IncompleteTriple> triples =
          this.context.getValueIncompleteTriples();
        for (RDFa.IncompleteTriple triple : triples) {
          if (triple.getValueDirection() == RDFa.ChainingDirection.Forward) {
            this.outputGraph.add(new RDFTriple(
              this.context.getValueParentSubject(),
              triple.getValuePredicate(),
              newSubject));
          } else {
            this.outputGraph.add(new RDFTriple(
              newSubject,
              triple.getValuePredicate(),
              this.context.getValueParentSubject()));
          }
        }
      }
      // Step 13
      if (recurse) {
        List<INode> childNodes = node.GetChildNodes();
        for (INode childNode : childNodes) {
          IElement childElement;
          RDFa.EvalContext oldContext = this.context;
          if (childNode instanceof IElement) {
            childElement = (IElement)childNode;
            // System.out.println("skip=%s vocab=%s local=%s",
            // skipElement, context.defaultVocab,
            // localDefaultVocab);
            if (skipElement) {
              RDFa.EvalContext ec = oldContext.Copy();
              ec.setValueLanguage(localLanguage);
              ec.setValueIriMap(iriMapLocal);
              ec.setValueNamespaces(namespacesLocal);
              this.context = ec;
              this.Process(childElement, false);
            } else {
              RDFa.EvalContext ec = new RDFa.EvalContext();
              ec.setValueBaseURI(oldContext.getValueBaseURI());
              ec.setValueIriMap(iriMapLocal);
              ec.setValueNamespaces(namespacesLocal);
              ec.setValueIncompleteTriples(incompleteTriplesLocal);
              ec.setValueParentSubject((newSubject == null) ?
                oldContext.getValueParentSubject() : newSubject);
              ec.setValueParentObject((currentObject == null) ? ((newSubject
                == null) ? oldContext.getValueParentSubject() : newSubject) :
                currentObject);
              ec.setValueLanguage(localLanguage);
              this.context = ec;
              this.Process(childElement, false);
            }
          }
          this.context = oldContext;
        }
      }
    }

    private RDFTerm RelativeResolve(String iri) {
      if (iri == null) {
        return null;
      }
      return (com.upokecenter.util.URIUtility.SplitIRI(iri) == null) ? null :
        RDFTerm.FromIRI(
          com.upokecenter.util.URIUtility.RelativeResolve(
            iri,
            this.context.getValueBaseURI()));
    }
  }
