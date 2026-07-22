package org.alloy.protocol.v2;

/**
 * Дыра в индексах после обрыва связи: last=40, arrived=100 → запрос 41..99.
 */
public class V2GapService {

    /**
     * @param lastAccepted последний успешно принятый индекс (-1 если ещё не было)
     * @param arrived      индекс только что принятой посылки
     */
    public V2HistoryCommand detectGap(int sessionNo, int lastAccepted, int arrived) {
        if (lastAccepted < 0) {
            return null;
        }
        if (arrived > lastAccepted + 1) {
            return V2HistoryCommand.requestHistory(sessionNo, lastAccepted + 1, arrived - 1);
        }
        return null;
    }
}
