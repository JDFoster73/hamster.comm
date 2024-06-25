/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hamster.comm.ipc.blob.impl;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.slf4j.Logger;

import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.SequentialMessageBlockWriter;
import hamster.comm.buffer.block.itf.ReadAllHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.GrowUpdateExpandableBufferFactory;
import hamster.comm.buffer.factory.StandardExpandableBufferFactoryCreator;
import hamster.comm.ipc.blob.BLOBConsumer;
import hamster.comm.ipc.blob.BLOBManager;
import hamster.comm.ipc.blob.BLOBProducer;

/**
 * The Binary Large Object Manager (BLOB Manager) allows large data blocks to be
 * communicated outside the main Bridge messaging framework.
 * 
 * BLOBs are communicated intra-process by a central, thread-safe BLOB manager
 * which allows code running inside service manager threads (SM or handler) to
 * access large data blocks in a thread-safe manner. BLOBs are communicated
 * inter-process by detecting the BLOB flag in the Bridge messaging header and
 * then transmitting the large object to the target process. The target will
 * recognise the BLOB flag, transfer the BLOB to the BLOB manager and then
 * replace the BLOB data with a BLOB reference.
 * 
 * The class is parameterised with the type of key that active BLOBs are stored
 * against.
 * 
 * @author jdf19
 */
public class BaseBLOBManager implements BLOBManager
{
  /**
   * <p>
   * Rolling cache id. When a BLOB object is added to the cache, it will use a
   * rolling 64bit ID to differentiate it from objects which have the same
   * capacity.
   * <p>
   * It is calculated from the size of this number space that we can create one
   * per nanosecond for over 584 years, which gives no practical chance of
   * collision.
   */
  private long rollingCacheID = 0;
  
  /**
   * <p>
   * Rolling BLOB ID. As with {@link #rollingCacheID}, the huge number space means
   * that there's no practical chance of collision.
   * <p>
   * Instances of the {@link BaseBLOBManager} class are process-wide, meaning that
   * BLOBs received by a process from another process will have different number
   * spaces. Care <b>MUST</b> be taken when transferring BLOB data across
   * processes that blob IDs are not also transferred across processes. A BLOB
   * instance arriving from a remote process <b>MUST</b> be given a local BLOB id.
   */
  private long blobID = 0;
  
  /**
   * The BLOB cache stores BLOB objects in weak references that can be cleaned up
   * at any time. If the VM cleans up cached BLOBs then we can simply create new
   * ones. Caching BLOBs in this way is an attempt to use garbage collection
   * efficiently.
   */
  protected final NavigableSet<WeakBLOBReference> blobCache;
  
  /**
   * BLOB reference for BLOB searching. Have a single instance for searches to
   * keep from creating a new one every time.
   */
  private final WeakBLOBReference searchRef = new WeakBLOBReference();
  
  /**
   * The active BLOB map. BLOBs in this map will have concrete references to them
   * so that they can't be garbage collected. They will be active an in use (i.e.
   * a BLOB has been sent from a source handler but not yet processed by the
   * destination handler)
   */
  private final Map<Long, AbstractBaseBLOBInstance> activeBlobs;
  
  /**
   * Reference queue of collected BLOB objects. Used to monitor weak reference
   * collection.
   */
  protected final ReferenceQueue<BLOBBlockBufferProvider> refManagementQueue;
  
  /**
   * Buffer factory. This factory instance is used to construct ByteBuffer
   * instances for each BLOB constructed.
   */
  private final GrowUpdateExpandableBufferFactory bufferFact = StandardExpandableBufferFactoryCreator.nonDirectBufferFactory();
  
  /**
   * Logger.
   */
  private final Logger logger;
  
  /**
   * Create BLOBManager storage for this process.
   * 
   * @param logger logger for logging BLOB operations.
   */
  public BaseBLOBManager(Logger logger)
  {
    // Create the reference management queue.
    refManagementQueue = new ReferenceQueue<>();
    
    // Create the blob cache.
    blobCache = new TreeSet<>();
    
    // Create the active blob map.
    activeBlobs = new HashMap<>();
    
    // Save logger reference.
    this.logger = logger;
  }
  
  /**
   * Check the reference queue. If any items have been collected (the referent
   * reference is null), remove the weak reference from the blob cache.
   */
  protected void cleanup()
  {
    Reference<? extends BLOBBlockBufferProvider> ref;
    while ((ref = refManagementQueue.poll()) != null)
    {
      // Remove the reference.
      if (!blobCache.remove(ref))
      {
        throw new RuntimeException();
      }
    }
  }
  
  /**
   * Find a BLOB object in the cache with a capacity larger than the given
   * indicative size.
   * 
   * @param indicativeSize
   * @return
   */
  private BLOBBlockBufferProvider getHigherCapacityFromCache(int indicativeSize)
  {
    // BLOB reference - weak reference to a (GC-collectable) BLOB buffer.
    WeakBLOBReference BLOBReference;
    
    // Set up the search reference.
    searchRef.setCapacity(indicativeSize).setID(0);
    
    // Search the cache for a BLOB instance entry of equal or higher capacity than
    // the indicated size from the caller.
    while ((BLOBReference = blobCache.ceiling(searchRef)) != null)
    {
      // Remove reference. It either contains a BLOB instance or null, either way it
      // needs to
      // come out of the cache.
      blobCache.remove(BLOBReference);
      
      // Get BLOB ref.
      BLOBBlockBufferProvider BLOBInstance = BLOBReference.get();
      
      // Check it hasn't been collected.
      if (BLOBInstance != null)
      {
        // Return it.
        return BLOBInstance;
      }
    }
    
    // Nothing doing - return null;
    return null;
  }
  
  /**
   * <p>
   * Construct a BLOB object. Either find a BLOB in the cache or create a new one,
   * and then store the BLOB in the active BLOB map.
   * 
   * @param indicativeSize the indicative required size of the BLOB in bytes. A
   *                       BLOB will grow beyond this size if required; the value
   *                       is simply meant to select the most appropriately-sized
   *                       BLOB in the cache.
   */
  public synchronized BLOBProducer constructBLOB(int indicativeSize)
  {
    
    BLOBBlockBufferProvider backingBuffer = getBackingBuffer(indicativeSize);
    // Return BLOB ID.
    AbstractBaseBLOBInstance internalBLOBReference = new BLOBProducerInstance(blobID, backingBuffer);
    
    // Put into the active blobs.
    // Store the BLOB. We have a STRONG REFERENCE to the buffer.
    addBLOB(blobID, internalBLOBReference);
    
    // Increment BLOB ID.
    blobID++;
    
    // Return the BLOBProducer.
    return internalBLOBReference;
  }
  
  /**
   * <p>
   * Construct a BLOB object. Either find a BLOB in the cache or create a new one,
   * and then store the BLOB in the active BLOB map.
   * 
   * @param indicativeSize the indicative required size of the BLOB in bytes. A
   *                       BLOB will grow beyond this size if required; the value
   *                       is simply meant to select the most appropriately-sized
   *                       BLOB in the cache.
   * @param wholeData      produce all BLOB data in one operation and return the
   *                       associated consumer.
   */
  public synchronized BLOBConsumer constructBLOB(int indicativeSize, WriteBlockHandler wholeData)
  {
    //Next Blob ID.
    long thisBlobID = blobID++;
    
    BLOBBlockBufferProvider backingBuffer = getBackingBuffer(indicativeSize);
    // Return BLOB ID.
    BLOBProducerInstance internalBLOBReference = new BLOBProducerInstance(thisBlobID, backingBuffer);
    
    // Put into the active blobs.
    // Store the BLOB. We have a STRONG REFERENCE to the buffer.
    addBLOB(thisBlobID, internalBLOBReference);
    
    // Generate the BLOB data.
    internalBLOBReference.produceFromHandler(wholeData);
    
    // Return the BLOBConsumer. Increment the blobID AFTER we've retrieved the blob
    // (POST-increment).
    return getConsumer(thisBlobID);
  }
  
  /**
   * <p>
   * Constructing a BLOB instance - get a backing buffer for the BLOB. If one
   * doesn't exist in the cache then create it.
   * 
   * @param indicativeSize an indication of the required size of the BLOB buffer.
   * @return
   */
  private BLOBBlockBufferProvider getBackingBuffer(int indicativeSize)
  {
    // Clean the cache up. Check we haven't got any references to cached objects
    // which have already been collected.
    cleanup();
    
    // Check for a cached BLOB buffer.
    BLOBBlockBufferProvider backingBuffer = getHigherCapacityFromCache(indicativeSize);
    
    // Have we managed to get a concrete reference to a BLOB buffer?
    if (backingBuffer == null)
    {
      // No buffer available. Create a new one.
      bufferFact.setGrowMultiplier(1.25F);
      bufferFact.setInitialBufferSize(indicativeSize);
      backingBuffer = new BLOBBlockBufferProvider(bufferFact, logger);
    }
    
    // Clean out any unused data from any previous use.
    backingBuffer.clearAll();
    
    // Return the provider.
    return backingBuffer;
  }
  
  /**
   * <p>
   * Dispose with BLOB object. Remove the concrete reference from the active BLOBs
   * map and check back in to the cache.
   * 
   * @param BLOBID the unique ID for the active BLOB. This <b>must</b> have been
   *               previously declared with a call to
   *               {@link #constructBLOB(int, long)} with that same BLOBID.
   */
  private synchronized void disposeBLOB(long ref)
  {
    // Get the BLOB object.
    AbstractBaseBLOBInstance removeBLOB = removeBLOB(ref);
    
    // Cache the buffer.
    BLOBBlockBufferProvider br = removeBLOB.BLOBData;
    blobCache.add(new WeakBLOBReference(refManagementQueue, br).setCapacity(br.getCapacity()).setID(rollingCacheID++));
  }
  
  /**
   * {@inheritDoc}
   */
  public synchronized BLOBConsumer retrieveConsumer(SequentialMessageBlockReader reader)// long blobID)
  {
    // Get BLOB id from reader.
    long blobID = reader.consumeLong();
    
    return getConsumer(blobID);
    
//    // Get the long id from the reader.
//    AbstractBaseBLOBInstance BLOBInstance = retrieveBLOB(blobID);
//    
//    //Test for null.
//    if(BLOBInstance == null) throw new IllegalArgumentException();
//    
//    //All OK - return a BLOBConsumer for the data.
//    return new BLOBConsumerInstance(BLOBInstance);
  }
  
  /**
   * <p>
   * BLOB Manager internal method for retrieving a BLOBConsumer.
   * 
   * @param id
   * @return
   */
  private synchronized BLOBConsumer getConsumer(long id)
  {
    // Get the long id from the reader.
    AbstractBaseBLOBInstance BLOBInstance = retrieveBLOB(id);
    
    // Test for null.
    if (BLOBInstance == null) throw new IllegalArgumentException();
    
    // All OK - return a BLOBConsumer for the data.
    return new BLOBConsumerInstance(BLOBInstance);
  }
  
//  /**
//   * {@inheritDoc}
//   */
//  public int identifierSize()
//  {
//    return Long.BYTES;
//  }
//  
  /**
   * <p>
   * 
   * @param sourceHALO
   * @param destHALO
   * @return
   */
  private void addBLOB(long BLOBID, AbstractBaseBLOBInstance blobInst)
  {
    // Get the BLOB for BLOB ID.
    activeBlobs.put(BLOBID, blobInst);
  }
  
  /**
   * <p>
   * 
   * @param sourceHALO
   * @param destHALO
   * @return
   */
  private AbstractBaseBLOBInstance removeBLOB(long BLOBID)
  {
    // Get the BLOB for BLOB ID.
    return activeBlobs.remove(BLOBID);
  }
  
  /**
   * <p>
   * 
   * @param sourceHALO
   * @param destHALO
   * @return
   */
  private AbstractBaseBLOBInstance retrieveBLOB(long BLOBID)
  {
    // Get the BLOB for BLOB ID.
    return activeBlobs.get(BLOBID);
  }
  
  /**
   * <p>
   * Implementation of abstract BLOB instance for single target. Once the BLOB has
   * been finished by its target consumer handler then it will be disposed.
   * 
   * @author jdf19
   *
   */
  private class BLOBProducerInstance extends AbstractBaseBLOBInstance
  {
    /**
     * <p>
     * True if the BLOB has been fully consumed and finished by all target
     * recipients.
     */
    private boolean finished = false;
    
    private final long id;
    
    public BLOBProducerInstance(long id, BLOBBlockBufferProvider BLOBData)
    {
      super(BLOBData);
      this.id = id;
    }
    
    @Override
    protected void check() throws IllegalStateException
    {
      if (finished) throw new IllegalStateException();
    }
    
    @Override
    public void finish()
    {
      disposeBLOB(id);
    }
    
//    @Override
//    public void serialiseID(SequentialMessageBlockWriter writer)
//    {
//      writer.produceLong(id);
//    }
    
    @Override
    public BLOBConsumer getConsumer()
    {
      return new BLOBConsumerInstance(this);
    }

    /**
     * <p>The BLOBProducer uses the {@link WriteBlockHandler#writeMessageBlock(SequentialMessageBlockWriter)} method
     * to produce the BLOB's id to a writer.
     */
    @Override
    public void writeMessageBlock(SequentialMessageBlockWriter writer)
    {
      writer.produceLong(id);
    }
  }
  
  /**
   * <p>
   * BLOB consumer implementation. This class implements {@link BLOBConsumer} and
   * allows BLOB data to be consumed in a variety of ways.
   * 
   * @author jdf19
   *
   */
  private class BLOBConsumerInstance implements BLOBConsumer
  {
    private final AbstractBaseBLOBInstance BLOBConsumerinstance;
    
    /**
     * <p>Maintain the finished status, so that subsequent calls to {@link #finish()} can be ignored.
     */
    private boolean isFinished = false;
    
    public BLOBConsumerInstance(AbstractBaseBLOBInstance multiConsumerBLOB)
    {
      this.BLOBConsumerinstance = multiConsumerBLOB;
    }
    
    @Override
    public int drainBufferToChannel(WritableByteChannel channel) throws IOException
    {
      return BLOBConsumerinstance.BLOBData.drainBufferToChannel(channel);
    }
    
    @Override
    public int drainBufferToChannel(WritableByteChannel channel, int maxBytesToSend) throws IOException
    {
      return BLOBConsumerinstance.BLOBData.drainBufferToChannel(channel, maxBytesToSend);
    }
    
    @Override
    public boolean hasDataToConsume()
    {
      return BLOBConsumerinstance.BLOBData.hasDataToConsume();
    }
    
    @Override
    public int getBLOBLen()
    {
      return BLOBConsumerinstance.BLOBData.readableBytesInBuffer();
    }
    
    @Override
    public void finish()
    {
      if(!isFinished)
      {
        //Finish the underlying BLOB instance.
        BLOBConsumerinstance.finish();
        //Set the finished flag.
        isFinished = true;
      }
    }
    
    @Override
    public void consumeStartAllBufferData(ReadAllHandler callback)
    {
      BLOBConsumerinstance.BLOBData.consumeStartAllBufferData(callback);
    }
    
    @Override
    public void consumeNextBufferData(ReadAllHandler callback)
    {
      BLOBConsumerinstance.BLOBData.consumeNextBufferData(callback);
    }
    
    @Override
    public int readableBytesInBuffer()
    {
      return BLOBConsumerinstance.BLOBData.readableBytesInBuffer();
    }
    
    @Override
    public void setAllConsumable()
    {
      BLOBConsumerinstance.BLOBData.setAllConsumable();
    }
  }
}
