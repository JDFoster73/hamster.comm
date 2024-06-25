/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.ipc.blob.impl;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * <p>{@link WeakReference} implementation for storing cached BLOB instances in a Map in such a
 * way that they can be collected by the GC.  This is an attempt to have a lot of memory allocation
 * which is instantly recoverable and does not require the GC to manage to multiple generations.
 *  
 * @author jdf19
 */
class WeakBLOBReference extends WeakReference<BLOBBlockBufferProvider> implements Comparable<WeakBLOBReference>
{
  /**
   * <p>The capacity of the referent BLOB object in bytes.
   */
  protected int capacity;
  
  /**
   * <p>Used to distinguish different BLOB instances of the same capacity when stored in 
   * an ordered collection.
   */
  protected long id;
  
  public WeakBLOBReference(ReferenceQueue<BLOBBlockBufferProvider> refQueue, BLOBBlockBufferProvider referent)
  {
    super(referent, refQueue);
    this.capacity = referent.getCapacity();
  } 

  WeakBLOBReference()
  {
    super(null);
  } 

  /**
   * <p>**FOR INTERNAL USE ONLY**
   * <p>Allow this class to be used as a search key.  Allow package classes only to access the capacity.  
   * 
   * @param capacity
   */
  WeakBLOBReference setCapacity(int capacity)
  {
    this.capacity = capacity;
    return this;
  }
  
  /**
   * <p>**FOR INTERNAL USE ONLY**
   * <p>Allow this class to be used as a search key.  Allow package classes only to access the capacity.  
   * 
   * @param capacity
   */
  WeakBLOBReference setID(long id)
  {
    this.id = id;
    return this;
  }

  /**
   * <p>Use the ID field to provide an ordering mechanism for fields that have the same capacity.
   */
  @Override
  public int compareTo(WeakBLOBReference o)
  {
    int capacityCompare = Integer.compare(capacity, o.capacity);
    return (capacityCompare != 0) ? capacityCompare : Long.compare(id, o.id);
  }

  @Override
  public String toString()
  {
    return "BLOBReference [capacity=" + capacity + ", id=" + id + "]";
  }
  
  
}
