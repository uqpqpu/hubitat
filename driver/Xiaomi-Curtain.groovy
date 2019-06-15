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
  definition(name: "Xiaomi Curtain", namespace: "uqpqpqu", author: "uqpqpqu", vid: "SmartThings-smartthings-Springs_Window_Fashions_Shade", ocfDeviceType: "oic.d.blind") {
    capability "Actuator"
    capability "Health Check"
    capability "Refresh"
    capability "Window Shade" 
    
    attribute "position", "decimal"
    attribute "Window Shade", "enum",  ["opening", "partially open", "closed", "open", "closing", "unknown"]

    // fingerprint endpointId: "0x01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0004, 0003, 0005, 000A, 0102, 000D, 0013, 0006, 0001, 0406", outClusters: "0019, 000A, 000D, 0102, 0013, 0006, 0001, 0406"
    fingerprint endpointId: "01", profileId: "0104", deviceId: "0202", inClusters: "0000, 0004, 0003, 0005, 000A, 0102, 000D, 0013, 0006, 0001, 0406", outClusters: "0019, 000A, 000D, 0102, 0013, 0006, 0001, 0406"
  }

  command "stop"

  preferences {
    input name: "mode", type: "bool", title: "Xiaomi Curtain Direction Set", description: "Reverse Mode ON", required: true, displayDuringSetup: true
  }
}

// Parse incoming device messages to generate events
def parse(String description) {
  def parseMap = zigbee.parseDescriptionAsMap(description)
  // log.debug "parseMap:${parseMap}"
  def event = zigbee.getEvent(description)

  try {
    if (parseMap.raw.startsWith("0104")) {
      log.debug "Xiaomi Curtain"
      //log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
    } else if (parseMap.raw.endsWith("0007")) {
      //log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
      log.debug "running…"
      if (state.targetPosition > state.previousPosition) {
        sendEvent(name:"windowShade", value: "opening")
      } else if (state.targetPosition < state.previousPosition) {
        sendEvent(name:"windowShade", value: "closing")
      }
    } else if (parseMap.endpoint && parseMap.endpoint.endsWith("01")) {
      //log.debug "endpoint 01 Event - parseMap:${parseMap}, event:${event}"
      if (parseMap["cluster"] == "000D" && parseMap["attrId"] == "0055") {
        long theValue = Long.parseLong(parseMap["value"], 16)
        float floatValue = Float.intBitsToFloat(theValue.intValue());
        def windowShadeStatus = ""
        int position = floatValue.intValue()
        if(mode == true) {
          if (theValue > 0x42c70000) {
            log.debug "pared: Just Closed"
            windowShadeStatus = "closed"
            position = 0
          } else if (theValue > 0) {
            log.debug "pared: ${position} % Partially Open"
            windowShadeStatus = "partially open"
            position = 100 - floatValue.intValue()
          } else {
            log.debug "pared: Just Fully Open"
            windowShadeStatus = "open"
            position = 100
          }
        } else {
          if (theValue > 0x42c70000) {
            log.debug "parsed: Just Fully Open"
            windowShadeStatus = "open"
            position = 100
          } else if (theValue > 0) {
            log.debug "parsed: ${position} % Partially Open"
            windowShadeStatus = "partially open"
            position = floatValue.intValue()
          } else {
            log.debug "parsed: Just Closed"
            windowShadeStatus = "closed"
            position = 0
          }
        }

        state.previousPosition = position

        def eventStack = []
        eventStack.push(createEvent(name:"windowShade", value: windowShadeStatus as String))
        eventStack.push(createEvent(name:"position", value: position))

        return eventStack
      }
    } else {
      if (parseMap["clusterId"] == "0102") {
        log.debug "parsed stop event"
        runIn(1, refresh)
        return
      }
      // parseMap:[raw:catchall: 0104 0102 01 01 0040 00 4745 00 00 0000 0B 01 0200, profileId:0104, clusterId:0102, clusterInt:258, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:4745, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[02, 00]], event:[:]
      log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
    }
  } catch (Exception e) {
    log.warn e
    log.debug "Unhandled Event - description:${description}, parseMap:${parseMap}, event:${event}"
  }
}

def close() {
  setPosition(0)
}

def open() {
  setPosition(100)
}

def stop() {
  log.debug "stop()"
  zigbee.command(0x0102, 0x02)
//  zigbee.readAttribute(0x000d, 0x0055)
  //setPosition(state.previousPosition)
//  zigbee.command(0x0102, 0x02)
//  zigbee.readAttribute(0x000d, 0x0055)
  //delayBetween([
  //  zigbee.command(0x0102, 0x02),
  //  "delay 1000",
  //  zigbee.readAttribute(0x000d, 0x0055)
  //])
  //zigbee.readAttribute(0x000d, 0x0055)
  //log.debug "end stop()"
  //runIn(1, refresh)
}

def setPosition(position) {
  if (position == null || position < 0) {
    position = 0
  } else if (position > 100) {
    position = 100
  }
  position = position as int

  state.targetPosition = position
    
  if (mode == true) {
    if(position == 100) {
      log.debug "Set Close"
      zigbee.command(0x0006, 0x00)
    } else if(position < 1) {
      log.debug "Set Open"
      zigbee.command(0x0006, 0x01)
    } else {
      log.debug "Set position: ${position}%"
      def f = 100 - position
      String hex = Integer.toHexString(Float.floatToIntBits(f)).toUpperCase()
      zigbee.writeAttribute(0x000d, 0x0055, 0x39, Float.floatToIntBits(position))
    }  
  } else {
    zigbee.writeAttribute(0x000d, 0x0055, 0x39, Float.floatToIntBits(position))

    // if (position == 100){
    //   log.debug "Set Open"
    //   //zigbee.command(0x0006, 0x01)
    //   zigbee.writeAttribute(0x000d, 0x0055, 0x39, Float.floatToIntBits(position))
    // } else if(position > 0) {
    //   log.debug "Set position: ${position}%"
    //   String hex = Integer.toHexString(Float.floatToIntBits(position)).toUpperCase()
    //   zigbee.writeAttribute(0x000d, 0x0055, 0x39, Float.floatToIntBits(position))
    // } else {
    //   log.debug "Set Close"
    //   zigbee.writeAttribute(0x000d, 0x0055, 0x39, Float.floatToIntBits(position))
    //   //zigbee.command(0x0006, 0x00)
    // }
  }
}

def refresh() {
  log.debug "refresh()"
//    "st rattr 0x${device.deviceNetworkId} ${1} 0x000d 0x0055"
   zigbee.readAttribute(0x000d, 0x0055)
}