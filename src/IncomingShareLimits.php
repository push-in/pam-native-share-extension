<?php

declare(strict_types=1);

namespace Pam\Native\ShareExtension;

use InvalidArgumentException;

/**
 * Intake limits, applied before any shared file is copied (Android), and how
 * long copied files are kept when the app never releases them (both).
 */
final readonly class IncomingShareLimits
{
    public function __construct(
        public int $maximumBytes = 314_572_800,
        public int $maximumItems = 20,
        public int $maximumVideoItems = 1,
        public int $retentionHours = 24,
    ) {
        if ($maximumBytes < 1 || $maximumItems < 1 || $maximumItems > 32
            || $maximumVideoItems < 1 || $maximumVideoItems > $maximumItems
            || $retentionHours < 1 || $retentionHours > 720) {
            throw new InvalidArgumentException('Invalid incoming-share limits.');
        }
    }

    /** @return array{maximumBytes: int, maximumItems: int, maximumVideoItems: int, retentionHours: int} */
    public function toPayload(): array
    {
        return [
            'maximumBytes' => $this->maximumBytes,
            'maximumItems' => $this->maximumItems,
            'maximumVideoItems' => $this->maximumVideoItems,
            'retentionHours' => $this->retentionHours,
        ];
    }
}
