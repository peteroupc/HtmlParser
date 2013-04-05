/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
*/
package com.upokecenter.io;

import java.io.IOException;

import com.upokecenter.util.IntList;


public final class IntListCharacterInput implements ICharacterInput {

	int pos;
	IntList ilist;

	public IntListCharacterInput(IntList ilist){
		this.ilist=ilist;
	}

	@Override
	public int read(int[] buf, int offset, int unitCount) throws IOException {
		if(offset<0 || unitCount<0 || offset+unitCount>buf.length)
			throw new IndexOutOfBoundsException();
		if(unitCount==0)return 0;
		int[] arr=this.ilist.array();
		int size=this.ilist.size();
		int count=0;
		while(pos<size && unitCount>0){
			buf[offset]=arr[pos];
			offset++;
			count++;
			unitCount--;
			pos++;
		}
		return count==0 ? -1 : count;
	}

	@Override
	public int read() throws IOException {
		int[] arr=this.ilist.array();
		if(pos<this.ilist.size())
			return arr[pos++];
		return -1;
	}

}
