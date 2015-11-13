import java.io.{BufferedReader, InputStreamReader, PrintWriter, BufferedWriter, OutputStreamWriter}
import java.net.{Socket, ServerSocket, SocketException}
import java.util.concurrent.{Executors}
import java.rmi.server.UID
import scala.io.BufferedSource

/**
 *
 * Object which encapsulates a multithreaded TCP server and worker class.
 *
**/
object ChatServer {

	var stop = false

	/**
	 *
	 * Multithreaded TCP server class
	 *
	**/
	class Server(port:Int) extends Runnable{
		val pool = java.util.concurrent.Executors.newFixedThreadPool(10)
		val serverSocket = new ServerSocket(port)
		var uniqueId = 0;
		var groups: List[Group]= List()
		var clients: List[Client]= List()
		
		/**
		 * Server run function
		 *
		 * Receives incomming connections over a server socket and passes them to workers
		**/
		def run(): Unit = {
			try {
				println("Server starting on port: " + port)
				//loop until a worker receives a message to kill the service
				while(!serverSocket.isClosed){
					try {
						if(!stop)
							pool.execute(new Worker(serverSocket.accept()))
					}
					catch {
						case e: SocketException => println("SocketException: accepting new connection failed")
					}	
				}
			}
			finally {
				pool.shutdown()
			}
		}
		
		/**
		 *
		 * Worker thread class
		 *
		**/
		class Worker(socket:Socket) extends Runnable {
		
			var sIn = new BufferedReader(new InputStreamReader(socket.getInputStream))
			val sOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")))
			var message = ""
			val IPaddress = socket.getLocalAddress().toString().drop(1)
			val port = serverSocket.getLocalPort()
			val studentID = "b3365ae84287f1f505e22479ea34ab6d68fc6238304ed3fe48108ca09b907788"

			/**
			 * Worker run function
			 *
			 * Receives messages over a given socket and handles responses.
			**/
			def run(){
				//println("THREAD " + Thread.currentThread().getId()+": running")
				try {
					while(!socket.isClosed()){
						if(socket.getInputStream().available() > 0){
							message = ""
							message = sIn.readLine()
							println("Received message: " + message)
							//BASE 
							if(message.startsWith("HELO")){
								base(message)
							}
							//JOINING
							else if(message.startsWith("JOIN_CHATROOM")){
								joinRoom()
							}
							//LEAVING
							else if(message.startsWith("LEAVE_CHATROOM")){
								leaveRoom()
							}
							//MESSAGING
							else if(message.startsWith("CHAT")){
								chat()
							}
							//TERMINATE CLIENT CONNECTION
							else if(message.startsWith("DISCONNECT")){
								disconnect()
							}
							//KILL
							else if(message == "KILL_SERVICE"){
								killService()
							}	
							//UNHANDLED			
							else{
								println("unhandled message type")
							}
						}
					}
					//println("thread " + Thread.currentThread().getId()+" closing")
				}
				catch {
					case e: SocketException => println("SocketException: worker run failed")
				}
			}
		
			/**
			 *
			 * Message response functions
			 *
			**/
			def joinRoom(){
				//println("THREAD " + Thread.currentThread().getId()+" JOINING")
				//extract data from input
				var lineData = message.dropWhile(_ != ':')
			    var roomName = lineData.drop(1)
				println("JOIN_CHATROOM: " + roomName)
				
				message = sIn.readLine()
				lineData = message.dropWhile(_ != ':')
				var joinIp = lineData.drop(1)
				println("CLIENT_IP: " + joinIp)
				
				message = sIn.readLine()
				lineData = message.dropWhile(_ != ':')
				var joinPort = lineData.drop(1)
				println("PORT: " + joinPort)
				
				message = sIn.readLine()
				lineData = message.dropWhile(_ != ':')
				var clientName = lineData.drop(1)
				println("CLIENT_NAME: " + clientName)
				
				var roomRef = 0
				var joinRef = 0

				//check if group already exists
				if(!groupExists(roomName)){
					roomRef = getUniqueId()
					groups = new Group(roomName, roomRef) :: groups
				}
				else{
					roomRef = getGroup(roomName).roomRef 
				}
				//check if user already exists
				if(!clientExists(clientName)){
					joinRef = getUniqueId()
					clients = new Client(clientName, joinRef, socket) :: clients
				}
				else{
					joinRef = getClient(clientName).joinRef 
				}
				
				var tempClient = getClient(clientName)
				var tempGroup = getGroup(roomName)
				
				tempGroup.addMember(tempClient)
				println("SENDING....")
				if(socket.isClosed) println("socket closed")
				//Send join message to client
				sOut.println("JOINED_CHATROOM: " + roomName
								+ "\nSERVER_IP: " + IPaddress
								+ "\nPORT: " + port
								+ "\nROOM_REF: " + roomRef
								+ "\nJOIN_ID:" + joinRef)
				println("SENT")
				sOut.flush
				//Send new member message to group
				var joinMsg = tempClient.handle + " has joined this chatroom."
				tempGroup.groupMessage(tempClient, joinMsg)
				//print server message
				println(tempClient.handle + "(" + tempClient.joinRef + "): joined " 
						+ tempGroup.roomName + "(" + tempGroup.roomRef + ")")
				println("")
			}
			
			def leaveRoom(){
				//println("THREAD " + Thread.currentThread().getId()+" LEAVING")
				var lineData = message.dropWhile(_ != ':')
			    var roomRef = lineData.drop(2)
				
				message = sIn.readLine()
				lineData = message.dropWhile(_ != ':')
				var joinId = lineData.drop(2)
				
				message = sIn.readLine()
				lineData = message.dropWhile(_ != ':')
				var clientName = lineData.drop(2)
				
				//if client in chat room - remove client
				var tempGroup = getGroupR(roomRef.toInt)
				println("length: " + tempGroup.members.length)
				if(tempGroup == null){
					println("LEAVE GROUP ERROR: group doesn't exist")
				}
				//block until any mesaages are sent
				//while(tempGroup.transmission)
				
				//send response to client // might be delayed
				sOut.println("LEFT_CHATROOM: " + roomRef
								+ "\nJOIN_ID: " + joinId)
				sOut.flush
				
				//send response to chatroom
				var leftMsg = clientName + " has left this chatroom."
				tempGroup.groupMessage(getClient(clientName), leftMsg)
								
				tempGroup.removeMember(joinId.toInt)

				//print server message
				println(clientName + "(" + joinId + "): left " 
						+ tempGroup.roomName + "(" + tempGroup.roomRef + ")")
				println("")
			}
			
			def chat(){
				println("THREAD " + Thread.currentThread().getId()+" CHAT")
				//extract data from input
				var lineData = message.dropWhile(_ != ':')
			    var roomRef = lineData.drop(2)
				println("CHAT: " + roomRef)
				
				message = sIn.readLine()
				lineData = message.dropWhile(_ != ':')
				var joinId = lineData.drop(2)
				println("JOIN_ID: " + joinId)
				
				message = sIn.readLine()
				lineData = message.dropWhile(_ != ':')
				var clientName = lineData.drop(2)
				println("CLIENT_NAME: " + clientName)
				
				message = sIn.readLine()
				lineData = message.dropWhile(_ != ':')
				var clientMsg = lineData.drop(2)
				println("MESSAGE: " + clientMsg)
				
				//broadcast message to group
				var tempGroup = getGroupR(roomRef.toInt)
				var tempClient = getClient(clientName)
				
				tempGroup.groupMessage(tempClient, clientMsg)
				println(tempClient.handle + "(" + tempClient.joinRef + "): sent message to " 
						+ tempGroup.roomName + "(" + tempGroup.roomRef + ")")
				println("")
				
			}
			
			def error(code:Int, description:String){
				sOut.println("ERROR_CODE: " + code
								+ "\nERROR_DESCRIPTION: " + description)
			}
			
			def base(message:String){
				sOut.println(message + "\nIP:" + IPaddress 
										+"\nPort:" + port 
										+"\nStudentID:" + studentID +"\n")
				sOut.flush()
			}
			
			def disconnect() {
				message = sIn.readLine()
				message = sIn.readLine()
				var lineData = message.dropWhile(_ != ':')
				var clientName = lineData.drop(1)
				
				var tempClient = getClient(clientName)
				tempClient.socket.close
				println(tempClient.handle + "(" + tempClient.joinRef + "): disconnected.") 
				//remove from groups
				for(g <- groups){
					if(g.hasMember(tempClient.joinRef)){			
						//notify group
						var leftMsg = clientName + " has left this chatroom."
						g.groupMessage(getClient(clientName), leftMsg)
						//remove from group
						g.removeMember(tempClient.joinRef)
						//print server message
						println(tempClient.handle + "(" + tempClient.joinRef + "): removed from " 
									+ g.roomName + "(" + g.roomRef + ")")
					} 
				}
			}
			
			def killService(){
				//println("THREAD " + Thread.currentThread().getId()+": kill service invoked")
				shutdown()
				socket.close()
			}
			
		}
		
		/**
		 * Utility function used to identify if a group exists
		 * @para groupName the group name
		 * @return result true if it exists, otherwise false
		**/
		def groupExists(groupName:String): Boolean = {
			if(groups != null){
				for(g <- groups){
					if(g.roomName == groupName){
						return true
					} 
				}
			}
			return false
		}
		
		def groupExists(ref:Int): Boolean = {
			if(groups != null){
				for(g <- groups){
					if(g.roomRef == ref){
						return true
					} 
				}
			}
			return false
		}
		
		def getGroup(groupName:String): Group = {
			for(g <- groups){
				if(g.roomName == groupName){
					return g
				} 
			}
			return null
		}
		
		def getGroupR(ref:Int): Group = {
			for(g <- groups){
				if(g.roomRef == ref){
					return g
				} 
			}
			return null
		}
		
		/**
		 * Utility function used to identify if a client exists
		 * @para clientName the client name
		 * @return result true if it exists, otherwise false
		**/
		def clientExists(clientName:String): Boolean = {
			if(clients != null){
				for(c <- clients){
					if(c.handle == clientName){
						return true
					} 
				}
			}
			return false
		}
		
		def getClient(clientNmae:String): Client = {
			for(c <- clients){
				if(c.handle == clientNmae){
					return c
				} 
			}
			return null
		}
		
		/**
		 * Utility function used to compute a unique id for groups and clients
		 * @para id the global groupId or clientId
		 * @return id the unique id
		**/
		def getUniqueId() : Int = synchronized{
			uniqueId = uniqueId + 1
			return uniqueId
		}
		
		/**
		 *var clientId = 0;
		 * Utility function called when a kill service message is received. Shuts down the server
		 *
		**/
		def shutdown(){
			println("Server shutting down...")
			try {
				serverSocket.close()
				pool.shutdownNow()
				System.exit(0)
			}
			catch {
				case e: SocketException => println("SocketException: closing server socket failed")
			}
		}
	}
  
	/**
	 *
	 * Main function which creates and runs a server instance on a given port.
	 *
	**/
	def main (args: Array[String]){
		new Server(args(0).toInt).run()
	}
}
