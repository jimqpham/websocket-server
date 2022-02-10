# Features added

### Improve communication between the server and clients

The client (or the JavaScript code) can send the following messages/requests:
  - `user-message < room > < sender > < enoch-timestamp > < content >`: Send the message from the user to the server for distribution
  - `set-username < username >`: Send a request to set the username for the current thread
  - `request-join < room > < username >`: Send a request to join a room
  - `request-room-load < room >`: Send a request asking for data (chat history and member list) of a room
  - `request-exit < username > < room >`: Send a request to exit a room


The server can send JSON Objects that contain the following field:
  - `{"type", "user-message-sender", "user-message-timestamp", "user-message-content", "room"}`: If the JSON object contains a message from an actual user. The `"type"` is `"user-message"`.
  - `{"type", "system-message-content", "room"}`: If the JSON object contains a notification/message from the system. The `"type"` is `"system-message"`.
  - `{"type", "member-list", "room"}`: If the JSON object contains a list of members in a room. The `"type"` is `"member-list"`.
  - Depending on the `"type"` of the JSON object it receives, the JavaScript will take appropriate actions. If it receives a system message or a user message, it prints them out on the canvas. If it receives a member list, it updates the current member list displayed on screen.
          

### Improve Front-End UI
          
  - Add clarifications to guide the user through the step: Create a username > Join a room > Send a message. Add checks to make sure the user follows that exact process: The room field won't open unless the username field is set. The composer won't open unless both the room and the username is set.
  - Allow the user to be in multiple rooms at once. When the user enters a room, the room name field on the left side bar remains prompting the user to enter another room. Click on any active rooms on the left side bar to load its content on screen.
  - If a message comes at a time when the user isn't looking at that room (but still in it), the respective room name on the sidebar turns bolder.
  - Add the member list to the sidebar showing who is in the room at that time.
  - Add the exit button when entering a room allowing users to exit at will
  - Different format for the system messages (no borders, center-aligned) and the user messages (bordered). The messages coming from the current user will show up to the right of their screen, while other messages from other users show up on the left.
  - Permanent storage: When the server is killed and loaded back up again, it draws data from the permanent storage so the previous messages won't be lost forever.
          
