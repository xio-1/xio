package org.xio.one.reactive.flow.internal;

import org.xio.one.reactive.flow.Flow;
import org.xio.one.reactive.flow.domain.item.Item;

import java.util.NavigableSet;
import java.util.logging.Logger;

public class SimpleHousekeeper implements Housekeeper {

    Logger logger = java.util.logging.Logger.getLogger(SimpleHousekeeper.class.getCanonicalName());

    public boolean housekeep(Flow flow) {
        if (flow.maxTTLSeconds() > 0) {
            long count = ((NavigableSet<Item>) flow.getSink().getItemStoreContents()).stream()
                    .filter(i -> !i.isReadyForHouseKeeping(flow.maxTTLSeconds()))
                    .map(d -> flow.getSink().getItemStoreContents().remove(d)).count();
            if (count > 0) {
                logger.info("cleaned flow " + flow.name() + " : removed " + count + " items");
            }
            return true;
        }
        return false;

    }
}
