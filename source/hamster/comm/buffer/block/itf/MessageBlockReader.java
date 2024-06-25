package hamster.comm.buffer.block.itf;

import java.nio.ByteOrder;

import hamster.comm.buffer.block.SequentialMessageBlockReader;

/**
 * <p>Inerface describing the standard methods of reading data from a message block.
 * @author jdf19
 *
 */
public interface MessageBlockReader
{
  /**
   * Consume a <code>long</code> primitive datatype from the underlying storage.
   * 
   * @return 8-byte signed long.
   */
  public long consumeLong();

  /**
   * Consume an <code>int</code> primitive datatype from the underlying storage.
   * 
   * @return 4-byte signed int.
   */
  public int consumeInt();

  /**
   * Consume a <code>short</code> primitive datatype from the underlying storage.
   * 
   * @return 2-byte signed short.
   */
  public short consumeShort();
    
  /**
   * Consume a <code>char</code> primitive datatype from the underlying storage.
   * 
   * @return 2-byte unsigned char.
   */
  public char consumeChar();
    
  /**
   * Consume a <code>byte</code> primitive datatype from the underlying storage.
   * 
   * @return 8-bit signed byte.
   */
  public byte consumeByte();
  
  /**
   * Consume a series of bytes from the internal buffer, transferring them into the given array.
   * There <b>must</b> be enough space in the target array to copy the bytes into - i.e. by.length
   * &gt;= (offset + len).  There must also be enough readable bytes to perform the requested transfer.
   * 
   * @param by byte array.
   * @param offset the index position in the byte array <code>by</code> to start copying bytes.
   * @param len the number of bytes to copy into the byte array.
   */
  public void consumeBytes(byte[] by, int offset, int len);

  /**
   * Consume a series of bytes from the internal buffer, transferring them into the given array.
   * There must also be enough readable bytes to perform the requested transfer.
   * 
   * @param byteArray byte array.
   */
  public void consumeBytes(byte[] byteArray);

  /**
   * Standard string object consists of 1-char (16-bit) length specifier, plus chars.
   * The chars are copied into the char buffer supplied and the number of chars
   * copied is returned.
   *  
   * @return a newly-constructed string, using the given string builder.
   */
  public String consumeCharString();

  /**
   * Consume a byte string into the given target byte array.
   * 
   * @param target the byte array to copy the fixed-length byte string into.  There <b>must</b> be enough bytes in the target buffer to hold the byte string.
   * @return the length of the byte string.
   */
  public int consumeByteString(byte[] target);

  /**
   * Consume a byte string into the given target byte array.
   * 
   * @return the newly-created byte array.
   */
  public byte[] consumeByteString();

  /**
   * Skip the given number of bytes.
   * 
   * @param bytes the number of bytes to skip from the incoming readable data.
   */
  public void skip(int bytes);

  /**
   * Allow buffer owner to change the byte order as required.  Fields may be present in one message which are encoded in different
   * byte orders.  Allowing the owner to change the byte order will allow these fields to be easily processed.
   * 
   * @param order the byte order of the data to consume.
   */
  public void setOrder(ByteOrder order);
  
  /**
   * <p>Return the total number of bytes in the currently processed message block.
   * 
   * @return the number of bytes in the read block.
   */
  public int bytesInBlock();
  
  /**
   * <p>Return the number of unconsumed bytes in the currently processed message block.
   * 
   * @return the number of bytes remaining in the read block.
   */
  public int bytesRemainingInBlock();

  /**
   * <p>The current read position, where 0 is the start byte for the message block.
   * 
   * @return the current byte position in the message block - i.e. the next byte to be consumed.
   */
  public int currentBlockReadPosition();

  /**
   * <p>Rewind to the start of the read block.
   * 
   */
  public void rewindToBlockStartPosition();

  /**
   * <p>Provide a new {@link SequentialMessageBlockReader} instance with only the cloned data of the currently processed
   * message block.
   *  
   * @return new {@link SequentialMessageBlockReader} instance with cloned data from current message block.
   */
  public SequentialMessageBlockReader cloneCurrentMessage();
}
