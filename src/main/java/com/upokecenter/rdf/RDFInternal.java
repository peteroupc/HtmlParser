package com.upokecenter.rdf;

import java.util.*;

import com.upokecenter.util.*;
/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

  final class RDFInternal {
private RDFInternal() {
}
    /**
     * Not documented yet.
     * @param triples The parameter {@code triples} is
     * a.getCollections().getGeneric().getISet() {PeterO.Rdf.RDFTriple} object.
     * @param bnodeLabels The parameter {@code bnodeLabels} is
     * a.getCollections().getGeneric().getMap() {System.String object.
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
            RDFTerm newNode = newBlankNodes.containsKey(oldname) ?
              newBlankNodes.get(oldname) : null;
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
            RDFTerm newNode = newBlankNodes.containsKey(oldname) ?
              newBlankNodes.get(oldname) : null;
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
        triples.remove(triple[0]);
        triples.add(triple[1]);
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
        // NOTE: Blank nodes that start with a digit are now allowed
        // under N-Triples
        // if (i == 0 && !((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
        // validnode = false;
        // break;
        // }
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
        node = "b" + Integer.toString((int)nodeindex[0]);
        if (!bnodeLabels.containsKey(node)) {
          return node;
        }
        ++nodeindex[0];
      }
    }
  }
