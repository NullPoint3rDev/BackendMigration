package org.alloy.protocol.v2;

import java.time.LocalDateTime;

import org.alloy.models.entities.V2SessionIndex;
import org.alloy.repositories.V2SessionIndexRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2IndexService {
    public static final String CHANNEL_LIVE = "LIVE";
    public static final String CHANNEL_HISTORY = "HISTORY";

    private final V2SessionIndexRepository repo;

    public V2IndexService(V2SessionIndexRepository repo) {
        this.repo = repo;
    }

    /** -1 если ещё не было. */
    @Transactional(readOnly = true)
    public int getLastIndex(String mac, int sessionNumber, String channel) {
        V2SessionIndex.PK pk = new V2SessionIndex.PK();
        pk.setMac(mac);
        pk.setSessionNumber(sessionNumber);
        pk.setChannel(channel);
        return repo.findById(pk).map(V2SessionIndex::getLastIndex).orElse(-1);
    }

    @Transactional
    public void saveLastIndex(String mac, int sessionNumber, String channel, int index) {
        V2SessionIndex.PK pk = new V2SessionIndex.PK();
        pk.setMac(mac);
        pk.setSessionNumber(sessionNumber);
        pk.setChannel(channel);

        V2SessionIndex row = repo.findById(pk).orElseGet(V2SessionIndex::new);
        row.setMac(mac);
        row.setSessionNumber(sessionNumber);
        row.setChannel(channel);
        row.setLastIndex(index);
        row.setUpdatedAt(LocalDateTime.now());
        repo.save(row);
    }
}
