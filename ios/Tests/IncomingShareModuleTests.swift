import UIKit
import XCTest
import PamNative
@testable import PamNativeShareExtension

final class IncomingShareModuleTests: XCTestCase {
    func testInitialConsumesOnlyFirstShareAndCopiesFilesIntoPrivateSandbox() throws {
        let suite = "dev.pam.incoming-share-test.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        let base = FileManager.default.temporaryDirectory
            .appendingPathComponent("pam-incoming-test-\(UUID().uuidString)")
        let groupRoot = base.appendingPathComponent("group")
        let privateRoot = base.appendingPathComponent("private")
        let inbox = groupRoot.appendingPathComponent("pam-share-inbox")
        try FileManager.default.createDirectory(at: inbox, withIntermediateDirectories: true)
        defer {
            defaults.removePersistentDomain(forName: suite)
            try? FileManager.default.removeItem(at: base)
        }
        let fileName = UUID().uuidString
        try Data("photo bytes".utf8).write(to: inbox.appendingPathComponent(fileName))
        defaults.set([
            ["kind": 1, "value": "Olá 🌎", "mimeType": "text/plain", "shareId": "first", "subject": "Picture", "createdAtMillis": 1],
            ["kind": 3, "value": fileName, "name": "original.jpg", "mimeType": "public.jpeg", "shareId": "first", "createdAtMillis": 1],
            ["kind": 1, "value": "next share", "mimeType": "text/plain", "shareId": "second", "createdAtMillis": 1],
        ], forKey: "pam.share.items")
        let module = IncomingShareModule(
            groupName: suite, groupRoot: groupRoot, privateRoot: privateRoot
        )
        defer { module.close() }

        let first = expectation(description: "first share")
        module.invoke(method: "initial", payload: Data()) { status, payload in
            defer { first.fulfill() }
            XCTAssertEqual(status, .success)
            do {
                let values = try WireMap.decode(payload)
                XCTAssertEqual(values["available"], .flag(true))
                XCTAssertEqual(values["text"], .text("Olá 🌎"))
                XCTAssertEqual(values["subject"], .text("Picture"))
                let json = try XCTUnwrap(values["files"])
                guard case let .text(rawFiles) = json,
                      let files = try JSONSerialization.jsonObject(
                        with: Data(rawFiles.utf8)
                      ) as? [[String: Any]],
                      let file = files.first,
                      let path = file["path"] as? String else {
                    XCTFail("Incoming file reference is missing")
                    return
                }
                XCTAssertEqual(files.count, 1)
                XCTAssertEqual(file["mimeType"] as? String, "image/jpeg")
                XCTAssertEqual(file["name"] as? String, "original.jpg")
                XCTAssertEqual(try Data(contentsOf: privateRoot.appendingPathComponent(path)),
                               Data("photo bytes".utf8))
                guard case let .text(eventId)? = values["eventId"] else {
                    XCTFail("Incoming share has no event id")
                    return
                }
                XCTAssertTrue(path.hasPrefix("imports/incoming/\(eventId)/"))
                // The app releases the share once it is done with its files.
                XCTAssertTrue(IncomingShareStorage.release(root: privateRoot, eventId: eventId))
                XCTAssertFalse(FileManager.default.fileExists(atPath: privateRoot.appendingPathComponent(path).path))
                XCTAssertFalse(FileManager.default.fileExists(
                    atPath: inbox.appendingPathComponent(fileName).path
                ))
            } catch { XCTFail("Cannot decode first share: \(error)") }
        }
        // The first UniformTypeIdentifiers lookup can be cold on a freshly
        // booted Simulator. Keep the assertion deterministic under the full
        // suite without changing the module's runtime behaviour.
        wait(for: [first], timeout: 10)

        let second = expectation(description: "next share")
        module.invoke(method: "next", payload: Data()) { status, payload in
            XCTAssertEqual(status, .success)
            XCTAssertEqual(try? WireMap.decode(payload)["text"], .text("next share"))
            second.fulfill()
        }
        wait(for: [second], timeout: 10)
    }

    func testSweepDeletesExpiredSharesAndLegacyCopiesOnly() throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent("pam-share-sweep-\(UUID().uuidString)")
        defer { try? FileManager.default.removeItem(at: root) }
        let manager = FileManager.default
        func file(_ path: String, age: TimeInterval) throws {
            let url = root.appendingPathComponent(path)
            try manager.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
            try Data("x".utf8).write(to: url)
            let date = Date().addingTimeInterval(-age)
            try manager.setAttributes([.modificationDate: date], ofItemAtPath: url.path)
            try manager.setAttributes([.modificationDate: date], ofItemAtPath: url.deletingLastPathComponent().path)
        }
        let day: TimeInterval = 86_400
        try file("imports/incoming/old/incoming-1.jpg", age: day + 60)
        try file("imports/incoming/new/incoming-2.jpg", age: 60)
        try file("imports/incoming-3.mp4", age: day + 60)
        try file("imports/gallery-4.jpg", age: day * 30)
        XCTAssertEqual(IncomingShareStorage.sweep(root: root, now: Date(), maxAge: day), 2)
        XCTAssertFalse(manager.fileExists(atPath: root.appendingPathComponent("imports/incoming/old").path))
        XCTAssertTrue(manager.fileExists(atPath: root.appendingPathComponent("imports/incoming/new/incoming-2.jpg").path))
        XCTAssertTrue(manager.fileExists(atPath: root.appendingPathComponent("imports/gallery-4.jpg").path))
        XCTAssertFalse(IncomingShareStorage.release(root: root, eventId: "../x"))
    }

    func testPendingListenerReceivesShareWhenAppBecomesActive() throws {
        let suite = "dev.pam.incoming-share-test.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let module = IncomingShareModule(groupName: suite)
        defer { module.close() }
        let received = expectation(description: "share after activation")
        module.invoke(method: "next", payload: Data()) { status, payload in
            XCTAssertEqual(status, .success)
            XCTAssertEqual(try? WireMap.decode(payload)["text"], .text("warm share"))
            received.fulfill()
        }
        let armed = expectation(description: "listener is armed")
        module.invoke(method: "next", payload: Data()) { status, _ in
            XCTAssertEqual(status, .failure)
            armed.fulfill()
        }
        wait(for: [armed], timeout: 2)
        defaults.set([
            ["kind": 1, "value": "warm share", "mimeType": "text/plain", "createdAtMillis": 3],
        ], forKey: "pam.share.items")
        NotificationCenter.default.post(
            name: UIApplication.didBecomeActiveNotification, object: nil
        )
        wait(for: [received], timeout: 2)
    }
}
