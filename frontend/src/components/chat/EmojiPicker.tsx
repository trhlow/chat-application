interface EmojiPickerProps {
  onSelect: (emoji: string) => void;
}

const emojis = [
  "😀", "😃", "😄", "😁", "😂", "😊", "😍", "🥰",
  "😘", "😎", "🤔", "😮", "😢", "😭", "😡", "👍",
  "👎", "👏", "🙌", "🙏", "💪", "❤️", "💙", "🔥",
  "✨", "🎉", "🎂", "✅", "❌", "💯", "👀", "🤝",
];

export const EmojiPicker = ({ onSelect }: EmojiPickerProps) => (
  <div
    className="grid max-h-64 grid-cols-8 gap-1 overflow-y-auto rounded-lg border border-border bg-card p-2 shadow-soft"
    aria-label="Chọn emoji"
    role="listbox"
  >
    {emojis.map((emoji) => (
      <button
        key={emoji}
        type="button"
        className="rounded-md p-2 text-lg transition hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        onClick={() => onSelect(emoji)}
        aria-label={`Chèn ${emoji}`}
        aria-selected="false"
        role="option"
      >
        {emoji}
      </button>
    ))}
  </div>
);
