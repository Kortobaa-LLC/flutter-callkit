//
//  VoIPController.swift
//  connectycube_flutter_call_kit
//
//  Created by Tereha on 19.11.2021.
//

import Foundation
import PushKit

class VoIPController : NSObject{
    let callKitController: CallKitController
    var tokenListener : ((String)->Void)?
    var voipToken: String?
    
    public required init(withCallKitController callKitController: CallKitController) {
        self.callKitController = callKitController
        super.init()
        
        //http://stackoverflow.com/questions/27245808/implement-pushkit-and-test-in-development-behavior/28562124#28562124
        let pushRegistry = PKPushRegistry(queue: DispatchQueue.main)
        pushRegistry.delegate = self
        pushRegistry.desiredPushTypes = Set<PKPushType>([.voIP])
    }
    
    func getVoIPToken() -> String? {
        return voipToken
    }
}

//MARK: VoIP Token notifications
extension VoIPController: PKPushRegistryDelegate {
    func pushRegistry(_ registry: PKPushRegistry, didUpdate pushCredentials: PKPushCredentials, for type: PKPushType) {
        if pushCredentials.token.count == 0 {
            print("[VoIPController][pushRegistry] No device token!")
            return
        }
        
        print("[VoIPController][pushRegistry] token: \(pushCredentials.token)")

        // Original encoding
         let deviceToken: String = pushCredentials.token.reduce("", {$0 + String(format: "%02X", $1) })
        //Custom encoding
       // let deviceToken: String = pushCredentials.token.hexEncodedString()

        print("NATIVE [VoIPController][pushRegistry] deviceToken: \(deviceToken)")
        
        self.voipToken = deviceToken
        
        if tokenListener != nil {
            print("[VoIPController][pushRegistry] notify listener")
            tokenListener!(self.voipToken!)
        }
    }
    
    func pushRegistry(_ registry: PKPushRegistry, didReceiveIncomingPushWith payload: PKPushPayload, for type: PKPushType) {
        print("[VoIPController][pushRegistry][didReceiveIncomingPushWith] payload: \(payload.dictionaryPayload)")
        if type == .voIP{
        // Process Push 
        processVoIPPush(with: payload.dictionaryPayload) 
        }
    }


    // Push VoIP Processing
    private func processVoIPPush(with callData: Dictionary<AnyHashable, Any>
                                
         ) {
             NSLog("Enigma Push Notification Handling")
            // !
            let callId = callData["uuid"] as! String
            let callType =  2 // callData["call_type"] as! Int // => video : 1 / audio : 2
            let callInitiatorId = callData["caller_id"] as! Int
            let callInitiatorName = callData["caller_name"] as! String
            let callOpponentsString =  "" // callData["call_opponents"] as! String // FIXME
            let callOpponents = callOpponentsString.components(separatedBy: ",")
                .map { Int($0) ?? 0 }
            let userInfo = callData["token"] as! String // callData["user_info"] as? String // => Getting MEeting Token in userInfo
            // !

            // * Report the incoming voip push to callkit
            self.callKitController.reportIncomingCall(uuid: callId.lowercased(), callType: callType, callInitiatorId: callInitiatorId, callInitiatorName: callInitiatorName, opponents: callOpponents, userInfo: userInfo) { (error) in
                if(error == nil){
                    print("[VoIPController][didReceiveIncomingPushWith] reportIncomingCall SUCCESS")
                } else {
                    print("[VoIPController][didReceiveIncomingPushWith] reportIncomingCall ERROR: \(error?.localizedDescription ?? "none")")
                }
            }
    
         } // Process Push
}


/// * ############## Helper Classes & Extensions ##############

extension Foundation.Data {
    struct HexEncodingOptions: OptionSet {
        let rawValue: Int
        static let upperCase = HexEncodingOptions(rawValue: 1 << 0)
    }

    func hexEncodedString(options: HexEncodingOptions = []) -> String {
        let format = options.contains(.upperCase) ? "%02hhX" : "%02hhx"
        return self.map { String(format: format, $0) }.joined()
    }
}