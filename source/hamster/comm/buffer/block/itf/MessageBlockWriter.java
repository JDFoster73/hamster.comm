package hamster.comm.buffer.block.itf;

/**
 * <p>Classes extending this interface are writable buffers which produce primitive data.  Any of these methods can be
 * called to add the corresponding primitive type to the underlying buffer.</p>
 * @author jdf19
 *
 */
public interface MessageBlockWriter
{
  /**
   * Produce a float (signed 4-byte floating point) to the stream.
   * 
   * @param data the float to produce to the stream.
   * @return this buffer writer instance reference.
   */
  public MessageBlockWriter produceFloat(float data);
  
  /**
   * Produce a double (signed 8-byte floating point) to the stream.
   * 
   * @param data the double to produce to the stream.
   * @return this buffer writer instance reference.
   */
  public MessageBlockWriter produceDouble(double data);
  
  /**
   * Produce a long (signed 8-byte integer) to the stream.
   * 
   * @param data the long to produce to the stream.
   * @return this buffer writer instance reference.
   */
  public MessageBlockWriter produceLong(long data);

  /**
   * Produce an integer (signed 4-byte integer) to the stream.
   * 
   * @param data the integer to produce to the stream.
   * @return this buffer writer instance reference.
   */
  public MessageBlockWriter produceInt(int data);

  /**
   * Produce a short (signed 2-byte integer) to the stream.
   * 
   * @param data the short to produce to the stream.
   * @return this buffer writer instance reference.
   */
  public MessageBlockWriter produceShort(short data);

  /**
   * Produce a char (unicode character or unsigned 2-byte integer) to the stream.
   * 
   * @param data the char to produce to the stream.
   * @return this buffer writer instance reference.
   */
  public MessageBlockWriter produceChar(char data);

  /**
   * Produce a byte (signed 1-byte integer) to the stream.
   * 
   * @param data the byte to produce to the stream.
   * @return this buffer writer instance reference.
   */
  public MessageBlockWriter produceByte(byte data);

  /**
   * Produce the given byte array into this buffer writer.
   * 
   * @param data the byte array to produce into this buffer writer.
   * @param start the array position to start producing from.
   * @param len the amount of data to produce.
   * @return this buffer writer instance reference.
   */
  public MessageBlockWriter produceBytes(byte[] data, int start, int len);

  /**
   * Produce the given byte array into this buffer writer.
   * 
   * @param data the byte array to produce into this buffer writer.
   * @return this buffer writer instance reference.
   */
  public MessageBlockWriter produceBytes(byte[] data);
}
