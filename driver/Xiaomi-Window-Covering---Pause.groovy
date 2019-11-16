/**
 * eZEX C2O Light Switch Child Device - v1.0.2
 *
 *  github: Euiho Lee (flutia)
 *  email: flutia@naver.com
 *  Date: 2018-10-29
 *  Copyright flutia and stsmarthome (cafe.naver.com/stsmarthome/)
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
metadata {
    definition(name: "Xiaomi Window Covering - Pause", namespace: "uqpqpu", author: "uqpqpu", vid: "generic-switch", ocfDeviceType: "oic.d.light") {
        capability "Switch"
        capability "Actuator"
        capability "Refresh"
  }
}

def on() {
  parent.pause()
    sendEvent(name:"switch", value: "on")
    runIn(3, off)
}

def off() {
    sendEvent(name:"switch", value: "off")
}

void refresh() {
    parent.refresh()
}