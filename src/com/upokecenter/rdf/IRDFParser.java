// Written by Peter Occil, 2013. In the public domain.
// Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
package com.upokecenter.rdf;

import java.io.IOException;
import java.util.Set;

public interface IRDFParser {
	public Set<RDFTriple> parse() throws IOException;
}
