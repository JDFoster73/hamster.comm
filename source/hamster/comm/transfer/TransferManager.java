package hamster.comm.transfer;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import hamster.comm.server.NonBlockingCommunicationApplicationServer;

/**
 * <p>The channel transfer manager provides the ability to move an Object instance 
 * from one selector thread to another within the same process.
 * <p>Certain processes, for example, may wish to transfer connected sockets between {@link NonBlockingCommunicationApplicationServer} instances,
 * for example to balance communicating sockets within different threads.  A socket channel can be unregistered and 
 * provided in a call to {@link #initTransfer(SocketChannel)} which holds it in a process-wide map until a thread
 * holding a different selector calls the {@link #getTransferObject(int)} method to retrieve the connected channel.
 * The channel can then be registered to a different selector.
 * 
 * @author jdf19
 *
 */
public class TransferManager
{
  /**
   * Transfer key cache to avoid key conflicts.
   */
  private static final ArrayList<Integer> keyList = new ArrayList<>();
  
  /**
   * Transfer map maps transfer keys to socket channels.
   */
  private static final Map<Integer, Object> transferMap = new HashMap<>();
  
  /**
   * Incrementable counter for each new transfer key required.
   */
  private static int keyCounter = 1;
  
  /**
   * <p>Initiate transfer of a connected channel from one Thread to another.
   * 
   * @param channel the channel to transfer.
   * @return an integer transfer key that can be sent to a different Thread and used to reference the channel in a {@link #getTransferObject(int)} call.
   */
  public synchronized static int initTransfer(Object channel)
  {
    //Get a transfer key from the list.
    int key = (!keyList.isEmpty()) ? keyList.remove(0) : keyCounter++;

    //Put the channel in the transfer map.
    transferMap.put(key, channel);
    
    //Return the transfer key.
    return key;
  }
  
  /**
   * <p>Request the connected socket channel for the given transfer key.
   * 
   * @param transferKey the transfer key returned in a call to {@link #initTransfer(SocketChannel)}.
   * @return the socket channel for the transfer key.
   * @throws IllegalArgumentException if the transfer key is unknown.
   */
  public synchronized static Object getTransferObject(int transferKey) throws IllegalArgumentException
  {
    //Get the channel object for the transfer key.
    Object sc = transferMap.remove(transferKey);
    
    if(sc == null) throw new IllegalArgumentException();
    
    //Remove mapping and return transfer key to the map.
    keyList.add(transferKey);
    
    //Return the channel mapped to the transfer key.
    return sc;
  }
}
