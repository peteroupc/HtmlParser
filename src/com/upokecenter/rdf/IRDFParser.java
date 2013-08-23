/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
*/
package com.upokecenter.rdf;

import java.io.IOException;
import java.util.Set;

public interface IRDFParser {
  public Set<RDFTriple> parse() throws IOException;
}
