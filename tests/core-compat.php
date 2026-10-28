<?php
declare(strict_types=1);
use Pam\Native\ShareExtension\IncomingShare;
use Pam\Native\Internal\Runtime;
use Pam\Native\Internal\Wire;
use Pam\Native\ModuleResultStatus;
use Pam\Native\ShareExtension\IncomingShares;


$lastCall = static function (): array {
    $call = TestDiagnostics::$moduleCall;
    if ($call === null) {
        throw new RuntimeException('No native module call was recorded.');
    }

    return $call + ['values' => $call['payload'] === '' ? [] : Wire::decodeMap($call['payload'])];
};
$rejects = static function (Closure $operation): bool {
    try {
        $operation();
    } catch (InvalidArgumentException|RuntimeException|LogicException) {
        return true;
    }

    return false;
};


$incomingShare = null;
$incomingShareRequestId = IncomingShares::initial(
    static function (?IncomingShare $share) use (&$incomingShare): void {
        $incomingShare = $share;
    },
);
$incomingShareCall = TestDiagnostics::$moduleCall;
$assert(
    $incomingShareCall !== null
        && $incomingShareCall['requestId'] === $incomingShareRequestId
        && $incomingShareCall['module'] === 'incoming-share'
        && $incomingShareCall['method'] === 'initial',
    'IncomingShares facade did not request the cold-start payload.',
);
Runtime::dispatchModuleResult(
    $incomingShareRequestId,
    \Pam\Native\ModuleResultStatus::Success->value,
    Wire::map([
        'available' => true,
        'text' => 'Shared caption',
        'subject' => 'Shared subject',
        'mimeType' => 'image/jpeg',
        'files' => json_encode([[
            'path' => '/data/user/0/app/cache/pam-incoming-shares/incoming-photo.jpg',
            'name' => 'photo.jpg',
            'mimeType' => 'image/jpeg',
            'size' => 1234,
        ]], JSON_THROW_ON_ERROR),
    ]),
);
$assert(
    $incomingShare instanceof IncomingShare
        && $incomingShare->text === 'Shared caption'
        && count($incomingShare->files) === 1
        && $incomingShare->files[0]->name === 'photo.jpg'
        && $incomingShare->files[0]->size === 1234,
    'IncomingShares facade did not decode the sandboxed file payload.',
);

