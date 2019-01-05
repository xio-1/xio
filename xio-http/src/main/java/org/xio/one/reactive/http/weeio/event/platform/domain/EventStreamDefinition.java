package org.xio.one.reactive.http.weeio.event.platform.domain;

public class EventStreamDefinition {
  public static int EVENT_TTL_FOREVER = 0;
  private String name = "DEFAULT";
  private String description = "";
  private String timestampfieldname;
  private int eventTTLSeconds = EVENT_TTL_FOREVER;

  private String indexFieldName;
  private String eventWorkerClassName;

  public EventStreamDefinition() {
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

  @Override
  public String toString() {
    return this.name;

  }


}
