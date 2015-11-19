import java.io.{BufferedReader, InputStreamReader, PrintWriter, BufferedWriter, OutputStreamWriter}
/**
 * A class used to represent chat rooms on the server
 *
**/
class Group(name:String, id:Int) {
	val roomName = name
	val roomRef = id
	var members: List[Client] = List()
	
	/**
	 * Adds a user to the group members list
	 *
	**/
	def addMember(user:Client) {
		members = user :: members
	}
	
	/**
	 * Removes a user from the group members list
	 *
	**/
	def removeMember(id:Int) {
		var updatedMembers: List[Client] = List()
		for(m <- members){
			if(m.joinRef != id){
				updatedMembers = m :: updatedMembers
			} 
		}
		members = updatedMembers
	}
	
	/**
	 * Returns true if the group has the specified user listed
	 *
	**/
	def hasMember(id:Int) : Boolean ={
		if(members != null){
			for(m <- members){
				if(m.joinRef == id){
					return true
				} 
			}
		}
		return false
	}
	
	/**
	 * Broadcasts a specified message to all users listed within the group
	 *
	**/
	def groupMessage(sender:Client, message:String){
		for (m <- members) {
				val sOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m.socket.getOutputStream(), "UTF-8")))
				sOut.println("CHAT:" + roomRef
								+ "\nCLIENT_NAME:" + sender.handle
								+ "\nMESSAGE:" + message + "\n")
				sOut.flush()
		}
	}
}