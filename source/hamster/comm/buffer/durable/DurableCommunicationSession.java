package hamster.comm.buffer.durable;

import java.util.ResourceBundle;

import org.slf4j.Logger;

import hamster.comm.RollingCounter;
import hamster.comm.buffer.block.GeneralPipelineBuffer;
import hamster.comm.buffer.block.RetainingFillConsumePipelineBuffer;
import hamster.comm.buffer.block.RetainingProduceDrainPipelineBuffer;
import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.SequentialMessageBlockWriter;
import hamster.comm.buffer.block.itf.ConsumableCalculationBlockHandler;
import hamster.comm.buffer.block.itf.IncomingDataListener;
import hamster.comm.buffer.block.itf.ReadBlockHandler;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;
import hamster.comm.buffer.factory.ExpandableBufferFactory;
import hamster.comm.buffer.factory.StandardExpandableBufferFactoryCreator;
import hamster.comm.server.listener.ReadTransferHandler;

/**
 * <p>The {@link DurableCommunicationSession} class is a base class for durable (one session which transcends multiple connections) session
 * data exchange.  If communications are interrupted (physical but transient network error, OS network reinitialisation, ...) then we may lose
 * connections between two logical endpoints.  We don't receive any automatic notification that send data have reached their destination.
 * If communications are interrupted while data are being sent then some or all of that data could be lost.  This session class adds message framing
 * and control data to a simple stream so that whole messages are wrapped and preceded by a control packet which identifies the message length and
 * a sequential message ID counter.  
 * <p>This class provides a network-neutral mechanism for keeping track of sent and received session data and making sure that any
 * lost data are resent on reconnection.  The responsibility for sending and receiving data to and from the remote endpoint is solely the responsibility
 * of the owning class.
 * 
 * <p>There are two types of data managed by this session base:
 * <ul>
 * <li><b>Communication</b> data: this is raw data sent on network connections containing control and session data.  Session data are sent in discrete packets and control data is used to frame these packets and acknowledge receipt of stream data.
 * <li><b>Session</b> data: these are the data streams sent and received from user endpoints with no control data.  Data sent by one endpoint should appear exactly the same at the other endpoint. 
 * </ul>
 * 
 * <p>This particular implementation holds incoming and outgoing session data internally, i.e. it provides send and receive buffering.  Owning classes
 * can write outgoing data to the internal outgoing buffer and read incoming data from the internal incoming buffer. 
 * @author jdf19
 *
 */
public final class DurableCommunicationSession implements DurableBufferSessionController, OutgoingMessageConstructor
{
  /**
   * Logger for logging events.
   */
  private final Logger logger;
  
  /**
   * Resource strings for logging.
   */
  private final ResourceBundle commSystemBundle;
  
  /**
   * Create a unique session id.  This is purely used to identify the session in the log.  Creating a logging facade for each
   * session object may be better from an encapsulation perspective but it does add overheads.
   */
  private final String sessionID;
  
  /**
   * <p>The connection ID that is used to send INI and ACK messages and validate incoming ACK messages.  The initial connection
   * puts the count at 1.
   */
  private RollingCounter connectionIDCounter = new RollingCounter(1, 0x3ff0, 0x3ff0);
  
  /**
   * <p>The message ID counter that is used to identify messages in a strict sequence.
   */
  private RollingCounter messageIDCounter = new RollingCounter(1, 0x3ff0, 0);

  /**
   * <p>The last message ID received.  Starts at 0 initially, then wraps around from 1 > 0x3ff0.
   */
  private int lastMessageIDReceived = 0;
  
  /**
   * <p>The last message ID sent.  Starts at 0 initially, then wraps around from 1 > 0x3ff0.
   */
  //private int lastMessageIDSent = 0;

  /**
   * <p>Counter which counts up whenever a DAT message is received and is zeroed when an ACK message is sent.  Also zeroed when an INI message
   * is sent.
   */
  private int unacknowledgedReceivedMessages = 0;
  
  /**
   * <p>Sender for sending stream data to the other durable session endpoint.
   */
  private DurableSessionSender sessionSender;
  
  /**
   * <p>Incoming data listener callback - handles incoming message data.  When a message block of <b>application</b> data becomes available
   * (this being a block of user data <b>not</b> stream data, which contains control packets such as ACK and are of no concern to the
   * durable session owner) then this interface will be called for the durable session owner to handle.
   * the user data block.</p>
   */
  private IncomingDataListener sessionDataReceiver;
    
  /**
   * <p>Control packet helper for reading and sending control packets.
   */
  private final DurableSessionControlPacket controlPacket = new DurableSessionControlPacket();
  
  /**
   * <p>If not true then we need to receive an INI packet to correctly position the outgoing stream pointer.  No other packet can
   * be received on a newly-initialised connection.
   */
  private boolean connectionInitialised = false;
  
  /**
   * <p>Single class per durable session instance; handles incoming STREAM data.
   */
  private final ControlPacketHandler controlPacketHandler = new ControlPacketHandler();
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //BUFFERS  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  /**
   * <p>Pipeline buffer to create and send INIT messages to the remote peer upon a new connection.
   */
  private GeneralPipelineBuffer initBuffer;
  
  /**
   * <p>Outgoing buffer - retain buffered data until acknowledged by the remote peer.
   */
  private RetainingProduceDrainPipelineBuffer sendBuffer;
  
  /**
   * <p>Incoming buffer.  This is incoming STREAM data - mixed ACK and DAT packets.  The DAT packet contents (the APPLICATION data) will be isolated and
   * sent to the owner for handling.
   */
  private RetainingFillConsumePipelineBuffer receiveBuffer;
  
  /**
   * <p>Create an instance of a durable communication session.  This will be uninitialised and must first receive
   * and ACK from the remote endpoint.
   * <p>The owner creates the instance with a callback which it supplies to be given incoming data blocks to deal with.
   * The owner and session enjoy a one-to-one relationship and have the same lifecycle; a durable session can't be
   * re-assigned to a different owner.
   * 
   * @param logger for logging events to the system logger.
   * @param sessionID the session identifier for logging.
   * @param bufferFact buffer factory for initialising and expanding the outgoing buffer.
   * @param sessionDataReceiver the callback that will receive incoming session data.
   */
  public DurableCommunicationSession(Logger logger, String sessionID, ExpandableBufferFactory bufferFact, IncomingDataListener sessionDataReceiver)
  {
    //Read event receiver can't be null.
    //
    //if(readEventReceiver == null) throw new NullPointerException();
    initBuffer = new GeneralPipelineBuffer(StandardExpandableBufferFactoryCreator.directBufferFactory(DurableSessionControlPacket.PACKET_SIZE), "DBS_INI" + sessionID, logger);
    sendBuffer = new RetainingProduceDrainPipelineBuffer(StandardExpandableBufferFactoryCreator.directBufferFactory(30000), "DBS_SND" + sessionID, logger);
    receiveBuffer = new RetainingFillConsumePipelineBuffer(StandardExpandableBufferFactoryCreator.directBufferFactory(30000), "DBS_RCV" + sessionID, logger);
    
    //Store logger.
    this.logger = logger;
    
    //Store bundle.
    this.commSystemBundle = ResourceBundle.getBundle("hamster.comm.i18n.strings");
    
    //Store session id.
    this.sessionID = sessionID;
    
    //Store incoming data listener.
    this.sessionDataReceiver = sessionDataReceiver;
  }

  /**
   * <p>Update the session receiver.  Sometimes, the session receiver can only be created after the durable session has been set up,
   * for chicken/egg situations which require complex proxying.  This method allows the update of the session receiver after creation.
   * 
   * @param readEventReceiver the incoming data listener which will receive local durable session updates.
   * @return this instance reference for telescoping calls.
   */
  public DurableCommunicationSession updateSessionReceiver(IncomingDataListener sessionDataReceiver)
  {
    //Store a reference to the read event receiver that will be called with incoming session data.
    this.sessionDataReceiver = sessionDataReceiver;
    return this;
  }

  /**
   * <p>The durable session has connected to the remote session endpoint.  Prior to initialising the communication state, the connection <b>MUST</b> have been validated
   * at both ends to make sure the connection has not been hijacked.  The two ends increment their connection IDs in tandem and must not go out of sync.
   * 
   * <p>The first message we send and expect to receive is the INIT message which tells us the last received MID of the other endpoint.  
   * We can use this adjust the send stream position so that we can resend any data that were lost if a previous connection 
   * went down in bad order.
   * 
   * @param sessionSender durable session sender instance for sending stream data to the currently connected endpoint.
   */
  @Override
  public void reinitialiseCommunicationState(DurableSessionSender sessionSender)
  {
    //Store sender reference.
    this.sessionSender = sessionSender;
    
    //Unset the connection initialised flag.
    connectionInitialised = false;
    
    //Zero unacknowledged send message counter.
    unacknowledgedReceivedMessages = 0;

    //Next connection ID.
    connectionIDCounter.postincrementCount();
    
    //Clear the receive buffer.  Send will start from the beginning of the next expected message.
    receiveBuffer.clearAll();
    
    //We need to send an INIT first.  We need to receive an INIT before we send any data because we need to
    //re-adjust the send stream position.
    initBuffer.clearAll();
    initBuffer.produceFromHandler(new WriteBlockHandler()
    {
      
      @Override
      public void writeMessageBlock(SequentialMessageBlockWriter writer)
      {
        controlPacket.writeIniPacket(connectionIDCounter.getCurrentCount(), lastMessageIDReceived, writer);
        
        //Log message.
        if(logger.isDebugEnabled()) logger.debug(commSystemBundle.getString("hamster.00300001"), sessionID, controlPacket, receiveBuffer);
      }
    });
    
    //Send INI immediately.  We will ASSUME that a 6-byte packet will be written into a brand-new connection buffer in one go.
    if(sessionSender.sendOutgoingCommunicationData(initBuffer) != DurableSessionControlPacket.PACKET_SIZE) throw new IllegalStateException("New connection did not send INI PACKET!!!");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void constructOutgoingMessage(WriteBlockHandler messageData)
  {
    //Set the DAT callback.  This will be called to provide message data when we produce it below.
    this.controlPacketHandler.setDATMessageCallback(messageData);
    
    //Write message data block with DAT header to outgoing buffer.
    //int written = 
    this.sendBuffer.produceFromHandler(controlPacketHandler);
    
    //Make sure the end loop flag is ON, so this packet data will be sent at the end of the comm loop.  Only do this, however,
    //if the session has been established.
    if(sessionSender != null) this.sessionSender.setOutgoingLoopEndNotification(connectionInitialised);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void continueSending()
  {
    //Return immediately if there are no data in the outgoing buffer.
    if(!sendBuffer.hasDataToConsume()) return;

    //We can only send if the durable session has been established.  The data must wait in the buffer until this is the case.
    if(!connectionInitialised)
    {
      //Log message.
      //if(logger.isTraceEnabled()) logger.trace(commSystemBundle.getString("hamster.00300102"), sessionID);
      //Turn off end-of-loop notification which triggers the call to this method.  We can't have it on until the session is established.
      sessionSender.setOutgoingLoopEndNotification(false);
      //Return.
      return;
    }
    
    //Send data from the outgoing buffer.
    int i = sessionSender.sendOutgoingCommunicationData(sendBuffer);
    
   //Log message.
    //if(logger.isTraceEnabled()) logger.trace(commSystemBundle.getString("hamster.00300101"), sessionID, i, "");

    //If we have sent no data then the underlying outgoing channel buffer is full and we'll have to wait until the channel is readable again.
    //Make sure the end loop notification is OFF.
    if(i < 1) sessionSender.setOutgoingLoopEndNotification(false);
  }

  /**
   * <p>Transfer incoming data into the session buffer.  If the session is not initialised then the first packet must be an INI to reset the send to the
   * end of the peer's last known MID received.  This should start the session sending at the correct position to prevent data loss.
   */
  @Override
  public void handleCommunicationDataRead(ReadTransferHandler readHandler)
  {
    //Connection init?
    if(!connectionInitialised)
    {
      //Not initialised.  We need an INI packet.
      initBuffer.clearAll();
      //Transfer the INI packet into the buffer.
      //sessionSender.sendOutgoingCommunicationData(initBuffer);
      readHandler.transferChannelReadData(initBuffer);
      
      //Make sure init buffer is full.
      if(initBuffer.readableBytesInBuffer() < DurableSessionControlPacket.PACKET_SIZE) return;
      
      //Read the packet.
      initBuffer.consumeToHandler(controlPacket);
      
      //TODO CHECK PACKET IS INI AND CONN ID IS CORRECT.
      controlPacket.getCID(); 
      
      //INI packet contains the connection ID and the last MID to be processed by the peer endpoint.
      int MID = controlPacket.getMID();
      
      //Chop everything that's received off the start of the send buffer.
      controlPacketHandler.handleSendBufferReclaim(MID);
      
      //Start send location at 0 - everything received by the remote peer has been reclaimed so send pointer must now be 0.
      //The end of the last message received by the peer has been reclaimed and the next message (dead ACK or next DAT) is
      //at the front of the buffer.
      sendBuffer.locateReadPosition(0);
      
      //Log message.
      if(logger.isDebugEnabled()) logger.debug(commSystemBundle.getString("hamster.00300002"), sessionID, controlPacket, sendBuffer.outputSendBuffer());

      //Set the connection initialised flag.
      connectionInitialised = true;     
      
      //If we have outgoing data to send then make sure the end-of-loop notification flag is ON.
      //These send data may have appeared as a result of consuming incoming data to a handler which may have replied or initiated a new message.
      if(sendBuffer.hasDataToConsume()) sessionSender.setOutgoingLoopEndNotification(true);
      
    }

    //We must be able to read at least 6 bytes from the channel or this will have failed above.
    
    //Read in all available data.
    while(true)
    {
      //Read from the channel.
      int i = readHandler.transferChannelReadData(receiveBuffer);
      
      //Finished if we've received nothing.
      if(i < 1) break;
      
      //Handle incoming data.
      //This will handle full packets with durable header.  Each DAT packet will be handled by the client within only the bounds
      //of the payload.
      while(receiveBuffer.consumeToHandler(controlPacketHandler));

      //Do a full reclaim of consumed data.
      receiveBuffer.reclaimAllConsumedDataSpace();
      
      //Do we need an ACK packet?
      if(unacknowledgedReceivedMessages > 2)//20 
      {
        //Init the control packet handler.
        controlPacketHandler.clear();
        
        //Send ACK packet.  This is the default if no DAT producer is specified.
        this.sendBuffer.produceFromHandler(controlPacketHandler);

        //Set the end-loop write callback flag.
        sessionSender.setOutgoingLoopEndNotification(true);
      }
    }
  }
  
  /**
   * <p>The comms loop has ended.
   */
  @Override
  public void handleLoopEnd()
  {
    // TODO Do we want a single write here?
    
  }

  /**
   * <p>To string implementation.
   */
  @Override
  public String toString()
  {
    return "";
  }

  /**
   * <p>Output consumable bytes in the receive buffer in hex format.
   * 
   * @return string of hex formatted bytes contained by the receive buffer.
   */
  public String outputReceiveBuffer()
  {
    return receiveBuffer.outputReceiveBuffer();
  }

  /**
   * <p>Output sendable bytes in the send buffer in hex format.
   * 
   * @return string of hex formatted bytes contained by the send buffer.
   */
  public String outputSendBuffer()
  {
    return sendBuffer.outputSendBuffer();
  }

  /**
   * <p>Single instance of this class per enclosing session instance.  Handles all STREAM data.
   * 
   * @author jdf19
   *
   */
  private class ControlPacketHandler implements ReadableCompletionBlockHandler, WriteBlockHandler
  {
    /**
     * <p>This callback will produce DAT message data.
     */
    private WriteBlockHandler messageData;
    
    /**
     * <p>User callback - sets the boundaries on the payload of a DAT packet and calls the client to handle the data. 
     */
    private final UserDataHandlerCallback clientCallback = new UserDataHandlerCallback();
    
    /**
     * <p>Outgoing message scanner - looks for MID in send buffer for reclaim.
     */
    private final OutgoingMessageScanner outgoingMessageScanner = new OutgoingMessageScanner();
    
    /**
     * <p>Set the DAT message callback which will be called to produce message data.
     * 
     * @param messageData
     */
    void setDATMessageCallback(WriteBlockHandler messageData)
    {
      this.messageData = messageData;
    }
    
    /**
     * <p>Clear the message data - we need to make sure we're sending an ACK packet.
     */
    public void clear()
    {
      this.messageData = null;
    }

    /**
     * <p>Calculate message completeness.  This depends on the STREAM header for each packet.  DATs require the whole payload present
     * and other control packets simply need to be fully present to be handled.
     */
    @Override
    public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
    {
      //Check if we've got at least the header.
      if(dataReader.bytesInBlock() < DurableSessionControlPacket.PACKET_SIZE)
      {
        //Not enough data to determine the overall packet length.
        return -1;
      }
      else
      {
        //Read the control packet.
        controlPacket.fromReader(dataReader);
        
        //Check the length.
        int overallPacketLength = controlPacket.getOverallPacketLength();
        
        //Return overall packet length.
        return overallPacketLength;
      }
    }

    /**
     * <p>Process the block of STREAM data.  If the {@link #processDATLen} is NOT -1 then we need to send the block to the client for handling.
     */
    @Override
    public boolean readMessageBlock(SequentialMessageBlockReader reader)
    {
      //Read the control packet.  We need to consume this from the message before recursively calling the incoming message callback so that
      //the incoming stream is in the correct position.
      controlPacket.fromReader(reader);
      
      //Whole packet is ready.  The control packet will contain the header.
      //Response depends on packet.
      if(controlPacket.isACK())
      {
        //Handle reclaim.
        handleSendBufferReclaim(controlPacket.getCommandData());
        
        //Reset packet.
        controlPacket.resetPacket();
            
        //Return true - we always handle ACK packets.
        return true;
      }
      else if(controlPacket.isNOP())
      {
        if(logger.isDebugEnabled()) logger.debug(commSystemBundle.getString("hamster.0030000A"), sessionID, controlPacket);

        //Return true - we always handle NOP packets.
        return true;
      }
      else if(controlPacket.isDAT())
      {
        //Set the process DAT len with the following packet length.
        int processDATLen = controlPacket.getCommandData() - DurableSessionControlPacket.PACKET_SIZE;
        
        try
        {
          //Check for enough data to handle the message.
          if(processDATLen <= reader.bytesInBlock())
          {
            //Initialise the client callback with the amount of process data in the embedded packet.
            //clientCallback.init(processDATLen);

            //Call the client callback to handle the message data.
            boolean consumed = reader.processReadBlockHandler(clientCallback);

            //Set the last received MID.
            lastMessageIDReceived = controlPacket.getMID();
            
            //Increment the number of unacknowledged messages.
            unacknowledgedReceivedMessages++;

            //Log message.
            if(logger.isDebugEnabled()) logger.debug(commSystemBundle.getString("hamster.00300004"), sessionID, controlPacket, unacknowledgedReceivedMessages, receiveBuffer.outputReceiveBuffer());

            //Return consumed status.
            return consumed;
          }
        }
        finally
        {
          //Reset packet.
          controlPacket.resetPacket();        
        }
      }
      else
      {
        //TODO ILLEGAL CONTROL PACKET TYPE!!!  How to handle this????
        System.out.println("ILLEGAL PACKET!!! " + reader);
      }

      return false;
    }

    /**
     * <p>Handle send buffer reclaim.
     * @param MID 
     */
    void handleSendBufferReclaim(int MID)
    {
      //Check for MID 0 - don't process these as this is the initial condition for durable buffer start.
      if(MID == 0)
      {
        //Set MID == 0 so the scanner will look to discard any ACK packets.
        sendBuffer.scanConsumable(outgoingMessageScanner.set(0));
        int dataToReclaim = outgoingMessageScanner.res;
        //Tell the send buffer to discard the acknowledged data.
        sendBuffer.reclaimConsumedDataSpace(dataToReclaim);

        //Log message.
        if(logger.isDebugEnabled()) logger.trace(commSystemBundle.getString("hamster.00300005"), sessionID, controlPacket);
      }
      //Check the connection ID.
      //Don't do anything if the MID is equal to the last message ID sent.
      else if( (controlPacket.getCID() == connectionIDCounter.getCurrentCount()) )
      {
        //Current connection ID - process ACK.

        //Search the buffer for the given message id.  These messages have been received and processed so they can be reclaimed from the front of the buffer.
        //sendBuffer.
        if((controlPacket.getCommandData() != messageIDCounter.getCurrentCount()))
        {
          sendBuffer.scanConsumable(outgoingMessageScanner.set(controlPacket.getCommandData()));
          int dataToReclaim = outgoingMessageScanner.res;
          //Tell the send buffer to discard the acknowledged data.
          sendBuffer.reclaimConsumedDataSpace(dataToReclaim);
        }
        else
        {
          //ACK for MID that was the last one sent out.  Sending fully upto date so clear the send buffer completely.
          sendBuffer.clearBuffer();
        }

        //Log message.
        if(logger.isDebugEnabled()) logger.debug(commSystemBundle.getString("hamster.00300007"), sessionID, controlPacket, outgoingMessageScanner.res, sendBuffer.outputSendBuffer());
      }
      else
      {
        //ACK packet from previous connection.  Discard.
        //Log message.
        if(logger.isDebugEnabled()) logger.debug(commSystemBundle.getString("hamster.00300006"), sessionID, controlPacket, connectionIDCounter.getCurrentCount());
      }
    }
    
    /**
     * <p>Write block data.  If we've got a DAT handler specified then it's a DAT packet, otherwise it's an ACK packet.
     */
    @Override
    public void writeMessageBlock(SequentialMessageBlockWriter writer)
    {
      if(messageData != null)
      {
        //Prepare the DAT header.  The length isn't known yet; we'll come back and fill it in after writing the message.
        controlPacket.writeDatPacket(messageIDCounter.preincrementCount(), 0, writer);
        
        //Write the writable callback into the send buffer writer.
        writer.addWritableBlock(messageData);
        
        //Fill in the packet length.
        controlPacket.setPacketLen(writer);

        //Update the last message sent id.
        //lastMessageIDSent = messageIDCounter.getCurrentCount();
      }
      else
      {
        //ACK packet.
        controlPacket.writeAckPacket(connectionIDCounter.getCurrentCount(), lastMessageIDReceived, writer);

        //Zero unacknowledged send message counter.
        unacknowledgedReceivedMessages = 0;
      }

      //Log message.
      if(logger.isDebugEnabled()) logger.debug(commSystemBundle.getString("hamster.00300003"), sessionID, controlPacket, sendBuffer);

      //Finished - set message data reference to null.
      messageData = null;
    }
    
    /**
     * <p>Inner class which handles whole DAT packet callback to the incoming session data listener.
     * 
     * @author jdf19
     *
     */
    private class UserDataHandlerCallback implements ReadBlockHandler
    {
      @Override
      public boolean readMessageBlock(SequentialMessageBlockReader reader)
      {
        //Callback message listener.
        sessionDataReceiver.handleIncomingData(reader); 
        return true;
      }
    }
    
    /**
     * <p>Work out, given an outgoing MID, where it is in the send buffer and hence how many bytes can be reclaimed.  The MID
     * specified <b>MUST</b> exist in the send buffer if it has been specified in an INI or ACK packet from the peer endpoint.
     * 
     * @author jdf19
     *
     */
    private class OutgoingMessageScanner implements ConsumableCalculationBlockHandler
    {
      /**
       * <p>The message ID to search for.
       */
      int MID;
      
      /**
       * <p>The result of the calculation - the number of bytes to reclaim from the front of the send buffer.
       */
      int res;
      
      /**
       * <p>NOP ID - rolling counter simply provides identification of individual NOP packets. 
       */
      //int NID = 1;
      
      /**
       * <p>Control packet for calculation processing.
       */
      private DurableSessionControlPacket ctrlPacket = new DurableSessionControlPacket();
      
      /**
       * <p>Set the message id and reset the result.
       * 
       * @param MID message ID to search for.
       * @return this instance of {@link OutgoingMessageScanner}
       */
      OutgoingMessageScanner set(int MID)
      {
        this.MID = MID;
        this.res = 0;
        return this;
      }
      
      /**
       * <p>This scan will go differently depending on the MID.  We never send an MID of 0 and this condition indicates that no
       * message has been received as yet by the peer.  In this case, we simply scan the buffer and turn all non-DAT packets (ACK packets in actual fact)
       * to NOP packets.  They will be discarded by the peer when received.
       * <p>If the MID is non-zero, we need to calculate what the next MID would be.  We scan the buffer for all complete messages (there should NEVER be
       * incomplete message data in the buffer) and reclaim EVERYTHING behind the next calculated MID.  If we don't find the next calculated MID it is because
       * we haven't sent it.  Every non-DAT message after that MID, if it is found, will be converted to NOP.  We don't want to send ACK messages from a previous
       * connection.
       */
      @Override
      public void calculateBlock(SequentialMessageBlockReader dataReader)
      {
        //Gather data at the front of the buffer for reclaim upto the first non-reclaimable DAT (the next to be sent) or the end of the buffer if that DAT
        //does not yet exist.
        boolean gather = true;
        
        //MID to stop gathering at.
        int stopMID = (MID != 0) ? messageIDCounter.peekNextCount(MID) : 0;
        
        //Only process if there are enough data to do so.
        while((dataReader.bytesRemainingInBlock() >= DurableSessionControlPacket.PACKET_SIZE))
        {
          //Read the control packet.
          ctrlPacket.fromReader(dataReader);

          //Is DAT?
          if(ctrlPacket.isDAT())
          {
            //Still gathering?
            if(gather)
            {
              //Have we hit the stop MID?
              if( (ctrlPacket.getMID() == stopMID) || (stopMID == 0) )
              {
                //Stop gathering.
                gather = false;
              }
              else
              {
                //Gather the DAT message.
                res += ctrlPacket.getOverallPacketLength();  
              }
            }
            
            //Skip forward to the end of the message.
            dataReader.skip(ctrlPacket.getOverallPacketLength() - DurableSessionControlPacket.PACKET_SIZE);            
          }
          //Is non-DAT?
          else
          {
            //Still gathering?
            if(gather)
            {
              //Gather the ACK message.
              res += ctrlPacket.getOverallPacketLength();  
            }
            else
            {
              //Not gathering.  Convert to NOP.
              //
              //DurableSessionControlPacket.replaceNOP(dataReader, NID++);              
            }
          }
        }
      }
    }
  }
}
