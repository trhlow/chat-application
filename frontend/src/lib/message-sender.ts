interface MessageTransport<T> {
  sendRealtime: () => boolean;
  sendRest: () => Promise<T>;
}

export const sendWithSingleFallback = async <T>({
  sendRealtime,
  sendRest,
}: MessageTransport<T>): Promise<T | null> => {
  if (sendRealtime()) {
    return null;
  }

  return sendRest();
};
