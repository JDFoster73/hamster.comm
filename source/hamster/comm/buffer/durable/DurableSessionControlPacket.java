package hamster.comm.buffer.durable;

import java.util.HashMap;
import java.util.Map;

import hamster.comm.buffer.block.SequentialMessageBlockReader;
import hamster.comm.buffer.block.SequentialMessageBlockWriter;
import hamster.comm.buffer.block.itf.ReadableCompletionBlockHandler;
import hamster.comm.buffer.block.itf.WriteBlockHandler;

/**
 * <p>This class embodies a durable session control packet.  It can read from a data stream  or be set with field data and written to a data stream.
 * 
 * @author jdf19
 *
 */
public class DurableSessionControlPacket implements ReadableCompletionBlockHandler, WriteBlockHandler
{
  /**
   * <p>TYPE.  This is the packet type, one of NOP (no operation), INI (initialisation packet), ACK (acknowledgement packet) or DAT (data packet).
   * 
   * @author jdf19
   *
   */
  private enum TYPE
  {
    /**
     * NOP packet type.
     */
    NOP(0),
    /**
     * INI packet type.
     */  
    INI(1),
    /**
     * ACK packet type.
     */
    ACK(2),
    /**
     * DAT packet type.
     */
    DAT(3);

    /**
     * <p>Reverse lookup map.
     */
    private static final Map<Integer, TYPE> revLookup = new HashMap<>();
    
    /**
     * <p>Populate the reverse lookup map.
     */
    static
    {
      for(TYPE t : TYPE.values()) revLookup.put(t.type, t);
    }
    
    /**
     * <p>The enum value specific type code.
     */
    private final int type;
    
    /**
     * <p>Create each individual enum item.
     * @param i
     */
    private TYPE(int i)
    {
      this.type = i;
    }
    
    /**
     * <p>Get the type.
     * @return
     */
    int getType()
    {
      return type;
    }
    
    /**
     * <p>Reverse lookup the type from the type code.
     * 
     * @param code
     * @return
     */
    static TYPE fromCode(int code)
    {
      TYPE t = revLookup.get(code);
      if(t == null) throw new IllegalArgumentException();
      return t;
    }
  }
  
  /**
   * <p>Fixed durable session control packet.
   */
  public static final int PACKET_SIZE = 6;

  /**
   * <p>The position of the data field in the packet.
   */
  private static final int FIELD_POS_DATA = 2;

  /**
   * <p>NOP validator - fixed value.
   */
  private static final int NOP_VALIDATOR = 0x3A3A;

  /**
   * <p>The multiplexed [TYPE] and [CID|MID] field.
   */
  private char operation;
  
  /**
   * <p>Data length [DAT] or MID [INI|ACK]
   */
  private int data;
   
  /**
   * <p>The currently-set type of message packet.
   */
  private TYPE currentType;
  
  /**
   * <p>Create the control packet.
   */
  public DurableSessionControlPacket()
  {
    //Initially reset packet.
    resetPacket();
  }
  
  /**
   * <p>Reset the packet data.  This results in a NOP with a CID of 0.
   */
  public void resetPacket()
  {
    operation = 0;
    data = 0;
  }
 
  /**
   * <p>Static utility method which operates on a packet in a data reader.  Prior to calling this method,
   * the packet must have been consumed from the data reader.  This method will roll back the reader and
   * replace the serialised packet data with a NOP.
   * 
   * @param dataReader the reader to replace the packet data with a NOP.
   * @param nid the NOP id to use.
   */
  public static void replaceNOP(SequentialMessageBlockReader dataReader, int nid)
  {
    //Roll back PACKET_SIZE number of bytes.
    dataReader.rewind(PACKET_SIZE);
    
    //Replace fields NOP and data 0.
    dataReader.replaceChar(calculateOperationField(TYPE.NOP, NOP_VALIDATOR));
    dataReader.replaceInt(nid);
  }
  
  /**
   * <p>Write an ACK packet data to the given writer.
   * 
   * @param connectionID the connection instance ID that the packet was sent under.
   * @param messageID the message ID that is being acknowledged.
   * @param writer the writer to send the packet to.
   */
  public void writeAckPacket(int connectionID, int messageID, SequentialMessageBlockWriter writer)
  {
    setOperationValue(TYPE.ACK, connectionID);
    data = messageID;
    doWrite(writer);
  }
  
  /**
   * <p>Write an INI packet data to the given writer.
   * 
   * @param connectionID the connection instance ID that the packet was sent under.
   * @param messageID the message ID that is being initialised.
   * @param writer the writer to send the packet to.
   */
  public void writeIniPacket(int connectionID, int messageID, SequentialMessageBlockWriter writer)
  {
    setOperationValue(TYPE.INI, connectionID);
    data = messageID;
    doWrite(writer);
  }

  /**
   * <p>Write a DAT packet data to the given writer.
   * 
   * @param messageID the message ID of the packet.
   * @param packetLen the length of the data packet.
   * @param writer the writer to send the packet to.
   */
  public void writeDatPacket(int messageID, int packetLen, SequentialMessageBlockWriter writer)
  {
    setOperationValue(TYPE.DAT, messageID);
    data = packetLen;
    doWrite(writer);
  }
  
  /**
   * <p>Set up the packet as a NOP packet with the given connection id.
   * 
   * @param connectionID the connection instance ID that the packet was sent under.
   * @return this packet instance reference.
   */
  public DurableSessionControlPacket setupNopPacket(int connectionID)
  {
    setOperationValue(TYPE.NOP, connectionID);
    return this;
  }

  /**
   * <p>Set up the packet as an ACK packet with the given connection id and message ID.
   * 
   * @param connectionID the connection instance ID that the packet was sent under.
   * @param messageID the message ID that is being acknowledged.
   * @return this packet instance reference.
   */
  public DurableSessionControlPacket setupAckPacket(int connectionID, int messageID)
  {
    setOperationValue(TYPE.ACK, connectionID);
    data = messageID;
    return this;
  }
  
  /**
   * <p>Set up the packet as an INI packet with the given connection id and message ID.
   * 
   * @param connectionID the connection instance ID that the packet was sent under.
   * @param messageID the message ID that is being initialised.
   * @return this packet instance reference.
   */
  public DurableSessionControlPacket setupIniPacket(int connectionID, int messageID)
  {
    setOperationValue(TYPE.INI, connectionID);
    data = messageID;
    return this;
  }

  /**
   * <p>Set up the packet as a DAT packet with the given connection id and packet length.
   * 
   * @param messageID the message ID of the packet.
   * @param packetLen the length of the data packet.
   * @return this packet instance reference.
   */
  public DurableSessionControlPacket setupDatPacket(int messageID, int packetLen)
  {
    setOperationValue(TYPE.DAT, messageID);
    data = packetLen;
    return this;
  }

  /**
   * <p>Write the packet contents to the given writer.
   * 
   * @param writer the writer to write the packet contents to.
   */
  private void doWrite(SequentialMessageBlockWriter writer)
  {
    writer.produceChar(operation);
    writer.produceInt(data);
  }
  
  /**
   * <p>Test if this packet is set up as a NOP.
   * 
   * @return true if the current type field is set to NOP.
   */
  public boolean isNOP()
  {
    return currentType == TYPE.NOP;
  }
  
  /**
   * <p>Test if this packet is set up as an ACK.
   * 
   * @return true if the current type field is set to ACK.
   */
  public boolean isACK()
  {
    return currentType == TYPE.ACK;
  }
  
  /**
   * <p>Test if this packet is set up as a DAT.
   * 
   * @return true if the current type field is set to DAT.
   */
  public boolean isDAT()
  {
    return currentType == TYPE.DAT;
  }
  
  /**
   * <p>Test if this packet is set up as an INI.
   * 
   * @return true if the current type field is set to INI.
   */
  public boolean isINI()
  {
    return currentType == TYPE.INI;
  }

  /**
   * <p>Return the data field value.
   * 
   * @return the field value.  This depends on the packet type: in NOP, ACK and INI this is the connection ID; in a DAT packet is is the data packet length.  
   */
  public int getCommandData()
  {
    return data;
  }
  
  /**
   * <p>Provide a readable string representation of the packet contents.
   * 
   * @return readable string of packet contents.
   */
  public String toString()
  {
    switch(currentType)
    {
      case ACK:
        return "ACK(CID:" + getCID() + ", MID:" + data + ")";
      case INI:
        return "INI(CID:" + getCID() + ", MID:" + data + ")";
      case DAT:
        return "DAT(MID:" + getMID() + ",LEN:" + data + ")";
      case NOP:
        return "NOP(FIX:" + Integer.toHexString(getOperationValue()) + ",NID:" + data + ")";
      default:
        return "??? Unknown type " + Integer.toHexString(operation);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int messageBlockCompleteLength(SequentialMessageBlockReader dataReader)
  {
    return dataReader.bytesInBlock() >= PACKET_SIZE ? PACKET_SIZE : 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean readMessageBlock(SequentialMessageBlockReader reader)
  {
    fromReader(reader);
    return true;  //Always handle this message.
  }

  /**
   * <p>Consume the packet data from the given reader.
   *  
   * @param reader the reader to consume the packet data from.
   */
  public void fromReader(SequentialMessageBlockReader reader)
  {
    operation = reader.consumeChar();
    data = reader.consumeInt();
    
    //Set the current type.
    currentType = TYPE.fromCode(getOperationTypeCode());
    
    //Check the validity of NOP.
    if(currentType == TYPE.NOP)
    {
      //Get the NOP validator.
      int nopValidator = getOperationValue();
      //Check it.
      if(nopValidator != NOP_VALIDATOR)
      {
        System.out.println("BAD VALIDATOR!!!!!");
      }
    }
  }

  /**
   * <p>If this packet is DAT then return the overall length including payload.  Otherwise, it's just the control packet length.
   * 
   * @return the overall length of the packet described.  If not DAT then it is simply the PACKET_SIZE, if DAT then the number of bytes in the packet.
   */
  public int getOverallPacketLength()
  {
    return isDAT() ? data : PACKET_SIZE;
  }

  /**
   * <p>Use the block size of the writer to seek back to the length field and fill it in.
   * 
   * @param writer the writer to query how much data it has availalbe for.  This value is used to set the amount of data in a DAT packet.
   */
  public void setPacketLen(SequentialMessageBlockWriter writer)
  {
    //Setting LEN of DAT field.  Update the control packet.
    data = writer.blockMessageBytes();
    
    //Seek to the packet size field position and write the block size.
    writer.produceIntAt(data, FIELD_POS_DATA);
  }
  
  /**
   * <p>Set the operation field which is an amalgamation of the packet type (bits 15 - 14) and type-specific value code (bits 13 - 00).
   * 
   * @param type the packet type (INI, ACK, DAT, NOP).
   * @param val the CID (INI, ACK) or MID (DAT) or 0 (NOP).
   */
  private void setOperationValue(TYPE type, int val)
  {
    //Multiplex the type and value.
    int result = calculateOperationField(type, val);//((type.getType() & 0x3) << 14) | (val & 0x3fff);
    
    //Set the operation value.
    operation = (char) result;
    
    //Set the current type.
    currentType = type;
  }
  
  /**
   * <p>Calculate the operation field for the given type and val.
   * 
   * @param type
   * @param val
   * @return
   */
  private static char calculateOperationField(TYPE type, int val)
  {
    //Multiplex the type and value.
    int result = ((type.getType() & 0x3) << 14) | (val & 0x3fff);
    
    //Return the calculated operation value.
    return (char) result;
    
  }
  
  /**
   * <p>Get the operation type code.
   * 
   * @return operation type code.  See {@link TYPE}.
   */
  private int getOperationTypeCode()
  {
    return (operation >> 14) & 0x3;
  }
  
  /**
   * <p>Get the operation field code (CID, MID).
   * 
   * @return operation field code.
   */
  private int getOperationValue()
  {
    return operation & 0x3fff;
  }
  
  /**
   * <p>Get the connection ID.  In order for this method call to be meaningful, the packet type <b>must</b> be
   * set to either ACK or INI.  Anything else and a runtime exception will be thrown. 
   * 
   * @return the operation field value.
   */
  public int getCID() throws IllegalStateException
  {
    switch(currentType)
    {
      case ACK:
      case INI:
        return getOperationValue();
      default:
        //The control packet does not contain a CID and so can't service the request.
        throw new IllegalStateException();
    }
  }
  
  /**
   * <p>Get the connection ID.  In order for this method call to be meaningful, the packet type <b>must</b> be
   * set to either DAT, ACK or INI.  Anything else and a runtime exception will be thrown. 
   * 
   * @return the operation field value (if DAT) or the data if ACK or INI.
   */
  public int getMID() throws IllegalStateException
  {
    switch(currentType)
    {
      case ACK:
      case INI:
        return data;
      case DAT:
        return getOperationValue();
      default:
        //The control packet does not contain a CID and so can't service the request.
        throw new IllegalStateException();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeMessageBlock(SequentialMessageBlockWriter writer)
  {
    //
    doWrite(writer);
  }
}
