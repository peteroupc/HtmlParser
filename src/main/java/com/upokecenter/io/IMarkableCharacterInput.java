/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

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
