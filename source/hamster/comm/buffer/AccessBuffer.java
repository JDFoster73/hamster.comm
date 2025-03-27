package hamster.comm.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>The general pipeline buffer simply buffers data bytes.
 *
 * @author jdf19
 */
public class AccessBuffer extends BaseBuffer
{
  /**
   * <p>Buffer object that is wrapped by this utility class.
   */
  //protected final ByteBuffer internalBuffer;
  
  /**
   * Create an instance of the general pipeline buffer with the given factory and logger instances.
   *
   * @param bufferFact the buffer factory to use when creating an instance of the general pipeline buffer.
   */
  public AccessBuffer(BufferFactory bufferFact)
  {
    super(bufferFact);
    //internalBuffer = bufferFact.newBuffer();
  }

  /**
   * Transfer the available contents of the given BaseBuffer instance to this {@link AccessBuffer}.  The contents of the
   * buffer passed in the argument will replace any data in this {@link AccessBuffer} instance.
   *
   * @param buffer
   */
  public void transferFrom(PipelineBuffer buffer)
  {
    //Clear the internal buffer.
    internalBuffer.clear();

    //Transfer available data from the pipeline buffer.
    buffer.doTransferTo(internalBuffer);

    //Set the limit to the position.
    internalBuffer.limit(internalBuffer.position());
  }

  /**
   * Transfer the available contents of this {@link AccessBuffer} to the given BaseBuffer instance.
   *
   * @param buffer
   */
  public void transferTo(PipelineBuffer buffer)
  {
    //Set the buffer position to 0.
    internalBuffer.position(0);

    //Transfer out to the target buffer.
    buffer.doTransferFrom(internalBuffer);
  }

  /**
   * Initialise the {@link AccessBuffer} to a block of the given length (which MUST be less than or equal to the
   * capacity the {@link AccessBuffer} was initialised with) and zero the internal data.
   *
   * @param length
   */
  public void init(int length)
  {
    //Set the pos and limit to the length.
    internalBuffer.position(0);
    internalBuffer.limit(length);

    //Zero the data.
    while(internalBuffer.remaining() > 0)
    {
      int remaining = Math.min(internalBuffer.remaining(), 8);
      switch (remaining)
      {
        case 8:
          internalBuffer.putLong(0);
          break;
        case 7:
        case 6:
        case 5:
        case 4:
          internalBuffer.putInt(0);
          break;
        case 3:
        case 2:
          internalBuffer.putChar((char) 0);
          break;
        case 1:
          internalBuffer.put( (byte) 0);
          break;
      }
    }
  }

  public AccessBuffer setFromBytes(byte[] bytesToTransferToThisBuffer)
  {
    //Clear the buffer and write the data into it.
    internalBuffer.clear();
    //Add the bytes to the internal buffer.
    internalBuffer.put(bytesToTransferToThisBuffer);
    //
    return this;
  }

  /**
   * Transfer the raw byte data from the source array into this buffer instance.  There must be enough space in the
   * internal buffer to be able to transfer all bytes from the source array.
   *
   * @param bytesToTransferToThisBuffer
   * @param startIx
   * @param length
   * @return
   */
  public AccessBuffer produceFromBytes(byte[] bytesToTransferToThisBuffer, int startIx, int length)
  {
    //Clear the buffer and write the data into it.
    internalBuffer.clear();
    //Add the bytes to the internal buffer.
    internalBuffer.put(bytesToTransferToThisBuffer, startIx, length);
    //
    return this;
  }

  public AccessBuffer produceFromByteBuffer(ByteBuffer producer)
  {
    //Clear the buffer and write the data into it.
    internalBuffer.clear();
    //Produce to the given reference.
    internalBuffer.put(producer);
    //
    return this;
  }

  /**
   * Return the number of consumable bytes in the buffer.
   *
   * @return
   */
  public int size()
  {
    //Return the difference between produce and consume.
    return internalBuffer.position();
  }

  //MOVE DATA TO AND FROM COMMUNICATION CHANNELS


  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //CONSUME SCALAR DATA FROM THE BUFFER
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /// BYTE DATA

  public byte readByte(int atPosition)
  {
    //Get the next byte from the queue data.
    byte ret = internalBuffer.get(atPosition);

    //Return the data.
    return ret;
  }


  /// CHAR DATA

  public char readChar(int atPosition)
  {
    //Get the next byte from the queue data.
    char ret = internalBuffer.getChar(atPosition);

    //Return the data.
    return ret;
  }

  /// SHORT DATA

  public short readShort(int atPosition)
  {
    //Get the next byte from the queue data.
    short ret = internalBuffer.getShort(atPosition);

    //Return the data.
    return ret;
  }

  /// INT DATA

  public int readInt(int atPosition)
  {
    //Get the next byte from the queue data.
    int ret = internalBuffer.getInt(atPosition);

    //Return the data.
    return ret;
  }

  /// LONG DATA

  public long readLong(int atPosition)
  {
    //Get the next byte from the queue data.
    long ret = internalBuffer.getLong(atPosition);

    //Return the data.
    return ret;
  }

  /// FLOAT DATA

  public float readFloat(int atPosition)
  {
    //Get the next byte from the queue data.
    float ret = internalBuffer.getFloat(atPosition);

    //Return the data.
    return ret;
  }

  /// DOUBLE DATA

  public double readDouble(int atPosition)
  {
    //Get the next byte from the queue data.
    double ret = internalBuffer.getDouble(atPosition);

    //Return the data.
    return ret;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //PRODUCE SCALAR DATA TO THE BUFFER
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  
  ///*** Block message produce - set at the buffer location from the start of the message.

  /// BYTE DATA

  public AccessBuffer setByteAt(int atPosition, byte data)
  {
    //Get the next byte from the queue data.
    internalBuffer.put(atPosition, data);

    //Return the data.
    return this;
  }


  /// CHAR DATA

  public AccessBuffer setCharAt(int atPosition, char data)
  {
    //Get the next byte from the queue data.
    internalBuffer.putChar(atPosition, data);

    //Return the data.
    return this;
  }

  /// SHORT DATA

  public AccessBuffer setShortAt(int atPosition, short data)
  {
    //Get the next byte from the queue data.
    internalBuffer.putShort(atPosition, data);

    //Return the data.
    return this;
  }

  /// INT DATA

  public AccessBuffer setIntAt(int atPosition, int data)
  {
    //Get the next byte from the queue data.
    internalBuffer.putInt(atPosition, data);

    //Return the data.
    return this;
  }

  /// LONG DATA

  public AccessBuffer setLongAt(int atPosition, long data)
  {
    //Get the next byte from the queue data.
    internalBuffer.putLong(atPosition, data);

    //Return the data.
    return this;
  }

  /// FLOAT DATA

  public AccessBuffer setFloatAt(int atPosition, float data)
  {
    //Get the next byte from the queue data.
    internalBuffer.putFloat(atPosition, data);

    //Return the data.
    return this;
  }

  /// DOUBLE DATA

  public AccessBuffer setDoubleAt(int atPosition, double data)
  {
    //Get the next byte from the queue data.
    internalBuffer.putDouble(atPosition, data);

    //Return the data.
    return this;
  }


  /**
   * True if there are no consumable data in the buffer.
   * @return
   */
  public boolean isEmpty()
  {
    return this.internalBuffer.position() == 0;
  }

  public AccessBuffer setOrder(ByteOrder order)
  {
    this.internalBuffer.order(order);
    return this;
  }
  
}
