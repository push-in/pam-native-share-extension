import Foundation
import PamNative
public final class ShareExtensionModule:NativeModule,@unchecked Sendable{
 public init(){}
 public func invoke(method:String,payload:Data,completion:@escaping ModuleCompletion){guard method=="drain"else{completion(.failure,Data("Unknown method: \(method)".utf8));return};do{let group="group.\(Bundle.main.bundleIdentifier ?? "").pam-native";guard let defaults=UserDefaults(suiteName:group)else{throw InboxError.missingGroup};let rows=defaults.array(forKey:"pam.share.items")as?[[String:Any]] ?? [];defaults.removeObject(forKey:"pam.share.items");let json=try JSONSerialization.data(withJSONObject:rows);completion(.success,try WireMap.encode(["json":.text(String(data:json,encoding:.utf8) ?? "[]")]))}catch{completion(.failure,Data(String(describing:error).prefix(1024).utf8))}}
}
private enum InboxError:Error{case missingGroup}
