import java.net.{Socket}

/**
 * A class used to represent clients connected to various chat rooms
 *
**/
class Client(name:String, id:Int, clientSocket:Socket) {
	val handle = name
	val joinRef = id
	var socket = clientSocket
}