/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
*/
package com.upokecenter.io;

import java.io.IOException;

public interface IMarkableCharacterInput extends ICharacterInput {

  public int getMarkPosition();

  public void moveBack(int count) throws IOException;

  public int setHardMark();

  public void setMarkPosition(int pos) throws IOException;

  public int setSoftMark();

}