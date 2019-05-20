# sessions-store

A short explanation about the assignment.

Good day.

The application is implemented using SpringBoot with Gradle Wrapper. So it can be easily ran in IDE.
There is also a Dockerfile in the project, so it's possible to build and run the app in a container.

The endpoints are implmemented accroding to requirements in the document and can be reached via specified urls, e.g. http://localhost:8080/chargingSessions .

Application is thread-safe in a sense that fields that hold the maps are private and are only accessed via synchronised methods. 

# About computational complexity:

SessionStore is represented by a HashMap from a date time string to a sessions HashMap from uuid to a ChargingSession. The key of the first map is DateTime in ISO format without seconds converted to string, i.e. a ChargingSession updated on "2019-05-06T19:00:20.529" will be stored to a collection with a key "2019-05-06T19:00".

Also there is an index map which helps to find a date time key by a session id. 

**a. Adding a session to a store requires the following operations:**
  1. Getting a sessions map from the store map.
  2. Putting a new session to the session map.
  3. Putting the new session to the index map. 
  
  Looks like that in Java 8 the worse case scenarios for HashMap operations is O(log(n)). Which is what was required.
  
**b. Stopping a session:**
  1. Getting a DateTime key from the index map.
  2. Getting a sessions map from the store.
  3. Getting a session from the sessions map and updating some of it's values.
  4. Removing the session's id from the sessions map.
  5. Removing the session's id from the index.
  6. Adding the udpdated session back to the store and the index.
  
  Still looks like O(log(n)) to me.
  
**c. Getting all sessions:**
  Whatever I do here, it will still fit the "any" requirement:)
 
**d. Retrieving a summary:**
  This one is a little bit tricky. The requirement says to get "charging sessions for the last minute". Using the store map it's possilbe to to get sessions for every minute with one operation. But if the call is done during the first second of the minute (e.g. "2019-05-06T19:00:01.000"), then only sessions updated this second will be returned.
  
  That's why I have some extra steps to get sessions for last 60 seconds. Also I had to add a "stoppedAt" field which wasn't specified in the documents. So, the steps are: 
  1. Get a sessions map for the current minute.
  2. Get a sessions map for the previous minute.
  3. Iterate through sessions for the previous minute and calculate if they weren't updated earlier than 60 seconds ago.
  Iterating through all map's values is O(n), but in this case we iteration only through sessions for one minute. If the app runs for 1 day, then in will be 1440 sessions maps in the store map. So I think that complexity can still be O(log(n)) here. 
  
Thanks for an interesting assignemnt. If you have any questions, please let me know.
