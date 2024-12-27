package com.upokecenter.util;

import java.util.*;

import com.upokecenter.util.*;
using PeterO.Rdf;
using com.upokecenter.util;

  class RDFa1 implements IRDFParser {
    private static String getTextNodeText(INode node) {
      StringBuilder builder = new StringBuilder();
      for (Object child : node.getChildNodes()) {
        if (child.getNodeType() == NodeType.TEXT_NODE) {
          builder.append (((IText)child).getData());
        } else {
          builder.append (getTextNodeText (child));
        }
      }
      return builder.toString();
    }

    private static boolean isHtmlElement(IElement element, String name) {
      return element != null &&
        "http://www.w3.org/1999/xhtml".equals (element.getNamespaceURI()) &&
        name.equals (element.getLocalName());
    }

    private RDFa.EvalContext context;
    private Set<RDFTriple> outputGraph;

    private IDocument document;

    private boolean xhtml = false;

    private static final String
    RDF_XMLLITERAL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral";

    private static final String
    RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    private static List<String> relterms = (new String[] {
      "alternate",
      "appendix", "cite", "bookmark", "chapter", "contents", "copyright",
      "first", "glossary", "help", "icon", "index", "last",
      "license", "meta", "next", "prev",
      "role", "section", "start",
      "stylesheet", "subsection", "top",
      "up", "p3pv1"
    });

    private static int getCuriePrefixLength(String s, int offset, int length) {
      if (s == null || length == 0) {
        return -1;
      }
      if (s.charAt(offset) == ':') {
        return 0;
      }
      if (!isNCNameStartChar (s.charAt(offset))) {
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
        } else if (!isNCNameChar (c)) {
          return -1;
        }
        ++index;
      }
      return -1;
    }

    private static boolean hasNonTextChildNodes(INode node) {
      for (Object child : node.getChildNodes()) {
        if (child.getNodeType() != NodeType.TEXT_NODE) {
          return true;
        }
      }
      return false;
    }

    private static boolean isNCNameChar(int c) {
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

    private static boolean isNCNameStartChar(int c) {
      return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
        c == '_' || (c >= 0xc0 && c <= 0xd6) ||
        (c >= 0xd8 && c <= 0xf6) || (c >= 0xf8 && c <= 0x2ff) ||
        (c >= 0x370 && c <= 0x37d) || (c >= 0x37f && c <= 0x1fff) ||
        (c >= 0x200c && c <= 0x200d) || (c >= 0x2070 && c <= 0x218f) ||
        (c >= 0x2c00 && c <= 0x2fef) || (c >= 0x3001 && c <= 0xd7ff) ||
        (c >= 0xf900 && c <= 0xfdcf) || (c >= 0xfdf0 && c <= 0xfffd) ||
        (c >= 0x10000 && c <= 0xeffff);
    }

    private static boolean isValidCurieReference(
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
      indexes = URIUtility.SplitIRI(
        s,
        offset,
        length,
        URIUtility.ParseMode.IRIStrict);
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
      this.context.setValueBaseURI(document.getBaseURI());
      this.context.setValueNamespaces(new HashMap<String, String>());
      if (!URIUtility.HasScheme (this.context.getValueBaseURI())) {
        throw new IllegalArgumentException("baseURI: " + this.context.getValueBaseURI());
      }
      this.context.setValueParentSubject(RDFTerm.fromIRI(
          this.context.getValueBaseURI()));
      this.context.setValueParentObject(null);
      this.context.setValueIriMap(new HashMap<String, String>());
      this.context.setValueListMap(new HashMap<String, List<RDFTerm>>());
      this.context.setValueIncompleteTriples(new ArrayList<RDFa.IncompleteTriple>());
      this.context.setValueLanguage(null);
      this.outputGraph = new HashSet<RDFTriple>();
      if (isHtmlElement (document.getDocumentElement(), "html")) {
        this.xhtml = true;
      }
    }

    private RDFTerm generateBlankNode() {
      // Use "b:" as the prefix; according to the CURIE syntax,
      // "b:" can never begin a valid CURIE reference (in RDFa 1.0,
      // the reference has the broader production irelative-refValue),
      // so it can
      // be used to guarantee that generated blank nodes will never
      // conflict with those stated explicitly
      String blankNodeString = "b:" + (
        this.blankNode).toString();
      ++this.blankNode;
      RDFTerm term = RDFTerm.fromBlankNode (blankNodeString);
      this.bnodeLabels.put(blankNodeString, term);
      return term;
    }

    private String getCurie(
      String attribute,
      int offset,
      int length,
      Map<String, String> prefixMapping) {
      if (attribute == null) {
        return null;
      }
      int refIndex = offset;
      int refLength = length;
      int prefix = getCuriePrefixLength (attribute, refIndex, refLength);
      String prefixIri = null;
      if (prefix >= 0) {
        String prefixName = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(
            attribute.substring(
              refIndex, (
              refIndex)+((refIndex + prefix) - (refIndex))));
        refIndex += prefix + 1;
        refLength -= prefix + 1;
        prefixIri = prefixMapping.get(prefixName);
        prefixIri = (prefix
            == 0) ? RDFA_DEFAULT_PREFIX : prefixMapping.get(prefixName);
        if (prefixIri == null || "_" .equals (prefixName)) {
          return null;
        }
      } else
        // RDFa doesn't define a mapping for an absent prefix
      {
        return null;
      }
      if (!isValidCurieReference (attribute, refIndex, refLength)) {
        return null;
      }
      if (prefix >= 0) {
        return
          this.relativeResolve(
            prefixIri + attribute.substring(
              refIndex, (
              refIndex)+((refIndex + refLength) - refIndex)))
          .getValue();
        } else {
        return null;
      }
    }

    private String getCurie(
      String attribute,
      Map<String, String> prefixMapping) {
      return (attribute == null) ? null :
        this.getCurie (attribute, 0, attribute.length(), prefixMapping);
    }

    private RDFTerm getCurieOrBnode(
      String attribute,
      int offset,
      int length,
      Map<String, String> prefixMapping) {
      int refIndex = offset;
      int refLength = length;
      int prefix = getCuriePrefixLength (attribute, refIndex, refLength);
      String prefixIri = null;
      String prefixName = null;
      if (prefix >= 0) {
        String blank = "_";
        prefixName = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(
            attribute.substring(
              refIndex, (
              refIndex)+((refIndex + prefix) - (refIndex))));
        refIndex += prefix + 1;
        refLength -= prefix + 1;
        prefixIri = (prefix == 0) ? RDFA_DEFAULT_PREFIX :
          prefixMapping.get(prefixName);
        if (prefixIri == null &&
          !blank.equals (prefixName)) {
          return null;
        }
      } else
        // RDFa doesn't define a mapping for an absent prefix
      {
        return null;
      }
      if (!isValidCurieReference (attribute, refIndex, refLength)) {
        return null;
      }
      if (prefix >= 0) {
        if ("_".equals (prefixName)) {
          if (refLength == 0) {
            // use an empty blank node: the CURIE syntax
            // allows an empty reference;
            // see the comment
            // in generateBlankNode for why "b:" appears
            // at the beginning
            return this.getNamedBlankNode ("b:empty");
          }
          return
            this.getNamedBlankNode(
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
          this.relativeResolve(
            prefixIri + attribute.substring(
              refIndex, (
              refIndex)+((refIndex + refLength) - refIndex)));
      } else {
        return null;
      }
    }

    private RDFTerm getNamedBlankNode(String str) {
      RDFTerm term = RDFTerm.fromBlankNode (str);
      this.bnodeLabels.put(str, term);
      return term;
    }

    private String getRelTermOrCurie(
      String attribute,
      Map<String, String> prefixMapping) {
      return relterms.contains(com.upokecenter.util.DataUtilities.ToLowerCaseAscii (attribute)) ?
        ("http://www.w3.org/1999/xhtml/vocab#" +
          com.upokecenter.util.DataUtilities.ToLowerCaseAscii (attribute)) :
        this.getCurie (attribute, prefixMapping);
    }

    private RDFTerm getSafeCurieOrCurieOrIri(
      String attribute,
      Map<String, String> prefixMapping) {
      if (attribute == null) {
        return null;
      }
      int lastIndex = attribute.length() - 1;
      if (attribute.length() >= 2 && attribute.charAt(0) == '[' && attribute.charAt(lastIndex)
        == ']') {
        RDFTerm curie = this.getCurieOrBnode(
          attribute,
          1,
          attribute.length() - 2,
          prefixMapping);
        return curie;
      } else {
        RDFTerm curie = this.getCurieOrBnode(
          attribute,
          0,
          attribute.length(),
          prefixMapping);
        if (curie == null) {
          // evaluate as IRI
          return this.relativeResolve (attribute);
        }
        return curie;
      }
    }

    private void miniRdfXml(IElement node, RDFa.EvalContext evalContext) {
      this.miniRdfXml (node, evalContext, null);
    }

    // Processes a subset of RDF/XML metadata
    // Doesn't implement RDF/XML completely
    private void miniRdfXml(
      IElement node,
      RDFa.EvalContext evalContext,
      RDFTerm subject) {
      String language = evalContext.getValueLanguage();
      for (Object child : node.getChildNodes()) {
        IElement childElement = (child instanceof IElement) ?
          ((IElement)child) : null;
        if (childElement == null) {
          continue;
        }
        language = (node.getAttribute ("xml:lang") != null) ?
          node.getAttribute ("xml:lang") : evalContext.getValueLanguage();
        if (childElement.getLocalName().equals ("Description") &&
          RDF_NAMESPACE.equals (childElement.getNamespaceURI())) {
          RDFTerm about = this.relativeResolve (childElement.getAttributeNS
(RDF_NAMESPACE, "about"
));
          // System.out.println("about=%s.charAt(%s)"
          // ,about,childElement.getAttribute("about"));
          if (about == null) {
            about = subject;
            if (about == null) {
              continue;
            }
          }
          for (Object child2 : child.getChildNodes()) {
            IElement childElement2 = (child2 instanceof IElement) ? ((IElement)child2) :
              null;
            if (childElement2 == null) {
              continue;
            }
            this.miniRdfXmlChild (childElement2, about, language);
          }
        } else if (RDF_NAMESPACE.equals (childElement.getNamespaceURI())) {
          throw new UnsupportedOperationException();
        }
      }
    }

    private void miniRdfXmlChild(
      IElement node,
      RDFTerm subject,
      String language) {
      String nsname = node.getNamespaceURI();
      if (node.getAttribute ("xml:lang") != null) {
        language = node.getAttribute ("xml:lang");
      }
      String localname = node.getLocalName();
      RDFTerm predicate = this.relativeResolve (nsname + localname);
      if (!hasNonTextChildNodes (node)) {
        String content = getTextNodeText (node);
        RDFTerm literal;
        literal = (!((language) == null || (language).length() == 0)) ?
          RDFTerm.fromLangString (content, language) :
          RDFTerm.fromTypedString (content);
        this.outputGraph.Add (new RDFTriple(subject, predicate, literal));
      } else {
        String parseType = node.getAttributeNS (RDF_NAMESPACE, "parseType");
        if ("Literal".equals (parseType)) {
          throw new UnsupportedOperationException();
        }
        RDFTerm blank = this.generateBlankNode();
        this.context.setValueLanguage(language);
        this.miniRdfXml (node, this.context, blank);
        this.outputGraph.Add (new RDFTriple(subject, predicate, blank));
      }
    }

    public Set<RDFTriple> Parse() {
      this.process (this.document.getDocumentElement(), true);
      RDFInternal.replaceBlankNodes (this.outputGraph, this.bnodeLabels);
      return this.outputGraph;
    }

    private void process(IElement node, boolean root) {
      List<RDFa.IncompleteTriple> incompleteTriplesLocal = new
      ArrayList<RDFa.IncompleteTriple>();
      String localLanguage = this.context.getValueLanguage();
      RDFTerm newSubject = null;
      boolean recurse = true;
      boolean skipElement = false;
      RDFTerm currentObject = null;
      Map<String, String> namespacesLocal =
        new HashMap<String, String> (this.context.getValueNamespaces());
      Map<String, String> iriMapLocal =
        new HashMap<String, String> (this.context.getValueIriMap());
      String attr = null;
      if (!this.xhtml) {
        attr = node.getAttribute ("xml:base");
        if (attr != null) {
          this.context.setValueBaseURI(URIUtility.RelativeResolve(
            attr,
            this.context.getValueBaseURI()));
        }
      }
      // Support XML namespaces
      for (Object attrib : node.getAttributes()) {
        String name = com.upokecenter.util.DataUtilities.ToLowerCaseAscii (attrib.getName());
        // System.out.println(attrib);
        if (name.equals ("xmlns")) {
          // System.out.println("xmlns %s",attrib.getValue());
          iriMapLocal.put("", attrib.getValue());
          namespacesLocal.put("", attrib.getValue());
        } else if (name.startsWith("xmlns:") &&
          name.length() > 6) {
          String prefix = name.substring(6);
          // System.out.println("xmlns %s %s",prefix,attrib.getValue());
          if (!"_".equals (prefix)) {
            iriMapLocal.put(prefix, attrib.getValue());
          }
          namespacesLocal.put(prefix, attrib.getValue());
        }
      }
      attr = node.getAttribute ("xml:lang");
      if (attr != null) {
        localLanguage = attr;
      }
      // Support RDF/XML metadata
      if (node.getLocalName().equals ("RDF") &&
        RDF_NAMESPACE.equals (node.getNamespaceURI())) {
        this.miniRdfXml (node, this.context);
        return;
      }
      String rel = node.getAttribute ("rel");
      String rev = node.getAttribute ("rev");
      String property = node.getAttribute ("property");
      String content = node.getAttribute ("content");
      String datatype = node.getAttribute ("datatype");
      if (rel == null && rev == null) {
        // Step 4
        RDFTerm resource = this.getSafeCurieOrCurieOrIri(
            node.getAttribute ("about"),
            iriMapLocal);
        if (resource == null) {
          resource = this.getSafeCurieOrCurieOrIri(
              node.getAttribute ("resource"),
              iriMapLocal);
        }
        resource = (resource == null) ? (this.relativeResolve (node.getAttribute
("href"))) : resource;
        resource = (resource == null) ? (this.relativeResolve (node.getAttribute ("src"))) : resource;
        if (resource == null || resource.getKind() != RDFTerm.IRI) {
          String rdfTypeof = this.getCurie(
              node.getAttribute ("typeof"),
              iriMapLocal);
          if (isHtmlElement (node, "head") || isHtmlElement (node, "body")) {
            resource = this.getSafeCurieOrCurieOrIri ("",
  iriMapLocal);
          }
          if (resource == null && !this.xhtml && root) {
            resource = this.getSafeCurieOrCurieOrIri ("",
  iriMapLocal);
          }
          if (resource == null && rdfTypeof != null) {
            resource = this.generateBlankNode();
          }
          if (resource == null) {
            if (this.context.getValueParentObject() != null) {
              resource = this.context.getValueParentObject();
            }
            if (node.getAttribute ("property") == null) {
              skipElement = true;
            }
          }
          newSubject = resource;
        } else {
          newSubject = resource;
        }
      } else {
        // Step 5
        RDFTerm resource = this.getSafeCurieOrCurieOrIri(
            node.getAttribute ("about"),
            iriMapLocal);
        resource = (resource == null) ? (this.relativeResolve (node.getAttribute ("src"))) : resource;
        if (resource == null || resource.getKind() != RDFTerm.IRI) {
          String rdfTypeof = this.getCurie(
              node.getAttribute ("typeof"),
              iriMapLocal);
          if (isHtmlElement (node, "head") || isHtmlElement (node, "body")) {
            resource = this.getSafeCurieOrCurieOrIri ("",
  iriMapLocal);
          }
          if (resource == null && !this.xhtml && root) {
            resource = this.getSafeCurieOrCurieOrIri ("",
  iriMapLocal);
          }
          if (resource == null && rdfTypeof != null) {
            resource = this.generateBlankNode();
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
        resource = this.getSafeCurieOrCurieOrIri(
            node.getAttribute ("resource"),
            iriMapLocal);
        resource = (resource == null) ? (this.relativeResolve (node.getAttribute
("href"))) : resource;
        currentObject = resource;
      }
      // Step 6
      if (newSubject != null) {
        String[] types = StringUtility.SplitAtSpTabCrLf (node.getAttribute(
              "typeof"));
        for (Object type : types) {
          String iri = this.getCurie (type, iriMapLocal);
          if (iri != null) {
            this.outputGraph.Add (new RDFTriple(
                newSubject,
                RDFTerm.A,
                RDFTerm.fromIRI (iri)));
          }
        }
      }
      // Step 7
      if (currentObject != null) {
        String[] types = StringUtility.SplitAtSpTabCrLf (rel);
        for (Object type : types) {
          String iri = this.getRelTermOrCurie(
            type,
            iriMapLocal);

          if (iri != null) {
            this.outputGraph.Add (new RDFTriple(
                newSubject,
                RDFTerm.fromIRI (iri),
                currentObject));
          }
        }
        types = StringUtility.SplitAtSpTabCrLf (rev);
        for (Object type : types) {
          String iri = this.getRelTermOrCurie(
            type,
            iriMapLocal);
          if (iri != null) {
            this.outputGraph.Add (new RDFTriple(
                currentObject,
                RDFTerm.fromIRI (iri),
                newSubject));
          }
        }
      } else {
        // Step 8
        String[] types = StringUtility.SplitAtSpTabCrLf (rel);
        boolean hasPredicates = false;
        // Defines predicates
        for (Object type : types) {
          String iri = this.getRelTermOrCurie(
            type,
            iriMapLocal);
          if (iri != null) {
            if (!hasPredicates) {
              hasPredicates = true;
              currentObject = this.generateBlankNode();
            }
            RDFa.IncompleteTriple inc = new RDFa.IncompleteTriple();
            inc.setValuePredicate(RDFTerm.fromIRI (iri));
            inc.setValueDirection(RDFa.ChainingDirection.Forward);
            incompleteTriplesLocal.add(inc);
          }
        }
        types = StringUtility.SplitAtSpTabCrLf (rev);
        for (Object type : types) {
          String iri = this.getRelTermOrCurie(
            type,
            iriMapLocal);
          if (iri != null) {
            if (!hasPredicates) {
              hasPredicates = true;
              currentObject = this.generateBlankNode();
            }
            RDFa.IncompleteTriple inc = new RDFa.IncompleteTriple();
            inc.setValuePredicate(RDFTerm.fromIRI (iri));
            inc.setValueDirection(RDFa.ChainingDirection.Reverse);
            incompleteTriplesLocal.add(inc);
          }
        }
      }
      // Step 9
      String[] preds = StringUtility.SplitAtSpTabCrLf (property);
      String datatypeValue = this.getCurie(
        datatype,
        iriMapLocal);
      if (datatype != null && datatypeValue == null) {
        datatypeValue = "";
      }
      // System.out.println("datatype=[%s] prop=%s vocab=%s",
      // datatype, property, localDefaultVocab);
      // System.out.println("datatypeValue=[%s]",datatypeValue);
      RDFTerm currentProperty = null;
      for (Object pred : preds) {
        String iri = this.getCurie(
          pred,
          iriMapLocal);
        if (iri != null) {
          // System.out.println("iri=[%s]",iri);
          currentProperty = null;
          if (datatypeValue != null && datatypeValue.length() > 0 &&
            !datatypeValue.equals (RDF_XMLLITERAL)) {
            String literal = content;
            literal = (literal == null) ? (getTextNodeText (node)) : literal;
            currentProperty = RDFTerm.fromTypedString (literal, datatypeValue);
          } else if (node.getAttribute ("content") != null ||
            !hasNonTextChildNodes (node) ||
            (datatypeValue != null && datatypeValue.length() == 0)) {
            String literal = node.getAttribute ("content");
            literal = (literal == null) ? (getTextNodeText (node)) : literal;
            currentProperty = (!((localLanguage) == null || (localLanguage).length() == 0)) ?
              RDFTerm.fromLangString (literal, localLanguage) :
              RDFTerm.fromTypedString (literal);
            } else if (hasNonTextChildNodes (node) &&
            (datatypeValue == null || datatypeValue.equals (RDF_XMLLITERAL))) {
            // XML literal
            recurse = false;
            datatypeValue = (datatypeValue == null) ? (RDF_XMLLITERAL) : datatypeValue;
            try {
              String literal = ExclusiveCanonicalXML.canonicalize(
                node,
                false,
                namespacesLocal);
              currentProperty = RDFTerm.fromTypedString (literal,
  datatypeValue);
            } catch (IllegalArgumentException ex) {
              // failure to canonicalize
            }
          }

          this.outputGraph.Add (new RDFTriple(
              newSubject,
              RDFTerm.fromIRI (iri),
              currentProperty));
        }
      }
      // Step 10
      if (!skipElement && newSubject != null) {
        for (Object triple : this.context.getValueIncompleteTriples()) {
          if (triple.getValueDirection() == RDFa.ChainingDirection.Forward) {
            this.outputGraph.Add (new RDFTriple(
              this.context.getValueParentSubject(),
              triple.getValuePredicate(),
              newSubject));
          } else {
            this.outputGraph.Add (new RDFTriple(
              newSubject,
              triple.getValuePredicate(),
              this.context.getValueParentSubject()));
          }
        }
      }
      // Step 13
      if (recurse) {
        for (Object childNode : node.getChildNodes()) {
          IElement childElement;
          RDFa.EvalContext oldContext = this.context;
          if (childNode instanceof IElement) {
            childElement = (IElement)childNode;
            // System.out.println("skip=%s vocab=%s local=%s",
            // skipElement, context.defaultVocab,
            // localDefaultVocab);
            if (skipElement) {
              RDFa.EvalContext ec = oldContext.copy();
              ec.setValueLanguage(localLanguage);
              ec.setValueIriMap(iriMapLocal);
              ec.setValueNamespaces(namespacesLocal);
              this.context = ec;
              this.process (childElement, false);
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
              this.process (childElement, false);
            }
          }
          this.context = oldContext;
        }
      }
    }

    private RDFTerm relativeResolve(String iri) {
      if (iri == null) {
        return null;
      }
      return (URIUtility.SplitIRI (iri) == null) ? null :
        RDFTerm.fromIRI(
          URIUtility.RelativeResolve(
            iri,
            this.context.getValueBaseURI()));
    }
  }
