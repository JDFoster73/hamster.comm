/**
 * <p>This package provides support for <b><i>internal channels</i></b>.  An internal channel is a basic re-implementation
 * of java.nio pipes.  Java.nio pipes are curious in that they only support one direction and are implemented by a socket
 * which is forced to be unidirectional.  Many IPC applications will require both directions so the internal channel 
 * server will create a java.nio-like internal pipe implementation but one which uses both directions.
 */

package hamster.comm.ipc.blob;