/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
*/
package com.upokecenter.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for holding tasks that can be referred to by integer index.
 */
public final class ActionList<T> {

	private final List<IBoundAction<T>> actions;
	private final List<Object> boundObjects;
	private final List<T[]> postponeCall;
	private final Object syncRoot=new Object();

	public ActionList(){
		actions=new ArrayList<IBoundAction<T>>();
		boundObjects=new ArrayList<Object>();
		postponeCall=new ArrayList<T[]>();
	}

	public boolean rebindAction(int actionID, Object boundObject){
		//DebugUtility.log("Rebinding action %d",actionID);
		IBoundAction<T> action=null;
		if(actionID<0 || boundObject==null)return false;
		T[] postponed=null;
		synchronized(syncRoot){
			if(actionID>=actions.size())
				return false;
			action=actions.get(actionID);
			if(action==null)
				return false;
			boundObjects.set(actionID,boundObject);
			postponed=postponeCall.get(actionID);
			if(postponed!=null){
				actions.set(actionID,null);
				postponeCall.set(actionID,null);
				boundObjects.set(actionID,null);
			}
		}
		if(postponed!=null){
			//DebugUtility.log("Calling postponed action %d",actionID);
			action.action(boundObject,postponed);
		}
		return true;
	}

	public int registerAction(Object boundObject, IBoundAction<T> action){
		synchronized(syncRoot){
			for(int i=0;i<actions.size();i++){
				if(actions.get(i)==null){
					//DebugUtility.log("Adding action %d",i);
					actions.set(i,action);
					boundObjects.set(i,boundObject);
					postponeCall.set(i,null);
					return i;
				}
			}
			int ret=actions.size();
			//DebugUtility.log("Adding action %d",ret);
			actions.add(action);
			boundObjects.add(boundObject);
			postponeCall.add(null);
			return ret;
		}
	}

	public boolean removeAction(int actionID){
		//DebugUtility.log("Removing action %d",actionID);
		if(actionID<0)return false;
		synchronized(syncRoot){
			if(actionID>=actions.size())
				return false;
			actions.set(actionID,null);
			boundObjects.set(actionID,null);
			postponeCall.set(actionID,null);
		}
		return true;
	}

	public boolean triggerActionOnce(int actionID, T... parameters){
		//DebugUtility.log("Triggering action %d",actionID);
		IBoundAction<T> action=null;
		if(actionID<0)return false;
		Object boundObject=null;
		synchronized(syncRoot){
			if(actionID>=actions.size())
				return false;
			boundObject=boundObjects.get(actionID);
			if(boundObject==null){
				//DebugUtility.log("Postponing action %d",actionID);
				postponeCall.set(actionID,parameters);
				return false;
			}
			action=actions.get(actionID);
			actions.set(actionID,null);
			boundObjects.set(actionID,null);
			postponeCall.set(actionID,null);
		}
		if(action==null)return false;
		action.action(boundObject,parameters);
		return true;
	}

	public boolean unbindAction(int actionID){
		//DebugUtility.log("Unbinding action %d",actionID);
		IBoundAction<T> action=null;
		if(actionID<0)return false;
		synchronized(syncRoot){
			if(actionID>=actions.size())
				return false;
			action=actions.get(actionID);
			if(action==null)
				return false;
			boundObjects.set(actionID,null);
		}
		return true;
	}
}
