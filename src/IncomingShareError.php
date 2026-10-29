<?php

declare(strict_types=1);

namespace Pam\Native\ShareExtension;

enum IncomingShareError: int
{
    case FileTooLarge = 1;
    case CopyFailed = 2;
    case QueueFull = 3;
}
