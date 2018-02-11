package org.xio.one.stream;

import org.xio.one.stream.selector.Selector;

public final class AsyncStreamDefinitionBuilder {
  private String name="DEFAULT";
  private String description="";
  private String timestampfieldname;
  private int eventTTLSeconds=0;
  private Selector selector=new Selector();
  private String eventWorkerClassName;

  private AsyncStreamDefinitionBuilder() {
  }

  public static AsyncStreamDefinitionBuilder anEventStreamDefinition() {
    return new AsyncStreamDefinitionBuilder();
  }

  public AsyncStreamDefinitionBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public AsyncStreamDefinitionBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  public AsyncStreamDefinitionBuilder withTimestampfieldname(String timestampfieldname) {
    this.timestampfieldname = timestampfieldname;
    return this;
  }

  public AsyncStreamDefinitionBuilder withEventTTLSeconds(int eventTTLSeconds) {
    this.eventTTLSeconds = eventTTLSeconds;
    return this;
  }

  public AsyncStreamDefinitionBuilder withSelector(Selector selector) {
    this.selector = selector;
    return this;
  }

  public AsyncStreamDefinitionBuilder withEventWorkerClassName(String eventWorkerClassName) {
    this.eventWorkerClassName = eventWorkerClassName;
    return this;
  }

  public AsyncStreamDefinition build() {
    AsyncStreamDefinition eventStreamDefinition = new AsyncStreamDefinition();
    eventStreamDefinition.setName(name);
    eventStreamDefinition.setDescription(description);
    eventStreamDefinition.setTimestampfieldname(timestampfieldname);
    eventStreamDefinition.setEventTTLSeconds(eventTTLSeconds);
    eventStreamDefinition.setSelector(selector);
    eventStreamDefinition.setEventWorkerClassName(eventWorkerClassName);
    return eventStreamDefinition;
  }
}
