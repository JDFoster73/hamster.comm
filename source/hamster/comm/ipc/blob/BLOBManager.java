package hamster.comm.ipc.blob;

import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.itf.WriteBlockHandler;

/**
 * <p>Implementations of {@link BLOBManager} provide BLOB creation and retrieval services.
 *  
 * @author jdf19
 *
 */
public interface BLOBManager
{
  /**
   * <p>Deserialise a BLOB identifier from the stream.
   * 
   * @param reader the data stream to deserialise the BLOB identifier from.
   * 
   * @return {@link BLOBConsumer} that corresponds to the deserialised identifier.
   * 
   */
  public BLOBConsumer retrieveConsumer(SequentialMessageBlockReader reader);
  
  /**
   * <p>Construct a BLOB for producing data into. 
   * @param indicativeSize indicative size of the BLOB.  Simply used to select a cached instance if one exists; the instance will be expanded if required.
   * @return a newly-constructed BLOB producer.
   */
  public BLOBProducer constructBLOB(int indicativeSize);

  /**
   * <p>Construct a BLOB for producing data to, which can then be consumed later. 
   * @param indicativeSize indicative size of the BLOB.  Simply used to select a cached instance if one exists; the instance will be expanded if required.
   * @param data the {@link WriteBlockHandler} implementation that will produce the BLOB data to the BLOB instance.  These data can then be used in the {@link BLOBConsumer} methods of the returned instance.
   * @return consumer instance for consuming the blob data.
   */
  public BLOBConsumer constructBLOB(int indicativeSize, WriteBlockHandler data);
}
