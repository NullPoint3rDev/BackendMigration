package org.alloy.protocol.v2;

/**
 * После 0x04: если на плате last > server last — запросить недостающий диапазон.
 */
public final class V2HistoryRecover {
    private V2HistoryRecover() {}

    /**
     * @param serverLast последний индекс на сервере (−1 если нет)
     * @param firstIdx   первая запись на плате
     * @param lastIdx    последняя запись на плате
     * @return 0x05 или null если догонять нечего
     */
    public static V2HistoryCommand plan(int sessionNo, int serverLast, int firstIdx, int lastIdx) {
        if (lastIdx < firstIdx) {
            return null;
        }
        int from = serverLast < 0 ? firstIdx : serverLast + 1;
        if (from > lastIdx) {
            return null;
        }
        if (from < firstIdx) {
            from = firstIdx;
        }
        return V2HistoryCommand.requestHistory(sessionNo, from, lastIdx);
    }
}
