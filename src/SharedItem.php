<?php
declare(strict_types=1);namespace Pam\Native\ShareExtension;final readonly class SharedItem{public function __construct(public string$identifier,public SharedItemKind$kind,public string$value,public string$mimeType,public int$createdAtMillis){}}
