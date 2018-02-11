package org.xio.one.stream;

import org.xio.one.stream.selector.Selector;

import java.util.HashMap;
import java.util.Map;


/**
 * EventStream Definition that is used to define the meta-data of a EventStream
 *
 * @author Xio
 * Date: 10/12/13
 * Time: 21:00
 */
public class AsyncStreamDefinition {
  public static int EVENT_TTL_FOREVER = 0;
  private String name = "DEFAULT";
  private String description = "";
  private String timestampfieldname;
  private int eventTTLSeconds = EVENT_TTL_FOREVER;
  private Selector selector = new Selector();
  private String indexFieldName;
  private String eventWorkerClassName;
  private Map<String, Object> workerParams = new HashMap<>();

  public AsyncStreamDefinition() {
    super();
  }

  public String getIndexFieldName() {
    return indexFieldName;
  }

  public void setIndexFieldName(String indexFieldName) {
    this.indexFieldName = indexFieldName;
  }

  public void setTimestampfieldname(String timestampfieldname) {
    this.timestampfieldname = timestampfieldname;
  }

  public void setEventWorkerClassName(String eventWorkerClassName) {
    this.eventWorkerClassName = eventWorkerClassName;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getEventTTLSeconds() {
    return this.eventTTLSeconds;
  }

  public void setEventTTLSeconds(int eventTTLSeconds) {
    this.eventTTLSeconds = eventTTLSeconds;
  }

  public Selector getSelector() {
    return this.selector;
  }

  public void setSelector(Selector selector) {
    this.selector = selector;
  }

  public Map<String, Object> getWorkerParams() {
    return workerParams;
  }

  @Override
  public String toString() {
    return this.name;

  }


}
