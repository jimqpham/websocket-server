"use strict";

// Grab elements on the page and assign them to variables
let isUsernameSet = false;
let usernameField = document.getElementById("username-field");
let setUsernameBtn = document.getElementById("set-username-btn");

let isRoomnameSet = false;
let roomnameField = document.getElementById("room-field");
let joinRoomBtn = document.getElementById("join-room-btn");

let ws = new WebSocket("ws://localhost:8080");
let memberList = document.getElementById("current-member-list");
let mainCanvas = document.getElementById("main-canvas");

let serverStatus = document.getElementsByClassName("server-status")[0];

let exitRoomBtn = document.getElementById("exit-room-btn");

let composer = document.getElementById("composer");
let sendBtn = document.getElementById("send-btn");

/****************************************
 ******** USER NAME SECTION *************
 ****************************************/

// Return true if username is valid and false otherwise
function validateUsername () {
    if (usernameField.length === 0) {
        alert("Username must not be blank!");
        return false;
    }
    for (let i = 0; i < usernameField.value.length; i++) {
        if (usernameField.value.charAt(i) === ' ') {
            alert("Username does not accept whitespaces!");
            return false;
        }
    }
    return true;
}

// Set the current client's username
// Lock the username field
function setUsername () {
    ws.send("set-username " + usernameField.value);
    usernameField.classList.add("locked");
    usernameField.readOnly = true;
    usernameField.style.backgroundColor = "cornflowerblue";
    usernameField.style.color = "ghostwhite";
    isUsernameSet = true;
    setUsernameBtn.remove();
    unlockRoomnameField();
}

// If the user press enter on the username field, validate and set the username
function handleUsernameFieldKeyPress(event) {
    if (event.keyCode === 13) {
        if (validateUsername()) {
            setUsername();
        }
    }
}

// If the user click the set username button, validate and set the username
function handleSetUsernameBtnClick() {
    if (validateUsername()) {
        setUsername();
    }
}

// Callback assignment
usernameField.onclick = () => {usernameField.select();};
usernameField.onkeypress = handleUsernameFieldKeyPress;
setUsernameBtn.onclick = handleSetUsernameBtnClick;

/****************************************
 ******** ROOM NAME SECTION *************
 ****************************************/

// Return true if room name is valid and false otherwise
function validateRoomname () {
    for (let i=0; i < roomnameField.value.length; i++) {
        if (roomnameField.value.charAt(i) < 'a' || roomnameField.value.charAt(i) > 'z') {
            alert("Room name should only contain lowercase characters!");
            return false;
        }
    }
    return true;
}

// Make room name field editable
// Room name field is only editable when username is set
function unlockRoomnameField() {
    roomnameField.removeAttribute("readonly");
    roomnameField.style.backgroundColor = "ghostwhite";
    roomnameField.value = "enter room name";
}

// Send a request to join room to the server
function joinRoom() {

    let roomnameField = document.getElementById("selected-room");
    let roomName = roomnameField.value;

    ws.send("request-join " + roomName + " " + usernameField.value);
    console.log("request-join " + roomName + " " + usernameField.value);

}

// Set the room name when Enter is pressed
function handleRoomnameFieldKeyPress(event) {
    if (event.keyCode === 13) {
        if (validateRoomname() && validateUsername()) {
            isRoomnameSet = true;
            addRoomToRoomList();
            selectRoom(roomnameField.value);
            joinRoom(roomnameField.value, usernameField.value);
            roomnameField.value = "enter room name";
        }
    }
}

// Set the room name when join button is clicked
function handleJoinRoomBtnClick() {
    if (validateRoomname() && validateUsername()) {
        isRoomnameSet = true;
        addRoomToRoomList();
        selectRoom(roomnameField.value);
        joinRoom(roomnameField.value, usernameField.value);
        roomnameField.value = "enter room name";
    }
}

// Add the room to the list of room on the sidebar
function addRoomToRoomList() {
    let newRoomLabel = document.createElement("label");
    newRoomLabel.appendChild(document.createTextNode("# "));

    let newRoomName = document.createElement("input");
    newRoomName.classList.add("room-name", "locked");
    newRoomName.type = "text";
    newRoomName.value = roomnameField.value;
    newRoomName.readOnly = true;
    newRoomName.onclick = () => selectRoom(newRoomName.value);

    let newRoomWrap = document.createElement("li");
    newRoomWrap.classList.add("room-name-wrap");
    newRoomWrap.appendChild(newRoomLabel);
    newRoomWrap.appendChild(newRoomName);

    let currentRoomList = document.getElementById("current-room-list");
    currentRoomList.appendChild(newRoomWrap);
}

// remove the room from the list of room on the sidebar
function removeRoomFromRoomList(roomname) {
    roomname.parentElement.remove();
}

// SELECT A ROOM TO DISPLAY ITS CONTENT ON THE CANVAS
function selectRoom(selectedRoomname) {
    console.log(selectedRoomname + " is selected.");
    // Remove the previously selected one
    let oldSelected = document.getElementById("selected-room");
    if (oldSelected !== null) {
        oldSelected.removeAttribute("id");
    }

    // Have the new one selected
    let newSelected;
    let roomList = document.getElementsByClassName("room-name");
    for (let roomname of roomList) {
        if (roomname.value === selectedRoomname) {
            newSelected = roomname;
        }
    }
    newSelected.id = "selected-room";
    removeNotification(newSelected);

    // Refresh the canvas, show the exit room button
    // Show the room name, load the room's messages
    // And update the member list
    let chatroomTitle = document.getElementById("chatroom-title");
    chatroomTitle.innerText = "chatroom: #" + newSelected.value;
    let exitRoomBtn = document.getElementById("exit-room-btn");
    exitRoomBtn.style.display = "unset";
    unlockComposer();
    mainCanvas.innerHTML = "";
    clearMemberList();
    ws.send("request-room-load " + selectedRoomname);
}

// Return the name of the room currently selected
function getSelectedRoom() {
    let selectedRoom = document.getElementById("selected-room");
    return selectedRoom.value;
}

// Make the room name bolder if that room is not active/selected but receives a new message
function addNotification(roomname) {
    roomname.style.fontWeight = "900";
}

// Make the room name normal if the user clicks on that room to see the notification
function removeNotification(roomname) {
    roomname.style.fontWeight = "normal";
}

// Assigning callbacks
roomnameField.onclick = () => {roomnameField.select();};
roomnameField.onkeypress = handleRoomnameFieldKeyPress;
joinRoomBtn.onclick = handleJoinRoomBtnClick;
let roomnames = document.getElementsByClassName("room-name");
for (let roomname of roomnames) {
    roomname.onclick = () => selectRoom(roomname.value);
}

/****************************************
 ************ MEMBER LIST ***************
 ****************************************/

// Add member to member list on sidebar
function addMemberToList(member) {
    let newMember = document.createElement("li");
    newMember.innerHTML = "<label>@ </label>" +
        "<input class=\"member-name locked\" type=\"text\" value=\""
        + member + "\" readonly=\"readonly\"></li>";
    memberList.appendChild(newMember);
}

// Clear all the member
function clearMemberList() {
    memberList.innerHTML = "";
}

function updateMemberList(members) {
    clearMemberList();
    for (let member of members) {
        addMemberToList(member);
    }
}

/**********************************************************
 ********* SERVER STATUS & WEBSOCKET CB SECTION ***********
 **********************************************************/

function handleWSOpen() {
    serverStatus.value = "> connected";
}

function handleWSClose() {
    serverStatus.value = "> disconnected";
}

function handleWSMessage(event) {
    console.log("Receiving messages from server:\n" + event.data);
    let jsonMessage = JSON.parse(event.data);

    if (jsonMessage.type === "system-message") {
        if (jsonMessage.room === getSelectedRoom()) {
            printSystemMessage(jsonMessage["system-message-content"]);
        }
        else {
            for (let roomname of roomnames) {
                if (jsonMessage.room === roomname.value) {
                    addNotification(roomname);
                }
            }
        }
    }

    if (jsonMessage.type === "user-message") {
        if (jsonMessage.room === getSelectedRoom()) {
            printUserMessage(jsonMessage["user-message-sender"], jsonMessage["user-message-content"], jsonMessage["user-message-timestamp"]);
        }
        else {
            for (let roomname of roomnames) {
                if (jsonMessage.room === roomname.value) {
                    addNotification(roomname);
                }
            }
        }
    }

    if (jsonMessage.type === "member-list") {
        if (jsonMessage.room === getSelectedRoom()) {
            updateMemberList(jsonMessage["member-list"]);
        }
    }
}

function handleWSError() {
    serverStatus.value = "> error";
}

ws.onopen = handleWSOpen;
ws.onclose = handleWSClose;
ws.onmessage = handleWSMessage;
ws.onerror = handleWSError;

/****************************************
 ********* TOP BAR SECTION **************
 ****************************************/

// Exit a room, clear the canvas and member list
function handleExitRoomBtnClick() {
    let selectedRoom = document.getElementById("selected-room");
    selectedRoom.removeAttribute("id");
    ws.send("request-exit " + selectedRoom.value + " " + usernameField.value);
    console.log("request-exit " + selectedRoom.value + " " + usernameField.value);

    clearCanvas();
    clearMemberList();

    let chatroomTitle = document.getElementById("chatroom-title");
    chatroomTitle.innerText = "chatroom: no room selected\n\nenter username and join a room to start";

    let exitRoomBtn = document.getElementById("exit-room-btn");
    exitRoomBtn.style.display = "none";

    removeRoomFromRoomList(selectedRoom);
    lockComposer();
}

exitRoomBtn.onclick = handleExitRoomBtnClick;

/****************************************
 ******* MAIN CANVAS SECTION ************
 ****************************************/

function clearCanvas() {
    mainCanvas.innerHTML = "";
}

// print out the message from the system
function printSystemMessage(message) {

    let textNode = document.createTextNode(message);
    let systemMessage = document.createElement("p");
    systemMessage.classList.add("system-message-content");
    systemMessage.appendChild(textNode);
    let systemMessageBlock = document.createElement("div");
    systemMessageBlock.classList.add("system-message-block");
    systemMessageBlock.appendChild(systemMessage);
    mainCanvas.appendChild(systemMessageBlock);

    systemMessageBlock.scrollIntoView();
}

// print out the message from user
function printUserMessage(sender, content, timestamp) {
    let senderElem = document.createElement("p");
    senderElem.appendChild(document.createTextNode("@" + sender));
    senderElem.classList.add("message-sender");

    let contentElem = document.createElement("p");
    contentElem.appendChild(document.createTextNode(content));
    contentElem.classList.add("message-content");

    let timestampElem = document.createElement("p");
    timestampElem.appendChild(document.createTextNode(timestamp));
    timestampElem.classList.add("message-timestamp");

    let userMessageBlock = document.createElement("div");
    userMessageBlock.classList.add("user-message-block");
    if (sender === usernameField.value) {
        userMessageBlock.classList.add("from-current-user");
    }
    userMessageBlock.appendChild(senderElem);
    userMessageBlock.appendChild(contentElem);
    userMessageBlock.appendChild(timestampElem);

    mainCanvas.appendChild(userMessageBlock);
    userMessageBlock.scrollIntoView();
}

/****************************************
 ********** COMPOSER SECTION ************
 ****************************************/

function unlockComposer() {
    composer.removeAttribute("readonly");
    composer.style.backgroundColor = "#FFFFFF";
    composer.value = "";
}

function lockComposer() {
    composer.readOnly = true;
    composer.style.backgroundColor = "gray";
    composer.value = "Enter username and join a room first.";
}

function sendUserMessageToServer() {
    if (composer.value.trim() !== "") {
        let selectedRoom = document.getElementById("selected-room");
        ws.send("user-message " + selectedRoom.value + " " + usernameField.value
            + " " + new Date().getTime() + " " + composer.value);
        console.log("user-message " + selectedRoom.value + " " + usernameField.value
            + " " + new Date().getTime() + " " + composer.value);
    }
}

function handleComposerKeyPress(event) {
    if (event.keyCode === 13) {
        sendUserMessageToServer();
        composer.value = "";
        event.preventDefault();
    }
}

function handleSendBtnClick() {
    sendUserMessageToServer();
    composer.value = "";
}

composer.onkeypress = handleComposerKeyPress;
composer.onclick = () => {composer.select()};
sendBtn.onclick = handleSendBtnClick;