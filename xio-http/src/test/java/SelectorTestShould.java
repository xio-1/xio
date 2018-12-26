import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.xio.one.reactive.http.wee.event.platform.domain.Event;
import org.xio.one.reactive.http.wee.event.platform.domain.selector.FilterEntry;
import org.xio.one.reactive.http.wee.event.platform.domain.selector.FilterOperations;
import org.xio.one.reactive.http.wee.event.platform.domain.selector.Selector;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.xio.one.reactive.http.wee.event.platform.domain.EmptyEvent.EMPTY_EVENT;

public class SelectorTestShould {

  private Event result;

  @After
  public void report() {
    System.out.println(result.toString());
  }

  @Test
  public void returnEventWhenEventEqualsFieldValue() {
    Map<String, Object> eventValues = new HashMap<>();
    eventValues.put("testField", "Hello");
    Event event = new Event(eventValues);
    Selector selector = new Selector();
    selector.addFilterEntry(new FilterEntry("testField", FilterOperations.EQ, "Hello"));
    Assert.assertThat(result=selector.work(event), CoreMatchers.is(event));
  }

  @Test
  public void returnEmptyEventWhenEventNotEqualsFieldValue() {
    Map<String, Object> eventValues = new HashMap<>();
    eventValues.put("testField", "Hello");
    Event event = new Event(eventValues);

    Selector selector = new Selector();

    selector.addFilterEntry(new FilterEntry("testField", FilterOperations.EQ, "Hello2"));

    Assert.assertThat(result=selector.work(event), CoreMatchers.is(EMPTY_EVENT));

  }

  @Test
  public void returnEventWhenTwoNumbersAreEqual() {
    Map<String, Object> eventValues = new HashMap<>();
    eventValues.put("testField", 3.333);
    Event event = new Event(eventValues);

    Selector selector = new Selector();

    selector.addFilterEntry(new FilterEntry("testField", FilterOperations.EQ, 3.333));
    selector.work(event);
    Assert.assertThat(result=selector.work(event), CoreMatchers.is(event));
  }

  @Test
  public void returnEmptyEventWhenTwoNumbersAreNotEqual() {
    Map<String, Object> eventValues = new HashMap<>();
    eventValues.put("testField", 3.333);
    Event event = new Event(eventValues);

    Selector selector = new Selector();

    selector.addFilterEntry(new FilterEntry("testField", FilterOperations.EQ, 3));

    Assert.assertThat(result=selector.work(event), CoreMatchers.is(EMPTY_EVENT));
  }

  @Test
  public void returnEventWhenEventNumbersIsGreaterThan() {
    Map<String, Object> eventValues = new HashMap<>();
    eventValues.put("testField", 3.333);
    Event event = new Event(eventValues);

    Selector selector = new Selector();

    selector.addFilterEntry(new FilterEntry("testField", FilterOperations.GT, 2.333));
    selector.work(event);
    Assert.assertThat(result=selector.work(event), CoreMatchers.equalTo(event));
  }


  @Test
  public void returnEmptyEventWhenEventNumbersIsNotGreaterThan() {
    Map<String, Object> eventValues = new HashMap<>();
    eventValues.put("testField", 1.333);
    Event event = new Event(eventValues);

    Selector selector = new Selector();

    selector.addFilterEntry(new FilterEntry("testField", FilterOperations.GT, 2.333));
    selector.work(event);
    Assert.assertThat(result=selector.work(event), CoreMatchers.is(EMPTY_EVENT));
  }

  @Test
  public void returnEventWhenEventNumbersIsLessThan() {
    Map<String, Object> eventValues = new HashMap<>();
    eventValues.put("testField", 3.333);
    Event event = new Event(eventValues);

    Selector selector = new Selector();

    selector.addFilterEntry(new FilterEntry("testField", FilterOperations.LT, 4.333));
    selector.work(event);
    Assert.assertThat(result=selector.work(event), CoreMatchers.equalTo(event));
  }

  @Test
  public void returnEmptyEventWhenEventNumbersIsNotLessThan() {
    Map<String, Object> eventValues = new HashMap<>();
    eventValues.put("testField", 5.333);
    Event event = new Event(eventValues);

    Selector selector = new Selector();

    selector.addFilterEntry(new FilterEntry("testField", FilterOperations.LT, 2.333));
    selector.work(event);
    Assert.assertThat(result=selector.work(event), CoreMatchers.is(EMPTY_EVENT));
  }

}
