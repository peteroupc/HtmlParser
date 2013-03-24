package com.upokecenter.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * A character input stream where additional inputs can be stacked on
 * top of it.  It supports advanced marking capabilities.
 * 
 * @author Peter
 *
 */
public final class StackableCharacterInput implements IMarkableCharacterInput {


	private static class InputAndBuffer implements ICharacterInput {

		int[] buffer;
		ICharacterInput charInput;
		int pos=0;

		public InputAndBuffer(ICharacterInput charInput, int[] buffer, int offset, int length){
			this.charInput=charInput;
			if(length>0){
				this.buffer=new int[length];
				System.arraycopy(buffer,offset,this.buffer,0,length);
			} else {
				this.buffer=null;
			}
		}

		@Override
		public int read(int[] buf, int offset, int unitCount)
				throws IOException {
			if(buf==null)throw new IllegalArgumentException();
			if(offset<0 || unitCount<0 || offset+unitCount>buf.length)
				throw new IndexOutOfBoundsException();
			if(unitCount==0)return 0;
			int count=0;
			if(charInput!=null){
				int c=charInput.read(buf,offset,unitCount);
				if(c<=0){
					charInput=null;
				} else {
					offset+=c;
					unitCount-=c;
					count+=c;
				}
			}
			if(buffer!=null){
				int c=Math.min(unitCount,this.buffer.length-pos);
				if(c>0){
					System.arraycopy(this.buffer,pos,buf,offset,c);
				}
				pos+=c;
				count+=c;
				if(c==0){
					buffer=null;
				}
			}
			return (count==0) ? -1 : count;
		}

		@Override
		public int read() throws IOException {
			if(charInput!=null){
				int c=charInput.read();
				if(c>=0)return c;
				charInput=null;
			}
			if(buffer!=null){
				if(pos<buffer.length)
					return buffer[pos++];
				buffer=null;
			}
			return -1;
		}

	}

	int pos=0;
	int endpos=0;
	boolean haveMark=false;
	int[] buffer=null;
	List<ICharacterInput> stack=new ArrayList<ICharacterInput>();

	public StackableCharacterInput(ICharacterInput source) {
		this.stack.add(source);
	}

	@Override
	public int getMarkPosition(){
		return pos;
	}

	public void pushInput(ICharacterInput input){
    if((input)==null)throw new NullPointerException("input");
		// Move unread characters in buffer, since this new
		// input sits on top of the existing input
		stack.add(new InputAndBuffer(input,buffer,pos,endpos-pos));
		endpos=pos;
	}

	@Override
	public void setMarkPosition(int pos) throws IOException{
		if(!haveMark || pos<0 || pos>endpos)
			throw new IOException();
		this.pos=pos;
	}

	@Override
	public int markIfNeeded(){
		if(!haveMark){
			markToEnd();
		}
		return getMarkPosition();
	}

	@Override
	public int markToEnd(){
		if(buffer==null){
			buffer=new int[16];
			pos=0;
			endpos=0;
			haveMark=true;
		} else if(haveMark){
			// Already have a mark; shift buffer to the new mark
			if(pos>0 && pos<endpos){
				System.arraycopy(buffer,pos,buffer,0,endpos-pos);
			}
			endpos-=pos;
			pos=0;
		} else {
			pos=0;
			endpos=0;
			haveMark=true;
		}
		return 0;
	}

	private int readInternal(int[] buf, int offset, int unitCount) throws IOException {
		if(this.stack.size()==0)return -1;
    assert ((buf)!=null) : "buf";
assert ((offset)>=0) : ("offset not greater or equal to 0 ("+Integer.toString(offset)+")");
assert ((unitCount)>=0) : ("unitCount not greater or equal to 0 ("+Integer.toString(unitCount)+")");
assert ((offset+unitCount)<=buf.length) : ("offset+unitCount not less or equal to "+Integer.toString(buf.length)+" ("+Integer.toString(offset+unitCount)+")");
		if(unitCount==0)return 0;
		int count=0;
		while(this.stack.size()>0 && unitCount>0){
			int index=this.stack.size()-1;
			int c=this.stack.get(index).read(buf,offset,unitCount);
			if(c<=0){
				this.stack.remove(index);
				continue;
			}
			count+=c;
			unitCount-=c;
			if(unitCount==0){
				break;
			}
			this.stack.remove(index);
		}
		return count;
	}

	private int readInternal() throws IOException {
		if(this.stack.size()==0)return -1;
		while(this.stack.size()>0){
			int index=this.stack.size()-1;
			int c=this.stack.get(index).read();
			if(c==-1){
				this.stack.remove(index);
				continue;
			}
			return c;
		}
		return -1;
	}

	@Override
	public int read(int[] buf, int offset, int unitCount) throws IOException {
		if(haveMark){
      if((buf)==null)throw new NullPointerException("buf");
if((offset)<0)throw new IndexOutOfBoundsException("offset not greater or equal to 0 ("+Integer.toString(offset)+")");
if((unitCount)<0)throw new IndexOutOfBoundsException("unitCount not greater or equal to 0 ("+Integer.toString(unitCount)+")");
if((offset+unitCount)>buf.length)throw new IndexOutOfBoundsException("offset+unitCount not less or equal to "+Integer.toString(buf.length)+" ("+Integer.toString(offset+unitCount)+")");
			if(unitCount==0)return 0;
			// Read from buffer
			if(pos+unitCount<=endpos){
				System.arraycopy(buffer,pos,buf,offset,unitCount);
				pos+=unitCount;
				return unitCount;
			}
			// End pos is smaller than buffer size, fill
			// entire buffer if possible
			int count=0;
			if(endpos<buffer.length){
				count=readInternal(buffer,endpos,buffer.length-endpos);
				//DebugUtility.log("%s",this);
				if(count>0) {
					endpos+=count;
				}
			}
			int total=0;
			// Try reading from buffer again
			if(pos+unitCount<=endpos){
				System.arraycopy(buffer,pos,buf,offset,unitCount);
				pos+=unitCount;
				return unitCount;
			}
			// expand the buffer
			if(pos+unitCount>buffer.length){
				int[] newBuffer=new int[(buffer.length*2)+unitCount];
				System.arraycopy(buffer,0,newBuffer,0,buffer.length);
				buffer=newBuffer;
			}
			count=readInternal(buffer, endpos, Math.min(unitCount,buffer.length-endpos));
			if(count>0) {
				endpos+=count;
			}
			// Try reading from buffer a third time
			if(pos+unitCount<=endpos){
				System.arraycopy(buffer,pos,buf,offset,unitCount);
				pos+=unitCount;
				total+=unitCount;
			} else if(endpos>pos){
				System.arraycopy(buffer,pos,buf,offset,endpos-pos);
				total+=(endpos-pos);
				pos=endpos;
			}
			return (total==0) ? -1 : total;
		} else
			return readInternal(buf, offset, unitCount);
	}

	@Override
	public int read() throws IOException{
		if(haveMark){
			// Read from buffer
			if(pos<endpos)
				return buffer[pos++];
			//DebugUtility.log(this);
			// End pos is smaller than buffer size, fill
			// entire buffer if possible
			if(endpos<buffer.length){
				int count=readInternal(buffer,endpos,buffer.length-endpos);
				if(count>0) {
					endpos+=count;
				}
			}
			// Try reading from buffer again
			if(pos<endpos)
				return buffer[pos++];
			//DebugUtility.log(this);
			// No room, read next character and put it in buffer
			int c=readInternal();
			if(c<0)return c;
			if(pos>=buffer.length){
				int[] newBuffer=new int[buffer.length*2];
				System.arraycopy(buffer,0,newBuffer,0,buffer.length);
				buffer=newBuffer;
			}
			//DebugUtility.log(this);
			buffer[pos++]=(byte)c;
			endpos++;
			return c;
		} else
			return readInternal();
	}

	@Override
	public void moveBack(int count) throws IOException {
    if((count)<0)throw new IndexOutOfBoundsException("count not greater or equal to 0 ("+Integer.toString(count)+")");
		if(haveMark && pos>=count){
			pos-=count;
			return;
		}
		throw new IOException();
	}

}
