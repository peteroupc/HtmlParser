package com.upokecenter.html.data;

import java.util.*;

import com.upokecenter.html.*;
import com.upokecenter.util.*;
import com.upokecenter.util.*;
import com.upokecenter.rdf.*;

  /**
   * Not documented yet.
   */
  public class RDFa implements IRDFParser {
    enum ChainingDirection {
      None,
      Forward,
      Reverse,
    }

    static class EvalContext {
      public final String getValueBaseURI() { return propVarvaluebaseuri; }
public final void setValueBaseURI(String value) { propVarvaluebaseuri = value; }
private String propVarvaluebaseuri;

      public final RDFTerm getValueParentSubject() { return propVarvalueparentsubject; }
public final void setValueParentSubject(RDFTerm value) { propVarvalueparentsubject = value; }
private RDFTerm propVarvalueparentsubject;

      public final RDFTerm getValueParentObject() { return propVarvalueparentobject; }
public final void setValueParentObject(RDFTerm value) { propVarvalueparentobject = value; }
private RDFTerm propVarvalueparentobject;

      public final String getValueLanguage() { return propVarvaluelanguage; }
public final void setValueLanguage(String value) { propVarvaluelanguage = value; }
private String propVarvaluelanguage;

      public final Map<String, String> getValueIriMap() { return propVarvalueirimap; }
public final void setValueIriMap(Map<String, String> value) { propVarvalueirimap = value; }
private Map<String, String> propVarvalueirimap;

      public final List<IncompleteTriple> getValueIncompleteTriples() { return propVarvalueincompletetriples; }
public final void setValueIncompleteTriples(List<IncompleteTriple> value) { propVarvalueincompletetriples = value; }
private List<IncompleteTriple> propVarvalueincompletetriples;

      public final Map<String, List<RDFTerm>> getValueListMap() { return propVarvaluelistmap; }
public final void setValueListMap(Map<String, List<RDFTerm>> value) { propVarvaluelistmap = value; }
private Map<String, List<RDFTerm>> propVarvaluelistmap;

      public final Map<String, String> getValueTermMap() { return propVarvaluetermmap; }
public final void setValueTermMap(Map<String, String> value) { propVarvaluetermmap = value; }
private Map<String, String> propVarvaluetermmap;

      public final Map<String, String> getValueNamespaces() { return propVarvaluenamespaces; }
public final void setValueNamespaces(Map<String, String> value) { propVarvaluenamespaces = value; }
private Map<String, String> propVarvaluenamespaces;

      public final String getDefaultVocab() { return propVardefaultvocab; }
public final void setDefaultVocab(String value) { propVardefaultvocab = value; }
private String propVardefaultvocab;

      public EvalContext Copy() {
        EvalContext ec = new EvalContext();
        ec.setValueBaseURI(this.getValueBaseURI());
        ec.setValueParentSubject(this.getValueParentSubject());
        ec.setValueParentObject(this.getValueParentObject());
        ec.setValueLanguage(this.getValueLanguage());
        ec.setDefaultVocab(this.getDefaultVocab());
        ec.setValueIncompleteTriples(new ArrayList<IncompleteTriple>(this.getValueIncompleteTriples()));
        ec.setValueListMap((this.getValueListMap() == null) ? null : new
          HashMap<String, List<RDFTerm>>(this.getValueListMap()));
        ec.setValueNamespaces((this.getValueNamespaces() == null) ? null : new
          HashMap<String, String>(this.getValueNamespaces()));
        ec.setValueTermMap((this.getValueTermMap() == null) ? null : new
          HashMap<String, String>(this.getValueTermMap()));
        return ec;
      }
    }

    static class IncompleteTriple {
      public final List<RDFTerm> getTripleList() { return propVartriplelist; }
public final void setTripleList(List<RDFTerm> value) { propVartriplelist = value; }
private List<RDFTerm> propVartriplelist;

      public final RDFTerm getValuePredicate() { return propVarvaluepredicate; }
public final void setValuePredicate(RDFTerm value) { propVarvaluepredicate = value; }
private RDFTerm propVarvaluepredicate;

      public final ChainingDirection getValueDirection() { return propVarvaluedirection; }
public final void setValueDirection(ChainingDirection value) { propVarvaluedirection = value; }
private ChainingDirection propVarvaluedirection;

      @Override public String toString() {
        return "IncompleteTriple [TripleList" +
          "=" + this.getTripleList() + ", " + "ValuePredicate=" +
          this.getValuePredicate() + ", ValueDirection=" +
          this.getValueDirection() + "]";
      }
    }

    private static <TKey, TValue> TValue ValueOrDefault(
      Map<TKey, TValue> dict,
      TKey key,
      TValue defValue) {
      if (dict == null) {
        throw new NullPointerException("dict");
      }
      return dict.containsKey(key) ? dict.get(key) : defValue;
    }

    private static final String RDFA_DEFAULT_PREFIX =
      "http://www.w3.org/1999/xhtml/vocab#";

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

    private static boolean IsTermChar(int c) {
      return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
        c == '_' || c == '.' || c == '-' || c == '/' || (c >= '0' && c <= '9'
) || c == 0xb7 || (c >= 0xc0 && c <= 0xd6) ||
        (c >= 0xd8 && c <= 0xf6) || (c >= 0xf8 && c <= 0x2ff) ||
        (c >= 0x300 && c <= 0x37d) || (c >= 0x37f && c <= 0x1fff) ||
        (c >= 0x200c && c <= 0x200d) || (c >= 0x203f && c <= 0x2040) ||
        (c >= 0x2070 && c <= 0x218f) || (c >= 0x2c00 && c <= 0x2fef) ||
        (c >= 0x3001 && c <= 0xd7ff) || (c >= 0xf900 && c <= 0xfdcf) ||
        (c >= 0xfdf0 && c <= 0xfffd) || (c >= 0x10000 && c <= 0xeffff);
    }

    private IRDFParser parser;

    private EvalContext context;

    private Set<RDFTriple> outputGraph;

    private IDocument document;

    private static boolean xhtml_rdfa11 = false;

    private static final RDFTerm RDFA_USES_VOCABULARY =
      RDFTerm.FromIRI("http://www.w3.org/ns/rdfa#usesVocabulary");

    private static final String
    RDF_XMLLITERAL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral";

    private static final String[] ValueEmptyStringArray = new String[0];

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

    private static <T> T GetueCaseInsensitive(
      Map<String, T> map,
      String key) {
      if (key == null) {
        return ValueOrDefault(map, null, null);
      }
      key = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(key);
      for (var k : map.keySet()) {
        if (key.equals(com.upokecenter.util.DataUtilities.ToLowerCaseAscii(k))) {
          return map.get(k);
        }
      }
      return null;
    }

    private static boolean IsValidCurieReference(
      String s,
      int offset,
      int length) {
      return com.upokecenter.util.URIUtility.IsValidCurieReference(s, offset, length);
    }

    private static boolean IsValidTerm(String s) {
      if (s == null || s.length() == 0) {
        return false;
      }
      if (!IsNCNameStartChar(s.charAt(0))) {
        return false;
      }
      int index = 1;
      int valueSLength = s.length();
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
          return false;
        } else if (!IsTermChar(c)) {
          return false;
        }
        ++index;
      }
      return true;
    }

    private static String[] SplitPrefixList(String s) {
      if (s == null || s.length() == 0) {
        return ValueEmptyStringArray;
      }
      int index = 0;
      int valueSLength = s.length();
      while (index < valueSLength) {
        char c = s.charAt(index);
        if (c != 0x09 && c != 0x0a && c != 0x0d && c != 0x20) {
          break;
        }
        ++index;
      }
      if (index == s.length()) {
        return ValueEmptyStringArray;
      }
      StringBuilder prefix = new StringBuilder();
      StringBuilder iri = new StringBuilder();
      int state = 0; // Before NCName state
      ArrayList<String> strings = new ArrayList<String>();
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
          break;
        }
        if (state == 0) { // Before NCName
          if (c == 0x09 || c == 0x0a || c == 0x0d || c == 0x20) {
            // ignore whitespace
            ++index;
          } else if (IsNCNameStartChar(c)) {
            // start of NCName
            if (c <= 0xffff) {
              {
                prefix.append((char)c);
              }
            } else if (c <= 0x10ffff) {
              prefix.append((char)((((c - 0x10000) >> 10) & 0x3ff) | 0xd800));
              prefix.append((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
            }
            state = 1;
            ++index;
          } else {
            // error
            break;
          }
        } else if (state == 1) { // NCName
          if (c == ':') {
            state = 2;
            ++index;
          } else if (IsNCNameChar(c)) {
            // continuation of NCName
            if (c <= 0xffff) {
              {
                prefix.append((char)c);
              }
            } else if (c <= 0x10ffff) {
              prefix.append((char)((((c - 0x10000) >> 10) & 0x3ff) | 0xd800));
              prefix.append((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
            }
            ++index;
          } else {
            // error
            break;
          }
        } else if (state == 2) { // After NCName
          if (c == ' ') {
            state = 3;
            ++index;
          } else {
            // error
            break;
          }
        } else if (state == 3) { // Before IRI
          if (c == ' ') {
            ++index;
          } else {
            // start of IRI
            if (c <= 0xffff) {
              {
                iri.append((char)c);
              }
            } else if (c <= 0x10ffff) {
              iri.append((char)((((c - 0x10000) >> 10) & 0x3ff) | 0xd800));
              iri.append((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
            }
            state = 4;
            ++index;
          }
        } else if (state == 4) { // IRI
          if (c == 0x09 || c == 0x0a || c == 0x0d || c == 0x20) {
            String prefixString = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(
                prefix.toString());
            // add prefix only if it isn't empty;
            // empty prefixes will not have a mapping
            if (prefixString.length() > 0) {
              strings.add(prefixString);
              strings.add(iri.toString());
            }
            prefix.setLength(0);
            iri.setLength(0);
            state = 0;
            ++index;
          } else {
            // continuation of IRI
            if (c <= 0xffff) {
              {
                iri.append((char)c);
              }
            } else if (c <= 0x10ffff) {
              iri.append((char)((((c - 0x10000) >> 10) & 0x3ff) | 0xd800));
              iri.append((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
            }
            ++index;
          }
        }
      }
      if (state == 4) {
        strings.add(com.upokecenter.util.DataUtilities.ToLowerCaseAscii(prefix.toString()));
        strings.add(iri.toString());
      }
      return strings.toArray(new String[] { });
    }

    private int blankNode;
    private Map<String, RDFTerm> bnodeLabels = new
    HashMap<String, RDFTerm>();

    /**
     * Initializes a new instance of the RDFa class.
     * @param document The parameter {@code document} is an IDocument object.
     */
    public RDFa(IDocument document) {
      this.document = document;
      this.parser = null;
      this.context = new EvalContext();
      this.context.setDefaultVocab(null);
      this.context.setValueBaseURI(document.GetBaseURI());
      if (!URIUtility.HasScheme(this.context.getValueBaseURI())) {
        throw new IllegalArgumentException("ValueBaseURI: " +
          this.context.getValueBaseURI());
      }
      this.context.setValueParentSubject(RDFTerm.FromIRI(
          this.context.getValueBaseURI()));
      this.context.setValueParentObject(null);
      this.context.setValueNamespaces(new HashMap<String, String>());
      this.context.setValueIriMap(new HashMap<String, String>());
      this.context.setValueListMap(new HashMap<String, List<RDFTerm>>());
      this.context.setValueTermMap(new HashMap<String, String>());
      this.context.setValueIncompleteTriples(new ArrayList<IncompleteTriple>());
      this.context.setValueLanguage(null);
      this.outputGraph = new HashSet<RDFTriple>();
      this.context.getValueTermMap().put(
        "describedby",
        "http://www.w3.org/2007/05/powder-s#describedby");
      this.context.getValueTermMap().put(
        "license",
        "http://www.w3.org/1999/xhtml/vocab#license");
      this.context.getValueTermMap().put(
        "role",
        "http://www.w3.org/1999/xhtml/vocab#role");
      this.context.getValueIriMap().put("cc", "https://creativecommons.org/ns#");
      this.context.getValueIriMap().put("ctag", "http://commontag.org/ns#");
      this.context.getValueIriMap().put("dc", "http://purl.org/dc/terms/");
      this.context.getValueIriMap().put("dcterms", "http://purl.org/dc/terms/");
      this.context.getValueIriMap().put("dc11", "http://purl.org/dc/elements/1.1/");
      this.context.getValueIriMap().put("foaf", "http://xmlns.com/foaf/0.1/");
      this.context.getValueIriMap().put("gr", "http://purl.org/goodrelations/v1#");
      this.context.getValueIriMap().put(
        "ical",
        "http://www.w3.org/2002/12/cal/icaltzd#");
      this.context.getValueIriMap().put("og", "http://ogp.me/ns#");
      this.context.getValueIriMap().put("schema", "http://schema.org/");
      this.context.getValueIriMap().put("rev", "http://purl.org/stuff/rev#");
      this.context.getValueIriMap().put("sioc", "http://rdfs.org/sioc/ns#");
      this.context.getValueIriMap().put(
        "grddl",
        "http://www.w3.org/2003/g/Data-view#");
      this.context.getValueIriMap().put("ma", "http://www.w3.org/ns/ma-ont#");
      this.context.getValueIriMap().put("owl", "http://www.w3.org/2002/07/owl#");
      this.context.getValueIriMap().put("prov", "http://www.w3.org/ns/prov#");
      this.context.getValueIriMap().put(
        "rdf",
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
      this.context.getValueIriMap().put("rdfa", "http://www.w3.org/ns/rdfa#");
      this.context.getValueIriMap().put(
        "rdfs",
        "http://www.w3.org/2000/01/rdf-schema#");
      this.context.getValueIriMap().put("rif", "http://www.w3.org/2007/rif#");
      this.context.getValueIriMap().put("rr", "http://www.w3.org/ns/r2rml#");
      this.context.getValueIriMap().put(
        "sd",
        "http://www.w3.org/ns/sparql-service-description#");
      this.context.getValueIriMap().put(
        "skos",
        "http://www.w3.org/2004/02/skos/core#");
      this.context.getValueIriMap().put(
        "skosxl",
        "http://www.w3.org/2008/05/skos-xl#");
      this.context.getValueIriMap().put("v", "http://rdf.data-vocabulary.org/#");
      this.context.getValueIriMap().put("vcard",
        "http://www.w3.org/2006/vcard/ns#");
      this.context.getValueIriMap().put("void", "http://rdfs.org/ns/void#");
      this.context.getValueIriMap().put("wdr", "http://www.w3.org/2007/05/powder#");
      this.context.getValueIriMap().put(
        "wdrs",
        "http://www.w3.org/2007/05/powder-s#");
      this.context.getValueIriMap().put(
        "xhv",
        "http://www.w3.org/1999/xhtml/vocab#");
      this.context.getValueIriMap().put(
        "xml",
        "http://www.w3.org/XML/1998/namespaceValue");
      this.context.getValueIriMap().put("xsd", "http://www.w3.org/2001/XMLSchema#");
      IElement docElement = document.GetDocumentElement();
      if (docElement != null && IsHtmlElement(docElement, "Html")) {
        xhtml_rdfa11 = true;
        String version = docElement.GetAttribute("version");
        if (version != null && "XHTML+RDFa 1.1".equals(version)) {
          xhtml_rdfa11 = true;
          String[] terms = new String[] {
            "alternate", "appendix", "cite",
            "bookmark", "chapter", "contents",
            "copyright", "first", "glossary",
            "help", "icon", "index", "last",
            "license", "meta", "next", "prev",
            "previous", "section", "start",
            "stylesheet", "subsection", "top",
            "up", "p3pv1",
          };
          for (String term : terms) {
            this.context.getValueTermMap().put(
              term,
              "http://www.w3.org/1999/xhtml/vocab#" + term);
          }
        }
        if (version != null && "XHTML+RDFa 1.0".equals(version)) {
          this.parser = new RDFa1(document);
        }
      }
      this.ExtraContext();
    }

    private void ExtraContext() {
      this.context.getValueIriMap().put("bibo", "http://purl.org/ontology/bibo/");
      this.context.getValueIriMap().put("dbp", "http://dbpedia.org/property/");
      this.context.getValueIriMap().put("dbp-owl", "http://dbpedia.org/ontology/");
      this.context.getValueIriMap().put("dbr", "http://dbpedia.org/resource/");
      this.context.getValueIriMap().put("ex", "http://example.org/");
    }

    private RDFTerm GenerateBlankNode() {
      // Use "//" as the prefix; according to the CURIE syntax,
      // "//" can never begin a valid CURIE reference, so it can
      // be used to guarantee that generated blank nodes will never
      // conflict with those stated explicitly
      String blankNodeString = "//" + RDFa1.IntToString(this.blankNode);
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
 prefixIri = (prefix == 0) ? RDFA_DEFAULT_PREFIX :
ValueOrDefault(prefixMapping, prefixName, null);
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
              refIndex)+((refIndex + refLength) - refIndex))).GetValue();
      } else {
        return null;
      }
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
ValueOrDefault(prefixMapping, prefixName, null);
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
            // in GenerateBlankNode for why "//" appears
            // at the beginning
            return this.GetdBlankNode("//empty");
          }
          return
            this.GetdBlankNode(
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

    private String GetTermOrCurieOrAbsIri(
      String attribute,
      Map<String, String> prefixMapping,
      Map<String, String> termMapping,
      String DefaultVocab) {
      if (attribute == null) {
        return null;
      }
      if (IsValidTerm(attribute)) {
        if (DefaultVocab != null) {
          return this.RelativeResolve(DefaultVocab +
              attribute).GetValue();
        } else if (termMapping.containsKey(attribute)) {
          return ValueOrDefault(termMapping, attribute, null);
        } else {
          String value = GetueCaseInsensitive(termMapping, attribute);
          return value;
        }
      }
      String curie = this.GetCurie(
          attribute,
          0,
          attribute.length(),
          prefixMapping);
      if (curie == null) {
        // evaluate as IRI if it's absolute
        if (com.upokecenter.util.URIUtility.HasScheme(attribute)) {
          // System.out.println("has scheme: %s",attribute)
          return this.RelativeResolve(attribute).GetValue();
        }
        return null;
      }
      return curie;
    }

    /**
     * Not documented yet.
     * @return An ISet(RDFTriple) object.
     */
    public Set<RDFTriple> Parse() {
      if (this.parser != null) {
        return this.parser.Parse();
      }
      this.Process(this.document.GetDocumentElement(), true);
      RDFInternal.ReplaceBlankNodes(this.outputGraph, this.bnodeLabels);
      return this.outputGraph;
    }

    private void Process(IElement node, boolean root) {
      List<IncompleteTriple> incompleteTriplesLocal = new
      ArrayList<IncompleteTriple>();
      String localLanguage = this.context.getValueLanguage();
      RDFTerm newSubject = null;
      boolean skipElement = false;
      RDFTerm currentProperty = null;

      RDFTerm currentObject = null;
      RDFTerm typedResource = null;
      Map<String, String> iriMapLocal =
        new HashMap<String, String>(this.context.getValueIriMap());
      Map<String, String> namespacesLocal =
        new HashMap<String, String>(this.context.getValueNamespaces());
      Map<String, List<RDFTerm>> mapLocalOfLists =
        this.context.getValueListMap();
      Map<String, String> termMapLocal =
        new HashMap<String, String>(this.context.getValueTermMap());
      String localDefaultVocab = this.context.getDefaultVocab();
      String attr = null;
      // System.out.println("cur parobj.set(%s,%s"
      // , node.GetTagName(), context.getValueParentObject()));
      // System.out.println("_base=%s",context.getValueBaseURI());
      attr = node.GetAttribute("xml:base");
      if (attr != null) {
        this.context.setValueBaseURI(com.upokecenter.util.URIUtility.RelativeResolve(
            attr,
            this.context.getValueBaseURI()));
      }
      // Support deprecated XML ValueNamespaces
      for (IAttr attrib : node.GetAttributes()) {
        String name = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(attrib.GetName());
        // System.out.println(attrib);
        if (name.equals("xmlns")) {
          // System.out.println("xmlns %s",attrib.GetValue());
          iriMapLocal.put("",attrib.GetValue());
          namespacesLocal.put("",attrib.GetValue());
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
      attr = node.GetAttribute("vocab");
      if (attr != null) {
        if (attr.length() == 0) {
          // set default vocabulary to null
          localDefaultVocab = null;
        } else {
          // set default vocabulary to vocab IRI
          RDFTerm defPrefix = this.RelativeResolve(attr);
          localDefaultVocab = defPrefix.GetValue();
          this.outputGraph.add(new RDFTriple(
            RDFTerm.FromIRI(this.context.getValueBaseURI()),
            RDFA_USES_VOCABULARY,
            defPrefix));
        }
      }

      attr = node.GetAttribute("prefix");
      if (attr != null) {
        String[] prefixList = SplitPrefixList(attr);
        for (int i = 0; i < prefixList.length; i += 2) {
          // Add prefix and IRI to the map, unless the prefix
          // is "_"
          if (!"_".equals(prefixList[i])) {
            iriMapLocal.put(prefixList[i], prefixList[i + 1]);
          }
        }
      }
      attr = node.GetAttribute("lang");
      if (attr != null) {
        localLanguage = attr;
      }
      attr = node.GetAttribute("xml:lang");
      if (attr != null) {
        localLanguage = attr;
      }
      String rel = node.GetAttribute("rel");
      String rev = node.GetAttribute("rev");
      String property = node.GetAttribute("property");
      String content = node.GetAttribute("content");
      String datatype = node.GetAttribute("datatype");
      if (rel == null && rev == null) {
        // Step 5
        // System.out.println("%s %s",property,node.GetTagName());
        if (property != null && content == null && datatype == null) {
          RDFTerm about = this.GetSafeCurieOrCurieOrIri(
              node.GetAttribute("about"),
              iriMapLocal);
          if (about != null) {
            newSubject = about;
          } else if (root) {
            newSubject = this.GetSafeCurieOrCurieOrIri(
                "",
                iriMapLocal);
          } else if (this.context.getValueParentObject() != null) {
            newSubject = this.context.getValueParentObject();
          }
          String _typeof = node.GetAttribute("typeof");
          if (_typeof != null) {
            if (about != null) {
              typedResource = about;
            } else if (root) {
              typedResource = this.GetSafeCurieOrCurieOrIri(
                  "",
                  iriMapLocal);
            } else {
              RDFTerm resource = this.GetSafeCurieOrCurieOrIri(
                  node.GetAttribute("resource"),
                  iriMapLocal);
              resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
                "href"))) : resource;
              resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
                "src"))) : resource;
              // System.out.println("resource=%s",resource);
              if ((resource == null || resource.GetKind() != RDFTerm.IRI) &&
                xhtml_rdfa11) {
                if (IsHtmlElement(node, "head") ||
                  IsHtmlElement(node, "body")) {
                  newSubject = this.context.getValueParentObject();
                }
              }
              typedResource = (resource == null) ? this.GenerateBlankNode() :
                resource;
              currentObject = typedResource;
            }
          }
        } else {
          RDFTerm resource = this.GetSafeCurieOrCurieOrIri(
              node.GetAttribute("about"),
              iriMapLocal);
          if (resource == null) {
            resource = this.GetSafeCurieOrCurieOrIri(
                node.GetAttribute("resource"),
                iriMapLocal);
            // System.out.println("resource=%s %s %s",
            // node.GetAttribute("resource"),
            // resource, context.getValueParentObject());
          }
          resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
            "href"))) : resource;
          resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
            "src"))) : resource;
          if ((resource == null || resource.GetKind() != RDFTerm.IRI) &&
            xhtml_rdfa11) {
            if (IsHtmlElement(node, "head") ||
              IsHtmlElement(node, "body")) {
              resource = this.context.getValueParentObject();
            }
          }
          if (resource == null) {
            if (root) {
              newSubject = this.GetSafeCurieOrCurieOrIri(
                  "",
                  iriMapLocal);
            } else if (node.GetAttribute("typeof") != null) {
              newSubject = this.GenerateBlankNode();
            } else {
              if (this.context.getValueParentObject() != null) {
                newSubject = this.context.getValueParentObject();
              }
              if (node.GetAttribute("property") == null) {
                skipElement = true;
              }
            }
          } else {
            newSubject = resource;
          }
          if (node.GetAttribute("typeof") != null) {
            typedResource = newSubject;
          }
        }
      } else {
        // Step 6
        RDFTerm about = this.GetSafeCurieOrCurieOrIri(
            node.GetAttribute("about"),
            iriMapLocal);
        if (about != null) {
          newSubject = about;
        }
        if (node.GetAttribute("typeof") != null) {
          typedResource = newSubject;
        }
        if (about == null) {
          if (root) {
            about = this.GetSafeCurieOrCurieOrIri(
                "",
                iriMapLocal);
          } else if (this.context.getValueParentObject() != null) {
            newSubject = this.context.getValueParentObject();
          }
        }
        RDFTerm resource = this.GetSafeCurieOrCurieOrIri(
            node.GetAttribute("resource"),
            iriMapLocal);
        resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
          "href"))) : resource;
        resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
          "src"))) : resource;
        if ((resource == null || resource.GetKind() != RDFTerm.IRI) &&
          xhtml_rdfa11) {
          if (IsHtmlElement(node, "head") ||
            IsHtmlElement(node, "body")) {
            newSubject = this.context.getValueParentObject();
          }
        }
        if (resource == null && node.GetAttribute("typeof") != null &&
          node.GetAttribute("about") == null) {
          currentObject = this.GenerateBlankNode();
        } else if (resource != null) {
          currentObject = resource;
        }
        if (node.GetAttribute("typeof") != null &&
          node.GetAttribute("about") == null) {
          typedResource = currentObject;
        }
      }
      // Step 7
      if (typedResource != null) {
        String[] types = StringUtility.SplitAtSpTabCrLf(node.GetAttribute(
          "typeof"));
        for (String type : types) {
          String iri = this.GetTermOrCurieOrAbsIri(
              type,
              iriMapLocal,
              termMapLocal,
              localDefaultVocab);
          if (iri != null) {
            this.outputGraph.add(new RDFTriple(
              typedResource,
              RDFTerm.A,
              RDFTerm.FromIRI(iri)));
          }
        }
      }
      // Step 8
      if (newSubject != null &&
        !newSubject.equals(this.context.getValueParentObject())) {
        this.context.getValueListMap().clear();
      }
      // Step 9
      if (currentObject != null) {
        String inlist = node.GetAttribute("inlist");
        if (inlist != null && rel != null) {
          String[] types = StringUtility.SplitAtSpTabCrLf(rel);
          for (String type : types) {
            String iri = this.GetTermOrCurieOrAbsIri(
                type,
                iriMapLocal,
                termMapLocal,
                localDefaultVocab);
            if (iri != null) {
              if (!mapLocalOfLists.containsKey(iri)) {
                List<RDFTerm> newList = new ArrayList<RDFTerm>();
                newList.add(currentObject);
                mapLocalOfLists.put(iri, newList);
              } else {
                List<RDFTerm> existingList = mapLocalOfLists.get(iri);
                existingList.add(currentObject);
              }
            }
          }
        } else {
          String[] types = StringUtility.SplitAtSpTabCrLf(rel);

          for (String type : types) {
            String iri = this.GetTermOrCurieOrAbsIri(
                type,
                iriMapLocal,
                termMapLocal,
                localDefaultVocab);
            if (iri != null) {
              this.outputGraph.add(new RDFTriple(
                newSubject,
                RDFTerm.FromIRI(iri),
                currentObject));
            }
          }
          types = StringUtility.SplitAtSpTabCrLf(rev);
          for (String type : types) {
            String iri = this.GetTermOrCurieOrAbsIri(
                type,
                iriMapLocal,
                termMapLocal,
                localDefaultVocab);
            if (iri != null) {
              this.outputGraph.add(new RDFTriple(
                currentObject,
                RDFTerm.FromIRI(iri),
                newSubject));
            }
          }
        }
      } else {
        // Step 10
        String[] types = StringUtility.SplitAtSpTabCrLf(rel);
        boolean inlist = node.GetAttribute("inlist") != null;
        boolean hasPredicates = false;
        // Defines predicates
        for (String type : types) {
          String iri = this.GetTermOrCurieOrAbsIri(
              type,
              iriMapLocal,
              termMapLocal,
              localDefaultVocab);
          if (iri != null) {
            if (!hasPredicates) {
              hasPredicates = true;
              currentObject = this.GenerateBlankNode();
            }
            IncompleteTriple inc = new IncompleteTriple();
            if (inlist) {
              if (!mapLocalOfLists.containsKey(iri)) {
                List<RDFTerm> newList = new ArrayList<RDFTerm>();
                mapLocalOfLists.put(iri, newList);
                // NOTE: Should not be a Copy
                inc.setTripleList(newList);
              } else {
                List<RDFTerm> existingList = mapLocalOfLists.get(iri);
                inc.setTripleList(existingList);
              }
              inc.setValueDirection(ChainingDirection.None);
            } else {
              inc.setValuePredicate(RDFTerm.FromIRI(iri));
              inc.setValueDirection(ChainingDirection.Forward);
            }
            // System.out.println(inc);
            incompleteTriplesLocal.add(inc);
          }
        }
        types = StringUtility.SplitAtSpTabCrLf(rev);
        for (String type : types) {
          String iri = this.GetTermOrCurieOrAbsIri(
              type,
              iriMapLocal,
              termMapLocal,
              localDefaultVocab);
          if (iri != null) {
            if (!hasPredicates) {
              hasPredicates = true;
              currentObject = this.GenerateBlankNode();
            }
            IncompleteTriple inc = new IncompleteTriple();
            inc.setValuePredicate(RDFTerm.FromIRI(iri));
            inc.setValueDirection(ChainingDirection.Reverse);
            incompleteTriplesLocal.add(inc);
          }
        }
      }
      // Step 11
      String[] preds = StringUtility.SplitAtSpTabCrLf(property);
      String datatypeValue = this.GetTermOrCurieOrAbsIri(
          datatype,
          iriMapLocal,
          termMapLocal,
          localDefaultVocab);
      if (datatype != null && datatypeValue == null) {
        datatypeValue = "";
      }
      // System.out.println("datatype=[%s] prop=%s vocab=%s",
      // datatype, property, localDefaultVocab);
      // System.out.println("datatypeValue=[%s]",datatypeValue);
      for (String pred : preds) {
        String iri = this.GetTermOrCurieOrAbsIri(
            pred,
            iriMapLocal,
            termMapLocal,
            localDefaultVocab);
        if (iri != null) {
          // System.out.println("iri=[%s]",iri);
          currentProperty = null;
          if (datatypeValue != null && datatypeValue.length() > 0 &&
            !datatypeValue.equals(RDF_XMLLITERAL)) {
            String literal = content;
            literal = (literal == null) ? (GetTextNodeText(node)) : literal;
            currentProperty = RDFTerm.FromTypedString(literal, datatypeValue);
          } else if (datatypeValue != null && datatypeValue.length() == 0) {
            String literal = content;
            literal = (literal == null) ? (GetTextNodeText(node)) : literal;
            currentProperty = (!((localLanguage) == null || (localLanguage).length() == 0)) ?
              RDFTerm.FromLangString(literal, localLanguage) :
              RDFTerm.FromTypedString(literal);
          } else if (datatypeValue != null &&
            datatypeValue.equals(RDF_XMLLITERAL)) {
            // XML literal
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
          } else if (content != null) {
            String literal = content;
            currentProperty = (!((localLanguage) == null || (localLanguage).length() == 0)) ?
              RDFTerm.FromLangString(literal, localLanguage) :
              RDFTerm.FromTypedString(literal);
          } else if (rel == null && content == null && rev == null) {
            RDFTerm resource = this.GetSafeCurieOrCurieOrIri(
                node.GetAttribute("resource"),
                iriMapLocal);
            resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
              "href"))) : resource;
            resource = (resource == null) ? (this.RelativeResolve(node.GetAttribute(
              "src"))) : resource;
            if (resource != null) {
              currentProperty = resource;
            }
          }
          if (currentProperty == null) {
            if (node.GetAttribute("typeof") != null &&
              node.GetAttribute("about") == null) {
              currentProperty = typedResource;
            } else {
              String literal = content;
              literal = (literal == null) ? (GetTextNodeText(node)) : literal;
              currentProperty = (!((localLanguage) == null || (localLanguage).length() == 0)) ?
                RDFTerm.FromLangString(literal, localLanguage) :
                RDFTerm.FromTypedString(literal);
            }
          }
          // System.out.println("curprop: %s",currentProperty);
          if (node.GetAttribute("inlist") != null) {
            if (!mapLocalOfLists.containsKey(iri)) {
              List<RDFTerm> newList = new ArrayList<RDFTerm>();
              newList.add(currentProperty);
              mapLocalOfLists.put(iri, newList);
            } else {
              List<RDFTerm> existingList = mapLocalOfLists.get(iri);
              existingList.add(currentProperty);
            }
          } else {
            this.outputGraph.add(new RDFTriple(
              newSubject,
              RDFTerm.FromIRI(iri),
              currentProperty));
          }
        }
      }
      // Step 12
      if (!skipElement && newSubject != null) {
        List<IncompleteTriple> triples = this.context.getValueIncompleteTriples();
        for (IncompleteTriple triple : triples) {
          if (triple.getValueDirection() == ChainingDirection.None) {
            List<RDFTerm> TripleList = triple.getTripleList();
            TripleList.add(newSubject);
          } else if (triple.getValueDirection() == ChainingDirection.Forward) {
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
      for (INode childNode : node.GetChildNodes()) {
        IElement childElement;
        EvalContext oldContext = this.context;
        if (childNode instanceof IElement) {
          childElement = (IElement)childNode;
          // System.out.println("skip=%s vocab=%s local=%s",
          // skipElement, context.getDefaultVocab(),
          // localDefaultVocab);
          if (skipElement) {
            EvalContext ec = oldContext.Copy();
            ec.setValueLanguage(localLanguage);
            ec.setValueIriMap(iriMapLocal);
            this.context = ec;
            this.Process(childElement, false);
          } else {
            EvalContext ec = new EvalContext();
            ec.setValueBaseURI(oldContext.getValueBaseURI());
            ec.setValueNamespaces(namespacesLocal);
            ec.setValueIriMap(iriMapLocal);
            ec.setValueIncompleteTriples(incompleteTriplesLocal);
            ec.setValueListMap(mapLocalOfLists);
            ec.setValueTermMap(termMapLocal);
            ec.setValueParentSubject((newSubject == null) ?
              oldContext.getValueParentSubject() : newSubject);
            ec.setValueParentObject((currentObject == null) ? ((newSubject ==
              null) ? oldContext.getValueParentSubject() : newSubject) :
              currentObject);
            ec.setDefaultVocab(localDefaultVocab);
            ec.setValueLanguage(localLanguage);
            this.context = ec;
            this.Process(childElement, false);
          }
        }
        this.context = oldContext;
      }
      // Step 14
      for (var iri : mapLocalOfLists.keySet()) {
        if (!this.context.getValueListMap().containsKey(iri)) {
          List<RDFTerm> TripleList = mapLocalOfLists.get(iri);
          if (TripleList.size() == 0) {
            this.outputGraph.add(new RDFTriple(
              newSubject == null ? newSubject : this.context.getValueParentSubject(),
              RDFTerm.FromIRI(iri),
              RDFTerm.NIL));
          } else {
            RDFTerm bnode = this.GenerateBlankNode();
            this.outputGraph.add(new RDFTriple(
              newSubject == null ? newSubject : this.context.getValueParentSubject(),
              RDFTerm.FromIRI(iri),
              bnode));
            for (int i = 0; i < TripleList.size(); ++i) {
              RDFTerm nextBnode = (i == TripleList.size() - 1) ?
                this.GenerateBlankNode() : RDFTerm.NIL;
              this.outputGraph.add(new RDFTriple(
                bnode,
                RDFTerm.FIRST,
                TripleList.get(i)));
              this.outputGraph.add(new RDFTriple(
                bnode,
                RDFTerm.REST,
                nextBnode));
              bnode = nextBnode;
            }
          }
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
