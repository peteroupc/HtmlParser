/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
 */
package com.upokecenter.io;

import java.io.IOException;

public interface IMarkableCharacterInput extends ICharacterInput {

	public int getMarkPosition();

	public void setMarkPosition(int pos) throws IOException;

	public int setSoftMark();

	public int setHardMark();

	public void moveBack(int count) throws IOException;

}