<?php

declare(strict_types=1);

namespace Pam\Native\ShareExtension;

use Pam\Native\FileReference;

final readonly class IncomingShare
{
    /** @param list<FileReference> $files */
    public function __construct(
        public string $text,
        public string $subject,
        public string $mimeType,
        public array $files,
        public string $eventId = '',
    ) {
    }
}
