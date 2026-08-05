package org.alloy.protocol.v2;

/**
 * Авто-recover по каталогу SD: firstSession..lastSession → очередь 0x03,
 * дальше V2SessionInfoHandler сравнивает с serverLast и ставит 0x05.
 */
public final class V2SdRecover {
    /** ponytail: потолок опросов за один коннект; при переполнении берём хвост (свежие сессии). */
    public static final int MAX_SESSIONS = 64;

    private V2SdRecover() {}

    /**
     * Ставит 0x03 на каждую сессию в [first, last] (кроме первой — её возвращает для piggyback).
     *
     * @return первая команда 0x03 или null если диапазон пуст / нет очереди
     */
    public static V2HistoryCommand begin(
            V2CommandQueue commands, String mac, int firstSession, int lastSession) {
        if (commands == null || mac == null || mac.isEmpty()) {
            return null;
        }
        if (lastSession < firstSession) {
            return null;
        }
        int from = firstSession;
        int to = lastSession;
        long span = (long) to - (long) from + 1L;
        if (span > MAX_SESSIONS) {
            from = to - MAX_SESSIONS + 1;
        }
        V2HistoryCommand first = null;
        for (int s = from; s <= to; s++) {
            V2HistoryCommand c = V2HistoryCommand.requestSessionInfo(s);
            if (first == null) {
                first = c;
            } else {
                commands.enqueue(mac, c);
            }
        }
        return first;
    }
}
