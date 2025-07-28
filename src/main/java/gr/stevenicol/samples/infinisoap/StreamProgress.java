package gr.stevenicol.samples.infinisoap;

/**
 * Progress information for streaming cache operations.
 * Provides real-time feedback during large data uploads/downloads.
 */
public class StreamProgress {
    private final String key;
    private final int currentChunk;
    private final int totalChunks;
    private final String message;

    public StreamProgress(String key, int currentChunk, int totalChunks, String message) {
        this.key = key;
        this.currentChunk = currentChunk;
        this.totalChunks = totalChunks;
        this.message = message;
    }

    public String getKey() {
        return key;
    }

    public int getCurrentChunk() {
        return currentChunk;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public String getMessage() {
        return message;
    }

    public int getProgressPercent() {
        if (totalChunks == 0) return 0;
        return (int) ((double) currentChunk / totalChunks * 100);
    }

    public boolean isCompleted() {
        return currentChunk >= totalChunks && totalChunks > 0;
    }

    public boolean hasError() {
        return message.toLowerCase().contains("failed") || message.toLowerCase().contains("error");
    }

    @Override
    public String toString() {
        return "StreamProgress{" +
                "key='" + key + '\'' +
                ", progress=" + currentChunk + "/" + totalChunks +
                " (" + getProgressPercent() + "%)" +
                ", message='" + message + '\'' +
                '}';
    }
}