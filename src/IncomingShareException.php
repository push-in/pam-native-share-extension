<?php

declare(strict_types=1);

namespace Pam\Native\ShareExtension;

use RuntimeException;

final class IncomingShareException extends RuntimeException
{
    public function __construct(public readonly IncomingShareError $reason, public readonly string $eventId)
    {
        parent::__construct(match ($reason) {
            IncomingShareError::FileTooLarge => 'Shared file exceeds maximumBytes.',
            IncomingShareError::CopyFailed => 'Cannot copy shared file.',
            IncomingShareError::QueueFull => 'Incoming-share queue is full.',
        }, $reason->value);
    }
}
