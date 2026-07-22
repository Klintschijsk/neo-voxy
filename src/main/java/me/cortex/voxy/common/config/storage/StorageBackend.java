package me.cortex.voxy.common.config.storage;

import me.cortex.voxy.common.config.IMappingStorage;
import me.cortex.voxy.common.config.IStoredSectionPositionIterator;
import me.cortex.voxy.common.util.MemoryBuffer;

import java.util.ArrayList;
import java.util.List;

public abstract class StorageBackend implements IMappingStorage, IStoredSectionPositionIterator {

    //Implementation may use the scratch buffer as the return value, it MUST NOT free the scratch buffer
    public abstract MemoryBuffer getSectionData(long key, MemoryBuffer scratch);

    public abstract void setSectionData(long key, MemoryBuffer data);

    public abstract void deleteSectionData(long key);

    //Long-keyed tables beside the section store, for things derived from the world that are not voxel
    //data - a beacon index, later a contraption snapshot. Kept out of the section store because these
    //are read by their own consumers and must not turn up in iteratePositions, and out of the id
    //mappings because those are int-keyed and loaded whole.
    //
    //Default: no table, so a backend without one costs nothing and its caller simply does not persist.
    //Every table is written whole per key - a key is put or deleted, never edited in place - so a
    //re-derived key needs no diff against what was there.
    public interface AuxEntryConsumer {
        void accept(long key, byte[] value);
    }

    public boolean supportsAuxTable(String table) {
        return false;
    }

    public void putAux(String table, long key, byte[] value) {}

    //Single-key read. Used by consumers that ask about one section at a time rather than loading a whole
    //table up front, so nothing has to be held in memory between the write and the read.
    public byte[] getAux(String table, long key) {
        return null;
    }

    public void deleteAux(String table, long key) {}

    public void forEachAux(String table, AuxEntryConsumer consumer) {}

    public abstract void flush();

    public abstract void close();

    public List<StorageBackend> getChildBackends() {
        return List.of();
    }

    public final List<StorageBackend> collectAllBackends() {
        List<StorageBackend> backends = new ArrayList<>();
        backends.add(this);
        for (var child : this.getChildBackends()) {
            backends.addAll(child.collectAllBackends());
        }
        return backends;
    }
}
