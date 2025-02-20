package com.upokecenter.rdf;

import java.util.*;

/// <summary>Not documented yet.</summary>
public final class RDFHelper {
private RDFHelper() {
}
    private static final RDFTerm CanonicalBlank = RDFTerm.FromBlankNode("_");

    private static String BlankValue(RDFTriple triple) {
        if (triple.GetSubject().GetKind() == RDFTerm.BLANK) {
          return triple.GetSubject().GetValue();
        }
        return (triple.GetObject().GetKind() == RDFTerm.BLANK) ?
               triple.GetObject().GetValue() : "";
    }

    private static boolean IsSimpleBlank(RDFTriple triple) {
        // Has a blank subject or a blank Object,
        // or has both with the same value
        RDFTerm rdfsubj = triple.GetSubject();
        RDFTerm rdfobj = triple.GetObject();
        boolean subjBlank = rdfsubj.GetKind() == RDFTerm.BLANK;
        boolean objBlank = rdfobj.GetKind() == RDFTerm.BLANK;
        return (subjBlank != objBlank) || (subjBlank && objBlank &&

                rdfsubj.GetValue().equals(rdfobj.GetValue()));
    }

    private static class TermComparer implements Comparator<RDFTerm>
    {
        private static int StringCompare(String x, String y) {
            if (x == null) {
              if (y == null) {
                return 0;
              } else {
                    return -1;
                }
            } else {
                if (y == null) {
                  return 1;
                } else {
                    return x.compareTo(y);
                }
            }
        }
        static int CompareRDFTerm(RDFTerm x, RDFTerm y) {
            if (x.GetKind() != y.GetKind()) {
              return x.GetKind() < y.GetKind() ? -1 : 1;
            }
            int cmp;
            cmp = StringCompare(
                      x.GetValue(),
                      y.GetValue());
            if (cmp != 0) {
              return cmp;
            }

            cmp = StringCompare(
                      x.GetTypeOrLanguage(),
                      y.GetTypeOrLanguage());
            return cmp;
        }
        public int compare(RDFTerm x, RDFTerm y) {
            if (x == null) {
              if (y == null) {
                return 0;
              } else {
                    return -1;
                }
            } else {
                if (y == null) {
                  return 1;
                } else {
                    return CompareRDFTerm(x, y);
                }
            }
        }
    }

    private static class TripleComparer implements Comparator<RDFTriple>
    {
        public int compare(RDFTriple x, RDFTriple y) {
            if (x == null) {
              if (y == null) {
                return 0;
              } else {
                    return -1;
                }
            } else {
                if (y == null) {
                  return 1;
                } else {
                    int cmp;
                    cmp = TermComparer.CompareRDFTerm(x.GetSubject(),
                                                      y.GetSubject());
                    if (cmp != 0) {
                      return cmp;
                    }
                    cmp = TermComparer.CompareRDFTerm(x.GetPredicate(),
                                                      y.GetPredicate());
                    if (cmp != 0) {
                      return cmp;
                    }
                    cmp = TermComparer.CompareRDFTerm(x.GetObject(),
                                                      y.GetObject());
                    return cmp;
                }
            }
        }
    }

    private static class NonUniqueMapping {
        public final int getIndex() { return propVarindex; }
public final void setIndex(int value) { propVarindex = value; }
private int propVarindex;
        public final List<RDFTerm> getMappings() { return propVarmappings; }
private final List<RDFTerm> propVarmappings;
        public final List<Integer> getPerm() { return propVarperm; }
private final List<Integer> propVarperm;
        // NOTE: For another implementation of a permutation
        // iterator, see the documentation for Python's
        // 'itertools' module.
        public void FirstMapping() {
            int count = this.getMappings().size();
            for (int i = 0; i < count; ++i) {
              if (this.getPerm().size() < count) {
                this.getPerm().add(0);
              }
              this.getPerm().set(i, i);
            }
        }
        public boolean NextMapping() {
            // http://www.nayuki.io/page/next-lexicographical-permutation-algorithm
            int count = this.getMappings().size();
            for (var i = count - 1; i >= 0; --i) {
              if (i < count - 1 && this.getPerm().get(i) < this.getPerm().get(i + 1)) {
                 int pv = this.getPerm().get(i);
                 int minv = count;
                 int minpos = 0;
                 for (var j = i + 1; j < count; ++j) {
                   if (this.getPerm().get(j) == pv + 1) {
                       minpos = j;
                       break;
                    } else if (this.getPerm().get(j) > pv && this.getPerm().get(j) < minv) {
                      minv = this.getPerm().get(j);
                      minpos = j;
                    }
                 }
                 int tmp = this.getPerm().get(i);
                 this.getPerm().set(i, this.getPerm().get(minpos));
                 this.getPerm().set(minpos, tmp);
                 int sz = count - (i + 1);
                 for (int k = 0; k < (sz >> 1); k += 1) {
                         tmp = this.getPerm().get(i + 1 + k);
                         this.getPerm().set(i + 1 + k, this.getPerm().get(count - 1 - k));
                         this.getPerm().set(count - 1 - k, tmp);
                  }
                  return true;
               }
            }
            // No more permutations
            return false;
        }
        public NonUniqueMapping() {
            this.propVarmappings = new ArrayList<RDFTerm>();
            this.propVarperm = new ArrayList<Integer>();
            this.setIndex(0);
        }
        public NonUniqueMapping AddTerm(RDFTerm term) {
            this.getMappings().add(term);
            return this;
        }
    }

    private static class ListAndHash<T> {
        private final ArrayList<T> list;
        private int hash;
        private boolean dirty;
        public ListAndHash() {
            this.list = new ArrayList<T>();
            this.hash = 0;
            this.dirty = true;
        }
        public void Add(T v) {
            this.list.add(v);
            this.dirty = true;
        }
        public void Sort(Comparator<T> comparer) {
            java.util.Collections.sort(this.list, comparer);
            this.dirty = true;
        }
        @Override @SuppressWarnings("unchecked")
public boolean equals(Object obj) {
            ListAndHash<T> other = ((obj instanceof ListAndHash<?>) ? (ListAndHash<T>)obj : null);
            if (obj == null) {
              return false;
            }
            if (this.list.size() != other.list.size()) {
              return false;
            }
            for (int i = 0; i < this.list.size(); ++i) {
              if (!this.list.get(i).equals(other.list.get(i))) {
                return false;
              }
            }
            return true;
        }
        @Override public int hashCode() {
            if (this.dirty) {
                this.hash = this.list.size();
                for (int i = 0; i < this.list.size(); ++i) {
                    this.hash = (this.hash * 23);
                    this.hash = (this.hash +
                                          this.list.get(i).hashCode());
                }
                this.dirty = false;
            }
            return this.hash;
        }
    }

    private static int BlankNodeHash(
      RDFTerm term,
      Map<RDFTerm, List<RDFTriple>> triplesByTerm,
      Map<RDFTerm, Integer> hashes) {
        ArrayList<RDFTerm> stack = new ArrayList<RDFTerm>();
        return BlankNodeHash(term, triplesByTerm, hashes, stack);
    }

    // See Jeremy J. Carroll,
    // "Matching RDF Graphs", 2001.
    private static int BlankNodeHash(
      RDFTerm term,
      Map<RDFTerm, List<RDFTriple>> triplesByTerm,
      Map<RDFTerm, Integer> hashes,
      List<RDFTerm> stack) {
        if (term.GetKind() != RDFTerm.BLANK) {
          return term.hashCode();
        } else if (stack.contains(term)) {
            // Avoid cycles
            return hashes.get(term);
        } else if (false && stack.size() > 9) {
            // Avoid deep recursion
            return hashes.get(term);
        }
        // TODO: Rewrite to nonrecursive version
        stack.add(term);
        // System.out.println("" + stack.size() + " -> " + (term));
        int hash = ((int)0xddff0001);
        int termHashCode = term.hashCode();
        List<RDFTriple> triples = triplesByTerm.get(term);
        for (RDFTriple triple : triples) {
            boolean subjectBlank = triple.GetSubject().GetKind() == RDFTerm.BLANK;
            boolean objectBlank = triple.GetObject().GetKind() == RDFTerm.BLANK;
            if ((subjectBlank && triple.GetSubject().equals(term)) ||
                    (objectBlank && triple.GetObject().equals(term))) {
                int h = 0;
                // Hashes are combined by sum for commutativity,
                // so they won't be sensitive to order of the triples.
                if (!subjectBlank || !triple.GetSubject().equals(term)) {
                    h = (
                            h + 23 * BlankNodeHash(triple.GetSubject(),
                                                   triplesByTerm,
                                                   hashes,
                                                   stack));
                }
                h = (h + (29 * triple.GetPredicate().hashCode()));
                if (!objectBlank || !triple.GetObject().equals(term)) {
                    h = (
                            h + 31 * BlankNodeHash(triple.GetObject(),
                                                   triplesByTerm,
                                                   hashes,
                                                   stack));
                }
                hash = (hash + h);
            }
        }
        stack.remove(stack.size() - 1);
        return hash;
    }

    private static RDFTriple CanonicalTriple(RDFTriple triple) {
        RDFTerm rdfsubj = triple.GetSubject();
        RDFTerm rdfobj = triple.GetObject();
        if (rdfsubj.GetKind() != RDFTerm.BLANK &&
                rdfobj.GetKind() != RDFTerm.BLANK) {
          return triple;
        }
        return new RDFTriple(
          rdfsubj.GetKind() == RDFTerm.BLANK ? CanonicalBlank : rdfsubj,
          triple.GetPredicate(),
          rdfobj.GetKind() == RDFTerm.BLANK ? CanonicalBlank : rdfobj);
    }

    /**
     * Returns whether two RDF graphs are isomorphic; that is, they match apart
     * from their blank nodes, and there is a one-to-one mapping from one graph's
     * blank nodes to the other's such that the graphs match exactly when one
     * graph's blank nodes are replaced with the other's.
     * @param triples1 A set of RDF triples making up the first RDF graph.
     * @param triples2 A set of RDF triples making up the second RDF graph.
     * @return Either 'true' if the graphs are isomorphic, or 'false' otherwise.
     */
    public static boolean AreIsomorphic(
        Set<RDFTriple> triples1,
        Set<RDFTriple> triples2) {
        if (triples1.size() != triples2.size()) {
          return false;
        }
        ArrayList<RDFTriple> blanks1 = new ArrayList<RDFTriple>();
        ArrayList<RDFTriple> blanks2 = new ArrayList<RDFTriple>();
        boolean uniqueBlank1 = true;
        String blankName1 = null;
        boolean uniqueBlank2 = true;
        String blankName2 = null;
        for (RDFTriple triple : triples1) {
            RDFTerm rdfsubj = triple.GetSubject();
            RDFTerm rdfobj = triple.GetObject();
            if (rdfsubj.GetKind() == RDFTerm.BLANK ||
                    rdfobj.GetKind() == RDFTerm.BLANK) {
              if (uniqueBlank1 && rdfsubj.GetKind() == RDFTerm.BLANK) {
                    if (blankName1 != null &&
                            !rdfsubj.GetValue().equals(blankName1)) {
                      uniqueBlank1 = false;
                    } else {
                        blankName1 = rdfsubj.GetValue();
                    }
                }
                if (uniqueBlank1 && rdfobj.GetKind() == RDFTerm.BLANK) {
                  if (blankName1 != null &&
                            !rdfobj.GetValue().equals(blankName1)) {
                    uniqueBlank1 = false;
                  } else {
                        blankName1 = rdfobj.GetValue();
                    }
                }
                blanks1.add(triple);
            } else {
                if (!triples2.contains(triple)) {
                  return false;
                }
            }
        }
        for (RDFTriple triple : triples2) {
            RDFTerm rdfsubj = triple.GetSubject();
            RDFTerm rdfobj = triple.GetObject();
            if (rdfsubj.GetKind() == RDFTerm.BLANK ||
                    rdfobj.GetKind() == RDFTerm.BLANK) {
              if (uniqueBlank2 && rdfsubj.GetKind() == RDFTerm.BLANK) {
                    if (blankName2 != null &&
                            !rdfsubj.GetValue().equals(blankName2)) {
                      uniqueBlank2 = false;
                    } else {
                        blankName2 = rdfsubj.GetValue();
                    }
                }
                if (uniqueBlank2 && rdfobj.GetKind() == RDFTerm.BLANK) {
                  if (blankName2 != null &&
                            !rdfobj.GetValue().equals(blankName2)) {
                    uniqueBlank2 = false;
                  } else {
                        blankName2 = rdfobj.GetValue();
                    }
                }
                blanks2.add(triple);
            } else {
                if (!triples1.contains(triple)) {
                  return false;
                }
            }
        }
        if (blanks1.size() != blanks2.size()) {
          return false;
        }
        if (uniqueBlank1 != uniqueBlank2) {
          return false;
        }
        if (blanks1.size() == 0) {
// No RDF term has blanks
            return true;
        }
        if (blanks1.size() == 1) {
          RDFTriple blank1 = blanks1.get(0);
          RDFTriple blank2 = blanks2.get(0);
          if (!blank1.GetPredicate().equals(blank2.GetPredicate())) {
            return false;
          }
          boolean subjectBlank = false;
          boolean objectBlank = false;
          if (blank1.GetSubject().GetKind() == RDFTerm.BLANK) {
            if (blank1.GetSubject().GetKind() != RDFTerm.BLANK) {
              return false;
            }
            subjectBlank = true;
            } else {
                if (!blank1.GetSubject().equals(blank2.GetSubject())) {
                  return false;
                }
            }
            if (blank1.GetObject().GetKind() == RDFTerm.BLANK) {
              if (blank1.GetObject().GetKind() != RDFTerm.BLANK) {
                return false;
              }
              objectBlank = false;
            } else {
                if (!blank1.GetObject().equals(blank2.GetObject())) {
                  return false;
                }
            }
            if (subjectBlank && objectBlank) {
                return blank1.GetSubject().equals(blank1.GetObject()) ==
                       blank2.GetSubject().equals(blank2.GetObject());
            } else {
                return true;
            }
        }
        if (uniqueBlank1) {
            // One unique blank node in each graph
            HashSet<RDFTriple> blanks2Canonical = new HashSet<RDFTriple>();
            for (RDFTriple blank2 : blanks2) {
              blanks2Canonical.add(CanonicalTriple(blank2));
            }
            if (blanks2Canonical.size() != blanks2.size()) {
              throw new IllegalStateException();
            }
            for (RDFTriple blank1 : blanks1) {
              if (!blanks2Canonical.contains(CanonicalTriple(blank1))) {
                return false;
              }
            }
            return true;
        }
        // Nontrivial cases: More than one triple with a blank node, and
        // more than one unique blank node.
        HashMap<String, ListAndHash<RDFTriple>> simpleBlanks1 = new HashMap<String, ListAndHash<RDFTriple>>();
        HashMap<String, ListAndHash<RDFTriple>> simpleBlanks2 = new HashMap<String, ListAndHash<RDFTriple>>();
        int complexBlankCount1 = 0;
        int complexBlankCount2 = 0;
        for (RDFTriple blank2 : blanks2) {
          if (IsSimpleBlank(blank2)) {
                if (complexBlankCount1 == 0 && complexBlankCount2 == 0) {
                    String bv = BlankValue(blank2);
                    if (!simpleBlanks2.containsKey(bv)) {
                      simpleBlanks2.put(bv, new ListAndHash<RDFTriple>());
                    }
                    simpleBlanks2.get(bv).Add(CanonicalTriple(blank2));
                }
            } else {
                // Complex blank
                ++complexBlankCount2;
            }
        }
        for (RDFTriple blank1 : blanks1) {
          if (IsSimpleBlank(blank1)) {
                if (complexBlankCount1 == 0 && complexBlankCount2 == 0) {
                    String bv = BlankValue(blank1);
                    if (!simpleBlanks1.containsKey(bv)) {
                      simpleBlanks1.put(bv, new ListAndHash<RDFTriple>());
                    }
                    simpleBlanks1.get(bv).Add(CanonicalTriple(blank1));
                }
            } else {
                // Complex blank
                ++complexBlankCount1;
            }
        }
        TripleComparer comparer = new TripleComparer();
        HashMap<String, String> blank2To1 = new HashMap<String, String>();
        if (complexBlankCount1 == 0 && complexBlankCount2 == 0) {
          if (simpleBlanks1.size() != simpleBlanks2.size()) {
            return false;
          }
          for (var k : simpleBlanks1.keySet()) {
            simpleBlanks1.get(k).Sort(comparer);
          }
          for (var k : simpleBlanks2.keySet()) {
                var sb = simpleBlanks2.get(k);
                sb.Sort(comparer);
                String foundKey = null;
                for (var k1 : simpleBlanks1.keySet()) {
                  if (simpleBlanks1.get(k1).hashCode() == sb.hashCode() &&
                            simpleBlanks1.get(k1).equals(sb)) {
                        foundKey = k1;
                        break;
                    }
                }
                if (foundKey != null) {
                  simpleBlanks1.remove(foundKey);
                blank2To1.put(k, foundKey);
                } else {
                    return false;
                }
            }
            return true;
        }
        if (complexBlankCount1 != complexBlankCount2) {
          return false;
        }
        // Implement the isomorphism check in Jeremy J. Carroll,
        // "Matching RDF Graphs", 2001.
        HashMap<RDFTerm, List<RDFTriple>> triplesByTerm1 = new HashMap<RDFTerm, List<RDFTriple>>();
        HashMap<RDFTerm, List<RDFTriple>> triplesByTerm2 = new HashMap<RDFTerm, List<RDFTriple>>();
        for (RDFTriple blank1 : blanks1) {
            RDFTerm subject = blank1.GetSubject();
            RDFTerm rdfObject = blank1.GetObject();
            boolean hasTerm = false;
            if (subject.GetKind() == RDFTerm.BLANK) {
              if (!triplesByTerm1.containsKey(subject)) {
                triplesByTerm1.put(subject, new java.util.ArrayList<RDFTriple>(java.util.Arrays.asList(blank1)));
              } else {
                    List<RDFTriple> triplesList = triplesByTerm1.get(subject);
                    triplesList.add(blank1);
                }
                hasTerm = subject.equals(rdfObject);
            }
            if (rdfObject.GetKind() == RDFTerm.BLANK) {
              if (!triplesByTerm1.containsKey(rdfObject)) {
                triplesByTerm1.put(rdfObject, new java.util.ArrayList<RDFTriple>(java.util.Arrays.asList(blank1)));
              } else if (!hasTerm) {
                List<RDFTriple> triplesList = triplesByTerm1.get(rdfObject);
                  triplesList.add(blank1);
                }
            }
        }
        for (RDFTriple blank2 : blanks2) {
            RDFTerm subject = blank2.GetSubject();
            RDFTerm rdfObject = blank2.GetObject();
            boolean hasTerm = false;
            if (subject.GetKind() == RDFTerm.BLANK) {
              if (!triplesByTerm2.containsKey(subject)) {
                triplesByTerm2.put(subject, new java.util.ArrayList<RDFTriple>(java.util.Arrays.asList(blank2)));
              } else {
                    List<RDFTriple> triplesList = triplesByTerm2.get(subject);
                    triplesList.add(blank2);
                }
                hasTerm = subject.equals(rdfObject);
            }
            if (rdfObject.GetKind() == RDFTerm.BLANK) {
              if (!triplesByTerm2.containsKey(rdfObject)) {
                triplesByTerm2.put(rdfObject, new java.util.ArrayList<RDFTriple>(java.util.Arrays.asList(blank2)));
              } else if (!hasTerm) {
                List<RDFTriple> triplesList = triplesByTerm2.get(rdfObject);
                  triplesList.add(blank2);
                }
            }
        }
        if (triplesByTerm1.size() != triplesByTerm2.size()) {
          return false;
        }
        TermComparer tc = new TermComparer();
        HashMap<RDFTerm, Integer> blankTerms1Hashes = new HashMap<RDFTerm, Integer>();
        HashMap<RDFTerm, Integer> blankTerms2Hashes = new HashMap<RDFTerm, Integer>();
        for (var b : triplesByTerm1.keySet()) {
          blankTerms1Hashes.put(b, triplesByTerm1.get(b).size());
        }
        for (var b : triplesByTerm2.keySet()) {
          blankTerms2Hashes.put(b, triplesByTerm2.get(b).size());
        }
        HashMap<RDFTerm, Integer> blankTerms1Hashes2 = new HashMap<RDFTerm, Integer>();
        HashMap<RDFTerm, Integer> blankTerms2Hashes2 = new HashMap<RDFTerm, Integer>();
        HashMap<Integer, NonUniqueMapping> hashClassCounts1 = new HashMap<Integer, NonUniqueMapping>();
        HashMap<Integer, NonUniqueMapping> hashClassCounts2 = new HashMap<Integer, NonUniqueMapping>();
        int maxClassSize1 = 0;
        int maxClassSize2 = 0;
        for (var b : triplesByTerm1.keySet()) {
            int h = BlankNodeHash(b, triplesByTerm1, blankTerms1Hashes);
            blankTerms1Hashes2.put(b, h);
            if (!hashClassCounts1.containsKey(h)) {
              hashClassCounts1.put(h, new NonUniqueMapping().AddTerm(b));
            } else {
                hashClassCounts1.get(h).AddTerm(b);
            }
            maxClassSize1 = Math.max(maxClassSize1,
                                     hashClassCounts1.get(h).getMappings().size());
            // System.out.println("" + b + "=" + h +":
            // "+System.currentTimeMillis()/10000000.0);
        }
        for (var b : triplesByTerm2.keySet()) {
            int h = BlankNodeHash(b, triplesByTerm2, blankTerms2Hashes);
            blankTerms2Hashes2.put(b, h);
            // if (!hashClassCounts1.containsKey(h)) {
            // return false;
            // }
            if (!hashClassCounts2.containsKey(h)) {
              hashClassCounts2.put(h, new NonUniqueMapping().AddTerm(b));
            } else {
                hashClassCounts2.get(h).AddTerm(b);
            }
            maxClassSize2 = Math.max(maxClassSize2,
                                     hashClassCounts2.get(h).getMappings().size());
            // System.out.println("" + b + "=" + h +":
            // "+System.currentTimeMillis()/10000000.0);
        }
        if (maxClassSize1 != maxClassSize2) {
          return false;
        }
        if (hashClassCounts1.size() != hashClassCounts2.size()) {
          return false;
        }
        // System.out.println("maxClassSize=" + (maxClassSize1));
        {
            HashMap<RDFTerm, RDFTerm> uniqueMapping = new HashMap<RDFTerm, RDFTerm>();
            ArrayList<NonUniqueMapping> nonUniqueMappings1 = new ArrayList<NonUniqueMapping>();
            ArrayList<NonUniqueMapping> nonUniqueMappings2 = new ArrayList<NonUniqueMapping>();
            for (int hash : hashClassCounts1.keySet()) {
              if (!hashClassCounts2.containsKey(hash)) {
                return false;
              }
              var iat1 = hashClassCounts1.get(hash);
              var iat2 = hashClassCounts2.get(hash);
              if (iat1.getMappings().size() != iat2.getMappings().size()) {
                return false;
              }
              for (int i = 0; i < iat1.getMappings().size(); ++i) {
                    RDFTerm term1 = iat1.getMappings().get(i);
                    RDFTerm term2 = iat2.getMappings().get(i);
                    uniqueMapping.put(term1, term2);
                }
                if (iat1.getMappings().size() > 1) {
                  nonUniqueMappings1.add(iat1);
                  nonUniqueMappings2.add(iat2);
                  iat2.FirstMapping();
                }
            }
            while (true) {
                boolean failed = false;
                for (RDFTriple blank : blanks1) {
                    RDFTerm rdfSubj = blank.GetSubject();
                    RDFTerm rdfObj = blank.GetObject();
                    if (rdfSubj.GetKind() == RDFTerm.BLANK) {
                      rdfSubj = uniqueMapping.get(rdfSubj);
                    }
                    if (rdfObj.GetKind() == RDFTerm.BLANK) {
                      rdfObj = uniqueMapping.get(rdfObj);
                    }
                    RDFTriple triple = new RDFTriple(
                        rdfSubj,
                        blank.GetPredicate(),
                        rdfObj);
                    if (!blanks2.contains(triple)) {
                        failed = true;
                        break;
                    }
                }
                if (failed) {
                    // Choose a new mapping to try
                    boolean newMapping = false;
                    for (int k = 0; k < nonUniqueMappings2.size(); ++k) {
                      var numap1 = nonUniqueMappings1.get(k);
                      var numap2 = nonUniqueMappings2.get(k);
                      if (numap2.NextMapping()) {
                         newMapping = true;
                         for (int i = 0; i < numap2.getPerm().size(); ++i) {
                           int pi = numap2.getPerm().get(i);
                           RDFTerm term1 = numap1.getMappings().get(i);
                           RDFTerm term2 = numap2.getMappings().get(pi);
                           uniqueMapping.put(term1, term2);
                         }
                         break;
                      } else {
                        numap2.FirstMapping();
                        for (int i = 0; i < numap2.getPerm().size(); ++i) {
                           int pi = numap2.getPerm().get(i);
                           RDFTerm term1 = numap1.getMappings().get(i);
                           RDFTerm term2 = numap2.getMappings().get(pi);
                           uniqueMapping.put(term1, term2);
                        }
                      }
                    }
                    if (!newMapping) {
                      return false;
                    }
                } else {
                    break;
                }
            }
            return true;
        }
    }
}
