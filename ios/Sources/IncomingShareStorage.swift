import Foundation

/// Mirrors Android `IncomingShareStorage.kt`: shared files are copied to
/// `pam-files/imports/incoming/<eventId>/`, released by the app when it is
/// done with them and deleted after the retention otherwise.
enum IncomingShareStorage {
    static let incomingDirectory = "imports/incoming"

    static func validEventId(_ eventId: String) -> Bool {
        eventId.range(of: "^[A-Za-z0-9-]{1,64}$", options: .regularExpression) != nil
    }

    /// Deletes every file of one share event; false when nothing was there.
    static func release(root: URL, eventId: String) -> Bool {
        guard validEventId(eventId) else { return false }
        let directory = root.appendingPathComponent(incomingDirectory, isDirectory: true)
            .appendingPathComponent(eventId, isDirectory: true)
        return (try? FileManager.default.removeItem(at: directory)) != nil
    }

    /// Deletes share events (and `imports/incoming-*` files of older plugin
    /// versions) last modified before `now - maxAge`; returns how many.
    @discardableResult
    static func sweep(root: URL, now: Date, maxAge: TimeInterval) -> Int {
        let cutoff = now.addingTimeInterval(-maxAge)
        let manager = FileManager.default
        let keys: [URLResourceKey] = [.contentModificationDateKey, .isRegularFileKey]
        func expired(_ url: URL) -> Bool {
            let modified = (try? url.resourceValues(forKeys: [.contentModificationDateKey]))?.contentModificationDate
            return modified.map { $0 < cutoff } ?? false
        }
        let events = (try? manager.contentsOfDirectory(
            at: root.appendingPathComponent(incomingDirectory, isDirectory: true), includingPropertiesForKeys: keys
        )) ?? []
        let legacy = ((try? manager.contentsOfDirectory(
            at: root.appendingPathComponent("imports", isDirectory: true), includingPropertiesForKeys: keys
        )) ?? []).filter {
            $0.lastPathComponent.hasPrefix("incoming-")
                && (try? $0.resourceValues(forKeys: [.isRegularFileKey]))?.isRegularFile == true
        }
        var removed = 0
        for url in events + legacy where expired(url) {
            if (try? manager.removeItem(at: url)) != nil { removed += 1 }
        }
        return removed
    }
}
