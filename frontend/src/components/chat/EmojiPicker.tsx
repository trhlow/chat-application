interface EmojiPickerProps {
  onSelect: (emoji: string) => void;
}

const emojis = ["😀", "😂", "❤️", "👍", "🎉", "😢", "😮", "🙏"];

export const EmojiPicker = ({ onSelect }: EmojiPickerProps) => (
  <div className="grid grid-cols-4 gap-1 rounded-lg border border-border bg-card p-2">
    {emojis.map((emoji) => (
      <button
        key={emoji}
        type="button"
        className="rounded-md p-2 text-lg hover:bg-muted"
        onClick={() => onSelect(emoji)}
      >
        {emoji}
      </button>
    ))}
  </div>
);
