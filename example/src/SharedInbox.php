<?php

declare(strict_types=1);

namespace App;

use Pam\Native\App;
use Pam\Native\AppState;
use Pam\Native\Component;
use Pam\Native\Element;
use Pam\Native\ShareExtension\SharedItem;
use Pam\Native\ShareExtension\SharedItemKind;
use Pam\Native\ShareExtension\ShareInbox;
use Pam\Native\Style;
use Pam\Native\UI\Button;
use Pam\Native\UI\Column;
use Pam\Native\UI\SafeAreaView;
use Pam\Native\UI\Screen;
use Pam\Native\UI\Text;

/** Share text, links, photos or files to this app from any other app; they are listed here. */
final class SharedInbox extends Component
{
    private ShareInbox $inbox;

    /** @var list<SharedItem> */
    private array $items = [];

    public function boot(): void
    {
        $this->inbox = new ShareInbox();
        $this->drain();
        // Shares that arrive while the app is in the background are drained on resume.
        App::onStateChange(function (AppState $state): void {
            if ($state === AppState::Active) {
                $this->drain();
            }
        });
    }

    public function render(): Element
    {
        $rows = array_map(static fn (SharedItem $item): Text => Text::make(sprintf(
            '%s %s · %s',
            match ($item->kind) {
                SharedItemKind::Text => '📝',
                SharedItemKind::Url => '🔗',
                SharedItemKind::File => '📎',
            },
            strlen($item->value) > 60 ? substr($item->value, 0, 57).'…' : $item->value,
            $item->mimeType,
        )), $this->items);

        return Screen::make(
            SafeAreaView::make(
                Column::make(
                    Text::make('Shared with this app')->style(new Style(fontSize: 24, fontWeight: 700)),
                    Text::make(count($this->items).' item(s). Validate MIME type and content before using a file.'),
                    Button::make('Drain now')->onPress($this->drain(...)),
                    ...$rows,
                )->style(new Style(flexGrow: 1, padding: 24, gap: 10)),
            ),
        );
    }

    public function drain(): void
    {
        $this->inbox->drain(function (array $items): void {
            // Drained entries are consumed: keep (or move) what you need now.
            array_push($this->items, ...$items);
        });
    }
}
