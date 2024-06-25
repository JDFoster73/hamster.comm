/**
 * <p>This package contains classes which provide <b><i>durable</i></b> communications.  A durable channel contains a protocol wrapper
 * which implements a connection ID and message ID.  If a physical communication channel is dropped, there may be data which have been
 * sent but not received at the other end (in either or both directions).
 * <p>Upon (re-)connection, the two ends of a durable communication channel will synchronise themselves and resend any data which have
 * been lost.
 */

package hamster.comm.buffer.durable;