<?php

declare(strict_types=1);

namespace Pam\Native\ShareExtension;

use InvalidArgumentException;

/** Android intake limits, applied before any shared file is copied. */
final readonly class IncomingShareLimits
{
    public function __construct(
        public int $maximumBytes = 314_572_800,
        public int $maximumItems = 20,
        public int $maximumVideoItems = 1,
    ) {
        if ($maximumBytes < 1 || $maximumItems < 1 || $maximumItems > 32
            || $maximumVideoItems < 1 || $maximumVideoItems > $maximumItems) {
            throw new InvalidArgumentException('Invalid incoming-share limits.');
        }
    }

    /** @return array{maximumBytes: int, maximumItems: int, maximumVideoItems: int} */
    public function toPayload(): array
    {
        return [
            'maximumBytes' => $this->maximumBytes,
            'maximumItems' => $this->maximumItems,
            'maximumVideoItems' => $this->maximumVideoItems,
        ];
    }
}
