package com.upokecenter.html.data;

import java.util.*;

import com.upokecenter.rdf.*;

  final class RDFInternal {
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
            RDFTerm newNode = newBlankNodes.get(oldname);
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
            RDFTerm newNode = newBlankNodes.get(oldname);
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
      for (RDFTriple[] triple : changedTriples) {
        triples.remove(triple.get(0));
        triples.add(triple.get(1));
      }
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
        node = "b" + (nodeindex[0]).toString();
        if (!bnodeLabels.containsKey(node)) {
          return node;
        }
        ++nodeindex[0];
      }
    }

    private RDFInternal() {
    }
  }
