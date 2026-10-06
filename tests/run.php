<?php
declare(strict_types=1);

$roots = [
    'Pam\\Native\\ShareExtension\\' => dirname(__DIR__) . '/src/',
    'Pam\\Native\\' => dirname(__DIR__, 2) . '/../pam-native/packages/native/src/',
];
spl_autoload_register(static function (string $class) use ($roots): void {
    foreach ($roots as $prefix => $root) {
        if (str_starts_with($class, $prefix)) {
            $file = $root . str_replace('\\', '/', substr($class, strlen($prefix))) . '.php';
            if (is_file($file)) {
                require $file;
            }
            return;
        }
    }
});

use Pam\Native\ShareExtension\SharedItemKind;

$package = dirname(__DIR__);
$tests = 0;
$failures = 0;
$check = static function (string $name, bool $passed) use (&$tests, &$failures): void {
    $tests++;
    if (!$passed) {
        $failures++;
    }
    echo ($passed ? 'PASS ' : 'FAIL ') . $name . "\n";
};
$json = static fn (string $path): array => json_decode(
    (string) file_get_contents($package . '/' . $path),
    true,
    flags: JSON_THROW_ON_ERROR,
);
$mime = '~^[a-z0-9!#$&^_.+*-]+/(?:[a-z0-9!#$&^_.+*-]+|\*)$~';
// Mirrors PAM Native's resolution of plugins.<configKey> (accept, multiple).
$validShareConfig = static function (array $config) use ($mime): bool {
    if (array_diff(array_keys($config), ['accept', 'multiple']) !== []) {
        return false;
    }
    $accept = $config['accept'] ?? ['*/*'];
    if (!is_array($accept) || !array_is_list($accept) || $accept === [] || count($accept) > 16
        || count(array_unique($accept)) !== count($accept)) {
        return false;
    }
    foreach ($accept as $type) {
        if (!is_string($type) || preg_match($mime, $type) !== 1) {
            return false;
        }
    }
    $multiple = $config['multiple'] ?? true;
    if (is_bool($multiple)) {
        return true;
    }

    return is_array($multiple) && array_is_list($multiple) && array_diff($multiple, $accept) === [];
};

$check(
    'shared item kinds are sequential',
    array_map(static fn (SharedItemKind $kind): int => $kind->value, SharedItemKind::cases()) === [1, 2, 3],
);

$plugin = $json('pam-native.plugin.json');
$check(
    'share types are declared for PAM Native to generate, defaulting to */*',
    ($plugin['share'] ?? null) === ['configKey' => 'shareExtension', 'accept' => ['*/*'], 'multiple' => true],
);
$check(
    'the plugin requires the PAM Native release that reads share and no capability its runtime lacks',
    version_compare($plugin['pamNative']['minimum'], '1.16.0', '>=')
        && !isset($plugin['capabilities']),
);
$check(
    'Android intent filters are not hardcoded in a manifest fragment',
    !isset($plugin['android']['manifest']) && !is_file($package . '/android/src/main/AndroidManifest.xml'),
);
$infoPlist = (string) file_get_contents($package . '/ios/ShareExtension/Info.plist');
$check(
    'the iOS activation rule is generated, not hardcoded',
    !str_contains($infoPlist, 'NSExtensionActivationRule')
        && str_contains($infoPlist, 'com.apple.share-services'),
);
$check(
    'composer requires a PAM Native release with share configuration',
    $json('composer.json')['require']['pushinbr/pam-native'] === '^1.16',
);

$example = $json('example/pam-native.json');
$check(
    'the example narrows the share sheet to text, images and videos',
    ($example['plugins']['shareExtension'] ?? null) === [
        'accept' => ['text/plain', 'image/*', 'video/*'],
        'multiple' => ['image/*', 'video/*'],
    ] && $validShareConfig($example['plugins']['shareExtension']),
);
$check(
    'share configuration validation mirrors PAM Native',
    $validShareConfig([])
        && $validShareConfig(['accept' => ['application/pdf'], 'multiple' => false])
        && !$validShareConfig(['accept' => []])
        && !$validShareConfig(['accept' => ['Image/*']])
        && !$validShareConfig(['accept' => ['image/*'], 'multiple' => ['video/*']])
        && !$validShareConfig(['accept' => ['image/*'], 'types' => []]),
);

echo "{$tests} tests, {$failures} failures\n";
exit($failures === 0 ? 0 : 1);
