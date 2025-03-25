package com.upokecenter.html.data;

import java.util.*;

import com.upokecenter.rdf.*;

  final class RDFInternal {
    private static <TKey, TValue> TValue ValueOrDefault(
      Map<TKey, TValue> dict,
      TKey key,
      TValue defValue) {
      if (dict == null) {
        throw new NullPointerException("dict");
      }
      return dict.containsKey(key) ? dict.get(key) : defValue;
    }

    private static <TKey, TValue> TValue ValueOrNull(
      Map<TKey, TValue> dict,
      TKey key) {
      return ValueOrDefault(dict, key, null);
    }

    /**
     * Replaces certain blank nodes with blank nodes whose names meet the N-Triples
     * requirements.
     * @param triples A set of RDF triples.
     * @param bnodeLabels A mapping of blank node names already allocated. This
     * method will modify this object as needed to allocate new blank nodes.
     */
    static void ReplaceBlankNodes(
      Set<RDFTriple> triples,
      Map<String, RDFTerm> bnodeLabels) {
      if (bnodeLabels.size() == 0) {
        return;
      }
      Map<String, RDFTerm> newBlankNodes = new
      HashMap<String, RDFTerm>();
      List<RDFTriple[]> changedTriples = new ArrayList<RDFTriple[]>();
      int[] nodeindex = new int[] { 0 };
      for (RDFTriple triple : triples) {
        boolean changed = false;
        RDFTerm subj = triple.GetSubject();
        if (subj.GetKind() == RDFTerm.BLANK) {
          String oldname = subj.GetValue();
          String newname = SuggestBlankNodeName(
              oldname,
              nodeindex,
              bnodeLabels);
          if (!newname.equals(oldname)) {
            RDFTerm newNode = ValueOrDefault(newBlankNodes, oldname, null);
            if (newNode == null) {
              newNode = RDFTerm.FromBlankNode(newname);
              bnodeLabels.put(newname, newNode);
              newBlankNodes.put(oldname, newNode);
            }
            subj = newNode;
            changed = true;
          }
        }
        RDFTerm obj = triple.GetObject();
        if (obj.GetKind() == RDFTerm.BLANK) {
          String oldname = obj.GetValue();
          String newname = SuggestBlankNodeName(
              oldname,
              nodeindex,
              bnodeLabels);
          if (!newname.equals(oldname)) {
            RDFTerm newNode = ValueOrDefault(newBlankNodes, oldname, null);
            if (newNode == null) {
              newNode = RDFTerm.FromBlankNode(newname);
              bnodeLabels.put(newname, newNode);
              newBlankNodes.put(oldname, newNode);
            }
            obj = newNode;
            changed = true;
          }
        }
        if (changed) {
          RDFTriple[] newTriple = new RDFTriple[] {
            triple,
            new RDFTriple(subj, triple.GetPredicate(), obj),
          };
          changedTriples.add(newTriple);
        }
      }
      for (RDFTriple[] triple2 : changedTriples) {
        RDFTriple[] t2 = triple2;
        triples.remove(t2[0]);
        triples.add(t2[1]);
      }
    }

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

    private static String SuggestBlankNodeName(
      String node,
      int[] nodeindex,
      Map<String, RDFTerm> bnodeLabels) {
      boolean validnode = node.length() > 0;
      // Check if the blank node label is valid
      // under N-Triples
      for (int i = 0; i < node.length(); ++i) {
        int c = node.charAt(i);
        if (i == 0 && !((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
          validnode = false;
          break;
        }
        if (i >= 0 && !((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
          (c >= 'a' && c <= 'z'))) {
          validnode = false;
          break;
        }
      }
      if (validnode) {
        return node;
      }
      while (true) {
        // Generate a new blank node label,
        // and ensure it's unique
        node = "b" + IntToString(nodeindex[0]);
        if (!bnodeLabels.containsKey(node)) {
          return node;
        }
        ++nodeindex[0];
      }
    }

    private RDFInternal() {
    }
  }
