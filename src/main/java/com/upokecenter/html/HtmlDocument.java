package com.upokecenter.util;

import java.util.*;
using Com.Upokecenter.net;
using Com.Upokecenter.util;
import com.upokecenter.util.*;
import com.upokecenter.text.*;

  /**
   * Not documented yet.
   */
  public final class HtmlDocument {
private HtmlDocument() {
}
    /*
    private static final class ParseURLListener implements IResponseListener<IDocument> {
    public IDocument processResponse(String url, IReader
      stream,
        IHttpHeaders headers) {
      String contentType=headers.getHeaderField("content-type");
      return HtmlDocument.ParseStream(stream, headers.getUrl(), contentType,
          headers.getHeaderField("content-language"));
    }
    }

    /**
     * * Gets the absolute URL from an HTML element. @param node A HTML element
     * containing a URL @return an absolute URL of the element's SRC, DATA,
     * or HREF, or an empty _string if none exists.
     * @param node The parameter {@code node} is not documented yet.
     * @return A string object.
     */
    public static String getHref(IElement node) {
    String name = node.getTagName();
    String href="";
    if ("A".equals(name) ||
    "LINK".equals(name) ||
    "AREA".equals(name) ||
    "BASE".equals(name)) {
      href=node.getAttribute("href");
    } else if ("OBJECT".equals(name)) {
      href=node.getAttribute("data");
    } else if ("IMG".equals(name) ||
    "SCRIPT".equals(name) ||
    "FRAME".equals(name) ||
    "SOURCE".equals(name) ||
    "TRACK".equals(name) ||
    "IFRAME".equals(name) ||
    "AUDIO".equals(name) ||
    "VIDEO".equals(name) ||
    "EMBED".equals(name)) {
      href=node.getAttribute("src");
    } else {
    return "";
    }
    return (href==null || href.length()==0) ? ("") :
      (HtmlDocument.resolveURL(node, href, null));
    }

    /**
     * Utility method for converting a relative URL to an absolute one, using the
     * _base URI and the encoding of the given node. @param node An HTML
     * node, usually an IDocument or IElement @param href A relative or
     * absolute URL. @return An absolute URL.
     * @param node The parameter {@code node} is not documented yet.
     * @param href The parameter {@code href} is not documented yet.
     * @return A string object.
     */
    public static String getHref(INode node, String href) {
    return (href==null || href.length()==0) ? ("") :

        (HtmlDocument.resolveURL(
          node,
          href,
          null));
    }
    */

    /**
     * Not documented yet.
     * @param str The parameter {@code str} is a text string.
     * @return An IDocument object.
     */
    public static IDocument FromString(String str) {
      byte[] bytes = DataUtilities.GetUtf8Bytes (str, true);
      return ParseStream (DataIO.ToReader (bytes));
    }

    /**
     * Not documented yet.
     * @param str The parameter {@code str} is a text string.
     * @param state The parameter {@code state} is a text string.
     * @param lst The parameter {@code lst} is a text string.
     * @return An List(string[]) object.
     * @throws NullPointerException The parameter {@code state} is null.
     */
    public static List<String[]> ParseTokens(
      String str,
      String state,
      String lst) {
      byte[] bytes = DataUtilities.GetUtf8Bytes (str, true);

      // TODO: add lang (from Content-Language?)
      HtmlParser parser = new HtmlParser(
        DataIO.ToReader (bytes),
        "about:blank",
        "utf-8",
        null);
      if (state == null) {
        throw new NullPointerException("state");
      }
      return parser.parseTokens (state, lst);
    }

    /**
     * Not documented yet.
     * @param str The parameter {@code str} is a text string.
     * @param checkError Either {@code true} or {@code false}.
     * @return An IDocument object.
     */
    public static IDocument FromString(String str, boolean checkError) {
      byte[] bytes = DataUtilities.GetUtf8Bytes (str, true);
      return ParseStream (DataIO.ToReader (bytes), checkError);
    }

    /**
     * Not documented yet.
     * @param name The parameter {@code name} is a text string.
     * @return An IElement object.
     */
    public static IElement CreateHtmlElement(String name) {
      Element valueElement = new Element();
      valueElement.setLocalName (name);
      valueElement.setNamespace (HtmlCommon.HTML_NAMESPACE);
      return valueElement;
    }

    /**
     * Not documented yet.
     * @param name The parameter {@code name} is a text string.
     * @param namespaceName The parameter {@code namespaceName} is a text string.
     * @return An IElement object.
     */
    public static IElement CreateElement(
      String name,
      String namespaceName) {
      Element valueElement = new Element();
      valueElement.setLocalName (name);
      valueElement.setNamespace (namespaceName);
      return valueElement;
    }

    /**
     * Not documented yet.
     * @param nodes Not documented yet.
     * @throws NullPointerException The parameter {@code nodes} is null.
     */
    public static String ToDebugString(List<INode> nodes) {
      if (nodes == null) {
        throw new NullPointerException("nodes");
      }
      return Document.toDebugString (nodes);
    }

    /**
     * Not documented yet.
     * @param str The parameter {@code str} is a text string.
     * @param context The parameter {@code context} is a.getUpokecenter().getHtml().getIElement()
     * object.
     * @return An List(INode) object.
     */
    public static List<INode> FragmentFromString(
      String str,
      IElement context) {
      return FragmentFromString (str, context, false);
    }

    /**
     * Not documented yet.
     * @param str The parameter {@code str} is a text string.
     * @param context The parameter {@code context} is a.getUpokecenter().getHtml().getIElement()
     * object.
     * @param checkError The parameter {@code checkError} is either {@code true} or
     * {@code false}.
     * @return An List(INode) object.
     */
    public static List<INode> FragmentFromString(
      String str,
      IElement context,
      boolean checkError) {
      byte[] bytes = DataUtilities.GetUtf8Bytes (str, true);

      // TODO: add lang (from Content-Language?)
      HtmlParser parser = new HtmlParser(
        DataIO.ToReader (bytes),
        "about:blank",
        "utf-8",
        null);
      List<INode> ret = parser.checkError (checkError).parseFragment (context);
      return ret;
    }

    /**
     * Parses an HTML document from an input stream, using "about:blank" as its
     * address. @ if an I/O error occurs.
     * @param stream An input stream.
     * @return An IDocument object.
     */
    public static IDocument ParseStream(IReader stream) {
      return ParseStream (stream, "about:blank");
    }

    /**
     * Not documented yet.
     * @param stream The parameter {@code stream} is a IReader object.
     * @param checkError The parameter {@code checkError} is either {@code true} or
     * {@code false}.
     * @return An IDocument object.
     */
    public static IDocument ParseStream(IReader stream, boolean checkError) {
      return ParseStream (stream, "about:blank", "text/Html", null, checkError);
    }

    /**
     * Not documented yet.
     * @param stream The parameter {@code stream} is a IReader object.
     * @param address The parameter {@code address} is a text string.
     * @return An IDocument object.
     */
    public static IDocument ParseStream(
      IReader stream,
      String address) {
      return ParseStream (stream, address, "text/Html");
    }

    /**
     * Not documented yet.
     * @param stream The parameter {@code stream} is a IReader object.
     * @param address The parameter {@code address} is a text string.
     * @param contentType The parameter {@code contentType} is a text string.
     * @return An IDocument object.
     */
    public static IDocument ParseStream(
      IReader stream,
      String address,
      String contentType) {
      return ParseStream (stream, address, contentType, null);
    }

    /**
     * Not documented yet.
     * @param stream The parameter {@code stream} is a IReader object.
     * @param address The parameter {@code address} is a text string.
     * @param contentType The parameter {@code contentType} is a text string.
     * @param contentLang The parameter {@code contentLang} is a text string.
     * @return An IDocument object.
     */
    public static IDocument ParseStream(
      IReader stream,
      String address,
      String contentType,
      String contentLang) {
      return ParseStream (stream, address, contentType, contentLang, false);
    }

    /**
     * * Parses an HTML document from an input stream, using the given URL as its
     * address. @ if an I/O error occurs @ if the given address is not an
     * absolute URL.
     * @param stream An input stream representing an HTML document.
     * @param address An absolute URL representing an address.
     * @param contentType Desired MIME media type of the document, including the
     *  charset parameter, if any. Examples: "text/Html" or
     *  "application/xhtml+xml; charset=utf-8".
     * @param contentLang Language tag from the Content-Language header.
     * @param checkError Either {@code true} or {@code false}.
     * @return An IDocument representing the HTML document.
     * @throws NullPointerException The parameter {@code stream} or {@code
     * address} or {@code contentType} is null.
     */
    public static IDocument ParseStream(
      IReader stream,
      String address,
      String contentType,
      String contentLang,
      boolean checkError) {
      if (stream == null) {
        throw new NullPointerException("stream");
      }
      if (address == null) {
        throw new NullPointerException("address");
      }
      if (contentType == null) {
        throw new NullPointerException("contentType");
      }
      // TODO: Use MediaType to get media type and charset
      String mediatype = contentType;
      String charset = "utf-8";
      if (mediatype.equals("text/Html")) {
        // TODO: add lang (from Content-Language?)
        HtmlParser parser = new HtmlParser(stream, address, charset, contentLang);
        IDocument docret = parser.checkError (checkError).parse();
        return docret;
      } else if (mediatype.equals("application/xhtml+xml") ||
        mediatype.equals("application/xml") ||
        mediatype.equals("image/svg+xml") ||
        mediatype.equals("text/xml")) {
        throw new UnsupportedOperationException();
        // XhtmlParser parser = new XhtmlParser(stream, address, charset, contentLang);
        // return parser.parse();
      } else {
        throw new IllegalArgumentException("content type not supported: " +
mediatype);
      }
    }
  }
