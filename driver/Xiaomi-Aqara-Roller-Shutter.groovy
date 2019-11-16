/**
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *  2017 
 */

metadata {
    definition(name: "Xiaomi Aqara Roller Shutter", namespace: "ShinJjang", author: "ShinJjang", ocfDeviceType: "oic.d.blind") {
        capability "Window Shade"
        capability "Actuator"
        capability "Health Check"
        capability "Sensor"
        capability "Refresh"

        command "pause"

        fingerprint endpointId: "01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0004, 0003, 0005, 000A, 0102, 000D, 0013", outClusters: "000A", manufacturer: "LUMI", model: "lumi.curtain.aq2", deviceJoinName: "Xiaomi Aqara Roller Shutter"
    }

    preferences {
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
    def parseMap = zigbee.parseDescriptionAsMap(description)
    log.debug "parseMap:${parseMap}"
    def event = zigbee.getEvent(description)

    try {
        if (parseMap["cluster"] == "000D" && parseMap["attrId"] == "0055") {
            long theValue = Long.parseLong(parseMap["value"], 16)
            float floatValue = Float.intBitsToFloat(theValue.intValue());
            def curtainLevel = floatValue.intValue()
            log.debug "long => ${theValue}, float => ${floatValue}"
            /*
            if (parseMap.raw.endsWith("00000000") || parseMap["size"] == "16") { //&& parseMap["size"] == "28") {
                log.debug "level => ${curtainLevel}"
            } else if (parseMap.raw.endsWith("00000000") || parseMap["size"] == "1C") {
                */
                def eventStack = []                   
                def windowShadeStatus = ""
                log.debug "level => ${curtainLevel}"
                
                if (curtainLevel == 100) {
                    log.debug "Just Fully Open"
                    windowShadeStatus = "open"
                    curtainLevel = 100
                } else if (curtainLevel > 0) {
                    log.debug curtainLevel + '% Partially Open'
                    windowShadeStatus = "partially open"
                    curtainLevel = curtainLevel
                } else {
                    log.debug "Just Closed"
                    windowShadeStatus = "closed"
                    curtainLevel = 0
                }
                
                if (parseMap.additionalAttrs != null && parseMap.additionalAttrs[0].value.startsWith("03")) {  //A68E00
                    eventStack.push(createEvent(name: "windowShade", value: windowShadeStatus as String))
                }
                
                eventStack.push(createEvent(name: "position", value: curtainLevel))
                return eventStack
            /*
            } else {
                log.debug "running…"
                // log.debug "running… >> ${parseMap}"
            }*/
        } else if (parseMap["clusterId"] == "0102") {
            if (parseMap["attrId"] == "0008") {
                long endValue = Long.parseLong(parseMap["value"], 16)
                def curtainLevel = endValue
                log.debug "endValuecurtainLevel=>${curtainLevel}"
            } else if (parseMap["command"] == "0B") {
                log.debug "stopped"
            }
        } else if (parseMap["cluster"] == "0000" && parseMap["encoding"] == "42") {
            log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
        } else if (parseMap.raw.startsWith("0104")) {
            log.debug "Xiaomi Curtain"
            //  log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
        } else if (parseMap.raw.endsWith("0007")) {
            log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
        } else {
            log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
        }
    } catch (Exception e) {
        log.warn e
    }
}

def updated() {
    def childPauseButton = getChildDevice("${device.deviceNetworkId}-pause")
    
    if( childPauseButton == null ) {
        addChildDevice("uqpqpu", "Xiaomi Window Covering - Pause", "${device.deviceNetworkId}-pause",
           [label: "${device.label == null ? device.name : device.label} - Pause", isComponent: false]
      )
    }
}

def open() {
    setPosition(100)
}

def close() {
    setPosition(0)
}

def pause() {
    log.info "pause()"
    zigbee.command(0x0102, 0x02)
}

def setPosition(level) {
    if (level == null) {
        level = 0
    }
    level = level as int
    Integer currentLevel = device.currentValue("position")
    
    if (level > currentLevel) {
        sendEvent(name: "windowShade", value: "opening")
    } else if (level < currentLevel) {
        sendEvent(name: "windowShade", value: "closing")
    }
    
    if (level == 100) {
        log.info "open()"
        zigbee.writeAttribute(0x000d, 0x0055, 0x39, Float.floatToIntBits(100))
    } else if (level > 0) {
        log.info "Set Level: ${level}%"
        zigbee.writeAttribute(0x000d, 0x0055, 0x39, Float.floatToIntBits(level))
    } else {
        log.info "close()"
        zigbee.writeAttribute(0x000d, 0x0055, 0x39, 00000000)
    }
}

def ping() {
    return refresh()
}

def refresh() {
    log.debug "refresh()"
    zigbee.readAttribute(0x000d, 0x0055)
}