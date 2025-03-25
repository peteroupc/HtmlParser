package com.upokecenter.util;
/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

import java.util.*;

  /**
   * A class for holding tasks that can be referred to by integer index.
   * @param <T> Type parameter not documented yet.
   */
  public final class ActionList<T> {
    private List<IBoundAction<T>> actions;
    private List<Object> boundObjects;
    private List<T[]> postponeCall;
    private Object syncRoot = new Object();

    /**
     * Initializes a new instance of the {@link ActionList} class.
     */
    public ActionList() {
      this.actions = new ArrayList<IBoundAction<T>>();
      this.boundObjects = new ArrayList<Object>();
      this.postponeCall = new ArrayList<T[]>();
    }

    /**
     * Not documented yet.
     * @param actionID The parameter {@code actionID} is a 32-bit signed integer.
     * @param boundObject The parameter {@code boundObject} is a object object.
     * @return Either {@code true} or {@code false}.
     */
    public boolean RebindAction(int actionID, Object boundObject) {
      // System.out.println("Rebinding action %d",actionID);
      IBoundAction<T> action = null;
      if (actionID < 0 || boundObject == null) {
        return false;
      }
      T[] postponed = null;
      synchronized (this.syncRoot) {
        if (actionID >= this.actions.size()) {
          return false;
        }
        action = this.actions.get(actionID);
        if (action == null) {
          return false;
        }
        this.boundObjects.set(actionID, boundObject);
        postponed = this.postponeCall.get(actionID);
        if (postponed != null) {
          this.actions.set(actionID, null);
          this.postponeCall.set(actionID, null);
          this.boundObjects.set(actionID, null);
        }
      }
      if (postponed != null) {
        // System.out.println("Calling postponed action %d",actionID);
        action.Action(boundObject, postponed);
      }
      return true;
    }

    /**
     * Not documented yet.
     * @param boundObject The parameter {@code boundObject} is a object object.
     * @param action The parameter {@code action} is
     * a.getUpokecenter().getUtil().getIBoundAction() {`0} object.
     * @return A 32-bit signed integer.
     */
    public int RegisterAction(Object boundObject, IBoundAction<T> action) {
      synchronized (this.syncRoot) {
        for (int i = 0; i < this.actions.size(); ++i) {
          if (this.actions.get(i) == null) {
            // System.out.println("Adding action %d",i);
            this.actions.set(i, action);
            this.boundObjects.set(i, boundObject);
            this.postponeCall.set(i, null);
            return i;
          }
        }
        int ret = this.actions.size();
        // System.out.println("Adding action %d",ret);
        this.actions.add(action);
        this.boundObjects.add(boundObject);
        this.postponeCall.add(null);
        return ret;
      }
    }

    /**
     * Not documented yet.
     * @param actionID The parameter {@code actionID} is a 32-bit signed integer.
     * @return Either {@code true} or {@code false}.
     */
    public boolean RemoveAction(int actionID) {
      // System.out.println("Removing action %d",actionID);
      if (actionID < 0) {
        return false;
      }
      synchronized (this.syncRoot) {
        if (actionID >= this.actions.size()) {
          return false;
        }
        this.actions.set(actionID, null);
        this.boundObjects.set(actionID, null);
        this.postponeCall.set(actionID, null);
      }
      return true;
    }

    /**
     * Not documented yet.
     * @param actionID The parameter {@code actionID} is a 32-bit signed integer.
     * @param parameters The parameter {@code parameters} is a `0[] object.
     * @return Either {@code true} or {@code false}.
     */
    public boolean TriggerActionOnce(int actionID, T... parameters) {
      // System.out.println("Triggering action %d",actionID);
      IBoundAction<T> action = null;
      if (actionID < 0) {
        return false;
      }
      Object boundObject = null;
      synchronized (this.syncRoot) {
        if (actionID >= this.actions.size()) {
          return false;
        }
        boundObject = this.boundObjects.get(actionID);
        if (boundObject == null) {
          // System.out.println("Postponing action %d",actionID);
          this.postponeCall.set(actionID, parameters);
          return false;
        }
        action = this.actions.get(actionID);
        this.actions.set(actionID, null);
        this.boundObjects.set(actionID, null);
        this.postponeCall.set(actionID, null);
      }
      if (action == null) {
        return false;
      }
      action.Action(boundObject, parameters);
      return true;
    }

    /**
     * Not documented yet.
     * @param actionID The parameter {@code actionID} is a 32-bit signed integer.
     * @return Either {@code true} or {@code false}.
     */
    public boolean UnbindAction(int actionID) {
      // System.out.println("Unbinding action %d",actionID);
      IBoundAction<T> action = null;
      if (actionID < 0) {
        return false;
      }
      synchronized (this.syncRoot) {
        if (actionID >= this.actions.size()) {
          return false;
        }
        action = this.actions.get(actionID);
        if (action == null) {
          return false;
        }
        this.boundObjects.set(actionID, null);
      }
      return true;
    }
  }
