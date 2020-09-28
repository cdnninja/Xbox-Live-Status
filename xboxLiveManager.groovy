/**
 *  XboxLiveStatus
 *
 *  Copyright 2020 cdnninja
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Xbox Live Manager",
    namespace: "cdnninja",
    author: "cdnninja",
    description: "Call XAPI for xbox status with the intent of giving lighting control. ",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {

    page(name: "authPage")

}




/* Auth Page */
def authPage() {
    return dynamicPage(name: "authPage", , install: true) {
        section("X API Key") {
            input "APIKey", "text", "title": "API Key", multiple: false, required: true
            input "pollEnable", "bool", title: "Enable Polling", defaultValue: "true", submitOnChange: true

        }
    }
}

// The toQueryString implementation simply gathers everything in the passed in map and converts them to a string joined with the "&" character.
String toQueryString(Map m) {
        return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}


def clientListOpt() {
    getClientList().collect{[(it.key): it.value]}
}


def installed() {
    state.appInstalled = true
    log.debug "Installed with settings: ${settings}"
    initialize()   
}

def initialize() {
	state.authenticationToken = APIKey
    updateUserData()
    if (pollEnable) {
        runEvery1Minute(regularPolling)
    }
    
}


def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()

    if(state.authenticationToken) {

          updateUser();
        
    }


    if (!state.authenticationToken) {
        authPage()
    }

    initialize()

    subscribe(location, null, response, [filterEvents:false])   

}

def uninstalled() {
log.debug "Uninstalled()"
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {

    try {
        delete.each {
            deleteChildDevice(it.deviceNetworkId)
            log.info "Successfully Removed Child Device: ${it.displayName} (${it.deviceNetworkId})"
        }
    }
    catch (e) { log.error "There was an error (${e}) when trying to delete the child device" }
}

def response(evt) {	 

    log.trace "in response(" + evt + ")";

    def msg = parseLanMessage(evt.description);
  /*  if(msg && msg.body && msg.body.startsWith("<?xml")){

        def mediaContainer = new XmlSlurper().parseText(msg.body)

        log.debug "Parsing /status/sessions"
        getChildDevices().each { pht ->

            log.debug "Checking $pht for updates"

            // Convert the devices full network id to just the IP address of the device
            //def address = getPHTAddress(pht.deviceNetworkId);
            def identifier = getPHTIdentifier(pht.deviceNetworkId);

            // Look at all the current content playing, and determine if anything is playing on this device
            def currentPlayback = mediaContainer.Video.find { d -> d.Player.@machineIdentifier.text() == identifier }

            // If there is no content playing on this device, then the state is stopped
            def playbackState = "stopped";            

            // If we found active content on this device, look up its current state (i.e. playing or paused)
            if(currentPlayback) {

                playbackState = currentPlayback.Player.@state.text();
            }            

            log.trace "Determined that $pht is: " + playbackState

            //pht.setPlaybackState(playbackState);

            log.trace "Current playback type:" + currentPlayback.@type.text()
            pht.playbackType(currentPlayback.@type.text())
            switch(currentPlayback.@type.text()) {
                case "movie":
                pht.setPlaybackTitle(currentPlayback.@title.text());
                break;

                case "":
                pht.setPlaybackTitle("...");
                break;

                case "clip":
                pht.setPlaybackTitle("Trailer");
                break;  

                case "episode":
                pht.setPlaybackTitle(currentPlayback.@grandparentTitle.text() + ": " + currentPlayback.@title.text());
            }
        }

    }
    */
}
def updateUI(){
	log.debug "Updating UI"
	def xboxLiveUser = state.gamerTag
    if(xboxLiveUser) { 

        log.info "Updating Xbox User: " + xboxLiveUser

        def children = getChildDevices();
        def child_deviceNetworkID = childDeviceID(xboxLiveUser);
        log.debug "Children DeviceID: " + child_deviceNetworkID
        log.debug "Children: " + children
        def liveUser = children.find{ d -> d.deviceNetworkId.contains(xboxLiveUser) 
      	log.debug "OnlineState:" + state.onlineState
        if(state.onlineState != "Offline") {
            log.debug "Titles: " + state.devices[0].titles
            def ActiveApp = state.devices[0].titles.find{active -> active.placement == "Full"}
            log.debug "active app: "+  ActiveApp
        }
        if(state.onlineState == "Offline")
        {
        	log.debug "setting " + children[0] + " to stopped"
   			children[0].setPlaybackState("stopped");
            children[0].setPlaybackTitle("");
        } else if (state.onlineState == "Online" && ActiveApp.name == "Home")
        {
        	log.debug "setting: " + children[0] + " to Paused"
        	children[0].setPlaybackState("paused");
            log.debug "device list: " + state.devices
            children[0].setPlaybackTitle(ActiveApp.name);
        } else {
        	log.debug "setting: " + children[0] + " to Playing"
        	children[0].setPlaybackState("playing");
            log.debug "device list: " + state.devices
            children[0].setPlaybackTitle(ActiveApp.name);
        
        }
        
        
  
        }  



}
}
def updateUser(){
	def xboxLiveUser = state.gamerTag
 
    if(xboxLiveUser) { 

        log.info "Updating Xbox User: " + xboxLiveUser

        def children = getChildDevices();
        def child_deviceNetworkID = childDeviceID(xboxLiveUser);
        log.debug "Children DeviceID: " + child_deviceNetworkID
        log.debug "Children"
        def liveUser = children.find{ d -> d.deviceNetworkId.contains(xboxLiveUser)  }  
        if(!liveUser){ 
        	def liveuser;	
            // The Xbox User does not exist, create it
            log.debug "This Xbox Live User does not exist, creating a new one now " + xboxLiveUser + location.name + "Location: "+ location.hubs[0].id
            liveuser = addChildDevice("cdnninja", "XboxLiveStatus", xboxLiveUser, location.hubs[0]?.id, [Label:xboxLiveUser, name:xboxLiveUser])	
            log.debug "User Created"
        } 
        
		
        // Renew the subscription
        subscribe(liveuser, "switch", switchChange)
    }
}

def String childDeviceID(xboxLiveUser) {

    def id = "LiveUser." + xboxLiveUser
    log.trace "childDeviceID: $id";
    return id;
}
def String getPHTAddress(deviceNetworkId) {

    //def parts = deviceNetworkId.tokenize('.');
    //def part = parts[6] + "." + parts[7] + "." + parts[8] + "." + parts[9];
    //log.trace "PHTAddress: $part"

    return part;
}
def String getPHTIdentifier(deviceNetworkId) {

   // def parts = deviceNetworkId.tokenize('.');
    //def part = parts[5];    
    //log.trace "PHTIdentifier: $part"

    return part;
}
def String getPHTCommand(deviceNetworkId) {

   // def parts = deviceNetworkId.tokenize('.');
   // def part = parts[10];
    //log.trace "PHTCommand: $part"

    return part
}
def String getPHTAttribute(deviceNetworkId) {

   // def parts = deviceNetworkId.tokenize('.');
   // def part = parts[11];
    //log.trace "PHTAttribute: $part"

    return parts[11];
}

def switchChange(evt) {

    // We are only interested in event data which contains 
    if(evt.value == "on" || evt.value == "off") return;   

    log.debug "Plex Home Theatre event received: " + evt.value;

    def parts = evt.value.tokenize('.');

    // Parse out the PHT IP address from the event data
    def phtIP = getPHTAddress(evt.value);

    // Parse out the new switch state from the event data
    def command = getPHTCommand(evt.value);

    //log.debug "phtIP: " + phtIP
    log.debug "Command: $command"

    switch(command) {
        case "next":
        log.debug "Sending command 'next' to $phtIP"
        next(phtIP);
        break;

        case "previous":
        log.debug "Sending command 'previous' to $phtIP"
        previous(phtIP);
        break;

        case "play":
        case "pause":
        // Toggle the play / pause button for this PHT
        playpause(phtIP);
        break;

        case "stop":            
        stop(phtIP);
        break;

        case "scanNewClients":
        getClients();

        case "setVolume":
        setVolume(phtIP, getPHTAttribute(evt.value));
        break;
    }

    return;
}

def setVolume(phtIP, level) {
    log.debug "Executing 'setVolume'"

    //executeRequest("/system/players/$phtIP/playback/setParameters?volume=$level", "GET");
}

def regularPolling() { 


    log.debug "Polling for user state"
    if(state.authenticationToken) {
        updateUserStatus();
    }

}

def updateUserStatus(){
    log.debug "Executing 'updateUserStatus'"
	log.debug "xuid: " + state.accountXUID
    def resp = executeRequest("${state.accountXUID}/presence", "GET")
    state.onlineState = resp.data.state 
    state.LastSeen = resp.data.lastSeen
    state.devices = resp.data.devices
    log.debug "OnlineState is: " + state.onlineState
    log.debug "Devices: " + state.devices
    log.debug "Last Seen: " + state.LastSeen
    updateUI()

}

def updateUserData(){
    log.debug "Executing 'updateXUID'"

    def resp = executeRequest("accountXuid", "GET")
    state.accountXUID = resp.data.xuid
    state.gamerTag = resp.data.gamerTag
    log.debug "xuid: " + state.accountXUID
    log.debug "Gamer Tag: " + state.gamerTag

	
}


def playpause(phtIP) {
    log.debug "Executing 'playpause'"

  //  executeRequest("/system/players/" + phtIP + "/playback/play", "GET");
}

def stop(phtIP) {
    log.debug "Executing 'stop'"

    //executeRequest("/system/players/" + phtIP + "/playback/stop", "GET");
}

def next(phtIP) {
    log.debug "Executing 'next'"

   // executeRequest("/system/players/" + phtIP + "/playback/skipNext", "GET");
}

def previous(phtIP) {
    log.debug "Executing 'next'"

   // executeRequest("/system/players/" + phtIP + "/playback/skipPrevious", "GET");
}

def executeRequest(Path, method) {

    log.debug "The " + method + " path is: " + Path;


    
    def params = [
        uri: "https://xapi.us/v2/",
        path : Path,
        headers: [
            'X-AUTH': state.authenticationToken,
        ]
    ]
	def response
    try {    
        httpGet(params) { resp ->
            response = resp
            
            
        }

       
    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $params"
        log.debug "Storing the failed action to try later"
}
	log.debug "response: " + response
    //updateUI()
    return response
    
}


/* Helper functions to get the network device ID */
private String NetworkDeviceId(){
    def iphex = convertIPtoHex(settings.piIP).toUpperCase()
    def porthex = convertPortToHex(settings.piPort)
    return "$iphex:$porthex" 
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    //log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    //log.debug hexport
    return hexport
}

