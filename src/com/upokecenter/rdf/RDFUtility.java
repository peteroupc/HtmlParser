/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
*/


package com.upokecenter.rdf;

import java.util.Set;

public final class RDFUtility {
  public static boolean areIsomorphic(Set<RDFTriple> graph1, Set<RDFTriple> graph2){
    if(graph1==null)return graph2==null;
    if(graph1.equals(graph2))return true;
    // Graphs must have the same size to be isomorphic
    if(graph1.size()!=graph2.size())return false;
    for(RDFTriple triple : graph1){
      // do a strict comparison
      if(triple.getSubject().getKind()!=RDFTerm.BLANK &&
          triple.getObject().getKind()!=RDFTerm.BLANK){
        if(!graph2.contains(triple))
          return false;
      } else {
        // do a lax comparison
        boolean found=false;
        for(RDFTriple triple2 : graph2){
          if(laxEqual(triple,triple2)){
            found=true;
            break;
          }
        }
        if(!found)return false;
      }
    }
    return true;
  }

  /**
   * A lax comparer of RDF triples which doesn't compare
   * blank node labels
   * 
   * @param a
   * @param b
   * 
   */
  private static boolean laxEqual(RDFTriple a, RDFTriple b){
    if(a==null)return (b==null);
    if(a.equals(b))return true;
    if(a.getSubject().getKind()!=b.getSubject().getKind())
      return false;
    if(a.getObject().getKind()!=b.getObject().getKind())
      return false;
    if(!a.getPredicate().equals(b.getPredicate()))
      return false;
    if(a.getSubject().getKind()!=RDFTerm.BLANK){
      if(!a.getSubject().equals(b.getSubject()))
        return false;
    }
    if(a.getObject().getKind()!=RDFTerm.BLANK){
      if(!a.getObject().equals(b.getObject()))
        return false;
    }
    return true;
  }
  /**
   * 
   * Converts a set of RDF Triples to a JSON object.  The object
   * contains all the subjects, each of which contains a dictionary
   * of predicates for that subject, and each dictionary contains
   * a list of objects for the subject and predicate.  The
   * subject can either be a URI or a blank node (which starts
   * with "_:".
   * 
   * @param triples
   * 
   */
  /*
  public static com.upokecenter.json.JSONObject RDFtoJSON(Set<RDFTriple> triples){
    Map<RDFTerm,List<RDFTriple>> subjects=new HashMap<RDFTerm,List<RDFTriple>>();
    com.upokecenter.json.JSONObject rootJson=new com.upokecenter.json.JSONObject();
    for(RDFTriple triple : triples){
      List<RDFTriple> subjectList=subjects.get(triple.getSubject());
      if(subjectList==null){
        subjectList=new ArrayList<RDFTriple>();
        subjects.put(triple.getSubject(),subjectList);
      }
      subjectList.add(triple);
    }
    for(RDFTerm subject : subjects.keySet()){
      com.upokecenter.json.JSONObject subjectJson=new com.upokecenter.json.JSONObject();
      Map<RDFTerm,List<RDFTerm>> predicates=new HashMap<RDFTerm,List<RDFTerm>>();
      for(RDFTriple triple : triples){
        List<RDFTerm> subjectList=predicates.get(triple.getPredicate());
        if(subjectList==null){
          subjectList=new ArrayList<RDFTerm>();
          predicates.put(triple.getPredicate(),subjectList);
        }
        subjectList.add(triple.getObject());
      }
      for(RDFTerm predicate : predicates.keySet()){
        com.upokecenter.json.JSONArray valueArray=new com.upokecenter.json.JSONArray();
        for(RDFTerm obj : predicates.get(predicate)){
          com.upokecenter.json.JSONObject valueJson=new com.upokecenter.json.JSONObject();
          if(obj.getKind()==RDFTerm.IRI){
            valueJson.put("type","uri");
            valueJson.put("value",obj.getValue());
          } else if(obj.getKind()==RDFTerm.LANGSTRING){
            valueJson.put("type","literal");
            valueJson.put("value",obj.getValue());
            valueJson.put("lang",obj.getTypeOrLanguage());
          } else if(obj.getKind()==RDFTerm.TYPEDSTRING){
            valueJson.put("type","literal");
            valueJson.put("value",obj.getValue());
            if(!obj.isOrdinaryString()) {
              valueJson.put("lang",obj.getTypeOrLanguage());
            }
          } else if(obj.getKind()==RDFTerm.BLANK){
            valueJson.put("type","bnode");
            valueJson.put("value",obj.getValue());
          }
          valueArray.put(valueJson);
        }
        subjectJson.put(predicate.getValue(),valueArray);
      }
      String subjKey=(subject.getKind()==RDFTerm.BLANK ? "_:" : "")+subject.getValue();
      rootJson.put(subjKey,subjectJson);
    }
    return rootJson;
  }
   */
  private RDFUtility(){}
}
