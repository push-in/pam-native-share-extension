import Foundation
import Social
import UniformTypeIdentifiers

final class ShareViewController: SLComposeServiceViewController {
    override func isContentValid() -> Bool { true }

    override func didSelectPost() {
        let group = DispatchGroup()
        let lock = NSLock()
        let shareID = UUID().uuidString
        let now = Int64(Date().timeIntervalSince1970 * 1_000)
        let items = extensionContext?.inputItems as? [NSExtensionItem] ?? []
        let subject = items.compactMap { $0.attributedTitle?.string }.first ?? ""
        var rows: [[String: Any]] = []

        let text = contentText ?? ""
        if !text.isEmpty {
            rows.append(row(kind: 1, value: text, mime: "text/plain",
                            time: now, shareID: shareID, subject: subject))
        }
        for item in items {
            for provider in item.attachments ?? [] {
                if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) {
                    group.enter()
                    provider.loadItem(forTypeIdentifier: UTType.url.identifier) { [weak self] value, _ in
                        defer { group.leave() }
                        guard let self, let url = value as? URL else { return }
                        let entry: [String: Any]?
                        if url.isFileURL, let copied = self.copyToGroup(url) {
                            let mime = UTType(filenameExtension: url.pathExtension)?.preferredMIMEType
                                ?? self.mimeType(for: provider)
                            entry = self.row(kind: 3, value: copied, mime: mime,
                                             time: now, shareID: shareID, subject: subject,
                                             name: provider.suggestedName ?? url.lastPathComponent)
                        } else if !url.isFileURL {
                            entry = self.row(kind: 2, value: url.absoluteString, mime: "text/uri-list",
                                             time: now, shareID: shareID, subject: subject)
                        } else {
                            entry = nil
                        }
                        if let entry {
                            lock.lock()
                            rows.append(entry)
                            lock.unlock()
                        }
                    }
                    continue
                }

                guard let type = provider.registeredTypeIdentifiers.first(where: {
                    UTType($0)?.conforms(to: .data) == true
                }) else { continue }
                group.enter()
                provider.loadFileRepresentation(forTypeIdentifier: type) { [weak self] url, _ in
                    defer { group.leave() }
                    guard let self, let url, let copied = self.copyToGroup(url) else { return }
                    let entry = self.row(kind: 3, value: copied, mime: self.mimeType(for: provider),
                                         time: now, shareID: shareID, subject: subject,
                                         name: provider.suggestedName ?? url.lastPathComponent)
                    lock.lock()
                    rows.append(entry)
                    lock.unlock()
                }
            }
        }
        group.notify(queue: .main) {
            self.append(rows)
            self.extensionContext?.completeRequest(returningItems: [])
        }
    }

    private func row(
        kind: Int, value: String, mime: String, time: Int64,
        shareID: String, subject: String, name: String = ""
    ) -> [String: Any] {
        [
            "id": UUID().uuidString,
            "shareId": shareID,
            "kind": kind,
            "value": String(value.prefix(kind == 1 ? 65_536 : 8_192)),
            "mimeType": String(mime.prefix(255)),
            "createdAtMillis": time,
            "subject": String(subject.prefix(4_096)),
            "name": String(name.prefix(255)),
        ]
    }

    private func mimeType(for provider: NSItemProvider) -> String {
        for identifier in provider.registeredTypeIdentifiers {
            if let mime = UTType(identifier)?.preferredMIMEType { return mime }
        }
        return "application/octet-stream"
    }

    private var groupName: String? {
        Bundle.main.object(forInfoDictionaryKey: "PamNativeAppGroup") as? String
    }

    private func copyToGroup(_ source: URL) -> String? {
        guard let groupName,
              let root = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: groupName)
        else { return nil }
        let access = source.startAccessingSecurityScopedResource()
        defer { if access { source.stopAccessingSecurityScopedResource() } }
        guard let size = try? source.resourceValues(forKeys: [.fileSizeKey]).fileSize,
              size >= 0, size <= 64 * 1_024 * 1_024 else { return nil }
        let inbox = root.appendingPathComponent("pam-share-inbox", isDirectory: true)
        do {
            try FileManager.default.createDirectory(at: inbox, withIntermediateDirectories: true)
            let name = UUID().uuidString
            let destination = inbox.appendingPathComponent(name)
            try FileManager.default.copyItem(at: source, to: destination)
            let copiedSize = try destination.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? -1
            guard copiedSize == size else {
                try? FileManager.default.removeItem(at: destination)
                return nil
            }
            return name
        } catch {
            return nil
        }
    }

    private func append(_ rows: [[String: Any]]) {
        guard !rows.isEmpty, let groupName, let defaults = UserDefaults(suiteName: groupName) else { return }
        var existing = defaults.array(forKey: "pam.share.items") as? [[String: Any]] ?? []
        existing.append(contentsOf: rows)
        defaults.set(Array(existing.suffix(256)), forKey: "pam.share.items")
    }

    override func configurationItems() -> [Any]! { [] }
}
