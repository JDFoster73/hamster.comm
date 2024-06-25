package hamster.comm.buffer.stream;

/**
 * <p>
 * A buffer object that supports atomic operations. It receives data and
 * provides data in user-defined blocks.
 * <p>
 * This class is meant to be used as a building block in other classes; it
 * supports the notions of filling, producing, draining and consuming but
 * delegates the actual buffer transfer to subclasses.
 * <p>
 * When consuming data in a block, ALL DATA MUST BE CONSUMED in the call.  Not doing so, even if it does not cause an immediate problem,
 * can introduce bugs if allowed.  An exception will be thrown if a consume block operation does not fully consume the data in the block.
 * <p>
 * This class is definitely not thread-safe; it MUST be used in a
 * single-threaded environment.
 * 
 * @author jdf19
 *
 */
public class StreamBufferBase
{
}
