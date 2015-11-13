import java.io.{PrintStream, IOException}

/**
 * A class used to represent chat rooms on the server
 *
**/
class Group(name:String, id:Int) {
	val roomName = name
	val roomRef = id
	var members: List[Client] = List()
	var transmission = false;
	
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
		members.filterNot(m => m.joinRef == id)
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
		transmission = true
		for (m <- members) {
			var sOut = new PrintStream(m.socket.getOutputStream())
			sOut.println("CHAT:" + roomRef
							+ "\nCLIENT_NAME:" + sender.handle
							+ "\nMESSAGE:" + message + "\n")
			sOut.flush()
		}
		transmission = false
	}
}